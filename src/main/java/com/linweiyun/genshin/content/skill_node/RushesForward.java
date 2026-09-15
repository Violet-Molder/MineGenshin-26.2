package com.linweiyun.genshin.content.skill_node;

import com.linweiyun.genshin.content.skill_node.math.HorizonEndVec3;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class RushesForward {

    private final Player player;
    private final float distance;

    public RushesForward(Player player, float distance) {
        this.player = player;
        this.distance = distance;
    }

    public Vec3 execute() {
        Vec3 delta = new HorizonEndVec3(player, distance).execute();
        if (player.level().isClientSide()) {
            DashSystem.startDash(player, delta);
        }
        return delta;
    }
}