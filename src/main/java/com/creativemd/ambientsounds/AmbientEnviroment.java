package com.creativemd.ambientsounds;

import java.util.List;

import com.creativemd.ambientsounds.utils.PairList;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class AmbientEnviroment {
	
	public World world;
	
	public boolean soundsDisabled = false;
	
	public boolean night;
	
	public boolean raining;
	public boolean thundering;
	
	public PairList<BiomeArea, Float> biomes;
	public BlockEnviroment blocks = new BlockEnviroment();
	
	public EntityPlayer player;
	
	public double underwater;
	public double averageHeight;
	public double relativeHeight;
	public int minHeight;
	public int maxHeight;
	
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
		if (underwater > 0)
			blocks.outsideVolume = 0;
	}
	
	public void setHeight(TerrainHeight terrain) {
		this.averageHeight = terrain.averageHeight;
		this.relativeHeight = player.posY - terrain.averageHeight;
		this.minHeight = terrain.minHeight;
		this.maxHeight = terrain.maxHeight;
	}
	
	public static class TerrainHeight {
		
		public final double averageHeight;
		public final int minHeight;
		public final int maxHeight;
		
		public TerrainHeight(double averageHeight, int minHeight, int maxHeight) {
			this.averageHeight = averageHeight;
			this.minHeight = minHeight;
			this.maxHeight = maxHeight;
		}
		
	}
	
	public class BlockEnviroment {
		
		public double averageLight;
		public double outsideVolume;
		protected BlockSpot[] spots;
		
		public BlockEnviroment() {
			this.spots = new BlockSpot[EnumFacing.VALUES.length];
			
		}
		
		public void updateAllDirections(AmbientEngine engine) {
			int lightspots = 0;
			averageLight = 0;
			MutableBlockPos pos = new MutableBlockPos();
			for (EnumFacing facing : EnumFacing.VALUES) {
				BlockSpot spot = updateDirection(pos, facing, engine);
				if (spot != null) {
					spots[facing.ordinal()] = spot;
					averageLight += spot.light;
					lightspots++;
				} else
					spots[facing.ordinal()] = null;
				
			}
			if (lightspots == 0)
				averageLight = world.getLight(pos.setPos(player));
			else
				averageLight /= lightspots;
			outsideVolume = calculateOutsideVolume(engine);
		}
		
		protected BlockSpot updateDirection(MutableBlockPos pos, EnumFacing facing, AmbientEngine engine) {
			pos.setPos(player);
			pos.setY(pos.getY() + 1);
			
			for (int i = 1; i < engine.blockScanDistance; i++) {
				pos.setPos(pos.getX() + facing.getFrontOffsetX(), pos.getY() + facing.getFrontOffsetY(), pos.getZ() + facing.getFrontOffsetZ());
				IBlockState state = world.getBlockState(pos);
				if (state.isOpaqueCube())
					return new BlockSpot(state, i, world.getLight(pos.offset(facing.getOpposite())));
			}
			return null;
		}
		
		protected double calculateOutsideVolume(AmbientEngine engine) {
			Integer distanceX;
			if (spots[EnumFacing.EAST.ordinal()] == null)
				distanceX = null;
			else if (spots[EnumFacing.WEST.ordinal()] == null)
				distanceX = null;
			else
				distanceX = spots[EnumFacing.EAST.ordinal()].distance + spots[EnumFacing.WEST.ordinal()].distance;
			
			Integer distanceY;
			if (spots[EnumFacing.UP.ordinal()] == null)
				distanceY = null;
			else if (spots[EnumFacing.DOWN.ordinal()] == null)
				distanceY = null;
			else
				distanceY = spots[EnumFacing.UP.ordinal()].distance + spots[EnumFacing.DOWN.ordinal()].distance;
			
			Integer distanceZ;
			if (spots[EnumFacing.SOUTH.ordinal()] == null)
				distanceZ = null;
			else if (spots[EnumFacing.NORTH.ordinal()] == null)
				distanceZ = null;
			else
				distanceZ = spots[EnumFacing.SOUTH.ordinal()].distance + spots[EnumFacing.NORTH.ordinal()].distance;
			
			double volumeVertical;
			if (distanceY == null)
				volumeVertical = 1;
			else
				volumeVertical = MathHelper.clamp((distanceY - engine.outsideDistanceMin) / (double) (engine.outsideDistanceMax - engine.outsideDistanceMin), 0, 1);
			
			double volumeHorizontal;
			if (distanceX == null || distanceZ == null)
				volumeHorizontal = 1;
			else
				volumeHorizontal = MathHelper.clamp((Math.max(distanceX, distanceZ) - engine.outsideDistanceMin) / (double) (engine.outsideDistanceMax - engine.outsideDistanceMin), 0, 1);
			
			return volumeHorizontal * volumeVertical;
		}
		
	}
	
	public static class BlockSpot {
		public IBlockState state;
		public int distance;
		public int light;
		
		public BlockSpot(IBlockState state, int distance, int light) {
			this.state = state;
			this.distance = distance;
			this.light = light;
		}
		
		public Material getMaterial() {
			return state.getMaterial();
		}
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
