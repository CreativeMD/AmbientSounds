package com.creativemd.ambientsounds;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

import com.creativemd.ambientsounds.AmbientSound.SoundStream;

import io.netty.util.internal.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.Sound;
import net.minecraft.client.audio.SoundEventAccessor;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import paulscode.sound.Library;
import paulscode.sound.SoundSystem;
import paulscode.sound.SoundSystemConfig;
import paulscode.sound.Source;

public class AmbientSoundEngine {
	
	private static Field system = ReflectionHelper.findField(SoundManager.class, new String[] { "sndSystem", "field_148620_e" });
	private static Field loaded = ReflectionHelper.findField(SoundManager.class, new String[] { "loaded", "field_148617_f" });
	
	public Library library;
	
	public SoundManager manager;
	public GameSettings settings;
	
	private List<SoundStream> sounds = new ArrayList<>();
	
	public int playingCount() {
		synchronized (sounds) {
			return sounds.size();
		}
	}
	
	public SoundSystem getSystem() {
		try {
			return (SoundSystem) system.get(manager);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			
		}
		return null;
	}
	
	public AmbientSoundEngine(SoundManager manager, GameSettings settings) {
		this.settings = settings;
		this.manager = manager;
	}
	
	public void tick() {
		
		try {
			if (!loaded.getBoolean(manager))
				return;
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
		SoundSystem system = getSystem();
		
		if (library == null)
			this.library = ReflectionHelper.getPrivateValue(SoundSystem.class, getSystem(), "soundLibrary");
		
		synchronized (sounds) {
			Double mute = null;
			try {
				for (SoundStream sound : sounds) {
					double soundMute = sound.mute();
					if (soundMute > 0 && (mute == null || mute < soundMute))
						mute = soundMute;
				}
				
				synchronized (SoundSystemConfig.THREAD_SYNC) {
					for (Iterator iterator = sounds.iterator(); iterator.hasNext();) {
						SoundStream sound = (SoundStream) iterator.next();
						
						Source source = library.getSource(sound.systemName);
						boolean playing;
						if (source == null)
							if (sound.hasPlayedOnce())
								playing = false;
							else
								continue;
						else
							playing = source.playing();
						
						if (sound.hasPlayedOnce() && !playing) {
							sound.onFinished();
							if (source != null)
								source.stop();
							iterator.remove();
							continue;
						} else if (!sound.hasPlayedOnce() && playing)
							sound.setPlayedOnce();
						
						source.setPitch((float) sound.pitch);
						if (source.toLoop != sound.loop())
							source.toLoop = sound.loop();
						if (mute == null || sound.mute() >= mute)
							source.sourceVolume = (float) sound.volume * getVolume(SoundCategory.AMBIENT);
						else
							source.sourceVolume = (float) (sound.volume * (1 - mute)) * getVolume(SoundCategory.AMBIENT);
					}
				}
				
			} catch (ConcurrentModificationException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void stop(SoundStream sound) {
		SoundSystem system = getSystem();
		
		system.stop(sound.systemName);
		system.removeSource(sound.systemName);
		synchronized (sounds) {
			sounds.remove(sound);
		}
	}
	
	public void play(int offset, SoundStream stream) {
		try {
			if (!loaded.getBoolean(manager))
				return;
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
		SoundSystem system = getSystem();
		
		ResourceLocation resourcelocation = stream.location;
		SoundEventAccessor soundeventaccessor = manager.sndHandler.getAccessor(resourcelocation);
		if (soundeventaccessor == null)
			throw new RuntimeException("Missing accessor for " + resourcelocation);
		Sound sound = soundeventaccessor.cloneEntry();
		
		SoundCategory soundcategory = SoundCategory.AMBIENT;
		float f = 16.0F;
		float f1 = this.getClampedVolume((float) stream.volume);
		float f2 = this.getClampedPitch((float) stream.pitch);
		
		if (f1 > 0.0F) {
			String s = MathHelper.getRandomUUID(ThreadLocalRandom.current()).toString();
			ResourceLocation resourcelocation1 = sound.getSoundAsOggLocation();
			
			if (sound.isStreaming())
				system.newStreamingSource(false, s, getURLForSoundResource(resourcelocation1), resourcelocation1.toString(), stream.loop(), 0, 0, 0, 0, f);
			else
				system.newSource(false, s, getURLForSoundResource(resourcelocation1), resourcelocation1.toString(), stream.loop(), 0, 0, 0, 0, f);
			
			stream.systemName = s;
			system.setPitch(s, f2);
			system.setVolume(s, f1);
			system.play(s);
			
			stream.onStart();
			synchronized (sounds) {
				sounds.add(stream);
			}
		}
	}
	
	public void stopAll() {
		synchronized (sounds) {
			for (SoundStream sound : sounds) {
				stop(sound);
				sound.onFinished();
			}
		}
	}
	
	private static URL getURLForSoundResource(final ResourceLocation p_148612_0_) {
		String s = String.format("%s:%s:%s", new Object[] { "mcsounddomain", p_148612_0_.getResourceDomain(), p_148612_0_.getResourcePath() });
		URLStreamHandler urlstreamhandler = new URLStreamHandler() {
			@Override
			protected URLConnection openConnection(final URL p_openConnection_1_) {
				return new URLConnection(p_openConnection_1_) {
					@Override
					public void connect() throws IOException {
					}
					
					@Override
					public InputStream getInputStream() throws IOException {
						return Minecraft.getMinecraft().getResourceManager().getResource(p_148612_0_).getInputStream();
					}
				};
			}
		};
		
		try {
			return new URL((URL) null, s, urlstreamhandler);
		} catch (MalformedURLException var4) {
			throw new Error("TODO: Sanely handle url exception! :D");
		}
	}
	
	private float getClampedPitch(float pitch) {
		return MathHelper.clamp(pitch, 0.5F, 2.0F);
	}
	
	private float getClampedVolume(float volume) {
		return MathHelper.clamp(volume * getVolume(SoundCategory.AMBIENT), 0.0F, 1.0F);
	}
	
	private float getVolume(SoundCategory category) {
		return category != null && category != SoundCategory.MASTER ? settings.getSoundLevel(category) : 1.0F;
	}
	
}
