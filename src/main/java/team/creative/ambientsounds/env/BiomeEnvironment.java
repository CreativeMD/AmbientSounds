package team.creative.ambientsounds.env;

import java.util.Iterator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.Precipitation;
import team.creative.ambientsounds.AmbientEngine;
import team.creative.ambientsounds.AmbientVolume;
import team.creative.ambientsounds.env.BiomeEnvironment.BiomeArea;
import team.creative.creativecore.client.CreativeCoreClient;
import team.creative.creativecore.common.util.type.list.Pair;
import team.creative.creativecore.common.util.type.list.PairList;

public class BiomeEnvironment implements Iterable<Pair<BiomeArea, AmbientVolume>> {
    
    private final PairList<BiomeArea, AmbientVolume> biomes = new PairList<>();
    private double highestRainVolume;
    
    public BiomeEnvironment() {}
    
    public BiomeEnvironment(AmbientEngine engine, Player player, Level level, AmbientVolume volume) {
        highestRainVolume = 0;
        if (volume.volume() > 0.0) {
            BlockPos center = BlockPos.containing(player.getEyePosition(CreativeCoreClient.getFrameTime()));
            MutableBlockPos pos = new MutableBlockPos();
            for (int x = -engine.biomeScanCount; x <= engine.biomeScanCount; x++) {
                for (int z = -engine.biomeScanCount; z <= engine.biomeScanCount; z++) {
                    pos.set(center.getX() + x * engine.biomeScanDistance, center.getY(), center.getZ() + z * engine.biomeScanDistance);
                    Holder<Biome> holder = level.getBiome(pos);
                    
                    float biomeConditionVolume = (float) ((1 - center.distSqr(pos) / engine.squaredBiomeDistance) * volume.conditionVolume());
                    
                    if (level.isRaining() && holder.value().getPrecipitationAt(pos) == Precipitation.RAIN)
                        highestRainVolume = Math.max(highestRainVolume, biomeConditionVolume * volume.settingVolume());
                    
                    BiomeArea area = new BiomeArea(level, holder, pos);
                    Pair<BiomeArea, AmbientVolume> before = biomes.getPair(area);
                    if (before == null)
                        biomes.add(area, new AmbientVolume(biomeConditionVolume, volume.settingVolume()));
                    else if (before.value.conditionVolume() < biomeConditionVolume)
                        before.value.setConditionVolumeDirect(biomeConditionVolume);
                }
            }
            
            biomes.sort((x, y) -> y.value.compareTo(x.value));
        }
    }
    
    @Override
    public Iterator<Pair<BiomeArea, AmbientVolume>> iterator() {
        return biomes.iterator();
    }
    
    public double rainVolume() {
        return highestRainVolume;
    }
    
    public static class BiomeArea {
        
        public final Holder<Biome> biome;
        public final ResourceLocation location;
        public final BlockPos pos;
        
        public BiomeArea(Level level, Holder<Biome> biome, BlockPos pos) {
            this.biome = biome;
            this.location = level.registryAccess().registryOrThrow(Registries.BIOME).getKey(biome.value());
            this.pos = pos;
        }
        
        public boolean checkBiome(String[] names) {
            for (String name : names)
                if (location.toString().matches(".*" + name.replace("*", ".*") + ".*"))
                    return true;
            return false;
        }
        
        @Override
        public boolean equals(Object object) {
            if (object instanceof BiomeArea)
                return ((BiomeArea) object).biome.equals(biome);
            return false;
        }
        
        @Override
        public int hashCode() {
            return biome.hashCode();
        }
        
    }
    
}
