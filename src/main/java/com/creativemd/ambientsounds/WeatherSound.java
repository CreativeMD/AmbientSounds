package com.creativemd.ambientsounds;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class WeatherSound extends AmbientSound {
	
	public static enum WeatherType {
		SUNNY {
			@Override
			public boolean isWeather(World world, BlockPos pos) {
				return !RAINY.isWeather(world, pos);
			}
		},
		RAINY {
			@Override
			public boolean isWeather(World world, BlockPos pos) {
				return world.isRainingAt(pos);
			}
		},
		STORMY {
			@Override
			public boolean isWeather(World world, BlockPos pos) {
				return RAINY.isWeather(world, pos) && world.isThundering();
			}
		};
		
		
		public abstract boolean isWeather(World world, BlockPos pos);
		
	}
	
	public WeatherType type;
	
	public WeatherSound(String name, float volume, WeatherType type) {
		super(name, volume);
		this.type = type;
	}
	
	public float getMutingFactorPriority()
	{
		return 0.5F;
	}
	
	public float getMutingFactor()
	{
		return 0.8F;
	}

	@Override
	public float getVolume(World world, EntityPlayer player, Biome biome, boolean isNight, float height) {
		if(type.isWeather(world, player.getPosition()))
			if(height < 1)
				return getVolumeFromHeight(1, height);
			else
				return 1;
		return 0;
	}

}
