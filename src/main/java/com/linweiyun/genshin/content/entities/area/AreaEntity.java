package com.linweiyun.genshin.content.entities.area;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.character.PGCharacterData;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 领域实体基类 —— 所有领域（Field/Barrrier）的抽象基类
 *
 * 设计参考原版 AreaEffectCloud，但独立实现：
 * - 移除了药水（PotionContents）相关的所有代码
 * - 移除了粒子特效（粒子由子类自行管理）
 * - 移除了同一目标重复触发的CD（victims map / reapplicationDelay）
 * - 移除了增加持续时间（durationOnUse）和扩大范围（radiusOnUse）的代码
 * - 移除了等待时间（waitTime）
 *
 * 数据持久化：使用 LDLib2 的 @Persisted 注解 + IPersistedSerializable
 * 数据同步：半径（radius）通过 SynchedEntityData 实时同步给客户端
 */
public abstract class AreaEntity extends Entity implements IPersistedSerializable {

    // ========== SynchedEntityData 同步字段 ==========

    // 半径数据同步器 —— 用于实时同步半径到客户端（影响碰撞箱和显示）
    private static final EntityDataAccessor<Float> DATA_RADIUS =
            SynchedEntityData.defineId(AreaEntity.class, EntityDataSerializers.FLOAT);

    // ========== LDLib2 持久化字段 ==========

    // 触发领域的玩家UUID —— 记录是哪个玩家释放了这个领域
    @Persisted(key = "owner_uuid")
    protected String ownerUUID = "";

    // 触发领域的角色UUID —— 记录是玩家的哪个角色（PGCharacterData）触发了领域
    @Persisted(key = "character_uuid")
    protected int characterUUID = 0;

    // 领域剩余持续时间（tick）—— -1 表示无限持续
    @Persisted(key = "duration")
    protected int duration = -1;

    // 领域形状类型 —— 球体或圆柱体
    @Persisted(key = "shape_type")
    protected AreaShapeType shapeType = AreaShapeType.SPHERE;

    // 水平半径 —— 领域的水平方向半径
    @Persisted(key = "horizontal_radius")
    protected float horizontalRadius = 3.0f;

    // 垂直半径 —— 领域的垂直方向半径（仅圆柱体使用）
    @Persisted(key = "vertical_radius")
    protected float verticalRadius = 3.0f;

    // 领域唯一ID —— 用于标识特定领域类型（如612001为镇灵之鼎）
    @Persisted(key = "field_uuid")
    protected int fieldUUID = 0;

    // ========== 构造函数 ==========

