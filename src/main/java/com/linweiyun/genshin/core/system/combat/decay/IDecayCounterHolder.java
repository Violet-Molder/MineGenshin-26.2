package com.linweiyun.genshin.core.system.combat.decay;

/**
 * 计时计数器持有者接口
 *
 * 实现方式有两种：
 * 1. 本mod实体（TeyvatLivingEntity）：直接在类中实现
 * 2. 非本mod实体：通过LivingEntityDecayMixin注入实现
 */
public interface IDecayCounterHolder {

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
