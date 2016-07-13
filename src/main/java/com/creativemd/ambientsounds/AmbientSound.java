package com.creativemd.ambientsounds;

import java.util.ArrayList;

import com.creativemd.ambientsounds.WeatherSound.WeatherType;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public abstract class AmbientSound {
	
	public static ArrayList<AmbientSound> sounds = new ArrayList<AmbientSound>();
	
	public static AmbientSound savanna = new BiomesSound(new String[]{"savanna", "heathland", "prairie"}, "savanna", 0.5F, false).setMinTemperature(0.5F);
	public static AmbientSound savannaNight = new BiomesSound(new String[]{"savanna", "heathland", "prairie"}, "savanna-night", 0.5F, true).setMinTemperature(0.5F);
	public static AmbientSound forest = new BiomesSound(new String[]{"forest", "blossom", "land of lakes", "mangrove", "woods", "meadow", "orchard", "origin island", "thicket", "woodland"}, "forest", 0.5F, false).setMinTemperature(0.1F);
	public static AmbientSound forestNight = new BiomesSound(new String[]{"forest", "blossom", "land of lakes", "mangrove", "woods", "meadow", "orchard", "origin island", "thicket", "woodland"}, "forest-night", 0.5F, true).setMinTemperature(0.1F);
	public static AmbientSound taiga = new BiomesSound(new String[]{"taiga", "grove", "jade cliffs", "shield"}, "taiga", 0.5F, false).setMinTemperature(0.1F);
	public static AmbientSound taigaNight = new BiomesSound(new String[]{"taiga", "grove", "jade cliffs", "shield"}, "taiga-night", 0.5F, true).setMinTemperature(0.1F);
	public static AmbientSound plains = new BiomesSound(new String[]{"plains", "chaparral", "fen", "field", "flower", "grass", "oasis", "tundra"}, "plains", 0.5F, false).setMinTemperature(0.1F);
	public static AmbientSound plainsNight = new BiomesSound(new String[]{"plains", "chaparral", "fen", "field", "flower", "grass", "oasis", "tundra"}, "plains-night", 0.5F, true).setMinTemperature(0.1F);
	public static AmbientSound jungle = new BiomesSound(new String[]{"jungle", "sacred springs", "tropical"}, "jungle", 0.5F, false).setMinTemperature(0.5F);
	public static AmbientSound jungleNight = new BiomesSound(new String[]{"jungle", "sacred springs", "tropical"}, "jungle-night", 0.5F, true).setMinTemperature(0.5F);
	public static AmbientSound swampland = new BiomesSound(new String[]{"swampland", "bayou", "bog", "marsh", "moor", "mystic grove", "silkglades", "sludgepit", "wetland"}, "swampland", 0.5F, false).setMinTemperature(0.3F);
	public static AmbientSound swamplandNight = new BiomesSound(new String[]{"swampland", "bayou", "bog", "marsh", "moor", "mystic grove", "silkglades", "sludgepit", "wetland"}, "swampland-night", 0.5F, true).setMinTemperature(0.3F);
	
	public static AmbientSound beach = new BiomesSound(new String[]{"beach", "mangrove"}, "beach", 0.5F, false).setIgnoreTime();
	
	public static AmbientSound ocean = new BiomesSound(new String[]{"river", "ocean"}, "ocean", 0.5F, false).setIgnoreTime();
	
	public static AmbientSound snow = new BiomesSound(new String[]{"frozen", "ice", "cold", "desert", "arctic", "glacier", "quagmire", "snow"}, "snow", 0.7F, false).setIgnoreTime();
	
	public static AmbientSound nether = new BiomesSound(new String[]{"hell", "dead", "inferno", "corrupted sands", "boneyard", "phantasmagoric inferno", "polar chasm", "undergarden", "visceral heap"}, "nether", 0.3F, false).setIgnoreTime().setIgnoreLocation();
	public static AmbientSound end = new BiomesSound(new String[]{"the end"}, "end", 0.4F, false).setIgnoreTime().setIgnoreLocation();
	
	public static AmbientSound mesa = new BiomesSound(new String[]{"mesa", "canyon", "dunes", "outback", "steppe", "xeric shrubland"}, "mesa", 0.5F, false).setIgnoreTime();
	public static AmbientSound extremeHills = new BiomesSound(new String[]{"extreme hills", "alps", "brushland", "crag", "highland", "mountain", "volcanic", "wasteland"}, "extremehills", 0.4F, false).setIgnoreTime();
	
	public static AmbientSound unterwater = new UnterwaterSound("underwater", 0.5F);
	public static AmbientSound cave = new CaveSound("cave", 0.2F);
	
	public static AmbientSound storm = new WeatherSound("storm", 0.5F, WeatherType.STORMY);
	
	
	public IEnhancedPositionSound sound;
	public float volume;
	public float overridenVolume;
	public float muteFactor = 1F;
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
		//this.muteFactor = 1;
	}
	
	public void updateVolume()
	{
		this.sound.volume = this.overridenVolume * muteFactor;
	}
	
	public void setVolume(float volume)
	{
		this.sound.donePlaying = false;
		this.overridenVolume = volume;
		this.sound.volume = volume * muteFactor;
		if(volume <= 0)
		{
			//System.out.println("Stopping sound " + name);
			sound.donePlaying = true;
			//Minecraft.getMinecraft().getSoundHandler().stop(sound.resource.toString(), null);
			Minecraft.getMinecraft().addScheduledTask(new Runnable() {
				
				@Override
				public void run() {
					Minecraft.getMinecraft().getSoundHandler().stopSound(sound);
				}
			});
			resetVolume();
			TickHandler.playing.remove(this);
		}
	}
	
	public float getMutingFactorPriority()
	{
		return 0.0F;
	}
	
	public float getMutingFactor()
	{
		return 0.0F;
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
	
	public float getVolumeFromHeight(int preferedHeight, float height)
	{
		if(height > preferedHeight-1 && height <= preferedHeight)
			return height-(preferedHeight-1);
		if(height > preferedHeight && height < preferedHeight+1)
			return 1-(height-preferedHeight);
		return 0;
	}
	
	/**height: 0 = Underground/Cave, 1 = Biome/ Surface, 2 = Mountain/Windy Air, 3 = Space**/
	public abstract float getVolume(World world, EntityPlayer player, Biome biome, boolean isNight, float height);
	
}