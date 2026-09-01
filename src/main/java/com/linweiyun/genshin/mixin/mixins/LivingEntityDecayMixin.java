package com.linweiyun.genshin.mixin.mixins;

import com.linweiyun.genshin.core.system.combat.decay.DecayCounterManager;
import com.linweiyun.genshin.core.system.combat.decay.DecayCounterWorker;
import com.linweiyun.genshin.core.system.combat.decay.IDecayCounterHolder;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * LivingEntity Mixin - 为所有LivingEntity注入计时计数器支持
 *
 * 注入目标：net.minecraft.world.entity.LivingEntity
 * 实现接口：IDecayCounterHolder
 *
 * 工作原理：
 * 1. 向LivingEntity注入decayCounterManager字段
 * 2. 实现IDecayCounterHolder接口方法
 * 3. 首次调用getDecayCounterManager()时创建实例
 * 4. 实例创建后自动注册到DecayCounterWorker
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDecayMixin implements IDecayCounterHolder {

    // 注入字段 - 延迟初始化
    @Unique
    private DecayCounterManager genshin$decayCounterManager;

    @Override
    public DecayCounterManager getDecayCounterManager() {
        if (genshin$decayCounterManager == null) {
            genshin$decayCounterManager = new DecayCounterManager((LivingEntity)(Object) this);
            // 注册到Worker线程
            DecayCounterWorker.getInstance()
                    .registerManager(genshin$decayCounterManager);
        }
        return genshin$decayCounterManager;
    }

    @Override
    public boolean hasDecayCounterManager() {
        return genshin$decayCounterManager != null;
    }
}