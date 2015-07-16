package com.creativemd.ambientsounds;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

public class BiomesSound extends AmbientSound{
	
	public String[] biomes;
	public boolean isNight;
	public boolean needTime = true;
	
	public BiomesSound(String[] biomes, String customTexture, float volume, boolean isNight)
	{
		super(customTexture, volume);
		this.biomes = biomes;
		this.isNight = isNight;
	}
	
	public BiomesSound setIgnoreTime()
	{
		needTime = false;
		return this;
	}
	
	@Override
	public float getVolume(World world, EntityPlayer player, BiomeGenBase biome, boolean isNight) {
		if(isNight == this.isNight || !needTime)
			for (int i = 0; i < biomes.length; i++)
				if(biome.biomeName.toLowerCase().contains(biomes[i].toLowerCase()))
						return 1;
		return 0;
	}
	
}
