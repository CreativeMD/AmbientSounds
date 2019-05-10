package com.creativemd.ambientsounds;

import org.apache.commons.lang3.ArrayUtils;

import com.creativemd.ambientsounds.AmbientEnviroment.TerrainHeight;
import com.google.gson.annotations.SerializedName;

import net.minecraft.world.World;

public class AmbientDimension {
	
	public String name;
	
	@SerializedName("disable-all")
	public boolean disableAll = false;
	
	@SerializedName("biome-selector")
	public AmbientCondition biomeSelector;
	
	public AmbientRegion[] regions;
	
	public Boolean night;
	public Boolean rain;
	public Boolean storm;
	
	public Integer id;
	
	@SerializedName(value = "dimension-ids")
	public int[] dimensionIds;
	@SerializedName(value = "dimension-names")
	public String[] dimensionNames;
	
	@SerializedName(value = "bad-dimension-ids")
	public int[] badDimensionIds;
	@SerializedName(value = "bad-dimension-names")
	public String[] badDimensionNames;
	
	@SerializedName(value = "average-height")
	public Integer averageHeight;
	
	public void init(AmbientEngine engine) {
		if (biomeSelector != null)
			biomeSelector.init(engine);
		
		if (badDimensionNames != null)
			for (int i = 0; i < badDimensionNames.length; i++)
				badDimensionNames[i] = ".*" + badDimensionNames[i].toLowerCase().replace("*", ".*").replace("?", "\\?") + ".*";
			
		if (dimensionNames != null)
			for (int i = 0; i < dimensionNames.length; i++)
				dimensionNames[i] = ".*" + dimensionNames[i].toLowerCase().replace("*", ".*").replace("?", "\\?") + ".*";
	}
	
	public boolean is(World world) {
		
		if (badDimensionIds != null && ArrayUtils.contains(badDimensionIds, world.provider.getDimension()))
			return false;
		
		String dimensionTypeName = world.provider.getDimensionType().getName();
		
		if (badDimensionNames != null) {
			for (int j = 0; j < badDimensionNames.length; j++)
				if (dimensionTypeName.matches(badDimensionNames[j]))
					return false;
		}
		
		if (id != null && world.provider.getDimension() == id)
			return true;
		
		if (dimensionIds != null && ArrayUtils.contains(dimensionIds, world.provider.getDimension()))
			return true;
		
		if (dimensionNames != null) {
			for (int j = 0; j < dimensionNames.length; j++)
				if (dimensionTypeName.matches(dimensionNames[j]))
					return true;
		}
		
		return id == null && dimensionIds == null && dimensionNames == null;
	}
	
	public void manipulateEnviroment(AmbientEnviroment env) {
		env.soundsDisabled = disableAll;
		
		if (night != null)
			env.night = night;
		
		if (rain != null)
			env.raining = rain;
		
		if (storm != null)
			env.thundering = storm;
		
		if (biomeSelector != null) {
			AmbientSelection selection = biomeSelector.value(env);
			if (selection != null)
				env.biomeVolume = selection.getEntireVolume();
			else
				env.biomeVolume = 0;
		}
		
		if (averageHeight != null)
			env.setHeight(new TerrainHeight(averageHeight, (int) averageHeight, (int) averageHeight));
	}
	
	@Override
	public String toString() {
		return name;
	}
	
}
