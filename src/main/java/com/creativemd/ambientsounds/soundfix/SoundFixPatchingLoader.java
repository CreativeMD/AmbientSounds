package com.creativemd.ambientsounds.soundfix;

import java.io.File;
import java.util.Map;

import com.creativemd.creativecore.transformer.TransformerNames;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.MCVersion;

public class SoundFixPatchingLoader implements IFMLLoadingPlugin {
	
	public static File location;

	
	@Override
	public String[] getASMTransformerClass() {
		return new String[]{SoundFixTransformer.class.getName()};
	}

	@Override
	public String getModContainerClass() {
		return null;
	}

	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {
		location = (File) data.get("coremodLocation");
		TransformerNames.obfuscated = (boolean) data.get("runtimeDeobfuscationEnabled");
	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}

}
