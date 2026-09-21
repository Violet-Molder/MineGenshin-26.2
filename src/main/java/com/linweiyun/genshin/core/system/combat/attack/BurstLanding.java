package com.linweiyun.genshin.core.system.combat.attack;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 「这一发跃起下坠大招的落点在哪」—— 客户端算好之后交给服务端。
 *
 * <h2>为什么要传</h2>
 * 下坠类大招<b>不追踪</b>：落点在「摆好姿态、开始下坠」的那一刻就定死了
 * （角度取决于当时双方的位置关系）。这个决定在客户端做（位移也是客户端驱动的），
 * 但落地那一下的伤害在服务端结算 —— 所以必须把落点带过去，
 * 否则服务端只能拿「当下玩家的位置」，那又变成追踪了。
 *
 * <p>时序：客户端在下坠开始那一 tick 发落点 → 服务端在 {@code hits[].delay}
 * （= 跃起 + 下坠的刻数）那一刻取出它算伤害。中间隔着十几刻，所以是「先存后取」。
 *
 * <p>纯服务端状态（客户端也会调 {@code set}，但只在本地存一份，不影响服务端）。
 */
public final class BurstLanding {

    private static final Map<UUID, Vec3> PENDING = new ConcurrentHashMap<>();

    private BurstLanding() {
    }

    /** 记下这一发大招的落点（后来的覆盖先前的）。 */
    public static void set(Player player, Vec3 landing) {
        if (player == null || landing == null) {
            return;
        }
        PENDING.put(player.getUUID(), landing);
    }

    /** 看一眼落点（不清）。 */
    @Nullable
    public static Vec3 peek(Player player) {
        return player == null ? null : PENDING.get(player.getUUID());
    }

    /** 取走落点（伤害结算完就清掉，下一发重新来）。 */
    @Nullable
    public static Vec3 consume(Player player) {
        return player == null ? null : PENDING.remove(player.getUUID());
    }

    /** 玩家下线/换角色时清掉。 */
    public static void clear(Player player) {
        if (player != null) {
            PENDING.remove(player.getUUID());
        }
    }
}
