package com.linweiyun.genshin.core.system.reaction;

import com.linweiyun.genshin.core.status.StatusInstance;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.enums.ElementalsGIM;

/**
 * 优先级计算器 —— 处理默认序列 + 例外因素
 *
 * 例外因素（全部在这里集中处理）：
 * ① 目标处在冻结状态下时，碎冰的优先级最高（需检查是否有 SHATTER 反应）
 * ② 蔓激化/超激化优先于各不共存反应，感电/燃烧滞后于各不共存反应
 * ③ 冻结状态下或存在冻标记的目标禁止发生感电/蒸发
 * ④ 冰与冻共同消耗（同时消耗，不是顺序）、草与激共同消耗
 */
public class ReactionPriorityCalculator {

    /** 默认优先级序列（数字越小越优先） */
    private static final ElementalsGIM[] DEFAULT_ORDER = {
            ElementalsGIM.ANEMO, ElementalsGIM.CYRO, ElementalsGIM.ELECTRO,
            ElementalsGIM.HYDRO, ElementalsGIM.FROZEN, ElementalsGIM.PYRO,
            ElementalsGIM.DENDRO, ElementalsGIM.AGGRAVATE, ElementalsGIM.GEO
    };

    /** 给定后手元素 + 先手元素 + 反应，计算实际优先级 */
    public static int computeFor(ElementalsGIM attackerElement,
                                 ElementalsGIM defenderElement,
                                 ElementalReaction reaction) {
        ElementalsGIM defMain = defenderElement.getMainElement();
        int baseIdx = indexOf(defMain);
        if (baseIdx < 0) baseIdx = 50;

        // TODO: 例外因素的判断需要 ReactionContext（目标容器、冻结状态等）
        // 目前只做默认序列，后续逐步添加例外
        // ① 冻结状态下碎冰最高（需要检查容器是否有 FROZEN）
        // ② 蔓激化/超激化优先（reaction 类型判断）
        // ③ 冻结藏水/藏冰的特殊处理
        // ④ 冰+冻 同时消耗（需要在消耗逻辑里特殊处理，不在优先级里）

        return baseIdx;
    }

    private static int indexOf(ElementalsGIM mainElement) {
        for (int i = 0; i < DEFAULT_ORDER.length; i++) {
            if (DEFAULT_ORDER[i] == mainElement) return i;
        }
        return -1;
    }

    /** 工具：容器里是否有未完成的 FROZEN 实例 */
    public static boolean hasFrozen(StatusContainer container) {
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (inst instanceof ElementalAttachmentInstance ea
                    && ea.getElement() == ElementalsGIM.FROZEN) return true;
        }
        return false;
    }

    /** 工具：容器里是否有未完成的 AGGRAVATE 实例 */
    public static boolean hasAggravate(StatusContainer container) {
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (inst instanceof ElementalAttachmentInstance ea
                    && ea.getElement() == ElementalsGIM.AGGRAVATE) return true;
        }
        return false;
    }
}
