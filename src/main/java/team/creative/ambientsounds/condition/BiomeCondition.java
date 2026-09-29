package team.creative.ambientsounds.condition;

import java.util.regex.Pattern;

import net.minecraft.resources.ResourceLocation;

public record BiomeCondition(boolean tag, Pattern pattern) {
    
    public static BiomeCondition of(String name) {
        if (name.startsWith("#"))
            return new BiomeCondition(true, Pattern.compile(".*" + name.substring(1).replace("*", ".*") + ".*"));
        return new BiomeCondition(false, Pattern.compile(".*" + name.replace("*", ".*") + ".*"));
    }
    
    public static BiomeCondition[] of(String[] names) {
        if (names == null)
            return null;
        BiomeCondition[] compiled = new BiomeCondition[names.length];
        for (int i = 0; i < names.length; i++)
            compiled[i] = of(names[i]);
        return compiled;
    }
    
    /** Whether the pattern matches the given biome or biome tag id. The result only depends on both, so it is remembered. */
    public boolean matches(ResourceLocation location) {
        return BiomeMatchCache.matches(pattern, location);
    }
    
}
