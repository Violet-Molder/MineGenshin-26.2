package com.linweiyun.genshin.core.system.reaction.builtin;

import com.linweiyun.genshin.config.reaction.ReactionConfig;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.system.reaction.ElementalReaction;
import com.linweiyun.genshin.core.system.reaction.ReactionContext;
import com.linweiyun.genshin.core.system.reaction.ReactionResult;
import com.linweiyun.genshin.enums.ElementalReactionType;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class SuperConductReaction extends ElementalReaction {
    public static final Logger LOGGER = LogUtils.getLogger();
    private static float getReactionMultiplier() { return ReactionConfig.SUPERCONDUCT.getFloat(); }

    protected SuperConductReaction(ElementalReactionType reactionType,
                                   String elementAId, String elementBId,
                                   float ratioA, float ratioB, int basePriority) {
        super(reactionType, elementAId, elementBId, ratioA, ratioB, basePriority);
    }


    @Override
    public ReactionResult execute(ReactionContext context) {
        GenshinElement elA = getElementA();
        GenshinElement elB = getElementB();
        GenshinElement attackerMain = context.attackerElement().getMainElement();
        boolean attackerIsA = (attackerMain == elA);
        GenshinElement defenderTarget = attackerIsA ? elB : elA;
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
        consumeElementUnit(context.targetContainer(), elB, consumedB);
        consumeElementUnit(context.targetContainer(), elA, consumedA);
        return null;
    }
}