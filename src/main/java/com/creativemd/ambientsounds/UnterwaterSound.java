package com.creativemd.ambientsounds;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class UnterwaterSound extends AmbientSound{

	public UnterwaterSound(String name, float volume) {
		super(name, volume);
	}
	
	public float getMutingFactorPriority()
	{
		return 0.9F;
	}
	
	public float getMutingFactor()
	{
		return 0.9F;
	}

	@Override
	public float getVolume(World world, EntityPlayer player, Biome biome, boolean isNight, float height) {
		if(player.isInsideOfMaterial(Material.WATER))
			return 1;
		return 0;
	}
	
	@Override
	public float fadeInAmount()
	{
		return 0.01F;
	}
	
	@Override
	public float fadeOutAmount()
	{
		return 0.1F;
	}
	
}
