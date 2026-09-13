package com.linweiyun.genshin.content.entities.teyvat.monster.slime;

import com.linweiyun.genshin.content.entities.teyvat.monster.TeyvatMonster;
import com.linweiyun.genshin.core.element.ModElements;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class SlimeCyro extends TeyvatSlime{
    public SlimeCyro(EntityType<? extends TeyvatMonster> type, Level level) {
        super(type, level);
        this.setElement(ModElements.CYRO.get());
    }
}