package team.creative.ambientsounds.condition;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

import net.minecraft.resources.Identifier;

/** Remembers whether a pattern matches a biome or biome tag id, so each regex runs once per id instead of on every environment scan.
 * Patterns are weak keys: the entries of conditions dropped by a reload can be collected. Which tags a biome has is still read live by the caller. */
class BiomeMatchCache {
    
    private static final int MAX_LOCATIONS_PER_PATTERN = 256;
    private static final ThreadLocal<Map<Pattern, Map<Identifier, Boolean>>> CACHE = ThreadLocal.withInitial(WeakHashMap::new);
    
    static boolean matches(Pattern pattern, Identifier location) {
        Map<Pattern, Map<Identifier, Boolean>> patterns = CACHE.get();
        Map<Identifier, Boolean> locations = patterns.get(pattern);
        if (locations == null) {
            locations = new HashMap<>();
            patterns.put(pattern, locations);
        }
        Boolean result = locations.get(location);
        if (result != null)
            return result;
        boolean matches = pattern.matcher(location.toString()).matches();
        if (locations.size() < MAX_LOCATIONS_PER_PATTERN)
            locations.put(location, matches);
        return matches;
    }
    
}
