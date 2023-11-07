package team.creative.ambientsounds.entity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import team.creative.ambientsounds.AmbientCondition.AmbientMinMaxFadeCondition;
import team.creative.ambientsounds.AmbientEngine;
import team.creative.ambientsounds.env.AmbientEnvironment;

public class AmbientEntityCondition {
    
    public AmbientMinMaxFadeCondition distance;
    
    @SerializedName("distance-x")
    public AmbientMinMaxFadeCondition distanceX;
    @SerializedName("distance-y")
    public AmbientMinMaxFadeCondition distanceY;
    @SerializedName("distance-z")
    public AmbientMinMaxFadeCondition distanceZ;
    
    public AmbientMinMaxFadeCondition count;
    
    @JsonAdapter(StringJson.class)
    public String[] name;
    @SerializedName("bad-name")
    @JsonAdapter(StringJson.class)
    public String[] badName;
    
    @JsonAdapter(StringJson.class)
    public String[] type;
    @SerializedName("bad-type")
    @JsonAdapter(StringJson.class)
    public String[] badType;
    
    private transient List<EntityType> parsedType;
    private transient List<EntityType> parsedBadType;
    
    public Map<String, String> scores;
    @JsonAdapter(StringJson.class)
    public String[] tag;
    @SerializedName("bad-tag")
    @JsonAdapter(StringJson.class)
    public String[] badTag;
    
    @JsonAdapter(StringJson.class)
    public String[] team;
    @SerializedName("bad-team")
    @JsonAdapter(StringJson.class)
    public String[] badTeam;
    
    @JsonAdapter(StringJson.class)
    public String[] nbt;
    @SerializedName("bad-nbt")
    @JsonAdapter(StringJson.class)
    public String[] badNbt;
    
    private transient List<CompoundTag> parsedNbt;
    private transient List<CompoundTag> parsedBadNbt;
    
    public AmbientMinMaxFadeCondition level;
    
    public AmbientMinMaxFadeCondition x_rotation;
    public AmbientMinMaxFadeCondition y_rotation;
    
    public void init(AmbientEngine engine) {
        
        if (distance != null) { // Square Distance to save performance
            if (distance.min != null)
                distance.min *= distance.min;
            if (distance.max != null)
                distance.max *= distance.max;
            if (distance.fade != null)
                distance.fade *= distance.fade;
        }
        
        if (type != null) {
            parsedType = new ArrayList<>();
            for (String entityType : type) {
                ResourceLocation location = new ResourceLocation(entityType);
                var result = BuiltInRegistries.ENTITY_TYPE.getOptional(location);
                if (result.isPresent())
                    parsedType.add(result.get());
            }
        }
        
        if (badType != null) {
            parsedBadType = new ArrayList<>();
            for (String entityType : badType) {
                ResourceLocation location = new ResourceLocation(entityType);
                var result = BuiltInRegistries.ENTITY_TYPE.getOptional(location);
                if (result.isPresent())
                    parsedBadType.add(result.get());
            }
        }
        
        if (nbt != null) {
            parsedNbt = new ArrayList<>();
            for (String tag : nbt)
                try {
                    parsedNbt.add(TagParser.parseTag(tag));
                } catch (CommandSyntaxException e) {
                    e.printStackTrace();
                }
        }
        
        if (badNbt != null) {
            parsedBadNbt = new ArrayList<>();
            for (String tag : badNbt)
                try {
                    parsedBadNbt.add(TagParser.parseTag(tag));
                } catch (CommandSyntaxException e) {
                    e.printStackTrace();
                }
        }
    }
    
    public double value(AmbientEnvironment env) {
        double highest = 0;
        
        int counted = 0;
        
        for (Entity entity : env.entity.all()) {
            
            if (name != null && !checkEntityName(name, entity))
                continue;
            
            if (badName != null && checkEntityName(badName, entity))
                continue;
            
            if (parsedType != null && !parsedType.contains(entity.getType()))
                continue;
            
            if (parsedBadType != null && parsedBadType.contains(entity.getType()))
                continue;
            
            if (tag != null && !containsTag(tag, entity))
                continue;
            
            if (badTag != null && containsTag(badTag, entity))
                continue;
            
            if (team != null && !isTeam(team, entity))
                continue;
            
            if (badTeam != null && isTeam(badTeam, entity))
                continue;
            
            if (parsedNbt != null && !hasNbt(parsedNbt, entity))
                continue;
            
            if (parsedBadNbt != null && hasNbt(parsedBadNbt, entity))
                continue;
            
            double current = 1;
            
            if (distance != null)
                current *= distance.volume(env.entity.squaredDistance(entity));
            
            if (distanceX != null)
                current *= distanceX.volume(entity.position().x - env.entity.x());
            
            if (distanceY != null)
                current *= distanceY.volume(entity.position().y - env.entity.y());
            
            if (distanceZ != null)
                current *= distanceZ.volume(entity.position().z - env.entity.z());
            
            if (x_rotation != null)
                current *= x_rotation.volume(entity.getXRot());
            
            if (y_rotation != null)
                current *= y_rotation.volume(entity.getYRot());
            
            counted++;
            if (current == 1 && count == null)
                return 1;
            
            highest = Math.max(current, highest);
        }
        
        if (count != null)
            highest *= count.volume(counted);
        
        return highest;
    }
    
    private boolean hasNbt(List<CompoundTag> nbt, Entity entity) {
        CompoundTag tag = entity.saveWithoutId(new CompoundTag());
        for (CompoundTag check : nbt)
            if (NbtUtils.compareNbt(check, tag, true))
                return true;
        return false;
    }
    
    private boolean isTeam(String[] team, Entity entity) {
        if (!(entity instanceof LivingEntity))
            return false;
        return ArrayUtils.contains(team, entity.getTeam() == null ? "" : entity.getTeam().getName());
    }
    
    private boolean containsTag(String[] tag, Entity entity) {
        Set<String> tags = entity.getTags();
        for (int i = 0; i < tag.length; i++)
            if (tags.contains(tag[i]))
                return true;
        return false;
    }
    
    private boolean checkEntityName(String[] name, Entity entity) {
        if (entity instanceof Player p && ArrayUtils.contains(name, p.getGameProfile().getName()))
            return true;
        if (ArrayUtils.contains(name, entity.getStringUUID()))
            return true;
        if (entity.hasCustomName() && ArrayUtils.contains(name, entity.getCustomName().getString()))
            return true;
        return false;
    }
    
    public static class StringJson extends TypeAdapter<String[]> {
        
        @Override
        public void write(JsonWriter out, String[] value) throws IOException {
            if (value.length > 1) {
                out.beginArray();
                for (String string : value)
                    out.value(string);
                out.endArray();
            } else
                out.value(value.length == 0 ? "" : value[0]);
        }
        
        @Override
        public String[] read(JsonReader in) throws IOException {
            JsonToken token = in.peek();
            if (token == JsonToken.STRING) {
                String value = in.nextString();
                if (value.isEmpty())
                    return new String[0];
                return new String[] { value };
            }
            ArrayList<String> list = new ArrayList<>();
            in.beginArray();
            while (in.hasNext()) {
                list.add(in.nextString());
            }
            in.endArray();
            return list.toArray(new String[list.size()]);
        }
        
    }
    
}
