package com.linweiyun.genshin.content.entities.teyvat.monster;

import com.linweiyun.genshin.content.entities.teyvat.TeyvatHostile;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public abstract class TeyvatMonster extends Monster implements TeyvatHostile {
    protected TeyvatMonster(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }
}