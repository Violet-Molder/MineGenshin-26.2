package com.linweiyun.genshin.content.skill_node;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class AreaEntityCollector {

    private final Level level;
    private final Vec3 pos1;
    private final Vec3 pos2;
    private final float inflate;

    public AreaEntityCollector(Level level, Vec3 pos1, Vec3 pos2) {
        this(level, pos1, pos2, 0.0F);
    }

    public AreaEntityCollector(Level level, Vec3 pos1, Vec3 pos2, float inflate) {
        this.level = level;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.inflate = inflate;
    }

    public List<LivingEntity> execute() {
        AABB aabb = new AABB(pos1, pos2).inflate(inflate);
        return level.getEntitiesOfClass(LivingEntity.class, aabb,
                entity -> entity.isAlive() && !entity.isSpectator());
    }
}