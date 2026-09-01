package com.linweiyun.genshin.content.items.character.player_character.polearm_character;

import com.linweiyun.genshin.Config;
import com.linweiyun.genshin.client.gui.screens.GUIServerHelperGIM;
import com.linweiyun.genshin.content.effects.itemstacks.ItemStackEffectHelper;
import com.linweiyun.genshin.content.effects.itemstacks.character.skill.shenhe.IcyQuillEffect;
import com.linweiyun.genshin.content.effects.itemstacks.instance.ItemStackEffectInstance;
import com.linweiyun.genshin.content.effects.itemstacks.registries.ItemStackEffects;
import com.linweiyun.genshin.content.entities.attachments.AttachmentRegistration;
import com.linweiyun.genshin.content.entities.attachments.attachment.CharacterParty;
import com.linweiyun.genshin.content.entities.entity.EntityRegister;
import com.linweiyun.genshin.content.entities.entity.summon.field.player.FieldTalismanSpirit;
import com.linweiyun.genshin.content.items.character.data.PlayerCharacterData;
import com.linweiyun.genshin.content.items.components.DataComponentCharacter;
import com.linweiyun.genshin.content.items.components.DataComponentRegistryCharacter;
import com.linweiyun.genshin.core.system.combat.CombatHelper;
import com.linweiyun.genshin.enums.CharacterAscendAttribute;
import com.linweiyun.genshin.enums.DamageTypeEnum;
import com.linweiyun.genshin.enums.ElementalsGIM;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class ShenHe extends PolearmCharacter {
  public ShenHe(Properties properties) {
    super(
        properties.component(
            DataComponentRegistryCharacter.CHARACTER_DATA.get(),
            new DataComponentCharacter(new PlayerCharacterData(1011, 24, 65))));
    starRating = 5;
    characterUUID = 135001;
    name = Component.translatable("character.name.shenhe");
    maxObtainingEnergy = 80;
    CHARACTER_ATK = Config.SHENHE_ATK;
    CHARACTER_DEF = Config.SHENHE_DEF;
    CHARACTER_HP = Config.SHENHE_HP;
    this.ASCEND_ATTRIBUTE = CharacterAscendAttribute.ATK;
    this.ELEMENTAL = ElementalsGIM.CYRO;
    this.SKILL_MAX_COOLDOWN_TICK = 10 * 20;
    this.BURST_MAX_COOLDOWN_TICK = 10 * 20;
  }

  @Override
  public @NotNull InteractionResultHolder<ItemStack> use(
      Level level, @NotNull Player player, @NotNull InteractionHand usedHand) {
    if (level.isClientSide) {
      GUIServerHelperGIM.openCharacterPartyScreen(player);
    }
    return super.use(level, player, usedHand);
  }

  @Override
  public void elementalSkill(Player player) {
    CharacterParty characterParty =
        player.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
    ItemStack character = characterParty.getStackByCharacter(this);
    PlayerCharacterData currentData = CombatHelper.getCharacterData(character);
    Level level = player.level();
    tujin(player, level, character);
    if (!level.isClientSide()) {
      for (int i = 0; i < characterParty.getSlots(); i++) {
        ItemStack stackInSlot = characterParty.getStackInSlot(i);
        if (!stackInSlot.isEmpty()) {
          ItemStackEffectInstance instance =
              new ItemStackEffectInstance(ItemStackEffects.ICE_QUILL_EFFECT.get(), 2400, 1, false);
          instance.setIntData(IcyQuillEffect.icyQuillCountKey, 5);
          instance.setIntData(IcyQuillEffect.icyQuillDamageKey, 100);
          ItemStackEffectHelper.addEffect(stackInSlot, player, instance);
        }
      }
    }
  }

  @Override
  public void elementalBurst(Player player) {
    if (!player.level().isClientSide) {
      CharacterParty characterParty =
          player.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
      ItemStack stack = characterParty.getStackByCharacter(this);
      PlayerCharacterData currentData = CombatHelper.getCharacterData(stack);
      if (currentData != null) {
        FieldTalismanSpirit fieldTalismanSpirit =
            EntityRegister.FIELD_TALISMAN_SPIRIT.get().create(player.level());
        if (fieldTalismanSpirit != null) {
          fieldTalismanSpirit.setPos(player.position());
          fieldTalismanSpirit.setCaser(stack);
          fieldTalismanSpirit.setOwner(player);
          player.level().addFreshEntity(fieldTalismanSpirit);
        }
      }
    }
  }

  private void tujin(Player player, Level level, ItemStack character) {
    Vec3 lookVec = player.getLookAngle().normalize();
    Vec3 startPos = player.position();
    Vec3 endPos = startPos.add(lookVec.scale(5.0));
    HitResult hit =
        level.clip(
            new ClipContext(
                startPos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
    AABB aabb = new AABB(startPos, endPos).inflate(1.0);
    List<LivingEntity> entities =
        level.getEntitiesOfClass(
            LivingEntity.class,
            aabb,
            entity -> entity != player && entity.isAlive() && !entity.isSpectator());

    for (LivingEntity entity : entities) {
      Vec3 knockback = lookVec.scale(0.5);
      entity.push(knockback.x, 0.0, knockback.z);
      CombatHelper.hurt(
          player, character, entity, DamageTypeEnum.ELEMENTAL_SKILL, ElementalsGIM.CYRO, 0.7f, 6f);
    }
    player.setDeltaMovement(lookVec.scale(2.2));
  }
}