package com.linweiyun.genshin.content.items.food;

import com.linweiyun.genshin.content.entities.attachments.AttachmentRegistration;
import com.linweiyun.genshin.content.entities.attachments.attachment.CharacterParty;
import com.linweiyun.genshin.content.items.character.player_character.PlayerCharacter;
import com.linweiyun.genshin.content.items.components.DataComponentRegistryCharacter;
import com.linweiyun.genshin.core.system.combat.CombatHelper;
import com.linweiyun.genshin.core.food.CharacterFoodProperties;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class FoodItem extends Item {
  public FoodItem(Properties properties) {
    super(properties);
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    CharacterFoodProperties charFood =
        stack.get(DataComponentRegistryCharacter.CHARACTER_FOOD.get());
    if (charFood != null) {
      player.startUsingItem(hand);
      return InteractionResultHolder.consume(stack);
    }

    return InteractionResultHolder.pass(stack);
  }

  @Override
  public int getUseDuration(ItemStack stack, LivingEntity entity) {
    CharacterFoodProperties charFood =
        stack.get(DataComponentRegistryCharacter.CHARACTER_FOOD.get());
    return charFood != null ? charFood.eatDurationTicks() : 0;
  }

  @Override
  public UseAnim getUseAnimation(ItemStack stack) {
    return UseAnim.EAT;
  }

  @Override
  public @NotNull ItemStack finishUsingItem(
      @NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity livingEntity) {
    if (!(livingEntity instanceof Player player)) return stack;
    CharacterParty party = player.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
    if (!party.getCurrentCharacter().isEmpty()
        && party.getCurrentCharacter().getItem() instanceof PlayerCharacter currentChar) {
      currentChar.eat(player, stack);
    }

    for (ItemStack characterStack : party.getItems()) {
      if (characterStack.isEmpty()) continue;
      var data = CombatHelper.getCharacterData(characterStack);
      if (data != null && data.getCurrentHP() <= 0) {
        float healAmount = (float) (data.getMaxHP().getTotalValue(true) * 0.1f);
        data.changeData(characterStack, playerData -> playerData.healHP(healAmount), player);
      }
    }
    if (!player.isCreative()) {
      stack.shrink(1);
    }

    return stack;
  }
}
