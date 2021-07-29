package team.creative.ambientsounds;

import java.util.LinkedHashMap;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;

public class AmbientEnviroment {
    
    public Level level;
    
    public boolean soundsDisabled = false;
    
    public boolean night;
    
    public boolean overallRaining;
    public boolean raining;
    public boolean snowing;
    public boolean thundering;
    
    public LinkedHashMap<BiomeArea, Float> biomes;
    public BlockEnviroment blocks = new BlockEnviroment();
    
    public Player player;
    
    public double underwater;
    public double averageHeight;
    public double relativeHeight;
    public int minHeight;
    public int maxHeight;
    
    public double biomeVolume = 1;
    public AmbientDimension dimension;
    
    public AmbientEnviroment(Player player) {
        this.player = player;
        this.level = player.level;
    }
    
    public void updateLevel() {
        this.overallRaining = level.isRaining();
        this.raining = level.isRainingAt(player.blockPosition());
        Biome biome = level.getBiome(player.blockPosition());
        this.snowing = biome.getPrecipitation() == Biome.Precipitation.SNOW && level.isRaining();
        this.thundering = level.isThundering();
    }
    
    public void setSunAngle(float sunAngle) {
        this.night = sunAngle > 90 && sunAngle < 270;
    }
    
    public void setUnderwater(double underwater) {
        this.underwater = underwater;
        if (underwater > 0)
            blocks.outsideVolume = 0;
    }
    
    public void setHeight(TerrainHeight terrain) {
        this.averageHeight = terrain.averageHeight;
        this.relativeHeight = player.getEyeY() - terrain.averageHeight;
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
            this.spots = new BlockSpot[Direction.values().length];
            
        }
        
        public void updateAllDirections(AmbientEngine engine) {
            int lightspots = 0;
            averageLight = 0;
            MutableBlockPos pos = new MutableBlockPos();
            for (Direction facing : Direction.values()) {
                BlockSpot spot = updateDirection(pos, facing, engine);
                if (spot != null) {
                    spots[facing.ordinal()] = spot;
                    averageLight += spot.light;
                    lightspots++;
                } else
                    spots[facing.ordinal()] = null;
                
            }
            if (lightspots == 0)
                averageLight = level.getLightEmission(pos.set(player.blockPosition()));
            else
                averageLight /= lightspots;
            outsideVolume = calculateOutsideVolume(engine);
        }
        
        protected BlockSpot updateDirection(MutableBlockPos pos, Direction facing, AmbientEngine engine) {
            pos.set(player.blockPosition());
            pos.setY(pos.getY() + 1);
            
            for (int i = 1; i < engine.blockScanDistance; i++) {
                pos.set(pos.getX() + facing.getStepX(), pos.getY() + facing.getStepY(), pos.getZ() + facing.getStepZ());
                BlockState state = level.getBlockState(pos);
                if (state.isCollisionShapeFullBlock(level, pos))
                    return new BlockSpot(state, i, level.getLightEmission(pos.move(facing.getOpposite())));
            }
            return null;
        }
        
        protected double calculateOutsideVolume(AmbientEngine engine) {
            Integer distanceX;
            if (spots[Direction.EAST.ordinal()] == null)
                distanceX = null;
            else if (spots[Direction.WEST.ordinal()] == null)
                distanceX = null;
            else
                distanceX = spots[Direction.EAST.ordinal()].distance + spots[Direction.WEST.ordinal()].distance;
            
            Integer distanceY;
            if (spots[Direction.UP.ordinal()] == null)
                distanceY = null;
            else if (spots[Direction.DOWN.ordinal()] == null)
                distanceY = null;
            else
                distanceY = spots[Direction.UP.ordinal()].distance + spots[Direction.DOWN.ordinal()].distance;
            
            Integer distanceZ;
            if (spots[Direction.SOUTH.ordinal()] == null)
                distanceZ = null;
            else if (spots[Direction.NORTH.ordinal()] == null)
                distanceZ = null;
            else
                distanceZ = spots[Direction.SOUTH.ordinal()].distance + spots[Direction.NORTH.ordinal()].distance;
            
            double volumeVertical;
            if (distanceY == null)
                volumeVertical = 1;
            else
                volumeVertical = Mth.clamp((distanceY - engine.outsideDistanceMin) / (double) (engine.outsideDistanceMax - engine.outsideDistanceMin), 0, 1);
            
            double volumeHorizontal;
            if (distanceX == null || distanceZ == null)
                volumeHorizontal = 1;
            else
                volumeHorizontal = Mth.clamp((Math.max(distanceX, distanceZ) - engine.outsideDistanceMin) / (double) (engine.outsideDistanceMax - engine.outsideDistanceMin), 0, 1);
            
            return volumeHorizontal * volumeVertical;
        }
        
    }
    
    public static class BlockSpot {
        public BlockState state;
        public int distance;
        public int light;
        
        public BlockSpot(BlockState state, int distance, int light) {
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
                String biomename = biome.getBiomeCategory().getName().toLowerCase().replace("_", " ");
                if (biomename.matches(".*" + name.replace("*", ".*") + ".*"))
                    return true;
            }
            return false;
        }
        
        public boolean checkTopBlock(List<Block> topBlocks) {
            return topBlocks.contains(biome.getGenerationSettings().getSurfaceBuilderConfig().getTopMaterial().getBlock());
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
