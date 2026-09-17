package com.linweiyun.genshin.core.system.combat.damage;

import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.system.combat.decay.DecayGroup;
import com.linweiyun.genshin.core.system.combat.decay.DecayGroups;
import com.linweiyun.genshin.enums.AttackType;
import com.linweiyun.genshin.enums.ElementalReactionType;

import javax.annotation.Nullable;
import java.util.List;

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
    private final AttackType attackType;                 // 攻击类型 —— 决定伤害分类和衰减标签
    private final GenshinElement element;                 // 伤害元素 —— 决定伤害的元素属性

    // ========== 基础伤害区 - 倍率 ==========
    private final float atkMultiplier;                   // 攻击力倍率（技能面板描述的那个）
    private final float hpMultiplier;                    // 生命值倍率
    private final float defMultiplier;                   // 防御力倍率
    private final float emMultiplier;                    // 元素精通倍率
    private final float skillMultiplierBonus;            // 技能倍率提升（行秋4命、深渊buff等，不是技能面板倍率）
    private final float flatDamageBonus;                 // 基础伤害附加（申鹤冰凌等）


    // ========== 元素附着数据 ==========
    private final float elementAmount;                   // 基础元素量 —— 本次攻击的原始元素量

    // ========== 衰减系统 ==========
    private final DecayGroup decayGroup;                // 衰减组别 —— 决定三个序列（元素量、伤害、削韧）和清除时间

    // ========== 攻击者角色信息 ==========
    private final PGCharacter attackerCharacter;       // 攻击者的PGCharacter —— 非玩家攻击者时为null

    // ========== 剧变反应标记 ==========
    private final ElementalReactionType transformativeReactionType;

    // ========== 月曜/星烁 附加乘区 ==========
    private final float lunarBaseBonus;                  // 月反应基础伤害倍率加成 (角色天赋提供, 如基于生命值)
    private final float lunarBaseFlat;                   // 月反应基础伤害附加

    // ========== 月感电多角色贡献者 ==========
    private List<PGCharacter> lunarContributors;         // 参与月感电反应的角色列表（雷暴云伤害用）

    // ========== 伤害类型 - 决定走哪条计算管线 ==========
    public enum DamageType {
        DIRECT,
        TRANSFORMATIVE,
        QUICKEN,
        LUNAR,
        STELLAR
    }

    private final DamageType damageType;

    // ========== 运行时计算结果 ==========
    private boolean crit;                               // 本次伤害是否暴击（计算时由战斗系统设置）

    // ========== 构造函数 ==========
    private ModDamageSpec(AttackType attackType, GenshinElement element,
                          float atkMultiplier, float hpMultiplier, float defMultiplier, float emMultiplier,
                          float skillMultiplierBonus, float flatDamageBonus,
                          float elementAmount, DecayGroup decayGroup,
                          PGCharacter attackerCharacter) {
        this(attackType, element,
                atkMultiplier, hpMultiplier, defMultiplier, emMultiplier,
                skillMultiplierBonus, flatDamageBonus,
                elementAmount, decayGroup,
                attackerCharacter,
                DamageType.DIRECT, null, 0f, 0f);
    }

    private ModDamageSpec(AttackType attackType, GenshinElement element,
                          float atkMultiplier, float hpMultiplier, float defMultiplier, float emMultiplier,
                          float skillMultiplierBonus, float flatDamageBonus,
                          float elementAmount, DecayGroup decayGroup,
                          PGCharacter attackerCharacter,
                          DamageType damageType,
                          ElementalReactionType reactionType) {
        this(attackType, element,
                atkMultiplier, hpMultiplier, defMultiplier, emMultiplier,
                skillMultiplierBonus, flatDamageBonus,
                elementAmount, decayGroup,
                attackerCharacter, damageType, reactionType, 0f, 0f);
    }

    private ModDamageSpec(AttackType attackType, GenshinElement element,
                          float atkMultiplier, float hpMultiplier, float defMultiplier, float emMultiplier,
                          float skillMultiplierBonus, float flatDamageBonus,
                          float elementAmount, DecayGroup decayGroup,
                          PGCharacter attackerCharacter,
                          DamageType damageType,
                          ElementalReactionType reactionType,
                          float lunarBaseBonus, float lunarBaseFlat) {
        this.attackType = attackType;
        this.element = element;
        this.atkMultiplier = atkMultiplier;
        this.hpMultiplier = hpMultiplier;
        this.defMultiplier = defMultiplier;
        this.emMultiplier = emMultiplier;
        this.skillMultiplierBonus = skillMultiplierBonus;
        this.flatDamageBonus = flatDamageBonus;
        this.elementAmount = elementAmount;
        this.decayGroup = decayGroup;
        this.attackerCharacter = attackerCharacter;
        this.damageType = damageType;
        this.transformativeReactionType = reactionType;
        this.lunarBaseBonus = lunarBaseBonus;
        this.lunarBaseFlat = lunarBaseFlat;
    }

    public static ModDamageSpec transformative(ElementalReactionType reactionType, GenshinElement element) {
        return new ModDamageSpec(
                AttackType.SPECIAL, element,
                0f, 0f, 0f, 0f,
                0f, 0f,
                0f, null,
                null,
                DamageType.TRANSFORMATIVE, reactionType, 0f, 0f);
    }

    public static ModDamageSpec lunar(ElementalReactionType reactionType) {
        return new ModDamageSpec(
                AttackType.LUNAR_CHARGED, ModElements.ELECTRO.get(),
                0f, 0f, 0f, 0f,
                0f, 0f,
                0f, null,
                null,
                DamageType.LUNAR, reactionType, 0f, 0f);
    }

    public static ModDamageSpec lunarDirect(float atkMultiplier, float elementAmount) {
        return new ModDamageSpec(
                AttackType.LUNAR_CHARGED, ModElements.ELECTRO.get(),
                atkMultiplier, 0f, 0f, 0f,
                0f, 0f,
                elementAmount, null,
                null,
                DamageType.LUNAR, ElementalReactionType.LUNAR_CHARGED, 0f, 0f);
    }

    public DamageType getDamageType() {
        return damageType;
    }

    public boolean isTransformative() {
        return damageType == DamageType.TRANSFORMATIVE;
    }

    public boolean isLunar() {
        return damageType == DamageType.LUNAR;
    }

    public ElementalReactionType getTransformativeReactionType() {
        return transformativeReactionType;
    }

    public float getLunarBaseBonus() { return lunarBaseBonus; }
    public float getLunarBaseFlat() { return lunarBaseFlat; }

    public List<PGCharacter> getLunarContributors() { return lunarContributors; }
    public void setLunarContributors(List<PGCharacter> contributors) { this.lunarContributors = contributors; }

    public ModDamageSpec withFlatDamageBonus(float newFlatBonus) {
        return new ModDamageSpec(
                this.attackType, this.element,
                this.atkMultiplier, this.hpMultiplier, this.defMultiplier, this.emMultiplier,
                this.skillMultiplierBonus, newFlatBonus,
                this.elementAmount, this.decayGroup,
                this.attackerCharacter, this.damageType, this.transformativeReactionType,
                this.lunarBaseBonus, this.lunarBaseFlat
        );
    }

    // ========== Getter ==========
    public AttackType getAttackType() { return attackType; }
    public GenshinElement getElement() { return element; }
    public float getAtkMultiplier() { return atkMultiplier; }
    public float getHpMultiplier() { return hpMultiplier; }
    public float getDefMultiplier() { return defMultiplier; }
    public float getEmMultiplier() { return emMultiplier; }
    public float getSkillMultiplierBonus() { return skillMultiplierBonus; }
    public float getFlatDamageBonus() { return flatDamageBonus; }
    public float getElementAmount() { return elementAmount; }
    public DecayGroup getDecayGroup() { return decayGroup; }
    public @Nullable PGCharacter getAttackerCharacter() { return attackerCharacter; }

    // ========== 便捷判断 ==========
    public boolean isElemental() { return element != ModElements.FYSIKOS.get(); }
    public boolean isPhysical() { return element == ModElements.FYSIKOS.get(); }
    public boolean hasDecayTag() { return attackType.hasDecayTag(); }
    public boolean hasAuraPotential() { return elementAmount > 0 && isElemental(); }

    public boolean isCrit() { return crit; }
    public void setCrit(boolean crit) { this.crit = crit; }

    public DecayGroup getEffectiveDecayGroup() {
        return decayGroup != null ? decayGroup : DecayGroups.DEFAULT_NORMAL_ATTACK;
    }
    public String getDecayTag() { return attackType.getDecayTag(); }


    // ========== 工厂方法 ==========

    /**
     * 创建一个新的 Builder
     * @param attackType 攻击类型（必填）
     * @param element 伤害元素（必填）
     * @return 新的 Builder 实例
     */
    public static Builder builder(AttackType attackType, GenshinElement element) {
        return new Builder(attackType, element);
    }

    /**
     * 快速创建一个物理伤害规格
     */
    public static ModDamageSpec physical(AttackType attackType, float multiplier) {
        return builder(attackType, ModElements.FYSIKOS.get())
                .multiplier(multiplier)
                .build();
    }

    /**
     * 快速创建一个带默认附着的元素伤害规格
     */
    public static ModDamageSpec elemental(AttackType attackType, GenshinElement element, float multiplier) {
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
     * DamageSpec normal = DamageSpec.builder(AttackType.NORMAL_ATTACK, ModElements.PYRO.get())
     *     .multiplier(1.5f)
     *     .elementAmount(1.0f)
     *     .build();
     *
     * // 申鹤战技（自定义衰减组别：0.1s清除，只有第1次附着）
     * DecayGroup shenheGroup = new DecayGroup(2,
     *     DecaySequence.of(1,0,0,0,0,0,0),
     *     DecaySequence.DEFAULT_DAMAGE,
     *     DecaySequence.DEFAULT_POISE);
     * DamageSpec skill = DamageSpec.builder(AttackType.ELEMENTAL_SKILL, ModElements.CYRO.get())
     *     .multiplier(2.5f)
     *     .elementAmount(1.0f)
     *     .decayGroup(shenheGroup)
     *     .build();
     *
     * // 0伤害但有附着的特殊攻击
     * DamageSpec auraOnly = DamageSpec.builder(AttackType.SPECIAL, ModElements.HYDRO.get())
     *     .multiplier(0.0f)
     *     .elementAmount(1.0f)
     *     .build();
     * </pre>
     */
    // ========== Builder ==========
    public static class Builder {
        private final AttackType attackType;
        private final GenshinElement element;

        private float atkMultiplier = 1.0f;
        private float hpMultiplier = 0.0f;
        private float defMultiplier = 0.0f;
        private float emMultiplier = 0.0f;
        private float skillMultiplierBonus = 0.0f;
        private float flatDamageBonus = 0.0f;
        private float elementAmount = 0.0f;
        private DecayGroup decayGroup = null;
        private PGCharacter attackerCharacter = null;

        private Builder(AttackType attackType, GenshinElement element) {
            this.attackType = attackType;
            this.element = element;
        }

        /** 设置攻击力倍率（旧的 multiplier 方法指向这里，保持兼容） */
        public Builder multiplier(float v) { return atkMultiplier(v); }
        public Builder atkMultiplier(float v) { this.atkMultiplier = v; return this; }
        public Builder hpMultiplier(float v) { this.hpMultiplier = v; return this; }
        public Builder defMultiplier(float v) { this.defMultiplier = v; return this; }
        public Builder emMultiplier(float v) { this.emMultiplier = v; return this; }

        /** 设置技能倍率提升（不是技能面板上的倍率，是额外乘的，如行秋4命、深渊buff） */
        public Builder skillMultiplierBonus(float v) { this.skillMultiplierBonus = v; return this; }

        public Builder flatBonus(float v) { return flatDamageBonus(v); }
        public Builder flatDamageBonus(float v) { this.flatDamageBonus = v; return this; }

        public Builder elementAmount(float v) { this.elementAmount = v; return this; }
        public Builder decayGroup(DecayGroup g) { this.decayGroup = g; return this; }
        public Builder attackerCharacter(PGCharacter c) { this.attackerCharacter = c; return this; }

        public ModDamageSpec build() {
            return new ModDamageSpec(
                    attackType, element,
                    atkMultiplier, hpMultiplier, defMultiplier, emMultiplier,
                    skillMultiplierBonus, flatDamageBonus,
                    elementAmount, decayGroup,
                    attackerCharacter
            );
        }
    }

    // ========== 调试输出 ==========

    @Override
    public String toString() {
        return "DamageSpec{" +
                "type=" + attackType +
                ", element=" + element +
                ", atkMult=" + atkMultiplier +
                ", flatBonus=" + flatDamageBonus +
                ", elementAmt=" + elementAmount +
                '}';
    }
}