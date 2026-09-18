package com.linweiyun.genshin.content.skill_node;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.LockedTargetData;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.List;

/**
 * 索敌节点——从视野或方圆范围内选取最优目标并锁定。
 * <p>
 * 索敌源类型：
 * <ul>
 *   <li>{@link SourceType#CHARACTER} 角色索敌：使用锁定附件，支持视野/方圆索敌</li>
 *   <li>{@link SourceType#ALLY_SUMMON_FREE} 己方召唤物-自由：完全自主索敌，使用锁定附件</li>
 *   <li>{@link SourceType#ALLY_SUMMON_COOP} 己方召唤物-协同：优先攻击角色正在攻击的目标，使用锁定附件</li>
 *   <li>{@link SourceType#TRACKING_FREE} 追踪实体-自由：自主索敌，不使用锁定附件，只攻击一次</li>
 *   <li>{@link SourceType#TRACKING_COOP} 追踪实体-协同：优先攻击角色正在攻击的目标，不使用锁定附件，只攻击一次</li>
 * </ul>
 * <p>
 * 索敌模式：
 * <ul>
 *   <li>{@link TargetingType#LINE_OF_SIGHT LINE_OF_SIGHT}（视野索敌）：以实体视线方向延伸 range 距离，横截面 5×5 的隧道</li>
 *   <li>{@link TargetingType#RADIUS RADIUS}（方圆索敌）：以实体为中心、range 为半径的球形范围</li>
 * </ul>
 */
public class TargetSeeker {

    public enum SourceType {
        /** 角色索敌：使用锁定附件 */
        CHARACTER,
        /** 己方召唤物-自由：完全自主索敌，使用锁定附件 */
        ALLY_SUMMON_FREE,
        /** 己方召唤物-协同：优先攻击角色正在攻击的目标，使用锁定附件 */
        ALLY_SUMMON_COOP,
        /** 追踪实体-自由：自主索敌，不使用锁定附件，只攻击一次 */
        TRACKING_FREE,
        /** 追踪实体-协同：优先攻击角色正在攻击的目标，不使用锁定附件，只攻击一次 */
        TRACKING_COOP
    }

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
    private final TargetingType targetingType;

    /** 索敌源类型 */
    private final SourceType sourceType;

    /** 协同模式关联的角色（用于TRACKING_COOP和ALLY_SUMMON_COOP） */
    private final PGCharacter associatedCharacter;

    /**
     * @param source        发起索敌的实体
     * @param range         索敌范围（视野模式为延伸距离，方圆模式为半径）
     * @param targetingType 索敌模式
     * @param sourceType    索敌源类型
     * @param associatedChar 协同模式关联的角色（非协同模式传null）
     */
    public TargetSeeker(Entity source, double range, TargetingType targetingType, SourceType sourceType, PGCharacter associatedChar) {
        this.source = source;
        this.range = range;
        this.targetingType = targetingType;
        this.sourceType = sourceType;
        this.associatedCharacter = associatedChar;
    }

    /**
     * 简化构造函数（默认CHARACTER类型，向后兼容）
     */
    public TargetSeeker(Entity source, double range, TargetingType type) {
        this(source, range, type, SourceType.CHARACTER, null);
    }

    // ==================== 主流程 ====================

    /**
     * 执行索敌，返回最优目标。
     */
    public LivingEntity execute() {
        if (source == null || range <= 0) return null;

        Level level = source.level();
        long tick = level.getGameTime();

        // 根据源类型分发逻辑
        return switch (sourceType) {
            case CHARACTER -> executeCharacter(level, tick);
            case ALLY_SUMMON_FREE -> executeAllySummonFree(level, tick);
            case ALLY_SUMMON_COOP -> executeAllySummonCoop(level, tick);
            case TRACKING_FREE -> executeTrackingFree(level);
            case TRACKING_COOP -> executeTrackingCoop(level);
        };
    }

