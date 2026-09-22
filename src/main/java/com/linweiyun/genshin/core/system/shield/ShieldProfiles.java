package com.linweiyun.genshin.core.system.shield;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 护盾模板注册表 —— 每种盾在这里写一份配置，实体套盾时按 key 取。
 *
 * <h2>为什么用「key + 静态模板」而不是把配置塞进实体</h2>
 * 配置（分类维度 + 三张元素表）是<b>不会变</b>的，实体身上只需要存
 * 「还剩多少、什么时候到期」（{@link ShieldState}）。这样同步和存档都只有几个数字，
 * 表不用过网络。
 *
 * <h2>生物要改自己那面盾怎么办</h2>
 * {@link ShieldProfile#from(ShieldProfile)} 派生一份、改掉想改的项，再
 * {@link #register(ShieldProfile)} 成新 key，套盾时用新 key 即可。
 * 例如大型冰史莱姆在深渊里被加强，就注册一份
 * {@code minegenshin:cryo_element_abyss}。
 *
 * <h2>本文件里哪些是真的、哪些是占位</h2>
 * 只有 {@link #CRYO_ELEMENT} 是完整实现；其余几个是<b>占位</b>：
 * 分类维度齐全、数值给的是「能跑通」的默认值，具体机制待做。
 */
public final class ShieldProfiles {

    //TEMP
    private static final Map<String, ShieldProfile> REGISTRY = new ConcurrentHashMap<>();

    // ==================== 占位 / 已实现的 key ====================

    /**
     * ★ 已实现：<b>冰元素全抵挡纯元素盾</b>。
     *
     * <table border="1">
     *   <caption>元素消耗表（每 1 单位附着消耗多少盾量）</caption>
     *   <tr><th>元素</th><th>消耗</th><th>说明</th></tr>
     *   <tr><td>冰</td><td>0</td><td>免疫：不附着、不消耗、无效果</td></tr>
     *   <tr><td>水</td><td>0</td><td>水直接被吞掉：不附着、不冻结、不消耗</td></tr>
     *   <tr><td>火</td><td>2.0</td><td>1 单位火消耗 2 点冰</td></tr>
     *   <tr><td>雷</td><td>0.1</td><td>反应照常；超导伤害那 0.05 暂未实装（超导未接线）</td></tr>
     *   <tr><td>草</td><td>0</td><td>无反应，不消耗</td></tr>
     *   <tr><td>风 / 岩</td><td>0.1</td><td>反应照常</td></tr>
     *   <tr><td>物理</td><td>0</td><td>只吃削韧</td></tr>
     * </table>
     */
    //TEMP
    public static final String CRYO_ELEMENT = "minegenshin:cryo_element";

    /** 占位：伤害盾（打多少伤害扣多少盾量，元素附着不影响）。 */
    //TEMP
    public static final String DAMAGE = "minegenshin:damage_shield";

    /** 占位：混合盾（伤害 + 元素 + 削韧一起扣）。 */
    //TEMP
    public static final String MIXED = "minegenshin:mixed_shield";

    /** 占位：白盾（全靠削韧击破）。 */
    //TEMP
    public static final String POISE = "minegenshin:poise_shield";

    /** 占位：衰减型盾（默认 90% 打到盾上）。 */
    //TEMP
    public static final String PARTIAL = "minegenshin:partial_shield";

    /** 占位：盾牌型盾（只挡正面一个扇形，盾量显示在手持物品上）。 */
    //TEMP
    public static final String HELD = "minegenshin:held_shield";

    /** 占位：玩家元素盾（对应元素 250% 吸收、岩 150%、其余 100%）。 */
    //TEMP
    public static final String PLAYER_ELEMENTAL = "minegenshin:player_elemental_shield";

    static {
        register(cryoElementShield());
        register(placeholderDamageShield());
        register(placeholderMixedShield());
        register(placeholderPoiseShield());
        register(placeholderPartialShield());
        register(placeholderHeldShield());
        register(placeholderPlayerElementalShield());
    }

    //TEMP
    private ShieldProfiles() {
    }

    // ==================== 注册 / 查询 ====================

    //TEMP
    public static ShieldProfile register(ShieldProfile profile) {
        REGISTRY.put(profile.key(), profile);
        return profile;
    }

    //TEMP
    @Nullable
    public static ShieldProfile get(@Nullable String key) {
        return key == null ? null : REGISTRY.get(key);
    }

    // ==================== 具体模板 ====================

    /**
     * ★ 真实现：冰元素 · 全抵挡 · 纯元素盾 · 罩型 · 永续。
     *
     * <p>伤害完全打不进本体（{@link ShieldEffect#FULL}），但伤害<b>不消耗盾量</b>
     * （{@link ShieldBreakType#ELEMENT}）—— 破盾只能靠元素附着和削韧。
     */
    //TEMP
    public static ShieldProfile cryoElementShield() {
        return ShieldProfile.builder(CRYO_ELEMENT)
                .effect(ShieldEffect.FULL)
                .shape(ShieldShape.AURA)
                .breakType(ShieldBreakType.ELEMENT)
                // 盾的元素：决定盾条颜色
                .element(com.linweiyun.genshin.core.element.ModElements.CYRO.get())
                // 免疫：冰不附着、水被吞掉
                .blockAttachment(ShieldElement.CYRO)
                .blockAttachment(ShieldElement.HYDRO)
                // 元素消耗表
                .consume(ShieldElement.PYRO, 2.0f)
                .consume(ShieldElement.ELECTRO, 0.1f)
                .consume(ShieldElement.ANEMO, 0.1f)
                .consume(ShieldElement.GEO, 0.1f)
                // 免疫冻结与寒元素（减速）
                .immuneToFreeze()
                .immuneToChill()
                .build();
    }

    /**
     * 占位：伤害盾。
     *
     * <p>机制：打多少伤害扣多少盾量，附着不影响。
     * <b>待补</b>：不同元素对盾的伤害吸收差异（现在全 100%）。
     */
    //TEMP
    private static ShieldProfile placeholderDamageShield() {
        return ShieldProfile.builder(DAMAGE)
                .effect(ShieldEffect.FULL)
                .shape(ShieldShape.AURA)
                .breakType(ShieldBreakType.DAMAGE)
                .build();
    }

    /**
     * 占位：混合盾。
     *
     * <p>机制：伤害、元素附着、削韧三者一起扣盾量。
     */
    //TEMP
    private static ShieldProfile placeholderMixedShield() {
        return ShieldProfile.builder(MIXED)
                .effect(ShieldEffect.FULL)
                .shape(ShieldShape.AURA)
                .breakType(ShieldBreakType.MIXED)
                .build();
    }

    /**
     * 占位：白盾。
     *
     * <p>机制：伤害和元素都不扣，只有削韧扣。
     * <b>待补</b>：独立的韧性条与破韧硬直（现在只是把削韧量扣到盾上）。
     */
    //TEMP
    private static ShieldProfile placeholderPoiseShield() {
        return ShieldProfile.builder(POISE)
                .effect(ShieldEffect.FULL)
                .shape(ShieldShape.AURA)
                .breakType(ShieldBreakType.POISE)
                .build();
    }

    /**
     * 占位：衰减型盾（仅怪物持有）。
     *
     * <p>机制：伤害的 {@code partialRatio}（默认 90%）打到盾上，其余穿透。
     */
    //TEMP
    private static ShieldProfile placeholderPartialShield() {
        return ShieldProfile.builder(PARTIAL)
                .effect(ShieldEffect.PARTIAL)
                .partialRatio(0.9f)
                .shape(ShieldShape.AURA)
                .breakType(ShieldBreakType.DAMAGE)
                .build();
    }

    /**
     * 占位：盾牌型盾（木盾丘丘人那种）。
     *
     * <p>机制：只抵挡正面扇形内的攻击。
     * <b>待补</b>：手持物品模型上的盾量显示（且不随动画位移）。
     */
    //TEMP
    private static ShieldProfile placeholderHeldShield() {
        return ShieldProfile.builder(HELD)
                .effect(ShieldEffect.FULL)
                .shape(ShieldShape.HELD)
                .breakType(ShieldBreakType.MIXED)
                .build();
    }

    /**
     * 占位：玩家元素盾。
     *
     * <p>机制：对应元素吸收 250%、岩 150%、物理与其余 100%
     * （「100 量的火盾能挡 250 火伤和 100 其他伤害」）。
     * <b>待补</b>：接入角色技能提供的护盾。
     */
    //TEMP
    private static ShieldProfile placeholderPlayerElementalShield() {
        return ShieldProfile.builder(PLAYER_ELEMENTAL)
                .effect(ShieldEffect.FULL)
                .shape(ShieldShape.AURA)
                .breakType(ShieldBreakType.DAMAGE)
                .absorbAll(1.0f)
                .absorb(ShieldElement.GEO, 1.5f)
                .absorb(ShieldElement.PYRO, 2.5f)
                .build();
    }
}
