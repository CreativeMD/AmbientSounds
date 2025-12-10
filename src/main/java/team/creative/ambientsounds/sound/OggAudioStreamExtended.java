package team.creative.ambientsounds.sound;

import java.io.IOException;

import net.minecraft.resources.Identifier;

public interface OggAudioStreamExtended {
    
    public boolean setPositionRandomly(long length, Identifier id) throws IOException;
}
