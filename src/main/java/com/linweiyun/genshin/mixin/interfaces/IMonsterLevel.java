package com.linweiyun.genshin.mixin.interfaces;

import com.linweiyun.genshin.content.entities.teyvat.TeyvatLiving;
import com.linweiyun.genshin.core.element.GenshinElement;

public interface IMonsterLevel extends TeyvatLiving {

    @Override
    int getMonsterLevel();

    @Override
    void setMonsterLevel(int level);

    @Override
    int getDefense();

    @Override
    float getElementResistance(GenshinElement element);

    @Override
    float getPhysicalResistance();
}