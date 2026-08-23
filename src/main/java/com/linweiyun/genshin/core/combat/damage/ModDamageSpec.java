package com.linweiyun.genshin.core.combat.damage;

import com.linweiyun.genshin.enums.AttackType;
import com.linweiyun.genshin.enums.ElementalsGIM;

/**
 * 伤害规格 —— 定义一次攻击的完整参数规格
 *
 * 核心设计：攻击衰减系统
 * 每次攻击都携带衰减标签和（可选的）衰减组别：
 * - 衰减标签（来自AttackType）：决定哪些攻击共用同一套计时计数器
 * - 衰减组别（DecayGroup）：决定三个序列（元素量、伤害、削韧）和清除时间
 *
 * 附着机制：
 * - 每次攻击都施加附着，实际附着量 = 基础元素量 × 元素量序列系数
 * - 系数为0 = 无附着，系数为1 = 全额附着
 *
 * 衰减组别的指定方式：
 * - 方式1：不指定（decayGroup=null）→ 战斗系统根据攻击者+角色+攻击类型查找对应组别
 * - 方式2：直接指定 → 使用指定的组别（用于特殊攻击覆盖默认行为）
 *
 * 计时计数器位置：
 * - 存储在目标实体身上（不是攻击者身上）
 * - 按 (目标, 攻击者, 角色, 标签) 独立计算
 * - 第一次命中时创建，清除时间到期后销毁
 *
 * 数据流向：
 *   技能/效果 → 创建 DamageSpec → 嵌入 ModDamageSource
 *     → ModDamageSource 进入 MC 伤害管线
 *     → 战斗事件拦截 → 从 DamageSource 取出 DamageSpec → 读取全部数据
 *     → 战斗系统解析衰减组别（使用规格指定的或查找默认的）
 *     → 查询目标身上的计时计数器，获取当前命中次数
 *     → 从三个序列中读取对应系数
 *     → 实际元素量 = 基础元素量 × 元素量系数
 *     → 实际伤害 = 计算伤害 × 伤害系数
 *     → 实际削韧 = 基础削韧 × 削韧系数
 */
public class ModDamageSpec {

    // ========== 核心伤害数据 ==========

    // 攻击类型 —— 决定伤害分类和衰减标签
    private final AttackType attackType;

    // 伤害元素 —— 决定伤害的元素属性
    private final ElementalsGIM element;

    // 伤害倍率 —— 攻击力乘数（如普通攻击的 1.5 倍率）
    private final float damageMultiplier;

    // 固定伤害加成 —— 在倍率计算后额外附加的固定伤害值
    private final float flatDamageBonus;

    // ========== 元素附着数据 ==========

    // 基础元素量 —— 本次攻击的原始元素量
    private final float elementAmount;

    // ========== 衰减系统 ==========

    // 衰减组别 —— 可选的自定义衰减组别
    private final DecayGroup decayGroup;

    // ========== 构造函数 ==========

    /**
     * 完整构造函数 —— 包含所有字段
     * 通常不直接使用，而是通过 Builder 构建
     */
    private ModDamageSpec(AttackType attackType, ElementalsGIM element,
                          float damageMultiplier, float flatDamageBonus,
                          float elementAmount, DecayGroup decayGroup) {
        this.attackType = attackType;
        this.element = element;
        this.damageMultiplier = damageMultiplier;
        this.flatDamageBonus = flatDamageBonus;
        this.elementAmount = elementAmount;
        this.decayGroup = decayGroup;
    }
    public ModDamageSpec withFlatDamageBonus(float newFlatBonus) {
        return new ModDamageSpec(
                this.attackType,
                this.element,
                this.damageMultiplier,
                newFlatBonus,
                this.elementAmount,
                this.decayGroup
        );
    }

    // ========== Getter 方法 ==========

    /** 获取攻击类型 */
    public AttackType getAttackType() {
        return attackType;
    }

    /** 获取伤害元素 */
    public ElementalsGIM getElement() {
        return element;
    }

    /** 获取伤害倍率（攻击力乘数） */
    public float getDamageMultiplier() {
        return damageMultiplier;
    }

    /** 获取固定伤害加成 */
    public float getFlatDamageBonus() {
        return flatDamageBonus;
    }

    /** 获取基础元素量 */
    public float getElementAmount() {
        return elementAmount;
    }

    /** 获取自定义衰减组别（null=使用默认） */
    public DecayGroup getDecayGroup() {
        return decayGroup;
    }

    /**
     * 获取有效的衰减组别
     * 如果规格指定了自定义组别则返回自定义组别，否则返回默认组别
     * @return 有效的衰减组别
     */
    public DecayGroup getEffectiveDecayGroup() {
        return decayGroup != null ? decayGroup : DecayGroup.DEFAULT;
    }

    /**
     * 获取该攻击类型的衰减标签
     * 来自AttackType，决定计时计数器的共用关系
     * @return 衰减标签字符串，null表示不参与附着冷却
     */
    public String getDecayTag() {
        return attackType.getDecayTag();
    }

    // ========== 便捷判断方法 ==========

    /** 判断是否为元素伤害（非物理） */
    public boolean isElemental() {
        return element != ElementalsGIM.FYSIKOS;
    }

