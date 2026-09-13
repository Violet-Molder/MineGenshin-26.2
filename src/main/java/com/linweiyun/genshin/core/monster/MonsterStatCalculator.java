package com.linweiyun.genshin.core.monster;

import com.linweiyun.genshin.config.MonsterAttackConfig;
import com.linweiyun.genshin.config.MonsterHealthConfig;

public class MonsterStatCalculator {

    public static float calculateHealth(int level, float entityMultiplier, float environmentMultiplier) {
        return (float) (MonsterHealthConfig.getHealthBase(level) * entityMultiplier * environmentMultiplier);
    }

    public static float calculateAttack(int level, float entityMultiplier, float environmentMultiplier) {
        return (float) (MonsterAttackConfig.getAttackBase(level) * entityMultiplier * environmentMultiplier);
    }
}