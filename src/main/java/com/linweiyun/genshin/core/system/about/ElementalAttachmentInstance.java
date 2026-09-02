package com.linweiyun.genshin.core.system.about;

import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.status.StatusInstance;
import com.linweiyun.genshin.enums.ElementalsGIM;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.beans.Transient;

public class ElementalAttachmentInstance extends StatusInstance {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TYPE_ID = "elemental_attachment";

    @Persisted(key = "element")
    private ElementalsGIM element;

    @Persisted(key = "source")
    private AttachmentSource source;

    @Persisted(key = "profile")
    private AttachmentProfile profile;

    @Persisted(key = "quantity")
    private float quantity;

    @Persisted(key = "current_decay_per_second")
    private float currentDecayPerSecond;

    @Persisted(key = "permanent")
    private boolean permanent;

    @Persisted(key = "replenish_tick")
    private int replenishTick;

    @Persisted(key = "replenish_amount")
    private float replenishAmount;

    @Persisted(key = "replenish_timer")
    private int replenishTimer;

    private transient StatusContainer container;

    public ElementalAttachmentInstance(ElementalsGIM element, AttachmentSource source,
                                       AttachmentProfile profile, float initialQuantity) {
        this.typeId = TYPE_ID;
        this.element = element;
        this.source = source;
        this.profile = profile;
        this.quantity = initialQuantity;
        this.currentDecayPerSecond = profile.getDecayPerSecond();
        this.permanent = profile.isPermanent();
        this.replenishTick = 200;
        this.replenishAmount = 1.0f;
        this.replenishTimer = replenishTick;
    }

    public ElementalAttachmentInstance() {
        this.typeId = TYPE_ID;
    }

    @Override
    public void tick() {
        if (permanent) {
            replenishTimer--;
            if (replenishTimer <= 0) {
                quantity = Math.min(quantity + replenishAmount, profile.getBaseQuantity());
                replenishTimer = replenishTick;
            }
            return;
        }
        float effectiveDecayPerSecond;
        if (element == ElementalsGIM.FROZEN
                && container != null
                && container.getFrozenDecayState() != null) {
            effectiveDecayPerSecond = container.getFrozenDecayState().getCurrentDecayRate();
        } else {
            effectiveDecayPerSecond = currentDecayPerSecond;
        }
        float decayPerTick = effectiveDecayPerSecond / 20f;

        quantity = Math.max(0f, quantity - decayPerTick);
    }

    @Override
    public boolean isFinished() {
        return quantity <= 0f;
    }

    @Override
    public StatusInstance copy() {
        ElementalAttachmentInstance c = new ElementalAttachmentInstance();
        c.element = this.element;
        c.source = this.source;
        c.profile = this.profile;
        c.quantity = this.quantity;
        c.currentDecayPerSecond = this.currentDecayPerSecond;
        c.permanent = this.permanent;
        c.replenishTick = this.replenishTick;
        c.replenishAmount = this.replenishAmount;
        c.replenishTimer = this.replenishTimer;
        c.container = null;
        return c;
    }
    public void setContainer(StatusContainer container) {
        this.container = container;
    }

    // ========== 覆盖规则 & 消耗 ==========

    /** 覆盖规则：量多则覆盖，设置新 quantity（衰减速率是否替换由 Helper 决定） */
    public void refreshQuantity(float newQuantity) {
        this.quantity = newQuantity;
    }

    /** 覆盖规则：火/激/燃 覆盖时直接替换衰减速率 */
    public void overrideDecayRate(float newRate) {
        this.currentDecayPerSecond = newRate;
    }

    /** 消耗（元素反应调用），返回实际消耗量 */
    public float consume(float amount) {
        float actual = Math.min(amount, quantity);
        quantity -= actual;
        return actual;
    }

    // ========== Getter ==========

    public ElementalsGIM getElement() { return element; }
    public AttachmentSource getSource() { return source; }
    public AttachmentProfile getProfile() { return profile; }
    public float getQuantity() { return quantity; }
    public float getCurrentDecayPerSecond() { return currentDecayPerSecond; }
    public boolean isPermanent() { return permanent; }
}