package com.linweiyun.genshin.core.system.combat.decay;

/**
 * 单次攻击的衰减系数结果
 *
 * 三个系数分别用于调整：
 * - elementCoefficient: 元素附着量系数（0=不附着，1=全额附着）
 * - damageCoefficient: 伤害输出系数（0=无伤害，1=全额伤害）
 * - poiseCoefficient: 削韧量系数（0=不削韧，1=全额削韧）
 */
public class DecayResult {

    public static final DecayResult NONE = new DecayResult(1.0f, 1.0f, 1.0f);

    private final float elementCoefficient;
    private final float damageCoefficient;
    private final float poiseCoefficient;

    public DecayResult(float elementCoefficient, float damageCoefficient, float poiseCoefficient) {
        this.elementCoefficient = elementCoefficient;
        this.damageCoefficient = damageCoefficient;
        this.poiseCoefficient = poiseCoefficient;
    }

    public float getElementCoefficient() { return elementCoefficient; }
    public float getDamageCoefficient() { return damageCoefficient; }
    public float getPoiseCoefficient() { return poiseCoefficient; }

    public boolean hasElementAttachment() { return elementCoefficient > 0; }
    public boolean hasDamage() { return damageCoefficient > 0; }

    @Override
    public String toString() {
        return "DecayResult{elem=" + elementCoefficient + ", dmg=" + damageCoefficient + ", poise=" + poiseCoefficient + '}';
    }
}