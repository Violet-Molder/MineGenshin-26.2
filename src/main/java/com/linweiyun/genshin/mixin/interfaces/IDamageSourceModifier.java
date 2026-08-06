package com.linweiyun.genshin.mixin.interfaces;

import net.minecraft.world.damagesource.DamageSource;

/**
 * Mixin 接口 —— 注入到 NeoForge 的 DamageContainer 中
 * 允许在伤害管线中替换 DamageSource
 */
public interface IDamageSourceModifier {

    /**
     * 设置替换用的伤害源
     * @param newSource 替换用的伤害源（通常是内嵌了DamageSpec的ModDamageSource）
     */
    void setModifiedSource(DamageSource newSource);

    /**
     * 获取当前有效的伤害源
     * 如果已设置替换源则返回替换源，否则返回原始源
     * @return 当前有效的伤害源
     */
    DamageSource getEffectiveSource();
}