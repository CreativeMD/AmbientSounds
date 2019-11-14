package team.creative.ambientsounds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AmbientRegion extends AmbientCondition {
	
	public String name;
	public transient double volumeSetting = 1;
	protected transient boolean active;
	
	public AmbientSound[] sounds;
	transient List<AmbientSound> playing = new ArrayList<>();
	
	public transient AmbientDimension dimension;
	
	public AmbientRegion() {
		
	}
	
	@Override
	public String regionName() {
		return name;
	}
	
	@Override
	public void init(AmbientEngine engine) {
		super.init(engine);
		
		if (sounds != null)
			for (AmbientSound sound : sounds)
				sound.init(engine);
	}
	
	@Override
	public AmbientSelection value(AmbientEnviroment env) {
		if (dimension != null && !dimension.is(env.world))
			return null;
		if (volumeSetting == 0)
			return null;
		AmbientSelection selection = super.value(env);
		if (selection != null)
			selection.volume *= volumeSetting;
		return selection;
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
		
		if (sounds == null)
			return false;
		
		AmbientSelection selection = value(env);
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
	
	@Override
	public String toString() {
		return name + ", playing: " + playing.size();
	}
	
}
