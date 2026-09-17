package com.linweiyun.genshin.content.skill_node;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.LockedTargetData;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.List;

/**
 * 索敌节点——从视野或方圆范围内选取最优目标并锁定。
 * <p>
 * 两种索敌模式：
 * <ul>
 *   <li>{@link TargetingType#LINE_OF_SIGHT LINE_OF_SIGHT}（视野索敌）：以实体视线方向延伸 range 距离，
 *       横截面 5×5 的隧道，适合玩家角色。</li>
 *   <li>{@link TargetingType#RADIUS RADIUS}（方圆索敌）：以实体为中心、range 为半径的球形范围，
 *       适合召唤物。</li>
 * </ul>
 * <p>
 * 锁定规则：
 * <ol>
 *   <li>敌对生物优先级最高。如果已锁定的是友好生物、但附近出现更近的敌对生物，立即切过去。</li>
 *   <li>锁定通过玩家 Attachment 持久化，不同技能节点共享同一个锁定目标。</li>
 *   <li>每次 execute() 命中锁定目标时，锁定时间<b>重置为 4 秒（80 tick）</b>，保证连续攻击不丢目标。</li>
 *   <li>4 秒不攻击则锁自动解除，下次 execute() 重新索敌。</li>
 * </ol>
 * <p>
 * 用法：{@code LivingEntity target = new TargetSeeker(source, 10.0, LINE_OF_SIGHT).execute();}
 */
public class TargetSeeker {

    public enum TargetingType {
        /** 视野索敌：沿视线方向延伸，5×5 隧道 */
        LINE_OF_SIGHT,
        /** 方圆索敌：以实体为中心的球形范围 */
        RADIUS
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 视野索敌隧道截面半径的一半（2.5 格） */
    private static final double TUNNEL_HALF_SIZE = 2.5;

    /** 优先级阈值：range × 1/2。友好物在阈值内且敌对在阈值外时优先友好 */
    private static final double PRIORITY_THRESHOLD = 0.5;

    /** 距离平方比较时的小容差，避免浮点边缘漏判 */
    private static final double RANGE_SQ_EPSILON = 4.0;

    /** 锁定持续时间：4 秒 = 80 tick */
    private static final int LOCK_DURATION_TICKS = 80;

    /** 发起索敌的实体（玩家或召唤物） */
    private final Entity source;

    /** 索敌范围/距离 */
    private final double range;

    /** 索敌类型 */
    private final TargetingType type;

    /**
     * @param source 发起索敌的实体
     * @param range  索敌范围（视野模式为延伸距离，方圆模式为半径）
     * @param type   索敌类型
     */
    public TargetSeeker(Entity source, double range, TargetingType type) {
        this.source = source;
        this.range = range;
        this.type = type;
    }

    // ==================== 主流程 ====================

    /**
     * 执行索敌，返回最优目标（已有锁定时优先返回锁定目标）。
     *
     * @return 目标实体，未找到返回 null
     */
    public LivingEntity execute() {
        if (source == null || range <= 0) return null;

        Level level = source.level();
        long tick = level.getGameTime();

        // 1. 检查已有锁
        LockedTargetData lockData = source.getData(AttachmentRegistration.LOCKED_TARGET);
        if (lockData.isValid(tick)) {
            Entity locked = level.getEntity(lockData.targetId());
            if (locked instanceof LivingEntity livingLocked
                    && locked.isAlive()
                    && source.distanceTo(livingLocked) <= range
                    && hasVisionOf(livingLocked)) {

                // 锁定的是敌对生物：直接返回，刷新锁定时间
                if (livingLocked instanceof Monster) {
                    refreshLock(tick, livingLocked);
                    return livingLocked;
                }

                // 锁定的是友好生物：检查是否有更近的敌对可以抢
                LivingEntity closerHostile = findClosestHostileInRange();
                if (closerHostile != null && source.distanceToSqr(closerHostile) < source.distanceToSqr(livingLocked)) {
                    // 更近的敌对生物抢锁
                    source.setData(AttachmentRegistration.LOCKED_TARGET,
                            new LockedTargetData(closerHostile.getUUID(), tick + LOCK_DURATION_TICKS));
                    return closerHostile;
                }

                // 无更近敌对，保持友好锁并刷新
                refreshLock(tick, livingLocked);
                return livingLocked;
            }
            // 目标不在范围内 / 死亡 / 不可见 → 清锁重搜
            source.setData(AttachmentRegistration.LOCKED_TARGET, LockedTargetData.EMPTY);
        }

        // 2. 无锁 → 按类型收集候选
        List<LivingEntity> candidates = switch (type) {
            case LINE_OF_SIGHT -> collectLineOfSight();
            case RADIUS -> collectRadius();
        };

        if (candidates.isEmpty()) return null;

        // 3. 优先级排序
        LivingEntity target = selectBestTarget(candidates);

        // 4. 锁定新目标
        if (target != null) {
            source.setData(AttachmentRegistration.LOCKED_TARGET,
                    new LockedTargetData(target.getUUID(), tick + LOCK_DURATION_TICKS));
            LOGGER.debug("[TargetSeeker] 新锁定: {} (过期tick={})", target.getName().getString(), tick + LOCK_DURATION_TICKS);
        }

        return target;
    }

