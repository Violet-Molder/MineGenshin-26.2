package com.linweiyun.genshin.mixin.mixins;

import com.linweiyun.genshin.enums.ElementalsGIM;
import com.linweiyun.genshin.mixin.interfaces.IMonsterLevel;
import net.minecraft.world.entity.monster.Monster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Monster.class)
public abstract class MonsterMixin implements IMonsterLevel {

    private static final float DEFAULT_RESISTANCE = 0.10f;

    @Unique
    private int genshin$monsterLevel = 0;

    @Unique
    private boolean genshin$levelLocked = false;

    @Override
    public int genshin$getMonsterLevel() {
        return genshin$monsterLevel;
    }

    @Override
    public void genshin$setMonsterLevel(int level) {
        if (genshin$levelLocked) return;
        this.genshin$monsterLevel = level;
        this.genshin$levelLocked = true;
    }

    @Override
    public int genshin$getDefense() {
        return genshin$monsterLevel * 5 + 500;
    }

    @Override
    public float genshin$getElementResistance(ElementalsGIM element) {
        return DEFAULT_RESISTANCE;
    }

    @Override
    public float genshin$getPhysicalResistance() {
        return DEFAULT_RESISTANCE;
    }
}