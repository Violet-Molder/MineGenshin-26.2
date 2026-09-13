package com.linweiyun.genshin.mixin.mixins;

import com.linweiyun.genshin.content.entities.teyvat.TeyvatLiving;
import com.linweiyun.genshin.enums.ElementalsGIM;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MagmaCube.class)
public abstract class MagmaCubeResistanceMixin implements TeyvatLiving {

    private static final float PYRO_RESISTANCE = 0.80f;

    @Override
    public float getElementResistance(ElementalsGIM element) {
        if (element == ElementalsGIM.PYRO) return PYRO_RESISTANCE;
        return 0.10f;
    }

    @Override
    public float getPhysicalResistance() {
        return 0.10f;
    }
}