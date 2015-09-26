package com.creativemd.ambientsounds;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.sql.Ref;
import java.util.ArrayList;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundEventAccessorComposite;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.audio.SoundPoolEntry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
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
	public static final int tickTime = 60;
	public static int timeToTick = 0;
	public static float height = 1;
	
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
					
					float lightLevel =  world.getLightBrightness((int)player.posX, (int)player.posY, (int)player.posZ);
					
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
				
				for (int i = 0; i < AmbientSound.sounds.size(); i++) {
					AmbientSound sound = AmbientSound.sounds.get(i);
					if(sound.canPlaySound())
					{
						float volume = sound.getVolume(world, player, biome, isNight, height);
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
			}else{
				playing.clear();
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
            if(!world.isAirBlock(x, y, z))
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
