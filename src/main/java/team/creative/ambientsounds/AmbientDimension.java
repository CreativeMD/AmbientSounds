package team.creative.ambientsounds;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.Level;
import team.creative.ambientsounds.env.AmbientEnvironment;
import team.creative.creativecore.common.config.api.CreativeConfig;

public class AmbientDimension {
    
    @CreativeConfig.DecimalRange(min = 0, max = 1)
    public transient double volumeSetting = 1;
    public transient HashMap<String, AmbientRegion> regions;
    
    public transient HashMap<String, AmbientCondition> biomeTypeSelectors = new HashMap<>();
    
    public String name;
    
    public boolean mute = false;
    
    @SerializedName("biome-selector")
    public AmbientCondition biomeSelector;
    
    public Boolean night;
    public Boolean rain;
    public Boolean storm;
    
    @SerializedName(value = "dimension-names")
    public String[] dimensionNames;
    
    @SerializedName(value = "bad-dimension-names")
    public String[] badDimensionNames;
    
    @SerializedName(value = "average-height")
    public Integer averageHeight;
    
    public void load(AmbientEngine engine, Gson gson, ResourceManager manager, JsonObject object) throws IOException {
        regions = new HashMap<>();
        for (Resource resource : manager.getResourceStack(new ResourceLocation(AmbientSounds.MODID, engine.name + "/dimension_regions/" + name + ".json"))) {
            InputStream input = resource.open();
            try {
                AmbientRegion[] regions = gson.fromJson(JsonParser.parseString(IOUtils.toString(input, Charsets.UTF_8)), AmbientRegion[].class);
                for (int j = 0; j < regions.length; j++) {
                    AmbientRegion region = regions[j];
                    region.dimension = this;
                    this.regions.put(region.name, region);
                    region.load(engine, gson, manager);
                }
            } finally {
                input.close();
            }
        }
        
        for (String type : engine.biomeTypes) {
            JsonElement element = object.get(type + "-selector");
            if (element != null)
                biomeTypeSelectors.put(type, gson.fromJson(element, AmbientCondition.class));
        }
    }
    
    public void init(AmbientEngine engine) {
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
        String dimensionTypeName = level.dimension().location().toString();
        
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
