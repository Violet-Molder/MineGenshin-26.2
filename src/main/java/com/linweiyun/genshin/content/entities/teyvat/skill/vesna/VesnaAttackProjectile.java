package com.linweiyun.genshin.content.entities.teyvat.skill.vesna;

import com.linweiyun.genshin.content.entities.ModEntities;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.LockedTargetData;
import com.linweiyun.genshin.core.character.CharacterHelper;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.content.skill_node.TargetSeeker;
import com.linweiyun.genshin.core.character.sword.vesna.Vesna;
import com.linweiyun.genshin.core.character.sword.vesna.VesnaEnergy;
import com.linweiyun.genshin.core.sync.ISyncManagedEntity;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import com.lowdragmc.lowdraglib2.syncdata.storage.IManagedStorage;
import com.mojang.logging.LogUtils;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
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
public class VesnaAttackProjectile extends Entity implements ISyncManagedEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** 优先追踪目标的距离阈值（10格） */
    private static final double PRIORITY_TARGET_RANGE = 10.0;

    /** 自主索敌半径（10格） */
    private static final double AUTONOMOUS_SEARCH_RADIUS = 10.0;

    /** 飞行速度 */
    private static final double FLY_SPEED = 0.8;

    /** 跟随玩家时的环绕距离 */
    private static final double ORBIT_DISTANCE = 1.5;

    /** 跟随玩家时距离目标位置多近就停止移动 */
    private static final double FOLLOW_STOP_DISTANCE = 0.3;

    /** 实体最大存活时间（tick），10秒 = 200 tick */
    private static final int MAX_LIFE_TIME = 200;

    /** LDLib2 managed field storage - powers @DescSynced and @Persisted */
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    /** 召唤者角色（LDLib2自动同步和持久化） */
    @Persisted(key = "character")
    @DescSynced
    private Vesna character;

    /** 当前追踪的目标（LDLib2自动同步和持久化，一旦锁定不再改变） */
    @Persisted(key = "target")
    @DescSynced
    private LivingEntity target;

    @Persisted(key = "test")
    @DescSynced
    private int test = 100;
    /** 是否已经命中过目标 */
    private boolean hasHit = false;

    /** 初始同步标记：首次服务端 tick 时执行全量同步 */
    private boolean initialSynced = false;

    // ========== ISyncManagedEntity / IManaged implementation ==========

    @Override
    public Entity getSelf() {
        return this;
    }

    @Override
    public IManagedStorage getSyncStorage() {
        return syncStorage;
    }

    @Override
    public void notifyPersistence() {
        // Entity persistence is handled by Minecraft's save system
    }

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
            // 在玩家后方/侧方生成（避开前方120°扇形），距离1.5格
            double baseYaw = ownerPlayer.getYRot() * Math.PI / 180.0;
            double angle = baseYaw + Math.PI / 3.0 + level.getRandom().nextDouble() * (Math.PI * 4.0 / 3.0);
            Vec3 spawnPos = pos.add(
                    Math.cos(angle) * 1.5,
                    0.5,
                    Math.sin(angle) * 1.5
            );
            projectile.setPos(spawnPos.x, spawnPos.y + 1.0, spawnPos.z);
            projectile.character = ownerCharacter;
            projectile.setNoGravity(true);
            projectile.test = 15222;
            return projectile;
        }

        return null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

    }



    @Override
    public void tick() {
        super.tick();
        LOGGER.info("VesnaAttackProjectile tick: {}", character);

        // 超时销毁
        if (this.tickCount >= MAX_LIFE_TIME) {
            this.discard();
            return;
        }
        // 已命中后销毁
        if (this.hasHit) {
            this.discard();
            return;
        }

        // ========== 服务端：索敌 + 移动 + 碰撞 ==========
        if (!this.level().isClientSide()) {
            if (character == null) {
                this.discard();
                return;
            }

            // 首次 tick 执行全量同步
            if (!initialSynced) {
                initialSynced = true;
                sync(true);
            }

            // 寻找敌人目标
            if (this.target == null || !this.target.isAlive() || !(this.target instanceof Monster)) {
                TargetSeeker seeker = new TargetSeeker(
                        this,
                        AUTONOMOUS_SEARCH_RADIUS,
                        TargetSeeker.TargetingType.RADIUS,
                        TargetSeeker.SourceType.TRACKING_COOP,
                        character,
                        TargetSeeker.TargetFilter.HOSTILE_ONLY
                );
                LivingEntity result = seeker.execute();
                if (result != null) {
                    this.target = result;
                } else {
                    // 没找到敌人 → 跟随玩家
                    this.target = getOwnerPlayer();
                }
            }

            if (this.target != null && this.target.isAlive()) {
                flyTowardsTarget();
                this.move(MoverType.SELF, this.getDeltaMovement());
                // 仅对锁定的索敌目标触发碰撞销毁
                boolean isEnemyTarget = this.target instanceof Monster;
                if (isEnemyTarget && this.getBoundingBox().intersects(this.target.getBoundingBox())) {
                    onHitTarget(this.target);
                }
            }

            passivelySync();
            return;
        }

        // ========== 客户端：仅移动渲染 ==========
        if (character == null) {
            return;
        }
        if (this.target != null && this.target.isAlive()) {
            flyTowardsTarget();
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
    }


    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float v) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput valueInput) {
        loadManagedPersistentData(valueInput);
    }

    /**
     * 获取角色对应的玩家。
     */
    private Player getOwnerPlayer() {
        if (character == null) return null;
        return character.getData().getOwnerPlayer();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput valueOutput) {
        saveManagedPersistentData(valueOutput, false);
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
     * 飞向目标。若目标为玩家则绕圈，足够近时停止移动避免抖动。
     */
    private void flyTowardsTarget() {
        if (this.target == null) {
            return;
        }

        Vec3 targetPos;
        if (this.target instanceof Player player) {
            targetPos = getOrbitPosition(player);
        } else {
            targetPos = this.target.getBoundingBox().getCenter();
        }

        Vec3 currentPos = this.position();
        double dist = currentPos.distanceTo(targetPos);

        // 跟随玩家且已足够接近 → 停住，避免来回抖动
        if (this.target instanceof Player && dist < FOLLOW_STOP_DISTANCE) {
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }

        // 追玩家时速度不超过剩余距离，防止冲过头回弹
        double speed = this.target instanceof Player ? Math.min(FLY_SPEED, dist) : FLY_SPEED;
        Vec3 direction = targetPos.subtract(currentPos).normalize();
        Vec3 velocity = direction.scale(speed);
        this.setDeltaMovement(velocity);
    }

    /**
     * 计算绕玩家分散环绕的目标位置。每个实体根据自己的 UUID 分散到不同角度。
     */
    private Vec3 getOrbitPosition(Player player) {
        // 使用实体 UUID 的低位决定角度，保证同一实体始终在同一个位置
        int hash = this.getUUID().hashCode();
        double angle = ((double)(hash & 0xFFFF) / 0xFFFF) * Math.PI * 2.0;

        Vec3 playerPos = player.position();
        return new Vec3(
                playerPos.x + Math.cos(angle) * ORBIT_DISTANCE,
                playerPos.y + player.getEyeHeight() * 0.6,
                playerPos.z + Math.sin(angle) * ORBIT_DISTANCE
        );
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