package com.creativemd.ambientsounds;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@Mod(modid = AmbientSounds.modid, version = AmbientSounds.version, name = "Ambient Sounds", acceptedMinecraftVersions = "", clientSideOnly = true)
public class AmbientSounds {
	
	public static final String modid = "ambientsounds";
	public static final String version = "2.0.0";
	
	public static final Logger logger = LogManager.getLogger(AmbientSounds.modid);
	
	@EventHandler
	public void loadComplete(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(new TickHandler());
		
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
