package com.linweiyun.genshin.core.system.combat.damage;

import com.linweiyun.genshin.content.entities.teyvat.TeyvatLivingEntity;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import com.linweiyun.genshin.enums.ElementalsGIM;
import net.minecraft.world.entity.LivingEntity;

/**
 * 统一从攻击方 / 被攻击方身上获取等级、防御、元素抗性。
 * 三种来源：
 *   1. PGCharacter          → 从角色数据里读
 *   2. TeyvatLivingEntity   → MOD 的实体，以后会加自定义属性，目前先给默认值
 *   3. 普通 LivingEntity    → MC 原生实体，给默认值
 */
public final class CombatEntityAccessor {
    private CombatEntityAccessor() {}

    /** TeyvatLivingEntity 和普通 LivingEntity 的默认等级 */
    private static final int DEFAULT_MOB_LEVEL = 10;
    /** TeyvatLivingEntity 和普通 LivingEntity 的默认元素抗性 10% */
    private static final float DEFAULT_MOB_RESISTANCE = 0.10f;

    // ==================== 等级 ====================

    /**
     * 获取攻击方等级
     */
    public static int getAttackerLevel(LivingEntity attacker, PGCharacter attackerCharacter) {
        if (attackerCharacter != null) {
            return attackerCharacter.getData().getLevel();
        }
        // TODO: TeyvatLivingEntity 以后加 getLevel() 接口方法
        return DEFAULT_MOB_LEVEL;
    }

    /**
     * 获取被攻击方等级（剧变反应等级系数用，防御公式本身不用）
     */
    public static int getDefenderLevel(LivingEntity defender, PGCharacter defenderCharacter) {
        if (defenderCharacter != null) {
            return defenderCharacter.getData().getLevel();
        }
        return DEFAULT_MOB_LEVEL;
    }

    // ==================== 防御力 ====================

    /**
     * 获取被攻击方防御力
     * PGCharacter → 角色 DEF 属性
     * TeyvatLivingEntity / 普通 LivingEntity → 怪物防御力 = 等级系数 = level×500+500
     */
    public static double getDefenderDefense(LivingEntity defender, PGCharacter defenderCharacter) {
        if (defenderCharacter != null) {
            return defenderCharacter.getData().getAttributeTotalValue(ModAttributes.DEF.value());
        }
        // TODO: TeyvatLivingEntity 以后加 getDefense() 接口方法
        double defense = CombatMath.levelCoefficient(DEFAULT_MOB_LEVEL);
        return defense;
    }

    // ==================== 元素抗性 ====================

    /**
     * 获取被攻击方对应元素抗性（小数，如 0.3 = 30%）
     */
    public static float getDefenderResistance(LivingEntity defender, PGCharacter defenderCharacter, ElementalsGIM element) {
        if (element == ElementalsGIM.FYSIKOS) {
            return getPhysicalResistance(defender, defenderCharacter);
        }
        if (defenderCharacter != null) {
            return getElementResistanceFromCharacter(defenderCharacter, element);
        }
        // TODO: TeyvatLivingEntity 以后加 getResistance(element) 接口方法
        return DEFAULT_MOB_RESISTANCE;
    }

    private static float getElementResistanceFromCharacter(PGCharacter c, ElementalsGIM element) {
        var data = c.getData();
        return switch (element) {
            case PYRO    -> (float) data.getAttributeTotalValue(ModAttributes.PYRO_RES.value());
            case HYDRO   -> (float) data.getAttributeTotalValue(ModAttributes.HYDRO_RES.value());
            case DENDRO  -> (float) data.getAttributeTotalValue(ModAttributes.DENDRO_RES.value());
            case ELECTRO -> (float) data.getAttributeTotalValue(ModAttributes.ELECTRO_RES.value());
            case ANEMO   -> (float) data.getAttributeTotalValue(ModAttributes.ANEMO_RES.value());
            case CYRO    -> (float) data.getAttributeTotalValue(ModAttributes.CYRO_RES.value());
            case GEO     -> (float) data.getAttributeTotalValue(ModAttributes.GEO_RES.value());
            default      -> 0.0f;
        };
    }

    private static float getPhysicalResistance(LivingEntity defender, PGCharacter defenderCharacter) {
        if (defenderCharacter != null) {
            return (float) defenderCharacter.getData().getAttributeTotalValue(ModAttributes.PHYSICAL_RES.value());
        }
        // TODO: TeyvatLivingEntity 以后加 getPhysicalResistance()
        return DEFAULT_MOB_RESISTANCE;
    }
}