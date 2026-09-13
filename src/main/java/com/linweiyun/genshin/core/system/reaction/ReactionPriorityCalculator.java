package com.linweiyun.genshin.core.system.reaction;

import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.status.StatusInstance;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.genshin.core.attachment.StatusContainer;

public class ReactionPriorityCalculator {

    private static final GenshinElement[] DEFAULT_ORDER = {
            ModElements.ANEMO.get(), ModElements.CYRO.get(), ModElements.ELECTRO.get(),
            ModElements.HYDRO.get(), ModElements.FROZEN.get(), ModElements.PYRO.get(),
            ModElements.DENDRO.get(), ModElements.AGGRAVATE.get(), ModElements.GEO.get()
    };

    public static int computeFor(GenshinElement attackerElement,
                                 GenshinElement defenderElement,
                                 ElementalReaction reaction) {
        GenshinElement defMain = defenderElement.getMainElement();
        int baseIdx = indexOf(defMain);
        if (baseIdx < 0) baseIdx = 50;
        return baseIdx;
    }

    private static int indexOf(GenshinElement mainElement) {
        for (int i = 0; i < DEFAULT_ORDER.length; i++) {
            if (DEFAULT_ORDER[i] == mainElement) return i;
        }
        return -1;
    }

    public static boolean hasFrozen(StatusContainer container) {
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (inst instanceof ElementalAttachmentInstance ea
                    && ea.getElement() == ModElements.FROZEN.get()) return true;
        }
        return false;
    }

    public static boolean hasAggravate(StatusContainer container) {
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (inst instanceof ElementalAttachmentInstance ea
                    && ea.getElement() == ModElements.AGGRAVATE.get()) return true;
        }
        return false;
    }
}