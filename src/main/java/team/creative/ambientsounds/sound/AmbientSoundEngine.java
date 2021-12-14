package team.creative.ambientsounds.sound;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;

import com.mojang.blaze3d.audio.Channel;

import net.minecraft.client.Options;
import net.minecraft.client.sounds.LoopingAudioStream;
import net.minecraft.client.sounds.SoundManager;
import net.minecraftforge.client.event.sound.PlayStreamingSourceEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import team.creative.ambientsounds.AmbientSound.SoundStream;

public class AmbientSoundEngine {
    
    private static final Field sourceField = ObfuscationReflectionHelper.findField(Channel.class, "f_83642_");
    private static final Field streamField = ObfuscationReflectionHelper.findField(Channel.class, "f_83645_");
    private static final Field bufferedInputStreamField = ObfuscationReflectionHelper.findField(LoopingAudioStream.class, "f_120161_");
    
    public SoundManager manager;
    public Options options;
    
    private List<SoundStream> sounds = new ArrayList<>();
    
    public int playingCount() {
        synchronized (sounds) {
            return sounds.size();
        }
    }
    
    public AmbientSoundEngine(SoundManager manager, Options options) {
        this.options = options;
        this.manager = manager;
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    public void tick() {
        
        // Is still playing
        synchronized (sounds) {
            Double mute = null;
            try {
                for (SoundStream sound : sounds) {
                    double soundMute = sound.mute();
                    if (soundMute > 0 && (mute == null || mute < soundMute))
                        mute = soundMute;
                }
                
                for (Iterator<SoundStream> iterator = sounds.iterator(); iterator.hasNext();) {
                    SoundStream sound = iterator.next();
                    
                    boolean playing;
                    if (!manager.isActive(sound))
                        if (sound.hasPlayedOnce())
                            playing = false;
                        else
                            continue;
                    else
                        playing = true;
                    
                    if (sound.hasPlayedOnce() && !playing) {
                        sound.onFinished();
                        manager.stop(sound);
                        iterator.remove();
                        continue;
                    } else if (!sound.hasPlayedOnce() && playing)
                        sound.setPlayedOnce();
                    
                    if (mute == null || sound.mute() >= mute)
                        sound.generatedVoume = (float) sound.volume;
                    else
                        sound.generatedVoume = (float) (sound.volume * (1 - mute));
                }
                
            } catch (ConcurrentModificationException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void stop(SoundStream sound) {
        manager.stop(sound);
        synchronized (sounds) {
            sounds.remove(sound);
        }
    }
    
    public void play(SoundStream stream) {
        manager.play(stream);
        stream.onStart();
        synchronized (sounds) {
            sounds.add(stream);
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
    
    @SubscribeEvent
    public void play(PlayStreamingSourceEvent event) {
        if (event.getSound() instanceof SoundStream stream) {
            try {
                int source = sourceField.getInt(event.getChannel());
                LoopingAudioStream looping = (LoopingAudioStream) streamField.get(event.getChannel());
                BufferedInputStream in = (BufferedInputStream) bufferedInputStreamField.get(looping);
                int length = in.available() + AL11.alGetSourcei(source, AL11.AL_BYTE_OFFSET);
                int offset = (int) (Math.random() * length);
                AL10.alSourcef(source, AL11.AL_BYTE_OFFSET, offset);
            } catch (IllegalArgumentException | IllegalAccessException | IOException e) {
                e.printStackTrace();
            }
            
        }
    }
    
}
