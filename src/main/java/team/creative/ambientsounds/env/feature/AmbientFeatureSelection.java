package team.creative.ambientsounds.env.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import team.creative.ambientsounds.env.pocket.BlockDistribution;

public class AmbientFeatureSelection {
    
    public transient List<AmbientBlockGroup> group = new ArrayList<>();
    
    public String[] groups;
    
    public AmbientFeatureSelection[] and;
    public AmbientFeatureSelection[] or;
    public AmbientFeatureSelection[] not;
    
    @SerializedName(value = "low-weight")
    public Double lowWeight;
    @SerializedName(value = "high-weight")
    public Double highWeight;
    
    @SerializedName(value = "low-count")
    public Double lowCount;
    @SerializedName(value = "high-count")
    public Double highCount;
    
    public void collectGroups(HashSet<String> groups) {
        if (this.groups != null)
            groups.addAll(Arrays.asList(this.groups));
        if (and != null)
            for (int i = 0; i < and.length; i++)
                and[i].collectGroups(groups);
        if (or != null)
            for (int i = 0; i < or.length; i++)
                or[i].collectGroups(groups);
        if (not != null)
            for (int i = 0; i < not.length; i++)
                not[i].collectGroups(groups);
    }
    
    public double volume(HashMap<String, BlockDistribution> distribution) {
        double volume = 1;
        if (lowWeight != null || highWeight != null || lowCount != null || highCount != null) {
            BlockDistribution collected = new BlockDistribution();
            for (int i = 0; i < groups.length; i++) {
                BlockDistribution result = distribution.get(groups[i]);
                if (result != null)
                    collected.add(result);
            }
            
            if (lowWeight != null || highWeight != null) {
                if (lowWeight != null && lowWeight > collected.percentage)
                    return 0;
                if (highWeight == null || highWeight < collected.percentage)
                    volume *= 1;
                else {
                    double low = lowWeight == null ? 0 : lowWeight;
                    volume *= (collected.percentage - low) / (highWeight - low);
                }
            }
            
            if (lowCount != null || highCount != null) {
                if (lowCount != null && lowCount > collected.count)
                    return 0;
                if (highCount == null || highCount < collected.count)
                    volume *= 1;
                else {
                    double low = lowCount == null ? 0 : lowCount;
                    volume *= (collected.count - low) / (highCount - low);
                }
            }
        }
        
        if (and != null)
            for (int i = 0; i < and.length; i++)
                volume *= and[i].volume(distribution);
            
        if (or != null) {
            double orVolume = 0;
            for (int i = 0; i < or.length; i++)
                orVolume = Math.max(orVolume, or[i].volume(distribution));
            volume *= orVolume;
        }
        
        if (not != null)
            for (int i = 0; i < not.length; i++)
                if (not[i].volume(distribution) > 0)
                    return 0;
                
        return volume;
    }
    
}
