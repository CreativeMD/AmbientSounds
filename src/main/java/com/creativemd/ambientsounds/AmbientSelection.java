package com.creativemd.ambientsounds;

import java.lang.reflect.Field;

public class AmbientSelection {
	
	public double volume;
	public final AmbientCondition condition;
	
	public AmbientSelection subSelection = null;
	
	public AmbientSelection(AmbientCondition condition) {
		this.volume = condition.volume;
		this.condition = condition;
	}
	
	public double getEntireVolume() {
		return subSelection != null ? subSelection.getEntireVolume() * volume : volume;
	}
	
	public AmbientSelection getLast() {
		if (subSelection == null)
			return this;
		return subSelection.getLast();
	}
	
	public AmbientSoundProperties getProperties() {
		AmbientSoundProperties properties = new AmbientSoundProperties();
		assignProperties(properties);
		return properties;
	}
	
	protected void assignProperties(AmbientSoundProperties properties) {
		try {
			for (Field field : AmbientSoundProperties.class.getFields()) {
				Object value = field.get(condition);
				if (value != null)
					field.set(properties, value);
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
		if (subSelection != null)
			assignProperties(properties);
	}
	
}
