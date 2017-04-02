package com.creativemd.ambientsounds;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.creativemd.ambientsounds.AmbientCondition.AmbientArrayAndCondition;
import com.creativemd.ambientsounds.AmbientCondition.AmbientArrayOrCondition;
import com.creativemd.ambientsounds.AmbientCondition.AmbientConditionObjectParser;
import com.creativemd.ambientsounds.AmbientSituation.BiomeArea;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class AmbientSound {
	
	public static AmbientSoundSelectorParser parser = new AmbientSoundSelectorParser();
	
	public static class AmbientSoundSelectorParser extends AmbientConditionObjectParser {
		
		@Override
		public void treatUnknownValues(AmbientCondition condition, HashMap<String, JsonElement> unknown)
		{
			condition.unknownValues = new HashMap<>();
			
			for (Iterator<Entry<String, JsonElement>> iterator = unknown.entrySet().iterator(); iterator.hasNext();) {
				Entry<String, JsonElement> entry = iterator.next();
				for (Iterator<Entry<String, AmbientPropertyType>> iterator2 = propertiesTypes.entrySet().iterator(); iterator2.hasNext();) {
					Entry<String, AmbientPropertyType> property = iterator2.next();
					if(property.getKey().equals(entry.getKey()))
					{
						if(property.getValue().isElementValid(entry.getValue()))
						{
							condition.unknownValues.put(property.getKey(), property.getValue().getElementValue(entry.getValue()));
						}else
							throw new IllegalArgumentException("Expected '" + entry.getKey() + "' to be a " + property.getValue().name().toLowerCase() + "!");
						break;
					}
				}
			}
		}
	}
	
	public static enum AmbientPropertyType {
		STRING
		{
			@Override
			public boolean isElementValid(JsonElement element) {
				return element.isJsonPrimitive() && ((JsonPrimitive) element).isString();
			}

			@Override
			public Object getElementValue(JsonElement element) {
				return element.getAsString();
			}
		},
		BOOLEAN
		{
			@Override
			public boolean isElementValid(JsonElement element) {
				return element.isJsonPrimitive() && ((JsonPrimitive) element).isBoolean();
			}

			@Override
			public Object getElementValue(JsonElement element) {
				return element.getAsBoolean();
			}
		 },
		 NUMBER
		 {
			 @Override
			 public boolean isElementValid(JsonElement element) {
				 return element.isJsonPrimitive();
			 }

			@Override
			public Object getElementValue(JsonElement element) {
				return element.getAsDouble();
			}
		 };
		
		public abstract boolean isElementValid(JsonElement element);
		
		public abstract Object getElementValue(JsonElement element);
		
	}
	
	public static LinkedHashMap<String, AmbientPropertyType> propertiesTypes = new LinkedHashMap<>();
	
	static
	{
		propertiesTypes.put("mute", AmbientPropertyType.NUMBER);
		propertiesTypes.put("minPause", AmbientPropertyType.NUMBER);
		propertiesTypes.put("maxPause", AmbientPropertyType.NUMBER);
		propertiesTypes.put("length", AmbientPropertyType.NUMBER);
		propertiesTypes.put("volume", AmbientPropertyType.NUMBER);
		propertiesTypes.put("day", AmbientPropertyType.NUMBER);
		propertiesTypes.put("night", AmbientPropertyType.NUMBER);
		propertiesTypes.put("fade", AmbientPropertyType.NUMBER);
		propertiesTypes.put("fadeIn", AmbientPropertyType.NUMBER);
		propertiesTypes.put("fadeOut", AmbientPropertyType.NUMBER);
	}
	
	public static Object getValue(HashMap<String, Object> values, String identifier, Object defaultValue)
	{
		Object value = values.get(identifier);
		if(value != null)
			return value;
		return defaultValue;
	}
	
	public ResourceLocation name;
	
	public boolean isFull;
	
	/**!= 0 if there is a limit for the length of the audio**/
	public int playingTime = -1;
	
	/**The time the sound waits before it will be played again**/
	public int pause = 0;
	
	public IEnhancedPositionSound sound;
	
	public float currentVolume;
	public float aimedVolume;
	
	public final HashMap<String, Object> properties = new HashMap<>();
	public AmbientCondition condition;
	
	public AmbientSound(JsonObject object) throws IllegalArgumentException {
		
		for (Iterator<Entry<String, JsonElement>> iterator = object.entrySet().iterator(); iterator.hasNext();) {
			Entry<String, JsonElement> entry = iterator.next();
			
			if(entry.getKey().equals("source"))
			{
				if(AmbientPropertyType.STRING.isElementValid(entry.getValue()))
					name = new ResourceLocation(entry.getValue().getAsString());
				else
					throw new IllegalArgumentException("Expected 'name' to be a string!");
			}
			if(entry.getKey().equals("full"))
			{
				if(AmbientPropertyType.BOOLEAN.isElementValid(entry.getValue()))
					isFull = entry.getValue().getAsBoolean();
				else
					throw new IllegalArgumentException("Expected 'full' to be a boolean!");
			}
			if(entry.getKey().equals("selector"))
			{
				condition = parser.parseCondition(entry.getValue());
			}
			
			if(condition == null)
				condition = new AmbientCondition() {
					
					@Override
					public boolean is(AmbientSituation situation) {
						return true;
					}

					@Override
					public boolean requiresBiome() {
						return false;
					}
				};
			
			for (Iterator<Entry<String, AmbientPropertyType>> iterator2 = propertiesTypes.entrySet().iterator(); iterator2.hasNext();) {
				Entry<String, AmbientPropertyType> property = iterator2.next();
				if(property.getKey().equals(entry.getKey()))
				{
					if(property.getValue().isElementValid(entry.getValue()))
					{
						properties.put(property.getKey(), property.getValue().getElementValue(entry.getValue()));
					}else
						throw new IllegalArgumentException("Expected '" + entry.getKey() + "' to be a " + property.getValue().name().toLowerCase() + "!");
					break;
				}
			}
		}
		
		if(name == null)
			throw new IllegalArgumentException("No 'name' given!");
	}
	
	public boolean pickCondition(ArrayList<AmbientCondition> conditions, AmbientSituation situation)
	{
		int index = conditions.size()-1;
		AmbientCondition condition = conditions.get(index);
		if(condition instanceof AmbientArrayAndCondition)
		{
			AmbientCondition[] subConditions = ((AmbientArrayAndCondition) condition).conditions;
			for (int i = 0; i < subConditions.length; i++) {
				conditions.add(subConditions[i]);
				if(!pickCondition(conditions, situation))
				{
					conditions.remove(index);
					return false;
				}
			}
			return true;
		}else if(condition instanceof AmbientArrayOrCondition){
			AmbientCondition[] subConditions = ((AmbientArrayOrCondition) condition).conditions;
			ArrayList<BiomeArea> previous = situation.selectedBiomes;
			for (int i = 0; i < subConditions.length; i++) {
				situation.selectedBiomes = new ArrayList<>(previous);
				conditions.add(subConditions[i]);
				if(pickCondition(conditions, situation))
					return true;
			}
			conditions.remove(index);
			return false;
		}else{
			if(condition.is(situation))
				return true;
			else
			{
				conditions.remove(index);
				return false;
			}
		}
	}
	
	public boolean isSoundPlaying()
	{
		return sound != null;
	}
	
	public boolean inTickList = false;
	
	public float fadeInAmount = 0;
	public float fadeOutAmount = 0;
	
	public void stopSound()
	{
		if(isSoundPlaying())
		{
			Minecraft.getMinecraft().getSoundHandler().stopSound(sound);
			sound = null;
		}
	}
	
	public void tick(float mute)
	{
		if(isSoundPlaying())
		{
			float aimedVolume = this.aimedVolume * mute;
			if(currentVolume < aimedVolume)
				currentVolume += Math.min(fadeInAmount, aimedVolume-currentVolume);
			else if(currentVolume > aimedVolume)
				currentVolume -= Math.min(fadeOutAmount, currentVolume-aimedVolume);
			sound.volume = currentVolume;
			//if(!Minecraft.getMinecraft().getSoundHandler().isSoundPlaying(sound))
				//Minecraft.getMinecraft().getSoundHandler().playSound(sound);
			if(aimedVolume == 0 && currentVolume == 0)
			{
				stopSound();
			}
		}else if(pause > 0)
			pause--;
		else{
			//TODO Implement playing sound!!!
		}
	}
	
	public float mutingFactor = 0;
	
	public boolean update(AmbientSituation situation)
	{
		ArrayList<AmbientCondition> conditions = new ArrayList<>();
		conditions.add(condition);
		if(!pickCondition(conditions, situation))
		{
			aimedVolume = 0;
			return false;
		}
		
		float volume = 1.0F;
		boolean requiresBiomes = false;
		HashMap<String, Object> values = new HashMap<>(properties);
		for (int i = 0; i < conditions.size(); i++) { 
			volume = conditions.get(i).getVolumeModifier(situation); //TODO CHECK IF THIS WORKS!!!
			if(conditions.get(i).unknownValues != null)
				values.putAll(conditions.get(i).unknownValues);
			if(conditions.get(i).requiresBiome())
				requiresBiomes = true;
		}
		
		if(isFull && !requiresBiomes)
			situation.selectedBiomes.clear();
		
		int minPause = (int) ((double) ((Double) getValue(values, "minPause", 0D)));
		int maxPause = (int) ((double) ((Double) getValue(values, "maxPause", 0D)));
		int length = (int) ((double) ((Double) getValue(values, "length", 0D)));
		
		float configVolume = 1;
		if(values.containsKey("volume"))
			configVolume = (float) ((double) ((Double) getValue(values, "volume", 0D)));
		float day = (float) ((double) ((Double) getValue(values, "day", 0D)));
		float night = (float) ((double) ((Double) getValue(values, "night", 0D)));
		
		
		
		float fade = (float) ((double) ((Double) getValue(values, "fade", 0.001)));
		this.fadeInAmount = this.fadeOutAmount = fade;
		if(values.containsKey("fadeIn"))
			fadeInAmount = (float) ((double) ((Double) getValue(values, "fadeIn", 0.001)));
		if(values.containsKey("fadeOut"))
			fadeOutAmount = (float) ((double) ((Double) getValue(values, "fadeOut", 0.001)));
		
		if(situation.isNight)
			volume *= night;
		else
			volume *= day;
		
		if(requiresBiomes)
		{
			float biomeVolume = 0;
			
			for (int i = 0; i < situation.selectedBiomes.size(); i++) {
				biomeVolume = Math.max(biomeVolume, situation.biomes.get(situation.selectedBiomes.get(i)));
			}
			
			volume *= biomeVolume;
		}
		
		this.mutingFactor = (float) ((double) ((Double) getValue(values, "mute", 0D))) * volume;
		
		volume *= configVolume;
		
		if(volume > 0 && pause == 0 && !isSoundPlaying())
		{
			if(isFull)
				situation.playedFull = true;
			currentVolume = 0.001F;
			sound = new IEnhancedPositionSound(name, currentVolume, 1.0F);
			sound.repeat = minPause == 0 && maxPause == 0;
			Minecraft.getMinecraft().getSoundHandler().playSound(sound);
		}
		
		aimedVolume = volume;
		
		return volume > 0;
		
	}
	
	@Override
	public String toString()
	{
		return name + ", " + currentVolume + "/" + aimedVolume;
	}
	
}
