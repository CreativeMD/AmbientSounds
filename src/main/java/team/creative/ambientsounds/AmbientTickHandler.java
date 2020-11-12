package team.creative.ambientsounds;

import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Strings;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.RenderTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.gui.GuiUtils;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import team.creative.ambientsounds.AmbientEnviroment.BiomeArea;
import team.creative.ambientsounds.utils.Pair;
import team.creative.ambientsounds.utils.PairList;
import team.creative.creativecore.CreativeCore;
import team.creative.creativecore.common.config.holder.ConfigHolderDynamic;
import team.creative.creativecore.common.config.holder.CreativeConfigRegistry;
import team.creative.creativecore.common.config.sync.ConfigSynchronization;

public class AmbientTickHandler {
	
	private static Minecraft mc = Minecraft.getInstance();
	
	public AmbientSoundEngine soundEngine;
	public AmbientEnviroment enviroment = null;
	public AmbientEngine engine;
	public int timer = 0;
	
	public boolean showDebugInfo = false;
	
	public void setEngine(AmbientEngine engine) {
		this.engine = engine;
		initConfiguration();
	}
	
	public void initConfiguration() {
		CreativeConfigRegistry.ROOT.removeField(AmbientSounds.MODID);
		ConfigHolderDynamic holder = CreativeConfigRegistry.ROOT.registerFolder(AmbientSounds.MODID, ConfigSynchronization.CLIENT);
		ConfigHolderDynamic sounds = holder.registerFolder("sounds");
		Field soundField = ObfuscationReflectionHelper.findField(AmbientSound.class, "volumeSetting");
		for (Pair<String, AmbientRegion> pair : engine.allRegions)
			if (pair.value.sounds != null)
				for (AmbientSound sound : pair.value.sounds)
					sounds.registerField(pair.key + "." + sound.name, soundField, sound);
				
		ConfigHolderDynamic dimensions = holder.registerFolder("dimensions");
		Field dimensionField = ObfuscationReflectionHelper.findField(AmbientDimension.class, "volumeSetting");
		for (AmbientDimension dimension : engine.dimensions)
			dimensions.registerField(dimension.name, dimensionField, dimension);
		
		holder.registerField("silent-dimensions", ObfuscationReflectionHelper.findField(AmbientEngine.class, "silentDimensions"), engine);
		
		CreativeCore.CONFIG_HANDLER.load(AmbientSounds.MODID, Dist.CLIENT);
	}
	
	@SubscribeEvent
	public void onClientChat(ClientChatEvent event) {
		String message = event.getMessage();
		if (message.startsWith("/ambient-reload")) {
			if (engine != null)
				engine.stopEngine();
			setEngine(AmbientEngine.loadAmbientEngine(soundEngine));
			event.setCanceled(true);
		} else if (message.startsWith("/ambient-debug")) {
			showDebugInfo = !showDebugInfo;
			event.setCanceled(true);
		}
	}
	
	/* @SubscribeEvent
	 * public void onWorldUnload(WorldEvent.Unload event) {
	 * if (!event.getWorld().isRemote())
	 * return;
	 * 
	 * if (engine != null)
	 * engine.stopEngine();
	 * 
	 * enviroment = null;
	 * timer = 0;
	 * } */
	
	private static DecimalFormat df = new DecimalFormat("0.##");
	
	private String format(Object value) {
		if (value instanceof Double || value instanceof Float)
			return df.format(value);
		return value.toString();
	}
	
	private String format(List<Pair<String, Object>> details) {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (Pair<String, Object> pair : details) {
			if (!first)
				builder.append(",");
			else
				first = false;
			builder.append(TextFormatting.YELLOW + pair.key + TextFormatting.RESET + ":" + format(pair.value));
		}
		return builder.toString();
	}
	
