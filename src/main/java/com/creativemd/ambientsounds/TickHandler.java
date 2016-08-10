package com.creativemd.ambientsounds;

import java.util.ArrayList;

import com.creativemd.ambientsounds.env.AmbientEnv;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Tick;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class TickHandler {
	
	public static ArrayList<AmbientSound> playing = new ArrayList<AmbientSound>();
	
	@SubscribeEvent
	public void onWorldUnload(WorldEvent.Unload event)
	{
		for (int i = 0; i < AmbientEnv.envs.size(); i++) {
			AmbientEnv.envs.get(i).resetTick();
		}
	}
	
	@SubscribeEvent
	public synchronized void onClientTick(ClientTickEvent event)
	{
		if(event.phase == Phase.END)
		{			
			Minecraft mc = Minecraft.getMinecraft();
			EntityPlayer player = mc.thePlayer;
			World world = mc.theWorld;
			
			if(world != null && player != null && mc.gameSettings.getSoundLevel(SoundCategory.AMBIENT) > 0)
			{
				float angle = (float) (world.getCelestialAngle(mc.getRenderPartialTicks())); //0.25-0.75
				boolean isNight = !(angle > 0.75F || angle < 0.25F);
				
				for (int i = 0; i < AmbientEnv.envs.size(); i++) {
					AmbientEnv env = AmbientEnv.envs.get(i);
					env.tick(world, player);
				}
				
				
				float mutingFactor = 0.0F;
				float mutingPriority = 0.0F;
				
				for (int i = 0; i < AmbientSound.sounds.size(); i++) {
					AmbientSound sound = AmbientSound.sounds.get(i);
					sound.tick();
					sound.muteFactor = 1F;
					sound.updateVolume();
					if(sound.canPlaySound())
					{
						float volume = sound.getVolume(world, player, isNight);
						if(volume > 0)
						{
							mutingFactor += sound.getMutingFactor()*(sound.overridenVolume/sound.volume);
							mutingPriority = Math.max(mutingPriority, sound.getMutingFactorPriority());
							if(!playing.contains(sound))
							{
								sound.setVolume(0.00001F);
								try{
									if(sound.playSound())
										playing.add(sound);
								}catch (Exception e){
									e.printStackTrace();
								}
								
							}else{
								if(sound.getTimeToWait() <= 0)
								{
									if(!mc.getSoundHandler().isSoundPlaying(sound.sound))
										playing.remove(sound);
									sound.resetTimeToWait();
								}
									
								if(sound.overridenVolume < sound.volume*volume)
									sound.setVolume(sound.overridenVolume + sound.fadeInAmount());
								else if(sound.overridenVolume > sound.volume*volume + sound.fadeInAmount())
									sound.setVolume(sound.overridenVolume - sound.fadeInAmount());
								else if(sound.overridenVolume > sound.volume*volume)
								{
									sound.overridenVolume = sound.volume*volume;
									sound.sound.volume = sound.volume*volume;
								}
							}
						}else if(volume <= 0)
							if(playing.contains(sound))
								sound.setVolume(sound.overridenVolume - sound.fadeOutAmount());
					}
				}
				mutingFactor = Math.min(1F, mutingFactor);
				if(mutingFactor > 0)
				{
					for (int i = 0; i < playing.size(); i++) {
						if(playing.get(i).getMutingFactorPriority() < mutingPriority)
						{
							playing.get(i).muteFactor = 1-mutingFactor;
							playing.get(i).updateVolume();
						}
					}
				}
				
			}else{
				for (int i = 0; i < playing.size(); i++) {
					playing.get(i).setVolume(0);
				}
			}
			
		}
	}
}
