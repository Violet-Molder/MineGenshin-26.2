package com.linweiyun.genshin.content.skill_node;

import com.linweiyun.genshin.content.skill_node.math.HorizonEndVec3;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class RushesForward {

    public static void execute(Player player, float distance) {
        Vec3 dash = HorizonEndVec3.execute(player, distance);
        player.setDeltaMovement(dash);
    }
}
