package com.creativemd.ambientsounds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.creativemd.ambientsounds.AmbientSituation.BiomeArea;

import net.minecraft.block.BlockLeaves;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.client.event.sound.SoundSetupEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import paulscode.sound.SoundSystemConfig;
import paulscode.sound.SoundSystemException;

public class TickHandler {
	
	public static ArrayList<AmbientSound> playing = new ArrayList<AmbientSound>();
	
	@SubscribeEvent
	public void onWorldUnload(WorldEvent.Unload event)
	{
		if(!event.getWorld().isRemote)
			return ;
		
		for (int i = 0; i < playing.size(); i++) {
			try{
				playing.get(i).stopSound();
			}catch(Exception e){
				
			}
			playing.get(i).inTickList = false;
		}
		playing.clear();
		situation = null;
		timer = 0;
	}
	
	public int timer = 0;
	public int envUpdateTickTime = 60;
	public int soundTickTime = 2;
	
	public static float calculateAverageHeight(World world, EntityPlayer player)
	{
		float sum = 0;
		int count = 0;
		
		for (int x = -2; x < 3; x++) {
			for (int z = -2; z < 3; z++) {
				int posX = (int) (player.posX+x*2);
				int posZ = (int) (player.posZ+z*2);
				int height = getHeightBlock(world, posX, posZ);
				
				sum += height;
				count++;
			}
		}
		float average = sum/count;
		
		float y = (float) player.posY;
		return y-average;
	}
	
	public static int getHeightBlock(World world, int x, int z)
    {
        int y;
        int heighest = 2;

        for (y = 45; y < 256; ++y)
        {
        	IBlockState state = world.getBlockState(new BlockPos(x, y, z));
            if((state.isOpaqueCube() && !(state.getBlock() instanceof BlockLeaves)) || state.getBlock() == Blocks.WATER)
            	heighest = y;
        }

        return heighest;
    }
	
	private static LinkedHashMap<BiomeArea, Float> sortByFloatValue(Map<BiomeArea, Float> unsortMap) {
	    List<Map.Entry<BiomeArea, Float>> list = new LinkedList<Map.Entry<BiomeArea, Float>>(unsortMap.entrySet());
	    Collections.sort(list, new Comparator<Map.Entry<BiomeArea, Float>>() {
	        public int compare(Map.Entry<BiomeArea, Float> o1, Map.Entry<BiomeArea, Float> o2) {
	            return (o1.getValue()).compareTo(o2.getValue());
	        }
	    });
	    LinkedHashMap<BiomeArea, Float> sortedMap = new LinkedHashMap<BiomeArea, Float>();
	    for (Map.Entry<BiomeArea, Float> entry : list) {
	        sortedMap.put(entry.getKey(), entry.getValue());
	    }

	    return sortedMap;
	}
	
	public static LinkedHashMap<BiomeArea, Float> calculateBiomes(World world, EntityPlayer player, float volume)
	{
		LinkedHashMap<BiomeArea, Float> biomes = new LinkedHashMap<>();
		
		if(world.provider.getDimension() == -1 || world.provider.getDimension() == 1)
			volume = 1F;
		
		if(volume > 0.0)
		{
			int range = 10;
			int stepSize = 5;
			
			int posX = (int) player.posX;
			int posZ = (int) player.posZ;
			BlockPos center = new BlockPos(posX, 0, posZ);
			
			for (int x = -range; x <= range; x+=stepSize) {
				for (int z = -range; z <= range; z+=stepSize) {
					BlockPos pos = new BlockPos(posX+x, 0, posZ+z);
					Biome biome = world.getBiome(pos);
					
					
					float biomeVolume = (float) ((1-Math.sqrt(center.distanceSq(pos))/(range*2))*volume);
					BiomeArea area = new BiomeArea(biome, pos);
					if(biomes.containsKey(area))
						biomes.put(area, Math.max(biomes.get(area), biomeVolume));
					else
						biomes.put(area, biomeVolume);
				}
			}
			
			return sortByFloatValue(biomes);
		}
		return biomes;
	}
	
