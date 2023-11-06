package team.creative.ambientsounds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.server.packs.resources.ResourceProvider;

@Mixin(SoundBufferLibrary.class)
public interface SoundBufferLibraryAccessor {
    
    @Accessor
    public ResourceProvider getResourceManager();
}
