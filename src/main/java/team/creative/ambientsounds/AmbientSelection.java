package team.creative.ambientsounds;

import java.lang.reflect.Field;

public class AmbientSelection extends AmbientVolume {
    
    public final AmbientCondition condition;
    public AmbientSelection subSelection = null;
    
    public AmbientSelection(AmbientCondition condition) {
        super(1, condition.volume);
        this.condition = condition;
    }
    
    @Override
    public double conditionVolume() {
        return subSelection != null ? subSelection.conditionVolume() * super.conditionVolume() : super.conditionVolume();
    }
    
    @Override
    public double settingVolume() {
        return subSelection != null ? subSelection.settingVolume() * super.settingVolume() : super.settingVolume();
    }
    
    @Override
    public double volume() {
        return subSelection != null ? subSelection.volume() * super.volume() : super.volume();
    }
    
    public AmbientSelection last() {
        if (subSelection == null)
            return this;
        return subSelection.last();
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
            subSelection.assignProperties(properties);
    }
    
}
