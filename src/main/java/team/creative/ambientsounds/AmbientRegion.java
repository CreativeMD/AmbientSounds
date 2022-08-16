package team.creative.ambientsounds;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import team.creative.ambientsounds.env.AmbientEnvironment;
import team.creative.creativecore.common.config.api.CreativeConfig;

public class AmbientRegion extends AmbientCondition {
    
    public String name;
    @CreativeConfig.DecimalRange(min = 0, max = 1)
    public transient double volumeSetting = 1;
    public AmbientStackType stack = AmbientStackType.overwrite;
    protected transient boolean active;
    public transient LinkedHashMap<String, AmbientSound> sounds = new LinkedHashMap<>();
    
    transient List<AmbientSound> playing = new ArrayList<>();
    
    public transient AmbientDimension dimension;
    
    public AmbientRegion() {}
    
    public void load(AmbientEngine engine, Gson gson, ResourceManager manager) throws IOException {
        this.sounds = new LinkedHashMap<>();
        for (Resource resource : manager
                .getResourceStack(new ResourceLocation(AmbientSounds.MODID, engine.name + "/sounds/" + (dimension != null ? dimension.name + "." : "") + name + ".json"))) {
            InputStream input = resource.open();
            try {
                try {
                    AmbientSound[] sounds = gson.fromJson(JsonParser.parseString(IOUtils.toString(input, Charsets.UTF_8)), AmbientSound[].class);
                    for (int j = 0; j < sounds.length; j++) {
                        AmbientSound sound = sounds[j];
                        this.sounds.put(sound.name, sound);
                    }
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            } finally {
                input.close();
            }
        }
    }
    
    @Override
    public String regionName() {
        return name;
    }
    
    public void apply(AmbientRegion region) {
        for (Field field : getClass().getFields()) {
            if (Modifier.isTransient(field.getModifiers()) || Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers()))
                continue;
            
            try {
                region.stack.apply(this, field, region);
            } catch (IllegalArgumentException | IllegalAccessException e) {}
        }
    }
    
    @Override
    public void init(AmbientEngine engine) {
        super.init(engine);
        
        if (sounds != null)
            for (AmbientSound sound : sounds.values())
                sound.init(engine);
    }
    
    @Override
    public AmbientSelection value(AmbientEnvironment env) {
        if (dimension != null && dimension != env.dimension)
            return null;
        if (volumeSetting == 0)
            return null;
        AmbientSelection selection = super.value(env);
        if (selection != null)
            selection.volume *= volumeSetting;
        return selection;
    }
    
    public boolean fastTick(AmbientEnvironment env) {
        if (!playing.isEmpty()) {
            for (Iterator<AmbientSound> iterator = playing.iterator(); iterator.hasNext();) {
                AmbientSound sound = iterator.next();
                if (!sound.fastTick(env)) {
                    sound.deactivate();
                    iterator.remove();
                }
            }
        }
        
        return !playing.isEmpty();
    }
    
    public boolean tick(AmbientEnvironment env) {
        
        if (sounds == null)
            return false;
        
        AmbientSelection selection = value(env);
        for (AmbientSound sound : sounds.values()) {
            if (sound.tick(env, selection)) {
                if (!sound.isActive()) {
                    sound.activate();
                    playing.add(sound);
                }
            } else if (sound.isActive()) {
                sound.deactivate();
                playing.remove(sound);
            }
        }
        
        return !playing.isEmpty();
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void activate() {
        active = true;
    }
    
    public void deactivate() {
        active = false;
        
        if (!playing.isEmpty()) {
            for (AmbientSound sound : playing)
                sound.deactivate();
            playing.clear();
        }
    }
    
    @Override
    public String toString() {
        return name + ", playing: " + playing.size();
    }
    
}
