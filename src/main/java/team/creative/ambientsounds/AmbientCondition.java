package team.creative.ambientsounds;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import net.minecraft.util.Mth;
import team.creative.ambientsounds.entity.AmbientEntityCondition;
import team.creative.ambientsounds.env.AmbientEnvironment;
import team.creative.ambientsounds.env.BiomeEnvironment.BiomeArea;
import team.creative.ambientsounds.env.BiomeEnvironment.BiomeStats;
import team.creative.creativecore.common.util.type.list.Pair;

public class AmbientCondition extends AmbientSoundProperties {
    
    public Boolean always;
    
    public double volume = 1.0;
    @SerializedName(value = "night")
    public double nightVolume = 1.0;
    @SerializedName(value = "day")
    public double dayVolume = 1.0;
    
    @SerializedName(value = "biome-type")
    public String biomeType;
    
    public String[] biomes;
    @SerializedName(value = "bad-biomes")
    public String[] badBiomes;
    
    public Boolean raining;
    @SerializedName(value = "overall-raining")
    public Boolean overallRaining;
    public Boolean snowing;
    public Boolean storming;
    
    public AmbientMinMaxFadeCondition underwater;
    
    @SerializedName(value = "relative-height")
    public AmbientMinMaxFadeSpecialCondition relativeHeight;
    @SerializedName(value = "absolute-height")
    public AmbientMinMaxFadeCondition absoluteHeight;
    @SerializedName(value = "min-height-relative")
    public AmbientMinMaxFadeCondition minHeightRelative;
    @SerializedName(value = "max-height-relative")
    public AmbientMinMaxFadeCondition maxHeightRelative;
    
    public AmbientMinMaxFadeCondition light;
    
    @SerializedName(value = "sky-light")
    public AmbientMinMaxFadeCondition skyLight;
    
    public AmbientMinMaxFadeCondition air;
    
    public AmbientMinMaxFadeCondition temperature;
    
    public String[] features;
    @SerializedName(value = "bad-features")
    public String[] badFeatures;
    
    public AmbientCondition[] variants;
    
    public String[] regions;
    transient List<AmbientRegion> regionList;
    
    @SerializedName(value = "bad-regions")
    public String[] badRegions;
    transient List<AmbientRegion> badRegionList;
    
    public AmbientEntityCondition entity;
    
    public String regionName() {
        return null;
    }
    
    @Override
    public void init(AmbientEngine engine) {
        super.init(engine);
        
        volume = Mth.clamp(volume, 0, 1);
        nightVolume = Mth.clamp(nightVolume, 0, 1);
        dayVolume = Mth.clamp(dayVolume, 0, 1);
        
        if (variants != null)
            for (int i = 0; i < variants.length; i++)
                variants[i].init(engine);
            
        if (regions != null) {
            regionList = new ArrayList<>();
            
            for (String regionName : regions) {
                AmbientRegion region = engine.getRegion(regionName);
                if (region != null && !regionName.equals(regionName()))
                    regionList.add(region);
            }
        }
        
        if (badRegions != null) {
            badRegionList = new ArrayList<>();
            
            for (String regionName : badRegions) {
                AmbientRegion region = engine.getRegion(regionName);
                if (region != null && !regionName.equals(regionName()))
                    badRegionList.add(region);
            }
        }
        
        if (biomeType == null)
            biomeType = engine.defaultBiomeType;
        
        if (entity != null)
            entity.init(engine);
    }
    
