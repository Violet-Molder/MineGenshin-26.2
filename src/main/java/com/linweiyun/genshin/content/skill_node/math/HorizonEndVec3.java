package com.linweiyun.genshin.content.skill_node.math;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class HorizonEndVec3 {

    private final Player player;
    private final float distance;

    public HorizonEndVec3(Player player) {
        this(player, 1.0f);
    }

    public HorizonEndVec3(Player player, float distance) {
        this.player = player;
        this.distance = distance;
    }

    public Vec3 execute() {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0, look.z);
        if (horizontal.lengthSqr() < 1e-6) {
            double rad = Math.toRadians(player.getYRot());
            horizontal = new Vec3(-Math.sin(rad), 0, Math.cos(rad));
        }
        return horizontal.normalize().scale(distance);
    }
}