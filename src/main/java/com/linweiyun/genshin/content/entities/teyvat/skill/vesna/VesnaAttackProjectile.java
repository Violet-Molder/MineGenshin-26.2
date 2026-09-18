package com.linweiyun.genshin.content.entities.teyvat.skill.vesna;

import com.linweiyun.genshin.content.entities.ModEntities;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.LockedTargetData;
import com.linweiyun.genshin.core.character.CharacterHelper;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.content.skill_node.TargetSeeker;
import com.linweiyun.genshin.core.character.sword.vesna.Vesna;
import com.linweiyun.genshin.core.character.sword.vesna.VesnaEnergy;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncPersistRPCBlockEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.UUID;

import static com.linweiyun.genshin.core.character.sword.vesna.Vesna.VESNA_ENERGY;

/**
 * 薇斯娜特殊状态下普通攻击产生的追踪实体。
 * <p>
 * 索敌逻辑：
 * <ol>
 *   <li>优先追踪玩家最近攻击的实体（5秒内，通过LockedTargetData记录）</li>
 *   <li>如果目标为null或距离超过10格，则切换为自主索敌（方圆索敌，半径10格）</li>
 * </ol>
 * <p>
 * 实体绑定召唤者角色（通过UUID），而不是玩家。
 */
public class VesnaAttackProjectile extends Entity implements IPersistedSerializable {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** 优先追踪目标的距离阈值（10格） */
    private static final double PRIORITY_TARGET_RANGE = 10.0;

    /** 自主索敌半径（10格） */
    private static final double AUTONOMOUS_SEARCH_RADIUS = 10.0;

    /** 飞行速度 */
    private static final double FLY_SPEED = 0.1;

    /** 实体最大存活时间（tick），10秒 = 200 tick */
    private static final int MAX_LIFE_TIME = 200;

    /** 召唤者角色（LDLib2自动同步和持久化） */
    @Persisted(key = "character")
    @DescSynced
    private Vesna character;

    /** 当前追踪的目标（LDLib2自动同步和持久化，一旦锁定不再改变） */
    @Persisted(key = "target")
    @DescSynced
    private LivingEntity target;

    /** 是否已经命中过目标 */
    private boolean hasHit = false;

    public VesnaAttackProjectile(EntityType<? extends VesnaAttackProjectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = false;
    }

    /**
     * 创建实体并绑定召唤者角色。
     *
     * @param level          世界
     * @param ownerCharacter 召唤者角色（不是玩家）
     */
    public static VesnaAttackProjectile create(Level level, Vesna ownerCharacter, Vec3 pos) {
         VesnaAttackProjectile projectile = ModEntities.VESNA_ATTACK_PROJECTILE.get().create(level, EntitySpawnReason.EVENT);
        if (projectile != null) {
            var ownerPlayer = ownerCharacter.getData().getOwnerPlayer();
            Vec3 spawnPos = pos.add(ownerPlayer.getLookAngle().scale(2.0));
            projectile.setPos(spawnPos.x, spawnPos.y + 1.0, spawnPos.z);
            projectile.character = ownerCharacter;
            projectile.setNoGravity(true);
            LOGGER.info("VesnaAttackProjectile create success at {}", spawnPos);
            return projectile;
        }
        LOGGER.info("VesnaAttackProjectile create failed");

        return null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

    }



