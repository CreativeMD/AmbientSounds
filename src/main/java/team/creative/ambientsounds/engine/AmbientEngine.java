package team.creative.ambientsounds.engine;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.Level;
import team.creative.ambientsounds.AmbientSounds;
import team.creative.ambientsounds.block.AmbientBlockGroup;
import team.creative.ambientsounds.dimension.AmbientDimension;
import team.creative.ambientsounds.environment.AmbientEnvironment;
import team.creative.ambientsounds.environment.feature.AmbientFeature;
import team.creative.ambientsounds.environment.pocket.AirPocketGroup;
import team.creative.ambientsounds.region.AmbientRegion;
import team.creative.ambientsounds.sound.AmbientSound;
import team.creative.ambientsounds.sound.AmbientSoundCategory;
import team.creative.ambientsounds.sound.AmbientSoundCollection;
import team.creative.ambientsounds.sound.AmbientSoundEngine;
import team.creative.creativecore.client.render.text.DebugTextRenderer;

public class AmbientEngine {
    
    public static final ResourceLocation CONFIG_LOCATION = ResourceLocation.tryBuild(AmbientSounds.MODID, "config.json");
    public static final String ENGINE_LOCATION = "engine.json";
    public static final String DIMENSIONS_LOCATION = "dimensions";
    public static final String REGIONS_LOCATION = "regions";
    public static final String SOUNDCOLLECTIONS_LOCATION = "sound_collections";
    public static final String SOUNDCATEGORIES_LOCATION = "sound_categories";
    public static final String BLOCKGROUPS_LOCATION = "blockgroups";
    public static final String FEATURES_LOCATION = "features";
    
