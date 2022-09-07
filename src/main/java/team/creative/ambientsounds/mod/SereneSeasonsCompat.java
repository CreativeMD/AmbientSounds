package team.creative.ambientsounds.mod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import team.creative.creativecore.reflection.ReflectionHelper;

public class SereneSeasonsCompat {
    
    private static final Method getBiomeTemperature;
    
    static {
        Method temp = null;
        try {
            Class clazz = Class.forName("sereneseasons.season.SeasonHooks");
            temp = ReflectionHelper.findMethod(clazz, "getBiomeTemperature", Level.class, Holder.class, BlockPos.class);
        } catch (Exception e) {}
        getBiomeTemperature = temp;
    }
    
    public static float getTemperature(Player player) {
        Level level = player.level;
        Holder<Biome> biome = level.getBiome(player.blockPosition());
        if (getBiomeTemperature != null)
            try {
                return (float) getBiomeTemperature.invoke(null, level, biome, player.blockPosition());
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        return biome.value().getBaseTemperature();
    }
    
}
