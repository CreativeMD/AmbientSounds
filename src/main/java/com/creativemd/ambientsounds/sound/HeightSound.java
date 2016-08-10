package com.creativemd.ambientsounds.sound;

import com.creativemd.ambientsounds.AmbientSound;
import com.creativemd.ambientsounds.env.AmbientEnv;
import com.creativemd.ambientsounds.env.HeightEnv.HeightArea;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class HeightSound extends AmbientSound {
	
	public HeightArea preferedHeight;
	
	public HeightSound(String name, float volume, HeightArea preferedHeight) {
		super(AmbientEnv.height, name, volume);
		this.preferedHeight = preferedHeight;
	}
	
	public float getVolume(World world, EntityPlayer player, boolean isNight, float volume) {
		return volume;
	}
	
	public static float getVolumeForHeightArea(HeightArea area)
	{
		Float volume = AmbientEnv.height.currentHeight.get(area);
		if(volume != null)
			return volume;
		return 0.0F;
	}
	
	@Override
	public float getVolume(World world, EntityPlayer player, boolean isNight) {
		return getVolume(world, player, isNight, getVolumeForHeightArea(preferedHeight));
	}
	
}
