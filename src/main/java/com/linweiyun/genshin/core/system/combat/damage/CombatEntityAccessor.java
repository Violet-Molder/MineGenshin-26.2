package com.linweiyun.genshin.core.system.combat.damage;

import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import com.linweiyun.genshin.enums.ElementalsGIM;
import com.linweiyun.genshin.mixin.interfaces.IMonsterLevel;
import net.minecraft.client.model.animal.fish.PufferfishBigModel;
import net.minecraft.world.entity.LivingEntity;

/**
 * 统一从攻击方 / 被攻击方身上获取等级、防御、元素抗性。
 * 来源优先级：
 *   1. PGCharacter     → 角色数据里读
 *   2. IMonsterLevel   → Mixin 注入的怪物数据（外部怪物才有）
 *   3. 默认值兜底
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
        if (attacker instanceof IMonsterLevel ml && ml.genshin$getMonsterLevel() > 0) {
            return ml.genshin$getMonsterLevel();
        }
        return DEFAULT_MOB_LEVEL;
    }

    /**
     * 获取被攻击方等级（剧变反应等级系数用，防御公式本身不用）
     */
    public static int getDefenderLevel(LivingEntity defender, PGCharacter defenderCharacter) {
        if (defenderCharacter != null) {
            return defenderCharacter.getData().getLevel();
        }
        if (defender instanceof IMonsterLevel ml && ml.genshin$getMonsterLevel() > 0) {
            return ml.genshin$getMonsterLevel();
        }
        return DEFAULT_MOB_LEVEL;
    }

    public static float getDamageBonus(PGCharacter character, ElementalsGIM element) {
        if (character == null) return 0;
        return getElementBonusFromCharacter(character, element);
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
        if (defender instanceof IMonsterLevel ml && ml.genshin$getMonsterLevel() > 0) {
            return ml.genshin$getDefense();
        }
        return CombatMath.levelCoefficient(DEFAULT_MOB_LEVEL);
    }

    // ==================== 元素加成 ====================
    private static float getElementBonusFromCharacter(PGCharacter character, ElementalsGIM elemental) {
        var data = character.getData();
        return switch (elemental) {
            case PYRO    -> (float) data.getAttributeTotalValue(ModAttributes.PYRO_BONUS.value());
            case HYDRO   -> (float) data.getAttributeTotalValue(ModAttributes.HYDRO_BONUS.value());
            case DENDRO  -> (float) data.getAttributeTotalValue(ModAttributes.DENDRO_BONUS.value());
            case ELECTRO -> (float) data.getAttributeTotalValue(ModAttributes.ELECTRO_BONUS.value());
            case ANEMO   -> (float) data.getAttributeTotalValue(ModAttributes.ANEMO_BONUS.value());
            case CYRO    -> (float) data.getAttributeTotalValue(ModAttributes.CYRO_BONUS.value());
            case GEO     -> (float) data.getAttributeTotalValue(ModAttributes.GEO_BONUS.value());
            default      -> 0.0f;
        };
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
        if (defender instanceof IMonsterLevel ml) {
            return ml.genshin$getElementResistance(element);
        }
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
        if (defender instanceof IMonsterLevel ml) {
            return ml.genshin$getPhysicalResistance();
        }
        return DEFAULT_MOB_RESISTANCE;
    }
}