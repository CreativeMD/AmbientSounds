package com.creativemd.ambientsounds;

import java.util.ArrayList;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.client.event.sound.SoundLoadEvent;

public class TickHandler {
	
	public static ArrayList<AmbientSound> playing = new ArrayList<AmbientSound>();
	
	@SubscribeEvent
	public void onClientTick(ClientTickEvent event)
	{
		
		if(event.phase == Phase.END)
		{
			//long start = System.currentTimeMillis();
			Minecraft mc = Minecraft.getMinecraft();
			EntityPlayer player = mc.thePlayer;
			World world = mc.theWorld;
			
			if(world != null && player != null && mc.gameSettings.getSoundLevel(SoundCategory.AMBIENT) > 0)
			{
				boolean isNight = world.getWorldTime() > 12600 && world.getWorldTime() < 23400;
				BiomeGenBase biome = world.getBiomeGenForCoords((int)player.posX, (int)player.posZ);
				for (int i = 0; i < AmbientSound.sounds.size(); i++) {
					AmbientSound sound = AmbientSound.sounds.get(i);
					if(sound.canPlaySound())
					{
						float volume = sound.getVolume(world, player, biome, isNight);
						if(volume > 0)
						{
							if(!playing.contains(sound))
							{
								sound.setVolume(0.00001F);
								if(!mc.getSoundHandler().isSoundPlaying(sound.sound))
									mc.getSoundHandler().playSound(sound.sound);
								playing.add(sound);
							}else{
								if(sound.overridenVolume < sound.volume)
									sound.setVolume(sound.overridenVolume + sound.fadeInAmount());
								else if(sound.overridenVolume > sound.volume + sound.fadeInAmount())
									sound.setVolume(sound.overridenVolume - sound.fadeInAmount());
								else if(sound.overridenVolume > sound.volume)
									sound.resetVolume();	
							}
						}else if(volume <= 0)
							if(playing.contains(sound))
								sound.setVolume(sound.overridenVolume - sound.fadeOutAmount());
					}
				}
			}
			//System.out.println((System.currentTimeMillis()-start) + " ms for ticking!");
		}
		
	}
	
	@SubscribeEvent
	public void onSoundSystemLoad(SoundLoadEvent event)
	{
		System.out.println("Preloading " + AmbientSound.sounds.size() + " sounds!");
		for (int i = 0; i < AmbientSound.sounds.size(); i++) {
			event.manager.playSound(AmbientSound.sounds.get(i).sound);
			event.manager.stopSound(AmbientSound.sounds.get(i).sound);
		}
		System.out.println("Preloaded " + AmbientSound.sounds.size() + " sounds!");
	}
	
	/*public void playSound(double p_72980_1_, double p_72980_3_, double p_72980_5_, String p_72980_7_, float p_72980_8_, float p_72980_9_, boolean p_72980_10_)
    {
        double d3 = this.mc.renderViewEntity.getDistanceSq(p_72980_1_, p_72980_3_, p_72980_5_);
        PositionedSoundRecord positionedsoundrecord = new PositionedSoundRecord(new ResourceLocation(p_72980_7_), p_72980_8_, p_72980_9_, (float)p_72980_1_, (float)p_72980_3_, (float)p_72980_5_);

        if (p_72980_10_ && d3 > 100.0D)
        {
            double d4 = Math.sqrt(d3) / 40.0D;
            this.mc.getSoundHandler().playDelayedSound(positionedsoundrecord, (int)(d4 * 20.0D));
        }
        else
        {
            this.mc.getSoundHandler().playSound(positionedsoundrecord);
        }
    }*/
}
