package com.linweiyun.genshin.core.system.reaction.builtin;

import com.linweiyun.genshin.core.status.StatusInstance;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.genshin.core.system.reaction.ElementalReaction;
import com.linweiyun.genshin.core.system.reaction.ReactionContext;
import com.linweiyun.genshin.core.system.reaction.ReactionResult;
import com.linweiyun.genshin.enums.ElementalsGIM;
import com.linweiyun.genshin.enums.ElementalReactionType;
import net.minecraft.world.entity.LivingEntity;
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

    private static final float DOMINANT_MULTIPLIER = 2.0f;
    private static final float SUBMISSIVE_MULTIPLIER = 1.5f;

    public MeltReaction(ElementalReactionType type,
                        ElementalsGIM elementA, ElementalsGIM elementB,
                        float ratioA, float ratioB, int basePriority) {
        super(type, elementA, elementB, ratioA, ratioB, basePriority);
    }

    @Override
    public ReactionResult execute(ReactionContext ctx) {
        ElementalsGIM attackerMain = ctx.attackerElement.getMainElement();
        boolean attackerIsA = (attackerMain == elementA);

        // 找到目标身上主元素为 elementB 的所有实例（CYRO 主元素，可能是 CYRO 或 FROZEN）
        ElementalsGIM defenderTarget = attackerIsA ? elementB : elementA;
        float totalDefenderQty = sumMainElementQuantity(ctx, defenderTarget);

        if (totalDefenderQty <= 0f) {
            return ReactionResult.builder(reactionType).build();
        }

        float attackerQty = ctx.attackerQuantity;

        // 按比例同时消耗
        float consumedA, consumedB;
        if (attackerIsA) {
            float[] consumed = calculateConsumption(attackerQty, totalDefenderQty, false);
            consumedA = consumed[0]; consumedB = consumed[1];
        } else {
            float[] consumed = calculateConsumption(totalDefenderQty, attackerQty, true);
            consumedA = consumed[0]; consumedB = consumed[1];
        }

        // 从容器里扣
        if (attackerIsA) {
            consumeAttacker(ctx.target, elementA, consumedA);
            consumeDefenderMain(ctx.targetContainer, elementB, consumedB);
        } else {
            consumeAttacker(ctx.target, elementB, consumedB);
            consumeDefenderMain(ctx.targetContainer, elementA, consumedA);
        }

        // 倍率
        boolean dominant = attackerIsA;
        float multiplier = dominant ? DOMINANT_MULTIPLIER : SUBMISSIVE_MULTIPLIER;
        float consumedAttacker = attackerIsA ? consumedA : consumedB;

        return ReactionResult.builder(reactionType)
                .reacted()
                .consumedAttacker(consumedAttacker)
                .consumedDefender(attackerIsA ? consumedB : consumedA)
                .amplified(multiplier)
                .build();
    }

    private float sumMainElementQuantity(ReactionContext ctx, ElementalsGIM mainTarget) {
        float sum = 0f;
        for (StatusInstance inst : ctx.targetContainer.getAll()) {
            if (inst.isFinished()) continue;
            if (!(inst instanceof ElementalAttachmentInstance ea)) continue;
            if (ea.getElement().getMainElement() == mainTarget) {
                sum += ea.getQuantity();
            }
        }
        return sum;
    }

    private void consumeAttacker(LivingEntity target, ElementalsGIM element, float amount) {
        ElementalAttachmentHelper.consume(target, element, amount);
    }

    /**
     * 消耗目标身上主元素为 mainTarget 的所有实例（如消耗 CYRO 主元素时同时消耗 CYRO 和 FROZEN）
     * 不严格保证消耗顺序，实际中如果有冻结藏冰/藏水的情况需要特殊顺序
     */
    private void consumeDefenderMain(com.linweiyun.genshin.core.attachment.StatusContainer container,
                                     ElementalsGIM mainTarget, float amount) {
        float remaining = amount;
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (!(inst instanceof ElementalAttachmentInstance ea)) continue;
            if (ea.getElement().getMainElement() != mainTarget) continue;
            float consumed = ea.consume(remaining);
            remaining -= consumed;
            if (remaining <= 0f) break;
        }
    }
}
