package team.creative.ambientsounds;

import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.fml.common.Mod;
import team.creative.ambientsounds.engine.AmbientEngine;
import team.creative.ambientsounds.engine.AmbientTickHandler;
import team.creative.creativecore.CreativeCore;
import team.creative.creativecore.ICreativeLoader;
import team.creative.creativecore.client.ClientLoader;
import team.creative.creativecore.client.CreativeCoreClient;

@Mod(value = AmbientSounds.MODID)
public class AmbientSounds implements ClientLoader {
    
    public static final Logger LOGGER = LogManager.getLogger(AmbientSounds.MODID);
    public static final String MODID = "ambientsounds";
    public static final AmbientSoundsConfig CONFIG = new AmbientSoundsConfig();
    
    public static AmbientTickHandler TICK_HANDLER;
    
    public AmbientSounds() {
        ICreativeLoader loader = CreativeCore.loader();
        loader.registerClient(this);
    }
    
    public static void scheduleReload() {
        TICK_HANDLER.scheduleReload();
    }
    
    public static void reloadAsync() {
        CompletableFuture.runAsync(AmbientSounds::reload, Util.backgroundExecutor());
    }
    
    private static synchronized void reload() {
        if (TICK_HANDLER.engine != null)
            TICK_HANDLER.engine.stopEngine();
        if (TICK_HANDLER.environment != null)
            TICK_HANDLER.environment.reload();
        TICK_HANDLER.setEngine(AmbientEngine.loadAmbientEngine(TICK_HANDLER.soundEngine));
    }
    
    @Override
    public void onInitializeClient() {
        ICreativeLoader loader = CreativeCore.loader();
        loader.registerDisplayTest(() -> loader.ignoreServerNetworkConstant(), (a, b) -> true);
        
        TICK_HANDLER = new AmbientTickHandler();
        loader.registerClientTick(TICK_HANDLER::onTick);
        loader.registerClientRenderGui(TICK_HANDLER::onRender);
        loader.registerLoadLevel(TICK_HANDLER::loadLevel);
        
        Runnable register = () -> {
            Minecraft minecraft = Minecraft.getInstance();
            ReloadableResourceManager reloadableResourceManager = (ReloadableResourceManager) minecraft.getResourceManager();
            
            reloadableResourceManager.registerReloadListener(new SimplePreparableReloadListener<Void>() {
                @Override
                protected Void prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
                    AmbientSounds.reloadAsync();
                    return null;
                }
                
                @Override
                protected void apply(Void object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
                    // NO-OP
                }
            });
        };
        
        if (loader.fabric())
            loader.registerClientStarted(register);
        else
            Minecraft.getInstance().execute(register);
        
        CreativeCoreClient.registerClientConfig(MODID);
    }
    
    @Override
    public <T> void registerClientCommands(CommandDispatcher<T> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<T>literal("ambient-debug").executes(x -> {
            TICK_HANDLER.showDebugInfo = !TICK_HANDLER.showDebugInfo;
            return Command.SINGLE_SUCCESS;
        }));
        dispatcher.register(LiteralArgumentBuilder.<T>literal("ambient-reload").executes(x -> {
            AmbientSounds.reloadAsync();
            return Command.SINGLE_SUCCESS;
        }));
    }
    
}
