package com.creativemd.ambientsounds;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class AmbientSituation {
	
	public static class BiomeArea {
		
		public final Biome biome;
		public final BlockPos pos;
		
		public BiomeArea(Biome biome, BlockPos pos) {
			this.biome = biome;
			this.pos = pos;
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
	
	public World world;
	public EntityPlayer player;
	public LinkedHashMap<BiomeArea, Float> biomes;
	public float relativeHeight;
	public boolean isNight;
	public boolean playedFull = false;
	public boolean isRaining;
	public boolean isThundering;
	
	public float biomeVolume = 1.0F;
	
	public ArrayList<BiomeArea> selectedBiomes;
	
	public AmbientSituation(World world, EntityPlayer player, LinkedHashMap<BiomeArea, Float> biomes, float relativeHeight, boolean isNight) {
		this.world = world;
		this.player = player;
		this.biomes = biomes;
		this.relativeHeight = relativeHeight;
		this.isNight = isNight;
		this.isRaining = world.isRainingAt(player.getPosition());
		this.isThundering = world.isThundering();
	}
	
}
