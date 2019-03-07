package com.creativemd.ambientsounds;

import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

public class AmbientGuiConfig extends GuiConfig {
	
	public AmbientGuiConfig(GuiScreen parentScreen, List<IConfigElement> configElements) {
		super(parentScreen, configElements, AmbientSounds.modid, false, false, "AmbientSounds");
	}
	
}