    // ==================== 锁定 / 刷新 ====================

    /**
     * 判定实体是否对目标有视线连通（用于锁定状态扩大搜索时使用）。
     * 条件：无障碍物遮挡 且 目标在正前方。
     */
    private boolean hasVisionOf(Entity target) {
        if (source instanceof LivingEntity livingSource) {
            if (!livingSource.hasLineOfSight(target)) return false;
            Vec3 lookDir = source.getLookAngle().normalize();
            Vec3 toTarget = target.getBoundingBox().getCenter()
                    .subtract(source.getEyePosition()).normalize();
            return toTarget.dot(lookDir) > 0.3;
        }
        return true;
    }

    /**
     * 刷新锁定时间为 80 tick，保证连续攻击不丢目标。
     */
    private void refreshLock(long tick, LivingEntity target) {
        source.setData(AttachmentRegistration.LOCKED_TARGET,
                new LockedTargetData(target.getUUID(), tick + LOCK_DURATION_TICKS));
    }

    /** 清空锁定，切换技能或目标时调用 */
    public static void clearLock(Entity source) {
        source.setData(AttachmentRegistration.LOCKED_TARGET, LockedTargetData.EMPTY);
    }

    // ==================== 候选收集 ====================

    /**
     * 视野索敌：沿视线方向延伸 range 距离，横截面 5×5 隧道。
     * 只收集隧道内的活体。
     */
    private List<LivingEntity> collectLineOfSight() {
        Vec3 eyePos = source.getEyePosition();
        Vec3 lookDir = source.getLookAngle().normalize();
        Vec3 endPos = eyePos.add(lookDir.scale(range));

        // 粗筛：包围盒
        AABB searchBox = new AABB(eyePos, endPos).inflate(TUNNEL_HALF_SIZE);

        return source.level().getEntitiesOfClass(LivingEntity.class, searchBox, entity ->
                entity.isAlive()
                        && !entity.isSpectator()
                        && entity != source
                        && isInForwardTunnel(entity)
        );
    }

    /**
     * 精确判断目标是否在视线隧道内：投影到视线方向，垂直偏离 ≤ 2.5 格。
     */
    private boolean isInForwardTunnel(Entity target) {
        Vec3 eyePos = source.getEyePosition();
        Vec3 lookDir = source.getLookAngle().normalize();
        Vec3 targetCenter = target.getBoundingBox().getCenter();
        Vec3 toTarget = targetCenter.subtract(eyePos);

        // 沿视线方向的投影距离
        double forwardDist = toTarget.dot(lookDir);
        if (forwardDist < 0 || forwardDist > range) return false;

        // 垂直偏离距离
        Vec3 projected = eyePos.add(lookDir.scale(forwardDist));
        Vec3 perpendicular = targetCenter.subtract(projected);

        return perpendicular.lengthSqr() <= TUNNEL_HALF_SIZE * TUNNEL_HALF_SIZE;
    }

    /**
     * 方圆索敌：以实体为中心，range 为半径的球形范围。
     */
    private List<LivingEntity> collectRadius() {
        double rangeSq = range * range + RANGE_SQ_EPSILON;
        AABB searchBox = source.getBoundingBox().inflate(range);

        return source.level().getEntitiesOfClass(LivingEntity.class, searchBox, entity ->
                entity.isAlive()
                        && !entity.isSpectator()
                        && entity != source
                        && source.distanceToSqr(entity) <= rangeSq
        );
    }

    /**
     * 在锁定友好目标时，查找范围内是否有更近的敌对生物可抢锁。
     */
    private LivingEntity findClosestHostileInRange() {
        double rangeSq = range * range + RANGE_SQ_EPSILON;
        AABB searchBox = source.getBoundingBox().inflate(range);

        return source.level().getEntitiesOfClass(LivingEntity.class, searchBox, entity ->
                        entity instanceof Monster
                                && entity.isAlive()
                                && !entity.isSpectator()
                                && entity != source
                                && source.distanceToSqr(entity) <= rangeSq)
                .stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(source)))
                .orElse(null);
    }

    // ==================== 优先级排序 ====================

    /**
     * 从候选列表中选出最优目标。
     * <p>
     * 规则：敌对优先。唯一例外——友好生物在阈值内（range × 1/2）
     * 且敌对在阈值外时，选友好。
     */
    private LivingEntity selectBestTarget(List<LivingEntity> candidates) {
        List<LivingEntity> hostile = candidates.stream()
                .filter(e -> e instanceof Monster)
                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(source)))
                .toList();
        List<LivingEntity> friendly = candidates.stream()
                .filter(e -> !(e instanceof Monster))
                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(source)))
                .toList();

        if (hostile.isEmpty() && friendly.isEmpty()) return null;
        if (hostile.isEmpty()) return friendly.get(0);
        if (friendly.isEmpty()) return hostile.get(0);

        // 友好极近 + 敌对极远 → 选友好，否则选最近的敌对
        double threshold = range * PRIORITY_THRESHOLD;
        double closestHostileDist = hostile.get(0).distanceTo(source);
        double closestFriendlyDist = friendly.get(0).distanceTo(source);

        if (closestFriendlyDist <= threshold && closestHostileDist > range - threshold) {
            return friendly.get(0);
        }

        return hostile.get(0);
    }
}