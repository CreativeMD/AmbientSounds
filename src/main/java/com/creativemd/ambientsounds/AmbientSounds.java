package com.creativemd.ambientsounds;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import paulscode.sound.SoundSystemConfig;

@SideOnly(Side.CLIENT)
@Mod(modid = AmbientSounds.modid, version = AmbientSounds.version, name = "Ambient Sounds", acceptedMinecraftVersions = "", clientSideOnly = true, guiFactory = "com.creativemd.ambientsounds.AmbientSettings")
public class AmbientSounds {
	
	public static final String modid = "ambientsounds";
	public static final String version = "2.0.0";
	
	public static final Logger logger = LogManager.getLogger(AmbientSounds.modid);
	
	public static Configuration config;
	
	public static boolean debugging;
	public static int streamingChannels = 11;
	public static int normalChannels = 21;
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		config = new Configuration(event.getSuggestedConfigurationFile());
		config.load();
		debugging = config.getBoolean("debugging", "Custom", false, "Useful if you want to modify the engine");
		streamingChannels = config.getInt("streamingChannels", "engine", streamingChannels, 1, 32, "Streaming + Normal channels may have to be 32 in total.");
		normalChannels = config.getInt("normalChannels", "engine", normalChannels, 1, 32, "Streaming + Normal channels may have to be 32 in total.");
		config.save();
		
		MinecraftForge.EVENT_BUS.register(new TickHandler());
	}
	
	@EventHandler
	public void loadComplete(FMLInitializationEvent event) {
		ClientCommandHandler.instance.registerCommand(new CommandBase() {
			
			@Override
			public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
				AmbientSoundLoader.reloadAmbientSounds();
			}

			@Override
			public String getCommandName() {
				return "ambient-reload";
			}

			@Override
			public String getCommandUsage(ICommandSender sender) {
				return "reload ambient sound engine";
			}
		});
		
		
		Minecraft minecraft = Minecraft.getMinecraft();
		IReloadableResourceManager reloadableResourceManager = (IReloadableResourceManager) minecraft.getResourceManager();
		reloadableResourceManager.registerReloadListener(new IResourceManagerReloadListener() {
			@Override
			public void onResourceManagerReload(IResourceManager resourceManager) {
				AmbientSoundLoader.reloadAmbientSounds();
			}
		});
	}
	
}
