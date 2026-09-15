package com.linweiyun.genshin.content.skill_node;

import com.linweiyun.genshin.content.skill_node.math.HorizonEndVec3;
import net.minecraft.world.entity.player.Player;

public class RushesForward {

    private final Player player;
    private final float distance;

    public RushesForward(Player player, float distance) {
        this.player = player;
        this.distance = distance;
    }

    public void execute() {
        player.setDeltaMovement(new HorizonEndVec3(player, distance).execute());
    }
}