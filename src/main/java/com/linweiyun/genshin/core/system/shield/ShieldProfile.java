package com.linweiyun.genshin.core.system.shield;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * 一份护盾的<b>配置</b>（不变的那部分）—— 分类维度 + 元素表 + 免疫开关。
 *
 * <p>和 {@link ShieldState}（会被扣、会被存进实体的那部分）分开：
 * 配置是「模板」，可以注册一次给很多实体用；状态是「这一只身上现在还剩多少」。
 *
 * <p>生物给自己套盾时可以改属性 —— 改的是这里：拿 {@link ShieldProfiles} 里的模板
 * {@link Builder#from(ShieldProfile) 派生}一份新的、改掉想改的项，再注册成一个新 key。
 *
 * <h2>三张元素表</h2>
 * <ul>
 *   <li>{@code consumePerUnit} —— 每 1 单位附着消耗多少盾量（元素盾/混合盾用）。
 *       例如冰盾遇火：1 单位火消耗 2 点冰。</li>
 *   <li>{@code absorbMultiplier} —— 每个元素的伤害吸收倍率（伤害盾/混合盾用）。
 *       玩家元素盾：对应元素 250%、岩 150%、其余 100%。</li>
 *   <li>{@code blockAttachment} —— 该元素的附着是不是被盾整个吞掉（不附着、不参与反应）。
 *       冰盾遇水就是这一条。</li>
 * </ul>
 */
public final class ShieldProfile {

    //TEMP
    private final String key;
    //TEMP
    private final ShieldEffect effect;
    //TEMP
    private final ShieldShape shape;
    //TEMP
    private final ShieldBreakType breakType;
    //TEMP
    private final float partialRatio;
    //TEMP
    private final float[] consumePerUnit;
    //TEMP
    private final float[] absorbMultiplier;
    //TEMP
    private final boolean[] blockAttachment;
    //TEMP
    private final boolean immuneToFreeze;
    //TEMP
    private final boolean immuneToChill;
    //TEMP
    private final boolean permanent;

    /**
     * 这个盾是哪个元素的（决定盾条颜色等表现）。白盾/伤害盾这类没有元素的填 null。
     */
    //TEMP
    @Nullable
    private final com.linweiyun.genshin.core.element.GenshinElement element;

    //TEMP
    private ShieldProfile(Builder builder) {
        this.key = builder.key;
        this.effect = builder.effect;
        this.shape = builder.shape;
        this.breakType = builder.breakType;
        this.partialRatio = builder.partialRatio;
        this.consumePerUnit = builder.consumePerUnit.clone();
        this.absorbMultiplier = builder.absorbMultiplier.clone();
        this.blockAttachment = builder.blockAttachment.clone();
        this.immuneToFreeze = builder.immuneToFreeze;
        this.immuneToChill = builder.immuneToChill;
        this.permanent = builder.permanent;
        this.element = builder.element;
    }

    /** 盾的元素；没有就是 null（白盾）。 */
    //TEMP
    @Nullable
    public com.linweiyun.genshin.core.element.GenshinElement element() {
        return this.element;
    }

    // ---------- 读取 ----------

    //TEMP
    public String key() {
        return this.key;
    }

    //TEMP
    public ShieldEffect effect() {
        return this.effect;
    }

    //TEMP
    public ShieldShape shape() {
        return this.shape;
    }

    //TEMP
    public ShieldBreakType breakType() {
        return this.breakType;
    }

    /** 衰减型盾里打到盾上的比例（0~1）。其他类型忽略。 */
    //TEMP
    public float partialRatio() {
        return this.partialRatio;
    }

    /** 元素攻击每 1 单位附着消耗的盾量。 */
    //TEMP
    public float consumePerUnit(ShieldElement element) {
        return this.consumePerUnit[element.slot()];
    }

    /** 某个元素的伤害吸收倍率。 */
    //TEMP
    public float absorbMultiplier(ShieldElement element) {
        return this.absorbMultiplier[element.slot()];
    }

    /** 该元素的附着是否被盾整个吞掉。 */
    //TEMP
    public boolean blocksAttachment(ShieldElement element) {
        return this.blockAttachment[element.slot()];
    }

    /** 持盾期间免疫冻结。 */
    //TEMP
    public boolean immuneToFreeze() {
        return this.immuneToFreeze;
    }

    /** 持盾期间免疫寒元素（冰元素带来的减速）。 */
    //TEMP
    public boolean immuneToChill() {
        return this.immuneToChill;
    }

    /** 是不是永续盾（没有剩余时间）。 */
    //TEMP
    public boolean permanent() {
        return this.permanent;
    }

    /** 盾量完全由元素/削韧扣掉时也要走元素反应吗（默认走）。 */
    //TEMP
    public String describe() {
        return "ShieldProfile{" + this.key + ", " + this.effect + ", " + this.shape + ", " + this.breakType
                + ", consume=" + Arrays.toString(this.consumePerUnit)
                + ", absorb=" + Arrays.toString(this.absorbMultiplier) + '}';
    }

    // ==================== Builder ====================

    //TEMP
    public static Builder builder(String key) {
        return new Builder(key);
    }

    /** 派生一份配置副本 —— 生物改写自己那面盾时用这个。 */
    //TEMP
    public static Builder from(ShieldProfile source) {
        Builder builder = new Builder(source.key);
        builder.effect = source.effect;
        builder.shape = source.shape;
        builder.breakType = source.breakType;
        builder.partialRatio = source.partialRatio;
        builder.consumePerUnit = source.consumePerUnit.clone();
        builder.absorbMultiplier = source.absorbMultiplier.clone();
        builder.blockAttachment = source.blockAttachment.clone();
        builder.immuneToFreeze = source.immuneToFreeze;
        builder.immuneToChill = source.immuneToChill;
        builder.permanent = source.permanent;
        builder.element = source.element;
        return builder;
    }

    //TEMP
    public static final class Builder {

        //TEMP
        private final String key;
        //TEMP
        private ShieldEffect effect = ShieldEffect.FULL;
        //TEMP
        private ShieldShape shape = ShieldShape.AURA;
        //TEMP
        private ShieldBreakType breakType = ShieldBreakType.DAMAGE;
        //TEMP
        private float partialRatio = 0.9f;
        //TEMP
        private float[] consumePerUnit = new float[ShieldElement.COUNT];
        //TEMP
        private float[] absorbMultiplier = filled(1.0f);
        //TEMP
        private boolean[] blockAttachment = new boolean[ShieldElement.COUNT];
        //TEMP
        private boolean immuneToFreeze;
        //TEMP
        private boolean immuneToChill;
        //TEMP
        private boolean permanent = true;
        //TEMP
        private com.linweiyun.genshin.core.element.GenshinElement element;

        //TEMP
        public Builder element(com.linweiyun.genshin.core.element.GenshinElement element) {
            this.element = element;
            return this;
        }

        //TEMP
        private Builder(String key) {
            this.key = key;
        }

        //TEMP
        private static float[] filled(float value) {
            float[] array = new float[ShieldElement.COUNT];
            Arrays.fill(array, value);
            return array;
        }

        //TEMP
        public Builder effect(ShieldEffect effect) {
            this.effect = effect;
            return this;
        }

        //TEMP
        public Builder shape(ShieldShape shape) {
            this.shape = shape;
            return this;
        }

        //TEMP
        public Builder breakType(ShieldBreakType breakType) {
            this.breakType = breakType;
            return this;
        }

        //TEMP
        public Builder partialRatio(float ratio) {
            this.partialRatio = ratio;
            return this;
        }

        //TEMP
        public Builder consume(ShieldElement element, float perUnit) {
            this.consumePerUnit[element.slot()] = perUnit;
            return this;
        }

        //TEMP
        public Builder absorb(ShieldElement element, float multiplier) {
            this.absorbMultiplier[element.slot()] = multiplier;
            return this;
        }

        //TEMP
        public Builder absorbAll(float multiplier) {
            Arrays.fill(this.absorbMultiplier, multiplier);
            return this;
        }

        //TEMP
        public Builder blockAttachment(ShieldElement element) {
            this.blockAttachment[element.slot()] = true;
            return this;
        }

        //TEMP
        public Builder immuneToFreeze() {
            this.immuneToFreeze = true;
            return this;
        }

        //TEMP
        public Builder immuneToChill() {
            this.immuneToChill = true;
            return this;
        }

        //TEMP
        public Builder timed() {
            this.permanent = false;
            return this;
        }

        //TEMP
        public ShieldProfile build() {
            return new ShieldProfile(this);
        }
    }

    /** 取配置；没注册过就返回 null。 */
    //TEMP
    @Nullable
    public static ShieldProfile of(@Nullable String key) {
        return ShieldProfiles.get(key);
    }
}
