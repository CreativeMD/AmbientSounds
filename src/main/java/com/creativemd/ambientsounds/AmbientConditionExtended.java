package com.creativemd.ambientsounds;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class AmbientConditionExtended extends AmbientCondition {
	
	public String[] regions;
	transient List<AmbientRegion> regionList;
	
	@SerializedName(value = "bad-regions")
	public String[] badRegions;
	transient List<AmbientRegion> badRegionList;
	
	public String name() {
		return null;
	}
	
	public void init(AmbientEngine engine) {
		super.init(engine);
		
		if (regions != null) {
			regionList = new ArrayList<>();
			
			for (String regionName : regions) {
				AmbientRegion region = engine.getRegion(regionName);
				if (region != null && !regionName.equals(name()))
					regionList.add(region);
			}
		}
		
		if (badRegions != null) {
			badRegionList = new ArrayList<>();
			
			for (String regionName : badRegions) {
				AmbientRegion region = engine.getRegion(regionName);
				if (region != null && !regionName.equals(name()))
					badRegionList.add(region);
			}
		}
	}
	
	@Override
	public AmbientSelection value(AmbientEnviroment env) {
		AmbientSelection selection = super.value(env);
		if (selection == null || selection.volume <= 0)
			return null;
		
		if (badRegionList != null)
			for (AmbientRegion region : badRegionList)
				if (region.value(env) != null)
					return null;
				
		if (regionList != null)
			for (AmbientRegion region : regionList) {
				AmbientSelection subSelection = region.value(env);
				if (subSelection == null)
					return null;
				
				selection.volume *= subSelection.volume;
			}
		
		return selection;
	}
}