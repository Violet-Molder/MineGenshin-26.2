package com.linweiyun.genshin.content.entities.teyvat.monster;

import com.linweiyun.genshin.content.entities.teyvat.ElementalCreature;
import com.linweiyun.genshin.content.entities.teyvat.TeyvatHostile;
import com.linweiyun.genshin.core.system.about.ElementalAttachable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public abstract class TeyvatMonster extends Monster implements TeyvatHostile, ElementalAttachable {
    protected TeyvatMonster(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    /**
     * 元素生物的统一免疫入口。
     *
     * <p>放在基类里，任何实现 {@link ElementalCreature} 的怪物都自动获得
     * 「同元素伤害免疫」，不用每个实体各写一遍。
     */
    //TEMP
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this instanceof ElementalCreature creature && creature.isImmuneToSource(source)) {
            return false;
        }
        return super.hurtServer(level, source, damage);
    }
}
