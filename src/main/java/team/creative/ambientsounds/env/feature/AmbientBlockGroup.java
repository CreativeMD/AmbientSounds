package team.creative.ambientsounds.env.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import team.creative.creativecore.common.util.mc.MaterialUtils;

public class AmbientBlockGroup {
    
    public List<TagKey<Block>> tags = new ArrayList<>();
    public List<Block> blocks = new ArrayList<>();
    public List<Material> materials = new ArrayList<>();
    
    public List<String> data = new ArrayList<>();
    
    public void onClientLoad() {
        tags.clear();
        blocks.clear();
        materials.clear();
        
        for (int i = 0; i < data.size(); i++) {
            String entry = data.get(i);
            if (entry.startsWith("t->")) {
                TagKey<Block> tag = TagKey.create(Registry.BLOCK_REGISTRY, new ResourceLocation(entry.replace("t->", "")));
                if (tag != null)
                    tags.add(tag);
            } else if (entry.startsWith("m->")) {
                Material material = MaterialUtils.getMaterial(entry.replace("m->", ""));
                if (material != null)
                    materials.add(material);
            } else {
                Block block = Registry.BLOCK.get(new ResourceLocation(entry));
                if (block != null)
                    blocks.add(block);
            }
        }
    }
    
    public void add(String[] data) {
        this.data.addAll(Arrays.asList(data));
    }
    
    public boolean isEmpty() {
        return blocks.isEmpty() && tags.isEmpty();
    }
    
    public boolean is(BlockState state) {
        if (!blocks.isEmpty())
            for (Block block : blocks)
                if (state.is(block))
                    return true;
                
        if (!tags.isEmpty())
            for (TagKey<Block> tag : tags)
                if (state.is(tag))
                    return true;
                
        if (!materials.isEmpty())
            for (Material material : materials)
                if (state.getMaterial() == material)
                    return true;
                
        return false;
    }
    
}
