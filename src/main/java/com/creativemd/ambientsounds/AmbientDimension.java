package com.creativemd.ambientsounds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.creativemd.ambientsounds.AmbientCondition.AmbientConditionParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.minecraft.world.World;

public class AmbientDimension {
	
	public final String name;
	public final ArrayList<AmbientDimensionProperty> properties = new ArrayList<>();
	
	public AmbientDimension(JsonElement element) throws IllegalArgumentException
	{
		if(element.isJsonObject())
		{
			JsonObject object = element.getAsJsonObject();
			name = object.get("name").getAsString();
			
			for (Iterator<Entry<String, AmbientDimensionPropertyParser>> iterator = dimensionParser.entrySet().iterator(); iterator.hasNext();) {
				Entry<String, AmbientDimensionPropertyParser> entry = iterator.next();
				JsonElement property = object.get(entry.getKey());
				if(property != null)
				{
					AmbientDimensionProperty ambientDimensionProperty = entry.getValue().parseCondition(property);
					if(ambientDimensionProperty != null)
						properties.add(ambientDimensionProperty);
				}
			}
		}else
			throw new IllegalArgumentException("Invalid Dimension " + element);
	}
	
	public boolean is(World world)
	{
		for (int i = 0; i < properties.size(); i++) {
			if(!properties.get(i).is(world))
				return false;
		}
		return true;
	}
	
	public void manipulateSituation(AmbientSituation situation)
	{
		for (int i = 0; i < properties.size(); i++) {
			properties.get(i).manipulateSituation(situation);				
		}
	}
	
	public abstract static class AmbientDimensionProperty {
		
		public abstract boolean is(World world);
		
		public abstract void manipulateSituation(AmbientSituation situation);
		
	}
	
	public abstract static class AmbientDimensionPropertyParser {
		
		public abstract AmbientDimensionProperty parseCondition(JsonElement element) throws IllegalArgumentException;
		
	}
	
	public static LinkedHashMap<String, AmbientDimensionPropertyParser> dimensionParser = new LinkedHashMap<>();
	
	static
	{
		dimensionParser.put("id", new AmbientDimensionPropertyParser() {
			
			@Override
			public AmbientDimensionProperty parseCondition(JsonElement element) throws IllegalArgumentException {
				if(element.isJsonPrimitive() && ((JsonPrimitive) element).isNumber())
				{
				
					return new AmbientDimensionProperty() {
						
						@Override
						public void manipulateSituation(AmbientSituation situation) {
							
						}
						
						@Override
						public boolean is(World world) {
							return world.provider.getDimension() == element.getAsInt();
						}
					};
				}else
					throw new IllegalArgumentException("Expected a number instead of '" + element + "'!");
			}
		});
		dimensionParser.put("rain", new AmbientDimensionPropertyParser() {
			
			@Override
			public AmbientDimensionProperty parseCondition(JsonElement element) throws IllegalArgumentException {
				if(element.isJsonPrimitive() && ((JsonPrimitive) element).isBoolean())
				{
				
					return new AmbientDimensionProperty() {
						
						@Override
						public void manipulateSituation(AmbientSituation situation) {
							situation.isRaining = ((JsonPrimitive) element).getAsBoolean();
						}
						
						@Override
						public boolean is(World world) {
							return true;
						}
					};
				}else
					throw new IllegalArgumentException("Expected a boolean instead of '" + element + "'!");
			}
		});
		dimensionParser.put("storm", new AmbientDimensionPropertyParser() {
			
			@Override
			public AmbientDimensionProperty parseCondition(JsonElement element) throws IllegalArgumentException {
				if(element.isJsonPrimitive() && ((JsonPrimitive) element).isBoolean())
				{
				
					return new AmbientDimensionProperty() {
						
						@Override
						public void manipulateSituation(AmbientSituation situation) {
							situation.isThundering = ((JsonPrimitive) element).getAsBoolean();
						}
						
						@Override
						public boolean is(World world) {
							return true;
						}
					};
				}else
					throw new IllegalArgumentException("Expected a boolean instead of '" + element + "'!");
			}
		});
		dimensionParser.put("average-height", new AmbientDimensionPropertyParser() {
			
			@Override
			public AmbientDimensionProperty parseCondition(JsonElement element) throws IllegalArgumentException {
				if(element.isJsonPrimitive() && ((JsonPrimitive) element).isNumber())
				{
				
					return new AmbientDimensionProperty() {
						
						@Override
						public void manipulateSituation(AmbientSituation situation) {
							situation.relativeHeight = ((JsonPrimitive) element).getAsFloat();
						}
						
						@Override
						public boolean is(World world) {
							return true;
						}
					};
				}else
					throw new IllegalArgumentException("Expected a number instead of '" + element + "'!");
			}
		});
		dimensionParser.put("isNight", new AmbientDimensionPropertyParser() {
			
			@Override
			public AmbientDimensionProperty parseCondition(JsonElement element) throws IllegalArgumentException {
				if(element.isJsonPrimitive() && ((JsonPrimitive) element).isBoolean())
				{
				
					return new AmbientDimensionProperty() {
						
						@Override
						public void manipulateSituation(AmbientSituation situation) {
							situation.isNight = ((JsonPrimitive) element).getAsBoolean();
						}
						
						@Override
						public boolean is(World world) {
							return true;
						}
					};
				}else
					throw new IllegalArgumentException("Expected a boolean instead of '" + element + "'!");
			}
		});
		dimensionParser.put("biome-selector", new AmbientDimensionPropertyParser() {
			
			@Override
			public AmbientDimensionProperty parseCondition(JsonElement element) throws IllegalArgumentException {
				AmbientCondition condition = AmbientCondition.parser.parseCondition(element);
				if(condition != null)
				{
				
					return new AmbientDimensionProperty() {
						
						@Override
						public void manipulateSituation(AmbientSituation situation) {
							AmbientSoundResult result = new AmbientSoundResult();
							condition.is(situation, result);
							situation.biomeVolume = result.volume;
						}
						
						@Override
						public boolean is(World world) {
							return true;
						}
					};
				}else
					throw new IllegalArgumentException("Missing condition");
			}
		});
	}	
	
	
}
