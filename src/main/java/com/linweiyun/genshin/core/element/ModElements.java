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

    /** 冰 —— 只负责附着本身；「寒冷」减速由 {@link ColdElement}（寒）承载。 */
    public static final DeferredHolder<GenshinElement, GenshinElement> CYRO = ELEMENTS.register(
            "cyro", () -> new GenshinElement(false, false, "elemental.gim.cyro"));

    public static final DeferredHolder<GenshinElement, GenshinElement> GEO = ELEMENTS.register(
            "geo", () -> new GenshinElement(false, true, "elemental.gim.geo"));

    // ======== 类元素（关联主元素，注册后调用 setupSubElements 设置） ========
    /** 冻 —— 冻结反应生成物，只负责附着本身；「禁 AI」由 {@link ColdElement}（寒）承载。 */
    public static final DeferredHolder<GenshinElement, GenshinElement> FROZEN = ELEMENTS.register(
            "frozen", () -> new GenshinElement(false, false, "elemental.gim.frozen"));

    /**
     * <b>寒</b> —— 冰/冻的附加效果载体（减速、禁 AI）。
     *
     * <p>刻意<b>不</b>做成「mainElement 归并到冰」的类元素：这个代码库里 mainElement 的唯一含义是
     * 「参与反应配对时并入主元素」，归并进去寒就会被当成冰消耗/扩散（要四处写例外）。
     * 独立注册后没有任何反应以寒为配对方，天然不参与反应。
     * 伴随关系（有冰/冻就有寒）由 {@code ColdAura} 每 tick 同步。
     */
    public static final DeferredHolder<GenshinElement, ColdElement> COLD = ELEMENTS.register(
            "cold", () -> new ColdElement("elemental.gim.cold"));

    public static final DeferredHolder<GenshinElement, GenshinElement> AGGRAVATE = ELEMENTS.register(
            "aggravate", () -> new GenshinElement(true, false, "elemental.gim.aggravate"));

    public static final DeferredHolder<GenshinElement, GenshinElement> BURNING = ELEMENTS.register(
            "burning", () -> new GenshinElement(true, false, "elemental.gim.burning"));

    public static final DeferredHolder<GenshinElement, GenshinElement> WOOD = ELEMENTS.register(
            "wood", () -> new GenshinElement(false, false, "elemental.gim.wood"));

    /**
     * 在所有元素注册完成后调用，设置类元素的主元素关联
     *
     * <p>注意这里<b>没有</b>寒：寒是独立元素（效果载体），不并入冰参与反应配对 ——
     * 理由见 {@link #COLD} 的注释。
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
