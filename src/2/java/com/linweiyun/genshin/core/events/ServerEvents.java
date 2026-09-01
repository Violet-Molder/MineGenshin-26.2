package com.linweiyun.genshin.core.events;

import static com.linweiyun.genshin.content.entities.attachments.AttachmentRegistration.PRIMOGEM_ATTACHMENT;

import com.linweiyun.genshin.Config;
import com.linweiyun.genshin.content.effects.entities.about_elemental_effect.AboutElementalEffect;
import com.linweiyun.genshin.content.effects.entities.elemental_reaction.FrozenEffect;
import com.linweiyun.genshin.content.effects.entities.instance.MobEffectInstanceAboutElemental;
import com.linweiyun.genshin.content.entities.attachments.AttachmentRegistration;
import com.linweiyun.genshin.content.entities.attachments.attachment.*;
import com.linweiyun.genshin.content.entities.damagesource.ElementalDamageSourceGIM;
import com.linweiyun.genshin.content.entities.entity.teyvat.spectial.ElementalOrb;
import com.linweiyun.genshin.content.entities.entity.EntityRegister;
import com.linweiyun.genshin.content.items.character.data.PlayerCharacterData;
import com.linweiyun.genshin.content.items.character.player_character.PlayerCharacter;
import com.linweiyun.genshin.core.system.combat.CombatHelper;
import com.linweiyun.genshin.core.network.NetworkManager;
import com.linweiyun.genshin.enums.DamageTypeEnum;
import com.linweiyun.genshin.enums.ElementalsGIM;
import com.linweiyun.genshin.mixin_interfaces.IDamageContainerAccessor;
import com.linweiyun.genshin.mixin_interfaces.IEntityNoAIAccessor;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

