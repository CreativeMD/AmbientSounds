package team.creative.ambientsounds;

import com.google.gson.annotations.SerializedName;

import net.minecraft.util.Mth;
import team.creative.ambientsounds.AmbientCondition.AmbientMinMaxCondition;
import team.creative.ambientsounds.env.AmbientEnviroment;

public class AmbientSoundProperties {
    
    @SerializedName(value = "transition")
    public Integer transition;
    
    public Double pitch = 1D;
    
    @SerializedName(value = "fade-volume")
    public Double fadeVolume;
    @SerializedName(value = "fade-in-volume")
    public Double fadeInVolume;
    @SerializedName(value = "fade-out-volume")
    public Double fadeOutVolume;
    
    @SerializedName(value = "fade-pitch")
    public Double fadePitch;
    @SerializedName(value = "fade-in-pitch")
    public Double fadeInPitch;
    @SerializedName(value = "fade-out-pitch")
    public Double fadeOutPitch;
    
    public Double mute = 0D;
    
    //public AmbientMinMaxCondition offset;
    public AmbientMinMaxCondition pause;
    public AmbientMinMaxCondition length;
    
    @SerializedName(value = "underwater-pitch")
    public AmbientMinMaxClimbingProperty underwaterPitch;
    
    public String category;
    
    public void init(AmbientEngine engine) {
        mute = Mth.clamp(mute, 0, 1);
    }
    
    public double getFadeInVolume(AmbientEngine engine) {
        if (fadeInVolume != null)
            return fadeInVolume;
        if (fadeVolume != null)
            return fadeVolume;
        return engine.fadeVolume;
    }
    
    public double getFadeOutVolume(AmbientEngine engine) {
        if (fadeOutVolume != null)
            return fadeOutVolume;
        if (fadeVolume != null)
            return fadeVolume;
        return engine.fadeVolume;
    }
    
    public double getFadeInPitch(AmbientEngine engine) {
        if (fadeInPitch != null)
            return fadeInPitch;
        if (fadePitch != null)
            return fadePitch;
        return engine.fadePitch;
    }
    
    public double getFadeOutPitch(AmbientEngine engine) {
        if (fadeOutPitch != null)
            return fadeOutPitch;
        if (fadePitch != null)
            return fadePitch;
        return engine.fadePitch;
    }
    
    public float getPitch(AmbientEnviroment env) {
        if (underwaterPitch != null)
            return (pitch != null ? (float) (double) pitch : 1) + (float) underwaterPitch.getValue(env.underwater);
        return pitch != null ? (float) (double) pitch : 1;
    }
    
    public static class AmbientMinMaxClimbingProperty {
        
        public double min = 0;
        public double max;
        @SerializedName(value = "distance-factor")
        public double distanceFactor = 1;
        
        public double getValue(double value) {
            if (max <= min)
                max = min + 1;
            double distance = max - min;
            return value / distance * distanceFactor;
        }
        
    }
    
}
