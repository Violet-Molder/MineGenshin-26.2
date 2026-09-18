package com.linweiyun.genshin.core.system.reaction;

import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.status.StatusInstance;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.genshin.core.system.registry.ModRegistries;
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
     */
    private static final GenshinElement[] DEFAULT_PRIORITY_ORDER = {
            ModElements.ANEMO.get(), ModElements.CYRO.get(), ModElements.ELECTRO.get(),
            ModElements.HYDRO.get(), ModElements.FROZEN.get(), ModElements.PYRO.get(),
            ModElements.DENDRO.get(), ModElements.AGGRAVATE.get(), ModElements.GEO.get()
    };

    public static int getDefaultPriority(GenshinElement mainElement) {
        for (int i = 0; i < DEFAULT_PRIORITY_ORDER.length; i++) {
            if (DEFAULT_PRIORITY_ORDER[i] == mainElement) return i;
        }
        return 99;
    }

    public static boolean canElementReact(GenshinElement attackerElement,
                                          StatusContainer container) {
        GenshinElement attackerMain = attackerElement.getMainElement();
        if (attackerMain == null) return false;

        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (!(inst instanceof ElementalAttachmentInstance ea)) continue;
            if (ea.getElement().isInstant()) continue;
            if (ea.getElement() == ModElements.FYSIKOS.get()) continue;

            GenshinElement defenderMain = ea.getElement().getMainElement();
            if (defenderMain == null) continue;

            for (ElementalReaction reaction : ModRegistries.ELEMENTAL_REACTIONS_REGISTRY) {
                if (reaction.canMatch(attackerMain, defenderMain)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static ReactionResult tryReactAfterAttach(ReactionContext context) {
        float remainingAttackerQty = context.attackerUnit();

        List<ElementalAttachmentInstance> defenders = collectDefenders(context);

        if (defenders.isEmpty()) {
            return ReactionResult.builder(null).build();
        }

        List<Candidate> candidates = new ArrayList<>();
        for (ElementalAttachmentInstance defender : defenders) {
            GenshinElement defenderMain = defender.getElement().getMainElement();
            GenshinElement attackerMain = context.attackerElement().getMainElement();

            for (ElementalReaction reaction : ModRegistries.ELEMENTAL_REACTIONS_REGISTRY) {
                if (!reaction.canMatch(attackerMain, defenderMain)) continue;

                boolean blocked = reaction.isBlocked(context);
                if (blocked) continue;

                int priority = reaction.getBasePriority();
                if (priority < 0) {
                    priority = ReactionPriorityCalculator.computeFor(
                            context.attackerElement(), defender.getElement(), reaction);
                }

                candidates.add(new Candidate(reaction, defender, priority));
            }
        }

        candidates.sort(Comparator.comparingInt(c -> c.priority));

        ReactionResult firstAmplified = null;
        boolean anyReactionOccurred = false;

        for (Candidate cand : candidates) {
            if (remainingAttackerQty <= 0f) break;
            if (cand.defender.isFinished()) continue;

            ReactionContext roundContext = new ReactionContext(
                    context.attackerElement(),
                    remainingAttackerQty,
                    context.attackerSource(),
                    context.attackerProfile(),
                    context.damageSpec(),
                    context.attackerEntity(),
                    context.targetContainer(),
                    context.targetEntity()
            );

            ReactionResult result = cand.reaction.execute(roundContext);

            if (result.isReacted()) {
                anyReactionOccurred = true;
                remainingAttackerQty -= result.getConsumedAttacker();
                if (firstAmplified == null && result.isAmplified()) {
                    firstAmplified = result;
                }
            }
        }

        if (anyReactionOccurred) {
            applyAttackerResidual(context, remainingAttackerQty);
        }

        return firstAmplified != null ? firstAmplified :
                ReactionResult.builder(null).build();
    }

    private static List<ElementalAttachmentInstance> collectDefenders(ReactionContext context) {
        List<ElementalAttachmentInstance> result = new ArrayList<>();
        GenshinElement attackerMain = context.attackerElement().getMainElement();

        for (StatusInstance inst : context.targetContainer().getAll()) {
            if (inst.isFinished()) continue;
            if (!(inst instanceof ElementalAttachmentInstance ea)) continue;

            GenshinElement defMain = ea.getElement().getMainElement();
            if (defMain == attackerMain) continue;
            if (ea.getElement() == ModElements.FYSIKOS.get()) continue;
            if (ea.getElement().isInstant()) continue;

            result.add(ea);
        }
        return result;
    }

    private static void applyAttackerResidual(ReactionContext context, float remainingQty) {
        if (remainingQty <= 0f) return;

        if (context.attackerFollowsNoResidualRule()) {
            ElementalAttachmentHelper.consume(
                    context.targetContainer(), context.attackerElement(), Float.MAX_VALUE);
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