package com.linweiyun.genshin.core.system.reaction.builtin;

import com.linweiyun.genshin.config.ElementalReactionConfig;
import com.linweiyun.genshin.core.system.reaction.ElementalReaction;
import com.linweiyun.genshin.core.system.reaction.ReactionContext;
import com.linweiyun.genshin.core.system.reaction.ReactionResult;
import com.linweiyun.genshin.enums.ElementalReactionType;
import com.linweiyun.genshin.enums.ElementalsGIM;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class SuperConductReaction extends ElementalReaction {
    public static final Logger LOGGER = LogUtils.getLogger();
    private static float getReactionMultiplier() { return Float.parseFloat(ElementalReactionConfig.SUPERCONDUCT_COEFFICIENT.get()); }
    /**
     * @param reactionType 反应类型枚举
     * @param elementA     元素1（消耗 ratioA 份）
     * @param elementB     元素2（消耗 ratioB 份，被克制方）
     * @param ratioA       元素1消耗系数
     * @param ratioB       元素2消耗系数
     * @param basePriority 默认优先级（数值越小越优先）
     */
    protected SuperConductReaction(ElementalReactionType reactionType,
                                   ElementalsGIM elementA, ElementalsGIM elementB,
                                   float ratioA, float ratioB, int basePriority) {
        super(reactionType, elementA, elementB, ratioA, ratioB, basePriority);
    }


    @Override
    public ReactionResult execute(ReactionContext context) {
        ElementalsGIM attackerMain = context.attackerElement().getMainElement();
        boolean attackerIsA = (attackerMain == elementA);
        ElementalsGIM defenderTarget = attackerIsA ? elementB : elementA;
        float totalDefenderUnit = sumConsumable(context.targetContainer(), defenderTarget);

        if (totalDefenderUnit <= 0) {
            return ReactionResult.builder(reactionType).build();
        }
        float attackerUnit = context.attackerUnit();

        float consumedA, consumedB;
        float[] consumed;
        if (attackerIsA) {
            consumed = calculateConsumption(attackerUnit, totalDefenderUnit);
        } else {
            consumed = calculateConsumption(totalDefenderUnit, attackerUnit);
        }
        consumedA = consumed[0]; consumedB = consumed[1];
        consumeElementUnit(context.targetContainer(), elementB, consumedB);
        consumeElementUnit(context.targetContainer(), elementA, consumedA);
        return null;
    }
}