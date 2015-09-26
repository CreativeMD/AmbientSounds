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
	
	public static AmbientSound savanna = new BiomesSound("savanna", 0.5F, false).setMinTemperature(0.5F);
	public static AmbientSound savannaNight = new BiomesSound("savanna", "savanna-night", 0.5F, true).setMinTemperature(0.5F);
	public static AmbientSound forest = new BiomesSound(new String[]{"forest", "taiga"}, "forest", 0.5F, false).setMinTemperature(0.1F);
	public static AmbientSound forestNight = new BiomesSound(new String[]{"forest", "taiga"}, "forest-night", 0.5F, true).setMinTemperature(0.1F);
	public static AmbientSound plains = new BiomesSound("plains", 0.5F, false).setMinTemperature(0.1F);
	public static AmbientSound plainsNight = new BiomesSound("plains", "plains-night", 0.5F, true).setMinTemperature(0.1F);
	public static AmbientSound jungle = new BiomesSound("jungle", 0.5F, false).setMinTemperature(0.5F);
	public static AmbientSound jungleNight = new BiomesSound("jungle", "jungle-night", 0.5F, true).setMinTemperature(0.5F);
	public static AmbientSound swampland = new BiomesSound("swampland", 0.5F, false).setMinTemperature(0.3F);
	public static AmbientSound swamplandNight = new BiomesSound("swampland", "swampland-night", 0.5F, true).setMinTemperature(0.3F);
	
	public static AmbientSound beach = new BiomesSound("beach", "beach", 0.5F, false).setIgnoreTime();
	
	public static AmbientSound ocean = new BiomesSound(new String[]{"river", "ocean"}, "ocean", 0.5F, false).setIgnoreTime();
	
	public static AmbientSound snow = new BiomesSound(new String[]{"frozen", "ice", "cold", "desert"}, "snow", 0.7F, false).setIgnoreTime();
	//public static AmbientSound river = new BiomeSound("river", "ocean", 0.5F, false).setIgnoreTime();
	
	public static AmbientSound unterwater = new UnterwaterSound("underwater", 0.5F);
	public static AmbientSound cave = new CaveSound("cave", 0.2F);
	
	
	public IEnhancedPositionSound sound;
	public float volume;
	public float overridenVolume;
	public boolean loaded = false;
	public String name;
	
	public AmbientSound(String name, float volume)
	{
		sounds.add(this);
		this.name = name;
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
			//System.out.println("Stopping sound " + sound.getPositionedSoundLocation().getResourcePath());
			sound.donePlaying = true;
			Minecraft.getMinecraft().getSoundHandler().stopSound(sound);
			resetVolume();
			TickHandler.playing.remove(this);
		}
	}
	
	public boolean canPlaySound()
	{
		return loaded;
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
	
	public float getVolumeFromHeight(int preferedHeight, float height)
	{
		if(height > preferedHeight-1 && height <= preferedHeight)
			return height-(preferedHeight-1);
		if(height > preferedHeight && height < preferedHeight+1)
			return 1-(height-preferedHeight);
		return 0;
	}
	
	/**height: 0 = Underground/Cave, 1 = Biome/ Surface, 2 = Mountain/Windy Air, 3 = Space**/
	public abstract float getVolume(World world, EntityPlayer player, BiomeGenBase biome, boolean isNight, float height);
	
}