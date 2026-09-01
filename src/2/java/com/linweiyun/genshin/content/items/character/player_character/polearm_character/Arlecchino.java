package com.linweiyun.genshin.content.items.character.player_character.polearm_character;

import com.linweiyun.genshin.Config;
import com.linweiyun.genshin.content.effects.itemstacks.ItemStackEffectHelper;
import com.linweiyun.genshin.content.effects.itemstacks.instance.ItemStackEffectInstance;
import com.linweiyun.genshin.content.effects.itemstacks.registries.ItemStackEffects;
import com.linweiyun.genshin.content.entities.attachments.AttachmentRegistration;
import com.linweiyun.genshin.content.entities.attachments.attachment.CharacterParty;
import com.linweiyun.genshin.content.items.character.data.PlayerCharacterData;
import com.linweiyun.genshin.content.items.components.DataComponentCharacter;
import com.linweiyun.genshin.content.items.components.DataComponentRegistryCharacter;
import com.linweiyun.genshin.core.system.combat.CombatHelper;
import com.linweiyun.genshin.enums.CharacterAscendAttribute;
import com.linweiyun.genshin.enums.ElementalsGIM;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class Arlecchino extends PolearmCharacter {
  public Arlecchino(Properties properties) {
    super(
        properties.component(
            DataComponentRegistryCharacter.CHARACTER_DATA.value(),
            new DataComponentCharacter(new PlayerCharacterData(1020, 27, 60))));
    starRating = 5;
    characterUUID = 135002;
    maxObtainingEnergy = 80;
    name = Component.translatable("character.name.arlecchino");
    CHARACTER_ATK = Config.SHENHE_ATK;
    CHARACTER_DEF = Config.SHENHE_DEF;
    CHARACTER_HP = Config.SHENHE_HP;
    this.ASCEND_ATTRIBUTE = CharacterAscendAttribute.ATK;
    this.ELEMENTAL = ElementalsGIM.PYRO;
    this.SKILL_MAX_COOLDOWN_TICK = 20 * 20;
    this.BURST_MAX_COOLDOWN_TICK = 20 * 20;
  }

  @Override
  public @NotNull InteractionResultHolder<ItemStack> use(
      @NotNull Level level, @NotNull Player player, @NotNull InteractionHand usedHand) {

    return super.use(level, player, usedHand);
  }

  @Override
  public void elementalSkill(Player player) {
    CharacterParty characterParty =
        player.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
    ItemStack character = characterParty.getStackByCharacter(this);
    PlayerCharacterData currentData = CombatHelper.getCharacterData(character);
    Level level = player.level();
    if (!level.isClientSide()) {
      for (int i = 0; i < characterParty.getSlots(); i++) {
        ItemStack stackInSlot = characterParty.getStackInSlot(i);
        if (!stackInSlot.isEmpty()) {
          ItemStackEffectInstance instance =
              new ItemStackEffectInstance(
                  ItemStackEffects.REGENERATION_EFFECT.get(), 200, 1, false);
          System.out.println(stackInSlot);
          ItemStackEffectHelper.addEffect(stackInSlot, player, instance);
        }
      }
    }
  }

  @Override
  public void elementalBurst(Player player) {
    Level level = player.level();
    CharacterParty characterParty =
        player.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
    ItemStack stack = characterParty.getStackByCharacter(this);
    PlayerCharacterData currentData = CombatHelper.getCharacterData(stack);
    if (!level.isClientSide) {
      if (currentData != null) {

        currentData.changeData(stack, data -> data.setCurrentObtainingEnergy(0), player);
      }
    }
  }
}
