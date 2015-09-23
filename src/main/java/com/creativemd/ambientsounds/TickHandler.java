package com.creativemd.ambientsounds;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.sql.Ref;
import java.util.ArrayList;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundEventAccessorComposite;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.audio.SoundPoolEntry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.client.event.sound.SoundSetupEvent;
import paulscode.sound.SoundSystem;

public class TickHandler {
	
	public static ArrayList<AmbientSound> playing = new ArrayList<AmbientSound>();
	//public static ArrayList<AmbientSound> loading = new ArrayList<AmbientSound>();
	//public static ArrayList<AmbientSound> loaded = new ArrayList<AmbientSound>();
	
	public static boolean loaded = false;
	
	@SubscribeEvent
	public void onClientTick(ClientTickEvent event)
	{
		
		if(event.phase == Phase.END)
		{
			//long start = System.currentTimeMillis();
			Minecraft mc = Minecraft.getMinecraft();
			EntityPlayer player = mc.thePlayer;
			World world = mc.theWorld;
			
			if(!loaded)
			{
				new LoadSoundsThread().start();
				loaded = true;
			}
			
			if(world != null && player != null && mc.gameSettings.getSoundLevel(SoundCategory.AMBIENT) > 0)
			{
				long time = world.getWorldTime() - ((int) (world.getWorldTime()/24000))*24000;
				boolean isNight = time > 12600 && time < 23400;
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
								//if(loaded.contains(sound))
								//{
									try{
										if(!mc.getSoundHandler().isSoundPlaying(sound.sound))
											mc.getSoundHandler().playSound(sound.sound);
										playing.add(sound);
									}catch (Exception e){
										
									}
								/*}else if(!loading.contains(sound)){
									loading.add(sound);
									
									//new LoadSoundThread(sound).start();
								}*/
								
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
