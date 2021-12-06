package team.creative.ambientsounds.env;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import team.creative.ambientsounds.AmbientDimension;
import team.creative.ambientsounds.AmbientEngine;

public class TerrainEnviroment {
    
    public double averageHeight;
    
    public int minHeight;
    public int maxHeight;
    
    public AirPocket airPocket = new AirPocket();
    public AirPocketScanner scanner;
    
    public TerrainEnviroment() {
        this.averageHeight = 60;
        this.minHeight = 60;
        this.maxHeight = 60;
    }
    
    public void analyze(AmbientEngine engine, AmbientDimension dimension, Player player, Level level) {
        analyzeHeight(engine, dimension, player, level);
        analyzeAirPocket(engine, player, level);
    }
    
    public void analyzeHeight(AmbientEngine engine, AmbientDimension dimension, Player player, Level level) {
        if (dimension.averageHeight != null) {
            this.averageHeight = dimension.averageHeight;
            this.minHeight = dimension.averageHeight;
            this.maxHeight = dimension.averageHeight;
            return;
        }
        int sum = 0;
        int count = 0;
        
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        
        MutableBlockPos pos = new MutableBlockPos();
        BlockPos center = player.blockPosition();
        
        for (int x = -engine.averageHeightScanCount; x <= engine.averageHeightScanCount; x++) {
            for (int z = -engine.averageHeightScanCount; z <= engine.averageHeightScanCount; z++) {
                
                pos.set(center.getX() + engine.averageHeightScanDistance * x, center.getY(), center.getZ() + engine.averageHeightScanDistance * z);
                int height = getHeightBlock(level, pos);
                
                min = Math.min(height, min);
                max = Math.max(height, max);
                sum += height;
                count++;
            }
        }
        
        this.averageHeight = (double) sum / count;
        this.minHeight = min;
        this.maxHeight = max;
    }
    
    public void analyzeAirPocket(AmbientEngine engine, Player player, Level level) {
        scanner = new AirPocketScanner(engine, level, player.eyeBlockPosition(), x -> {
            airPocket = x;
            scanner = null;
        });
    }
    
    public static int getHeightBlock(Level level, MutableBlockPos pos) {
        int y;
        int heighest = 0;
        
        for (y = level.dimensionType().height(); y > 0; --y) {
            pos.setY(y);
            BlockState state = level.getBlockState(pos);
            if ((state.isSolidRender(level, pos) && !(state.getBlock() instanceof LeavesBlock)) || state.is(Blocks.WATER)) {
                heighest = y;
                break;
            }
        }
        
        return heighest;
    }
}
