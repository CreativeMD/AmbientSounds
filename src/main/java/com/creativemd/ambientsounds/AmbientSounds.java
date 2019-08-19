package com.creativemd.ambientsounds;

import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.resources.IResourceManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.resource.IResourceType;
import net.minecraftforge.resource.ISelectiveResourceReloadListener;

@Mod(value = "ambientsounds")
public class AmbientSounds {
	
	public static final Logger LOGGER = LogManager.getLogger();
	
	public AmbientSounds() {
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::preInit);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
	}
	
	//public static CommentedConfig config;
	public static int streamingChannels = 11;
	public static int normalChannels = 21;
	
	public static AmbientTickHandler tickHandler;
	
	private void preInit(final FMLCommonSetupEvent event) {
		tickHandler = new AmbientTickHandler();
		MinecraftForge.EVENT_BUS.register(tickHandler);
	}
	
	private void doClientStuff(final FMLClientSetupEvent event) {
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
