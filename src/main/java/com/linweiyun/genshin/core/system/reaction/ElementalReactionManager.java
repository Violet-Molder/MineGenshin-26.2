package com.linweiyun.genshin.core.system.reaction;

import com.linweiyun.genshin.core.status.StatusInstance;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.genshin.core.system.registry.ModRegistries;
import com.linweiyun.genshin.enums.ElementalsGIM;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 元素反应管理器 —— 单例（用静态方法）
 *
 * 核心流程（附着入口调用 tryReactAfterAttach）：
 *   1. 拿到目标身上所有先手元素（遍历 container 里的 ElementalAttachmentInstance）
 *   2. 拿到本次后手附着的元素和量
 *   3. 收集所有"后手元素 + 某个先手元素"能触发的反应
 *   4. 按默认优先级排序（优先级小的先）
 *   5. 依次执行反应，每一轮消耗后检查后手是否还有剩余
 *   6. 所有反应执行完后，处理后手残留（NORMAL_ATTACK 残留清 0，其他保留）
 */
public class ElementalReactionManager {

    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 默认反应优先级序列 —— 后手元素与各先手元素反应的默认顺序
     * 风(ANEMO) 冰(CYRO) 电(ELECTRO) 水(HYDRO) 冻(FROZEN) 火(PYRO) 草(DENDRO) 激(AGGRAVATE) 岩(GEO)
     */
    private static final ElementalsGIM[] DEFAULT_PRIORITY_ORDER = {
            ElementalsGIM.ANEMO, ElementalsGIM.CYRO, ElementalsGIM.ELECTRO,
            ElementalsGIM.HYDRO, ElementalsGIM.FROZEN, ElementalsGIM.PYRO,
            ElementalsGIM.DENDRO, ElementalsGIM.AGGRAVATE, ElementalsGIM.GEO
    };

    /** 给定主元素返回其在默认序列中的 index（越小越优先） */
    public static int getDefaultPriority(ElementalsGIM mainElement) {
        for (int i = 0; i < DEFAULT_PRIORITY_ORDER.length; i++) {
            if (DEFAULT_PRIORITY_ORDER[i] == mainElement) return i;
        }
        return 99;
    }

    /**
     * 附着后触发反应的主入口 —— 建议在 HurtEntityHelper 中，ElementalAttachmentHelper.attach 之后调用
     *
     * @param context 反应上下文
     * @return 首个触发的反应结果（如果是增幅反应，需用其倍率修改伤害）
     */
    public static ReactionResult tryReactAfterAttach(ReactionContext context) {
        // 后手附着量（全额参与反应，先手已在附着时衰减到 0.8，后手全额）
        float remainingAttackerQty = context.attackerQuantity;

        // 收集所有"先手元素实例"（排除后手自己的那一份，虽然刚附着的也是后手元素）
        List<ElementalAttachmentInstance> defenders = collectDefenders(context);

        if (defenders.isEmpty()) {
            return ReactionResult.builder(null).build();
        }

        // 找到每个先手元素能触发的反应，按优先级排序
        List<Candidate> candidates = new ArrayList<>();
        for (ElementalAttachmentInstance defender : defenders) {
            ElementalsGIM defenderMain = defender.getElement().getMainElement();
            ElementalsGIM attackerMain = context.attackerElement.getMainElement();

            for (ElementalReaction reaction : ModRegistries.ELEMENTAL_REACTIONS_REGISTRY) {
                if (!reaction.canMatch(attackerMain, defenderMain)) continue;
                if (reaction.isBlocked(context)) continue;

                int priority = reaction.getBasePriority();
                // 如果反应未设置专属优先级，则按默认序列算
                if (priority < 0) {
                    priority = ReactionPriorityCalculator.computeFor(
                            context.attackerElement, defender.getElement(), reaction);
                }

                candidates.add(new Candidate(reaction, defender, priority));
            }
        }

        // 按优先级排序（小的在前）
        candidates.sort(Comparator.comparingInt(c -> c.priority));

        ReactionResult firstAmplified = null;

        // 依次执行
        for (Candidate cand : candidates) {
            // 后手已经被消耗完了，停止
            if (remainingAttackerQty <= 0f) break;
            // 先手的那个实例已经空了，跳过
            if (cand.defender.isFinished()) continue;

            ReactionContext roundContext = new ReactionContext(
                    context.target,
                    context.attackerElement,
                    remainingAttackerQty,
                    context.attackerSource,
                    context.attackerProfile,
                    context.damageSpec,
                    context.attackerEntity,
                    context.targetContainer
            );

            ReactionResult result = cand.reaction.execute(roundContext);

            if (result.isReacted()) {
                remainingAttackerQty -= result.getConsumedAttacker();
                if (firstAmplified == null && result.isAmplified()) {
                    firstAmplified = result;
                }
                // 把反应产生的残留也加回去（比如 SPECIAL 附着）
                if (result.getAttackerResidual() > 0f) {
                    // 残留处理由具体反应负责（比如冻结生成冻元素），这里只处理基本的
                }
            }
        }

        // 处理后手残留
        applyAttackerResidual(context, remainingAttackerQty);

        return firstAmplified != null ? firstAmplified :
                ReactionResult.builder(null).build();
    }

    /** 收集目标身上的"先手元素"实例（排除自己这方附着的、元素不反应的等） */
    private static List<ElementalAttachmentInstance> collectDefenders(ReactionContext context) {
        List<ElementalAttachmentInstance> result = new ArrayList<>();
        ElementalsGIM attackerMain = context.attackerElement.getMainElement();

        for (StatusInstance inst : context.targetContainer.getAll()) {
            if (inst.isFinished()) continue;
            if (!(inst instanceof ElementalAttachmentInstance ea)) continue;

            ElementalsGIM defMain = ea.getElement().getMainElement();
            // 同主元素不反应（后手火不会跟先手火反应）
            if (defMain == attackerMain) continue;
            // FYSIKOS 不参与反应
            if (ea.getElement() == ElementalsGIM.FYSIKOS) continue;

            result.add(ea);
        }
        return result;
    }

    /** 后手残留处理 */
    private static void applyAttackerResidual(ReactionContext context, float remainingQty) {
        if (remainingQty <= 0f) return;

        if (context.attackerFollowsNoResidualRule()) {
            // 常规附着遵循后手不残留：反应后清 0
            ElementalAttachmentHelper.consume(
                    context.target, context.attackerElement, Float.MAX_VALUE);
        } else {
            // 直接附着：保留剩余量（不用管，附着时已经加进容器了）
            // 如果反应需要额外生成元素（如冻结生成冻），在反应的 execute 里处理
        }
    }

    private static class Candidate {
        final ElementalReaction reaction;
        final ElementalAttachmentInstance defender;
        final int priority;

        Candidate(ElementalReaction r, ElementalAttachmentInstance d, int p) {
            this.reaction = r; this.defender = d; this.priority = p;
        }
    }
}
