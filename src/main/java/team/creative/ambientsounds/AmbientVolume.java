package team.creative.ambientsounds;

public class AmbientVolume implements Comparable<AmbientVolume> {
    
    public static final AmbientVolume SILENT = new AmbientVolume(0, 0) {
        
        @Override
        public void mulCondition(double volume) {}
        
        @Override
        public void mulSetting(double volume) {}
        
    };
    
    public static final AmbientVolume MAX = new AmbientVolume(1, 1) {
        
        @Override
        public void mulCondition(double volume) {}
        
        @Override
        public void mulSetting(double volume) {}
        
    };
    
    /** Used to determine the volume of all the conditions. Included for mute value. Basically tells how good the conditions are meet. */
    private double conditionVolume;
    /** Used to determine the volume of all settings. Will be excluded when calculating mute value */
    private double settingVolume;
    
    public AmbientVolume(double conditionVolume, double settingVolume) {
        this.conditionVolume = conditionVolume;
        this.settingVolume = settingVolume;
    }
    
    public AmbientVolume() {
        this(1, 1);
    }
    
    @Deprecated
    public void setConditionVolumeDirect(double volume) {
        this.conditionVolume = volume;
    }
    
    public void mulVolume(AmbientVolume selection) {
        mulCondition(selection.conditionVolume());
        mulSetting(selection.settingVolume());
    }
    
    public void mulCondition(double volume) {
        conditionVolume *= volume;
    }
    
    public void mulSetting(double volume) {
        settingVolume *= volume;
    }
    
    public double conditionVolume() {
        return conditionVolume;
    }
    
    public double settingVolume() {
        return settingVolume;
    }
    
    public double volume() {
        return settingVolume * conditionVolume;
    }
    
    @Override
    public int compareTo(AmbientVolume o) {
        return Double.compare(volume(), o.volume());
    }
    
    @Override
    public String toString() {
        return AmbientTickHandler.DECIMAL_FORMAT.format(volume()) + "(" + AmbientTickHandler.DECIMAL_FORMAT.format(conditionVolume()) + ")";
    }
    
    public AmbientVolume copy() {
        return new AmbientVolume(conditionVolume, settingVolume);
    }
    
}
