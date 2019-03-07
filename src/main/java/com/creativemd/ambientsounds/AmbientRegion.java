package com.creativemd.ambientsounds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AmbientRegion extends AmbientConditionExtended {
	
	public String name;
	public transient double volumeSetting = 1;
	protected transient boolean active;
	
	public AmbientSound[] sounds;
	transient List<AmbientSound> playing = new ArrayList<>();
	
	public AmbientRegion() {
		
	}
	
	@Override
	public String name() {
		return name;
	}
	
	@Override
	public void init(AmbientEngine engine) {
		super.init(engine);
		
		if (sounds != null)
			for (AmbientSound sound : sounds)
				sound.init(engine);
	}
	
	public boolean fastTick() {
		if (!playing.isEmpty()) {
			for (Iterator<AmbientSound> iterator = playing.iterator(); iterator.hasNext();) {
				AmbientSound sound = iterator.next();
				if (!sound.fastTick()) {
					sound.deactivate();
					iterator.remove();
				}
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
		
		if (!playing.isEmpty()) {
			for (AmbientSound sound : playing)
				sound.deactivate();
			playing.clear();
		}
	}
	
}
