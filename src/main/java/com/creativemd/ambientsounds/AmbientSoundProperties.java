package com.creativemd.ambientsounds;

import com.creativemd.ambientsounds.AmbientCondition.AmbientMinMaxCondition;
import com.google.gson.annotations.SerializedName;

import net.minecraft.util.math.MathHelper;

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
			fadeInVolume = fadeVolume == null ? 0.01 : fadeVolume;
		if (fadeOutVolume == null)
			fadeOutVolume = fadeVolume == null ? 0.01 : fadeVolume;
		
		if (fadeInPitch == null)
			fadeInPitch = fadePitch == null ? 0.01 : fadePitch;
		if (fadeOutPitch == null)
			fadeOutPitch = fadePitch == null ? 0.01 : fadePitch;
		
		if (mute == null)
			mute = 0D;
		else
			mute = MathHelper.clamp(mute, 0, 1);
	}
	
	public float getPitch(AmbientEnviroment env) {
		if (underwaterPitch != null)
			return (float) underwaterPitch.getValue(env.underwater);
		return pitch != null ? (float) (double) pitch : 1;
	}
	
	public static class AmbientMinMaxClimbingProperty {
		
		public double min;
		public double max;
		@SerializedName(value = "distance-factor")
		public double distanceFactor;
		
		public double getValue(double value) {
			double distance = max - min;
			return value / distance * distanceFactor;
		}
		
	}
	
}
