package team.creative.ambientsounds;

import com.google.gson.annotations.SerializedName;

import net.minecraft.util.Mth;
import team.creative.ambientsounds.AmbientCondition.AmbientMinMaxCondition;
import team.creative.ambientsounds.env.AmbientEnviroment;

public class AmbientSoundProperties {
    
    @SerializedName(value = "transition")
    public Integer transition;
    
    public Double pitch;
    
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
    
    public Double mute;
    
    //public AmbientMinMaxCondition offset;
    public AmbientMinMaxCondition pause;
    public AmbientMinMaxCondition length;
    
    @SerializedName(value = "underwater-pitch")
    public AmbientMinMaxClimbingProperty underwaterPitch;
    
    public void init(AmbientEngine engine) {
        if (pitch == null)
            pitch = 1D;
        
        if (fadeInVolume == null)
            fadeInVolume = fadeVolume == null ? 0.005 : fadeVolume;
        if (fadeOutVolume == null)
            fadeOutVolume = fadeVolume == null ? 0.005 : fadeVolume;
        
        if (fadeInPitch == null)
            fadeInPitch = fadePitch == null ? 0.005 : fadePitch;
        if (fadeOutPitch == null)
            fadeOutPitch = fadePitch == null ? 0.005 : fadePitch;
        
        if (mute == null)
            mute = 0D;
        else
            mute = Mth.clamp(mute, 0, 1);
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
