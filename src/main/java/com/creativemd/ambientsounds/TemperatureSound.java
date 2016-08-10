package com.creativemd.ambientsounds;

import com.creativemd.ambientsounds.env.AmbientEnv;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class TemperatureSound extends AmbientSound {
	
	public int min;
	public int max;
	public boolean isNight = false;
	public boolean needTime = true;
	
	public TemperatureSound(String name, float volume, int minTemp, int maxTemp)
	{
		super(AmbientEnv.biome, name, volume);
		this.min = minTemp;
		this.max = maxTemp;
	}
	
	public TemperatureSound setIgnoreTime()
	{
		needTime = false;
		return this;
	}
	
	@Override
	public float getVolume(World world, EntityPlayer player, boolean isNight) {
		if((isNight == this.isNight || !needTime))
		{
			float volume = 0.0F;
			for (Biome biome : AmbientEnv.biome.biomes.keySet()) {
				if(biome.getTemperature() >= min && biome.getTemperature() <= max)
					volume = Math.max(volume, AmbientEnv.biome.biomes.get(biome));
			}
			return volume;
		}
		return 0;
	}

}
