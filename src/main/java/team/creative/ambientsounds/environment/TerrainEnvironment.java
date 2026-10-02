package team.creative.ambientsounds.environment;

import java.util.function.Predicate;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import team.creative.ambientsounds.dimension.AmbientDimension;
import team.creative.ambientsounds.engine.AmbientEngine;
import team.creative.ambientsounds.environment.pocket.AirPocket;
import team.creative.ambientsounds.environment.pocket.AirPocketScanner;
import team.creative.creativecore.client.CreativeCoreClient;
import team.creative.creativecore.client.render.text.DebugTextRenderer;

public class TerrainEnvironment {
    
    /** Every state getHeightBlock stops at passes this (solid render requires canOcclude), so a section whose palette has no such state can be skipped */
    private static final Predicate<BlockState> POSSIBLE_HEIGHT_BLOCK = state -> state.canOcclude() || state.is(BlockTags.LEAVES) || state.getFluidState().is(FluidTags.WATER);
    
    public static int getHeightBlock(Level level, MutableBlockPos pos) {
        int y;
        int heighest = 0;
        
        for (y = getHeightScanStart(level, pos); y > level.getMinY(); --y) {
            pos.setY(y);
            BlockState state = level.getBlockState(pos);
            if (state.isSolidRender() || state.is(BlockTags.LEAVES) || level.getFluidState(pos).is(FluidTags.WATER)) {
                heighest = y;
                break;
            }
        }
        
        return heighest;
    }
    
    /** Highest y getHeightBlock has to start at: the top of the highest section whose palette could contain a height block.
     * Only loaded columns of the client level are bounded, every other case starts at the build limit like before. */
    private static int getHeightScanStart(Level level, MutableBlockPos pos) {
        int start = level.getMaxY();
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        if (level.getClass() != ClientLevel.class || !level.hasChunk(chunkX, chunkZ))
            return start;
        // above the build limit the level reports void air and sections without blocks report air, both must be unable to match
        if (POSSIBLE_HEIGHT_BLOCK.test(Blocks.VOID_AIR.defaultBlockState()) || POSSIBLE_HEIGHT_BLOCK.test(Blocks.AIR.defaultBlockState()))
            return start;
        LevelChunkSection[] sections = level.getChunk(chunkX, chunkZ).getSections();
        for (int i = sections.length - 1; i >= 0; i--)
            if (sections[i] != null && sections[i].maybeHas(POSSIBLE_HEIGHT_BLOCK))
                return SectionPos.sectionToBlockCoord(level.getMinSectionY() + i, 15);
        return level.getMinY() + 1; // nothing to find, the scan still ends at the same y
    }
    
    public double averageHeight;
    
    public int minHeight;
    public int maxHeight;
    
    public AirPocket airPocket = new AirPocket();
    public AirPocketScanner scanner;
    
    public TerrainEnvironment() {
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
        if (scanner == null)
            scanner = new AirPocketScanner(engine, level, BlockPos.containing(player.getEyePosition(CreativeCoreClient.getFrameTime())), x -> {
                airPocket = x;
                scanner = null;
            });
    }
    
    public void collectDetails(DebugTextRenderer text) {
        text.detail("features", airPocket.features.toString(DebugTextRenderer.DECIMAL_FORMAT));
        text.detail("light", airPocket.averageLight);
        text.detail("block-light", airPocket.averageBlockLight);
        text.detail("sky-light", airPocket.averageSkyLight);
        text.detail("air", airPocket.air);
        text.detail("sky", airPocket.sky);
    }
    
}
