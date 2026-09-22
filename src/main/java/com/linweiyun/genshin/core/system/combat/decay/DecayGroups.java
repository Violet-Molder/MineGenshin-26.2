package com.linweiyun.genshin.core.system.combat.decay;

import com.linweiyun.genshin.core.system.combat.damage.DecaySequence;

public class DecayGroups {
    /**
     * 默认普通攻击衰减组别
     * 清除时间：2.5秒（50 tick）
     * 元素量序列：[1,0,0,1,0,0,...] 长度24（每3次附着1次）
     * 伤害序列：不衰减（命中多少次都是 1.0）
     * 削韧序列：不衰减（命中多少次都是 1.0）
     */
    public static final DecayGroup DEFAULT_NORMAL_ATTACK = new DecayGroup(
            50,
            DecaySequence.DEFAULT_ELEMENT,
            DecaySequence.DEFAULT_DAMAGE,
            DecaySequence.DEFAULT_POISE
    );

    /**
     * 默认元素战技衰减组别
     * 清除时间：0.1秒（2 tick）
     * 元素量序列：全1 长度15（每次都可附着）
     * 伤害序列：不衰减（命中多少次都是 1.0）
     * 削韧序列：不衰减（命中多少次都是 1.0）
     */
    public static final DecayGroup DEFAULT_ELEMENTAL_SKILL = new DecayGroup(
            2,
            DecaySequence.createFilled(1.0f, 15),
            DecaySequence.DEFAULT_DAMAGE,
            DecaySequence.DEFAULT_POISE
    );

    /**
     * 默认元素爆发衰减组别
     * 清除时间：0.1秒（2 tick）
     * 元素量序列：全1 长度15（每次都可附着）
     * 伤害序列：不衰减（命中多少次都是 1.0）
     * 削韧序列：不衰减（命中多少次都是 1.0）
     */
    public static final DecayGroup DEFAULT_ELEMENTAL_BURST = new DecayGroup(
            2,
            DecaySequence.createFilled(1.0f, 15),
            DecaySequence.DEFAULT_DAMAGE,
            DecaySequence.DEFAULT_POISE
    );

    public static final DecayGroup SHENHE_SKILL = new DecayGroup(
            2,
            DecaySequence.createRepeating(new float[]{1,0,0,0,0,0,0}, 1),
            DecaySequence.DEFAULT_DAMAGE,
            DecaySequence.DEFAULT_POISE
    );
}
