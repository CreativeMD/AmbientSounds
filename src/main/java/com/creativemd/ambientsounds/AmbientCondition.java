package com.creativemd.ambientsounds;

import java.util.ArrayList;
import java.util.List;

import com.creativemd.ambientsounds.AmbientEnviroment.BiomeArea;
import com.creativemd.ambientsounds.AmbientEnviroment.BlockSpot;
import com.creativemd.ambientsounds.utils.Pair;
import com.google.gson.annotations.SerializedName;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.MathHelper;

public class AmbientCondition extends AmbientSoundProperties {
	
	public Boolean always;
	
	public double volume = 1.0;
	@SerializedName(value = "night")
	public double nightVolume = 1.0;
	@SerializedName(value = "day")
	public double dayVolume = 1.0;
	
	public String[] biomes;
	@SerializedName(value = "bad-biomes")
	public String[] badBiomes;
	@SerializedName(value = "special-biomes")
	public AmbientBiomeCondition specialBiome;
	
	public Boolean raining;
	public Boolean storming;
	
	public AmbientMinMaxFadeCondition underwater;
	
	@SerializedName(value = "relative-height")
	public AmbientMinMaxFadeCondition relativeHeight;
	@SerializedName(value = "absolute-height")
	public AmbientMinMaxFadeCondition absoluteHeight;
	@SerializedName(value = "min-height-relative")
	public AmbientMinMaxFadeCondition minHeightRelative;
	@SerializedName(value = "max-height-relative")
	public AmbientMinMaxFadeCondition maxHeightRelative;
	
	public AmbientMinMaxFadeCondition light;
	
	public AmbientMaterialCondition blocks;
	
	public AmbientCondition[] variants;
	
	public String[] regions;
	transient List<AmbientRegion> regionList;
	
	@SerializedName(value = "bad-regions")
	public String[] badRegions;
	transient List<AmbientRegion> badRegionList;
	
	public Boolean outside;
	
	public String regionName() {
		return null;
	}
	
	@Override
	public void init(AmbientEngine engine) {
		super.init(engine);
		
		volume = MathHelper.clamp(volume, 0, 1);
		nightVolume = MathHelper.clamp(nightVolume, 0, 1);
		dayVolume = MathHelper.clamp(dayVolume, 0, 1);
		
		if (specialBiome != null)
			specialBiome.init();
		
		if (blocks != null)
			blocks.init();
		
		if (variants != null)
			for (int i = 0; i < variants.length; i++)
				variants[i].init(engine);
			
		if (regions != null) {
			regionList = new ArrayList<>();
			
			for (String regionName : regions) {
				AmbientRegion region = engine.getRegion(regionName);
				if (region != null && !regionName.equals(regionName()))
					regionList.add(region);
			}
		}
		
		if (badRegions != null) {
			badRegionList = new ArrayList<>();
			
			for (String regionName : badRegions) {
				AmbientRegion region = engine.getRegion(regionName);
				if (region != null && !regionName.equals(regionName()))
					badRegionList.add(region);
			}
		}
	}
	
