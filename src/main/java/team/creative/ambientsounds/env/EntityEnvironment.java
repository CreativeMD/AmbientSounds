package team.creative.ambientsounds.env;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import team.creative.ambientsounds.AmbientDimension;

public class EntityEnvironment {
    
    private List<Entity> entities = new ArrayList<>();
    private Vec3 position;
    
    public void analyzeFast(AmbientDimension dimension, Player player, Level level, float deltaTime) {
        position = player.getEyePosition(deltaTime);
        entities.clear();
        for (Entity entity : ((ClientLevel) level).entitiesForRendering())
            entities.add(entity);;
    }
    
    public double squaredDistance(Entity entity) {
        return entity.distanceToSqr(position);
    }
    
    public double x() {
        return position.x;
    }
    
    public double y() {
        return position.y;
    }
    
    public double z() {
        return position.z;
    }
    
    public Iterable<Entity> all() {
        return entities;
    }
    
}
