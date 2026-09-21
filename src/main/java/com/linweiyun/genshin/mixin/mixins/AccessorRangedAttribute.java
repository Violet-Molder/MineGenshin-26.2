package com.linweiyun.genshin.mixin.mixins;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RangedAttribute.class)
public interface AccessorRangedAttribute {

    @Accessor("minValue")
    @Mutable
    void minegenshin$setMinValue(double minValue);

    @Accessor("maxValue")
    @Mutable
    void minegenshin$setMaxValue(double maxValue);
}
