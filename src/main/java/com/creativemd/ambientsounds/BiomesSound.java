package com.creativemd.ambientsounds;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeManager;

public class BiomesSound extends AmbientSound{
	
	public String[] biomes;
	public boolean isNight;
	public boolean needTime = true;
	public boolean ignoreLocation = false;
	public float minTemperature = -100;
	
	public BiomesSound(String biome, float volume, boolean isNight)
	{
		this(biome, biome, volume, isNight);
	}
	
	public BiomesSound(String biome, String soundName, float volume, boolean isNight)
	{
		this(new String[]{biome}, soundName, volume, isNight);
	}	
	
	public BiomesSound(String[] biomes, String soundName, float volume, boolean isNight)
	{
		super(soundName, volume);
		this.biomes = biomes;
		this.isNight = isNight;
	}
	
	public BiomesSound setMinTemperature(float temperature)
	{
		this.minTemperature = temperature;
		return this;
	}
	
	public BiomesSound setIgnoreLocation()
	{
		ignoreLocation = true;
		return this;
	}
	
	public BiomesSound setIgnoreTime()
	{
		needTime = false;
		return this;
	}
	
	public static boolean checkBiome(String name, Biome biome)
	{
		return biome.getBiomeName().toLowerCase().contains(name.toLowerCase()) || biome.getBiomeName().toLowerCase().contains(name.toLowerCase().replace(" ", "_"));
	}
	
	@Override
	public float getVolume(World world, EntityPlayer player, Biome biome, boolean isNight, float height) {
		if((isNight == this.isNight || !needTime) && biome.getTemperature() >= minTemperature)
			for (int i = 0; i < biomes.length; i++)
				if(checkBiome(biomes[i], biome))
					return ignoreLocation ? 1 : getVolumeFromHeight(1, height);
		return 0;
	}
	
}
