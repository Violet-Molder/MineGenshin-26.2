package com.linweiyun.genshin.core.attribute;

import com.linweiyun.genshin.registry.ModRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModAttributes {
    public static final DeferredRegister<AttributeType> ATTRIBUTES = ModRegistries.ATTRIBUTE_TYPES;

    public static final DeferredHolder<AttributeType, AttributeType> MAX_HP =
            ATTRIBUTES.register("max_hp", () -> new AttributeType("max_hp", "attribute.minegenshin.max_hp", 0));

    public static final DeferredHolder<AttributeType, AttributeType> ATK =
            ATTRIBUTES.register("atk", () -> new AttributeType("atk", "attribute.minegenshin.atk", 0));

    public static final DeferredHolder<AttributeType, AttributeType> DEF =
            ATTRIBUTES.register("def", () -> new AttributeType("def", "attribute.minegenshin.def", 0));
    public static void register(IEventBus bus) {
        ATTRIBUTES.register(bus);
    }
}
