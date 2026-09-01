package com.linweiyun.genshin.core.network;

import com.linweiyun.genshin.content.effects.itemstacks.ItemStackEffectList;
import com.linweiyun.genshin.content.entities.attachments.AttachmentRegistration;
import com.linweiyun.genshin.content.entities.attachments.attachment.*;
import com.linweiyun.genshin.content.items.character.data.PlayerCharacterData;
import com.linweiyun.genshin.content.items.components.DataComponentRegistryCharacter;
import com.linweiyun.genshin.core.system.combat.CombatHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ClientHandler {
  public static void primogemClientHandler(long amount) {
    Player player = Minecraft.getInstance().player;
    assert player != null;
    PlayerPrimogemAttachment primogem = player.getData(AttachmentRegistration.PRIMOGEM_ATTACHMENT);
    primogem.changePacketPrimogem(amount);
  }

  public static void characterPartyClientHandler(CompoundTag inventoryData) {
    Player player = Minecraft.getInstance().player;
    assert player != null;
    CharacterParty characterParty =
        player.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
    characterParty.deserializeNBT(player.registryAccess(), inventoryData);
  }

  static void characterSheetClientHandler(CompoundTag inventoryData) {
    Player player = Minecraft.getInstance().player;
    assert player != null;
    CharacterSheet characterSheet =
        player.getData(AttachmentRegistration.CHARACTER_SHEET_ATTACHMENT);
    characterSheet.deserializeNBT(player.registryAccess(), inventoryData);
  }

  static void characterSheetSortMethodClientHandler(CharacterSheet.SortMethod sortMethod) {
    Player clientPlayer = Minecraft.getInstance().player;
    assert clientPlayer != null;
    CharacterSheet characterSheet =
        clientPlayer.getData(AttachmentRegistration.CHARACTER_SHEET_ATTACHMENT);
    characterSheet.setPacketSortMethod(sortMethod);
  }

  static void genshinModeClientHandler(boolean isGenshinMode) {
    Player clientPlayer = Minecraft.getInstance().player;
    assert clientPlayer != null;
    PlayerGenshinModeAttachment modeAttachment =
        clientPlayer.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT.get());
    modeAttachment.setPacketGenshinMode(isGenshinMode);
  }

  static void characterSelectionClientHandler(int characterId) {
    Player clientPlayer = Minecraft.getInstance().player;
    assert clientPlayer != null;
    CharacterParty characterParty =
        clientPlayer.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT.get());
    characterParty.setPacketCurrentCharacter(characterId);
  }

  public static void characterDataClientHandler(CompoundTag inventoryData, int slot) {
    Player player = Minecraft.getInstance().player;
    assert player != null;
    CharacterParty characterParty =
        player.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
    ItemStack stack = characterParty.getStackInSlot(slot);
    PlayerCharacterData characterData = CombatHelper.getCharacterData(stack);
    if (characterData != null) {
      characterData.deserializeNBT(player.registryAccess(), inventoryData);
    }
  }

  public static void itemStackEffectsClientHandler(ListTag effectsData, int slot) {
    Player player = Minecraft.getInstance().player;
    assert player != null;
    CharacterParty characterParty =
        player.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
    ItemStack stack = characterParty.getStackInSlot(slot);
    ItemStackEffectList effectList =
        ItemStackEffectList.CODEC
            .parse(NbtOps.INSTANCE, effectsData)
            .result()
            .orElse(ItemStackEffectList.EMPTY);
    stack.set(DataComponentRegistryCharacter.ITEM_STACK_EFFECTS.get(), effectList);
  }
}
