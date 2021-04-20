package team.creative.ambientsounds;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

import cpw.mods.modlauncher.api.INameMappingService.Domain;
import net.minecraft.client.GameSettings;
import net.minecraft.client.audio.SoundHandler;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper.UnableToFindFieldException;
import team.creative.ambientsounds.AmbientSound.SoundStream;

public class AmbientSoundEngine {
    
    public static Field findField(Class<?> classToAccess, String fieldName) {
        try {
            Field f = classToAccess.getDeclaredField(ObfuscationReflectionHelper.remapName(Domain.FIELD, fieldName));
            f.setAccessible(true);
            return f;
        } catch (UnableToFindFieldException e) {
            AmbientSounds.LOGGER
                    .error("Unable to locate field {} ({}) on type {}", fieldName, ObfuscationReflectionHelper.remapName(Domain.FIELD, fieldName), classToAccess.getName(), e);
            AmbientSounds.LOGGER
                    .error("Unable to access field {} ({}) on type {}", fieldName, ObfuscationReflectionHelper.remapName(Domain.FIELD, fieldName), classToAccess.getName(), e);
            throw new RuntimeException("Unable to access field=" + fieldName, e);
        } catch (Exception e) {
            throw new RuntimeException("Unable to find field=" + fieldName, e);
        }
    }
    
    public SoundHandler handler;
    public GameSettings settings;
    
    private List<SoundStream> sounds = new ArrayList<>();
    
    public int playingCount() {
        synchronized (sounds) {
            return sounds.size();
        }
    }
    
    public AmbientSoundEngine(SoundHandler handler, GameSettings settings) {
        this.settings = settings;
        this.handler = handler;
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
                    if (!handler.isActive(sound))
                        if (sound.hasPlayedOnce())
                            playing = false;
                        else
                            continue;
                    else
                        playing = true;
                    
                    if (sound.hasPlayedOnce() && !playing) {
                        sound.onFinished();
                        handler.stop(sound);
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
        handler.stop(sound);
        synchronized (sounds) {
            sounds.remove(sound);
        }
    }
    
    public void play(SoundStream stream) {
        handler.play(stream);
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
    
}
