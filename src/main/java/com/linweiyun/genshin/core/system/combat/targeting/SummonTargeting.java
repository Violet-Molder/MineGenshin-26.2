package com.linweiyun.genshin.core.system.combat.targeting;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * 召唤物的索敌模式。
 *
 * <h2>为什么按「玩家」而不是「角色」取目标</h2>
 * 召唤物是角色召唤的，但那个角色可能已经挂到后台（玩家切人了），
 * 而玩家本身一直在场。所以「主人当前在打谁」要问<b>玩家</b>。
 * 服务端的锁由攻击请求包喂进 {@link CombatTargeting}，所以服务端也读得到。
 */
public enum SummonTargeting {

    /**
     * 优先咬主人的当前锁定目标（默认，大多数召唤物）。
     *
     * <p>主人没锁定时，保持自己已有的目标；自己也没有才独立找。
     */
    OWNER_TARGET,

    /** 主人有锁定就咬它，否则永远自己找（不保留自己的旧目标）。 */
    OWNER_TARGET_ELSE_SELF,

    /** 完全独立索敌：自己找自己的，无视主人在打谁。 */
    INDEPENDENT;

    /** 默认模式 —— 新增召唤物不改就用这个。 */
    public static SummonTargeting defaultMode() {
        return OWNER_TARGET;
    }

    /**
     * 算出这只召唤物这一刻该打谁。
     *
     * @param ownerPlayer     召唤者的玩家实体；可能为 null（离线/未绑定）
     * @param ownTarget       召唤物自己已有的目标；没有传 null
     * @param range           召唤物自己的索敌半径
     * @param independentSeeker 完全独立索敌时用哪个方法找（把调用方原有的 {@code TargetSeeker} 传进来）
     * @return 这一刻的目标；没有就 null
     */
    @Nullable
    public LivingEntity resolve(@Nullable Player ownerPlayer,
                               @Nullable LivingEntity ownTarget,
                               double range,
                               Supplier<@Nullable LivingEntity> independentSeeker) {

        LivingEntity fromOwner = ownerTargetWithinRange(ownerPlayer, range);

        return switch (this) {
            // 主人指定了目标就听主人的；主人没在打就自己找
            case OWNER_TARGET -> {
                if (fromOwner != null) {
                    yield fromOwner;
                }
                if (ownTarget != null && ownTarget.isAlive()) {
                    yield ownTarget;
                }
                yield independentSeeker.get();
            }
            // 严格跟主人：主人没目标就现找（不留旧目标）
            case OWNER_TARGET_ELSE_SELF -> fromOwner != null ? fromOwner : independentSeeker.get();
            // 完全独立
            case INDEPENDENT -> (ownTarget != null && ownTarget.isAlive())
                    ? ownTarget
                    : independentSeeker.get();
        };
    }

    /**
     * 主人当前锁定的目标，且必须在召唤物的索敌半径内。
     *
     * <p>放宽到 {@code range * 1.5}：主人打得比召唤物远一点是正常的，
     * 完全不放宽会导致召唤物总在主人刚锁定时空转。
     */
    @Nullable
    private static LivingEntity ownerTargetWithinRange(@Nullable Player ownerPlayer, double range) {
        if (ownerPlayer == null || !ownerPlayer.isAlive()) {
            return null;
        }
        LivingEntity target = CombatTargeting.current(ownerPlayer);
        if (target == null || !target.isAlive()) {
            return null;
        }
        double limit = range * 1.5;
        return ownerPlayer.distanceToSqr(target) <= limit * limit ? target : null;
    }
}
