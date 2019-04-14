package com.creativemd.ambientsounds;

import org.apache.commons.lang3.ArrayUtils;

import com.google.gson.annotations.SerializedName;

import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

public class AmbientDimension {
	
	public String name;
	
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
	
	@SerializedName(value = "average-height")
	public Integer averageHeight;
	
	public void init(AmbientEngine engine) {
		if (biomeSelector != null)
			biomeSelector.init(engine);
	}
	
	public boolean is(World world) {
		
		if (id != null && world.dimension.getDimension().getType().getId() == id)
			return true;
		
		if (dimensionIds != null && ArrayUtils.contains(dimensionIds, world.dimension.getDimension().getType().getId()))
			return true;
		
		if (dimensionNames != null) {
			for (int j = 0; j < dimensionNames.length; j++) {
				if (dimensionNames[j].matches(".*" + DimensionType.func_212678_a(world.dimension.getDimension().getType()).getPath().toLowerCase().replace("*", ".*") + ".*"))
					return true;
			}
		}
		
		return id == null && dimensionIds == null && dimensionNames == null;
	}
	
	public void manipulateEnviroment(AmbientEnviroment env) {
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
			env.setHeight(averageHeight);
	}
	
	@Override
	public String toString() {
		return name;
	}
	
}
