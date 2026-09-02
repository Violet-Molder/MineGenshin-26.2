package com.linweiyun.genshin.core.system.reaction.builtin;

import com.linweiyun.genshin.core.system.about.AttachmentProfile;
import com.linweiyun.genshin.core.system.about.AttachmentSource;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSpec;
import com.linweiyun.genshin.core.system.reaction.ElementalReaction;
import com.linweiyun.genshin.core.system.reaction.ReactionContext;
import com.linweiyun.genshin.core.system.reaction.ReactionPriorityCalculator;
import com.linweiyun.genshin.core.system.reaction.ReactionResult;
import com.linweiyun.genshin.enums.ElementalsGIM;
import com.linweiyun.genshin.enums.ElementalReactionType;
import net.minecraft.world.entity.LivingEntity;

/**
 * 蒸发反应 —— 增幅反应
 *
 * 消耗比：火:水 = 1:2（火1份，水2份，同时消耗）
 *   先手水后手火 → 倍率 2.0（火蒸发，火克水）
 *   先手火后手水 → 倍率 1.5（水蒸发，水被克）
 *
 * 例外：冻结状态下（目标身上有 FROZEN）禁止蒸发反应
 *
 * TODO: 完整的蒸发倍率取决于火元素精通和水元素精通，当前先硬编码倍率
 */
public class VaporizeReaction extends ElementalReaction {

    private static final float DOMINANT_MULTIPLIER = 2.0f;
    private static final float SUBMISSIVE_MULTIPLIER = 1.5f;

    public VaporizeReaction(ElementalReactionType type,
                            ElementalsGIM elementA, ElementalsGIM elementB,
                            float ratioA, float ratioB, int basePriority) {
        super(type, elementA, elementB, ratioA, ratioB, basePriority);
    }

    @Override
    public boolean isBlocked(ReactionContext context) {
        // 冻结状态下禁止蒸发
        return ReactionPriorityCalculator.hasFrozen(context.targetContainer);
    }

    @Override
    public ReactionResult execute(ReactionContext ctx) {
        // 确定先手是 A 还是 B，以及实际对应的 ElementalsGIM
        ElementalsGIM attackerMain = ctx.attackerElement.getMainElement();
        boolean attackerIsA = (attackerMain == elementA);

        // 找到目标身上对应的先手实例
        ElementalAttachmentInstance defInstance = findDefenderInstance(ctx,
                attackerIsA ? elementB : elementA);

        if (defInstance == null || defInstance.isFinished()) {
            return ReactionResult.builder(reactionType).build();
        }

        float attackerQty = ctx.attackerQuantity;
        float defenderQty = defInstance.getQuantity();

        // 按比例同时消耗
        float consumedA, consumedB;
        float[] consumed;
        if (attackerIsA) {
            // 后手是 A，先手是 B
            consumed = calculateConsumption(attackerQty, defenderQty, false);
        } else {
            // 后手是 B，先手是 A
            consumed = calculateConsumption(defenderQty, attackerQty, true);
            // consumed[0] 是 A 的消耗，consumed[1] 是 B 的消耗
        }
        consumedA = consumed[0];
        consumedB = consumed[1];

        // 实际从容器里扣
        if (attackerIsA) {
            consumeAttacker(ctx.target, elementA, consumedA);
            defInstance.consume(consumedB);
        } else {
            consumeAttacker(ctx.target, elementB, consumedB);
            defInstance.consume(consumedA);
        }

        // 判断克制关系 → 倍率
        // elementA 是克制方（消耗少的）
        // attackerIsA = true → 后手是克制方 → 倍率 2.0
        // attackerIsA = false → 后手是被克制方 → 倍率 1.5
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

    private ElementalAttachmentInstance findDefenderInstance(ReactionContext ctx,
                                                             ElementalsGIM targetMain) {
        // 遍历容器，找到主元素匹配的先手实例（优先找主元素精确匹配的，再找类元素）
        ElementalAttachmentInstance subElementMatch = null;
        for (com.linweiyun.genshin.core.status.StatusInstance inst : ctx.targetContainer.getAll()) {
            if (inst.isFinished()) continue;
            if (!(inst instanceof ElementalAttachmentInstance ea)) continue;
            ElementalsGIM eaElement = ea.getElement();
            if (eaElement.getMainElement() != targetMain) continue;
            // 精确匹配优先
            if (eaElement == targetMain) return ea;
            if (subElementMatch == null) subElementMatch = ea;
        }
        return subElementMatch;
    }

    private void consumeAttacker(LivingEntity target, ElementalsGIM element, float amount) {
        ElementalAttachmentHelper.consume(target, element, amount);
    }
}
