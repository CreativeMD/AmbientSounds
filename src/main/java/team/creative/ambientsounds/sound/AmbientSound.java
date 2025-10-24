package team.creative.ambientsounds.sound;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.google.gson.annotations.JsonAdapter;

import net.minecraft.Util;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.LoopingAudioStream;
import net.minecraft.client.sounds.LoopingAudioStream.AudioStreamProvider;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import team.creative.ambientsounds.AmbientSounds;
import team.creative.ambientsounds.condition.AmbientCondition;
import team.creative.ambientsounds.condition.AmbientSelection;
import team.creative.ambientsounds.condition.AmbientSelectionMulti;
import team.creative.ambientsounds.condition.AmbientVolume;
import team.creative.ambientsounds.engine.AmbientEngine;
import team.creative.ambientsounds.engine.AmbientEngineLoadException;
import team.creative.ambientsounds.entity.AmbientEntityCondition.StringJson;
import team.creative.ambientsounds.environment.AmbientEnvironment;
import team.creative.ambientsounds.mixin.SoundBufferLibraryAccessor;
import team.creative.creativecore.client.render.text.DebugTextRenderer;
import team.creative.creativecore.client.sound.SpecialSoundInstance;
import team.creative.creativecore.common.config.api.CreativeConfig;
import team.creative.creativecore.common.util.mc.ResourceUtils;

public class AmbientSound extends AmbientCondition {
    
    private static final Random RANDOM = new Random();
    private static final List<Field> COPYFIELDS = new ArrayList<>();
    
