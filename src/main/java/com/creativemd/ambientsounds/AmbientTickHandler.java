package com.creativemd.ambientsounds;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.creativemd.ambientsounds.AmbientEnviroment.BiomeArea;
import com.creativemd.ambientsounds.utils.Pair;
import com.creativemd.ambientsounds.utils.PairList;
import com.google.common.base.Strings;
import com.mojang.realmsclient.gui.ChatFormatting;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.client.event.sound.SoundSetupEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import paulscode.sound.SoundSystemConfig;

public class AmbientTickHandler {
	
	private static Minecraft mc = Minecraft.getMinecraft();
	
	public AmbientSoundEngine soundEngine;
	public AmbientEnviroment enviroment = null;
	public AmbientEngine engine;
	public int timer = 0;
	
	public boolean showDebugInfo = false;
	
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
	
	private static DecimalFormat df = new DecimalFormat("0.##");
	
	private String format(Object value) {
		if (value instanceof Double || value instanceof Float)
			return df.format(value);
		return value.toString();
	}
	
	private String format(PairList<String, Object> details) {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (Pair<String, Object> pair : details) {
			if (!first)
				builder.append(",");
			else
				first = false;
			builder.append(ChatFormatting.YELLOW + pair.key + ChatFormatting.RESET + ":" + format(pair.value));
		}
		return builder.toString();
	}
	
	@SubscribeEvent
	public void onRender(RenderTickEvent event) {
		if (showDebugInfo && event.phase == Phase.END && engine != null && mc.inGameHasFocus && enviroment != null) {
			
			GlStateManager.pushMatrix();
			List<String> list = new ArrayList<>();
			
			AmbientDimension dimension = engine.getDimension(mc.world);
			PairList<String, Object> details = new PairList<>();
			details.add("night", enviroment.night);
			details.add("rain", enviroment.raining);
			details.add("storm", enviroment.thundering);
			details.add("b-volume", enviroment.biomeVolume);
			details.add("underwater", enviroment.underwater);
			details.add("dim-name", mc.world.provider.getDimensionType().getName());
			
			list.add(format(details));
			
			details.clear();
			
			for (Pair<BiomeArea, Float> pair : enviroment.biomes) {
				details.add(pair.key.biome.getBiomeName(), pair.value);
			}
			
			list.add(format(details));
			
			details.clear();
			
			details.add("dimension", dimension);
			details.add("playing", engine.soundEngine.playingCount());
			details.add("light", enviroment.blocks.averageLight);
			details.add("outside", enviroment.blocks.outsideVolume);
			details.add("height", df.format(enviroment.relativeHeight) + "," + df.format(enviroment.averageHeight) + "," + df.format(enviroment.player.posY - enviroment.minHeight) + "," + df.format(enviroment.player.posY - enviroment.maxHeight));
			
			list.add(format(details));
			
			details.clear();
			
			for (AmbientRegion region : engine.activeRegions) {
				
				details.add("region", ChatFormatting.DARK_GREEN + region.name + ChatFormatting.RESET);
				details.add("playing", region.playing.size());
				
				list.add(format(details));
				
				details.clear();
				for (AmbientSound sound : region.playing) {
					
					if (!sound.isPlaying())
						continue;
					
					String text = "";
					if (sound.stream1 != null) {
						details.add("n", sound.stream1.location);
						details.add("v", sound.stream1.volume);
						details.add("i", sound.stream1.index);
						details.add("p", sound.stream1.pitch);
						details.add("t", sound.stream1.ticksPlayed);
						details.add("d", sound.stream1.duration);
						
						text = "[" + format(details) + "]";
						
						details.clear();
					}
					
					if (sound.stream2 != null) {
						details.add("n", sound.stream2.location);
						details.add("v", sound.stream2.volume);
						details.add("i", sound.stream2.index);
						details.add("p", sound.stream2.pitch);
						details.add("t", sound.stream2.ticksPlayed);
						details.add("d", sound.stream2.duration);
						
						text += "[" + format(details) + "]";
						
						details.clear();
					}
					
					list.add(text);
				}
			}
			
			for (int i = 0; i < list.size(); ++i) {
				String s = list.get(i);
				
				if (!Strings.isNullOrEmpty(s)) {
					int j = mc.fontRenderer.FONT_HEIGHT;
					int k = mc.fontRenderer.getStringWidth(s);
					int l = 2;
					int i1 = 2 + j * i;
					Gui.drawRect(1, i1 - 1, 2 + k + 1, i1 + j - 1, -1873784752);
					mc.fontRenderer.drawString(s, 2, i1, 14737632);
				}
			}
			GlStateManager.popMatrix();
		}
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
					
					enviroment.blocks.updateAllDirections(engine);
				}
				
				if (timer % engine.soundTickTime == 0) {
					
					enviroment.setSunAngle((world.getCelestialAngle(mc.getRenderPartialTicks())));
					enviroment.updateWorld();
					int depth = 0;
					if (player.isInsideOfMaterial(Material.WATER)) {
						AxisAlignedBB bb = player.getEntityBoundingBox().grow(-0.10000000149011612D, -0.4000000059604645D, -0.10000000149011612D);
						while (world.isMaterialInBB(bb, Material.WATER)) {
							depth++;
							bb = bb.offset(0, 1, 0);
						}
						depth--;
					}
					enviroment.setUnderwater(depth);
					
					engine.tick(enviroment);
				}
				
				engine.fastTick();
				
				timer++;
			} else if (!engine.activeRegions.isEmpty())
				engine.stopEngine();
		}
	}
}