    /** 判断是否为物理伤害 */
    public boolean isPhysical() {
        return element == ElementalsGIM.FYSIKOS;
    }

    /**
     * 判断该攻击是否参与附着冷却系统
     * @return true=有衰减标签，参与冷却；false=无标签，不参与
     */
    public boolean hasDecayTag() {
        return attackType.hasDecayTag();
    }

    /**
     * 判断该攻击是否有元素附着能力（基础元素量>0 且 是元素伤害）
     * 注意：这只代表"有能力"附着，实际是否附着还取决于元素量序列系数
     * @return true=有附着能力，false=无附着能力
     */
    public boolean hasAuraPotential() {
        return elementAmount > 0 && isElemental();
    }

    // ========== 工厂方法 ==========

    /**
     * 创建一个新的 Builder
     * @param attackType 攻击类型（必填）
     * @param element 伤害元素（必填）
     * @return 新的 Builder 实例
     */
    public static Builder builder(AttackType attackType, ElementalsGIM element) {
        return new Builder(attackType, element);
    }

    /**
     * 快速创建一个物理伤害规格
     * @param attackType 攻击类型
     * @param multiplier 伤害倍率
     * @return 物理伤害规格实例
     */
    public static ModDamageSpec physical(AttackType attackType, float multiplier) {
        return builder(attackType, ElementalsGIM.FYSIKOS)
                .multiplier(multiplier)
                .build();
    }

    /**
     * 快速创建一个带默认附着的元素伤害规格
     * 使用默认元素量1.0，使用默认衰减组别
     * @param attackType 攻击类型
     * @param element 伤害元素
     * @param multiplier 伤害倍率
     * @return 元素伤害规格实例
     */
    public static ModDamageSpec elemental(AttackType attackType, ElementalsGIM element, float multiplier) {
        return builder(attackType, element)
                .multiplier(multiplier)
                .elementAmount(1.0f)
                .build();
    }

    // ========== Builder 内部类 ==========

    /**
     * 伤害规格构建器 —— 使用 Builder 模式灵活构建 DamageSpec
     *
     * 使用示例：
     * <pre>
     * // 普通元素攻击（使用默认衰减组别）
     * DamageSpec normal = DamageSpec.builder(AttackType.NORMAL_ATTACK, ElementalsGIM.PYRO)
     *     .multiplier(1.5f)
     *     .elementAmount(1.0f)
     *     .build();
     *
     * // 申鹤战技（自定义衰减组别：0.1s清除，只有第1次附着）
     * DecayGroup shenheGroup = new DecayGroup(2,
     *     DecaySequence.of(1,0,0,0,0,0,0),
     *     DecaySequence.DEFAULT_DAMAGE,
     *     DecaySequence.DEFAULT_POISE);
     * DamageSpec skill = DamageSpec.builder(AttackType.ELEMENTAL_SKILL, ElementalsGIM.CYRO)
     *     .multiplier(2.5f)
     *     .elementAmount(1.0f)
     *     .decayGroup(shenheGroup)
     *     .build();
     *
     * // 0伤害但有附着的特殊攻击
     * DamageSpec auraOnly = DamageSpec.builder(AttackType.SPECIAL, ElementalsGIM.HYDRO)
     *     .multiplier(0.0f)
     *     .elementAmount(1.0f)
     *     .build();
     * </pre>
     */
    public static class Builder {

        // 必填字段
        private final AttackType attackType;
        private final ElementalsGIM element;

        // 可选字段（带默认值）
        private float multiplier = 1.0f;
        private float flatBonus = 0.0f;
        private float elementAmount = 0.0f;
        private DecayGroup decayGroup = null;

        /**
         * Builder 构造函数
         * @param attackType 攻击类型（必填）
         * @param element 伤害元素（必填）
         */
        private Builder(AttackType attackType, ElementalsGIM element) {
            this.attackType = attackType;
            this.element = element;
        }

        /** 设置伤害倍率 */
        public Builder multiplier(float multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        /** 设置固定伤害加成 */
        public Builder flatBonus(float flatBonus) {
            this.flatBonus = flatBonus;
            return this;
        }

        /** 设置基础元素量 */
        public Builder elementAmount(float amount) {
            this.elementAmount = amount;
            return this;
        }

        /** 设置自定义衰减组别 */
        public Builder decayGroup(DecayGroup group) {
            this.decayGroup = group;
            return this;
        }

        /**
         * 构建伤害规格
         * @return 构建完成的 DamageSpec 实例
         */
        public ModDamageSpec build() {
            return new ModDamageSpec(
                    attackType,
                    element,
                    multiplier,
                    flatBonus,
                    elementAmount,
                    decayGroup
            );
        }
    }

    // ========== 调试输出 ==========

    @Override
    public String toString() {
        return "DamageSpec{" +
                "type=" + attackType +
                ", element=" + element +
                ", multiplier=" + damageMultiplier +
                ", flatBonus=" + flatDamageBonus +
                ", elementAmt=" + elementAmount +
                ", decayGroup=" + (decayGroup != null ? "custom" : "default") +
                '}';
    }
}
