package com.creativemd.ambientsounds;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.ITickableSound;
import net.minecraft.client.audio.Sound;
import net.minecraft.client.audio.SoundEventAccessor;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class IEnhancedPositionSound implements ITickableSound
{
	protected Sound sound;
	private SoundEventAccessor soundEvent;
    public final ResourceLocation resource;
    public float volume = 1.0F;
    public float pitch = 1.0F;
    public float xPos = 0;
    public float yPos = 0;
    public float zPos = 0;
    public boolean repeat = true;
    public int delay = 0;
    public ISound.AttenuationType type;
    public boolean playing = false;
    
    public boolean hasBeenAdded = false;
    
    public String systemName;
    
    public IEnhancedPositionSound(ResourceLocation resource, float volume, float pitch)
    {
        this.type = ISound.AttenuationType.NONE;
        this.resource = resource;
        this.volume = volume;
        this.pitch = pitch;
    }
    
    @Override
    public ResourceLocation getSoundLocation()
    {
        return this.resource;
    }
    
    @Override
    public boolean canRepeat()
    {
        return this.repeat;
    }
    
    @Override
    public int getRepeatDelay()
    {
        return delay;
    }
    
    @Override
    public float getVolume()
    {
        return this.volume;
    }
    
    @Override
    public float getPitch()
    {
        return this.pitch;
    }
    
    @Override
    public float getXPosF()
    {
        return this.xPos;
    }
    
    @Override
    public float getYPosF()
    {
        return this.yPos;
    }
    
    @Override
    public float getZPosF()
    {
        return this.zPos;
    }
    
    @Override
    public ISound.AttenuationType getAttenuationType()
    {
        return this.type;
    }

	@Override
	public SoundEventAccessor createAccessor(SoundHandler handler) {
		this.soundEvent = handler.getAccessor(this.resource);

        if (this.soundEvent == null)
        {
            this.sound = SoundHandler.MISSING_SOUND;
        }
        else
        {
            this.sound = this.soundEvent.cloneEntry();
        }
        
        return this.soundEvent;
	}

	@Override
	public Sound getSound() {
		return this.sound;
	}

	@Override
	public SoundCategory getCategory() {
		return SoundCategory.AMBIENT;
	}

	@Override
	public void update() {
		
	}

	@Override
	public boolean isDonePlaying() {
		return false;
	}
}