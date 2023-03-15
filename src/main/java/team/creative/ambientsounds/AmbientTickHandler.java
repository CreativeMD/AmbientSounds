package team.creative.ambientsounds;

import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import team.creative.ambientsounds.env.AmbientEnvironment;
import team.creative.ambientsounds.sound.AmbientSoundEngine;
import team.creative.creativecore.CreativeCore;
import team.creative.creativecore.Side;
import team.creative.creativecore.common.config.holder.ConfigHolderDynamic;
import team.creative.creativecore.common.config.holder.CreativeConfigRegistry;
import team.creative.creativecore.common.config.sync.ConfigSynchronization;
import team.creative.creativecore.common.util.type.list.Pair;
import team.creative.creativecore.reflection.ReflectionHelper;

public class AmbientTickHandler {
    
    private static Minecraft mc = Minecraft.getInstance();
    
    public AmbientSoundEngine soundEngine;
    public AmbientEnvironment environment = null;
    public AmbientEngine engine;
    public int timer = 0;
    
    public boolean showDebugInfo = false;
    private boolean shouldReload = false;
    
    public void scheduleReload() {
        shouldReload = true;
    }
    
    public void setEngine(AmbientEngine engine) {
        this.engine = engine;
        initConfiguration();
    }
    
    public void initConfiguration() {
        CreativeConfigRegistry.ROOT.removeField(AmbientSounds.MODID);
        
        ConfigHolderDynamic holder = CreativeConfigRegistry.ROOT.registerFolder(AmbientSounds.MODID, ConfigSynchronization.CLIENT);
        
        holder.registerValue("general", AmbientSounds.CONFIG);
        
        if (engine == null)
            return;
        
        ConfigHolderDynamic dimensions = holder.registerFolder("dimensions");
        Field dimensionField = ReflectionHelper.findField(AmbientDimension.class, "volumeSetting");
        for (AmbientDimension dimension : engine.dimensions.values())
            dimensions.registerField(dimension.name, dimensionField, dimension);
        
        ConfigHolderDynamic regions = holder.registerFolder("regions");
        Field regionField = ReflectionHelper.findField(AmbientRegion.class, "volumeSetting");
        Field soundField = ReflectionHelper.findField(AmbientSound.class, "volumeSetting");
        for (Entry<String, AmbientRegion> pair : engine.allRegions.entrySet()) {
            ConfigHolderDynamic region = regions.registerFolder(pair.getKey().replace(".", "_"));
            region.registerField("overall", regionField, pair.getValue());
            if (pair.getValue().sounds != null)
                for (AmbientSound sound : pair.getValue().sounds.values())
                    region.registerField(sound.name, soundField, sound);
        }
        
        holder.registerField("fade-volume", ReflectionHelper.findField(AmbientEngine.class, "fadeVolume"), engine);
        holder.registerField("fade-pitch", ReflectionHelper.findField(AmbientEngine.class, "fadePitch"), engine);
        holder.registerField("silent-dimensions", ReflectionHelper.findField(AmbientEngine.class, "silentDimensions"), engine);
        
        CreativeCore.CONFIG_HANDLER.load(AmbientSounds.MODID, Side.CLIENT);
    }
    
    public static final DecimalFormat df = new DecimalFormat("0.##");
    
    private String format(Object value) {
        if (value instanceof Double || value instanceof Float)
            return df.format(value);
        return value.toString();
    }
    
