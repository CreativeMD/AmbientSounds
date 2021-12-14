package team.creative.ambientsounds.env.feature;

import java.util.Map.Entry;

import com.google.gson.annotations.SerializedName;

import net.minecraft.world.level.block.state.BlockState;
import team.creative.creativecore.common.util.type.map.HashMapDouble;

public class AmbientFeature {
    
    public String name;
    public transient AmbientBlockGroup blocks = new AmbientBlockGroup();
    public transient AmbientBlockGroup badBlocks = new AmbientBlockGroup();
    
    @SerializedName(value = "low-weight")
    public Double lowWeight;
    @SerializedName(value = "high-weight")
    public Double highWeight;
    
    @SerializedName(value = "low-count")
    public Double lowCount;
    @SerializedName(value = "high-count")
    public Double highCount;
    
    public double volume(HashMapDouble<BlockState> foundPercentage, HashMapDouble<BlockState> foundCount) {
        double volume = 1;
        if (lowWeight != null || highWeight != null) {
            double percentage = 0;
            for (Entry<BlockState, Double> entry : foundPercentage.entrySet())
                if (blocks.is(entry.getKey()))
                    percentage += entry.getValue();
            if (lowWeight != null && lowWeight > percentage)
                return 0;
            if (highWeight == null || highWeight < percentage)
                volume *= 1;
            else {
                double low = lowWeight == null ? 0 : lowWeight;
                volume *= (percentage - low) / (highWeight - low);
            }
        }
        
        if (lowCount != null || highCount != null) {
            double count = 0;
            for (Entry<BlockState, Double> entry : foundCount.entrySet())
                if (blocks.is(entry.getKey()))
                    count += entry.getValue();
            if (lowCount != null && lowCount > count)
                return 0;
            if (highCount == null || highCount < count)
                volume *= 1;
            else {
                double low = lowCount == null ? 0 : lowCount;
                volume *= (count - low) / (highCount - low);
            }
        }
        
        return volume;
    }
    
    public void onClientLoad() {
        if (blocks != null)
            blocks.onClientLoad();
        if (badBlocks != null)
            badBlocks.onClientLoad();
    }
    
}
