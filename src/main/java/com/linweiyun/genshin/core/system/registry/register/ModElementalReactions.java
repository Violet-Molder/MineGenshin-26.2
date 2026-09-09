package com.linweiyun.genshin.core.system.registry.register;

import com.linweiyun.genshin.core.system.reaction.ElementalReaction;
import com.linweiyun.genshin.core.system.reaction.builtin.FreezeReaction;
import com.linweiyun.genshin.core.system.reaction.builtin.MeltReaction;
import com.linweiyun.genshin.core.system.reaction.builtin.VaporizeReaction;
import com.linweiyun.genshin.core.system.registry.ModRegistries;
import com.linweiyun.genshin.enums.ElementalReactionType;
import com.linweiyun.genshin.enums.ElementalsGIM;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModElementalReactions {

    public static final DeferredRegister<ElementalReaction> ELEMENTAL_REACTIONS = ModRegistries.ELEMENTAL_REACTIONS;

    // 蒸发：水:火 = 1:2，水克火
    public static final DeferredHolder<ElementalReaction, VaporizeReaction> VAPORIZE = ELEMENTAL_REACTIONS.register(
            "vaporize",
            () -> new VaporizeReaction(
                    ElementalReactionType.VAPORIZE,
                    ElementalsGIM.HYDRO, ElementalsGIM.PYRO,
                    1f, 2f,
                    0));

    // 融化：火:冰 = 1:2，火克冰（冻通过 getMainElement 归并到 CYRO）
    public static final DeferredHolder<ElementalReaction, MeltReaction> MELT = ELEMENTAL_REACTIONS.register(
            "melt",
            () -> new MeltReaction(
                    ElementalReactionType.MELT,
                    ElementalsGIM.PYRO, ElementalsGIM.CYRO,
                    1f, 2f,
                    0));

    // 冻结：水:冰 = 1:1
    public static final DeferredHolder<ElementalReaction, FreezeReaction> FREEZE = ELEMENTAL_REACTIONS.register(
            "freeze",
            () -> new FreezeReaction(
                    ElementalReactionType.FROZEN,
                    ElementalsGIM.HYDRO, ElementalsGIM.CYRO,
                    1f, 1f,
                    0));

    public static void register(IEventBus eventBus) {
        ELEMENTAL_REACTIONS.register(eventBus);
    }
}