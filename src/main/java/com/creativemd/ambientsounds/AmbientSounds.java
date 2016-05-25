package com.creativemd.ambientsounds;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod(modid = AmbientSounds.modid, version = AmbientSounds.version, name = "Ambient Sounds")
public class AmbientSounds {
	
	public static final String modid = "ambientsounds";
	public static final String version = "1.1";
	
	
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
