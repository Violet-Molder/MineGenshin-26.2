package com.linweiyun.genshin.core.system.about;

import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.status.StatusInstance;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

public class ElementalAttachmentInstance extends StatusInstance {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TYPE_ID = "elemental_attachment";

    @Persisted(key = "element_id")
    private String elementId;

    private transient GenshinElement element;

    @Persisted(key = "source")
    private AttachmentSource source;

    @Persisted(key = "profile")
    private AttachmentProfile profile;

    @Persisted(key = "unit")
    private float unit;

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
    private transient LivingEntity owner;

    public ElementalAttachmentInstance(GenshinElement element, AttachmentSource source,
                                       AttachmentProfile profile, float initialUnit) {
        this.typeId = TYPE_ID;
        this.element = element;
        this.elementId = resolveElementId(element);
        this.source = source;
        this.profile = profile;
        this.unit = initialUnit;
        this.currentDecayPerSecond = profile.getDecayPerSecond();
        this.permanent = profile.isPermanent();
        this.replenishTick = 200;
        this.replenishAmount = 1.0f;
        this.replenishTimer = replenishTick;
    }

    public ElementalAttachmentInstance() {
        this.typeId = TYPE_ID;
        this.elementId = "minegenshin:fysikos";
        this.source = AttachmentSource.SPECIAL;
        this.profile = new AttachmentProfile(0f, 0f, 0f, 0f);
    }

    @Override
    public void tick() {
        if (permanent) {
            replenishTimer--;
            if (replenishTimer <= 0) {
                unit = Math.min(unit + replenishAmount, profile.getBaseQuantity());
                replenishTimer = replenishTick;
            }
            return;
        }
        float effectiveDecayPerSecond;
        GenshinElement e = getElement();
        if (e == ModElements.FROZEN.get()
                && container != null
                && container.getFrozenDecayState() != null) {
            effectiveDecayPerSecond = container.getFrozenDecayState().getCurrentDecayRate();
        } else {
            effectiveDecayPerSecond = currentDecayPerSecond;
        }
        float decayPerTick = effectiveDecayPerSecond / 20f;

        unit = Math.max(0f, unit - decayPerTick);
    }

    @Override
    public boolean isFinished() {
        return unit <= 0f;
    }

    @Override
    public StatusInstance copy() {
        ElementalAttachmentInstance c = new ElementalAttachmentInstance();
        c.element = this.element;
        c.elementId = this.elementId;
        c.source = this.source;
        c.profile = this.profile;
        c.unit = this.unit;
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

    public void setOwner(LivingEntity owner) {
        this.owner = owner;
    }

    @Override
    public void onRemove() {
        if (element != null && owner != null && GenshinElement.isNonPlayerLiving(owner)) {
            element.onDetach(owner);
        }
    }

    // ========== 覆盖规则 & 消耗 ==========

    /** 覆盖规则：量多则覆盖，设置新 quantity（衰减速率是否替换由 Helper 决定） */
    public void refreshQuantity(float newQuantity) {
        this.unit = newQuantity;
    }

    /** 覆盖规则：火/激/燃 覆盖时直接替换衰减速率 */
    public void overrideDecayRate(float newRate) {
        this.currentDecayPerSecond = newRate;
    }

    /** 消耗（元素反应调用），返回实际消耗量 */
    public float consume(float amount) {
        float actual = Math.min(amount, unit);
        unit -= actual;
        return actual;
    }

    // ========== Getter ==========

    public GenshinElement getElement() {
        if (element == null && elementId != null && !elementId.isEmpty()) {
            String[] parts = elementId.split(":", 2);
            Identifier id = Identifier.fromNamespaceAndPath(parts[0], parts[1]);
            element = com.linweiyun.genshin.core.system.registry.ModRegistries.ELEMENT_REGISTRY.get(id).map(r -> r.value()).orElse(null);
        }
        return element;
    }
    public AttachmentSource getSource() { return source; }
    public AttachmentProfile getProfile() { return profile; }
    public float getUnit() { return unit; }
    public float getCurrentDecayPerSecond() { return currentDecayPerSecond; }
    public boolean isPermanent() { return permanent; }
    public LivingEntity getOwner() { return owner; }

    private static String resolveElementId(GenshinElement element) {
        Identifier key = com.linweiyun.genshin.core.system.registry.ModRegistries.ELEMENT_REGISTRY.getKey(element);
        return key != null ? key.toString() : "minegenshin:fysikos";
    }
}