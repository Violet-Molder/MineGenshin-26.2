package com.linweiyun.genshin.core.system.combat.damage;

public final class CombatMath {
    private CombatMath() {}

    /** 原神等级系数：level × 500 + 500 */
    public static double levelCoefficient(int level) {
        return level * 500.0 + 500.0;
    }

    /**
     * 防御区：
     * 1 - (攻方等级系数) / (攻方等级系数 + 被攻方防御)
     */
    public static float defenseZone(int attackerLevel, double defenderDef) {
        double atkCoef = levelCoefficient(attackerLevel);
        return (float) (1.0 - atkCoef / (atkCoef + defenderDef));
    }

    /**
     * 抗性区 —— 原神分段公式
     * res < 0        → 1 - res/2       （负抗性=加伤，比如 -5% → 1.025，承伤+2.5%）
     * 0 ≤ res < 0.75 → 1 - res
     * res ≥ 0.75     → 1 / (1 + 4×res)  （75% 时 = 0.25，和第二段衔接）
     *
     * @param res 抗性小数，如 0.3 = 30%，-0.05 = -5%
     */
    public static float resistanceZone(float res) {
        if (res <= 0f) {
            return 1.0f - res / 2.0f;
        } else if (res <= 0.75f) {
            return 1.0f - res;
        } else {
            return 1.0f / (1.0f + 4.0f * res);
        }
    }
}