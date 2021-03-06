package team.creative.ambientsounds;

import java.util.function.Predicate;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.resources.IResourceManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import net.minecraftforge.resource.IResourceType;
import net.minecraftforge.resource.ISelectiveResourceReloadListener;
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
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        
        tickHandler = new AmbientTickHandler();
        MinecraftForge.EVENT_BUS.register(tickHandler);
        
        Minecraft minecraft = Minecraft.getInstance();
        IReloadableResourceManager reloadableResourceManager = (IReloadableResourceManager) minecraft.getResourceManager();
        
        reloadableResourceManager.registerReloadListener(new ISelectiveResourceReloadListener() {
            
            @Override
            public void onResourceManagerReload(IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate) {
                if (tickHandler.engine != null)
                    tickHandler.engine.stopEngine();
                tickHandler.setEngine(AmbientEngine.loadAmbientEngine(tickHandler.soundEngine));
            }
        });
        event.enqueueWork(() -> {
            ClientCommandRegistry.register(LiteralArgumentBuilder.<ISuggestionProvider>literal("ambient-debug").executes(x -> {
                tickHandler.showDebugInfo = !tickHandler.showDebugInfo;
                return Command.SINGLE_SUCCESS;
            }));
            ClientCommandRegistry.register(LiteralArgumentBuilder.<ISuggestionProvider>literal("ambient-reload").executes(x -> {
                if (tickHandler.engine != null)
                    tickHandler.engine.stopEngine();
                tickHandler.setEngine(AmbientEngine.loadAmbientEngine(tickHandler.soundEngine));
                return Command.SINGLE_SUCCESS;
            }));
        });
        
        CreativeCoreClient.registerClientConfig(MODID);
    }
    
}
