package team.creative.ambientsounds.mixin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import javax.sound.sampled.AudioFormat;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;

import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.resources.Identifier;
import team.creative.ambientsounds.AmbientSounds;
import team.creative.ambientsounds.sound.OggAudioStreamExtended;

@Mixin(JOrbisAudioStream.class)
public abstract class OggAudioStreamMixin implements OggAudioStreamExtended {
    
    private static final Random RANDOM = new Random();
    
    @Shadow
    @Final
    private AudioFormat audioFormat;
    
    @Shadow
    @Final
    private InputStream input;
    
    @Override
    public boolean setPositionRandomly(long length, Identifier id) throws IOException {
        if (length == 0)
            return true;
        int skipped = RANDOM.nextInt((int) (length - length / 4));
        input.skipNBytes(skipped);
        int searched = 0;
        int errors = 0;
        while (true) {
            try {
                for (int i = 0; i < 2; i++) {
                    searched++;
                    if (!((JOrbisAudioStream) (Object) this).readChunk(x -> {})) {
                        AmbientSounds.LOGGER.error("Possibly reached end of file {}/{}", skipped, length);
                        return false;
                    }
                }
            } catch (IOException | IllegalStateException e) {
                try {
                    errors++;
                    readToBuffer();
                } catch (IOException e2) {
                    AmbientSounds.LOGGER.error("Failed to play sound with offset {}/{}", skipped, length);
                    AmbientSounds.LOGGER.error(e2);
                    return false;
                }
                continue;
            }
            if (searched < 6 || (errors < 1 && searched < 512))
                continue;
            return true;
        }
    }
    
    @Shadow
    private Packet readPacket() throws IOException {
        throw new UnsupportedOperationException();
    }
    
    @Shadow
    private Page readPage() throws IOException {
        throw new UnsupportedOperationException();
    }
    
    @Shadow
    private boolean readToBuffer() throws IOException {
        throw new UnsupportedOperationException();
    }
    
}
