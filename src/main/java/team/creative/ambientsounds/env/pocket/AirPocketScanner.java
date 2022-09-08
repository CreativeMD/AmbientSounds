package team.creative.ambientsounds.env.pocket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import team.creative.ambientsounds.AmbientEngine;
import team.creative.ambientsounds.AmbientSounds;
import team.creative.ambientsounds.env.feature.AmbientBlockGroup;
import team.creative.creativecore.common.util.type.map.HashMapDouble;
import team.creative.creativecore.common.util.type.set.QuadBitSet;

public class AirPocketScanner extends Thread {
    
    public final AmbientEngine engine;
    public final Level level;
    public final BlockPos origin;
    
    private List<HashMap<BlockPosInspection, BlockPosInspection>> toScan = new ArrayList<>();
    private final HashMapDouble<BlockState> foundCount = new HashMapDouble<>();
    private QuadBitSet sky = new QuadBitSet();
    
    private double distributionCounter;
    private int totalSize = 0;
    private int currentDistance = 0;
    private int lightValueCounter = 0;
    private int skyLightValueCounter = 0;
    private int faceCounter = 0;
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
        HashMap<BlockPosInspection, BlockPosInspection> first = new HashMap<>();
        BlockPosInspection ins = new BlockPosInspection(origin);
        first.put(ins, ins);
        this.toScan.add(first);
        int steps = 0;
        while (currentDistance < toScan.size()) {
            for (BlockPosInspection pos : toScan.get(currentDistance).keySet()) {
                scan(level, currentDistance, pos);
                steps++;
                if (steps > AmbientSounds.CONFIG.scanStepAmount) {
                    steps = 0;
                    try {
                        sleep(1);
                    } catch (InterruptedException e) {}
                }
            }
            currentDistance++;
        }
        
        HashMap<String, BlockDistribution> distribution = new HashMap<>();
        for (Entry<String, AmbientBlockGroup> entry : engine.groups.entrySet()) {
            BlockDistribution dist = new BlockDistribution();
            for (Entry<BlockState, Double> state : foundCount.entrySet())
                if (entry.getValue().is(state.getKey()))
                    dist.add(state.getValue());
            dist.calculatePercentage(distributionCounter);
            distribution.put(entry.getKey(), dist);
        }
        
        consumer.accept(new AirPocket(engine, distribution, lightValueCounter / (double) faceCounter, skyLightValueCounter / (double) faceCounter, air / (double) engine.maxAirPocketCount));
    }
    
    protected HashMap<BlockPosInspection, BlockPosInspection> getOrCreate(int distance) {
        if (distance >= toScan.size()) {
            HashMap<BlockPosInspection, BlockPosInspection> set = new HashMap<>();
            toScan.add(set);
            return set;
        }
        return toScan.get(distance);
    }
    
    protected void findState(BlockState state, int distance) {
        double factor = engine.airWeightFactor(distance);
        distributionCounter += factor;
        foundCount.put(state, factor);
    }
    
    protected void scan(Level level, int distance, BlockPosInspection pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !(state.isCollisionShapeFullBlock(level, pos) || engine.considerSolid.is(state))) {
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
                HashMap<BlockPosInspection, BlockPosInspection> map = getOrCreate(distance);
                BlockPos newPos = pos.relative(direction);
                BlockPosInspection ins = map.get(newPos);
                if (ins != null)
                    ins.add(direction.getOpposite());
                else {
                    ins = new BlockPosInspection(newPos, direction.getOpposite());
                    map.put(ins, ins);
                }
                totalSize++;
            }
        } else {
            if (!sky.get(pos.getX(), pos.getZ()) && pos.isUp() && level.canSeeSky(pos.above())) {
                sky.set(pos.getX(), pos.getZ());
                if (distance < engine.airPocketDistance)
                    air = engine.maxAirPocketCount;
            }
            for (Direction direction : pos) {
                BlockPos other = pos.relative(direction);
                lightValueCounter += level.getLightEmission(other);
                skyLightValueCounter += level.getBrightness(LightLayer.SKY, other);
                faceCounter++;
            }
            findState(state, distance);
        }
    }
    
    public static class BlockPosInspection extends BlockPos implements Iterable<Direction> {
        
        protected boolean east;
        protected boolean west;
        protected boolean up;
        protected boolean down;
        protected boolean south;
        protected boolean north;
        
        public BlockPosInspection(BlockPos pos) {
            super(pos);
            this.east = this.west = this.up = this.down = this.south = this.north = true;
        }
        
        public BlockPosInspection(BlockPos pos, Direction direction) {
            super(pos);
            add(direction);
        }
        
        public void add(Direction direction) {
            switch (direction) {
                case DOWN:
                    this.down = true;
                    break;
                case EAST:
                    this.east = true;
                    break;
                case NORTH:
                    this.north = true;
                    break;
                case SOUTH:
                    this.south = true;
                    break;
                case UP:
                    this.up = true;
                    break;
                case WEST:
                    this.west = true;
                    break;
                default:
                    break;
                
            }
        }
        
        public boolean isUp() {
            return up;
        }
        
        public boolean is(Direction direction) {
            switch (direction) {
                case DOWN:
                    return down;
                case EAST:
                    return east;
                case NORTH:
                    return north;
                case SOUTH:
                    return south;
                case UP:
                    return up;
                case WEST:
                    return west;
                default:
                    return false;
            }
        }
        
        @Override
        public Iterator<Direction> iterator() {
            return new Iterator<Direction>() {
                
                int next = findNext(-1);
                
                int findNext(int next) {
                    while (next < 6) {
                        next++;
                        switch (next) {
                            case 0:
                                if (east)
                                    return next;
                                break;
                            case 1:
                                if (west)
                                    return next;
                                break;
                            case 2:
                                if (up)
                                    return next;
                                break;
                            case 3:
                                if (down)
                                    return next;
                                break;
                            case 4:
                                if (south)
                                    return next;
                                break;
                            case 5:
                                if (north)
                                    return next;
                                break;
                        }
                    }
                    return next;
                }
                
                @Override
                public boolean hasNext() {
                    return next >= 0 && next < 6;
                }
                
                @Override
                public Direction next() {
                    Direction result;
                    switch (next) {
                        case 0:
                            result = Direction.EAST;
                            break;
                        case 1:
                            result = Direction.WEST;
                            break;
                        case 2:
                            result = Direction.UP;
                            break;
                        case 3:
                            result = Direction.DOWN;
                            break;
                        case 4:
                            result = Direction.SOUTH;
                            break;
                        case 5:
                            result = Direction.NORTH;
                            break;
                        default:
                            result = null;
                            break;
                    }
                    next = findNext(next);
                    return result;
                }
            };
        }
        
    }
}
