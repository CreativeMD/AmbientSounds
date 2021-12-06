package team.creative.ambientsounds.env;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import net.minecraft.world.level.block.state.BlockState;
import team.creative.ambientsounds.AmbientEngine;

public class AirPocket {
    
    public final List<String> blockGroups = new ArrayList<>();
    public final double averageLight;
    
    public AirPocket() {
        averageLight = 15;
    }
    
    public AirPocket(AmbientEngine engine, HashMap<BlockState, Integer> found, double averageLight) {
        this.averageLight = averageLight;
        for (Entry<String, BlockGroup> entry : engine.toScan.entrySet())
            for (BlockState state : found.keySet()) {
                if (entry.getValue().is(state)) {
                    blockGroups.add(entry.getKey());
                    break;
                }
            }
    }
    
    public boolean contains(String[] blocks) {
        for (int i = 0; i < blocks.length; i++)
            if (blockGroups.contains(blocks[i]))
                return true;
        return false;
    }
    
}
