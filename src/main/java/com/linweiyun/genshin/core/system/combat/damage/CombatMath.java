package com.linweiyun.genshin.core.system.combat.damage;

public final class CombatMath {
    private CombatMath() {}

    /** 原神等级系数：level × 500 + 500 */
    public static double levelCoefficient(int level) {
        return level * 5.0 + 500.0;
    }
    public static float dmgBonusZone(float elementalBonus) {
        return 1.0f + elementalBonus;
    }

    /**
     * 防御区 = 攻方等级系数 / (攻方等级系数 + 被攻方防御)。
     *
     * <p>也就是「这一下能打进去多少比例」，值域 (0,1)，防御越高越小。
     *
     * <p><b>注意</b>：早期版本这里返回的是它的<b>补数</b>（{@code 1 - 防御区}），
     * 调用方要再取一次 {@code 1 -} 才得到真正的防御区 —— 双重取反很容易看错，
     * 现在直接返回防御区本身。
     */
    public static float defenseZone(int attackerLevel, double defenderDef) {
        double atkCoef = levelCoefficient(attackerLevel);
        return (float) (atkCoef / (atkCoef + defenderDef));
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