package com.creativemd.ambientsounds;

import com.creativemd.ambientsounds.env.AmbientEnv;
import com.creativemd.ambientsounds.env.HeightEnv.HeightArea;
import com.creativemd.ambientsounds.sound.HeightSound;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class WeatherSound extends HeightSound {
	
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
				return world.isThundering();
			}
		};
		
		
		public abstract boolean isWeather(World world, BlockPos pos);
		
	}
	
	public WeatherType type;
	
	public WeatherSound(String name, float volume, WeatherType type) {
		super(name, volume, HeightArea.Surface);
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
	public float getVolume(World world, EntityPlayer player, boolean isNight, float volume) {
		if(type.isWeather(world, player.getPosition())){
			if(AmbientEnv.height.currentHeight.containsKey(HeightArea.Sky))
				return 1;
			else
				return volume;
		}
		return 0;
	}

}
