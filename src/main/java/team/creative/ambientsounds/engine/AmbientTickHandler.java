package team.creative.ambientsounds.engine;

import java.lang.reflect.Field;
import java.util.Map.Entry;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import team.creative.ambientsounds.AmbientSounds;
import team.creative.ambientsounds.dimension.AmbientDimension;
import team.creative.ambientsounds.environment.AmbientEnvironment;
import team.creative.ambientsounds.region.AmbientRegion;
import team.creative.ambientsounds.sound.AmbientSound;
import team.creative.ambientsounds.sound.AmbientSoundCategory;
import team.creative.ambientsounds.sound.AmbientSoundEngine;
import team.creative.creativecore.CreativeCore;
import team.creative.creativecore.Side;
import team.creative.creativecore.client.render.text.DebugTextRenderer;
import team.creative.creativecore.common.config.holder.ConfigHolderDynamic;
import team.creative.creativecore.common.config.holder.CreativeConfigRegistry;
import team.creative.creativecore.common.config.sync.ConfigSynchronization;
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
            if (pair.getValue().loadedSounds != null)
                for (AmbientSound sound : pair.getValue().loadedSounds.values())
                    region.registerField(sound.name, soundField, sound);
        }
        
        ConfigHolderDynamic categories = holder.registerFolder("categories");
        Field categoryField = ReflectionHelper.findField(AmbientSoundCategory.class, "volumeSetting");
        for (AmbientSoundCategory cat : engine.sortedSoundCategories)
            createSoundCategoryConfiguration(categories, cat, categoryField);
        
        holder.registerField("fade-volume", ReflectionHelper.findField(AmbientEngine.class, "fadeVolume"), engine);
        holder.registerField("fade-pitch", ReflectionHelper.findField(AmbientEngine.class, "fadePitch"), engine);
        holder.registerField("silent-dimensions", ReflectionHelper.findField(AmbientEngine.class, "silentDimensions"), engine);
        
        CreativeCore.CONFIG_HANDLER.load(Minecraft.getInstance().level != null ? Minecraft.getInstance().level.registryAccess() : null, AmbientSounds.MODID, Side.CLIENT);
    }
    
    private void createSoundCategoryConfiguration(ConfigHolderDynamic parent, AmbientSoundCategory cat, Field categoryField) {
        if (!cat.children.isEmpty())
            parent = parent.registerFolder(cat.name);
        parent.registerField(cat.name, categoryField, cat);
        for (AmbientSoundCategory child : cat.children)
            createSoundCategoryConfiguration(parent, child, categoryField);
    }
    
    public void onRender(Object object) {
        if (showDebugInfo && engine != null && !mc.isPaused() && environment != null && mc.level != null) {
            GuiGraphics graphics = (GuiGraphics) object;
            
            graphics.nextStratum();
            
            DebugTextRenderer text = new DebugTextRenderer();
            
            engine.collectDetails(text);
            
            text.detail("playing", engine.soundEngine.playingCount()).detail("dim-name", mc.level.dimension().location()).newLine();
            
            environment.collectLevelDetails(text);
            text.newLine();
            
            environment.collectPlayerDetails(text, mc.player);
            text.newLine();
            
            environment.collectTerrainDetails(text);
            text.newLine();
            
            environment.collectBiomeDetails(text);
            text.newLine();
            
            for (AmbientSoundCategory cat : engine.sortedSoundCategories)
                cat.collectDetails(text);
            text.newLine();
            
            for (AmbientRegion region : engine.activeRegions) {
                text.detail("region", ChatFormatting.DARK_GREEN + region.name + ChatFormatting.RESET);
                text.detail("playing", region.playing.size());
                text.newLine();
                
                for (AmbientSound sound : region.playing) {
                    if (!sound.isPlaying())
                        continue;
                    
                    if (sound.stream1 != null)
                        sound.stream1.collectDetails(text);
                    if (sound.stream2 != null)
                        sound.stream2.collectDetails(text);
                    text.newLine();
                }
            }
            
            text.render(mc.font, graphics);
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
            AmbientSounds.reloadAsync();
            shouldReload = false;
        }
        
        if (engine == null)
            return;
        
        Level level = mc.level;
        Player player = mc.player;
        
        if (level != null && player != null && mc.options.getSoundSourceVolume(SoundSource.AMBIENT) > 0) {
            
            if (mc.isPaused())
                return;
            
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
                environment.analyzeFast(newDimension, player, level, mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
                environment.dimension.manipulateEnviroment(environment);
                
                engine.tick(environment);
            }
            
            engine.fastTick(environment);
            
            timer++;
        } else if (!engine.activeRegions.isEmpty())
            engine.stopEngine();
    }
    
}
