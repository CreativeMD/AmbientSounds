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
	protected Sound field_184367_a;
	private SoundEventAccessor field_184369_l;
    public final ResourceLocation sound;
    public float volume = 1.0F;
    public float pitch = 1.0F;
    public float xPos = 0;
    public float yPos = 0;
    public float zPos = 0;
    public boolean repeat = true;
    public int delay = 0;
    public ISound.AttenuationType type;
    public boolean donePlaying = false;

    public IEnhancedPositionSound(ResourceLocation sound, float volume, float pitch)
    {
        this.type = ISound.AttenuationType.NONE;
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }
    
    @Override
    public ResourceLocation getSoundLocation()
    {
        return this.sound;
    }
    
    @Override
    public boolean canRepeat()
    {
        return this.repeat;
    }
    
    @Override
    public int getRepeatDelay()
    {
        return this.delay;
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
	public void update() {
		
	}

	@Override
	public boolean isDonePlaying() {
		return donePlaying;
	}

	@Override
	public SoundEventAccessor func_184366_a(SoundHandler p_184366_1_) {
		this.field_184369_l = p_184366_1_.func_184398_a(getSoundLocation());

        if (this.field_184369_l == null)
        {
           this.field_184367_a = SoundHandler.missing_sound;
        }
        else
        {
            this.field_184367_a = this.field_184369_l.cloneEntry();
        }

        return this.field_184369_l;
	}

	@Override
	public Sound getSound() {
		return this.field_184367_a;
	}

	@Override
	public SoundCategory getCategory() {
		return SoundCategory.AMBIENT;
	}
}