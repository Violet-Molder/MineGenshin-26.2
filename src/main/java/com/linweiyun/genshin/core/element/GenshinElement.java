package com.linweiyun.genshin.core.element;

import com.linweiyun.genshin.core.system.registry.ModRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 元素基类 —— 所有元素的统一抽象，通过 Minecraft Registry 注册
 *
 * 属性：
 *   - canOverrideDecay: 覆盖同种元素附着时是否替换衰减速率（火/激/燃=true）
 *   - instant: 是否为瞬时附着（风/岩=true，附着后立即消失）
 *   - mainElement: 类元素关联的主元素（null=主元素，非null=类元素）
 *   - translationKey: 本地化键
 *
 * 钩子（子类可覆盖）：
 *   - onAttach: 元素附着到生物身上时调用
 *   - onDetach: 元素从生物身上移除时调用
 * 注意：这是元素自身的特性，不是"附着效果"。后续可能有其他时机触发的钩子。
 */
public class GenshinElement {

    private GenshinElement mainElement;

    private final boolean canOverrideDecay;
    private final boolean instant;
    private final String translationKey;

    protected GenshinElement(boolean canOverrideDecay, boolean instant, String translationKey) {
        this.canOverrideDecay = canOverrideDecay;
        this.instant = instant;
        this.translationKey = translationKey;
        this.mainElement = null;
    }

    void setMainElement(GenshinElement mainElement) {
        this.mainElement = mainElement;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public String getId() {
        Identifier key = ModRegistries.ELEMENT_REGISTRY.getKey(this);
        return key != null ? key.getPath() : "";
    }

    public boolean canOverrideDecay() {
        return canOverrideDecay;
    }

    public boolean isInstant() {
        return instant;
    }

    public GenshinElement getMainElement() {
        return mainElement != null ? mainElement : this;
    }

    public boolean isMainElement() {
        return mainElement == null;
    }

    public boolean isSubElement() {
        return mainElement != null;
    }

    /**
     * 元素附着到生物身上时调用（默认空实现）
     */
    public void onAttach(LivingEntity entity) {
    }

    /**
     * 元素从生物身上移除时调用（默认空实现）
     */
    public void onDetach(LivingEntity entity) {
    }

    public static boolean isNonPlayerLiving(LivingEntity entity) {
        return entity != null && !(entity instanceof Player);
    }
}