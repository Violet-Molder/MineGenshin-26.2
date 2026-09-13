package com.linweiyun.genshin.mixin.interfaces;

import com.linweiyun.genshin.content.entities.teyvat.TeyvatLiving;
import com.linweiyun.genshin.enums.ElementalsGIM;

public interface IMonsterLevel extends TeyvatLiving {

    @Override
    int getMonsterLevel();

    @Override
    void setMonsterLevel(int level);

    @Override
    int getDefense();

    @Override
    float getElementResistance(ElementalsGIM element);

    @Override
    float getPhysicalResistance();
}