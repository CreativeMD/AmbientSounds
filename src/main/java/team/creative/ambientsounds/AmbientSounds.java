package team.creative.ambientsounds;

import java.util.function.Predicate;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.Minecraft;
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
		
		reloadableResourceManager.addReloadListener(new ISelectiveResourceReloadListener() {
			
			@Override
			public void onResourceManagerReload(IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate) {
				if (tickHandler.engine != null)
					tickHandler.engine.stopEngine();
				tickHandler.setEngine(AmbientEngine.loadAmbientEngine(tickHandler.soundEngine));
			}
		});
	}
	
}
