package com.creativemd.ambientsounds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.creativemd.ambientsounds.AmbientSituation.BiomeArea;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public abstract class AmbientCondition {
	
	public static class AmbientArrayOrCondition extends AmbientCondition {
		
		public final AmbientCondition[] conditions;
		
		public AmbientArrayOrCondition(AmbientCondition[] conditions) {
			this.conditions = conditions;
		}

		@Override
		public boolean is(AmbientSituation situation, AmbientSoundResult result) {
			ArrayList<BiomeArea> previous = situation.selectedBiomes;
			for (int i = 0; i < conditions.length; i++) {
				situation.selectedBiomes = new ArrayList<>(previous);
				if(conditions[i].is(situation, result))
				{
					result.conditions.add(conditions[i]);
					return true;
				}
			}
			situation.selectedBiomes = previous;
			return false;
		}
		
	}
	
	public static class AmbientArrayAndCondition extends AmbientCondition {
		
		public final AmbientCondition[] conditions;
		
		public AmbientArrayAndCondition(AmbientCondition[] conditions) {
			this.conditions = conditions;
		}
		
		@Override
		public boolean is(AmbientSituation situation, AmbientSoundResult result) {
			ArrayList<BiomeArea> previous = situation.selectedBiomes;
			for (int i = 0; i < conditions.length; i++) {
				situation.selectedBiomes = new ArrayList<>(previous);
				if(!conditions[i].is(situation, result))
					return false;
				result.conditions.add(conditions[i]);
			}
			return true;
		}
		
	}
	
	public static class AmbientInvertCondition extends AmbientCondition {
		
		public AmbientCondition condition;
		
		public AmbientInvertCondition(AmbientCondition condition) {
			this.condition = condition;
		}

		@Override
		public boolean is(AmbientSituation situation, AmbientSoundResult result) {
			return !condition.is(situation, result);
		}
		
	}
	
	public static class AmbientRegionCondition extends AmbientCondition {
		
		public String regionName;
		
		public AmbientCondition condition;
		
		public AmbientRegionCondition(String region)
		{
			this.regionName = region;
		}

		@Override
		public boolean is(AmbientSituation situation, AmbientSoundResult result) {
			if(condition == null)
			{
				condition = AmbientSoundLoader.regions.get(regionName);
				if(condition == null)
					throw new IllegalArgumentException("Region '" + regionName + "' does not exist!");
			}
			return condition.is(situation, result);
		}
		
	}
	
	public static abstract class AmbientConditionParser {
		
		public abstract AmbientCondition parseCondition(JsonElement element) throws IllegalArgumentException;
		
	}
	
	public static AmbientConditionObjectParser parser = new AmbientConditionObjectParser();
	
	public static class AmbientConditionObjectParser extends AmbientConditionParser {
		
		@Override
		public AmbientCondition parseCondition(JsonElement element) throws IllegalArgumentException
		{
			if(element.isJsonArray())
			{
				JsonArray array = element.getAsJsonArray();
				AmbientCondition[] conditions = new AmbientCondition[array.size()];
				for (int i = 0; i < array.size(); i++) {
					try{
						conditions[i] = parseCondition(array.get(i));
					}catch(Exception e){
						AmbientSounds.logger.error("Could not load condition of '" + element + "'!");
						e.printStackTrace();
					}
				}
				return new AmbientArrayOrCondition(conditions);
			}else if(element.isJsonObject()){
				JsonObject object = element.getAsJsonObject();
				ArrayList<AmbientCondition> conditions = new ArrayList<>();
				HashMap<String, JsonElement> unknown = new HashMap<>();
				
				for (Iterator<Entry<String, JsonElement>> iterator = object.entrySet().iterator(); iterator.hasNext();) {
					Entry<String, JsonElement> entry = iterator.next();
					AmbientConditionParser parser = regionSelectors.get(entry.getKey());
					if(parser != null)
					{
						try{
							conditions.add(parser.parseCondition(entry.getValue()));
						}catch(Exception e){
							AmbientSounds.logger.error("Could not load condition of '" + entry.getKey() + ":" + entry.getValue() + "'!");
							e.printStackTrace();
						}
					}	
					else
						unknown.put(entry.getKey(), entry.getValue());
				}
				
				AmbientCondition condition = new AmbientArrayAndCondition(conditions.toArray(new AmbientCondition[0]));
				treatUnknownValues(condition, unknown);
				return condition;				
			}
			throw new IllegalArgumentException("Expected element to be either an array or an object!");
		}
		
		public void treatUnknownValues(AmbientCondition condition, HashMap<String, JsonElement> unknown)
		{
			
		}
	}
	
	public static abstract class AmbientMathConditionParser extends AmbientConditionParser {
		
		public static enum MathOperator {
			
			greater_equals(">=") {
				@Override
				public boolean is(double base, double value) {
					return base <= value;
				}
			},
			smaller_equals("<=") {
				@Override
				public boolean is(double base, double value) {
					return base >= value;
				}
			},
			greater(">") {
				@Override
				public boolean is(double base, double value) {
					return base < value;
				}
			},
			smaller("<") {
				@Override
				public boolean is(double base, double value) {
					return base > value;
				}
			},
			equals("=") {
				@Override
				public boolean is(double base, double value) {
					return base == value;
				}
			};
			
			public final String identifier;
			
			private MathOperator(String identifier) {
				this.identifier = identifier;
			}
			
			public abstract boolean is(double base, double value);
			
			public static MathOperator getOperator(String input) throws IllegalArgumentException
			{
				for (int i = 0; i < values().length; i++) {
					if(input.startsWith(values()[i].identifier))
						return values()[i];
				}
				throw new IllegalArgumentException("Missing valid operator: '" + input + "'!");
			}
			
		}
		
		public abstract double getValue(AmbientSituation situation);
		
		public boolean is(MathOperator operator, double value, AmbientSituation situation)
		{
			return operator.is(value, getValue(situation));
		}
		
		public abstract boolean requiresBiome();
		
		public double[] points;
		
		@Override
		public AmbientCondition parseCondition(JsonElement element) throws IllegalArgumentException {
			if(element.isJsonPrimitive() && ((JsonPrimitive) element).isString())
			{
				String[] parts = element.getAsString().split("&");
				AmbientCondition[] conditions = new AmbientCondition[parts.length];
				points = new double[parts.length];
				for (int i = 0; i < parts.length; i++) {
					MathOperator operator = MathOperator.getOperator(parts[i]);
					double value = Double.parseDouble(parts[i].replaceFirst(operator.identifier, ""));
					points[i] = value;
					conditions[i] = new AmbientCondition() {
						@Override
						public boolean is(AmbientSituation situation, AmbientSoundResult result) {
							if(AmbientMathConditionParser.this.is(operator, value, situation))
							{
								if(AmbientMathConditionParser.this.requiresBiome())
									result.takeBiome = true;
								return true;
							}
							return false;
						}
					};
				}
				return new AmbientArrayAndCondition(conditions);
			}
			throw new IllegalArgumentException("Expected a string!");
		}
	}
	
	public static LinkedHashMap<String, AmbientConditionParser> regionSelectors = new LinkedHashMap<>();
	
	static
	{
		regionSelectors.put("always", new AmbientConditionParser() {

			@Override
			public AmbientCondition parseCondition(JsonElement element) throws IllegalArgumentException
			{
				if(element.isJsonPrimitive() && ((JsonPrimitive) element).isBoolean())
				{
					if(element.getAsBoolean())
					{
						return new AmbientCondition() {
							@Override
							public boolean is(AmbientSituation situation, AmbientSoundResult result) {
								return true;
							}
						};
					}
					return new AmbientCondition() {
						@Override
						public boolean is(AmbientSituation situation, AmbientSoundResult result) {
							return false;
						}
					};
				}
				throw new IllegalArgumentException("Expected a boolean!");
			}
		});
		
		regionSelectors.put("biomes", new AmbientConditionParser() {

			@Override
			public AmbientCondition parseCondition(JsonElement element) throws IllegalArgumentException
			{
				if(element.isJsonArray())
				{
					JsonArray array = element.getAsJsonArray();
					String[] names = new String[array.size()];
					for (int i = 0; i < names.length; i++) {
						names[i] = array.get(i).getAsString();
					}
					
					return new AmbientCondition() {
						@Override
						public boolean is(AmbientSituation situation, AmbientSoundResult result) {
							int i = 0;
							while(i < situation.selectedBiomes.size())
							{
								BiomeArea area = situation.selectedBiomes.get(i);
								boolean foundIt = false;
								for (int j = 0; j < names.length; j++) {
									if(checkBiome(names[j], area.biome))
									{
										i++;
										foundIt = true;
										break;
									}
								}
								if(!foundIt)
									situation.selectedBiomes.remove(i);
							}
							
							if(!situation.selectedBiomes.isEmpty())
							{
								result.takeBiome = true;
								return true;
							}
							
							return false;
						}
					};
				}
				throw new IllegalArgumentException("Expected a string array!");
			}
		});
		
		regionSelectors.put("bad-biomes", new AmbientConditionParser() {

			@Override
			public AmbientCondition parseCondition(JsonElement element) throws IllegalArgumentException
			{
				if(element.isJsonArray())
				{
					JsonArray array = element.getAsJsonArray();
					String[] names = new String[array.size()];
					for (int i = 0; i < names.length; i++) {
						names[i] = array.get(i).getAsString();
					}
					
					return new AmbientCondition() {
						@Override
						public boolean is(AmbientSituation situation, AmbientSoundResult result) {
							int i = 0;
							while(i < situation.selectedBiomes.size())
							{
								BiomeArea area = situation.selectedBiomes.get(i);
								for (int j = 0; j < names.length; j++) {
									if(checkBiome(names[j], area.biome))
										return false;
								}
							}
							
							return true;
						}
					};
				}
				throw new IllegalArgumentException("Expected a string array!");
			}
		});
		
		regionSelectors.put("temperature", new AmbientMathConditionParser() {
			
			@Override
			public boolean is(MathOperator operator, double value, AmbientSituation situation)
			{
				int i = 0;
				while(i < situation.selectedBiomes.size())
				{
					BiomeArea area = situation.selectedBiomes.get(i);
					if(operator.is(value, area.biome.getFloatTemperature(area.pos)))
						i++;
					else
						situation.selectedBiomes.remove(i);
				}
				return !situation.selectedBiomes.isEmpty();
			}
			
			@Override
			public double getValue(AmbientSituation situation) {
				return 0;
			}

			@Override
			public boolean requiresBiome() {
				return true;
			}
		});
		
		regionSelectors.put("position", new AmbientConditionParser() {
			
			@Override
			public AmbientCondition parseCondition(JsonElement element) throws IllegalArgumentException
			{
				if(element.isJsonObject())
				{
					JsonObject object = element.getAsJsonObject();
					double fadeValue = 0;
					if(object.has("fadeValue"))
					{
						JsonElement fade = object.get("fadeValue");
						if(fade.isJsonPrimitive())
							fadeValue = fade.getAsDouble();
						else
							throw new IllegalArgumentException("Expected a number for 'fadeValue'!");
					}
					
					
					double[] relativePointsTemp = null;
					AmbientCondition relativePosition = null;
					if(object.has("relativePosition"))
					{
						JsonElement relative = object.get("relativePosition");
						if(relative.isJsonPrimitive() && ((JsonPrimitive) relative).isString())
						{
							AmbientMathConditionParser parser = new AmbientMathConditionParser() {
								
								@Override
								public double getValue(AmbientSituation situation) {
									return situation.relativeHeight;
								}

								@Override
								public boolean requiresBiome() {
									return false;
								}
							};
							relativePosition = parser.parseCondition(relative);
							relativePointsTemp = parser.points;
							
						}else
							throw new IllegalArgumentException("Expected a string for 'relativePosition'!");
					}
					
					final AmbientCondition relative = relativePosition;
					
					double[] absolutePointsTemp = null;
					AmbientCondition absolutePosition = null;
					if(object.has("absolutePosition"))
					{
						JsonElement absolute = object.get("absolutePosition");
						if(absolute.isJsonPrimitive() && ((JsonPrimitive) absolute).isString())
						{
							AmbientMathConditionParser parser = new AmbientMathConditionParser() {
								
								@Override
								public double getValue(AmbientSituation situation) {
									return situation.player.posY;
								}

								@Override
								public boolean requiresBiome() {
									return false;
								}
							};
							absolutePosition = parser.parseCondition(absolute);
							absolutePointsTemp = parser.points;
						}else
							throw new IllegalArgumentException("Expected a string for 'absolutePosition'!");
					}
					
					final AmbientCondition absolute = absolutePosition;
					
					final double fade = fadeValue;
					final double[] relativePoints = relativePointsTemp;
					final double[] absolutePoints = absolutePointsTemp;
					
					return new AmbientCondition() {
						@Override
						public boolean is(AmbientSituation situation, AmbientSoundResult result) {
							if(relative != null && !relative.is(situation, result))
								return false;
							if(absolute != null && !absolute.is(situation, result))
								return false;
							
							double distance = fade;
							
							if(relativePoints != null)
							{
								for (int i = 0; i < relativePoints.length; i++) {
									distance = Math.min(distance, Math.abs(situation.relativeHeight-relativePoints[i]));
								}
							}
							
							if(absolutePoints != null)
							{
								for (int i = 0; i < absolutePoints.length; i++) {
									distance = Math.min(distance, Math.abs(situation.relativeHeight-absolutePoints[i]));
								}
							}	
							
							if(distance < fade)
								result.volume *= (float) (distance/fade);
							
							return true;
						}
					};
				}
				throw new IllegalArgumentException("Expected an object!");
			}
		});
		
		regionSelectors.put("underwater", new AmbientConditionParser() {

			@Override
			public AmbientCondition parseCondition(JsonElement element) throws IllegalArgumentException
			{
				if(element.isJsonPrimitive() && ((JsonPrimitive) element).isBoolean())
				{
					return new AmbientCondition() {
						@Override
						public boolean is(AmbientSituation situation, AmbientSoundResult result) {
							return element.getAsBoolean() == situation.player.isInsideOfMaterial(Material.WATER);
						}
					};
				}
				throw new IllegalArgumentException("Expected a boolean!");
			}
		});
		
		regionSelectors.put("underwater-pitch", new AmbientConditionParser() {

			@Override
			public AmbientCondition parseCondition(JsonElement element) throws IllegalArgumentException
			{
				if(element.isJsonObject())
				{
					JsonObject object = element.getAsJsonObject();
					return new AmbientCondition() {
						@Override
						public boolean is(AmbientSituation situation, AmbientSoundResult result) {
							if(situation.player.isInsideOfMaterial(Material.WATER))
							{
								
								int depth = 0;
								AxisAlignedBB bb = situation.player.getEntityBoundingBox().grow(-0.10000000149011612D, -0.4000000059604645D, -0.10000000149011612D);
								while(situation.world.isMaterialInBB(bb, Material.WATER))
								{
									depth++;
									bb = bb.offset(new BlockPos(0, 1, 0));
								}
								
								double minPitch = object.has("min") ? object.get("min").getAsDouble() : 0.5;
								double maxPitch = object.has("max") ? object.get("max").getAsDouble() : 2;
								double pitchPerBlock = object.has("pitchPerBlock") ? object.get("pitchPerBlock").getAsDouble() : 0.3;
								
								result.pitch = (float) Math.max(minPitch, maxPitch - (depth * pitchPerBlock));
								return true;
							}
							return false;
						}
					};
				}
				throw new IllegalArgumentException("Expected an object!");
			}
		});
		
		regionSelectors.put("isRaining", new AmbientConditionParser() {

			@Override
			public AmbientCondition parseCondition(JsonElement element) throws IllegalArgumentException
			{
				if(element.isJsonPrimitive() && ((JsonPrimitive) element).isBoolean())
				{
					return new AmbientCondition() {
						@Override
						public boolean is(AmbientSituation situation, AmbientSoundResult result) {
							return element.getAsBoolean() == situation.isRaining;
						}
					};
				}
				throw new IllegalArgumentException("Expected a boolean!");
			}
		});
		
		regionSelectors.put("isStorming", new AmbientConditionParser() {

			@Override
			public AmbientCondition parseCondition(JsonElement element) throws IllegalArgumentException
			{
				if(element.isJsonPrimitive() && ((JsonPrimitive) element).isBoolean())
				{
					return new AmbientCondition() {
						
						@Override
						public boolean is(AmbientSituation situation, AmbientSoundResult result) {
							return element.getAsBoolean() == situation.isThundering;
						}
					};
				}
				throw new IllegalArgumentException("Expected a boolean!");
			}
		});
		
		regionSelectors.put("top-block", new AmbientConditionParser() {

			@Override
			public AmbientCondition parseCondition(JsonElement element) throws IllegalArgumentException
			{
				if(element.isJsonPrimitive() && ((JsonPrimitive) element).isString())
				{
					String name = element.getAsString();
					Block block = Block.getBlockFromName(name);
					
					if(block == null)
						throw new IllegalArgumentException("Invalid block name '" + name + "'!");
					
					return new AmbientCondition() {
						@Override
						public boolean is(AmbientSituation situation, AmbientSoundResult result) {
							int i = 0;
							while(i < situation.selectedBiomes.size())
							{
								BiomeArea area = situation.selectedBiomes.get(i);
								if(area.biome.topBlock.getBlock() == block)
									i++;
								else
									situation.selectedBiomes.remove(i);
							}
							if(!situation.selectedBiomes.isEmpty())
							{
								result.takeBiome = true;
								return true;
							}
							return false;
						}
					};
				}
				throw new IllegalArgumentException("Expected a string!");
			}
		});
		

		regionSelectors.put("treesPerChunk", new AmbientMathConditionParser() {
			
			@Override
			public boolean is(MathOperator operator, double value, AmbientSituation situation)
			{
				int i = 0;
				while(i < situation.selectedBiomes.size())
				{
					BiomeArea area = situation.selectedBiomes.get(i);
					if(operator.is(value, area.biome.theBiomeDecorator.treesPerChunk))
						i++;
					else
						situation.selectedBiomes.remove(i);
				}
				return !situation.selectedBiomes.isEmpty();
			}
			
			@Override
			public double getValue(AmbientSituation situation) {
				return 0;
			}

			@Override
			public boolean requiresBiome() {
				return true;
			}
		});
		
		regionSelectors.put("regions", new AmbientConditionParser() {

			@Override
			public AmbientCondition parseCondition(JsonElement element) throws IllegalArgumentException
			{
				if(element.isJsonArray())
				{
					JsonArray array = element.getAsJsonArray();
					AmbientCondition[] regions = new AmbientCondition[array.size()];
					for (int i = 0; i < regions.length; i++) {
						regions[i] = new AmbientRegionCondition(array.get(i).getAsString());
					}
					return new AmbientArrayOrCondition(regions);
				}
				throw new IllegalArgumentException("Expected a string array!");
			}
		});
		
		regionSelectors.put("bad-regions", new AmbientConditionParser() {

			@Override
			public AmbientCondition parseCondition(JsonElement element) throws IllegalArgumentException
			{
				if(element.isJsonArray())
				{
					JsonArray array = element.getAsJsonArray();
					AmbientCondition[] regions = new AmbientCondition[array.size()];
					for (int i = 0; i < regions.length; i++) {
						regions[i] = new AmbientInvertCondition(new AmbientRegionCondition(array.get(i).getAsString()));
					}
					return new AmbientArrayAndCondition(regions);
				}
				throw new IllegalArgumentException("Expected a string array!");
			}
		});
		
		regionSelectors.put("variants", new AmbientConditionParser() {

			@Override
			public AmbientCondition parseCondition(JsonElement element) throws IllegalArgumentException
			{
				return parser.parseCondition(element);
			}
		});
		
		regionSelectors.put("dimensions", new AmbientConditionParser() {
			
			@Override
			public AmbientCondition parseCondition(JsonElement element) throws IllegalArgumentException {
				if(element.isJsonArray())
				{
					JsonArray array = element.getAsJsonArray();
					return new AmbientCondition() {
						
						
						@Override
						public boolean is(AmbientSituation situation, AmbientSoundResult result) {
							for(int i = 0; i < array.size(); i++)
							{
								JsonElement element = array.get(i);
								if(element.getAsJsonPrimitive().isNumber())
								{
									if(situation.world.provider.getDimension() == element.getAsInt())
										return true;
								}
								else if(element.getAsJsonPrimitive().isString())
								{
									if(situation.world.provider.getDimensionType().name().equals(element.getAsString()))
										return true;
								}
								else
									throw new IllegalArgumentException("Expected a string or a number!");
							}
							return false;
						}
					};
				}
				throw new IllegalArgumentException("Expected a string array!");
			}
		});
		
		regionSelectors.put("bad-dimensions", new AmbientConditionParser() {
			
			@Override
			public AmbientCondition parseCondition(JsonElement element) throws IllegalArgumentException {
				if(element.isJsonArray())
				{
					JsonArray array = element.getAsJsonArray();
					return new AmbientCondition() {
						
						
						@Override
						public boolean is(AmbientSituation situation, AmbientSoundResult result) {
							for(int i = 0; i < array.size(); i++)
							{
								JsonElement element = array.get(i);
								if(element.getAsJsonPrimitive().isNumber())
								{
									if(situation.world.provider.getDimension() == element.getAsInt())
										return false;
								}
								else if(element.getAsJsonPrimitive().isString())
								{
									if(situation.world.provider.getDimensionType().name().equals(element.getAsString()))
										return false;
								}
								else
									throw new IllegalArgumentException("Expected a string or a number!");
							}
							return true;
						}
					};
				}
				throw new IllegalArgumentException("Expected a string array!");
			}
		});
	}
	
	public static boolean checkBiome(String name, Biome biome)
	{
		String biomename = biome.getBiomeName().toLowerCase().replace("_", " ");
		return biomename.matches(".*" + name.replace("*", ".*") + ".*"); //THIS WILL NOT WORK!!!!
		
	}
	
	public HashMap<String, Object> unknownValues;
	
	public AmbientCondition() {
		
	}
	
	public abstract boolean is(AmbientSituation situation, AmbientSoundResult result);
	
	/*public abstract boolean requiresBiome();
	
	public abstract boolean is(AmbientSituation situation);
	
	public float getVolumeModifier(AmbientSituation situation)
	{
		return 1.0F;
	}*/
	
}
