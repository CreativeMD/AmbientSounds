package com.creativemd.ambientsounds;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

public class BiomeSound extends AmbientSound{
	
	public String name;
	public boolean isNight;
	public boolean needTime = true;
	
	public BiomeSound(String name, String customTexture, float volume, boolean isNight)
	{
		super(customTexture, volume);
		this.name = name;
		this.isNight = isNight;
	}
	
	public BiomeSound(String name, float volume, boolean isNight)
	{
		super(name, volume);
		this.name = name;
		this.isNight = isNight;
	}
	
	public BiomeSound setIgnoreTime()
	{
		needTime = false;
		return this;
	}
	
	@Override
	public float getVolume(World world, EntityPlayer player, BiomeGenBase biome, boolean isNight) {
		if(biome.biomeName.toLowerCase().contains(name.toLowerCase()) && (isNight == this.isNight || !needTime))
			return 1;
		return 0;
	}
	
}
