package com.linweiyun.genshin.core.system.reaction.builtin;

import com.linweiyun.genshin.core.status.StatusInstance;
import com.linweiyun.genshin.core.system.about.AttachmentProfile;
import com.linweiyun.genshin.core.system.about.AttachmentSource;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.genshin.core.system.reaction.ElementalReaction;
import com.linweiyun.genshin.core.system.reaction.ReactionContext;
import com.linweiyun.genshin.core.system.reaction.ReactionResult;
import com.linweiyun.genshin.enums.ElementalsGIM;
import com.linweiyun.genshin.enums.ElementalReactionType;
import net.minecraft.world.entity.LivingEntity;

/**
 * 冻结反应 —— 特殊反应（非增幅非聚变）
 *
 * 消耗比：水:冰 = 1:1
 *   1水 + 1冰 同时消耗 → 生成 2 倍消耗量的 FROZEN（冻）元素在目标身上
 *
 * 关键规则：
 *   - 不能与 FROZEN 本身发生冻结反应（已经冻住了不能再冻）
 *   - 冻结反应不是增幅反应，不影响伤害
 *   - 冻结反应产生 FROZEN 元素（通过 ElementalAttachmentHelper.attach 添加）
 *
 * TODO: 完整冻结效果（减速、冻结实体等）暂不实现，待后续补充
 * TODO: 冻结藏冰/藏水逻辑 —— 冻结反应发生后还能残留额外的冰或水
 */
public class FreezeReaction extends ElementalReaction {

    /** 消耗总量 × 倍率 = 生成的冻元素量 */
    private static final float FROZEN_MULTIPLIER = 2.0f;

    public FreezeReaction(ElementalReactionType type,
                          ElementalsGIM elementA, ElementalsGIM elementB,
                          float ratioA, float ratioB, int basePriority) {
        super(type, elementA, elementB, ratioA, ratioB, basePriority);
    }

    /**
     * 冻结不能对 FROZEN 自身发生 —— 但可以与 CYRO 主元素（冰）反应
     * 实际上：后手水 + 先手冻 是可以反应的（藏水/藏冰逻辑）
     * 这里只禁止"后手元素和目标上都是 FROZEN 元素"的情况
     */
    @Override
    public boolean isBlocked(ReactionContext context) {
        if (context.attackerElement == ElementalsGIM.FROZEN) return true;
        return false;
    }

    @Override
    public ReactionResult execute(ReactionContext ctx) {
        ElementalsGIM attackerMain = ctx.attackerElement.getMainElement();
        boolean attackerIsA = (attackerMain == elementA);

        ElementalsGIM defenderTarget = attackerIsA ? elementB : elementA;
        float totalDefenderQty = sumMainElementQuantity(ctx, defenderTarget);

        if (totalDefenderQty <= 0f) {
            return ReactionResult.builder(reactionType).build();
        }

        float attackerQty = ctx.attackerQuantity;

        // 按 1:1 同时消耗
        float consumedA, consumedB;
        if (attackerIsA) {
            float[] consumed = calculateConsumption(attackerQty, totalDefenderQty, false);
            consumedA = consumed[0]; consumedB = consumed[1];
        } else {
            float[] consumed = calculateConsumption(totalDefenderQty, attackerQty, true);
            consumedA = consumed[0]; consumedB = consumed[1];
        }

        // 扣掉消耗的部分
        float totalConsumed = consumedA + consumedB;
        float consumedAttacker = attackerIsA ? consumedA : consumedB;

        if (attackerIsA) {
            consumeAttacker(ctx.target, elementA, consumedA);
            consumeDefenderMain(ctx.targetContainer, elementB, consumedB);
        } else {
            consumeAttacker(ctx.target, elementB, consumedB);
            consumeDefenderMain(ctx.targetContainer, elementA, consumedA);
        }

        // 生成冻元素（总量 × 2）
        if (totalConsumed > 0f) {
            float frozenQty = totalConsumed * FROZEN_MULTIPLIER;
            // TODO: 完整冻结效果（减速、冻结实体）待实现
            ElementalAttachmentHelper.attach(
                    ctx.target,
                    ElementalsGIM.FROZEN,
                    AttachmentSource.SPECIAL,
                    new AttachmentProfile(frozenQty, 1.0f, 0.0f, 0.0f)
            );
        }

        return ReactionResult.builder(reactionType)
                .reacted()
                .consumedAttacker(consumedAttacker)
                .consumedDefender(attackerIsA ? consumedB : consumedA)
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