package com.linweiyun.genshin.content.entities.area;

import com.linweiyun.genshin.core.attribute.ModAttributes;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.combat.attack.HurtEntityHelper;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSpec;
import com.linweiyun.genshin.enums.AttackType;
import com.linweiyun.genshin.enums.ElementalsGIM;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class TalismanSpiritArea extends AreaEntity {
    // ========== 领域常量配置 ==========

    // 伤害触发间隔（tick）—— 每1秒（20tick）对范围敌人造成一次伤害
    private static final int DAMAGE_INTERVAL_TICKS = 20;

    // 冰元素伤害倍率 —— 相对于角色攻击力的倍率
    private static final float DAMAGE_MULTIPLIER = 0.7f;

    // 领域总持续时间（tick）—— 12秒 = 240tick
    private static final int FIELD_DURATION = 30 * 20;

    // 水平半径（格）
    private static final float HORIZONTAL_RADIUS = 6.0f;

    // 垂直半径（格）
    private static final float VERTICAL_RADIUS = 2.0f;

    // 领域唯一标识UUID
    private static final int FIELD_UUID = 612001;

    // ========== 实例字段 ==========

    // 伤害计时器 —— 累计tick数，每 DAMAGE_INTERVAL_TICKS 次触发一次伤害
    private int damageTickCounter = 0;

    // 粒子位置缓存列表 —— 客户端渲染用
    private final List<Vec3> particlePositions = new ArrayList<>();

    // 粒子位置是否已计算 —— 避免每tick重复计算
    private boolean particlesCalculated = false;

    /**
     * 构造函数 —— 初始化镇灵之鼎领域
     */
    public TalismanSpiritArea(EntityType<?> type, Level level) {
        super(type, level);
        // 设置领域形状为圆柱体
        setShapeType(AreaShapeType.CYLINDER);
        // 设置水平半径
        setHorizontalRadius(HORIZONTAL_RADIUS);
        // 设置垂直半径
        setVerticalRadius(VERTICAL_RADIUS);
        // 设置领域UUID
        setFieldUUID(FIELD_UUID);
        // 设置持续时间
        setDuration(FIELD_DURATION);
    }

    // ========== Tick 逻辑 ==========

    @Override
    protected void serverTick() {
        super.serverTick();  // 父类处理持续时间倒计时

        // 伤害计时器递增
        damageTickCounter++;

        // 每隔 DAMAGE_INTERVAL_TICKS 次触发一次范围伤害
        if (damageTickCounter >= DAMAGE_INTERVAL_TICKS) {
            damageTickCounter = 0;  // 重置计时器
            applyFieldDamage();     // 对范围内敌人造成伤害
        }
    }

    @Override
    protected void clientTick() {
        // 首次tick时计算粒子位置（仅一次）
        if (!particlesCalculated) {
            calculateParticlePositions();
            particlesCalculated = true;
        }
        // 每tick生成缓存的粒子（仅临时视觉效果）
        spawnCachedParticles();
    }

    // ========== 领域伤害逻辑 ==========

    /**
     * 对领域范围内的所有敌人造成伤害
     * 使用圆柱体碰撞检测，而非简单AABB
     */
    private void applyFieldDamage() {
        // 构建圆柱体碰撞区域（用AABB作为粗筛，再用精确判定）
        double centerX = this.getX();
        double centerY = this.getY();
        double centerZ = this.getZ();
        float radius = getHorizontalRadius();
        float halfHeight = getVerticalRadius();

        // 粗筛：AABB范围
        AABB cylinderBounds = new AABB(
                centerX - radius, centerY - halfHeight, centerZ - radius,
                centerX + radius, centerY + halfHeight, centerZ + radius);

        // 获取范围内所有生物
        List<LivingEntity> entitiesInRange = this.level().getEntitiesOfClass(
                LivingEntity.class, cylinderBounds, this::isEntityInCylinder);

        // 获取拥有者信息
        Player owner = getOwner();
        PGCharacter ownerCharacter = getOwnerCharacter();

        if (owner == null || ownerCharacter == null) return;

        // 对每个范围内的实体造成伤害
        for (LivingEntity entity : entitiesInRange) {
            // 跳过拥有者自己
            if (entity.equals(owner)) continue;
            // 跳过拥有者的队友（这里简化处理，只跳过拥有者）
            // 如果需要跳过队友，可以在此扩展

            // 对目标施加伤害
            dealDamageToEntity(entity, owner, ownerCharacter);
        }
    }

    /**
     * 判定实体是否在圆柱体内（精确判定）
     * @param entity 待检测的实体
     * @return 是否在圆柱体内
     */
    private boolean isEntityInCylinder(LivingEntity entity) {
        double centerX = this.getX();
        double centerY = this.getY();
        double centerZ = this.getZ();
        float radius = getHorizontalRadius();
        float halfHeight = getVerticalRadius();

        Vec3 entityPos = entity.position();
        // 计算水平距离的平方（避免开方运算）
        double horizontalDistSq =
                (entityPos.x - centerX) * (entityPos.x - centerX)
                        + (entityPos.z - centerZ) * (entityPos.z - centerZ);
        // 计算垂直距离
        double verticalDist = Math.abs(entityPos.y - centerY);

        // 水平距离 <= 半径 且 垂直距离 <= 垂直半径
        return horizontalDistSq <= radius * radius && verticalDist <= halfHeight;
    }

    /**
     * 对单个目标造成冰元素伤害
     * 使用新的 ModDamageSpec 体系
     *
     * @param target 受伤实体
     * @param owner 伤害源头玩家
     * @param ownerCharacter 触发领域的角色数据
     */
    private void dealDamageToEntity(LivingEntity target, Player owner, PGCharacter ownerCharacter) {
        // 构建伤害规格：元素爆发类型 + 冰元素 + 0.7倍率
        ModDamageSpec spec = ModDamageSpec.builder(AttackType.ELEMENTAL_BURST, ElementalsGIM.CYRO)
                .multiplier(DAMAGE_MULTIPLIER)   // 伤害倍率
                .elementAmount(1.0f)              // 元素附着量
                .build();

        // 从规格创建伤害源
        ModDamageSource damageSource = ModDamageSource.from(spec, owner);

        // 计算实际伤害（简化版：使用角色ATK × 倍率）
        double characterATK = ownerCharacter.getData().getAttributeTotalValue(ModAttributes.ATK.value());
        float finalDamage = (float) (characterATK * DAMAGE_MULTIPLIER);

        // 对目标造成伤害
        HurtEntityHelper.hurtEntityForPlayer(damageSource, ownerCharacter, target);
    }

    // ========== 临时粒子效果（标注领域范围） ==========
    // TODO: 实际特效添加后，此方法及相关字段应移除

    /**
     * 计算粒子位置（仅执行一次，缓存结果）
     * 在圆柱体边界上均匀分布粒子点
     */
    private void calculateParticlePositions() {
        double centerX = this.getX();
        double centerY = this.getY();
        double centerZ = this.getZ();
        float radius = getHorizontalRadius();
        float halfHeight = getVerticalRadius();

        // 顶面和底面圆环粒子数
        int circleParticles = 32;
        for (int i = 0; i < circleParticles; i++) {
            double angle = 2 * Math.PI * i / circleParticles;
            double x = centerX + radius * Math.cos(angle);
            double z = centerZ + radius * Math.sin(angle);
            // 顶面
            particlePositions.add(new Vec3(x, centerY + halfHeight, z));
            // 底面
            particlePositions.add(new Vec3(x, centerY - halfHeight, z));
        }

        // 侧面竖线粒子
        int lineCount = 24;    // 竖线数量
        int steps = 7;         // 每条竖线的步数
        for (int i = 0; i < lineCount; i++) {
            double angle = 2 * Math.PI * i / lineCount;
            double x = centerX + radius * Math.cos(angle);
            double z = centerZ + radius * Math.sin(angle);
            for (int step = 0; step <= steps; step++) {
                double y = centerY - halfHeight + (2 * halfHeight * step / steps);
                particlePositions.add(new Vec3(x, y, z));
            }
        }
    }

    /**
     * 生成缓存的粒子 —— 每tick在所有预计算位置生成粒子
     * 仅用于临时标注领域范围
     */
    private void spawnCachedParticles() {
        Level level = this.level();
        for (Vec3 pos : particlePositions) {
            level.addParticle(ParticleTypes.SNOWFLAKE, pos.x, pos.y, pos.z,
                    0, 0, 0);  // 零速度，瞬生瞬灭
        }
    }
}
