package com.creativemd.ambientsounds;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

@Mod(modid = AmbientSounds.modid, version = AmbientSounds.version, name = "Ambient Sounds")
public class AmbientSounds {
	
	public static final String modid = "ambientsounds";
	public static final String version = "0.1";
	
	
	@EventHandler
    public void Init(FMLInitializationEvent event)
    {
		if(FMLCommonHandler.instance().getEffectiveSide().isClient())
		{
			MinecraftForge.EVENT_BUS.register(new TickHandler());
			FMLCommonHandler.instance().bus().register(new TickHandler());
		}
    }
	
}