	public AmbientSelection value(AmbientEnviroment env) {
		
		if (env.soundsDisabled)
			return null;
		
		if (always != null)
			return always ? new AmbientSelection(this) : null;
		
		if (volume <= 0)
			return null;
		
		if (env.night ? nightVolume <= 0 : dayVolume <= 0)
			return null;
		
		if (raining != null && env.raining != raining)
			return null;
		
		if (storming != null && env.thundering != storming)
			return null;
		
		if (outside != null)
			if (outside) {
				if (env.blocks.outsideVolume == 0)
					return null;
			} else if (env.blocks.outsideVolume == 1)
				return null;
			
		AmbientSelection selection = new AmbientSelection(this);
		
		selection.volume *= env.night ? nightVolume : dayVolume;
		
		if (outside != null)
			if (outside)
				selection.volume *= env.blocks.outsideVolume;
			else
				selection.volume *= 1 - env.blocks.outsideVolume;
			
		if (badRegionList != null)
			for (AmbientRegion region : badRegionList)
				if (region.isActive())
					return null;
				
		if (regionList != null) {
			Double highest = null;
			for (AmbientRegion region : regionList) {
				AmbientSelection subSelection = region.value(env);
				
				if (subSelection != null)
					if (highest == null)
						highest = subSelection.volume;
					else
						highest = Math.max(subSelection.volume, highest);
			}
			
			if (highest == null)
				return null;
			
			selection.volume *= highest;
		}
		
		if (biomes != null || badBiomes != null || specialBiome != null) {
			Pair<BiomeArea, Float> highest = null;
			
			for (Pair<BiomeArea, Float> pair : env.biomes) {
				
				if (biomes != null && !pair.key.checkBiome(biomes))
					continue;
				
				if (badBiomes != null && pair.key.checkBiome(badBiomes))
					return null;
				
				if (specialBiome != null && !specialBiome.is(pair.key))
					continue;
				
				if (highest == null || highest.value < pair.value)
					highest = pair;
			}
			
			if (highest == null && (biomes != null || specialBiome != null))
				return null;
			else if (highest != null)
				selection.volume *= highest.value;
		}
		
		if (underwater != null) {
			double volume = underwater.volume(env.underwater);
			if (volume <= 0)
				return null;
			
			selection.volume *= volume;
		}
		
		if (relativeHeight != null) {
			double volume = relativeHeight.volume(env.relativeHeight);
			if (volume <= 0)
				return null;
			
			selection.volume *= volume;
		}
		
		if (minHeightRelative != null) {
			double volume = minHeightRelative.volume(env.player.posY - env.minHeight);
			if (volume <= 0)
				return null;
			
			selection.volume *= volume;
		}
		
		if (maxHeightRelative != null) {
			double volume = maxHeightRelative.volume(env.player.posY - env.maxHeight);
			if (volume <= 0)
				return null;
			
			selection.volume *= volume;
		}
		
		if (absoluteHeight != null) {
			double volume = absoluteHeight.volume(env.player.posY);
			if (volume <= 0)
				return null;
			
			selection.volume *= volume;
		}
		
		if (light != null) {
			double volume = light.volume(env.blocks.averageLight);
			if (volume <= 0)
				return null;
			
			selection.volume *= volume;
		}
		
		if (blocks != null) {
			double volume = blocks.volume(env);
			if (volume <= 0)
				return null;
			
			selection.volume *= volume;
		}
		
		if (variants != null) {
			AmbientSelection bestCondition = null;
			
			for (AmbientCondition condition : variants) {
				AmbientSelection subSelection = condition.value(env);
				if (subSelection != null && (bestCondition == null || bestCondition.volume < subSelection.volume))
					bestCondition = subSelection;
			}
			
			if (bestCondition == null)
				return null;
			
			selection.subSelection = bestCondition;
		}
		
		return selection;
	}
	
	public static class AmbientBiomeCondition {
		
		@SerializedName(value = "top-block")
		public String[] topBlock;
		
		transient List<Block> blocks;
		
		public AmbientMinMaxCondition temperature;
		
		@SerializedName(value = "trees-per-chunk")
		public AmbientMinMaxCondition treesPerChunk;
		@SerializedName(value = "waterlily-per-chunk")
		public AmbientMinMaxCondition waterlilyPerChunk;
		@SerializedName(value = "flowers-per-chunk")
		public AmbientMinMaxCondition flowersPerChunk;
		@SerializedName(value = "grass-per-chunk")
		public AmbientMinMaxCondition grassPerChunk;
		@SerializedName(value = "deadbush-per-chunk")
		public AmbientMinMaxCondition deadBushPerChunk;
		@SerializedName(value = "mushrooms-per-chunk")
		public AmbientMinMaxCondition mushroomsPerChunk;
		@SerializedName(value = "reeds-per-chunk")
		public AmbientMinMaxCondition reedsPerChunk;
		@SerializedName(value = "cacti-per-chunk")
		public AmbientMinMaxCondition cactiPerChunk;
		
		public void init() {
			if (topBlock != null) {
				blocks = new ArrayList<>();
				for (String blockName : topBlock) {
					Block block = Block.getBlockFromName(blockName);
					if (block != null && !(block instanceof BlockAir))
						blocks.add(block);
				}
			}
		}
		
		public boolean is(BiomeArea biome) {
			if (topBlock != null && !biome.checkTopBlock(blocks))
				return false;
			
			if (temperature != null && !temperature.is(biome.biome.getTemperature(biome.pos)))
				return false;
			
			if (treesPerChunk != null && !treesPerChunk.is(biome.biome.decorator.treesPerChunk))
				return false;
			
			if (waterlilyPerChunk != null && !waterlilyPerChunk.is(biome.biome.decorator.waterlilyPerChunk))
				return false;
			
			if (flowersPerChunk != null && !flowersPerChunk.is(biome.biome.decorator.flowersPerChunk))
				return false;
			
			if (grassPerChunk != null && !grassPerChunk.is(biome.biome.decorator.grassPerChunk))
				return false;
			
			if (deadBushPerChunk != null && !deadBushPerChunk.is(biome.biome.decorator.deadBushPerChunk))
				return false;
			
			if (mushroomsPerChunk != null && !mushroomsPerChunk.is(biome.biome.decorator.mushroomsPerChunk))
				return false;
			
			if (reedsPerChunk != null && !reedsPerChunk.is(biome.biome.decorator.reedsPerChunk))
				return false;
			
			if (cactiPerChunk != null && !cactiPerChunk.is(biome.biome.decorator.cactiPerChunk))
				return false;
			
			return true;
		}
		
	}
	
