package com.linweiyun.genshin.core.character.polearm;

import com.linweiyun.genshin.Config;
import com.linweiyun.genshin.content.effect.character.CharacterEffectHelper;
import com.linweiyun.genshin.content.effect.character.CharacterEffectInstance;
import com.linweiyun.genshin.content.effect.character.shenhe.IcyQuillEffect;
import com.linweiyun.genshin.content.entities.area.TalismanSpiritArea;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.attribute.ModAttributes;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.enums.CharacterAscendAttribute;
import com.linweiyun.genshin.enums.ElementalsGIM;
import com.linweiyun.genshin.registry.register.CharacterEffectRegister;
import com.linweiyun.genshin.registry.register.EntityRegister;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Shenhe extends PGCharacter {
    private static final Logger LOGGER= LogUtils.getLogger();

    public Shenhe() {
        super(135001, 5, Component.translatable("character.name.shenhe"),
                ElementalsGIM.CYRO, CharacterAscendAttribute.ATK,
                10 * 20, 15*20, 10 * 20, 80f, "shenhe",
                Map.of(
                        ModAttributes.MAX_HP.getId(), Config.SHENHE_HP,
                        ModAttributes.ATK.getId(), Config.SHENHE_ATK,
                        ModAttributes.DEF.getId(), Config.SHENHE_DEF
                ));
        data.setElementalSkillStacks(2);

    }

    @Override
    protected void triggerElementalSkill(Player player, int skillTime) {
        Level level = player.level();
        PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        if (skillTime < 1000) {
            Vec3 lookVec = player.getLookAngle().normalize();
            Vec3 startPos = player.position();
            Vec3 endPos = startPos.add(lookVec.scale(5.0));
            HitResult hit = level.clip(new ClipContext(startPos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            AABB aabb = new AABB(startPos, endPos).inflate(1.0);
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, aabb,
                    entity -> entity != player && entity.isAlive() && !entity.isSpectator());
            for (LivingEntity entity : entities) {
                Vec3 knockback = lookVec.scale(0.5);
                entity.push(knockback.x, 0.0, knockback.z);
                // TODO: 需要重构 CombatHelper.hurt 以接受 OwnedCharacter
            }
            player.setDeltaMovement(lookVec.scale(2.2));

            for (int i = 0; i < 4; i++) {
                PGCharacter partyChar = attachment.getPartyCharacter(i);
                if (partyChar != null) {
                    CharacterEffectInstance effect = new CharacterEffectInstance(CharacterEffectRegister.ICY_QUILL_EFFECT.get(), 200, 1);
                    effect.setIntData(IcyQuillEffect.ICY_QUILL_COUNT_KEY, 7);
                    CharacterEffectHelper.addEffect(player, partyChar, effect);
                }
            }
        } else {
            player.sendSystemMessage(Component.literal("长按"));
        }


    }

    @Override
    protected void triggerElementalBurst(Player player) {
        PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        PGCharacter character = attachment.getCurrentCharacter();

        TalismanSpiritArea field = EntityRegister.FIELD_TALISMAN_SPIRIT.get()
                .create(player.level(), EntitySpawnReason.EVENT);
        if (field != null) {
            field.setPos(player.position());
            if (character != null) {
                field.setOwner(player, character.getCharacterUUID());
            }
            boolean added = player.level().addFreshEntity(field);
        }
    }

    @Override
    public Map<Identifier, Supplier<List<? extends Integer>>> getStatGrowthMap() {
        return Map.of(
                ModAttributes.MAX_HP.getId(), Config.SHENHE_HP,
                ModAttributes.ATK.getId(), Config.SHENHE_ATK,
                ModAttributes.DEF.getId(), Config.SHENHE_DEF
        );
    }
}