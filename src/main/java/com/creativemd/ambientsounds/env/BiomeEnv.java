package com.creativemd.ambientsounds.env;

import java.util.HashMap;

import com.creativemd.ambientsounds.BiomesSound;
import com.creativemd.ambientsounds.env.HeightEnv.HeightArea;
import com.jcraft.jorbis.Block;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class BiomeEnv extends AmbientEnv {
	
	public HashMap<Biome, Float> biomes = new HashMap<>();
	
	@Override
	protected void update(World world, EntityPlayer player) {
		biomes.clear();
		
		Float volume = AmbientEnv.height.currentHeight.get(HeightArea.Surface);
		if(world.provider.getDimension() == -1 || world.provider.getDimension() == 1)
			volume = 1F;
		
		if(volume != null && volume > 0.0)
		{
			int range = 10;
			int stepSize = 5;
			
			int posX = (int) player.posX;
			int posZ = (int) player.posZ;
			BlockPos center = new BlockPos(posX, 0, posZ);
			
			for (int x = -range; x <= range; x+=stepSize) {
				for (int z = -range; z <= range; z+=stepSize) {
					BlockPos pos = new BlockPos(posX+x, 0, posZ+z);
					Biome biome = world.getBiome(pos);
					
					
					float biomeVolume = (float) ((1-Math.sqrt(center.distanceSq(pos))/(range*2))*volume);
					if(biomes.containsKey(biome))
						biomes.put(biome, Math.max(biomes.get(biome), biomeVolume));
					else
						biomes.put(biome, biomeVolume);
				}
			}			
		}
		
		//System.out.println(biomes);
	}

}
