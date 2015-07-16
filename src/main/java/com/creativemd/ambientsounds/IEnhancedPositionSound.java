package com.creativemd.ambientsounds;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.ITickableSound;
import net.minecraft.util.ResourceLocation;

@SideOnly(Side.CLIENT)
public class IEnhancedPositionSound implements ITickableSound
{
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
    public ResourceLocation getPositionedSoundLocation()
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
}