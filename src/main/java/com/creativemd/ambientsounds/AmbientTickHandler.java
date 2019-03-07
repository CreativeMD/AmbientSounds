package com.creativemd.ambientsounds;

import com.creativemd.creativecore.common.utils.type.PairList;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.client.event.sound.SoundSetupEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import paulscode.sound.SoundSystemConfig;

public class AmbientTickHandler {
	
	private static Minecraft mc = Minecraft.getMinecraft();
	
	public AmbientSoundEngine soundEngine;
	public AmbientEnviroment enviroment = null;
	public AmbientEngine engine;
	public int timer = 0;
	
	public void setEngine(AmbientEngine engine) {
		this.engine = engine;
	}
	
	@SubscribeEvent
	public void onWorldUnload(WorldEvent.Unload event) {
		if (!event.getWorld().isRemote)
			return;
		
		if (engine != null)
			engine.stopEngine();
		
		enviroment = null;
		timer = 0;
	}
	
	@SubscribeEvent
	public void onSoundLoadEvent(SoundLoadEvent event) {
		soundEngine = new AmbientSoundEngine(event.getManager(), mc.gameSettings);
		if (engine != null)
			engine.soundEngine = soundEngine;
	}
	
	@SubscribeEvent(priority = EventPriority.LOW)
	public void onSoundSetup(SoundSetupEvent event) {
		SoundSystemConfig.setNumberStreamingChannels(AmbientSounds.streamingChannels);
		SoundSystemConfig.setNumberNormalChannels(AmbientSounds.normalChannels);
	}
	
	@SubscribeEvent
	public void onTick(ClientTickEvent event) {
		if (event.phase == Phase.START && engine != null) {
			
			World world = mc.world;
			EntityPlayer player = mc.player;
			
			if (world != null && player != null && mc.gameSettings.getSoundLevel(SoundCategory.AMBIENT) > 0) {
				
				if (enviroment == null)
					enviroment = new AmbientEnviroment(player);
				
				enviroment.dimension = engine.getDimension(world);
				
				if (timer % engine.enviromentTickTime == 0) {
					enviroment.world = world;
					enviroment.player = player;
					enviroment.biomeVolume = 1;
					enviroment.setHeight(engine.calculateAverageHeight(world, player));
					
					enviroment.biomeVolume = 1.0F;
					
					if (enviroment.dimension != null)
						enviroment.dimension.manipulateEnviroment(enviroment);
					
					if (enviroment.biomeVolume > 0)
						enviroment.biomes = engine.calculateBiomes(world, player, enviroment.biomeVolume);
					else if (enviroment.biomes != null)
						enviroment.biomes.clear();
					else
						enviroment.biomes = new PairList<>();
					
					int lightspots = 0;
					enviroment.averageLight = 0;
					enviroment.blocks = new PairList<>();
					MutableBlockPos pos = new MutableBlockPos();
					for (EnumFacing facing : EnumFacing.VALUES) {
						pos.setPos(player);
						
						for (int i = 1; i < engine.blockScanDistance; i++) {
							pos.offset(facing);
							IBlockState state = world.getBlockState(pos);
							if (state.isBlockNormalCube() || state.isFullBlock() || state.isFullCube()) {
								enviroment.blocks.add(state, i);
								enviroment.averageLight += world.getLight(pos);
								lightspots++;
								break;
							}
						}
					}
					
					enviroment.averageLight /= lightspots;
				}
				
				if (timer % engine.soundTickTime == 0) {
					
					enviroment.setSunAngle((world.getCelestialAngle(mc.getRenderPartialTicks())));
					enviroment.updateWorld();
					
					int depth = 0;
					AxisAlignedBB bb = player.getEntityBoundingBox().grow(-0.10000000149011612D, -0.4000000059604645D, -0.10000000149011612D);
					while (world.isMaterialInBB(bb, Material.WATER)) {
						depth++;
						bb = bb.offset(0, 1, 0);
					}
					enviroment.setUnderwater(depth);
					
					engine.tick(enviroment);
				}
				
				engine.fastTick();
				
				timer++;
			}
		}
	}
}
