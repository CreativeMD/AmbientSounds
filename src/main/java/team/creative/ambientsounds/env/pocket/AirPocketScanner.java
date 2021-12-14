package team.creative.ambientsounds.env.pocket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import team.creative.ambientsounds.AmbientEngine;
import team.creative.creativecore.common.util.type.list.Pair;
import team.creative.creativecore.common.util.type.list.SingletonList;
import team.creative.creativecore.common.util.type.map.HashMapDouble;
import team.creative.creativecore.common.util.type.set.QuadBitSet;

public class AirPocketScanner extends Thread {
    
    public final AmbientEngine engine;
    public final Level level;
    public final BlockPos origin;
    
    private List<Collection<Pair<BlockPos, BlockPos>>> toScan = new ArrayList<>();
    private final HashMapDouble<BlockState> foundPercentage = new HashMapDouble<>();
    private final HashMapDouble<BlockState> foundCount = new HashMapDouble<>();
    private QuadBitSet sky = new QuadBitSet();
    
    private double distributionCounter;
    private int totalSize = 0;
    private int currentDistance = 0;
    private int lightValueCounter = 0;
    private int skyLightValueCounter = 0;
    private int blocks = 0;
    private int air;
    
    private Consumer<AirPocket> consumer;
    
    public AirPocketScanner(AmbientEngine engine, Level level, BlockPos origin, Consumer<AirPocket> consumer) {
        this.origin = origin;
        this.engine = engine;
        this.level = level;
        this.consumer = consumer;
        start();
    }
    
    @Override
    public void run() {
        this.toScan.add(new SingletonList<Pair<BlockPos, BlockPos>>(new Pair<>(origin, origin)));
        while (currentDistance < toScan.size()) {
            for (Pair<BlockPos, BlockPos> pos : toScan.get(currentDistance))
                scan(level, currentDistance, pos.key, pos.value);
            currentDistance++;
        }
        foundCount.putAll(foundPercentage);
        foundPercentage.scale(1 / distributionCounter);
        consumer.accept(new AirPocket(engine, foundPercentage, foundCount, lightValueCounter / (double) blocks, skyLightValueCounter / (double) blocks, air / (double) engine.maxAirPocketCount));
    }
    
    protected HashSet<Pair<BlockPos, BlockPos>> getOrCreate(int distance) {
        if (distance >= toScan.size()) {
            HashSet<Pair<BlockPos, BlockPos>> set = new HashSet<>();
            toScan.add(set);
            return set;
        }
        return (HashSet<Pair<BlockPos, BlockPos>>) toScan.get(distance);
    }
    
    protected void findState(BlockState state, int distance) {
        double factor = engine.airWeightFactor(distance);
        distributionCounter += factor;
        foundPercentage.put(state, factor);
    }
    
    protected void scan(Level level, int distance, BlockPos pos, BlockPos from) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !state.isCollisionShapeFullBlock(level, pos)) {
            if (!state.isAir())
                findState(state, distance);
            if (distance < engine.airPocketDistance && air < engine.maxAirPocketCount)
                air++;
            distance++;
            if (sky.get(pos.getX(), pos.getZ()) && sky.get(pos.getX() - 1, pos.getZ()) && sky.get(pos.getX(), pos.getZ() - 1) && sky.get(pos.getX() + 1, pos.getZ()) && sky
                    .get(pos.getX(), pos.getZ() + 1))
                return;
            for (int i = 0; i < Direction.values().length; i++) {
                if (totalSize > engine.airPocketCount)
                    return;
                
                Direction direction = Direction.values()[i];
                Axis axis = direction.getAxis();
                if (direction.getAxisDirection() == AxisDirection.POSITIVE) {
                    if (pos.get(axis) + 1 <= origin.get(axis))
                        continue;
                } else if (pos.get(axis) - 1 >= origin.get(axis))
                    continue;
                getOrCreate(distance).add(new Pair<>(pos.relative(direction), pos));
                totalSize++;
            }
        } else {
            if (!sky.get(pos.getX(), pos.getZ()) && level.canSeeSky(pos.above())) {
                sky.set(pos.getX(), pos.getZ());
                if (distance < engine.airPocketDistance)
                    air = engine.maxAirPocketCount;
            }
            lightValueCounter += level.getLightEmission(from);
            skyLightValueCounter += level.getBrightness(LightLayer.SKY, from);
            blocks++;
            findState(state, distance);
        }
    }
}
