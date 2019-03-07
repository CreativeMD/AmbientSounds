package com.creativemd.ambientsounds;

import java.util.List;

import com.creativemd.creativecore.common.utils.type.PairList;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class AmbientEnviroment {
	
	public World world;
	
	public boolean night;
	
	public boolean raining;
	public boolean thundering;
	
	public double averageLight;
	
	public PairList<BiomeArea, Float> biomes;
	public PairList<IBlockState, Integer> blocks;
	
	public double averageHeight;
	
	public EntityPlayer player;
	
	public double underwater;
	public double relativeHeight;
	
	public double biomeVolume = 1;
	public AmbientDimension dimension;
	
	public AmbientEnviroment(EntityPlayer player) {
		this.player = player;
		this.world = player.world;
	}
	
	public void updateWorld() {
		this.raining = world.isRainingAt(player.getPosition());
		this.thundering = world.isThundering();
	}
	
	public void setSunAngle(float sunAngle) {
		this.night = !(sunAngle > 0.75F || sunAngle < 0.25F);
	}
	
	public void setUnderwater(double underwater) {
		this.underwater = underwater;
	}
	
	public void setHeight(double averageHeight) {
		this.averageHeight = averageHeight;
		this.relativeHeight = player.posY - averageHeight;
	}
	
	public static class BiomeArea {
		
		public final Biome biome;
		public final BlockPos pos;
		
		public BiomeArea(Biome biome, BlockPos pos) {
			this.biome = biome;
			this.pos = pos;
		}
		
		public boolean checkBiome(String[] names) {
			for (String name : names) {
				String biomename = biome.getBiomeName().toLowerCase().replace("_", " ");
				if (biomename.matches(".*" + name.replace("*", ".*") + ".*"))
					return true;
			}
			return false;
		}
		
		public boolean checkTopBlock(List<Block> topBlocks) {
			return topBlocks.contains(biome.topBlock.getBlock());
		}
		
		@Override
		public boolean equals(Object object) {
			if (object instanceof BiomeArea)
				return ((BiomeArea) object).biome == biome;
			return false;
		}
		
		@Override
		public int hashCode() {
			return biome.hashCode();
		}
		
	}
}