@EventBusSubscriber
public class ServerEvents {
  @SubscribeEvent
  public static void onConfigReload(ModConfigEvent.Reloading event) {
    if (event.getConfig().getSpec() == Config.CHARACTER_EXP_SPEC) {
      var expList = Config.CHARACTER_UP_EXP.get();
      if (expList.size() != 89) {
        Config.CHARACTER_UP_EXP.set(Config.CHARACTER_UP_EXP.getDefault());
        Config.CHARACTER_EXP_SPEC.save();
      }
    }
  }
  @SubscribeEvent
  public static void onConfigLoad(ModConfigEvent.Loading event) {
    if (event.getConfig().getSpec() == Config.CHARACTER_EXP_SPEC) {
      var expList = Config.CHARACTER_UP_EXP.get();
      if (expList.size() != 89) {
        Config.CHARACTER_UP_EXP.set(Config.CHARACTER_UP_EXP.getDefault());
        Config.CHARACTER_EXP_SPEC.save();
      }
    }
  }
  @SubscribeEvent
  public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer serverPlayer) {
      PlayerPrimogemAttachment primogem = serverPlayer.getData(PRIMOGEM_ATTACHMENT);
      PlayerGenshinModeAttachment genshinMode =
          serverPlayer.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT);
      CharacterParty characterParty =
          serverPlayer.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
      CharacterSheet characterSheet =
          serverPlayer.getData(AttachmentRegistration.CHARACTER_SHEET_ATTACHMENT);

      NetworkManager.setPrimogemToPlayer(serverPlayer, primogem.getPrimogem());
      NetworkManager.setGenshinModeToPlayer(serverPlayer, genshinMode.isGenshinMode());
      NetworkManager.setCharacterSelectionToPlayer(
          serverPlayer, characterParty.getCurrentCharacterSlot());
      NetworkManager.setCharacterPartyToPlayer(
          serverPlayer, characterParty.serializeNBT(serverPlayer.registryAccess()));
      CompoundTag tag = characterSheet.serializeNBT(serverPlayer.registryAccess());
      NetworkManager.setCharacterSheetToPlayer(serverPlayer, tag);
    }
  }

  @SubscribeEvent
  public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    Player player = event.getEntity();
    CharacterParty party = player.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT.get());
  }

  @SubscribeEvent
  public static void onAttackEvent(AttackEntityEvent entityEvent) {
    Player player = entityEvent.getEntity();
    if (entityEvent.getTarget() instanceof LivingEntity target) {
      PlayerGenshinModeAttachment genshinMode =
          player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT);
      CharacterParty characterParty =
          player.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
      if (genshinMode.isGenshinMode()) {
        entityEvent.setCanceled(true);
        CombatHelper.hurt(
            player,
            characterParty.getCurrentCharacter(),
            target,
            DamageTypeEnum.NORMAL_ATTACK,
            1.0f);
      }
    }
  }

  @SubscribeEvent
  public static void onLivingEntityHurt(LivingDamageEvent.Pre event) {
    Entity entity = event.getSource().getEntity();
    LivingEntity target = event.getEntity();
    if (entity instanceof Player player) {
      if (!(event.getSource() instanceof ElementalDamageSourceGIM)) {
        if (event.getSource() instanceof IDamageContainerAccessor damageSource) {
          CharacterParty characterParty =
              player.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
          event.setNewDamage(
              CombatHelper.calculateDamage(
                  player,
                  characterParty.getCurrentCharacter(),
                  target,
                  DamageTypeEnum.NORMAL_ATTACK,
                  8.0f,
                  event.getOriginalDamage()));
          ElementalDamageSourceGIM elementalDamageSourceGIM =
              CombatHelper.calculateDamageSource(player, DamageTypeEnum.NORMAL_ATTACK);
          damageSource.setModifiedSource(elementalDamageSourceGIM);
        }
      }
      player.sendSystemMessage(Component.literal(String.valueOf(event.getNewDamage())));
    } else {
      event.setNewDamage(
          CombatHelper.calculateDamage(
              entity,
              ItemStack.EMPTY,
              target,
              DamageTypeEnum.NORMAL_ATTACK,
              1.0f,
              event.getOriginalDamage()));
    }

  }

  @SubscribeEvent
  public static void onLivingDeath(LivingDeathEvent event) {
    if (!event.getEntity().level().isClientSide) {
      double x = event.getEntity().getX();
      double y = event.getEntity().getY();
      double z = event.getEntity().getZ();

      ElementalOrb elementalOrb =
          EntityRegister.ELEMENTAL_ORB.get().create(event.getEntity().level());
      Objects.requireNonNull(elementalOrb).setPos(x, y + 0.5, z);
      elementalOrb.setElemental(ElementalsGIM.ELECTRO);
      elementalOrb.setValue(event.getEntity().level().random.nextInt(16) + 5);
      event.getEntity().level().addFreshEntity(elementalOrb);

      if (event.getSource().getEntity() instanceof Player player) {
        if (CombatHelper.entityIsGenshinMode(player)) {
          CharacterParty party = player.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
          ItemStack stack = party.getCurrentCharacter();
          if (!party.getCurrentCharacter().isEmpty()
                  && party.getCurrentCharacter().getItem() instanceof PlayerCharacter currentChar) {
            currentChar.addExperience(stack, player, 98000);
          }
        }
      }
    }
  }

  @SubscribeEvent
  public static void onMobEffectAdded(MobEffectEvent.Added event) {
    if (event.getEffectInstance() instanceof MobEffectInstanceAboutElemental instance) {
      LivingEntity entity = event.getEntity();
      DeferredHolder<MobEffect, AboutElementalEffect> effect =
              (DeferredHolder<MobEffect, AboutElementalEffect>) event.getEffectInstance().getEffect();
      if (entity.hasEffect(effect)) {
        MobEffectInstance existingEffect = entity.getEffect(effect);
        if (existingEffect instanceof MobEffectInstanceAboutElemental oldInstance) {
          float newTypeMaxAmount = instance.getAttachmentType().getInitialAmount();
          float oldTypeMaxAmount = oldInstance.getAttachmentType().getInitialAmount();
          MobEffectInstanceAboutElemental newInstance = null;
          entity.removeEffect(effect);
          if (newTypeMaxAmount >= oldTypeMaxAmount) {
            newInstance = instance;
          } else {
            float currentAmount = oldInstance.getAttachmentAmount();
            float newAmount = Math.min(oldTypeMaxAmount, currentAmount + newTypeMaxAmount);
            newInstance =
                    new MobEffectInstanceAboutElemental(
                            effect, oldInstance.getAttachmentType(), newAmount);
          }
          if (newInstance != null) {
            entity.addEffect(newInstance);
          }
        } else {
          entity.removeEffect(effect);
          entity.addEffect(instance);
        }
      }
    }
  }

  @SubscribeEvent
  public static void onMobEffectRemoved(MobEffectEvent.Expired event) {

    if (Objects.requireNonNull(event.getEffectInstance()).getEffect().value()
        instanceof FrozenEffect) {
      if (event.getEntity() instanceof Player player) {
        if (player.isCreative()) {
          player.getAbilities().mayfly = true;
          player.onUpdateAbilities();
        }
      } else if (event.getEntity() instanceof IEntityNoAIAccessor accessor) {
        accessor.setNoAi(false);
      }
    }
  }

  @SubscribeEvent
  public static void onMobEffectRemoved(MobEffectEvent.Remove event) {

    if (Objects.requireNonNull(event.getEffectInstance()).getEffect().value()
        instanceof FrozenEffect) {
      if (event.getEntity() instanceof Player player) {

        if (player.isCreative()) {
          player.getAbilities().mayfly = true;
          player.onUpdateAbilities();
        } else if (event.getEntity() instanceof IEntityNoAIAccessor accessor) {
          accessor.setNoAi(false);
        }
      }
    }
  }

  @SubscribeEvent
  public static void onClone(PlayerEvent.Clone event) {
    if (event.isWasDeath()) {
      if (event.getOriginal().hasData(PRIMOGEM_ATTACHMENT)) {
        event
            .getEntity()
            .getData(PRIMOGEM_ATTACHMENT)
            .changePacketPrimogem(event.getOriginal().getData(PRIMOGEM_ATTACHMENT).getPrimogem());
      }
      if (event.getOriginal().hasData(AttachmentRegistration.CHARACTER_SHEET_ATTACHMENT)) {
        event
            .getEntity()
            .getData(AttachmentRegistration.CHARACTER_SHEET_ATTACHMENT)
            .setItems(
                event
                    .getOriginal()
                    .getData(AttachmentRegistration.CHARACTER_SHEET_ATTACHMENT)
                    .getItems());
      }
      if (event.getOriginal().hasData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT)) {
        event
            .getEntity()
            .getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT)
            .setItems(
                event
                    .getOriginal()
                    .getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT)
                    .getItems());
        event
            .getEntity()
            .getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT)
            .setPacketCurrentCharacter(
                event
                    .getOriginal()
                    .getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT)
                    .getCurrentCharacterSlot());
      }
      if (event.getOriginal().hasData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT)) {
        event
            .getEntity()
            .getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT)
            .setPacketGenshinMode(
                event
                    .getOriginal()
                    .getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT)
                    .isGenshinMode());
      }
    }
  }

  @SubscribeEvent
  public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
    ServerPlayer player = (ServerPlayer) event.getEntity();
    PlayerPrimogemAttachment primogem = player.getData(PRIMOGEM_ATTACHMENT);
    PlayerGenshinModeAttachment genshinMode =
        player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT);
    CharacterParty characterParty =
        player.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
    for (ItemStack character : characterParty.getItems()) {
      PlayerCharacterData characterData = CombatHelper.getCharacterData(character);
      if (characterData != null) {
        characterData.setCurrentHP(
            Math.max(characterData.getCurrentHP(), characterData.getMaxHP().getTotalValue(true) * 0.4));
      }
    }
    CharacterSheet characterSheet =
        player.getData(AttachmentRegistration.CHARACTER_SHEET_ATTACHMENT.get());
    NetworkManager.setPrimogemToPlayer(player, primogem.getPrimogem());
    NetworkManager.setGenshinModeToPlayer(player, genshinMode.isGenshinMode());
    NetworkManager.setCharacterSelectionToPlayer(player, characterParty.getCurrentCharacterSlot());
    NetworkManager.setCharacterPartyToPlayer(
        player, characterParty.serializeNBT(player.registryAccess()));
    NetworkManager.setCharacterSheetToPlayer(
        player, characterSheet.serializeNBT(player.registryAccess()));
  }
}
