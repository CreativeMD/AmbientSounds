package com.creativemd.ambientsounds;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.lwjgl.openal.AL10;

import io.netty.util.internal.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.ISoundEventListener;
import net.minecraft.client.audio.ITickableSound;
import net.minecraft.client.audio.Sound;
import net.minecraft.client.audio.SoundEventAccessor;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import paulscode.sound.Channel;
import paulscode.sound.Library;
import paulscode.sound.SoundSystem;
import paulscode.sound.SoundSystemConfig;
import paulscode.sound.Source;
import paulscode.sound.libraries.ChannelLWJGLOpenAL;

public class AmbientSoundEngine {
	
	public SoundManager manager;
	
	public GameSettings settings;
	
	public List<IEnhancedPositionSound> sounds = new ArrayList<>();
	
	private static Field system = ReflectionHelper.findField(SoundManager.class, "sndSystem", "field_148620_e");
	private static Field loaded = ReflectionHelper.findField(SoundManager.class, "loaded", "field_148617_f");
	
	public SoundSystem getSystem()
	{
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
	
	/*public boolean isChannelPlaying(ChannelLWJGLOpenAL channel)
	{
		int state = AL10.alGetSourcei( channel.ALSource.get( 0 ),
                AL10.AL_SOURCE_STATE );
		
		if(state != AL10.AL_PAUSED && state != AL10.AL_STOPPED)
			return true;
		return false;
	}*/
	
	public void tick()
	{
		
		SoundSystem system = getSystem();
		Library library = ReflectionHelper.getPrivateValue(SoundSystem.class, system, "soundLibrary");
		//system.CommandQueue(null);
		//system.interruptCommandThread();
		
		
		Iterator<IEnhancedPositionSound> iterator = sounds.iterator();
		
		try {
			if(!loaded.getBoolean(manager))
				return ;
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
		
        while (iterator.hasNext())
        {
        	IEnhancedPositionSound sound = iterator.next();
        	
        	/*Source source = null;
        	synchronized( SoundSystemConfig.THREAD_SYNC )
            {
        		source = library.getSource(sound.systemName);;
            
        	
	        	if (source == null || source.channel == null || source.stopped() || (!source.toLoop && !isChannelPlaying((ChannelLWJGLOpenAL) source.channel)))
	            {*/
        		if(!system.playing(sound.systemName))
        		{
	        		if(sound.hasBeenAdded)
	        		{
		            	if(sound.repeat && AmbientSounds.debugging)
		            	{
		            		System.out.println("Unexpected ending sound " + sound.getSoundLocation() + " " + sound.systemName);
		            	}
		            	sound.playing = false;
		            	if(library != null)
		            		library.removeSource(sound.systemName);
		            	else
		            		System.out.println("No library found. Something went wrong!");
		                iterator.remove();
	        		}
	            }else{
	            	sound.hasBeenAdded = true;
	            }
            //}
        }
        
		for (IEnhancedPositionSound itickablesound : this.sounds)
        {
            itickablesound.update();
            
            system.setVolume(itickablesound.systemName, this.getClampedVolume(itickablesound));
            system.setPitch(itickablesound.systemName, this.getClampedPitch(itickablesound));
            system.setPosition(itickablesound.systemName, itickablesound.getXPosF(), itickablesound.getYPosF(), itickablesound.getZPosF());
        }
	}
	
	public void play(IEnhancedPositionSound p_sound)
	{
		try {
			if(!loaded.getBoolean(manager))
				return ;
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
		SoundSystem system = getSystem();
		
        SoundEventAccessor soundeventaccessor = p_sound.createAccessor(manager.sndHandler);
        ResourceLocation resourcelocation = p_sound.getSoundLocation();
        
        Sound sound = p_sound.getSound();
        float f3 = p_sound.getVolume();
        float f = 16.0F;

        if (f3 > 1.0F)
        {
            f *= f3;
        }

        SoundCategory soundcategory = p_sound.getCategory();
        float f1 = this.getClampedVolume(p_sound);
        float f2 = this.getClampedPitch(p_sound);

        if(f1 > 0.0F)
        {
            boolean flag = p_sound.canRepeat() && p_sound.getRepeatDelay() == 0;
            String s = MathHelper.getRandomUuid(ThreadLocalRandom.current()).toString();
            ResourceLocation resourcelocation1 = sound.getSoundAsOggLocation();

            if (sound.isStreaming())
            {
                system.newStreamingSource(false, s, getURLForSoundResource(resourcelocation1), resourcelocation1.toString(), flag, p_sound.getXPosF(), p_sound.getYPosF(), p_sound.getZPosF(), p_sound.getAttenuationType().getTypeInt(), f);
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.sound.PlayStreamingSourceEvent(manager, p_sound, s));
            }
            else
            {
            	system.newSource(false, s, getURLForSoundResource(resourcelocation1), resourcelocation1.toString(), flag, p_sound.getXPosF(), p_sound.getYPosF(), p_sound.getZPosF(), p_sound.getAttenuationType().getTypeInt(), f);
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.sound.PlaySoundSourceEvent(manager, p_sound, s));
            }
            
            p_sound.systemName = s;
            system.setPitch(s, f2);
            system.setVolume(s, f1);
            system.play(s);
            
            p_sound.playing = true;
            
            sounds.add(p_sound);
        }
	}
	
	private static URL getURLForSoundResource(final ResourceLocation p_148612_0_)
    {
        String s = String.format("%s:%s:%s", new Object[] {"mcsounddomain", p_148612_0_.getResourceDomain(), p_148612_0_.getResourcePath()});
        URLStreamHandler urlstreamhandler = new URLStreamHandler()
        {
            protected URLConnection openConnection(final URL p_openConnection_1_)
            {
                return new URLConnection(p_openConnection_1_)
                {
                    public void connect() throws IOException
                    {
                    }
                    public InputStream getInputStream() throws IOException
                    {
                        return Minecraft.getMinecraft().getResourceManager().getResource(p_148612_0_).getInputStream();
                    }
                };
            }
        };

        try
        {
            return new URL((URL)null, s, urlstreamhandler);
        }
        catch (MalformedURLException var4)
        {
            throw new Error("TODO: Sanely handle url exception! :D");
        }
    }
	
	private float getClampedPitch(ISound soundIn)
    {
        return MathHelper.clamp_float(soundIn.getPitch(), 0.5F, 2.0F);
    }

    private float getClampedVolume(ISound soundIn)
    {
        return MathHelper.clamp_float(soundIn.getVolume() * getVolume(soundIn.getCategory()), 0.0F, 1.0F);
    }
    
    private float getVolume(SoundCategory category)
    {
        return category != null && category != SoundCategory.MASTER ? settings.getSoundLevel(category) : 1.0F;
    }
	
	public void stop(IEnhancedPositionSound sound)
	{
		SoundSystem system = getSystem();
		
		sound.playing = false;
		system.stop(sound.systemName);
		system.removeSource(sound.systemName);
		sounds.remove(sound);
	}
	
}
