package team.creative.ambientsounds.env.pocket;

import net.minecraft.world.level.block.state.BlockState;
import team.creative.ambientsounds.AmbientEngine;
import team.creative.ambientsounds.env.feature.AmbientFeature;
import team.creative.creativecore.common.util.type.map.HashMapDouble;

public class AirPocket {
    
    public final HashMapDouble<String> features = new HashMapDouble<>();
    public final double averageLight;
    public final double averageSkyLight;
    public final double air;
    
    public AirPocket() {
        averageLight = 15;
        averageSkyLight = 15;
        air = 1;
    }
    
    public AirPocket(AmbientEngine engine, HashMapDouble<BlockState> foundPercentage, HashMapDouble<BlockState> foundCount, double averageLight, double averageSkyLight, double air) {
        this.averageLight = averageLight;
        this.averageSkyLight = averageSkyLight;
        this.air = air;
        for (AmbientFeature feature : engine.features) {
            double volume = feature.volume(foundPercentage, foundCount);
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
