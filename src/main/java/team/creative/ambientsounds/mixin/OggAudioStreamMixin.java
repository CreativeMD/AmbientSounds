package team.creative.ambientsounds.mixin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Random;

import javax.sound.sampled.AudioFormat;

import org.lwjgl.PointerBuffer;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.audio.OggAudioStream;

import team.creative.ambientsounds.sound.OggAudioStreamExtended;

@Mixin(OggAudioStream.class)
public abstract class OggAudioStreamMixin implements OggAudioStreamExtended {
    
    private static final Random RANDOM = new Random();
    
    @Shadow
    private long handle;
    
    @Shadow
    @Final
    private AudioFormat audioFormat;
    
    @Shadow
    @Final
    private InputStream input;
    
    @Shadow
    private ByteBuffer buffer;
    
    @Override
    public void setPositionRandomly(long length) throws IOException {
        if (length == 0)
            return;
        int skipped = RANDOM.nextInt((int) (length - length / 4));
        input.skipNBytes(skipped);
        STBVorbis.stb_vorbis_flush_pushdata(handle);
        buffer.limit(0);
        refillFromStream();
        if (!seekTillPage())
            throw new IOException("No page found till end of file.");
    }
    
    private boolean seekTillPage() throws IOException {
        try (MemoryStack memorystack = MemoryStack.stackPush()) {
            PointerBuffer data = memorystack.mallocPointer(1);
            IntBuffer channels = memorystack.mallocInt(1);
            IntBuffer samples = memorystack.mallocInt(1);
            
            while (true) {
                int used = STBVorbis.stb_vorbis_decode_frame_pushdata(this.handle, this.buffer, channels, data, samples);
                this.buffer.position(this.buffer.position() + used);
                int error = STBVorbis.stb_vorbis_get_error(this.handle);
                if (error == 1) {
                    this.forwardBuffer();
                    if (!this.refillFromStream())
                        return false;
                    continue;
                }
                
                if (error != 0)
                    throw new IOException("Failed to read Ogg file " + error);
                
                if (used == 0) {
                    buffer.limit(0);
                    refillFromStream();
                    continue;
                }
                
                int k = samples.get(0);
                if (k != 0) {
                    int l = channels.get(0);
                    if (l != 2 && l != 1)
                        throw new IllegalStateException("Invalid number of channels: " + l);
                    return true;
                }
            }
        }
    }
    
    @Shadow
    private void forwardBuffer() {
        throw new UnsupportedOperationException();
    }
    
    @Shadow
    private boolean refillFromStream() throws IOException {
        throw new UnsupportedOperationException();
    }
    
}
