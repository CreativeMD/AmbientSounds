package team.creative.ambientsounds.environment.pocket;

import java.util.HashMap;

import team.creative.ambientsounds.engine.AmbientEngine;
import team.creative.ambientsounds.environment.feature.AmbientFeature;
import team.creative.creativecore.common.util.type.map.HashMapDouble;

public class AirPocket {
    
    public final HashMapDouble<String> features = new HashMapDouble<>();
    public final double averageLight;
    public final double averageBlockLight;
    public final double averageSkyLight;
    public final double air;
    public final double sky;
    
    public AirPocket() {
        averageLight = 15;
        averageBlockLight = 15;
        averageSkyLight = 15;
        air = 1;
        sky = 1;
    }
    
    public AirPocket(AmbientEngine engine, HashMap<String, BlockDistribution> distribution, double averageLight, double averageBlockLight, double averageSkyLight, double air, int sky) {
        this.averageLight = averageLight;
        this.averageBlockLight = averageBlockLight;
        this.averageSkyLight = averageSkyLight;
        if (air < engine.airMin)
            this.air = 0;
        else if (air > engine.airMax)
            this.air = 1;
        else
            this.air = (air - engine.airMin) / (engine.airMax - engine.airMin);
        
        if (sky < engine.skyMinCount)
            this.sky = 0;
        else if (sky > engine.skyMaxCount)
            this.sky = 1;
        else
            this.sky = (sky - engine.skyMinCount) / (double) (engine.skyMaxCount - engine.skyMinCount);
        for (AmbientFeature feature : engine.features.values()) {
            double volume = feature.volume(distribution);
            if (volume > 0)
                features.put(feature.name, volume);
        }
    }
    
    public double volume(String[] features) {
        double volume = 0;
        for (int i = 0; i < features.length; i++)
            volume = Math.max(volume, this.features.getOrDefault(features[i], 0D));
        return volume;
    }
    
}
