package team.creative.ambientsounds;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import team.creative.ambientsounds.AmbientEnviroment.BiomeArea;
import team.creative.ambientsounds.AmbientEnviroment.TerrainHeight;

public class AmbientEngine {
    
    public static final ResourceLocation CONFIG_LOCATION = new ResourceLocation(AmbientSounds.MODID, "config.json");
    public static final String ENGINE_LOCATION = "engine.json";
    public static final String DIMENSIONS_LOCATION = "dimensions.json";
    public static final String REGIONS_LOCATION = "regions.json";
    public static final String SOUNDS_LOCATION = "sounds.json";
    
    private static final JsonParser parser = new JsonParser();
    private static final Gson gson = new GsonBuilder().registerTypeAdapter(ResourceLocation.class, new JsonDeserializer<ResourceLocation>() {
        
        @Override
        public ResourceLocation deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString())
                return new ResourceLocation(json.getAsString());
            return null;
        }
    }).create();
    
    public static AmbientEngine attemptToLoadEngine(AmbientSoundEngine soundEngine, ResourceManager manager, String name) throws Exception {
        AmbientEngine engine = gson.fromJson(parser
                .parse(IOUtils.toString(manager.getResource(new ResourceLocation(AmbientSounds.MODID, name + "/" + ENGINE_LOCATION)).getInputStream(), Charsets.UTF_8))
                .getAsJsonObject(), AmbientEngine.class);
        
        if (!engine.name.equals(name))
            throw new Exception("Invalid engine name");
        
        for (Resource resource : manager.getResources(new ResourceLocation(AmbientSounds.MODID, name + "/" + DIMENSIONS_LOCATION))) {
            AmbientDimension[] dimensions = gson.fromJson(parser.parse(IOUtils.toString(resource.getInputStream(), Charsets.UTF_8)), AmbientDimension[].class);
            for (int i = 0; i < dimensions.length; i++) {
                AmbientDimension dimension = dimensions[i];
                if (dimension.name == null || dimension.name.isEmpty())
                    AmbientSounds.LOGGER.error("Found invalid dimensions at {}", i);
                engine.dimensions.put(dimension.name, dimension);
                dimension.load(engine, gson, parser, manager);
                for (AmbientRegion region : dimension.regions.values())
                    if (engine.checkRegion(dimension, i, region))
                        engine.addRegion(region);
            }
        }
        
        for (Resource resource : manager.getResources(new ResourceLocation(AmbientSounds.MODID, name + "/" + REGIONS_LOCATION))) {
            AmbientRegion[] regions = gson.fromJson(parser.parse(IOUtils.toString(resource.getInputStream(), Charsets.UTF_8)), AmbientRegion[].class);
            for (int i = 0; i < regions.length; i++) {
                AmbientRegion region = regions[i];
                if (engine.checkRegion(null, i, region)) {
                    engine.generalRegions.put(region.name, region);
                    region.load(engine, gson, parser, manager);
                    engine.addRegion(region);
                }
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
            
            AmbientConfig config = gson
                    .fromJson(parser.parse(IOUtils.toString(manager.getResource(CONFIG_LOCATION).getInputStream(), Charsets.UTF_8)).getAsJsonObject(), AmbientConfig.class);
            
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
    
    public void init() {
        for (AmbientDimension dimension : dimensions.values())
            dimension.init(this);
        
        for (AmbientRegion region : allRegions.values())
            region.init(this);
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
    
    public TerrainHeight calculateAverageHeight(Level level, Player player) {
        int sum = 0;
        int count = 0;
        
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        
        MutableBlockPos pos = new MutableBlockPos();
        BlockPos center = player.blockPosition();
        
        for (int x = -averageHeightScanCount; x <= averageHeightScanCount; x++) {
            for (int z = -averageHeightScanCount; z <= averageHeightScanCount; z++) {
                
                pos.set(center.getX() + averageHeightScanDistance * x, center.getY(), center.getZ() + averageHeightScanDistance * z);
                int height = getHeightBlock(level, pos);
                
                min = Math.min(height, min);
                max = Math.max(height, max);
                sum += height;
                count++;
            }
        }
        return new TerrainHeight((double) sum / count, min, max);
    }
    
    public LinkedHashMap<BiomeArea, Float> calculateBiomes(Level level, Player player, double volume) {
        LinkedHashMap<BiomeArea, Float> biomes = new LinkedHashMap<>();
        if (volume > 0.0) {
            
            int posX = (int) player.getX();
            int posZ = (int) player.getZ();
            BlockPos center = new BlockPos(posX, 0, posZ);
            MutableBlockPos pos = new MutableBlockPos();
            for (int x = -biomeScanCount; x <= biomeScanCount; x++) {
                for (int z = -biomeScanCount; z <= biomeScanCount; z++) {
                    pos.set(posX + x * biomeScanDistance, 0, posZ + z * biomeScanDistance);
                    Biome biome = level.getBiome(pos);
                    
                    float biomeVolume = (float) ((1 - Math.sqrt(center.distSqr(pos)) / (biomeScanCount * biomeScanDistance * 2)) * volume);
                    BiomeArea area = new BiomeArea(biome, pos);
                    Float before = biomes.get(area);
                    if (before == null)
                        before = 0F;
                    biomes.put(area, Math.max(before, biomeVolume));
                }
            }
            
            List<Entry<BiomeArea, Float>> entries = new ArrayList<>(biomes.entrySet());
            Collections.sort(entries, new Comparator<Entry<BiomeArea, Float>>() {
                @Override
                public int compare(Entry<BiomeArea, Float> o1, Entry<BiomeArea, Float> o2) {
                    return o1.getValue().compareTo(o2.getValue());
                }
            });
            biomes = new LinkedHashMap<>();
            for (Map.Entry<BiomeArea, Float> entry : entries)
                biomes.put(entry.getKey(), entry.getValue());
        }
        return biomes;
    }
    
    public static int getHeightBlock(Level world, MutableBlockPos pos) {
        int y;
        int heighest = 2;
        
        for (y = 45; y < 256; ++y) {
            pos.setY(y);
            BlockState state = world.getBlockState(pos);
            if ((state.isSolidRender(world, pos) && !(state.getBlock() instanceof LeavesBlock)) || state.getBlock() == Blocks.WATER)
                heighest = y;
        }
        
        return heighest;
    }
    
}