    @Override
    public void tick() {
        super.tick();

        // 超时销毁
        if (this.tickCount >= MAX_LIFE_TIME) {
            LOGGER.info("VesnaAttackProjectile 超时销毁");
            this.discard();
            return;
        }
        // 已命中后销毁
        if (this.hasHit) {
            LOGGER.info("VesnaAttackProjectile 已命中目标");
            this.discard();
            return;
        }

        // 客户端处理移动
        if (this.level().isClientSide()) {
            if (character == null) {
                LOGGER.info("客户端VesnaAttackProjectile character为null");
                return;

            }
            // 如果target为null或目标已死亡，客户端也索敌（用于渲染）
            if (this.target == null || !this.target.isAlive()) {
                TargetSeeker seeker = new TargetSeeker(
                        this,
                        AUTONOMOUS_SEARCH_RADIUS,
                        TargetSeeker.TargetingType.RADIUS,
                        TargetSeeker.SourceType.TRACKING_COOP,
                        character
                );
                this.target = seeker.execute();
                LOGGER.info("VesnaAttackProjectile 客户端索敌完成，目标: {}", this.target);
            }

            if (this.target != null && this.target.isAlive()) {
                flyTowardsTarget();
                // 检测碰撞
                if (this.distanceTo(this.target) < 0.5) {
                    this.hasHit = true;
                }
            }

            this.move(MoverType.SELF, this.getDeltaMovement());
            return;
        }

        // 服务端只处理逻辑和验证
        if (character == null) {
            LOGGER.info("VesnaAttackProjectile character为null，销毁");
            this.discard();
            return;
        }

        // 如果target为null或目标已死亡，重新索敌
        if (this.target == null || !this.target.isAlive()) {
            TargetSeeker seeker = new TargetSeeker(
                    this,
                    AUTONOMOUS_SEARCH_RADIUS,
                    TargetSeeker.TargetingType.RADIUS,
                    TargetSeeker.SourceType.TRACKING_COOP,
                    character
            );
            this.target = seeker.execute();
            LOGGER.info("VesnaAttackProjectile 服务端索敌完成，目标: {}", this.target);
        }

        // 服务端验证碰撞
        if (this.target != null && this.target.isAlive() && this.distanceTo(this.target) < 0.5) {
            onHitTarget(this.target);
        }
    }


    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float v) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput valueInput) {

    }

    @Override
    protected void addAdditionalSaveData(ValueOutput valueOutput) {

    }

    /**
     * 获取优先目标：玩家最近攻击的实体（5秒内）。
     */
    private LivingEntity getPriorityTarget(net.minecraft.world.entity.player.Player player) {
        long currentTick = this.level().getGameTime();
        LockedTargetData lockData = player.getData(AttachmentRegistration.LOCKED_TARGET);

        if (!lockData.isValid(currentTick)) {
            return null;
        }

        Entity lockedEntity = this.level().getEntity(lockData.targetId());
        if (lockedEntity instanceof LivingEntity livingEntity && livingEntity.isAlive()) {
            return livingEntity;
        }

        return null;
    }


    /**
     * 飞向目标（客户端执行）。
     */
    private void flyTowardsTarget() {
        if (this.target == null) {
            return;
        }

        Vec3 targetPos = this.target.getBoundingBox().getCenter();
        Vec3 currentPos = this.position();
        Vec3 direction = targetPos.subtract(currentPos).normalize();

        Vec3 velocity = direction.scale(FLY_SPEED);
        this.setDeltaMovement(velocity);
    }

    /**
     * 客户端命中处理（只播放特效，不处理伤害）。
     */
    private void onHitTargetClient() {
        this.hasHit = true;
        // 客户端只播放特效，伤害由服务端处理
    }

    /**
     * 命中目标时调用（服务端执行）。
     */
    private void onHitTarget(LivingEntity target) {
        if (this.hasHit) {
            return;
        }

        this.hasHit = true;
        if (character == null) {
            this.discard();
            return;
        }
        spawnHitEffects();
        this.discard();
    }


    /**
     * 计算伤害（根据你的角色属性系统实现）。
     */
    private float calculateDamage(PGCharacter ownerCharacter) {
        // 根据角色攻击力、技能倍率等计算伤害
        // return ownerCharacter.getAttack() * skillMultiplier;
        return 10.0f; // 占位
    }

    /**
     * 生成命中特效。
     */
    private void spawnHitEffects() {
        if (this.level() instanceof ServerLevel serverLevel) {
            LOGGER.info("VesnaAttackProjectile hit target: {}", this.target);
            character.addEnergy(1);
        }
    }

}