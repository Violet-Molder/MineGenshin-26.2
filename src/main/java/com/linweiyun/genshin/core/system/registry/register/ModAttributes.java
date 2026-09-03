package com.linweiyun.genshin.core.system.registry.register;

import com.linweiyun.genshin.content.attribute.AttributeType;
import com.linweiyun.genshin.core.system.registry.ModRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModAttributes {
    public static final DeferredRegister<AttributeType> ATTRIBUTES = ModRegistries.ATTRIBUTE_TYPES;

    // Base Stats
    public static final DeferredHolder<AttributeType, AttributeType> MAX_HP =
            ATTRIBUTES.register("max_hp", () -> new AttributeType("max_hp", "attribute.minegenshin.max_hp", 0));
    public static final DeferredHolder<AttributeType, AttributeType> ATK =
            ATTRIBUTES.register("atk", () -> new AttributeType("atk", "attribute.minegenshin.atk", 0));
    public static final DeferredHolder<AttributeType, AttributeType> DEF =
            ATTRIBUTES.register("def", () -> new AttributeType("def", "attribute.minegenshin.def", 0));
    public static final DeferredHolder<AttributeType, AttributeType> ELEMENTAL_MASTERY =
            ATTRIBUTES.register("elemental_mastery", () -> new AttributeType("elemental_mastery", "attribute.minegenshin.elemental_mastery", 0));
    public static final DeferredHolder<AttributeType, AttributeType> MAX_STAMINA =
            ATTRIBUTES.register("max_stamina", () -> new AttributeType("max_stamina", "attribute.minegenshin.max_stamina", 0));

    // Advanced Stats
    public static final DeferredHolder<AttributeType, AttributeType> CR =
            ATTRIBUTES.register("crit_rate", () -> new AttributeType("cr", "attribute.minegenshin.cr", 0.05f));
    public static final DeferredHolder<AttributeType, AttributeType> CDG =
            ATTRIBUTES.register("crit_dmg", () -> new AttributeType("crit_dmg", "attribute.minegenshin.cdg", 0.5f));
    public static final DeferredHolder<AttributeType, AttributeType> HB =
            ATTRIBUTES.register("healing_bonus", () -> new AttributeType("cdg", "attribute.minegenshin.hb", 0));
    public static final DeferredHolder<AttributeType, AttributeType> IHB =
            ATTRIBUTES.register("incoming_healing_bonus", () -> new AttributeType("ihb", "attribute.minegenshin.ihb", 0));
    public static final DeferredHolder<AttributeType, AttributeType> ER =
            ATTRIBUTES.register("energy_recharge", () -> new AttributeType("er", "attribute.minegenshin.er", 1.0f));
    public static final DeferredHolder<AttributeType, AttributeType> CDR =
            ATTRIBUTES.register("cd_reduction", () -> new AttributeType("cdr", "attribute.minegenshin.cdr", 0));
    public static final DeferredHolder<AttributeType, AttributeType> SS =
            ATTRIBUTES.register("shield_strength", () -> new AttributeType("ss", "attribute.minegenshin.ss", 0));

    // Elemental Type
    public static final DeferredHolder<AttributeType, AttributeType> PYRO_DMGB =
            ATTRIBUTES.register("pyro_dmg_bonus", () -> new AttributeType("pyro_dmgb", "attribute.minegenshin.pyro_dmgb", 0));
    public static final DeferredHolder<AttributeType, AttributeType> PYRO_RES =
            ATTRIBUTES.register("pyro_res", () -> new AttributeType("pyro_res", "attribute.minegenshin.pyro_res", 0));
    public static final DeferredHolder<AttributeType, AttributeType> HYDRO_DMGB =
            ATTRIBUTES.register("hydro_dmg_bonus", () -> new AttributeType("hydro_dmgb", "attribute.minegenshin.hydro_dmgb", 0));
    public static final DeferredHolder<AttributeType, AttributeType> HYDRO_RES =
            ATTRIBUTES.register("hydro_res", () -> new AttributeType("hydro_res", "attribute.minegenshin.hydro_res", 0));
    public static final DeferredHolder<AttributeType, AttributeType> DENDRO_DMGB =
            ATTRIBUTES.register("dendro_dmg_bonus", () -> new AttributeType("dendro_dmgb", "attribute.minegenshin.dendro_dmgb", 0));
    public static final DeferredHolder<AttributeType, AttributeType> DENDRO_RES =
            ATTRIBUTES.register("dendro_res", () -> new AttributeType("dendro_res", "attribute.minegenshin.dendro_res", 0));
    public static final DeferredHolder<AttributeType, AttributeType> ELECTRO_DMGB =
            ATTRIBUTES.register("electro_dmg_bonus", () -> new AttributeType("electro_dmgb", "attribute.minegenshin.electro_dmgb", 0));
    public static final DeferredHolder<AttributeType, AttributeType> ELECTRO_RES =
            ATTRIBUTES.register("electro_res", () -> new AttributeType("electro_res", "attribute.minegenshin.electro_res", 0));
    public static final DeferredHolder<AttributeType, AttributeType> ANEMO_DMGB =
            ATTRIBUTES.register("anemo_dmg_bonus", () -> new AttributeType("anemo_dmgb", "attribute.minegenshin.anemo_dmgb", 0));
    public static final DeferredHolder<AttributeType, AttributeType> ANEMO_RES =
            ATTRIBUTES.register("anemo_res", () -> new AttributeType("anemo_res", "attribute.minegenshin.anemo_res", 0));
    public static final DeferredHolder<AttributeType, AttributeType> CYRO_DMGB =
            ATTRIBUTES.register("cyro_dmg_bonus", () -> new AttributeType("cyro_dmgb", "attribute.minegenshin.cyro_dmgb", 0));
    public static final DeferredHolder<AttributeType, AttributeType> CYRO_RES =
            ATTRIBUTES.register("cyro_res", () -> new AttributeType("cyro_res", "attribute.minegenshin.cyro_res", 0));
    public static final DeferredHolder<AttributeType, AttributeType> GEO_DMGB =
            ATTRIBUTES.register("geo_dmg_bonus", () -> new AttributeType("geo_dmgb", "attribute.minegenshin.geo_dmgb", 0));
    public static final DeferredHolder<AttributeType, AttributeType> GEO_RES =
            ATTRIBUTES.register("geo_res", () -> new AttributeType("geo_res", "attribute.minegenshin.geo_res", 0));
    public static final DeferredHolder<AttributeType, AttributeType> PHYSICAL_DMGB =
            ATTRIBUTES.register("physical_dmg_bonus", () -> new AttributeType("physical_dmgb", "attribute.minegenshin.physical_dmgb", 0));
    public static final DeferredHolder<AttributeType, AttributeType> PHYSICAL_RES =
            ATTRIBUTES.register("physical_res", () -> new AttributeType("physical_res", "attribute.minegenshin.physical_res", 0));

    // Special Attributes
    // 生命之契
    public static final DeferredHolder<AttributeType, AttributeType> BOL =
            ATTRIBUTES.register("bond_of_life", () -> new AttributeType("bol", "attribute.minegenshin.bol", 0));

    public static void register(IEventBus bus) {

        ATTRIBUTES.register(bus);
    }
}
