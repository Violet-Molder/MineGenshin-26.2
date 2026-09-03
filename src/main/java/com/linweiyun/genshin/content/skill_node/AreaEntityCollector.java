package com.linweiyun.genshin.content.skill_node;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class AreaEntityCollector {
    public static List<LivingEntity> execute(Level level, Vec3 pos1, Vec3 pos2) {
        return execute(level, pos1, pos2, 0.0F);
    }
    public static List<LivingEntity> execute(Level level, Vec3 pos1, Vec3 pos2, float inflate) {
        AABB aabb = new AABB(pos1, pos2).inflate(inflate);
        return level.getEntitiesOfClass(LivingEntity.class, aabb,
                entity -> entity.isAlive() && !entity.isSpectator());
    }
}