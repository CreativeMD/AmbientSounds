package team.creative.ambientsounds.dimension;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.Level;
import team.creative.ambientsounds.condition.AmbientCondition;
import team.creative.ambientsounds.condition.AmbientSelection;
import team.creative.ambientsounds.condition.AmbientVolume;
import team.creative.ambientsounds.engine.AmbientEngine;
import team.creative.ambientsounds.engine.AmbientEngineLoadException;
import team.creative.ambientsounds.engine.AmbientStackType;
import team.creative.ambientsounds.environment.AmbientEnvironment;
import team.creative.ambientsounds.region.AmbientRegion;
import team.creative.creativecore.common.config.api.CreativeConfig;

public class AmbientDimension {
    
    @CreativeConfig.DecimalRange(min = 0, max = 1)
    public transient double volumeSetting = 1;
    public transient HashMap<String, AmbientRegion> loadedRegions;
    
    public transient HashMap<String, AmbientCondition> biomeTypeSelectors = new HashMap<>();
    
    public transient String name;
    
    public boolean mute = false;
    
    @SerializedName("biome-selector")
    public AmbientCondition biomeSelector;
    
    public Boolean night;
    public Boolean rain;
    public Boolean storm;
    
    @SerializedName("dimension-names")
    public String[] dimensionNames;
    
    @SerializedName("bad-dimension-names")
    public String[] badDimensionNames;
    
    @SerializedName("average-height")
    public Integer averageHeight;
    
    public AmbientRegion[] regions;
    
    public AmbientStackType stack = AmbientStackType.overwrite;
    
    public void load(AmbientEngine engine, Gson gson, ResourceManager manager, JsonElement element) throws AmbientEngineLoadException {
        if (regions != null) {
            loadedRegions = new LinkedHashMap<>();
            for (int i = 0; i < regions.length; i++) {
                AmbientRegion region = regions[i];
                region.dimension = this;
                loadedRegions.put(region.name, region);
                region.load(engine, gson, manager);
            }
        }
        
        if (element.isJsonObject())
            for (String type : engine.biomeTypes) {
                JsonElement selector = element.getAsJsonObject().get(type + "-selector");
                if (selector != null)
                    biomeTypeSelectors.put(type, gson.fromJson(selector, AmbientCondition.class));
            }
    }
    
    public void init(AmbientEngine engine) throws AmbientEngineLoadException {
        if (biomeSelector != null)
            biomeSelector.init(engine);
        
        for (AmbientCondition condition : biomeTypeSelectors.values())
            condition.init(engine);
        
        if (badDimensionNames != null)
            for (int i = 0; i < badDimensionNames.length; i++)
                badDimensionNames[i] = ".*" + badDimensionNames[i].toLowerCase().replace("*", ".*").replace("?", "\\?") + ".*";
            
        if (dimensionNames != null)
            for (int i = 0; i < dimensionNames.length; i++)
                dimensionNames[i] = ".*" + dimensionNames[i].toLowerCase().replace("*", ".*").replace("?", "\\?") + ".*";
    }
    
    public boolean is(Level level) {
        String dimensionTypeName = level.dimension().identifier().toString();
        
        if (badDimensionNames != null)
            for (int j = 0; j < badDimensionNames.length; j++)
                if (dimensionTypeName.matches(badDimensionNames[j]))
                    return false;
                
        if (dimensionNames != null)
            for (int j = 0; j < dimensionNames.length; j++)
                if (dimensionTypeName.matches(dimensionNames[j]))
                    return true;
                
        return dimensionNames == null;
    }
    
    public void manipulateEnviroment(AmbientEnvironment env) {
        env.muted = mute;
        
        if (night != null)
            env.night = night;
        
        if (rain != null)
            env.raining = rain;
        
        if (storm != null)
            env.thundering = storm;
        
        if (biomeSelector != null) {
            AmbientSelection selection = biomeSelector.value(env);
            if (selection != null)
                env.biomeVolume = selection;
            else
                env.biomeVolume = AmbientVolume.SILENT;
        }
        
        env.biomeTypeVolumes.clear();
        for (Entry<String, AmbientCondition> entry : biomeTypeSelectors.entrySet()) {
            AmbientSelection selection = entry.getValue().value(env);
            if (selection != null)
                env.biomeTypeVolumes.put(entry.getKey(), selection);
            else
                env.biomeTypeVolumes.put(entry.getKey(), AmbientVolume.SILENT);
        }
    }
    
    @Override
    public String toString() {
        return name;
    }
    
}
