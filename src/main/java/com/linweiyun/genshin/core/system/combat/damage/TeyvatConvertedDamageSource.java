package com.linweiyun.genshin.core.system.combat.damage;

import net.minecraft.world.damagesource.DamageSource;

public class TeyvatConvertedDamageSource extends DamageSource {

    public TeyvatConvertedDamageSource(DamageSource original) {
        super(original.typeHolder(), original.getDirectEntity(), original.getEntity());
    }
}