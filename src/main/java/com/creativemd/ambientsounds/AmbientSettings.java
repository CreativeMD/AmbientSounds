package com.creativemd.ambientsounds;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.ConfigGuiType;
import net.minecraftforge.fml.client.config.GuiConfigEntries;
import net.minecraftforge.fml.client.config.GuiConfigEntries.IConfigEntry;
import net.minecraftforge.fml.client.config.GuiEditArrayEntries.IArrayEntry;
import net.minecraftforge.fml.client.config.IConfigElement;

public class AmbientSettings implements IModGuiFactory {
	
	@Override
	public void initialize(Minecraft minecraftInstance) {
		
	}
	
	@Override
	public boolean hasConfigGui() {
		return true;
	}
	
	@Override
	public GuiScreen createConfigGui(GuiScreen parentScreen) {
		List<IConfigElement> elements = new ArrayList<>();
		List<AmbientRegion> regions = AmbientSounds.tickHandler.engine.allRegions.values();
		for (int i = 0; i < regions.size(); i++) {
			AmbientRegion region = regions.get(i);
			elements.add(new IConfigElement() {
				
				double value = region.volumeSetting;
				
				@Override
				public boolean showInGui() {
					return true;
				}
				
				@Override
				public void setToDefault() {
					value = 1;
				}
				
				@Override
				public void set(Object[] aVal) {
					
				}
				
				@Override
				public void set(Object value) {
					region.volumeSetting = (Double) value;
					AmbientSounds.config.load();
					AmbientSounds.config.get("volume", region.name, 1).set(region.volumeSetting);
					AmbientSounds.config.save();
					this.value = (Double) value;
				}
				
				@Override
				public boolean requiresWorldRestart() {
					return false;
				}
				
				@Override
				public boolean requiresMcRestart() {
					return false;
				}
				
				@Override
				public boolean isProperty() {
					return true;
				}
				
				@Override
				public boolean isListLengthFixed() {
					return false;
				}
				
				@Override
				public boolean isList() {
					return false;
				}
				
				@Override
				public boolean isDefault() {
					return value == 1;
				}
				
				@Override
				public Pattern getValidationPattern() {
					return null;
				}
				
				@Override
				public String[] getValidValues() {
					return null;
				}
				
				@Override
				public ConfigGuiType getType() {
					return ConfigGuiType.DOUBLE;
				}
				
				@Override
				public String getQualifiedName() {
					return null;
				}
				
				@Override
				public String getName() {
					return region.name;
				}
				
				@Override
				public Object getMinValue() {
					return 0;
				}
				
				@Override
				public Object getMaxValue() {
					return 1;
				}
				
				@Override
				public int getMaxListLength() {
					return 0;
				}
				
				@Override
				public Object[] getList() {
					return null;
				}
				
				@Override
				public String getLanguageKey() {
					return region.name;
				}
				
				@Override
				public Object[] getDefaults() {
					return null;
				}
				
				@Override
				public Object getDefault() {
					return 1;
				}
				
				@Override
				public Class<? extends IConfigEntry> getConfigEntryClass() {
					return GuiConfigEntries.NumberSliderEntry.class;
				}
				
				@Override
				public String getComment() {
					return "";
				}
				
				@Override
				public List<IConfigElement> getChildElements() {
					return null;
				}
				
				@Override
				public Class<? extends IArrayEntry> getArrayEntryClass() {
					return null;
				}
				
				@Override
				public Object get() {
					return value;
				}
			});
			if (region.sounds != null)
				for (int j = 0; j < region.sounds.length; j++) {
					AmbientSound sound = region.sounds[j];
					elements.add(new IConfigElement() {
						
						double value = sound.volumeSetting;
						
						@Override
						public boolean showInGui() {
							return true;
						}
						
						@Override
						public void setToDefault() {
							value = 1;
						}
						
						@Override
						public void set(Object[] aVal) {
							
						}
						
						@Override
						public void set(Object value) {
							sound.volumeSetting = (Double) value;
							AmbientSounds.config.load();
							AmbientSounds.config.get("volume", sound.fullName, 1).set(sound.volumeSetting);
							AmbientSounds.config.save();
							this.value = (Double) value;
						}
						
						@Override
						public boolean requiresWorldRestart() {
							return false;
						}
						
						@Override
						public boolean requiresMcRestart() {
							return false;
						}
						
						@Override
						public boolean isProperty() {
							return true;
						}
						
						@Override
						public boolean isListLengthFixed() {
							return false;
						}
						
						@Override
						public boolean isList() {
							return false;
						}
						
						@Override
						public boolean isDefault() {
							return value == 1;
						}
						
						@Override
						public Pattern getValidationPattern() {
							return null;
						}
						
						@Override
						public String[] getValidValues() {
							return null;
						}
						
						@Override
						public ConfigGuiType getType() {
							return ConfigGuiType.DOUBLE;
						}
						
						@Override
						public String getQualifiedName() {
							return null;
						}
						
						@Override
						public String getName() {
							return sound.fullName;
						}
						
						@Override
						public Object getMinValue() {
							return 0;
						}
						
						@Override
						public Object getMaxValue() {
							return 1;
						}
						
						@Override
						public int getMaxListLength() {
							return 0;
						}
						
						@Override
						public Object[] getList() {
							return null;
						}
						
						@Override
						public String getLanguageKey() {
							return sound.fullName;
						}
						
						@Override
						public Object[] getDefaults() {
							return null;
						}
						
						@Override
						public Object getDefault() {
							return 1;
						}
						
						@Override
						public Class<? extends IConfigEntry> getConfigEntryClass() {
							return GuiConfigEntries.NumberSliderEntry.class;
						}
						
						@Override
						public String getComment() {
							return "";
						}
						
						@Override
						public List<IConfigElement> getChildElements() {
							return null;
						}
						
						@Override
						public Class<? extends IArrayEntry> getArrayEntryClass() {
							return null;
						}
						
						@Override
						public Object get() {
							return value;
						}
					});
				}
		}
		
		return new AmbientGuiConfig(parentScreen, elements);
	}
	
	@Override
	public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
		
		return null;
	}
	
}
