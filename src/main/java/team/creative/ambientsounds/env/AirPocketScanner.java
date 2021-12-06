package team.creative.ambientsounds.env;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import team.creative.ambientsounds.AmbientEngine;
import team.creative.creativecore.common.util.type.Pair;
import team.creative.creativecore.common.util.type.SingletonList;

public class AirPocketScanner extends Thread {
    
    public final AmbientEngine engine;
    public final Level level;
    public final BlockPos origin;
    private List<Collection<Pair<BlockPos, BlockPos>>> toScan = new ArrayList<>();
    private final HashMap<BlockState, Integer> found = new HashMap<>();
    private int totalSize = 0;
    private int currentDistance = 0;
    private int lightValueCounter = 0;
    private int blocks = 0;
    
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
        
        consumer.accept(new AirPocket(engine, found, lightValueCounter / (double) blocks));
    }
    
    protected HashSet<Pair<BlockPos, BlockPos>> getOrCreate(int distance) {
        if (distance <= toScan.size()) {
            HashSet<Pair<BlockPos, BlockPos>> set = new HashSet<>();
            toScan.add(set);
            return set;
        }
        return (HashSet<Pair<BlockPos, BlockPos>>) toScan.get(distance);
    }
    
    protected void scan(Level level, int distance, BlockPos pos, BlockPos from) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !state.isCollisionShapeFullBlock(level, pos)) {
            distance++;
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
            lightValueCounter += level.getLightEmission(from);
            blocks++;
            if (!found.containsKey(state))
                found.put(state, distance);
        }
    }
}