    /**
     * 角色索敌：使用锁定附件，支持视野/方圆索敌
     */
    private LivingEntity executeCharacter(Level level, long tick) {
        // 1. 检查已有锁
        LockedTargetData lockData = source.getData(AttachmentRegistration.LOCKED_TARGET);
        if (lockData.isValid(tick)) {
            Entity locked = level.getEntity(lockData.targetId());
            if (locked instanceof LivingEntity livingLocked
                    && locked.isAlive()
                    && source.distanceTo(livingLocked) <= range
                    && hasVisionOf(livingLocked)) {

                if (livingLocked instanceof Monster) {
                    refreshLock(tick, livingLocked);
                    LOGGER.info("TargetSeeker [CHARACTER] 锁定敌对生物，返回并刷新");
                    return livingLocked;
                }

                LivingEntity closerHostile = findClosestHostileInRange();
                if (closerHostile != null && source.distanceToSqr(closerHostile) < source.distanceToSqr(livingLocked)) {
                    LOGGER.info("TargetSeeker [CHARACTER] 更近敌对生物抢锁，目标: {}", closerHostile);
                    source.setData(AttachmentRegistration.LOCKED_TARGET,
                            new LockedTargetData(closerHostile.getUUID(), tick + LOCK_DURATION_TICKS));
                    return closerHostile;
                }
                LOGGER.info("TargetSeeker [CHARACTER] 无更近敌对，保持友好锁并刷新");
                refreshLock(tick, livingLocked);
                return livingLocked;
            }
            source.setData(AttachmentRegistration.LOCKED_TARGET, LockedTargetData.EMPTY);
        }

        // 2. 无锁 → 按类型收集候选
        List<LivingEntity> candidates = switch (targetingType) {
            case LINE_OF_SIGHT -> collectLineOfSight();
            case RADIUS -> collectRadius();
        };
        LOGGER.info("TargetSeeker [CHARACTER] 收集到 {} 个候选目标", candidates.size());

        if (candidates.isEmpty()) return null;

        // 3. 优先级排序
        LivingEntity target = selectBestTarget(candidates);

        // 4. 锁定新目标
        if (target != null) {
            source.setData(AttachmentRegistration.LOCKED_TARGET,
                    new LockedTargetData(target.getUUID(), tick + LOCK_DURATION_TICKS));
        }
        return target;
    }

    /**
     * 己方召唤物-自由：完全自主索敌，使用锁定附件
     */
    private LivingEntity executeAllySummonFree(Level level, long tick) {
        LockedTargetData lockData = source.getData(AttachmentRegistration.LOCKED_TARGET);
        if (lockData.isValid(tick)) {
            Entity locked = level.getEntity(lockData.targetId());
            if (locked instanceof LivingEntity livingLocked
                    && locked.isAlive()
                    && source.distanceTo(livingLocked) <= range) {
                refreshLock(tick, livingLocked);
                LOGGER.info("TargetSeeker [ALLY_SUMMON_FREE] 返回锁定目标: {}", livingLocked);
                return livingLocked;
            }
            source.setData(AttachmentRegistration.LOCKED_TARGET, LockedTargetData.EMPTY);
        }

        List<LivingEntity> candidates = collectRadius();
        LOGGER.info("TargetSeeker [ALLY_SUMMON_FREE] 收集到 {} 个候选目标", candidates.size());

        if (candidates.isEmpty()) return null;

        LivingEntity target = selectBestTarget(candidates);
        if (target != null) {
            source.setData(AttachmentRegistration.LOCKED_TARGET,
                    new LockedTargetData(target.getUUID(), tick + LOCK_DURATION_TICKS));
        }
        return target;
    }

    /**
     * 己方召唤物-协同：优先攻击角色正在攻击的目标，使用锁定附件
     */
    private LivingEntity executeAllySummonCoop(Level level, long tick) {
        // 优先获取角色当前攻击的目标
        LivingEntity charTarget = getCharacterLockedTarget(level, tick);
        if (charTarget != null && charTarget.isAlive() && source.distanceTo(charTarget) <= range) {
            source.setData(AttachmentRegistration.LOCKED_TARGET,
                    new LockedTargetData(charTarget.getUUID(), tick + LOCK_DURATION_TICKS));
            LOGGER.info("TargetSeeker [ALLY_SUMMON_COOP] 协同角色目标: {}", charTarget);
            return charTarget;
        }

        // 角色无目标时，自主索敌
        return executeAllySummonFree(level, tick);
    }

