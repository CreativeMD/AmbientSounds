package com.creativemd.ambientsounds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class AmbientRegion extends AmbientCondition {
	
	public String name;
	public transient double volumeSetting = 1;
	protected transient boolean active;
	
	public String[] regions;
	transient List<AmbientRegion> regionList;
	
	@SerializedName(value = "bad-regions")
	public String[] badRegions;
	transient List<AmbientRegion> badRegionList;
	
	public AmbientSound[] sounds;
	transient List<AmbientSound> playing = new ArrayList<>();
	
	public AmbientRegion() {
		
	}
	
	public void init(AmbientEngine engine) {
		super.init(engine);
		
		if (regions != null) {
			regionList = new ArrayList<>();
			
			for (String regionName : regions) {
				AmbientRegion region = engine.getRegion(regionName);
				if (region != null && !regionName.equals(this.name))
					regionList.add(region);
			}
		}
		
		if (badRegions != null) {
			badRegionList = new ArrayList<>();
			
			for (String regionName : badRegions) {
				AmbientRegion region = engine.getRegion(regionName);
				if (region != null && !regionName.equals(this.name))
					badRegionList.add(region);
			}
		}
		
		if (sounds != null)
			for (AmbientSound sound : sounds)
				sound.init(engine);
			
	}
	
	@Override
	public AmbientSelection value(AmbientEnviroment env) {
		AmbientSelection selection = super.value(env);
		if (selection == null || selection.volume <= 0)
			return null;
		
		for (AmbientRegion region : badRegionList)
			if (region.value(env) != null)
				return null;
			
		for (AmbientRegion region : regionList) {
			AmbientSelection subSelection = region.value(env);
			if (subSelection == null)
				return null;
			
			selection.volume *= subSelection.volume;
		}
		
		return selection;
	}
	
	public boolean fastTick() {
		if (!playing.isEmpty()) {
			for (Iterator<AmbientSound> iterator = playing.iterator(); iterator.hasNext();) {
				AmbientSound sound = iterator.next();
				if (!sound.fastTick())
					iterator.remove();
			}
		}
		
		return !playing.isEmpty();
	}
	
	public boolean tick(AmbientEnviroment env) {
		boolean activeBefore = active;
		
		AmbientSelection selection = value(env);
		if (selection != null) {
			for (AmbientSound sound : sounds) {
				if (sound.tick(env, selection)) {
					if (!sound.isActive()) {
						sound.activate();
						playing.add(sound);
					}
				} else if (sound.isActive()) {
					sound.deactivate();
					playing.remove(sound);
				}
			}
			
		} else
			fastTick();
		
		return !playing.isEmpty();
	}
	
	public boolean isActive() {
		return active;
	}
	
	public void activate() {
		active = true;
	}
	
	public void deactivate() {
		active = false;
	}
	
	public void stopSounds() {
		if (!playing.isEmpty()) {
			for (AmbientSound sound : playing)
				sound.deactivate();
			playing.clear();
		}
	}
	
}
