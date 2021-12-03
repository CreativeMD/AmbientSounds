package team.creative.ambientsounds.env;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public class BlockGroup {
    
    public List<Tag<Block>> tags = new ArrayList<>();
    public List<Block> blocks = new ArrayList<>();
    
    public void add(String[] data) {
        for (int i = 0; i < data.length; i++) {
            if (data[i].startsWith("t->")) {
                Tag<Block> tag = BlockTags.getAllTags().getTag(new ResourceLocation(data[i].replace("t->", "")));
                if (tag != null)
                    tags.add(tag);
            } else {
                Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(data[i]));
                if (block != null)
                    blocks.add(block);
            }
        }
    }
    
    public boolean is(BlockState state) {
        if (!blocks.isEmpty())
            for (Block block : blocks)
                if (state.is(block))
                    return true;
                
        if (!tags.isEmpty())
            for (Tag<Block> tag : tags)
                if (state.is(tag))
                    return true;
                
        return false;
    }
    
}