	@SubscribeEvent
	public void onRender(RenderTickEvent event) {
		if (showDebugInfo && event.phase == Phase.END && engine != null && !mc.isGamePaused() && enviroment != null && mc.world != null) {
			RenderSystem.pushMatrix();
			List<String> list = new ArrayList<>();
			
			AmbientDimension dimension = engine.getDimension(mc.world);
			List<Pair<String, Object>> details = new ArrayList<>();
			details.add(new Pair<>("night", enviroment.night));
			details.add(new Pair<>("rain", enviroment.raining));
			details.add(new Pair<>("storm", enviroment.thundering));
			details.add(new Pair<>("b-volume", enviroment.biomeVolume));
			details.add(new Pair<>("underwater", enviroment.underwater));
			details.add(new Pair<>("dim-name", mc.world.func_234923_W_().func_240901_a_().toString()));
			//details.add("dim-name", mc.world.func_234922_V_().func_240901_a_().toString());
			
			list.add(format(details));
			
			details.clear();
			
			for (Pair<BiomeArea, Float> pair : enviroment.biomes)
				details.add(new Pair<>(pair.key.biome.getCategory().getName(), pair.value));
			
			list.add(format(details));
			
			details.clear();
			
			details.add(new Pair<>("dimension", dimension));
			details.add(new Pair<>("playing", engine.soundEngine.playingCount()));
			details.add(new Pair<>("light", enviroment.blocks.averageLight));
			details.add(new Pair<>("outside", enviroment.blocks.outsideVolume));
			details.add(new Pair<>("height", df.format(enviroment.relativeHeight) + "," + df.format(enviroment.averageHeight) + "," + df.format(enviroment.player.getPosYEye() - enviroment.minHeight) + "," + df.format(enviroment.player.getPosYEye() - enviroment.maxHeight)));
			
			list.add(format(details));
			
			details.clear();
			
			for (AmbientRegion region : engine.activeRegions) {
				
				details.add(new Pair<>("region", TextFormatting.DARK_GREEN + region.name + TextFormatting.RESET));
				details.add(new Pair<>("playing", region.playing.size()));
				
				list.add(format(details));
				
				details.clear();
				for (AmbientSound sound : region.playing) {
					
					if (!sound.isPlaying())
						continue;
					
					String text = "";
					if (sound.stream1 != null) {
						details.add(new Pair<>("n", sound.stream1.location));
						details.add(new Pair<>("v", sound.stream1.volume));
						details.add(new Pair<>("i", sound.stream1.index));
						details.add(new Pair<>("p", sound.stream1.pitch));
						details.add(new Pair<>("t", sound.stream1.ticksPlayed));
						details.add(new Pair<>("d", sound.stream1.duration));
						
						text = "[" + format(details) + "]";
						
						details.clear();
					}
					
					if (sound.stream2 != null) {
						details.add(new Pair<>("n", sound.stream2.location));
						details.add(new Pair<>("v", sound.stream2.volume));
						details.add(new Pair<>("i", sound.stream2.index));
						details.add(new Pair<>("p", sound.stream2.pitch));
						details.add(new Pair<>("t", sound.stream2.ticksPlayed));
						details.add(new Pair<>("d", sound.stream2.duration));
						
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
					int i1 = 2 + j * i;
					MatrixStack mat = new MatrixStack();
					GuiUtils.drawGradientRect(mat.getLast().getMatrix(), 0, 1, i1 - 1, 2 + k + 1, i1 + j - 1, -1873784752, -1873784752);
					mc.fontRenderer.func_238405_a_(mat, s, 2, i1, 14737632);
				}
			}
			RenderSystem.popMatrix();
		}
	}
	
	@SubscribeEvent
	public void onTick(ClientTickEvent event) {
		if (event.phase == Phase.START) {
			
			if (soundEngine == null) {
				soundEngine = new AmbientSoundEngine(mc.getSoundHandler(), mc.gameSettings);
				if (engine != null)
					engine.soundEngine = soundEngine;
			}
			
			if (engine == null)
				setEngine(AmbientEngine.loadAmbientEngine(soundEngine));
			
			if (engine == null)
				return;
			
			World world = mc.world;
			PlayerEntity player = mc.player;
			
			if (world != null && player != null && !mc.isGamePaused() && mc.gameSettings.getSoundLevel(SoundCategory.AMBIENT) > 0) {
				
				if (enviroment == null)
					enviroment = new AmbientEnviroment(player);
				
				AmbientDimension newDimension = engine.getDimension(world);
				if (enviroment.dimension != newDimension) {
					engine.changeDimension(enviroment, newDimension);
					enviroment.dimension = newDimension;
				}
				
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
					
					//enviroment.setSunAngle((float) Math.toDegrees(world.getCelestialAngleRadians(mc.getRenderPartialTicks())));
					enviroment.night = world.isNightTime();
					enviroment.updateWorld();
					int depth = 0;
					if (player.areEyesInFluid(FluidTags.WATER)) {
						BlockPos blockpos = new BlockPos(player.getPositionVec()).up();
						while (world.getBlockState(blockpos).getMaterial() == Material.WATER) {
							depth++;
							blockpos = blockpos.up();
						}
						depth--;
					}
					enviroment.setUnderwater(depth);
					
					engine.tick(enviroment);
				}
				
				engine.fastTick(enviroment);
				
				timer++;
			} else if (!engine.activeRegions.isEmpty())
				engine.stopEngine();
		}
	}
}
