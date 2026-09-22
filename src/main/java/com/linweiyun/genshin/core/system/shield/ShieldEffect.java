package com.linweiyun.genshin.core.system.shield;

/**
 * 护盾的<b>效用</b>分类 —— 伤害怎么分到盾和血上。
 *
 * <p>两种取值互斥，是护盾最基础的一维。
 */
public enum ShieldEffect {

    /**
     * 全抵挡：存续期间受到的伤害<b>全部</b>作用于护盾。
     *
     * <p>盾量扣完之前本体一点血都不掉；盾量不足以吃下这一次伤害时，
     * 多出来的部分才落到本体身上。
     *
     * <p>玩家角色提供的护盾一律是这一种。
     */
    FULL,

    /**
     * 衰减型：伤害按 {@link ShieldState#partialRatio()} 的比例打到盾上，其余穿透到本体。
     *
     * <p>默认 90% 打盾、10% 穿透。这类盾<b>仅怪物持有</b>。
     */
    PARTIAL
}
