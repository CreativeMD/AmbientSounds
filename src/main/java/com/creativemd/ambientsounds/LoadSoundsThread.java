package com.creativemd.ambientsounds;

import java.lang.reflect.Field;
import java.net.URL;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundEventAccessorComposite;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.audio.SoundPoolEntry;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import paulscode.sound.FilenameURL;
import paulscode.sound.Library;
import paulscode.sound.SoundSystem;

@SideOnly(Side.CLIENT)
public class LoadSoundsThread extends Thread{
	
	@Override
	public void run()
	{
		this.setPriority(Thread.MIN_PRIORITY);
		Minecraft mc = Minecraft.getMinecraft();
		SoundManager manager = ReflectionHelper.getPrivateValue(SoundHandler.class, mc.getSoundHandler(), "sndManager", "field_147694_f");
		SoundSystem system = ReflectionHelper.getPrivateValue(SoundManager.class, manager, "sndSystem", "field_148620_e");
		Library libary = null;
		while(libary == null)
		{
			libary = ReflectionHelper.getPrivateValue(SoundSystem.class, system, "soundLibrary");
			try {
				sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		/*Field[] fields = SoundSystem.class.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			System.out.println("Field name=" + fields[i].getName() + " class=" + fields[i].getType().getName());
		}
		
		if(libary == null)
			libary = ReflectionHelper.getPrivateValue(SoundSystem.class, system, 5);
		if(libary == null)
			System.out.println("Failed to load libary ...");*/
		System.out.println("Loading AmbientSounds ...");
		for (int i = 0; i < AmbientSound.sounds.size(); i++) {
			System.out.println("Loading AmbientSound " + AmbientSound.sounds.get(i).name + " " + (i+1) + "/" + AmbientSound.sounds.size());
			SoundEventAccessorComposite soundeventaccessorcomposite = manager.sndHandler.getSound(AmbientSound.sounds.get(i).sound.getSoundLocation());
			ResourceLocation resourcelocation = null;
			if(soundeventaccessorcomposite != null)
			{
				SoundPoolEntry soundpoolentry = soundeventaccessorcomposite.cloneEntry();
				resourcelocation = soundpoolentry.getSoundPoolEntryLocation();
			}else{
				resourcelocation = AmbientSound.sounds.get(i).sound.getSoundLocation();
			}
				
			try {
				libary.loadSound(new FilenameURL((URL) ReflectionHelper.findMethod(SoundManager.class, null, new String[]{"getURLForSoundResource", "func_148612_a"}, ResourceLocation.class).invoke(null, resourcelocation), resourcelocation.toString()));
			} catch (Exception e) {
				e.printStackTrace();
			} // getURLForSoundResource(resourcelocation));
			AmbientSound.sounds.get(i).loaded = true;
			try {
				sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("Loaded AmbientSounds " + (i+1) + "/" + AmbientSound.sounds.size());
		}
		if(mc.thePlayer != null)
		{
			mc.thePlayer.addChatMessage(new ChatComponentText("Done loading AmbientSounds! FPS will increase!"));
		}
		System.out.println("Loaded AmbientSounds ...");
	}
	
}
