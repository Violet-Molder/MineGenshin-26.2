package com.linweiyun.genshin.core.element;

import com.linweiyun.genshin.core.system.about.host.ElementalHost;
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
 *   - effectCarrier: 是否只是「附加效果载体」（寒=true：不参与反应配对、不占 HUD 图标）
 *   - mainElement: 类元素关联的主元素（null=主元素，非null=类元素）
 *   - translationKey: 本地化键
 *
 * 钩子（子类可覆盖）：
 *   - onAttach: 元素附着到宿主身上时调用（宿主可能是生物、方块坐标、出战角色）
 *   - onDetach: 元素从宿主身上移除时调用
 * 注意：这是元素自身的特性，不是"附着效果"。后续可能有其他时机触发的钩子。
 */
public class GenshinElement {

    private GenshinElement mainElement;

    private final boolean canOverrideDecay;
    private final boolean instant;
    private final boolean effectCarrier;
    private final String translationKey;

    protected GenshinElement(boolean canOverrideDecay, boolean instant, String translationKey) {
        this(canOverrideDecay, instant, false, translationKey);
    }

    protected GenshinElement(boolean canOverrideDecay, boolean instant,
                             boolean effectCarrier, String translationKey) {
        this.canOverrideDecay = canOverrideDecay;
        this.instant = instant;
        this.effectCarrier = effectCarrier;
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

    /**
     * 是不是「附加效果载体」——寒这样只为承载附加效果而存在的元素。
     *
     * <p>它不参与反应配对（注册成独立元素，没有任何反应以它为配对方），
     * 也不该在 HUD 上占一个图标（没有 `cold.png`，显示出来就是坏图）。
     * 效果本体的位置见 {@link ColdElement}。
     */
    public boolean isEffectCarrier() {
        return effectCarrier;
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
     * 元素附着到宿主身上时调用（默认空实现）。
     *
     * <p>宿主是抽象：生物取 {@link ElementalHost#entity()}，方块取
     * {@link ElementalHost#level()} / {@link ElementalHost#blockPos()}。
     * 只对生物生效的元素效果（减速、禁 AI）应当先判 {@code host.entity() != null}。
     */
    public void onAttach(ElementalHost host) {
    }

    /**
     * 元素从宿主身上移除时调用（默认空实现）
     */
    public void onDetach(ElementalHost host) {
    }

    public static boolean isNonPlayerLiving(LivingEntity entity) {
        return entity != null && !(entity instanceof Player);
    }
}
