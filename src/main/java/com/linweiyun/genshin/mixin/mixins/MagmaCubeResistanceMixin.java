package com.linweiyun.genshin.mixin.mixins;

import com.linweiyun.genshin.enums.ElementalsGIM;
import com.linweiyun.genshin.mixin.interfaces.IMonsterLevel;

import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MagmaCube.class)
public abstract class MagmaCubeResistanceMixin implements IMonsterLevel {

    private static final float PYRO_RESISTANCE = 0.80f;
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
        return genshin$monsterLevel * 500 + 500;
    }

    @Override
    public float genshin$getElementResistance(ElementalsGIM element) {
        if (element == ElementalsGIM.PYRO) return PYRO_RESISTANCE;
        return DEFAULT_RESISTANCE;
    }

    @Override
    public float genshin$getPhysicalResistance() {
        return DEFAULT_RESISTANCE;
    }
}