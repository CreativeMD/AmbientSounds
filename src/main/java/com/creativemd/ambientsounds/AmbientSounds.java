package com.creativemd.ambientsounds;

import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.resources.IResourceManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.resource.IResourceType;
import net.minecraftforge.resource.ISelectiveResourceReloadListener;

@Mod(value = "ambientsounds")
public class AmbientSounds {
	
	public static final Logger LOGGER = LogManager.getLogger();
	
	public AmbientSounds() {
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::modConfig);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
	}
	
	private void modConfig(ModConfigEvent event) {
		/* config = event.getConfig().getConfigData();
		 * 
		 * debugging = config.getBoolean("debugging", "Custom", false, "Useful if you want to modify the engine");
		 * streamingChannels = config.getInt("streamingChannels", "engine", streamingChannels, 1, 32, "Streaming + Normal channels may have to be 32 in total.");
		 * normalChannels = config.getInt("normalChannels", "engine", normalChannels, 1, 32, "Streaming + Normal channels may have to be 32 in total."); */
		
	}
	
	//public static CommentedConfig config;
	
	public static int streamingChannels = 11;
	public static int normalChannels = 21;
	
	public static AmbientTickHandler tickHandler;
	
	private void doClientStuff(final FMLClientSetupEvent event) {
		tickHandler = new AmbientTickHandler();
		MinecraftForge.EVENT_BUS.register(tickHandler);
		/* ClientCommandHandler.instance.registerCommand(new CommandBase() {
		 * 
		 * @Override
		 * public String getUsage(ICommandSender sender) {
		 * return "reload ambient sound engine";
		 * }
		 * 
		 * @Override
		 * public String getName() {
		 * return "ambient-reload";
		 * }
		 * 
		 * @Override
		 * public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		 * if (tickHandler.engine != null)
		 * tickHandler.engine.stopEngine();
		 * tickHandler.setEngine(AmbientEngine.loadAmbientEngine(tickHandler.soundEngine));
		 * }
		 * });
		 * 
		 * ClientCommandHandler.instance.registerCommand(new CommandBase() {
		 * 
		 * @Override
		 * public String getUsage(ICommandSender sender) {
		 * return "show ambient engine debug info";
		 * }
		 * 
		 * @Override
		 * public String getName() {
		 * return "ambient-debug";
		 * }
		 * 
		 * @Override
		 * public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		 * tickHandler.showDebugInfo = !tickHandler.showDebugInfo;
		 * }
		 * }); */
		
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
