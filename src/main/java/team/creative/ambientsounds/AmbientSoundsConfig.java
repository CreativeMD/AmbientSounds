package team.creative.ambientsounds;

import team.creative.creativecore.Side;
import team.creative.creativecore.common.config.api.CreativeConfig;
import team.creative.creativecore.common.config.api.CreativeConfig.DecimalRange;
import team.creative.creativecore.common.config.api.ICreativeConfig;
import team.creative.creativecore.common.config.premade.SelectableConfig;

public class AmbientSoundsConfig implements ICreativeConfig {
    
    @CreativeConfig
    public SelectableConfig<String> engines = new SelectableConfig<>(0, "basic");
    
    @CreativeConfig
    @DecimalRange(min = 0, max = 1)
    public double volume = 1;
    
    @CreativeConfig
    public boolean useSoundMasterSource = false;
    
    @CreativeConfig
    public int scanStepAmount = 100;
    
    @CreativeConfig
    public boolean playSoundWithOffset = true;
    
    @Override
    public void configured(Side side) {
        if (AmbientEngine.hasLoadedAtLeastOnce() && AmbientEngine.hasEngineChanged(engines.get()))
            AmbientSounds.scheduleReload();
    }
    
}
