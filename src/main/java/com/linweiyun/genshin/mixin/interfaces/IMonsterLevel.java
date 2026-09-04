package com.linweiyun.genshin.mixin.interfaces;

import com.linweiyun.genshin.enums.ElementalsGIM;

public interface IMonsterLevel {

    int genshin$getMonsterLevel();

    void genshin$setMonsterLevel(int level);

    int genshin$getDefense();

    float genshin$getElementResistance(ElementalsGIM element);

    float genshin$getPhysicalResistance();
}