	public static class AmbientMinMaxCondition {
		
		public Double min;
		public Double max;
		
		public boolean is(double value) {
			if (min != null && value < min)
				return false;
			if (max != null && value > max)
				return false;
			return true;
		}
		
		public double randomValue() {
			if (max == null)
				if (min == null)
					return 0;
				else
					return min;
				
			if (min == null)
				min = 0D;
			
			double distance = max - min;
			return Math.random() * distance + min;
		}
		
	}
	
	public static class AmbientMinMaxFadeCondition extends AmbientMinMaxCondition {
		
		public Double fade;
		
		public double volume(double value) {
			if (!is(value))
				return 0;
			if (fade == null)
				return 1;
			
			double volume = 1;
			if (min != null)
				volume = MathHelper.clamp(Math.abs(value - min) / fade, 0, 1);
			if (max != null)
				volume = Math.min(volume, MathHelper.clamp(Math.abs(value - max) / fade, 0, 1));
			return volume;
		}
		
	}
	
	public static class AmbientMaterialCondition {
		
		public String[] materials;
		@SerializedName(value = "bad-materials")
		public String[] badMaterials;
		
		transient List<Material> mat;
		transient List<Material> badMat;
		
		public void init() {
			if (materials != null) {
				mat = new ArrayList<>();
				for (String string : materials) {
					Material material = getMaterial(string);
					if (material != null)
						mat.add(material);
				}
			}
			
			if (badMaterials != null) {
				badMat = new ArrayList<>();
				for (String string : badMaterials) {
					Material material = getMaterial(string);
					if (material != null)
						badMat.add(material);
				}
			}
		}
		
		public double volume(AmbientEnviroment env) {
			if (materials == null && badMaterials == null)
				return 1;
			
			boolean found = false;
			
			for (BlockSpot spot : env.blocks.spots) {
				if (spot == null)
					continue;
				if (!found && materials != null && mat.contains(spot.getMaterial()))
					found = true;
				
				if (badMaterials != null && badMat.contains(spot.getMaterial()))
					return 0;
			}
			
			return found ? 1 : 0;
		}
		
		static Material[] refMaterials = new Material[] { Material.GRASS, Material.GROUND, Material.WOOD, Material.ROCK,
		        Material.IRON, Material.ANVIL, Material.WATER, Material.LAVA, Material.LEAVES, Material.PLANTS,
		        Material.VINE, Material.SPONGE, Material.CLOTH, Material.FIRE, Material.SAND, Material.CIRCUITS,
		        Material.CARPET, Material.GLASS, Material.REDSTONE_LIGHT, Material.TNT, Material.CORAL, Material.ICE,
		        Material.PACKED_ICE, Material.SNOW, Material.CRAFTED_SNOW, Material.CACTUS, Material.CLAY,
		        Material.GOURD, Material.DRAGON_EGG, Material.PORTAL, Material.CAKE, Material.WEB, Material.PISTON,
		        Material.BARRIER, Material.STRUCTURE_VOID };
		
		static String[] refMaterialNames = new String[] { "GRASS", "GROUND", "WOOD", "ROCK", "IRON", "ANVIL", "WATER",
		        "LAVA", "LEAVES", "PLANTS", "VINE", "SPONGE", "CLOTH", "FIRE", "SAND", "CIRCUITS", "CARPET", "GLASS",
		        "REDSTONE_LIGHT", "TNT", "CORAL", "ICE", "PACKED_ICE", "SNOW", "CRAFTED_SNOW", "CACTUS", "CLAY",
		        "GOURD", "DRAGON_EGG", "PORTAL", "CAKE", "WEB", "PISTON", "BARRIER", "STRUCTURE_VOID" };
		
		public static Material getMaterial(String name) {
			for (int i = 0; i < refMaterialNames.length; i++)
				if (refMaterialNames[i].equalsIgnoreCase(name))
					return refMaterials[i];
			return null;
		}
		
	}
}
