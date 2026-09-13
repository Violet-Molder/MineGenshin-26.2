package com.linweiyun.genshin.mixin.mixins;

import com.linweiyun.genshin.content.entities.teyvat.NonTeyvatEntity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class LivingEntityTeyvatMixin implements NonTeyvatEntity {
}