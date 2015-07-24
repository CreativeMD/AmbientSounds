package com.creativemd.ambientsounds;

import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

public abstract class AmbientSound {
	
	public static ArrayList<AmbientSound> sounds = new ArrayList<AmbientSound>();
	
	public static AmbientSound savanna = new BiomeSound("savanna", 0.5F, false);
	public static AmbientSound forest = new BiomeSound("forest", 0.5F, false);
	public static AmbientSound forestNight = new BiomeSound("forest", "forest-night", 0.5F, true);
	public static AmbientSound plains = new BiomeSound("plains", 0.5F, false);
	public static AmbientSound plainsNight = new BiomeSound("plains", "plains-night", 0.5F, true);
	public static AmbientSound jungle = new BiomeSound("jungle", 0.5F, false);
	public static AmbientSound jungleNight = new BiomeSound("jungle", "jungle-night", 0.5F, true);
	public static AmbientSound swampland = new BiomeSound("swampland", 0.5F, false);
	public static AmbientSound swamplandNight = new BiomeSound("swampland", "swampland-night", 0.5F, true);
	
	public static AmbientSound ocean = new BiomesSound(new String[]{"river", "ocean"}, "ocean", 0.5F, false).setIgnoreTime();
	//public static AmbientSound river = new BiomeSound("river", "ocean", 0.5F, false).setIgnoreTime();
	
	public static AmbientSound unterwater = new UnterwaterSound("underwater", 0.5F);
	
	
	public IEnhancedPositionSound sound;
	public float volume;
	public float overridenVolume;
	
	public AmbientSound(String name, float volume)
	{
		sounds.add(this);
		this.sound = new IEnhancedPositionSound(new ResourceLocation(AmbientSounds.modid + ":" + name), volume, 1F);
		this.volume = volume;
		this.overridenVolume = volume;
	}
	
	public void resetVolume()
	{
		this.sound.volume = volume;
		this.overridenVolume = volume;
	}
	
	public void setVolume(float volume)
	{
		this.sound.donePlaying = false;
		this.overridenVolume = volume;
		this.sound.volume = volume;
		if(volume <= 0)
		{
			System.out.println("Stopping sound " + sound.getPositionedSoundLocation().getResourcePath());
			sound.donePlaying = true;
			Minecraft.getMinecraft().getSoundHandler().stopSound(sound);
			resetVolume();
			TickHandler.playing.remove(this);
		}
	}
	
	public boolean canPlaySound()
	{
		return true;
	}
	
	public static final float fadeAmount = 0.001F;
	
	public float fadeInAmount()
	{
		return fadeAmount;
	}
	
	public float fadeOutAmount()
	{
		return fadeAmount;
	}
	
	public abstract float getVolume(World world, EntityPlayer player, BiomeGenBase biome, boolean isNight);
	
}