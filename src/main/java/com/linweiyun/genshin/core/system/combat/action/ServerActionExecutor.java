package com.linweiyun.genshin.core.system.combat.action;

import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData.ActionStep;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData.Hit;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData.Move;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端动作执行器。
 * <p>
 * 根据 ActionStep 配置，按延时（刻）触发位移和 AOE 伤害检测。
 * 延时通过 {@link ServerTickScheduler} 实现，主线程安全。
 */
public final class ServerActionExecutor {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ServerActionExecutor() {}

    /**
     * 执行一个 ActionStep 的所有延时/即时任务。
     * @param player 动作发起玩家
     * @param step   动作步骤配置
     * @param characterId 角色 ID (textureId)，用于日志追踪
     */
    public static void execute(Player player, ActionStep step, String characterId) {
        if (player.level().isClientSide()) return;

        for (Move move : step.moves) {
            if (move.delay > 0) {
                ServerTickScheduler.schedule(move.delay, () -> applyMove(player, move));
            } else {
                applyMove(player, move);
            }
        }

        for (Hit hit : step.hits) {
            if (hit.delay > 0) {
                ServerTickScheduler.schedule(hit.delay, () -> applyHit(player, hit, characterId));
            } else {
                applyHit(player, hit, characterId);
            }
        }
    }

    // ==================== 位移 ====================

    private static void applyMove(Entity entity, Move move) {
        if (!entity.isAlive() || entity.isRemoved()) return;
        Vec3 look = entity.getLookAngle();
        entity.setDeltaMovement(entity.getDeltaMovement().add(
                look.x * move.speed, look.y * move.speed, look.z * move.speed));
        entity.hurtMarked = true;
    }

    // ==================== AOE 伤害检测 ====================

    private static void applyHit(Entity source, Hit hit, String characterId) {
        if (!source.isAlive() || source.isRemoved()) return;

        Vec3 look = source.getLookAngle();
        Vec3 center = source.position().add(
                look.x * hit.forward,
                hit.yOffset + source.getEyeHeight() * 0.5,
                look.z * hit.forward);
        double r = hit.scope;

        List<LivingEntity> targets = new ArrayList<>();
        for (LivingEntity e : source.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(center.x - r, center.y - r, center.z - r,
                        center.x + r, center.y + r, center.z + r))) {
            if (e == source) continue;
            if (!e.isAlive()) continue;
            targets.add(e);
        }

        LOGGER.debug("[ActionExecutor] Hit {} targets for {}, damage={}, scope={}",
                targets.size(), characterId, hit.damage, hit.scope);
    }
}