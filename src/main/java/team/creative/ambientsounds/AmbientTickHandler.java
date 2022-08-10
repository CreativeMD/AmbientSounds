package team.creative.ambientsounds;

import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.google.common.base.Strings;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import team.creative.ambientsounds.env.AmbientEnviroment;
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
    public AmbientEnviroment enviroment = null;
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
        if (showDebugInfo && engine != null && !mc.isPaused() && enviroment != null && mc.level != null) {
            List<String> list = new ArrayList<>();
            
            List<Pair<String, Object>> details = new ArrayList<>();
            engine.collectDetails(details);
            
            details.add(new Pair<>("playing", engine.soundEngine.playingCount()));
            details.add(new Pair<>("dim-name", mc.level.dimension().location()));
            
            list.add(format(details));
            details.clear();
            
            enviroment.collectLevelDetails(details);
            
            list.add(format(details));
            details.clear();
            
            enviroment.collectPlayerDetails(details, mc.player);
            
            list.add(format(details));
            details.clear();
            
            enviroment.collectTerrainDetails(details);
            
            list.add(format(details));
            details.clear();
            
            enviroment.collectBiomeDetails(details);
            
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
            
            for (int i = 0; i < list.size(); ++i) {
                String s = list.get(i);
                
                if (!Strings.isNullOrEmpty(s)) {
                    int j = mc.font.lineHeight;
                    int k = mc.font.width(s);
                    int i1 = 2 + j * i;
                    PoseStack mat = new PoseStack();
                    drawGradientRect(mat.last().pose(), 0, 1, i1 - 1, 2 + k + 1, i1 + j - 1, -1873784752, -1873784752);
                    mc.font.drawShadow(mat, s, 2, i1, 14737632);
                }
            }
        }
    }
    
    public static void drawGradientRect(Matrix4f mat, int zLevel, int left, int top, int right, int bottom, int startColor, int endColor) {
        float startAlpha = (startColor >> 24 & 255) / 255.0F;
        float startRed = (startColor >> 16 & 255) / 255.0F;
        float startGreen = (startColor >> 8 & 255) / 255.0F;
        float startBlue = (startColor & 255) / 255.0F;
        float endAlpha = (endColor >> 24 & 255) / 255.0F;
        float endRed = (endColor >> 16 & 255) / 255.0F;
        float endGreen = (endColor >> 8 & 255) / 255.0F;
        float endBlue = (endColor & 255) / 255.0F;
        
        RenderSystem.enableDepthTest();
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(mat, right, top, zLevel).color(startRed, startGreen, startBlue, startAlpha).endVertex();
        buffer.vertex(mat, left, top, zLevel).color(startRed, startGreen, startBlue, startAlpha).endVertex();
        buffer.vertex(mat, left, bottom, zLevel).color(endRed, endGreen, endBlue, endAlpha).endVertex();
        buffer.vertex(mat, right, bottom, zLevel).color(endRed, endGreen, endBlue, endAlpha).endVertex();
        tessellator.end();
        
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
    }
    
    public void loadLevel(LevelAccessor level) {
        if (level.isClientSide() && engine != null)
            engine.onClientLoad();
    }
    
    public void onTick() {
        if (soundEngine == null) {
            soundEngine = new AmbientSoundEngine(mc.getSoundManager(), mc.options);
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
            
            if (enviroment == null)
                enviroment = new AmbientEnviroment();
            
            AmbientDimension newDimension = engine.getDimension(level);
            if (enviroment.dimension != newDimension) {
                engine.changeDimension(enviroment, newDimension);
                enviroment.dimension = newDimension;
            }
            
            if (timer % engine.enviromentTickTime == 0)
                enviroment.analyzeSlow(newDimension, engine, player, level, timer);
            
            if (timer % engine.soundTickTime == 0) {
                enviroment.analyzeFast(newDimension, player, level, mc.getDeltaFrameTime());
                enviroment.dimension.manipulateEnviroment(enviroment);
                
                engine.tick(enviroment);
            }
            
            engine.fastTick(enviroment);
            
            timer++;
        } else if (!engine.activeRegions.isEmpty())
            engine.stopEngine();
    }
    
}
