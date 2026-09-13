package com.linweiyun.genshin.core.element;

import com.linweiyun.genshin.core.system.registry.ModRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 元素注册中心 —— 使用 DeferredRegister 将所有元素注册到 Minecraft Registry
 */
public class ModElements {

    public static final DeferredRegister<GenshinElement> ELEMENTS = ModRegistries.ELEMENTS;

    // ======== 主元素 ========
    public static final DeferredHolder<GenshinElement, GenshinElement> FYSIKOS = ELEMENTS.register(
            "fysikos", () -> new GenshinElement(false, false, "elemental.gim.fysikos"));

    public static final DeferredHolder<GenshinElement, GenshinElement> PYRO = ELEMENTS.register(
            "pyro", () -> new GenshinElement(true, false, "elemental.gim.pyro"));

    public static final DeferredHolder<GenshinElement, GenshinElement> HYDRO = ELEMENTS.register(
            "hydro", () -> new GenshinElement(false, false, "elemental.gim.hydro"));

    public static final DeferredHolder<GenshinElement, GenshinElement> ANEMO = ELEMENTS.register(
            "anemo", () -> new GenshinElement(false, true, "elemental.gim.anemo"));

    public static final DeferredHolder<GenshinElement, GenshinElement> ELECTRO = ELEMENTS.register(
            "electro", () -> new GenshinElement(false, false, "elemental.gim.electro"));

    public static final DeferredHolder<GenshinElement, GenshinElement> DENDRO = ELEMENTS.register(
            "dendro", () -> new GenshinElement(false, false, "elemental.gim.dendro"));

    public static final DeferredHolder<GenshinElement, CryoElement> CYRO = ELEMENTS.register(
            "cyro", () -> new CryoElement("elemental.gim.cyro"));

    public static final DeferredHolder<GenshinElement, GenshinElement> GEO = ELEMENTS.register(
            "geo", () -> new GenshinElement(false, true, "elemental.gim.geo"));

    // ======== 类元素（关联主元素，注册后调用 setupSubElements 设置） ========
    public static final DeferredHolder<GenshinElement, FrozenElement> FROZEN = ELEMENTS.register(
            "frozen", () -> new FrozenElement("elemental.gim.frozen"));

    public static final DeferredHolder<GenshinElement, GenshinElement> AGGRAVATE = ELEMENTS.register(
            "aggravate", () -> new GenshinElement(true, false, "elemental.gim.aggravate"));

    public static final DeferredHolder<GenshinElement, GenshinElement> BURNING = ELEMENTS.register(
            "burning", () -> new GenshinElement(true, false, "elemental.gim.burning"));

    public static final DeferredHolder<GenshinElement, GenshinElement> WOOD = ELEMENTS.register(
            "wood", () -> new GenshinElement(false, false, "elemental.gim.wood"));

    /**
     * 在所有元素注册完成后调用，设置类元素的主元素关联
     */
    public static void setupSubElements() {
        FROZEN.get().setMainElement(CYRO.get());
        AGGRAVATE.get().setMainElement(ELECTRO.get());
        BURNING.get().setMainElement(PYRO.get());
        WOOD.get().setMainElement(DENDRO.get());
    }

    public static void register(IEventBus eventBus) {
        ELEMENTS.register(eventBus);
    }
}