    public static final Gson GSON = new GsonBuilder().registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer()).create();
    
    private static String loadedEngine;
    
    public static boolean hasLoadedAtLeastOnce() {
        return loadedEngine != null;
    }
    
    public static boolean hasEngineChanged(String newEngine) {
        return loadedEngine == null || !loadedEngine.equals(newEngine);
    }
    
    public static AmbientEngine attemptToLoadEngine(AmbientSoundEngine soundEngine, ResourceManager manager, String name) throws Exception {
        InputStream engineInput = manager.getResource(ResourceLocation.tryBuild(AmbientSounds.MODID, name + "/" + ENGINE_LOCATION)).orElseThrow().open();
        try {
            AmbientEngine engine = GSON.fromJson(JsonParser.parseString(IOUtils.toString(engineInput, Charsets.UTF_8)).getAsJsonObject(), AmbientEngine.class);
            
            if (!engine.name.equals(name))
                throw new Exception("Invalid engine name");
            
            engine.dimensions = loadMultiple(manager, ResourceLocation.tryBuild(AmbientSounds.MODID, name + "/" + DIMENSIONS_LOCATION), AmbientDimension.class, x -> x.stack, (
                    dimension, dimensionName, json) -> {
                dimension.name = dimensionName;
                dimension.load(engine, GSON, manager, json);
                
                if (dimension.loadedRegions != null) {
                    int i = 0;
                    for (AmbientRegion region : dimension.loadedRegions.values()) {
                        if (engine.checkRegion(dimension, i, region))
                            engine.addRegion(region);
                        i++;
                    }
                }
            });
            
            engine.generalRegions = loadMultiple(manager, ResourceLocation.tryBuild(AmbientSounds.MODID, name + "/" + REGIONS_LOCATION), AmbientRegion.class, x -> x.stack, (region,
                    regionName, json) -> {
                region.name = regionName;
                region.load(engine, GSON, manager);
                engine.addRegion(region);
            });
            
            engine.blockGroups = new LinkedHashMap<>();
            String blockGroupPath = name + "/" + BLOCKGROUPS_LOCATION;
            int blockGroupSubstring = blockGroupPath.length() + 1;
            Map<ResourceLocation, List<Resource>> files = manager.listResourceStacks(blockGroupPath, x -> x.getNamespace().equals(AmbientSounds.MODID));
            for (Entry<ResourceLocation, List<Resource>> file : files.entrySet()) {
                AmbientBlockGroup group = new AmbientBlockGroup();
                String blockGroupName = file.getKey().getPath().substring(blockGroupSubstring).replace(".json", "");
                for (Resource resource : file.getValue()) {
                    InputStream input = resource.open();
                    try {
                        try {
                            group.add(GSON.fromJson(JsonParser.parseString(IOUtils.toString(input, Charsets.UTF_8)), String[].class));
                        } catch (JsonSyntaxException e) {
                            AmbientSounds.LOGGER.error("Failed to load blockgroup " + file.getKey().toString() + " " + resource.sourcePackId(), e);
                        }
                    } finally {
                        input.close();
                    }
                }
                engine.blockGroups.put(blockGroupName, group);
            }
            
            engine.soundCollections = loadMultiple(manager, ResourceLocation.tryBuild(AmbientSounds.MODID, name + "/" + SOUNDCOLLECTIONS_LOCATION), AmbientSoundCollection.class,
                x -> x.stack, (soundGroup, soundGroupName, json) -> {});
            
            engine.soundCategories = loadMultiple(manager, ResourceLocation.tryBuild(AmbientSounds.MODID, name + "/" + SOUNDCATEGORIES_LOCATION), AmbientSoundCategory.class,
                x -> x.stack, (soundCategory, soundCategoryName, json) -> soundCategory.name = soundCategoryName);
            
            engine.features = loadMultiple(manager, ResourceLocation.tryBuild(AmbientSounds.MODID, name + "/" + FEATURES_LOCATION), AmbientFeature.class, x -> x.stack, (feature,
                    featureName, json) -> feature.name = featureName);
            
            engine.silentDim = new AmbientDimension();
            engine.silentDim.name = "silent";
            engine.silentDim.volumeSetting = 0;
            engine.silentDim.mute = true;
            
            engine.init();
            
            engine.soundEngine = soundEngine;
            
            AmbientSounds.LOGGER.info(
                "Loaded AmbientEngine '{}' v{}. {} dimension(s), {} features, {} blockgroups, {} sound collections, {} regions, {} sounds, {} sound categories, {} solids and {} biome types",
                engine.name, engine.version, engine.dimensions.size(), engine.features.size(), engine.blockGroups.size(), engine.soundCollections.size(), engine.allRegions.size(),
                engine.allSounds.size(), engine.soundCategories.size(), engine.solids.length, engine.biomeTypes.length);
            return engine;
        } finally {
            engineInput.close();
        }
    }
    
    public static <T> void applyStackType(T base, T newBase, AmbientStackType stack) {
        for (Field field : base.getClass().getFields()) {
            if (Modifier.isTransient(field.getModifiers()) || Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers()))
                continue;
            
            try {
                stack.apply(base, field, newBase);
            } catch (IllegalArgumentException | IllegalAccessException e) {}
        }
    }
    
    public static <T> LinkedHashMap<String, T> loadMultiple(ResourceManager manager, ResourceLocation path, Class<T> clazz, Function<T, AmbientStackType> type,
            AmbientLoader<T> setNameAndInit) throws IOException {
        LinkedHashMap<String, T> map = new LinkedHashMap<>();
        int substring = path.getPath().length() + 1;
        Map<ResourceLocation, List<Resource>> files = manager.listResourceStacks(path.getPath(), x -> x.getNamespace().equals(path.getNamespace()));
        for (Entry<ResourceLocation, List<Resource>> file : files.entrySet()) {
            T base = null;
            String name = file.getKey().getPath().substring(substring).replace(".json", "");
            for (Resource resource : file.getValue()) {
                InputStream input = resource.open();
                try {
                    try {
                        JsonElement json = JsonParser.parseString(IOUtils.toString(input, Charsets.UTF_8));
                        T newBase = GSON.fromJson(json, clazz);
                        if (base == null)
                            base = newBase;
                        else {
                            AmbientStackType stack = type.apply(newBase);
                            if (stack == AmbientStackType.overwrite)
                                base = newBase;
                            else
                                applyStackType(base, newBase, stack);
                        }
                        setNameAndInit.setNameAndLoad(base, name, json);
                        map.put(name, base);
                    } catch (AmbientEngineLoadException | JsonSyntaxException e) {
                        AmbientSounds.LOGGER.error("Failed to load " + clazz.getSimpleName() + " in " + file.getKey().toString() + " " + resource.sourcePackId(), e);
                    }
                } finally {
                    input.close();
                }
            }
        }
        return map;
    }
    
    public static synchronized AmbientEngine loadAmbientEngine(AmbientSoundEngine soundEngine) {
        try {
            ResourceManager manager = Minecraft.getInstance().getResourceManager();
            
            InputStream input = manager.getResource(CONFIG_LOCATION).orElseThrow().open();
            try {
                AmbientEngineConfig config = GSON.fromJson(JsonParser.parseString(IOUtils.toString(input, Charsets.UTF_8)).getAsJsonObject(), AmbientEngineConfig.class);
                
                AmbientSounds.CONFIG.engines.updateArray(config.engines, config.defaultEngine);
                
                try {
                    loadedEngine = AmbientSounds.CONFIG.engines.get();
                    return attemptToLoadEngine(soundEngine, manager, AmbientSounds.CONFIG.engines.get());
                } catch (Exception e) {
                    AmbientSounds.LOGGER.error("Sound engine {} could not be loaded", AmbientSounds.CONFIG.engines.get(), e);
                }
            } finally {
                input.close();
            }
            
            throw new Exception();
        } catch (Exception e) {
            AmbientSounds.LOGGER.error("Not sound engine could be loaded, no sounds will be played!");
        }
        
        return null;
    }
    
    protected transient LinkedHashMap<String, AmbientDimension> dimensions;
    
    protected transient LinkedHashMap<String, AmbientRegion> allRegions = new LinkedHashMap<>();
    protected transient LinkedHashMap<String, AmbientRegion> generalRegions;
    protected transient List<AmbientRegion> activeRegions = new ArrayList<>();
    
    protected transient LinkedHashMap<String, AmbientSound> allSounds = new LinkedHashMap<>();
    protected transient LinkedHashMap<String, AmbientSoundCategory> soundCategories;
    protected transient List<AmbientSoundCategory> sortedSoundCategories;
    
    public transient LinkedHashMap<String, AmbientBlockGroup> blockGroups;
    public transient LinkedHashMap<String, AmbientSoundCollection> soundCollections;
    public transient LinkedHashMap<String, AmbientFeature> features;
    
    public transient AmbientSoundEngine soundEngine;
    
    protected transient List<String> silentDimensions = new ArrayList<>();
    protected transient AmbientDimension silentDim;
    
    protected transient List<Double> airPocketDistanceFactor;
    public transient int maxAirPocketCount;
    
    public transient AmbientBlockGroup considerSolid;
    public transient double squaredBiomeDistance;
    
    public AmbientRegion getRegion(String name) {
        return allRegions.get(name);
    }
    
    public String name;
    
    public String version;
    
    @SerializedName("environment-tick-time")
    public int environmentTickTime = 40;
    @SerializedName("sound-tick-time")
    public int soundTickTime = 4;
    @SerializedName("block-scan-distance")
    public int blockScanDistance = 40;
    
    @SerializedName("average-height-scan-distance")
    public int averageHeightScanDistance = 2;
    @SerializedName("average-height-scan-count")
    public int averageHeightScanCount = 5;
    
    @SerializedName("biome-scan-distance")
    public int biomeScanDistance = 5;
    @SerializedName("biome-scan-count")
    public int biomeScanCount = 3;
    
    @SerializedName("air-pocket-count")
    public int airPocketCount = 50000;
    @SerializedName("air-pocket-groups")
    public AirPocketGroup[] airPocketGroups = new AirPocketGroup[0];
    
    @SerializedName("air-distance")
    public int airDistance = 25;
    @SerializedName("air-sky-distance")
    public int airSkyDistance = 13;
    @SerializedName("air-sky-weight")
    public double airSkyWeight = 50;
    @SerializedName("air-min")
    public double airMin = 0.1;
    @SerializedName("air-max")
    public double airMax = 0.5;
    
    @SerializedName("sky-distance")
    public double skyDistance = 20;
    @SerializedName("sky-min-count")
    public int skyMinCount = 10;
    @SerializedName("sky-max-count")
    public int skyMaxCount = 100;
    
    public String[] solids = {};
    
    @SerializedName("biome-types")
    public String[] biomeTypes = {};
    @SerializedName("default-biome-type")
    public String defaultBiomeType;
    
    @SerializedName("fade-volume")
    public Double fadeVolume = 0.005D;
    
    @SerializedName("fade-pitch")
    public Double fadePitch = 0.005D;
    
    protected boolean checkRegion(AmbientDimension dimension, int i, AmbientRegion region) {
        if (region.name == null || region.name.isEmpty()) {
            if (dimension == null)
                AmbientSounds.LOGGER.error("Found invalid region at {}", i);
            else
                AmbientSounds.LOGGER.error("Found invalid region in '{}' at {}", dimension.name, i);
            return false;
        }
        return true;
    }
    
    protected void addRegion(AmbientRegion region) {
        allRegions.put((region.dimension != null ? region.dimension.name + "." : "") + region.name, region);
        region.volumeSetting = 1;
        
        String prefix = (region.dimension != null ? region.dimension.name + "." : "") + region.name + ".";
        if (region.sounds != null) {
            for (AmbientSound sound : region.loadedSounds.values()) {
                allSounds.put(prefix + sound.name, sound);
                sound.fullName = prefix + sound.name;
                sound.volumeSetting = 1;
            }
        }
    }
    
    public AmbientDimension getDimension(Level level) {
        String dimensionTypeName = level.dimension().location().toString();
        for (int i = 0; i < silentDimensions.size(); i++)
            if (dimensionTypeName.matches(".*" + silentDimensions.get(i).toLowerCase().replace("*", ".*").replace("?", "\\?") + ".*"))
                return silentDim;
            
        for (AmbientDimension dimension : dimensions.values())
            if (dimension.is(level))
                return dimension;
            
        return silentDim;
    }
    
    public void stopEngine() {
        if (!activeRegions.isEmpty()) {
            for (AmbientRegion region : activeRegions)
                region.deactivate();
            activeRegions.clear();
        }
    }
    
    public void consumeSoundCollections(String[] groups, Consumer<AmbientSound> consumer) {
        for (int i = 0; i < groups.length; i++) {
            AmbientSoundCollection group = soundCollections.get(groups[i]);
            if (group == null || group.sounds == null)
                continue;
            for (AmbientSound sound : group.sounds)
                consumer.accept(sound);
        }
    }
    
    public int airPocketVolume(int r) {
        int res = 0;
        for (int i = r; r > 0; r--) {
            int f;
            if (i == r)
                f = 1;
            else if (i == r - 1)
                f = 4;
            else if (i == r - 2)
                f = 7;
            else
                f = 8;
            res += (f * r * (r + 1) * 0.5);
        }
        return res;
    }
    
    public void init() throws AmbientEngineLoadException {
        airPocketDistanceFactor = new ArrayList<>();
        for (int i = 0; i < airPocketGroups.length; i++)
            for (int subDistance = 0; subDistance < airPocketGroups[i].distance; subDistance++)
                airPocketDistanceFactor.add(airPocketGroups[i].weight);
            
        maxAirPocketCount = airPocketVolume(airDistance);
        
        for (Entry<String, AmbientSoundCollection> group : soundCollections.entrySet())
            if (group.getValue().sounds != null)
                for (AmbientSound sound : group.getValue().sounds) {
                    sound.name = group.getKey() + "." + sound.name;
                    allSounds.put(sound.name, sound);
                    sound.init(this);
                }
            
        for (AmbientDimension dimension : dimensions.values())
            dimension.init(this);
        
        for (AmbientRegion region : allRegions.values())
            region.init(this);
        
        for (AmbientSoundCategory cat : soundCategories.values())
            cat.init(this);
        
        // Sort categories into new list which is sorted in a way later on calculations will be a lot easier. Furthermore circular references will result in an error
        sortedSoundCategories = new ArrayList<>();
        HashSet<String> unsorted = new HashSet<>(soundCategories.keySet());
        for (AmbientSoundCategory cat : soundCategories.values()) {
            if (cat.parent == null || cat.parent.isBlank()) {
                cat.postInit(unsorted);
                sortedSoundCategories.add(cat);
            } else if (cat.parentCategory == null)
                AmbientSounds.LOGGER.error("Could not parse {} sound category, because the parent '{}' does not exist.", cat.name, cat.parent);
            
            if (unsorted.isEmpty())
                break;
        }
        
        if (!unsorted.isEmpty()) // Check for unresolved categories, which are an independent circle and will be ignored.
            AmbientSounds.LOGGER.error("Could not resolve all sound categories. {} sound categories will be ignored {}.", unsorted.size(), unsorted);
        
        considerSolid = new AmbientBlockGroup();
        if (solids != null)
            considerSolid.add(solids);
        
        squaredBiomeDistance = Math.pow(biomeScanCount * biomeScanDistance * 2, 2); // It is actually twice the distance, so the farthest away biome still has half of the volume
        
        onClientLoad();
    }
    
    public void onClientLoad() {
        blockGroups.values().forEach(x -> x.onClientLoad());
        considerSolid.onClientLoad();
    }
    
    public double airWeightFactor(int distance) {
        if (distance >= airPocketDistanceFactor.size())
            return 0;
        return airPocketDistanceFactor.get(distance);
    }
    
    public void tick(AmbientEnvironment env) {
        for (AmbientSoundCategory cat : sortedSoundCategories)
            cat.tick(env, null);
        
        if (env.dimension.loadedRegions != null)
            for (AmbientRegion region : env.dimension.loadedRegions.values()) {
                if (region.tick(env)) {
                    if (!region.isActive()) {
                        region.activate();
                        activeRegions.add(region);
                    }
                } else if (region.isActive()) {
                    region.deactivate();
                    activeRegions.remove(region);
                }
            }
        
        for (AmbientRegion region : generalRegions.values()) {
            if (region.tick(env)) {
                if (!region.isActive()) {
                    region.activate();
                    activeRegions.add(region);
                }
            } else if (region.isActive()) {
                region.deactivate();
                activeRegions.remove(region);
            }
        }
        
    }
    
    public void fastTick(AmbientEnvironment env) {
        soundEngine.tick(env);
        
        if (!activeRegions.isEmpty()) {
            for (Iterator<AmbientRegion> iterator = activeRegions.iterator(); iterator.hasNext();) {
                AmbientRegion region = iterator.next();
                if (!region.fastTick(env)) {
                    region.deactivate();
                    iterator.remove();
                }
            }
        }
    }
    
    public void changeDimension(AmbientEnvironment env, AmbientDimension newDimension) {
        if (env.dimension == null || env.dimension.loadedRegions == null)
            return;
        
        for (AmbientRegion region : env.dimension.loadedRegions.values()) {
            if (region.isActive()) {
                region.deactivate();
                activeRegions.remove(region);
            }
        }
    }
    
    public void collectDetails(DebugTextRenderer text) {
        text.text(name + " v" + version);
    }
    
    public AmbientSoundCategory getSoundCategory(String name) {
        return soundCategories.get(name);
    }
    
}
