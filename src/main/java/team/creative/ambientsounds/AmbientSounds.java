package team.creative.ambientsounds;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fmllegacy.network.FMLNetworkConstants;
import team.creative.creativecore.client.CreativeCoreClient;
import team.creative.creativecore.client.command.ClientCommandRegistry;

@Mod(value = AmbientSounds.MODID)
public class AmbientSounds {
    
    public static final Logger LOGGER = LogManager.getLogger(AmbientSounds.MODID);
    
    public static final String MODID = "ambientsounds";
    
    public AmbientSounds() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
    }
    
    public static AmbientTickHandler tickHandler;
    
    private void doClientStuff(final FMLClientSetupEvent event) {
        ModLoadingContext.get()
                .registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        
        tickHandler = new AmbientTickHandler();
        MinecraftForge.EVENT_BUS.register(tickHandler);
        
        Minecraft minecraft = Minecraft.getInstance();
        ReloadableResourceManager reloadableResourceManager = (ReloadableResourceManager) minecraft.getResourceManager();
        
        reloadableResourceManager.registerReloadListener(new SimplePreparableReloadListener() {
            
            @Override
            protected void apply(Object p_10793_, ResourceManager p_10794_, ProfilerFiller p_10795_) {
                if (tickHandler.engine != null)
                    tickHandler.engine.stopEngine();
                tickHandler.setEngine(AmbientEngine.loadAmbientEngine(tickHandler.soundEngine));
            }
            
            @Override
            protected Object prepare(ResourceManager p_10796_, ProfilerFiller p_10797_) {
                return null;
            }
        });
        event.enqueueWork(() -> {
            ClientCommandRegistry.register(LiteralArgumentBuilder.<SharedSuggestionProvider>literal("ambient-debug").executes(x -> {
                tickHandler.showDebugInfo = !tickHandler.showDebugInfo;
                return Command.SINGLE_SUCCESS;
            }));
            ClientCommandRegistry.register(LiteralArgumentBuilder.<SharedSuggestionProvider>literal("ambient-reload").executes(x -> {
                if (tickHandler.engine != null)
                    tickHandler.engine.stopEngine();
                tickHandler.setEngine(AmbientEngine.loadAmbientEngine(tickHandler.soundEngine));
                return Command.SINGLE_SUCCESS;
            }));
        });
        
        CreativeCoreClient.registerClientConfig(MODID);
    }
    
}
