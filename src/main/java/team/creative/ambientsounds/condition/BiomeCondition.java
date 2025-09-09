package team.creative.ambientsounds.condition;

import java.util.regex.Pattern;

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
    
}