    /**
     * 领域实体构造函数
     * @param type 实体类型
     * @param level 所在关卡
     */
    public AreaEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;  // 禁用物理碰撞（领域是虚拟区域）
    }

    // ========== 同步数据定义 ==========

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RADIUS, 3.0f);  // 默认半径3格
    }

    // ========== Tick 逻辑 ==========

    @Override
    public void tick() {
        super.tick();
        // 服务端执行持续时间倒计时
        if (!this.level().isClientSide()) {
            this.serverTick();
        } else {
            this.clientTick();
        }
    }

    /**
     * 服务端Tick —— 处理持续时间、领域效果等
     */
    protected void serverTick() {
        // 持续时间倒计时
        if (this.duration != -1 && this.tickCount >= this.duration) {
            this.discard();  // 到期后移除实体
        }
    }

    /**
     * 客户端Tick —— 处理粒子等视觉效果（由子类覆盖）
     */
    protected void clientTick() {
        // 基类默认无客户端逻辑，子类可覆盖
    }

    // ========== 领域属性访问 ==========

    /** 获取领域形状类型 */
    public AreaShapeType getShapeType() {
        return shapeType;
    }

    /** 设置领域形状类型 */
    public void setShapeType(AreaShapeType shapeType) {
        this.shapeType = shapeType;
    }

    /** 获取水平半径 */
    public float getHorizontalRadius() {
        return horizontalRadius;
    }

    /** 设置水平半径（同时更新SynchedEntityData） */
    public void setHorizontalRadius(float radius) {
        this.horizontalRadius = radius;
        if (!this.level().isClientSide()) {
            this.getEntityData().set(DATA_RADIUS, Mth.clamp(radius, 0.5f, 32.0f));
        }
    }

    /** 获取垂直半径 */
    public float getVerticalRadius() {
        return verticalRadius;
    }

    /** 设置垂直半径 */
    public void setVerticalRadius(float radius) {
        this.verticalRadius = radius;
    }

    /** 获取领域UUID */
    public int getFieldUUID() {
        return fieldUUID;
    }

    /** 设置领域UUID */
    public void setFieldUUID(int uuid) {
        this.fieldUUID = uuid;
    }

    /** 获取领域持续时间（tick），-1表示无限 */
    public int getDuration() {
        return duration;
    }

    /** 设置领域持续时间（tick），-1表示无限 */
    public void setDuration(int duration) {
        this.duration = duration;
    }

    /** 获取当前同步的半径值（用于碰撞箱计算） */
    public float getRadius() {
        return this.getEntityData().get(DATA_RADIUS);
    }

    // ========== 拥有者相关 ==========

    /**
     * 获取触发领域的玩家（拥有者）
     * @return 玩家实体，可能为null（玩家离线或不存在）
     */
    @Nullable
    public Player getOwner() {
        if (ownerUUID == null || ownerUUID.isEmpty()) return null;
        try {
            UUID uuid = UUID.fromString(ownerUUID);
            return this.level().getPlayerByUUID(uuid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 获取触发领域的角色数据（PGCharacterData）
     * 从拥有者玩家的附件系统中查找对应角色
     * @return 角色数据，可能为null
     */
    @Nullable
    public PGCharacter getOwnerCharacter() {
        Player owner = getOwner();
        if (owner == null) return null;
        PlayerCharactersAttachment attachment = owner.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        return attachment.getCharacterByUUID(characterUUID);
    }

    /**
     * 设置领域拥有者
     * @param owner 触发领域的玩家
     * @param characterUUID 触发领域的角色UUID（PGCharacterData的UUID）
     */
    public void setOwner(Player owner, int characterUUID) {
        if (owner != null) {
            this.ownerUUID = owner.getStringUUID();  // 存储玩家UUID
        }
        this.characterUUID = characterUUID;  // 存储角色UUID
    }

    /** 获取拥有者玩家UUID字符串 */
    public String getOwnerUUIDString() {
        return ownerUUID;
    }

    /** 获取触发角色的UUID */
    public int getCharacterUUID() {
        return characterUUID;
    }

    // ========== 碰撞与维度 ==========

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        // 球体：直径为水平半径的2倍，高度也是直径
        // 圆柱体：直径为水平半径的2倍，高度为垂直半径的2倍
        if (shapeType == AreaShapeType.CYLINDER) {
            return EntityDimensions.scalable(horizontalRadius * 2.0f, verticalRadius * 2.0f);
        } else {
            float diameter = horizontalRadius * 2.0f;
            return EntityDimensions.scalable(diameter, diameter);
        }
    }

    @Override
    public void refreshDimensions() {
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        super.refreshDimensions();
        this.setPos(x, y, z);  // 刷新维度后保持位置
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;  // 领域不被活塞推动
    }

    @Override
    public final boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float damage) {
        return false;  // 领域实体不可被伤害
    }

    // ========== 数据同步回调 ==========

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (DATA_RADIUS.equals(accessor)) {
            this.refreshDimensions();  // 半径变化时刷新碰撞箱
        }
        super.onSyncedDataUpdated(accessor);
    }

    // ========== LDLib2 序列化 ==========
    // IPersistedSerializable 的序列化/反序列化由 LDLib2 自动处理
    // readAdditionalSaveData 和 addAdditionalSaveData 可以保留为空，
    // 因为 @Persisted 注解的字段会自动被 LDLib2 持久化

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        // LDLib2 自动处理 @Persisted 字段的反序列化
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        // LDLib2 自动处理 @Persisted 字段的序列化
    }
}