package com.creativemd.ambientsounds;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;

@Mod(modid = AmbientSounds.modid, version = AmbientSounds.version, name = "Ambient Sounds")
public class AmbientSounds {
	
	public static final String modid = "ambientsounds";
	public static final String version = "0.1";
	
	
	@EventHandler
	@SideOnly(Side.CLIENT)
    public void preInit(FMLPreInitializationEvent event)
    {
		Configuration config = new Configuration(event.getSuggestedConfigurationFile());
		config.load();
		for (int i = 0; i < AmbientSound.sounds.size(); i++) {
			if(AmbientSound.sounds.get(i) instanceof BiomesSound)
			{
				BiomesSound sound = (BiomesSound) AmbientSound.sounds.get(i);
				String[] biomes = config.get("biomeSounds", sound.name, toString(sound.biomes)).getString().split(";");
				sound.biomes = biomes;
			}
		}
		config.save();
    }
	
	public String toString(String[] array)
	{
		String result = "";
		for (int i = 0; i < array.length; i++) {
			result += array[i] + ";";
		}
		return result;
	}
	
	@EventHandler
	@SideOnly(Side.CLIENT)
    public void Init(FMLInitializationEvent event)
    {
		if(FMLCommonHandler.instance().getEffectiveSide().isClient())
		{
			MinecraftForge.EVENT_BUS.register(new TickHandler());
			FMLCommonHandler.instance().bus().register(new TickHandler());
		}
    }
	
}
