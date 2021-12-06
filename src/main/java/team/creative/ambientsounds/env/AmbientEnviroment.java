package team.creative.ambientsounds.env;

import java.util.List;
import java.util.Map.Entry;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.Material;
import team.creative.ambientsounds.AmbientDimension;
import team.creative.ambientsounds.AmbientEngine;
import team.creative.ambientsounds.AmbientTickHandler;
import team.creative.ambientsounds.env.BiomeEnviroment.BiomeArea;
import team.creative.creativecore.common.util.type.Pair;

public class AmbientEnviroment {
    
    public AmbientDimension dimension;
    
    public boolean muted = false;
    
    public boolean night;
    /** 0: night, 1: day ... has smooth transition */
    public double time;
    
    public boolean overallRaining;
    public boolean raining;
    public boolean snowing;
    public boolean thundering;
    
    public BiomeEnviroment biome = new BiomeEnviroment();
    public TerrainEnviroment terrain = new TerrainEnviroment();
    
    public double biomeVolume = 0;
    
    public double absoluteHeight;
    public double relativeHeight;
    public double relativeMinHeight;
    public double relativeMaxHeight;
    public double underwater;
    
    public AmbientEnviroment() {}
    
    public void analyzeFast(AmbientDimension dimension, Player player, Level level, float deltaTime) {
        this.dimension = dimension;
        this.overallRaining = level.isRaining();
        this.raining = level.isRainingAt(player.blockPosition());
        this.snowing = level.getBiome(player.blockPosition()).getPrecipitation() == Biome.Precipitation.SNOW && level.isRaining();
        this.thundering = level.isThundering();
        
        this.absoluteHeight = player.getEyeY();
        this.relativeHeight = absoluteHeight - terrain.averageHeight;
        this.relativeMinHeight = absoluteHeight - terrain.minHeight;
        this.relativeMaxHeight = absoluteHeight - terrain.maxHeight;
        
        analyzeUnderwater(player, level);
        analyzeTime(level, deltaTime);
    }
    
    public void analyzeTime(Level level, float deltaTime) {
        double sunAngle = Math.toDegrees(level.getSunAngle(deltaTime));
        this.night = sunAngle > 90 && sunAngle < 270;
        double fadeTime = 10;
        if (sunAngle > 90 - fadeTime && sunAngle < 90 + fadeTime)
            this.time = Math.min((sunAngle - (90 - fadeTime)) / (fadeTime * 2), 1);
        else if (sunAngle > 270 - fadeTime && sunAngle < 270 + fadeTime)
            this.time = Math.max(1 - ((sunAngle - (270 - fadeTime)) / (fadeTime * 2)), 0);
        else if (night)
            this.time = 0;
        else
            this.time = 1;
    }
    
    public void analyzeUnderwater(Player player, Level level) {
        int depth = 0;
        if (player.isEyeInFluid(FluidTags.WATER)) {
            BlockPos blockpos = new BlockPos(player.blockPosition());
            while (level.getBlockState(blockpos).getMaterial() == Material.WATER) {
                depth++;
                blockpos = blockpos.above();
            }
            depth--;
        }
        this.underwater = depth;
    }
    
    public void analyzeSlow(AmbientDimension dimension, AmbientEngine engine, Player player, Level level, float deltaTime) {
        terrain = new TerrainEnviroment(engine, dimension, player, level);
        biome = new BiomeEnviroment(engine, player, level, biomeVolume);
    }
    
    public void collectLevelDetails(List<Pair<String, Object>> details) {
        details.add(new Pair<>("dimension", dimension));
        details.add(new Pair<>("night", night));
        details.add(new Pair<>("rain", raining));
        details.add(new Pair<>("worldRain", overallRaining));
        details.add(new Pair<>("snow", snowing));
        details.add(new Pair<>("storm", thundering));
        details.add(new Pair<>("time", time));
        details.add(new Pair<>("underwater", underwater));
        
    }
    
    public void collectTerrainDetails(List<Pair<String, Object>> details, Player player) {
        details.add(new Pair<>("features", terrain.airPocket.blockGroups));
        details.add(new Pair<>("height", AmbientTickHandler.df.format(relativeHeight) + "," + AmbientTickHandler.df.format(terrain.averageHeight) + "," + AmbientTickHandler.df
                .format(player.getEyeY() - terrain.averageHeight) + "," + AmbientTickHandler.df.format(player.getEyeY() - terrain.maxHeight)));
    }
    
    public void collectBiomeDetails(List<Pair<String, Object>> details) {
        details.add(new Pair<>("b-volume", biomeVolume));
        for (Entry<BiomeArea, Float> pair : biome.biomes.entrySet())
            details.add(new Pair<>(pair.getKey().biome.getRegistryName().toString(), pair.getValue()));
    }
    
}