    {
        Class clazz = AmbientSound.class;
        while (clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isPrivate(field.getModifiers()) || Modifier.isFinal(field.getModifiers()))
                    continue;
                COPYFIELDS.add(field);
            }
            clazz = clazz.getSuperclass();
        }
    }
    
    public static SoundSource getSoundSource(String name) {
        if (AmbientSounds.CONFIG.useSoundMasterSource)
            return SoundSource.MASTER;
        if (name == null)
            return SoundSource.AMBIENT;
        for (int i = 0; i < SoundSource.values().length; i++)
            if (SoundSource.values()[i].getName().equals(name))
                return SoundSource.values()[i];
        return SoundSource.AMBIENT;
    }
    
    @CreativeConfig.DecimalRange(min = 0, max = 1)
    public transient double volumeSetting = 1;
    public String name;
    public transient String fullName;
    public ResourceLocation[] files;
    public double[] chances;
    @JsonAdapter(StringJson.class)
    public String[] category;
    
    public transient SoundStream stream1;
    public transient SoundStream stream2;
    
    protected transient boolean active;
    
    protected transient float cachedAimedConditionVolume;
    /** defines the aimed output position which includes all settings and is the effective value of the sound (ignoring transition volume and the mute factor) */
    protected transient float cachedAimedOutputVolume;
    protected transient AmbientVolume aimedVolume;
    
    protected transient float currentConditionVolume;
    /** defines the current volume including all settings and is the effective value of the sound (ignoring transition volume and the mute factor) */
    protected transient float currentOutputVolume;
    
    protected transient float aimedPitch;
    protected transient int transition;
    protected transient int transitionTime;
    
    protected transient int pauseTimer = -1;
    
    protected transient AmbientSoundProperties currentPropertries;
    protected transient AmbientEngine engine;
    
    protected transient List<AmbientSoundCategory> categories;
    
    @Override
    public void init(AmbientEngine engine) throws AmbientEngineLoadException {
        if (files == null || files.length == 0)
            throw new RuntimeException("Invalid sound " + name + " which does not contain any sound file");
        
        super.init(engine);
        
        this.engine = engine;
        
        if (chances == null) {
            chances = new double[files.length];
            Arrays.fill(chances, 1D / files.length);
        } else if (chances.length != files.length) {
            double[] newChances = new double[files.length];
            for (int i = 0; i < newChances.length; i++) {
                if (chances.length > i)
                    newChances[i] = chances[i];
                else
                    newChances[i] = 1D / files.length;
            }
            this.chances = newChances;
        }
        
        if (category != null && category.length > 0) {
            categories = new ArrayList<>();
            for (int i = 0; i < category.length; i++) {
                var cat = engine.getSoundCategory(category[i]);
                if (cat != null)
                    categories.add(cat);
                else
                    AmbientSounds.LOGGER.error("Could not find sound category {} for {}.", category[i], fullName);
            }
        }
    }
    
    protected int getRandomFile() {
        if (files.length == 1)
            return 0;
        return RANDOM.nextInt(files.length);
    }
    
    protected int getRandomFileExcept(int i) {
        if (files.length == 2)
            return i == 0 ? 1 : 0;
        int index = RANDOM.nextInt(files.length - 1);
        if (index >= i)
            index++;
        return index;
    }
    
    public boolean fastTick(AmbientEnvironment env) {
        if (currentConditionVolume < cachedAimedConditionVolume)
            currentConditionVolume += Math.min(currentPropertries.getFadeInVolume(engine), cachedAimedConditionVolume - currentConditionVolume);
        else if (currentConditionVolume > cachedAimedConditionVolume)
            currentConditionVolume -= Math.min(currentPropertries.getFadeOutVolume(engine), currentConditionVolume - cachedAimedConditionVolume);
        
        if (currentOutputVolume < cachedAimedOutputVolume)
            currentOutputVolume += Math.min(currentPropertries.getFadeInVolume(engine), cachedAimedOutputVolume - currentOutputVolume);
        else if (currentOutputVolume > cachedAimedOutputVolume)
            currentOutputVolume -= Math.min(currentPropertries.getFadeOutVolume(engine), currentOutputVolume - cachedAimedOutputVolume);
        
        if (isPlaying()) {
            
            if (inTransition()) { // Two files are played
                stream1.transitionVolume = (1D - (double) transition / transitionTime);
                stream2.transitionVolume = (double) transition / transitionTime;
                
                if (transition >= transitionTime) {
                    engine.soundEngine.stop(stream1);
                    stream1 = stream2;
                    stream2 = null;
                }
                
                transition++;
            } else { // Only one file is played at the moment
                
                if (stream1.duration == -1 && currentPropertries.length != null)
                    stream1.duration = (int) currentPropertries.length.randomValue();
                else if (stream1.duration > 0 && currentPropertries.length == null)
                    stream1.duration = -1;
                
                stream1.transitionVolume = 1;
                
                if (currentPropertries.length != null) { // If the sound has a length
                    
                    if (currentPropertries.pause == null && files.length > 1) { // Continuous transition
                        if (stream1.remaining() <= 0) {
                            transition = 0;
                            stream2 = playTransition(getRandomFileExcept(stream1.index), env);
                            transitionTime = currentPropertries.transition != null ? currentPropertries.transition : 60;
                        }
                    } else {
                        int fadeOutTime = (int) Math.ceil(cachedAimedConditionVolume / currentPropertries.getFadeOutVolume(engine));
                        
                        if (stream1.remaining() <= 0) { // Exceeded length
                            engine.soundEngine.stop(stream1);
                            stream1 = null;
                            pauseTimer = -1;
                        } else if (fadeOutTime > stream1.remaining()) // about to exceed length -> fade out
                            stream1.transitionVolume = stream1.remaining() / fadeOutTime;
                    }
                }
            }
            
            if (stream1 != null) {
                if (stream1.pitch < aimedPitch)
                    stream1.pitch += Math.min(currentPropertries.getFadeInPitch(engine), aimedPitch - stream1.pitch);
                else if (stream1.pitch > aimedPitch)
                    stream1.pitch -= Math.min(currentPropertries.getFadeOutPitch(engine), stream1.pitch - aimedPitch);
                stream1.ticksPlayed++;
            }
            if (stream2 != null) {
                if (stream2.pitch < aimedPitch)
                    stream2.pitch += Math.min(currentPropertries.getFadeInPitch(engine), aimedPitch - stream2.pitch);
                else if (stream2.pitch > aimedPitch)
                    stream2.pitch -= Math.min(currentPropertries.getFadeOutPitch(engine), stream2.pitch - aimedPitch);
                stream2.ticksPlayed++;
            }
        } else {
            
            if (stream2 != null) {
                engine.soundEngine.stop(stream2);
                stream2 = null;
            }
            
            if (pauseTimer == -1) {
                if (currentPropertries.pause != null)
                    pauseTimer = (int) currentPropertries.pause.randomValue();
            }
            
            if (pauseTimer <= 0)
                stream1 = play(getRandomFile(), env);
            else
                pauseTimer--;
        }
        
        return isAudible();
    }
    
    @Override
    public AmbientSelection value(AmbientEnvironment env) {
        if (volumeSetting == 0)
            return null;
        
        var value = super.value(env);
        if (value != null && categories != null) {
            List<AmbientSelection> collected = new ArrayList<>();
            for (AmbientSoundCategory cat : categories) {
                if (cat.selection == null)
                    return null;
                collected.add(cat.selection);
            }
            value = new AmbientSelectionMulti(value, collected);
        }
        return value;
    }
    
    public boolean isAudible() {
        return cachedAimedConditionVolume > 0 || currentConditionVolume > 0 || currentOutputVolume > 0;
    }
    
    public boolean tick(AmbientEnvironment env, AmbientSelection selection) {
        if (selection != null) {
            AmbientSelection soundSelection = value(env);
            
            if (soundSelection != null) {
                AmbientSelection last = selection.last();
                last.subSelection = soundSelection;
                
                aimedVolume = selection;
                cachedAimedConditionVolume = (float) selection.conditionVolume();
                cachedAimedOutputVolume = (float) (aimedVolume.volume() * volumeSetting * env.dimension.volumeSetting * AmbientSounds.CONFIG.volume);
                
                currentPropertries = selection.getProperties();
                last.subSelection = null;
                
                aimedPitch = Mth.clamp(currentPropertries.getPitch(env), 0.5F, 2.0F);
            } else {
                aimedVolume = AmbientVolume.SILENT;
                cachedAimedConditionVolume = cachedAimedOutputVolume = 0;
            }
        } else {
            aimedVolume = AmbientVolume.SILENT;
            cachedAimedConditionVolume = cachedAimedOutputVolume = 0;
        }
        
        return isAudible();
    }
    
    protected SoundStream play(int index, AmbientEnvironment env) {
        SoundStream stream = new SoundStream(index);
        stream.pitch = aimedPitch;
        if (currentPropertries.length != null)
            stream.duration = (int) currentPropertries.length.randomValue();
        
        engine.soundEngine.play(stream);
        return stream;
    }
    
    protected SoundStream playTransition(int index, AmbientEnvironment env) {
        SoundStream stream = new SoundStream(index);
        stream.pitch = aimedPitch;
        if (currentPropertries.length != null)
            stream.duration = (int) currentPropertries.length.randomValue();
        
        stream.transitionVolume = 0;
        stream.effectiveVolume = (float) stream.combinedVolume();
        engine.soundEngine.play(stream);
        return stream;
    }
    
    public boolean isPlaying() {
        return stream1 != null;
    }
    
    public boolean inTransition() {
        return stream1 != null && stream2 != null;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void activate() {
        active = true;
    }
    
    public void deactivate() {
        active = false;
        
        if (stream1 != null) {
            engine.soundEngine.stop(stream1);
            stream1 = null;
        }
        
        if (stream2 != null) {
            engine.soundEngine.stop(stream2);
            stream2 = null;
        }
    }
    
    public void onSoundFinished() {
        if (stream1 != null && stream1.finished) {
            stream1 = null;
            pauseTimer = -1;
        } else
            stream2 = null;
    }
    
    public boolean loop() {
        return currentPropertries.length != null || (currentPropertries.pause == null && files.length == 1);
    }
    
    public class SoundStream implements TickableSoundInstance, SpecialSoundInstance {
        
        private static final RandomSource rand = RandomSource.createNewThreadLocalInstance();
        
        public final int index;
        public final ResourceLocation location;
        
        /** effective volume is the volume that is actually played. It includes condition, transition and setting volume and the mute factor is also applied */
        public float effectiveVolume;
        
        public double transitionVolume = 1;
        
        public WeighedSoundEvents soundeventaccessor;
        
        public double pitch;
        public int duration = -1;
        public int ticksPlayed = 0;
        
        private boolean finished = false;
        private boolean playedOnce;
        public final SoundSource category;
        
        public SoundStream(int index) {
            this.index = index;
            this.location = AmbientSound.this.files[index];
            this.category = getSoundSource(currentPropertries.channel);
            this.effectiveVolume = (float) combinedVolume();
        }
        
        public boolean loop() {
            return AmbientSound.this.loop();
        }
        
        public int remaining() {
            return duration - ticksPlayed;
        }
        
        public double conditionVolume() {
            return currentConditionVolume;
        }
        
        /** includes condition, transition and setting volume. Used before mute factor is applied */
        public double combinedVolume() {
            return currentOutputVolume * transitionVolume;
        }
        
        public double mute() {
            return AmbientSound.this.currentPropertries.mute == null ? 0 : AmbientSound.this.currentPropertries.mute * conditionVolume();
        }
        
        public double mutePriority() {
            return AmbientSound.this.currentPropertries.mutePriority != null ? AmbientSound.this.currentPropertries.mutePriority : 0;
        }
        
        public void onStart() {
            this.finished = false;
            playedOnce = false;
        }
        
        public void onFinished() {
            this.finished = true;
            AmbientSound.this.onSoundFinished();
        }
        
        public boolean hasPlayedOnce() {
            return playedOnce;
        }
        
        public void setPlayedOnce() {
            playedOnce = true;
        }
        
        public boolean hasFinished() {
            return finished;
        }
        
        @Override
        public String toString() {
            return "l:" + location + ",v:" + DebugTextRenderer.DECIMAL_FORMAT.format(volume) + "(" + DebugTextRenderer.DECIMAL_FORMAT.format(
                conditionVolume()) + "),i:" + index + ",p:" + pitch + ",t:" + ticksPlayed + ",d:" + duration;
        }
        
        @Override
        public boolean isLooping() {
            return loop();
        }
        
        @Override
        public WeighedSoundEvents resolve(SoundManager sndHandler) {
            soundeventaccessor = sndHandler.getSoundEvent(location);
            return soundeventaccessor;
        }
        
        @Override
        public SoundInstance.Attenuation getAttenuation() {
            return SoundInstance.Attenuation.NONE;
        }
        
        @Override
        public SoundSource getSource() {
            return category;
        }
        
        @Override
        public float getPitch() {
            return (float) pitch;
        }
        
        @Override
        public int getDelay() {
            return 0;
        }
        
        @Override
        public Sound getSound() {
            return soundeventaccessor.getSound(rand);
        }
        
        @Override
        public ResourceLocation getLocation() {
            return location;
        }
        
        @Override
        public float getVolume() {
            return effectiveVolume;
        }
        
        @Override
        public double getX() {
            return 0;
        }
        
        @Override
        public double getY() {
            return 0;
        }
        
        @Override
        public double getZ() {
            return 0;
        }
        
        @Override
        public boolean isStopped() {
            return false;
        }
        
        @Override
        public void tick() {
            
        }
        
        @Override
        public boolean isRelative() {
            return true;
        }
        
        @Override
        public boolean canStartSilent() {
            return true;
        }
        
        @Override
        public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary loader, ResourceLocation id, boolean looping) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Resource resource = ((SoundBufferLibraryAccessor) loader).getResourceManager().getResourceOrThrow(id);
                    InputStream inputstream = resource.open();
                    return looping ? new LoopingAudioStream(new AudioStreamProvider() {
                        
                        boolean first = true;
                        
                        @Override
                        public AudioStream create(InputStream inputstream) throws IOException {
                            try {
                                JOrbisAudioStream stream = new JOrbisAudioStream(inputstream);
                                if (first && currentPropertries.randomOffset && AmbientSounds.CONFIG.playSoundWithOffset)
                                    if (!((OggAudioStreamExtended) stream).setPositionRandomly(ResourceUtils.length(PackType.CLIENT_RESOURCES, resource, id), id)) {
                                        inputstream.reset();
                                        stream = new JOrbisAudioStream(inputstream);
                                    }
                                first = false;
                                return stream;
                            } catch (Exception e2) {
                                inputstream.reset();
                                return new JOrbisAudioStream(inputstream);
                            }
                        }
                    }, inputstream) : new JOrbisAudioStream(inputstream);
                } catch (IOException ioexception) {
                    AmbientSounds.LOGGER.error(ioexception);
                    throw new CompletionException(ioexception);
                }
            }, Util.nonCriticalIoPool());
        }
        
        public void collectDetails(DebugTextRenderer text) {
            text.text("[");
            text.detail("n", location);
            text.detail("v", effectiveVolume);
            text.detail("cv", conditionVolume());
            text.detail("i", index);
            text.detail("p", pitch);
            text.detail("t", ticksPlayed);
            text.detail("d", duration);
            text.text("]");
        }
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(name);
        if (stream1 != null)
            builder.append("[" + stream1 + "]");
        if (stream2 != null)
            builder.append("[" + stream2 + "]");
        if (inTransition())
            builder.append("t: " + transition + "/" + transitionTime);
        return builder.toString();
    }
    
    public AmbientSound copy() {
        AmbientSound copy = new AmbientSound();
        for (Field field : COPYFIELDS)
            try {
                field.set(copy, field.get(this));
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        return copy;
    }
    
}
