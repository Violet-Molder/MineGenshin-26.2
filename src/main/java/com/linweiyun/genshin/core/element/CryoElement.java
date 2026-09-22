package com.linweiyun.genshin.core.element;

import com.linweiyun.genshin.content.entities.teyvat.ElementalCreature;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 冰元素 —— 附着在非玩家生物身上时降低10%移动速度（「寒冷」）。
 *
 * <p>例外：{@link ElementalCreature}（元素生物）免疫自己主元素的「寒冷」，
 * 所以冰史莱姆不会被自己挂的冰附着拖慢。免疫判定走
 * {@link ElementalCreature#isImmuneTo}，和伤害免疫用的是同一套规则。
 */
public class CryoElement extends GenshinElement {

    private static final Identifier SLOW_MODIFIER_ID =
            Identifier.fromNamespaceAndPath("minegenshin", "cryo_slow");

    /** 元素生物免疫冰元素时，连带免疫「寒冷」减速。 */
    private static boolean isColdImmune(LivingEntity entity) {
        return entity instanceof ElementalCreature creature
                && creature.isImmuneTo((GenshinElement) ModElements.CYRO.get());
    }

    protected CryoElement(String translationKey) {
        super(false, false, translationKey);
    }

    @Override
    public void onAttach(LivingEntity entity) {
        if (!isNonPlayerLiving(entity) || isColdImmune(entity)) return;
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
        if (!isNonPlayerLiving(entity) || isColdImmune(entity)) return;
        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SLOW_MODIFIER_ID);
        }
    }
}