package com.creativemd.ambientsounds;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

public class TemperatureSound extends AmbientSound{
	
	public int min;
	public int max;
	
	public TemperatureSound(String name, float volume, int minTemp, int maxTemp)
	{
		super(name, volume);
		this.min = minTemp;
		this.max = maxTemp;
	}
	
	@Override
	public float getVolume(World world, EntityPlayer player, BiomeGenBase biome, boolean isNight, float height) {
		if(biome.temperature >= min && biome.temperature <= max)
			return getVolumeFromHeight(1, height);
		return 0;
	}

}
