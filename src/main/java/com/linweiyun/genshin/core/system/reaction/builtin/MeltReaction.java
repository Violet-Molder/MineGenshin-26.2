package com.linweiyun.genshin.core.system.reaction.builtin;

import com.linweiyun.genshin.config.ElementalReactionConfig;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.status.StatusInstance;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.genshin.core.system.reaction.ElementalReaction;
import com.linweiyun.genshin.core.system.reaction.ReactionContext;
import com.linweiyun.genshin.core.system.reaction.ReactionResult;
import com.linweiyun.genshin.enums.ElementalReactionType;
/**
 * 融化反应 —— 增幅反应
 *
 * 消耗比：火:冰 = 1:2（火1份，冰2份，同时消耗）
 *   先手冰(或冻) 后手火 → 倍率 2.0（火融化，火克冰）
 *   先手火 后手冰(或冻) → 倍率 1.5（冰融化，冰被克）
 *
 * 冰和冻可以共同被消耗（通过 getMainElement 都归并到 CYRO）
 */
public class MeltReaction extends ElementalReaction {

    private static float getDominantMultiplier() { return Float.parseFloat(ElementalReactionConfig.MELT_COEFFICIENT_POSITIVE.get()); }
    private static float getSubmissiveMultiplier() { return Float.parseFloat(ElementalReactionConfig.MELT_COEFFICIENT_NEGATIVE.get()); }

    public MeltReaction(ElementalReactionType type,
                        String elementAId, String elementBId,
                        float ratioA, float ratioB, int basePriority) {
        super(type, elementAId, elementBId, ratioA, ratioB, basePriority);
    }

    @Override
    public ReactionResult execute(ReactionContext ctx) {
        GenshinElement elA = getElementA();
        GenshinElement elB = getElementB();
        GenshinElement attackerMain = ctx.attackerElement().getMainElement();
        boolean attackerIsA = (attackerMain == elA);

        // 找到目标身上主元素为 elementB 的所有实例（CYRO 主元素，可能是 CYRO 或 FROZEN）
        GenshinElement defenderTarget = attackerIsA ? elB : elA;
        float totalDefenderQty = sumMainElementQuantity(ctx, defenderTarget);

        if (totalDefenderQty <= 0f) {
            return ReactionResult.builder(reactionType).build();
        }

        float attackerQty = ctx.attackerUnit();

        // 按比例同时消耗
        float consumedA, consumedB;
        if (attackerIsA) {
            float[] consumed = calculateConsumption(attackerQty, totalDefenderQty);
            consumedA = consumed[0]; consumedB = consumed[1];
        } else {
            float[] consumed = calculateConsumption(totalDefenderQty, attackerQty);
            consumedA = consumed[0]; consumedB = consumed[1];
        }

        // 从容器里扣
        consumeElementUnit(ctx.targetContainer(), elB, consumedB);
        consumeElementUnit(ctx.targetContainer(), elA, consumedA);
        // 倍率
        boolean dominant = attackerIsA;
        float multiplier = dominant ? getDominantMultiplier() : getSubmissiveMultiplier();
        float consumedAttacker = attackerIsA ? consumedA : consumedB;

        return ReactionResult.builder(reactionType)
                .reacted()
                .consumedAttacker(consumedAttacker)
                .consumedDefender(attackerIsA ? consumedB : consumedA)
                .amplified(multiplier)
                .build();
    }

    private float sumMainElementQuantity(ReactionContext ctx, GenshinElement mainTarget) {
        float sum = 0f;
        for (StatusInstance inst : ctx.targetContainer().getAll()) {
            if (inst.isFinished()) continue;
            if (!(inst instanceof ElementalAttachmentInstance ea)) continue;
            if (ea.getElement().getMainElement() == mainTarget) {
                sum += ea.getUnit();
            }
        }
        return sum;
    }


}