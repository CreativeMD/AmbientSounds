package team.creative.ambientsounds;

import team.creative.creativecore.Side;
import team.creative.creativecore.common.config.api.CreativeConfig;
import team.creative.creativecore.common.config.api.ICreativeConfig;
import team.creative.creativecore.common.config.premade.SelectableConfig;

public class AmbientSoundsConfig implements ICreativeConfig {
    
    @CreativeConfig
    public SelectableConfig<String> engines = new SelectableConfig<>(0, "basic");
    
    @Override
    public void configured(Side side) {
        if (AmbientEngine.hasLoadedAtLeastOnce() && AmbientEngine.hasEngineChanged(engines.get()))
            AmbientSounds.scheduleReload();
    }
    
}
