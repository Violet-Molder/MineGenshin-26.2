package com.linweiyun.genshin.content.items.character.player_character.catalyst;

import com.linweiyun.genshin.Config;
import com.linweiyun.genshin.content.effects.itemstacks.ItemStackEffectHelper;
import com.linweiyun.genshin.content.effects.itemstacks.instance.ItemStackEffectInstance;
import com.linweiyun.genshin.content.effects.itemstacks.registries.ItemStackEffects;
import com.linweiyun.genshin.content.entities.attachments.AttachmentRegistration;
import com.linweiyun.genshin.content.entities.attachments.attachment.CharacterParty;
import com.linweiyun.genshin.content.items.character.data.PlayerCharacterData;
import com.linweiyun.genshin.content.items.components.DataComponentCharacter;
import com.linweiyun.genshin.content.items.components.DataComponentRegistryCharacter;
import com.linweiyun.genshin.enums.CharacterAscendAttribute;
import com.linweiyun.genshin.enums.ElementalsGIM;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class Columbina extends CatalystCharacter {
  public Columbina(Properties properties) {
    super(
        properties.component(
            DataComponentRegistryCharacter.CHARACTER_DATA.get(),
            new DataComponentCharacter(new PlayerCharacterData(1144, 7, 40))));
    starRating = 5;
    characterUUID = 145001;
    maxObtainingEnergy = 80;
    name = Component.translatable("character.name.columbina");
    CHARACTER_ATK = Config.SHENHE_ATK;
    CHARACTER_DEF = Config.SHENHE_DEF;
    CHARACTER_HP = Config.SHENHE_HP;
    this.ASCEND_ATTRIBUTE = CharacterAscendAttribute.ATK;
    this.SKILL_MAX_COOLDOWN_TICK = 17 * 20;
    this.BURST_MAX_COOLDOWN_TICK = 20 * 20;
    this.ELEMENTAL = ElementalsGIM.HYDRO;
  }

  @Override
  public void elementalSkill(Player player) {
    CharacterParty characterParty =
        player.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
    ItemStack currentCharacter = characterParty.getCurrentCharacter();

    if (currentCharacter.getItem() instanceof Columbina) {
      ItemStackEffectInstance instance =
          new ItemStackEffectInstance(ItemStackEffects.GRAVITY_RIPPLE_EFFECT.get(), 500, 1, false);
      ItemStackEffectHelper.addEffect(currentCharacter, player, instance);
    }
  }

  @Override
  public void elementalBurst(Player player) {}

  public ElementalsGIM getElement() {
    return super.getElement();
  }
}
