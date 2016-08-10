package com.creativemd.ambientsounds;

import com.creativemd.ambientsounds.env.HeightEnv;
import com.creativemd.ambientsounds.env.HeightEnv.HeightArea;
import com.creativemd.ambientsounds.sound.HeightSound;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class CaveSound extends HeightSound {

	public CaveSound(String name, float volume) {
		super(name, volume, HeightArea.Cave);
	}

	@Override
	public float getVolume(World world, EntityPlayer player, boolean isNight, float volume) {
		return (world.provider.getDimension() != -1 && world.provider.getDimension() != 1) ? volume : 0;
	}

}
