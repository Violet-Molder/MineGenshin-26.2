package com.linweiyun.genshin.core.system.registry.register;

import com.linweiyun.genshin.core.system.reaction.ElementalReaction;
import com.linweiyun.genshin.core.system.reaction.builtin.ElectroChargedReaction;
import com.linweiyun.genshin.core.system.reaction.builtin.FreezeReaction;
import com.linweiyun.genshin.core.system.reaction.builtin.LunarChargedReaction;
import com.linweiyun.genshin.core.system.reaction.builtin.MeltReaction;
import com.linweiyun.genshin.core.system.reaction.builtin.VaporizeReaction;
import com.linweiyun.genshin.core.system.registry.ModRegistries;
import com.linweiyun.genshin.enums.ElementalReactionType;
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
                    "minegenshin:hydro", "minegenshin:pyro",
                    1f, 2f,
                    0));

    // 融化：火:冰 = 1:2，火克冰（冻通过 getMainElement 归并到 CYRO）
    public static final DeferredHolder<ElementalReaction, MeltReaction> MELT = ELEMENTAL_REACTIONS.register(
            "melt",
            () -> new MeltReaction(
                    ElementalReactionType.MELT,
                    "minegenshin:pyro", "minegenshin:cyro",
                    1f, 2f,
                    0));

    // 冻结：水:冰 = 1:1
    public static final DeferredHolder<ElementalReaction, FreezeReaction> FREEZE = ELEMENTAL_REACTIONS.register(
            "freeze",
            () -> new FreezeReaction(
                    ElementalReactionType.FROZEN,
                    "minegenshin:hydro", "minegenshin:cyro",
                    1f, 1f,
                    0));

    // 月感电：水:雷 = 1:1，拦截感电反应（需要哥伦比娅在场，优先级高于普通感电）
    public static final DeferredHolder<ElementalReaction, LunarChargedReaction> LUNAR_CHARGED = ELEMENTAL_REACTIONS.register(
            "lunar_charged",
            () -> new LunarChargedReaction(
                    ElementalReactionType.LUNAR_CHARGED,
                    "minegenshin:hydro", "minegenshin:electro",
                    1f, 1f,
                    -1));

    // 感电：水:雷 = 1:1，共存反应
    public static final DeferredHolder<ElementalReaction, ElectroChargedReaction> ELECTRO_CHARGED = ELEMENTAL_REACTIONS.register(
            "electro_charged",
            () -> new ElectroChargedReaction(
                    ElementalReactionType.ELECTRO_CHARGED,
                    "minegenshin:hydro", "minegenshin:electro",
                    1f, 1f,
                    0));

    public static void register(IEventBus eventBus) {
        ELEMENTAL_REACTIONS.register(eventBus);
    }
}