    /**
     * 追踪实体-自由：自主索敌，不使用锁定附件，只攻击一次
     */
    private LivingEntity executeTrackingFree(Level level) {
        List<LivingEntity> candidates = collectRadius();
//        LOGGER.info("TargetSeeker [TRACKING_FREE] 收集到 {} 个候选目标", candidates.size());

        if (candidates.isEmpty()) return null;

        LivingEntity target = selectBestTarget(candidates);
//        LOGGER.info("TargetSeeker [TRACKING_FREE] 返回目标: {}", target);
        return target;
    }

    /**
     * 追踪实体-协同：优先攻击角色正在攻击的目标，不使用锁定附件，只攻击一次
     */
    private LivingEntity executeTrackingCoop(Level level) {
        long tick = level.getGameTime();

        // 优先获取角色当前攻击的目标
        LivingEntity charTarget = getCharacterLockedTarget(level, tick);
        if (charTarget != null && charTarget.isAlive()) {
            LOGGER.info("TargetSeeker [TRACKING_COOP] 协同角色目标: {}", charTarget);
            return charTarget;
        }

        // 角色无目标时，自主索敌
        return executeTrackingFree(level);
    }

    /**
     * 获取角色当前锁定的目标
     */
    private LivingEntity getCharacterLockedTarget(Level level, long tick) {
        if (associatedCharacter == null) return null;

        Player ownerPlayer = associatedCharacter.getData().getOwnerPlayer();
        if (ownerPlayer == null) return null;

        LockedTargetData lockData = ownerPlayer.getData(AttachmentRegistration.LOCKED_TARGET);
        if (!lockData.isValid(tick)) return null;

        Entity locked = level.getEntity(lockData.targetId());
        if (locked instanceof LivingEntity livingLocked && livingLocked.isAlive()) {
            return livingLocked;
        }

        return null;
    }

    // ==================== 锁定 / 刷新 ====================

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

    private void refreshLock(long tick, LivingEntity target) {
        source.setData(AttachmentRegistration.LOCKED_TARGET,
                new LockedTargetData(target.getUUID(), tick + LOCK_DURATION_TICKS));
    }

    public static void clearLock(Entity source) {
        source.setData(AttachmentRegistration.LOCKED_TARGET, LockedTargetData.EMPTY);
    }

    // ==================== 候选收集 ====================

    private List<LivingEntity> collectLineOfSight() {
        Vec3 eyePos = source.getEyePosition();
        Vec3 lookDir = source.getLookAngle().normalize();
        Vec3 endPos = eyePos.add(lookDir.scale(range));

        AABB searchBox = new AABB(eyePos, endPos).inflate(TUNNEL_HALF_SIZE);

        return source.level().getEntitiesOfClass(LivingEntity.class, searchBox, entity ->
                entity.isAlive()
                        && !entity.isSpectator()
                        && entity != source
                        && isInForwardTunnel(entity)
        );
    }

    private boolean isInForwardTunnel(Entity target) {
        Vec3 eyePos = source.getEyePosition();
        Vec3 lookDir = source.getLookAngle().normalize();
        Vec3 targetCenter = target.getBoundingBox().getCenter();
        Vec3 toTarget = targetCenter.subtract(eyePos);

        double forwardDist = toTarget.dot(lookDir);
        if (forwardDist < 0 || forwardDist > range) return false;

        Vec3 projected = eyePos.add(lookDir.scale(forwardDist));
        Vec3 perpendicular = targetCenter.subtract(projected);

        return perpendicular.lengthSqr() <= TUNNEL_HALF_SIZE * TUNNEL_HALF_SIZE;
    }

    private List<LivingEntity> collectRadius() {
        double rangeSq = range * range + RANGE_SQ_EPSILON;
        AABB searchBox = source.getBoundingBox().inflate(range);

        Player ownerPlayer = (associatedCharacter != null) ? associatedCharacter.getData().getOwnerPlayer() : null;

        return source.level().getEntitiesOfClass(LivingEntity.class, searchBox, entity ->
                entity.isAlive()
                        && !entity.isSpectator()
                        && entity != source
                        && entity != ownerPlayer
                        && source.distanceToSqr(entity) <= rangeSq
        );
    }

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

        double threshold = range * PRIORITY_THRESHOLD;
        double closestHostileDist = hostile.get(0).distanceTo(source);
        double closestFriendlyDist = friendly.get(0).distanceTo(source);

        if (closestFriendlyDist <= threshold && closestHostileDist > range - threshold) {
            return friendly.get(0);
        }

        return hostile.get(0);
    }
}