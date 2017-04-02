package com.creativemd.ambientsounds;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Charsets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;

public class AmbientSoundLoader {
	
	public static ResourceLocation engine = new ResourceLocation(AmbientSounds.modid, "engine.json");
	
	public static AmbientCondition biomeCondition;
	
	public static LinkedHashMap<String, AmbientCondition> regions = new LinkedHashMap<>();
	public static ArrayList<AmbientSound> sounds = new ArrayList<>();
	
	
	private static Minecraft mc = Minecraft.getMinecraft();
	private static JsonParser parser = new JsonParser();
	
	public static void reloadAmbientSounds()
	{
		regions.clear();
		sounds.clear();
		
		IResource resource;
		try {
			resource = mc.getResourceManager().getResource(engine);
			JsonObject root = parser.parse(IOUtils.toString(resource.getInputStream(), Charsets.UTF_8)).getAsJsonObject();
			
			JsonArray array = root.get("regions").getAsJsonArray();
			for (int i = 0; i < array.size(); i++) {
				try{
					if(array.get(i).isJsonObject())
					{
						JsonObject object = array.get(i).getAsJsonObject();
						JsonElement name = object.get("name");
						if(name.isJsonPrimitive() && ((JsonPrimitive) name).isString())
						{
							AmbientCondition condition = AmbientCondition.parser.parseCondition(object);
							if(condition == null)
								throw new IllegalArgumentException("Invalid condition of child in 'regions' array!");
							regions.put(name.getAsString(), condition);
						}else
							throw new IllegalArgumentException("Invalid name of child in 'regions' array!");
					}else
						throw new IllegalArgumentException("Invalid child of 'regions' array!");
				} catch (Exception e) {
					e.printStackTrace();
					AmbientSounds.logger.error("Could not load " + i + ". child of 'regions' array!");
				}
			}
			
			try{
				biomeCondition = AmbientCondition.parser.parseCondition(root.get("biome-selector"));
			} catch (Exception e) {
				e.printStackTrace();
				throw new JsonParseException("Could not load 'biome-selector'!");
			}
			
			array = root.get("sounds").getAsJsonArray();
			for (int i = 0; i < array.size(); i++) {
				try{
					if(array.get(i).isJsonObject())
					{
						JsonObject object = array.get(i).getAsJsonObject();
						sounds.add(new AmbientSound(object));
					}else
						throw new IllegalArgumentException("Invalid child of 'sounds' array!");
				} catch (Exception e) {
					e.printStackTrace();
					AmbientSounds.logger.error("Could not load " + i + ". child of 'sounds' array!");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			AmbientSounds.logger.error("Sound engine crashed, no sounds will be played!");
			regions.clear();
			sounds.clear();
		}		
		
	}
	
}
