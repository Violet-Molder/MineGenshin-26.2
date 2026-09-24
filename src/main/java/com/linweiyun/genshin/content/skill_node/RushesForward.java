package com.linweiyun.genshin.content.skill_node;

import com.linweiyun.genshin.content.skill_node.HorizonEndVec3;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 向前突进的方向计算。
 * <p>
 * 本类<b>不启动</b>任何 Dash —— 只负责算出水平方向的位移向量。
 * 调用方拿到 delta 后自行决定：
 * <ul>
 *     <li>纯位移：{@code DashSystem.startDash(player, delta, ticks)}</li>
 *     <li>带伤害：{@code DashSystem.startDamageDash(player, delta, ticks, onHit)}</li>
 * </ul>
 */
public class RushesForward {

    private final Player player;
    private final float distance;

    public RushesForward(Player player, float distance) {
        this.player = player;
        this.distance = distance;
    }

    public Vec3 execute() {
        return new HorizonEndVec3(player, distance).execute();
    }
}