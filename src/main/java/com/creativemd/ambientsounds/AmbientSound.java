package com.creativemd.ambientsounds;

import java.util.Arrays;
import java.util.Random;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

public class AmbientSound extends AmbientCondition {
	
	private static Random rand = new Random();
	
	public transient double volumeSetting = 1;
	public String name;
	public transient String fullName;
	public ResourceLocation[] files;
	public double[] chances;
	
	public transient SoundStream stream1;
	public transient SoundStream stream2;
	
	protected transient boolean active;
	
	protected transient float aimedVolume;
	protected transient float currentVolume;
	protected transient float aimedPitch;
	protected transient int transition;
	protected transient int transitionTime;
	
	protected transient int pauseTimer = -1;
	
	protected transient AmbientSoundProperties currentPropertries;
	protected transient AmbientEngine engine;
	
	@Override
	public void init(AmbientEngine engine) {
		if (files == null || files.length == 0)
			throw new RuntimeException("Invalid sound " + name + " which does not contain any sound file");
		
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
	}
	
	protected int getRandomFile() {
		if (files.length == 1)
			return 0;
		return rand.nextInt(files.length);
	}
	
	protected int getRandomFileExcept(int i) {
		if (files.length == 2)
			return i == 0 ? 1 : 0;
		int index = rand.nextInt(files.length - 1);
		if (index >= i)
			index++;
		return index;
	}
	
	public boolean fastTick() {
		
		if (currentVolume < aimedVolume)
			currentVolume += Math.min(currentPropertries.fadeInVolume, aimedVolume - currentVolume);
		else if (currentVolume > aimedVolume)
			currentVolume -= Math.min(currentPropertries.fadeOutVolume, currentVolume - aimedVolume);
		
		if (isPlaying()) {
			
			if (inTransition()) { // Two files are played
				stream1.volume = Math.max(0, Math.min(stream1.volume, getCombinedVolume() * (1D - (double) transition / transitionTime)));
				stream2.volume = Math.min(getCombinedVolume(), getCombinedVolume() * ((double) transition / transitionTime));
				
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
				
				stream1.volume = getCombinedVolume();
				
				if (currentPropertries.length != null) { // If the sound has a length
					
					if (currentPropertries.pause == null && files.length > 1) { // Continuous transition
						if (stream1.remaining() <= 0) {
							transition = 0;
							stream2 = play(getRandomFileExcept(stream1.index));
							stream2.volume = 0;
							transitionTime = currentPropertries.transition != null ? currentPropertries.transition : 60;
						}
					} else {
						int fadeOutTime = (int) Math.ceil(aimedVolume / currentPropertries.fadeOutVolume);
						
						if (stream1.remaining() <= 0) { // Exceeded length
							engine.soundEngine.stop(stream1);
							stream1 = null;
							pauseTimer = -1;
						} else if (fadeOutTime > stream1.remaining()) // about to exceed length -> fade out
							stream1.volume = getCombinedVolume() * stream1.remaining() / fadeOutTime;
					}
				}
			}
			
			if (stream1 != null) {
				
				if (stream1.pitch < aimedPitch)
					stream1.pitch += Math.min(currentPropertries.fadeInPitch, aimedPitch - stream1.pitch);
				else if (stream1.pitch > aimedPitch)
					stream1.pitch -= Math.min(currentPropertries.fadeOutPitch, stream1.pitch - aimedPitch);
				stream1.ticksPlayed++;
			}
			if (stream2 != null) {
				
				if (stream2.pitch < aimedPitch)
					stream2.pitch += Math.min(currentPropertries.fadeInPitch, aimedPitch - stream2.pitch);
				else if (stream2.pitch > aimedPitch)
					stream2.pitch -= Math.min(currentPropertries.fadeOutPitch, stream2.pitch - aimedPitch);
				stream2.ticksPlayed++;
			}
		} else {
			
			if (stream2 != null) {
				engine.soundEngine.stop(stream2);
				stream2 = null;
			}
			
			if (pauseTimer == -1)
				if (currentPropertries.pause != null)
					pauseTimer = (int) currentPropertries.pause.randomValue();
				
			if (pauseTimer <= 0)
				stream1 = play(getRandomFile());
			else
				pauseTimer--;
		}
		
		return aimedVolume > 0 || currentVolume > 0;
	}
	
	@Override
	public AmbientSelection value(AmbientEnviroment env) {
		if (volumeSetting == 0)
			return null;
		return super.value(env);
	}
	
	public boolean tick(AmbientEnviroment env, AmbientSelection selection) {
		if (selection != null) {
			AmbientSelection soundSelection = value(env);
			
			if (soundSelection != null) {
				AmbientSelection last = selection.getLast();
				last.subSelection = soundSelection;
				aimedVolume = (float) selection.getEntireVolume();
				currentPropertries = selection.getProperties();
				last.subSelection = null;
				
				aimedPitch = MathHelper.clamp(currentPropertries.getPitch(env), 0.5F, 2.0F);
			} else
				aimedVolume = 0;
		} else
			aimedVolume = 0;
		
		return aimedVolume > 0 || currentVolume > 0;
	}
	
	protected SoundStream play(int index) {
		SoundStream stream = new SoundStream(index);
		stream.pitch = aimedPitch;
		if (currentPropertries.length != null)
			stream.duration = (int) currentPropertries.length.randomValue();
		
		engine.soundEngine.play(randomOffset(), stream);
		return stream;
	}
	
	protected int randomOffset() {
		//return currentPropertries.offset != null ? (int) currentPropertries.offset.randomValue() : 0;
		return 0;
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
	
	public double getCombinedVolume() {
		return currentVolume * volumeSetting;
	}
	
	public class SoundStream {
		
		public final int index;
		public final ResourceLocation location;
		
		public String systemName;
		
		public double volume;
		public double pitch;
		public int duration = -1;
		public int ticksPlayed = 0;
		
		private boolean finished = false;
		private boolean playedOnce;
		
		public SoundStream(int index) {
			this.index = index;
			this.location = AmbientSound.this.files[index];
			this.volume = AmbientSound.this.getCombinedVolume();
		}
		
		public boolean loop() {
			return AmbientSound.this.loop();
		}
		
		public int remaining() {
			return duration - ticksPlayed;
		}
		
		public double mute() {
			return AmbientSound.this.currentPropertries.mute * volume;
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
			return "l" + location + "v:" + (Math.round((double) volume * 100) / 100D) + ",i:" + index + ",p:" + pitch + ",t:" + ticksPlayed + ",d:" + duration;
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
	
}
