package com.linweiyun.genshin.core.element;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 冰元素 —— 附着在非玩家生物身上时降低10%移动速度
 */
public class CryoElement extends GenshinElement {

    private static final Identifier SLOW_MODIFIER_ID =
            Identifier.fromNamespaceAndPath("minegenshin", "cryo_slow");

    protected CryoElement(String translationKey) {
        super(false, false, translationKey);
    }

    @Override
    public void onAttach(LivingEntity entity) {
        if (!isNonPlayerLiving(entity)) return;
        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        if (speed.hasModifier(SLOW_MODIFIER_ID)) return;
        speed.addPermanentModifier(
                new AttributeModifier(
                        SLOW_MODIFIER_ID,
                        -0.10,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                )
        );
    }

    @Override
    public void onDetach(LivingEntity entity) {
        if (!isNonPlayerLiving(entity)) return;
        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SLOW_MODIFIER_ID);
        }
    }
}