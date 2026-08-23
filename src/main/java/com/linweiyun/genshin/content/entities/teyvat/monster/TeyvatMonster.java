package com.linweiyun.genshin.content.entities.teyvat.monster;

import com.linweiyun.genshin.content.entities.teyvat.TeyvatLivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public abstract class TeyvatMonster extends Monster implements TeyvatLivingEntity {
    protected TeyvatMonster(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }
}