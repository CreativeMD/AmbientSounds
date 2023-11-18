package team.creative.ambientsounds.sound;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import team.creative.ambientsounds.AmbientSound.SoundStream;

public class AmbientSoundEngine {
    
    private static final Minecraft mc = Minecraft.getInstance();
    
    private List<SoundStream> sounds = new ArrayList<>();
    
    public int playingCount() {
        synchronized (sounds) {
            return sounds.size();
        }
    }
    
    public SoundManager getManager() {
        return mc.getSoundManager();
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
                    if (!getManager().isActive(sound))
                        if (sound.hasPlayedOnce())
                            playing = false;
                        else
                            continue;
                    else
                        playing = true;
                    
                    if (sound.hasPlayedOnce() && !playing) {
                        sound.onFinished();
                        getManager().stop(sound);
                        iterator.remove();
                        continue;
                    } else if (!sound.hasPlayedOnce() && playing)
                        sound.setPlayedOnce();
                    
                    if (mute == null || sound.mute() >= mute || sound.muteResistant())
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
        getManager().stop(sound);
        synchronized (sounds) {
            sounds.remove(sound);
        }
    }
    
    public void play(SoundStream stream) {
        getManager().play(stream);
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
