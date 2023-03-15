package team.creative.ambientsounds.env;

import java.util.HashMap;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import team.creative.ambientsounds.AmbientDimension;
import team.creative.ambientsounds.AmbientEngine;
import team.creative.ambientsounds.AmbientTickHandler;
import team.creative.ambientsounds.env.BiomeEnvironment.BiomeArea;
import team.creative.ambientsounds.env.BiomeEnvironment.BiomeStats;
import team.creative.ambientsounds.mod.SereneSeasonsCompat;
import team.creative.creativecore.common.util.type.list.Pair;

public class AmbientEnvironment {
    
    public AmbientDimension dimension;
    
    public boolean muted = false;
    
    public boolean night;
    /** 0: night, 1: day ... has smooth transition */
    public double time;
    
    public boolean overallRaining;
    public boolean raining;
    public boolean snowing;
    public boolean thundering;
    
    public BiomeEnvironment biome = new BiomeEnvironment();
    public TerrainEnvironment terrain = new TerrainEnvironment();
    
    public double biomeVolume;
    
    public HashMap<String, Double> biomeTypeVolumes = new HashMap<>();
    
    public double absoluteHeight;
    public double relativeHeight;
    public double relativeMinHeight;
    public double relativeMaxHeight;
    public double underwater;
    
    public double temperature;
    
    public AmbientEnvironment() {}
    
    public void analyzeFast(AmbientDimension dimension, Player player, Level level, float deltaTime) {
        this.dimension = dimension;
        this.overallRaining = level.isRaining();
        this.raining = level.isRainingAt(player.blockPosition());
        this.snowing = level.getBiome(player.blockPosition()).value().shouldSnow(level, player.blockPosition()) && level.isRaining();
        this.thundering = level.isThundering() && !snowing;
        
        this.absoluteHeight = player.getEyeY();
        this.relativeHeight = absoluteHeight - terrain.averageHeight;
        this.relativeMinHeight = absoluteHeight - terrain.minHeight;
        this.relativeMaxHeight = absoluteHeight - terrain.maxHeight;
        
        this.temperature = SereneSeasonsCompat.getTemperature(player);
        
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
            BlockPos blockpos = BlockPos.containing(player.getEyePosition());
            while (level.getFluidState(blockpos).is(FluidTags.WATER)) {
                depth++;
                blockpos = blockpos.above();
            }
        }
        this.underwater = depth;
    }
    
    public void analyzeSlow(AmbientDimension dimension, AmbientEngine engine, Player player, Level level, float deltaTime) {
        terrain.analyze(engine, dimension, player, level);
        biome = new BiomeEnvironment(engine, player, level, biomeVolume);
    }
    
    public void collectLevelDetails(List<Pair<String, Object>> details) {
        details.add(new Pair<>("dimension", dimension));
        details.add(new Pair<>("night", night));
        details.add(new Pair<>("rain", raining));
        details.add(new Pair<>("worldRain", overallRaining));
        details.add(new Pair<>("snow", snowing));
        details.add(new Pair<>("storm", thundering));
        details.add(new Pair<>("time", time));
        
    }
    
    public void collectPlayerDetails(List<Pair<String, Object>> details, Player player) {
        details.add(new Pair<>("underwater", underwater));
        details.add(new Pair<>("temp", temperature));
        details.add(new Pair<>("height", "r:" + AmbientTickHandler.df.format(relativeHeight) + ",a:" + AmbientTickHandler.df
                .format(terrain.averageHeight) + " (" + AmbientTickHandler.df.format(relativeMinHeight) + "," + AmbientTickHandler.df.format(relativeMaxHeight) + ")"));
        
    }
    
    public void collectTerrainDetails(List<Pair<String, Object>> details) {
        details.add(new Pair<>("features", terrain.airPocket.features.toString(AmbientTickHandler.df)));
        details.add(new Pair<>("light", terrain.airPocket.averageLight));
        details.add(new Pair<>("sky-light", terrain.airPocket.averageSkyLight));
        details.add(new Pair<>("air", terrain.airPocket.air));
    }
    
    public void collectBiomeDetails(List<Pair<String, Object>> details) {
        details.add(new Pair<>("b-volume", biomeVolume));
        for (Pair<BiomeArea, BiomeStats> pair : biome)
            details.add(new Pair<>(pair.getKey().location.toString(), pair.getValue()));
    }
    
    public void reload() {
        terrain.scanner = null;
    }
    
}
