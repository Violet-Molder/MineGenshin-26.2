package com.linweiyun.genshin.content.items.weapon;

import com.linweiyun.genshin.Minegenshin;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

public abstract class WeaponItemGenshin extends Item {
  public WeaponItemGenshin(Properties properties) {
    super(properties.component(DataComponents.TOOL, createToolProperties()));
  }

  public static final ResourceLocation BASE_ATTACK_KNOCKBACK_ID =
      ResourceLocation.fromNamespaceAndPath(Minegenshin.MOD_ID, "base_attack_knockback");
  public static final ResourceLocation BASE_ENTITY_INTERACTION_RANGE =
      ResourceLocation.fromNamespaceAndPath(Minegenshin.MOD_ID, "base_entity_interaction_range");

  public static Tool createToolProperties() {
    return new Tool(
        List.of(
            Tool.Rule.minesAndDrops(List.of(Blocks.COBWEB), 15.0F),
            Tool.Rule.overrideSpeed(BlockTags.SWORD_EFFICIENT, 1.5F)),
        1.0F,
        2);
  }

  public static ItemAttributeModifiers createAttributes(float attackDamage, float attackSpeed) {
    return ItemAttributeModifiers.builder()
        .add(
            Attributes.ATTACK_DAMAGE,
            new AttributeModifier(
                BASE_ATTACK_DAMAGE_ID,
                (double) attackDamage,
                AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND)
        .add(
            Attributes.ATTACK_SPEED,
            new AttributeModifier(
                BASE_ATTACK_SPEED_ID, (double) attackSpeed, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND)
        .add(
            Attributes.ATTACK_KNOCKBACK,
            new AttributeModifier(
                BASE_ATTACK_KNOCKBACK_ID, 2.0D, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND)
        .add(
            Attributes.ENTITY_INTERACTION_RANGE,
            new AttributeModifier(
                BASE_ENTITY_INTERACTION_RANGE, 2.0D, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND)
        .build();
  }

  @Override
  public boolean hurtEnemy(
      @NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
    return super.hurtEnemy(stack, target, attacker);
  }
}
