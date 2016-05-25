package com.creativemd.ambientsounds;

import java.util.ArrayList;

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
	//public static ArrayList<AmbientSound> loading = new ArrayList<AmbientSound>();
	//public static ArrayList<AmbientSound> loaded = new ArrayList<AmbientSound>();
	
	public static final int tickTime = 60;
	public static int timeToTick = 0;
	public static float height = 1;
	
	public static synchronized void handleSound(ISound sound)
	{
		if(!Minecraft.getMinecraft().getSoundHandler().isSoundPlaying(sound))
			Minecraft.getMinecraft().getSoundHandler().playSound(sound);
	}
	
	@SubscribeEvent
	public void onWorldUnload(WorldEvent.Unload event)
	{
		ArrayList<AmbientSound> playing = new ArrayList<>(TickHandler.playing);
		for (int i = 0; i < playing.size(); i++) {
			playing.get(i).setVolume(0);
		}
		timeToTick = 0;
	}
	
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
				float angle = (float) (world.getCelestialAngle(mc.getRenderPartialTicks())); //0.25-0.75
				//System.out.println(angle);
				//long time = world.getWorldTime() - ((int) (world.getWorldTime()/24000))*24000;
				//boolean isNight = time > 12600 && time < 23400;
				boolean isNight = !(angle > 0.75F || angle < 0.25F);
				Biome biome = world.getBiomeGenForCoords(new BlockPos((int)player.posX, 0, (int)player.posZ));
				
				timeToTick--;
				if(timeToTick <= 0)
				{
					float average = 0;
					int count = 0;
					
					for (int x = -2; x < 3; x++) {
						for (int z = -2; z < 3; z++) {
							int posX = (int) (player.posX+x*2);
							int posZ = (int) (player.posZ+z*2);
							int height = getHeightBlock(world, posX, posZ);
							
							//System.out.println("height x=" + posX + " z=" + posZ + " --> y" + height);
							//world.setBlock(posX, height+1, posZ, Blocks.diamond_block);
							if(count == 0)
								average = height;
							else
								average = (average*count+height)/(count+1F);
							
							//System.out.println("average=" + average);
							count++;
						}
					}
					
					float caveMax = average-15F;
					float biomeMin = average-5F;
					float biomeMax = average+15F;
					float windyMin = average+35F;
					float windyMax = 4000;
					
					float lightLevel =  world.getLightBrightness(new BlockPos((int)player.posX, (int)player.posY, (int)player.posZ));
					
					float y = (float) player.posY;
					if(caveMax > y || (y > caveMax && y < biomeMin+5 && lightLevel < 0.1))
						height = 0;
					else if(y > caveMax && y < biomeMin)
						height = (y-caveMax)/(biomeMin-caveMax);
					else if(y > biomeMin && y < biomeMax)
						height = 1;
					else if(y > biomeMax && y < windyMin)
						height = 1+(y-biomeMax)/(windyMin-biomeMax);
					else if(y > windyMin && y < windyMax)
						height = 2;
					else if(y > windyMax)
						height = 3;
					
					//System.out.println("Height: " + height + " playerY: " + y + " average: " + average + " light=" + lightLevel);
					
					timeToTick = tickTime;
				}
				
				float mutingFactor = 0.0F;
				float mutingPriority = 0.0F;
				
				for (int i = 0; i < AmbientSound.sounds.size(); i++) {
					AmbientSound sound = AmbientSound.sounds.get(i);
					sound.muteFactor = 1F;
					if(sound.canPlaySound())
					{
						float volume = sound.getVolume(world, player, biome, isNight, height);
						if(volume > 0)
						{
							mutingFactor += sound.getMutingFactor()*(sound.overridenVolume/sound.volume);
							mutingPriority = Math.max(mutingPriority, sound.getMutingFactorPriority());
							if(!playing.contains(sound))
							{
								sound.setVolume(0.00001F);
								//if(loaded.contains(sound))
								//{
									try{
										//if(!mc.getSoundHandler().isSoundPlaying(sound.sound))
											//mc.getSoundHandler().playSound(sound.sound);
										handleSound(sound.sound);
										playing.add(sound);
									}catch (Exception e){
										e.printStackTrace();
									}
								/*}else if(!loading.contains(sound)){
									loading.add(sound);
									
									//new LoadSoundThread(sound).start();
								}*/
								
							}else{
								if(sound.overridenVolume < sound.volume*volume)
									sound.setVolume(sound.overridenVolume + sound.fadeInAmount());
								else if(sound.overridenVolume > sound.volume*volume + sound.fadeInAmount())
									sound.setVolume(sound.overridenVolume - sound.fadeInAmount());
								else if(sound.overridenVolume > sound.volume*volume)
								{
									float temp = sound.volume;
									sound.volume = sound.volume*volume;
									sound.resetVolume();
									sound.volume = temp;
								}
							}
						}else if(volume <= 0)
							if(playing.contains(sound))
								sound.setVolume(sound.overridenVolume - sound.fadeOutAmount());
					}
				}
				mutingFactor = Math.min(1F, mutingFactor);
				for (int i = 0; i < playing.size(); i++) {
					if(playing.get(i).getMutingFactorPriority() < mutingPriority)
					{
						playing.get(i).muteFactor = 1-mutingFactor;
						playing.get(i).updateVolume();
					}
				}
				//System.out.println("muting: " + mutingFactor + ", priority: " + mutingPriority);
			}else{
				for (int i = 0; i < playing.size(); i++) {
					playing.get(i).setVolume(0);
				}
			}
			//System.out.println((System.currentTimeMillis()-start) + " ms for ticking!");
		}
		
	}
	
	public int getHeightBlock(World world, int x, int z)
    {
        int y;
        int heighest = 40;

        for (y = 45; y < 256; ++y)
        {
            if(world.isBlockNormalCube(new BlockPos(x, y, z), false))
            	heighest = y;
        }

        return heighest;
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