    public AmbientSelection value(AmbientEnvironment env) {
        
        if (env.muted)
            return null;
        
        if (always != null)
            return always ? new AmbientSelection(this) : null;
        
        if (volume <= 0)
            return null;
        
        if (env.night ? nightVolume <= 0 : dayVolume <= 0)
            return null;
        
        if (raining != null && raining != env.raining)
            return null;
        
        if (overallRaining != null && overallRaining != env.isRainAudibleAtSurface()) // excluded, volume is applied later
            return null;
        
        if (snowing != null && snowing != env.snowing)
            return null;
        
        if (storming != null && env.thundering != storming)
            return null;
        
        AmbientSelection selection = new AmbientSelection(this);
        
        selection.volume *= env.night ? nightVolume : dayVolume;
        
        if (badRegionList != null)
            for (AmbientRegion region : badRegionList)
                if (region.isActive())
                    return null;
                
        if (regionList != null) {
            Double highest = null;
            for (AmbientRegion region : regionList) {
                AmbientSelection subSelection = region.value(env);
                
                if (subSelection != null)
                    if (highest == null)
                        highest = subSelection.volume;
                    else
                        highest = Math.max(subSelection.volume, highest);
            }
            
            if (highest == null)
                return null;
            
            selection.volume *= highest;
        }
        
        if (biomes != null || badBiomes != null) {
            double highest = -1;
            
            for (Pair<BiomeArea, BiomeStats> pair : env.biome) {
                
                if (biomes != null && !pair.key.checkBiome(biomes))
                    continue;
                
                if (badBiomes != null && pair.key.checkBiome(badBiomes))
                    return null;
                
                double value = pair.value.volume(env, biomeType);
                if (highest == -1 || highest < value)
                    highest = value;
            }
            
            if (highest == -1 && biomes != null)
                return null;
            else if (highest != -1)
                selection.volume *= highest;
        }
        
        if (overallRaining != null && overallRaining)
            selection.volume *= env.rainSurfaceVolume;
        
        if (underwater != null) {
            double volume = underwater.volume(env.underwater);
            if (volume <= 0)
                return null;
            
            selection.volume *= volume;
        }
        
        if (relativeHeight != null) {
            double volume = relativeHeight.volume(env.relativeMinHeight, env.relativeHeight, env.relativeMaxHeight);
            if (volume <= 0)
                return null;
            
            selection.volume *= volume;
        }
        
        if (minHeightRelative != null) {
            double volume = minHeightRelative.volume(env.relativeMinHeight);
            if (volume <= 0)
                return null;
            
            selection.volume *= volume;
        }
        
        if (maxHeightRelative != null) {
            double volume = maxHeightRelative.volume(env.relativeMaxHeight);
            if (volume <= 0)
                return null;
            
            selection.volume *= volume;
        }
        
        if (absoluteHeight != null) {
            double volume = absoluteHeight.volume(env.absoluteHeight);
            if (volume <= 0)
                return null;
            
            selection.volume *= volume;
        }
        
        if (light != null) {
            double volume = light.volume(env.terrain.airPocket.averageLight);
            if (volume <= 0)
                return null;
            
            selection.volume *= volume;
        }
        
        if (skyLight != null) {
            double volume = skyLight.volume(env.terrain.airPocket.averageSkyLight);
            if (volume <= 0)
                return null;
            
            selection.volume *= volume;
        }
        
        if (air != null) {
            double volume = air.volume(env.terrain.airPocket.air);
            if (volume <= 0)
                return null;
            
            selection.volume *= volume;
        }
        
        if (badFeatures != null && env.terrain.airPocket.volume(badFeatures) > 0)
            return null;
        
        if (features != null) {
            double volume = env.terrain.airPocket.volume(features);
            if (volume <= 0)
                return null;
            
            selection.volume *= volume;
        }
        
        if (temperature != null) {
            double volume = temperature.volume(env.temperature);
            if (volume <= 0)
                return null;
            
            selection.volume *= volume;
        }
        
        if (entity != null) {
            double entityVolume = entity.value(env);
            if (entityVolume <= 0)
                return null;
            selection.volume *= entityVolume;
        }
        
        if (variants != null) {
            AmbientSelection bestCondition = null;
            
            for (AmbientCondition condition : variants) {
                AmbientSelection subSelection = condition.value(env);
                if (subSelection != null && (bestCondition == null || bestCondition.volume < subSelection.volume))
                    bestCondition = subSelection;
            }
            
            if (bestCondition == null)
                return null;
            
            selection.subSelection = bestCondition;
        }
        
        return selection;
    }
    
    public static class AmbientMinMaxCondition {
        
        public Double min;
        public Double max;
        
        public boolean is(double value) {
            if (min != null && value < min)
                return false;
            if (max != null && value > max)
                return false;
            return true;
        }
        
        public double randomValue() {
            if (max == null)
                if (min == null)
                    return 0;
                else
                    return min;
                
            if (min == null)
                min = 0D;
            
            double distance = max - min;
            return Math.random() * distance + min;
        }
        
    }
    
    public static class AmbientMinMaxFadeCondition extends AmbientMinMaxCondition {
        
        public Double fade;
        
        public double volume(double value) {
            if (!is(value))
                return 0;
            if (fade == null)
                return 1;
            
            double volume = 1;
            if (min != null)
                volume = Mth.clamp(Math.abs(value - min) / fade, 0, 1);
            if (max != null)
                volume = Math.min(volume, Mth.clamp(Math.abs(value - max) / fade, 0, 1));
            return volume;
        }
        
    }
    
    public static class AmbientMinMaxFadeSpecialCondition extends AmbientMinMaxFadeCondition {
        
        public double volume(double min, double value, double max) {
            if (fade == null)
                return is(value) ? 1 : 0;
            double volume = volume(value);
            if (volume == 1)
                return volume;
            return Math.max(volume(min), volume(max));
        }
        
    }
}