    private String format(List<Pair<String, Object>> details) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Pair<String, Object> pair : details) {
            if (!first)
                builder.append(",");
            else
                first = false;
            if (pair.key.isEmpty())
                builder.append(format(pair.value));
            else
                builder.append(ChatFormatting.YELLOW + pair.key + ChatFormatting.RESET + ":" + format(pair.value));
        }
        return builder.toString();
    }
    
    public void onRender() {
        if (showDebugInfo && engine != null && !mc.isPaused() && environment != null && mc.level != null) {
            List<String> list = new ArrayList<>();
            
            List<Pair<String, Object>> details = new ArrayList<>();
            engine.collectDetails(details);
            
            details.add(new Pair<>("playing", engine.soundEngine.playingCount()));
            details.add(new Pair<>("dim-name", mc.level.dimension().location()));
            
            list.add(format(details));
            details.clear();
            
            environment.collectLevelDetails(details);
            
            list.add(format(details));
            details.clear();
            
            environment.collectPlayerDetails(details, mc.player);
            
            list.add(format(details));
            details.clear();
            
            environment.collectTerrainDetails(details);
            
            list.add(format(details));
            details.clear();
            
            environment.collectBiomeDetails(details);
            
            list.add(format(details));
            details.clear();
            
            for (AmbientRegion region : engine.activeRegions) {
                
                details.add(new Pair<>("region", ChatFormatting.DARK_GREEN + region.name + ChatFormatting.RESET));
                details.add(new Pair<>("playing", region.playing.size()));
                
                list.add(format(details));
                
                details.clear();
                for (AmbientSound sound : region.playing) {
                    
                    if (!sound.isPlaying())
                        continue;
                    
                    String text = "";
                    if (sound.stream1 != null) {
                        details.add(new Pair<>("n", sound.stream1.location));
                        details.add(new Pair<>("v", sound.stream1.volume));
                        details.add(new Pair<>("i", sound.stream1.index));
                        details.add(new Pair<>("p", sound.stream1.pitch));
                        details.add(new Pair<>("t", sound.stream1.ticksPlayed));
                        details.add(new Pair<>("d", sound.stream1.duration));
                        
                        text = "[" + format(details) + "]";
                        
                        details.clear();
                    }
                    
                    if (sound.stream2 != null) {
                        details.add(new Pair<>("n", sound.stream2.location));
                        details.add(new Pair<>("v", sound.stream2.volume));
                        details.add(new Pair<>("i", sound.stream2.index));
                        details.add(new Pair<>("p", sound.stream2.pitch));
                        details.add(new Pair<>("t", sound.stream2.ticksPlayed));
                        details.add(new Pair<>("d", sound.stream2.duration));
                        
                        text += "[" + format(details) + "]";
                        
                        details.clear();
                    }
                    
                    list.add(text);
                }
            }
            RenderSystem.defaultBlendFunc();
            PoseStack mat = new PoseStack();
            Font font = mc.font;
            int top = 2;
            for (String msg : list) {
                if (msg != null && !msg.isEmpty()) {
                    GuiComponent.fill(mat, 1, top - 1, 2 + font.width(msg) + 1, top + font.lineHeight - 1, -1873784752);
                    font.draw(mat, msg, 2, top, 14737632);
                }
                top += font.lineHeight;
            }
        }
    }
    
    public void loadLevel(LevelAccessor level) {
        if (level.isClientSide() && engine != null)
            engine.onClientLoad();
    }
    
    public void onTick() {
        if (soundEngine == null) {
            soundEngine = new AmbientSoundEngine();
            if (engine == null)
                setEngine(AmbientEngine.loadAmbientEngine(soundEngine));
            if (engine != null)
                engine.soundEngine = soundEngine;
        }
        
        if (shouldReload) {
            AmbientSounds.reload();
            shouldReload = false;
        }
        
        if (engine == null)
            return;
        
        Level level = mc.level;
        Player player = mc.player;
        
        if (level != null && player != null && !mc.isPaused() && mc.options.getSoundSourceVolume(SoundSource.AMBIENT) > 0) {
            
            if (environment == null)
                environment = new AmbientEnvironment();
            
            AmbientDimension newDimension = engine.getDimension(level);
            if (environment.dimension != newDimension) {
                engine.changeDimension(environment, newDimension);
                environment.dimension = newDimension;
            }
            
            if (timer % engine.environmentTickTime == 0)
                environment.analyzeSlow(newDimension, engine, player, level, timer);
            
            if (timer % engine.soundTickTime == 0) {
                environment.analyzeFast(newDimension, player, level, mc.getDeltaFrameTime());
                environment.dimension.manipulateEnviroment(environment);
                
                engine.tick(environment);
            }
            
            engine.fastTick(environment);
            
            timer++;
        } else if (!engine.activeRegions.isEmpty())
            engine.stopEngine();
    }
    
}