	private static Minecraft mc = Minecraft.getMinecraft();
	
	public static AmbientSituation situation = null;
	
	@SubscribeEvent
	public void onTick(ClientTickEvent event)
	{
		if(event.phase == Phase.START)
		{
			AmbientSound.engine.tick();
			World world = mc.theWorld;
			EntityPlayer player = mc.thePlayer;
			
			if(world != null && player != null && mc.gameSettings.getSoundLevel(SoundCategory.AMBIENT) > 0)
			{
				if(situation == null)
					situation = new AmbientSituation(world, player, new LinkedHashMap<>(), 0, false);
				
				situation.playedFull = false;
				
				if(timer % envUpdateTickTime == 0)
				{
					situation.world = world;
					situation.player = player;
					situation.biomeVolume = 1;
					
					float angle = (float) (world.getCelestialAngle(mc.getRenderPartialTicks())); //0.25-0.75
					situation.isNight = !(angle > 0.75F || angle < 0.25F);
					situation.relativeHeight = calculateAverageHeight(world, player);
					
					AmbientDimension dimension = AmbientSoundLoader.getDimension(world);
					
					situation.isRaining = world.isRainingAt(player.getPosition());
					situation.isThundering = world.isThundering();
					
					situation.biomeVolume = 1.0F;
					
					situation.selectedBiomes = new ArrayList<>();
										
					if(dimension != null)
						dimension.manipulateSituation(situation);
						
					if(situation.biomeVolume > 0)
						situation.biomes = calculateBiomes(world, player, situation.biomeVolume);
					else if(situation.biomes != null)
						situation.biomes.clear();
					else
						situation.biomes = new LinkedHashMap<>();
				}
				
				if(timer % soundTickTime == 0)
				{
					ArrayList<BiomeArea> biomesFull = new ArrayList<>(situation.biomes.keySet());
					for (int i = 0; i < AmbientSoundLoader.sounds.size(); i++) {
						AmbientSound sound = AmbientSoundLoader.sounds.get(i);
						
						if(sound.isFull)
							situation.selectedBiomes = new ArrayList<>(biomesFull);
						else
							situation.selectedBiomes = new ArrayList<>(situation.biomes.keySet());
						
						boolean canBePlayed = sound.update(situation);
						
						if(canBePlayed && sound.isFull)
							biomesFull.removeAll(situation.selectedBiomes);
						
						if((canBePlayed || sound.isSoundPlaying()) && !sound.inTickList)
						{
							playing.add(sound);
							sound.inTickList = true;
						}else if(!canBePlayed && !sound.isSoundPlaying() && sound.inTickList){
							sound.inTickList = false;
							playing.remove(sound);
						}
					}
					
					if(AmbientSounds.debugging)
					{
						System.out.println("================Playing================");
						for (int i = 0; i < playing.size(); i++) {
							System.out.println(playing.get(i));
						}
					}
				}
				
				float mutingFactor = 0.0F;
				
				mutingFactor = Math.min(1F, mutingFactor);
				for (int i = 0; i < playing.size(); i++) {
					if(playing.get(i).mutingFactor*playing.get(i).currentVolume > mutingFactor)
					{
						mutingFactor = playing.get(i).mutingFactor*playing.get(i).currentVolume;
					}
				}
				
				float mute = 1-mutingFactor;
				for (int i = 0; i < playing.size(); i++) {
					playing.get(i).tick(mutingFactor > playing.get(i).mutingFactor*playing.get(i).currentVolume ? mute : 1F);
					
				}
				
				timer++;
			}
		}
	}
	
	@SubscribeEvent
	public void onSoundLoadEvent(SoundLoadEvent event)
	{
		AmbientSound.engine = new AmbientSoundEngine(event.getManager(), mc.gameSettings);
	}
	
	@SubscribeEvent(priority = EventPriority.LOW)
	public void onSoundSetup(SoundSetupEvent event) {
		SoundSystemConfig.setNumberStreamingChannels( AmbientSounds.streamingChannels );
		SoundSystemConfig.setNumberNormalChannels( AmbientSounds.normalChannels ); 
	}
}
