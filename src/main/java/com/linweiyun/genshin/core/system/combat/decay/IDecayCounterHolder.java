package com.linweiyun.genshin.core.system.combat.decay;

import com.linweiyun.genshin.content.entities.teyvat.TeyvatLiving;

/**
 * 计时计数器持有者接口
 *
 * 实现方式：
 * 1. Mod实体：通过继承 TeyvatHostile / TeyvatFriendly 获得
 * 2. 原版实体：通过 LivingEntityTeyvatMixin 注入 NonTeyvatEntity 获得
 */
public interface IDecayCounterHolder extends TeyvatLiving {

    /**
     * 获取该实体上的计时计数器管理器
     * 首次调用时自动初始化
     */
    DecayCounterManager getDecayCounterManager();

    /**
     * 是否已初始化计数器管理器
     */
    boolean hasDecayCounterManager();
}