package team.creative.ambientsounds.block;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Material;
import team.creative.ambientsounds.AmbientSounds;
import team.creative.creativecore.common.util.mc.MaterialUtils;
import team.creative.creativecore.common.util.registry.NamedHandlerRegistry;

public final class AmbientBlockFilters {
    
    public static final NamedHandlerRegistry<Function<String, Predicate<BlockState>>> REGISTRY = new NamedHandlerRegistry<Function<String, Predicate<BlockState>>>(x -> (state -> true));
    
    public static Predicate<BlockState> get(String data) {
        String[] conditions = data.split("&");
        Predicate<BlockState>[] parsed = new Predicate[conditions.length];
        for (int i = 0; i < parsed.length; i++)
            parsed[i] = parse(conditions[i]);
        
        return state -> {
            for (int i = 0; i < parsed.length; i++)
                if (!parsed[i].test(state))
                    return false;
            return true;
        };
    }
    
    private static Predicate<BlockState> parse(String data) {
        String[] parts = data.split("->");
        if (parts.length == 1) {
            Block block = Registry.BLOCK.get(new ResourceLocation(parts[0]));
            return state -> state.getBlock() == block;
        } else if (parts.length != 2) {
            AmbientSounds.LOGGER.error("Found invalid block filter '{}'. It will be ignored", data);
            return state -> true;
        }
        
        try {
            return REGISTRY.getOrThrow(parts[0]).apply(parts[1]);
        } catch (IllegalArgumentException e) {
            AmbientSounds.LOGGER.error("Found invalid block filter type {}. '{}' will be ignored", parts[0], data);
            return state -> true;
        }
    }
    
    static {
        REGISTRY.register("m", data -> {
            Material material = MaterialUtils.getMaterial(data);
            return state -> state.getMaterial() == material;
        });
        REGISTRY.register("t", data -> {
            TagKey<Block> tag = TagKey.create(Registry.BLOCK_REGISTRY, new ResourceLocation(data));
            return state -> state.is(tag);
        });
        REGISTRY.register("p", data -> {
            String[] parts = data.split("=");
            if (parts.length != 2) {
                AmbientSounds.LOGGER.error("Found invalid property condition '{}'. It will be ignored", data);
                return state -> true;
            }
            
            return state -> {
                Property<? extends Comparable> property = state.getBlock().getStateDefinition().getProperty(parts[0]);
                if (property == null)
                    return false;
                Optional value = property.getValue(parts[1]);
                if (value.isPresent())
                    return state.getValue(property).equals(value.get());
                return false;
            };
        });
    }
    
}
