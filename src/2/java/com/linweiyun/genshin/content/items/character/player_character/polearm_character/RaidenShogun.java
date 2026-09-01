package com.linweiyun.genshin.content.items.character.player_character.polearm_character;

import com.linweiyun.genshin.content.entities.attachments.AttachmentRegistration;
import com.linweiyun.genshin.content.entities.attachments.attachment.CharacterParty;
import com.linweiyun.genshin.content.entities.attachments.attachment.PlayerPrimogemAttachment;
import com.linweiyun.genshin.content.entities.entity.teyvat.spectial.ElementalOrb;
import com.linweiyun.genshin.content.entities.entity.EntityRegister;
import com.linweiyun.genshin.content.items.character.data.PlayerCharacterData;
import com.linweiyun.genshin.content.items.components.DataComponentCharacter;
import com.linweiyun.genshin.content.items.components.DataComponentRegistryCharacter;
import com.linweiyun.genshin.core.system.combat.CombatHelper;
import com.linweiyun.genshin.enums.ElementalsGIM;
import com.linweiyun.tutorial.TutorialScreenOne;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class RaidenShogun extends PolearmCharacter {
  public RaidenShogun(Properties properties) {
    super(
        properties.component(
            DataComponentRegistryCharacter.CHARACTER_DATA.get(),
            new DataComponentCharacter(new PlayerCharacterData(600, 24, 50))));
    starRating = 5;
    characterUUID = 135003;
    name = Component.translatable("character.name.raiden_shogun");
  }

  @Override
  public @NotNull InteractionResultHolder<ItemStack> use(
      Level level, Player player, @NotNull InteractionHand usedHand) {

    CharacterParty characterParty =
        player.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
    ItemStack character = characterParty.getCurrentCharacter();

    PlayerCharacterData currentData = CombatHelper.getCharacterData(character);
    int pri =
        Math.toIntExact(player.getData(AttachmentRegistration.PRIMOGEM_ATTACHMENT).getPrimogem());
    if (!level.isClientSide()) {

      PlayerPrimogemAttachment primogem =
          player.getData(AttachmentRegistration.PRIMOGEM_ATTACHMENT);
      primogem.addPrimogem(6000000L, (ServerPlayer) player);

      BlockPos pos = new BlockPos(player.getBlockX(), player.getBlockY(), player.getBlockZ());
      BlockState blockState = level.getBlockState(pos);
      if (currentData != null) {
        currentData.getCurrentObtainingEnergy();
      }
      ElementalOrb elementalOrb = EntityRegister.ELEMENTAL_ORB.get().create(level);
      assert elementalOrb != null;
      elementalOrb.setPos(player.getX(), player.getY(), player.getZ());
      elementalOrb.setElemental(ElementalsGIM.ELECTRO);
      elementalOrb.setValue(10);
      level.addFreshEntity(elementalOrb);
    } else {
      TutorialScreenOne.openScreen();
    }

    return super.use(level, player, usedHand);
  }

  @Override
  public void elementalSkill(Player player) {
    player.sendSystemMessage(Component.literal("这是雷神的元素战技"));
  }

  @Override
  public void elementalBurst(Player player) {
    Level level = player.level();
    player.sendSystemMessage(Component.literal("这是雷神的元素爆发"));
    CharacterParty characterParty =
        player.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
    ItemStack stack = characterParty.getCurrentCharacter();
    PlayerCharacterData currentData = CombatHelper.getCharacterData(stack);
    if (!level.isClientSide) {
      assert currentData != null;
      currentData.changeData(stack, data -> data.setCurrentObtainingEnergy(0), player);
    }
  }
}
