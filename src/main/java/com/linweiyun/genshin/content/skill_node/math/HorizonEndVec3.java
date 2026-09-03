package com.linweiyun.genshin.content.skill_node.math;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class HorizonEndVec3 {
    public static Vec3 execute(Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0, look.z);
        if (horizontal.lengthSqr() < 1e-6) {
            double rad = Math.toRadians(player.getYRot());
            horizontal = new Vec3(-Math.sin(rad), 0, Math.cos(rad));
        }
        return horizontal.normalize();
    }
    public static Vec3 execute(Player player, float distance) {
        Vec3 dir = execute(player);
        return dir.scale(distance);
    }
}
