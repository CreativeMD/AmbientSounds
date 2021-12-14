package team.creative.ambientsounds;

import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.Level;
import team.creative.ambientsounds.env.AmbientEnviroment;
import team.creative.ambientsounds.env.feature.AmbientFeature;
import team.creative.ambientsounds.env.pocket.AirPocketGroup;
import team.creative.ambientsounds.sound.AmbientSoundEngine;
import team.creative.creativecore.common.util.type.list.Pair;

public class AmbientEngine {
    
    public static final ResourceLocation CONFIG_LOCATION = new ResourceLocation(AmbientSounds.MODID, "config.json");
    public static final String ENGINE_LOCATION = "engine.json";
    public static final String DIMENSIONS_LOCATION = "dimensions.json";
    public static final String REGIONS_LOCATION = "regions.json";
    public static final String SOUNDS_LOCATION = "sounds.json";
    public static final String FEATURES_LOCATION = "features.json";
    
    private static final Gson gson = new GsonBuilder().registerTypeAdapter(ResourceLocation.class, new JsonDeserializer<ResourceLocation>() {
        
        @Override
        public ResourceLocation deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString())
                return new ResourceLocation(json.getAsString());
            return null;
        }
    }).create();
    
    public static AmbientEngine attemptToLoadEngine(AmbientSoundEngine soundEngine, ResourceManager manager, String name) throws Exception {
        AmbientEngine engine = gson.fromJson(JsonParser
                .parseString(IOUtils.toString(manager.getResource(new ResourceLocation(AmbientSounds.MODID, name + "/" + ENGINE_LOCATION)).getInputStream(), Charsets.UTF_8))
                .getAsJsonObject(), AmbientEngine.class);
        
        if (!engine.name.equals(name))
            throw new Exception("Invalid engine name");
        
        for (Resource resource : manager.getResources(new ResourceLocation(AmbientSounds.MODID, name + "/" + DIMENSIONS_LOCATION))) {
            AmbientDimension[] dimensions = gson.fromJson(JsonParser.parseString(IOUtils.toString(resource.getInputStream(), Charsets.UTF_8)), AmbientDimension[].class);
            for (int i = 0; i < dimensions.length; i++) {
                AmbientDimension dimension = dimensions[i];
                if (dimension.name == null || dimension.name.isEmpty())
                    AmbientSounds.LOGGER.error("Found invalid dimensions at {}", i);
                engine.dimensions.put(dimension.name, dimension);
                dimension.load(engine, gson, manager);
                for (AmbientRegion region : dimension.regions.values())
                    if (engine.checkRegion(dimension, i, region))
                        engine.addRegion(region);
            }
        }
        
        for (Resource resource : manager.getResources(new ResourceLocation(AmbientSounds.MODID, name + "/" + REGIONS_LOCATION))) {
            try {
                AmbientRegion[] regions = gson.fromJson(JsonParser.parseString(IOUtils.toString(resource.getInputStream(), Charsets.UTF_8)), AmbientRegion[].class);
                for (int i = 0; i < regions.length; i++) {
                    AmbientRegion region = regions[i];
                    if (engine.checkRegion(null, i, region)) {
                        engine.generalRegions.put(region.name, region);
                        region.load(engine, gson, manager);
                        engine.addRegion(region);
                    }
                }
            } catch (JsonSyntaxException e) {
                System.out.println("Failed to load  " + resource.getLocation());
                e.printStackTrace();
            }
        }
        
        engine.features = new ArrayList<>();
        for (Resource resource : manager.getResources(new ResourceLocation(AmbientSounds.MODID, name + "/" + FEATURES_LOCATION))) {
            AmbientFeature[] features = gson.fromJson(JsonParser.parseString(IOUtils.toString(resource.getInputStream(), Charsets.UTF_8)), AmbientFeature[].class);
            for (int i = 0; i < features.length; i++) {
                AmbientFeature feature = features[i];
                for (Resource scanResource : manager.getResources(new ResourceLocation(AmbientSounds.MODID, name + "/features/" + feature.name + ".json"))) {
                    try {
                        feature.blocks.add(gson.fromJson(JsonParser.parseString(IOUtils.toString(scanResource.getInputStream(), Charsets.UTF_8)), String[].class));
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    for (Resource scanResource : manager.getResources(new ResourceLocation(AmbientSounds.MODID, name + "/features/bad-" + feature.name + ".json"))) {
                        try {
                            feature.badBlocks.add(gson.fromJson(JsonParser.parseString(IOUtils.toString(scanResource.getInputStream(), Charsets.UTF_8)), String[].class));
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (FileNotFoundException e) {}
                engine.features.add(feature);
            }
        }
        
        engine.silentDim = new AmbientDimension();
        engine.silentDim.name = "silent";
        engine.silentDim.volumeSetting = 0;
        engine.silentDim.mute = true;
        
        engine.init();
        
        engine.soundEngine = soundEngine;
        
        AmbientSounds.LOGGER.info("Loaded AmbientEngine '{}' v{}. {} dimension(s), {} region(s) and {} sound(s)", engine.name, engine.version, engine.dimensions
                .size(), engine.allRegions.size(), engine.sounds.size());
        
        return engine;
    }
    
    public static AmbientEngine loadAmbientEngine(AmbientSoundEngine soundEngine) {
        try {
            ResourceManager manager = Minecraft.getInstance().getResourceManager();
            
            AmbientConfig config = gson.fromJson(JsonParser.parseString(IOUtils.toString(manager.getResource(CONFIG_LOCATION).getInputStream(), Charsets.UTF_8))
                    .getAsJsonObject(), AmbientConfig.class);
            
            if (!AmbientSounds.CONFIG.engine.equalsIgnoreCase("default"))
                try {
                    return attemptToLoadEngine(soundEngine, manager, AmbientSounds.CONFIG.engine);
                } catch (Exception e) {
                    AmbientSounds.LOGGER.error("Sound engine {} could not be loaded", AmbientSounds.CONFIG.engine);
                    e.printStackTrace();
                }
            
            try {
                return attemptToLoadEngine(soundEngine, manager, config.defaultEngine);
            } catch (Exception e) {
                AmbientSounds.LOGGER.error("Sound engine {} could not be loaded", AmbientSounds.CONFIG.engine);
                e.printStackTrace();
            }
            
            throw new Exception();
        } catch (Exception e) {
            AmbientSounds.LOGGER.error("Not sound engine could be loaded, no sounds will be played!");
        }
        
        return null;
    }
    
    protected transient LinkedHashMap<String, AmbientDimension> dimensions = new LinkedHashMap<>();
    
    protected transient LinkedHashMap<String, AmbientRegion> allRegions = new LinkedHashMap<>();
    protected transient LinkedHashMap<String, AmbientRegion> generalRegions = new LinkedHashMap<>();
    protected transient List<AmbientRegion> activeRegions = new ArrayList<>();
    
    protected transient LinkedHashMap<String, AmbientSound> sounds = new LinkedHashMap<>();
    
    protected transient List<String> silentDimensions = new ArrayList<>();
    
    protected transient AmbientSoundEngine soundEngine;
    
    protected transient AmbientDimension silentDim;
    
    public transient List<AmbientFeature> features;
    protected transient double[] airPocketDistanceFactor;
    public transient int maxAirPocketCount;
    
    public AmbientRegion getRegion(String name) {
        return allRegions.get(name);
    }
    
    public String name;
    
    public String version;
    
    @SerializedName(value = "enviroment-tick-time")
    public int enviromentTickTime = 40;
    @SerializedName(value = "sound-tick-time")
    public int soundTickTime = 4;
    @SerializedName(value = "block-scan-distance")
    public int blockScanDistance = 40;
    
    @SerializedName(value = "outside-distance-min")
    public int outsideDistanceMin = 2;
    @SerializedName(value = "outside-distance-max")
    public int outsideDistanceMax = 13;
    
    @SerializedName(value = "average-height-scan-distance")
    public int averageHeightScanDistance = 2;
    @SerializedName(value = "average-height-scan-count")
    public int averageHeightScanCount = 5;
    
    @SerializedName(value = "biome-scan-distance")
    public int biomeScanDistance = 5;
    @SerializedName(value = "biome-scan-count")
    public int biomeScanCount = 3;
    
    @SerializedName(value = "air-pocket-count")
    public int airPocketCount = 50000;
    @SerializedName(value = "air-pocket-distance")
    public int airPocketDistance = 25;
    @SerializedName(value = "air-pocket-groups")
    public AirPocketGroup[] airPocketGroups = new AirPocketGroup[0];
    
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
        allRegions.put(region.name, region);
        region.volumeSetting = 1;
        
        String prefix = (region.dimension != null ? region.dimension.name + "." : "") + region.name + ".";
        if (region.sounds != null) {
            for (AmbientSound sound : region.sounds.values()) {
                sounds.put(prefix + sound.name, sound);
                sound.fullName = prefix + sound.name;
                sound.volumeSetting = 1;
            }
        }
    }
    
    public AmbientDimension getDimension(Level level) {
        String dimensionTypeName = level.dimension().location().toString();
        if (silentDimensions.contains(dimensionTypeName))
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
    
    public void init() {
        airPocketDistanceFactor = new double[airPocketDistance + 1];
        int index = 0;
        int subDistance = 0;
        for (int distance = 0; distance < airPocketDistanceFactor.length; distance++) {
            if (index < airPocketGroups.length) {
                if (subDistance <= airPocketGroups[index].distance) {
                    airPocketDistanceFactor[distance] = airPocketGroups[index].weight;
                    subDistance++;
                    continue;
                } else {
                    subDistance = 0;
                    index++;
                    if (index < airPocketGroups.length) {
                        airPocketDistanceFactor[distance] = airPocketGroups[index].weight;
                        continue;
                    }
                }
            }
            airPocketDistanceFactor[distance] = 1;
        }
        
        maxAirPocketCount = airPocketVolume(airPocketDistance);
        
        for (AmbientDimension dimension : dimensions.values())
            dimension.init(this);
        
        for (AmbientRegion region : allRegions.values())
            region.init(this);
        
        onClientLoad();
    }
    
    public void onClientLoad() {
        features.forEach(x -> x.onClientLoad());
    }
    
    public double airWeightFactor(int distance) {
        if (distance >= airPocketDistanceFactor.length)
            return 1;
        return airPocketDistanceFactor[distance];
    }
    
    public void tick(AmbientEnviroment env) {
        if (env.dimension.regions != null)
            for (AmbientRegion region : env.dimension.regions.values()) {
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
    
    public void fastTick(AmbientEnviroment env) {
        soundEngine.tick();
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
    
    public void changeDimension(AmbientEnviroment env, AmbientDimension newDimension) {
        if (env.dimension == null || env.dimension.regions == null)
            return;
        
        for (AmbientRegion region : env.dimension.regions.values()) {
            if (region.isActive()) {
                region.deactivate();
                activeRegions.remove(region);
            }
        }
    }
    
    public void collectDetails(List<Pair<String, Object>> details) {
        details.add(new Pair<>("", name + " v" + version));
    }
    
}
