package com.creativemd.ambientsounds;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

public class CaveSound extends AmbientSound{

	public CaveSound(String name, float volume) {
		super(name, volume);
	}

	@Override
	public float getVolume(World world, EntityPlayer player, BiomeGenBase biome, boolean isNight, float height) {
		return (world.provider.getDimension() != -1 && world.provider.getDimension() != 1) ? getVolumeFromHeight(0, height) : 0;
	}

}
