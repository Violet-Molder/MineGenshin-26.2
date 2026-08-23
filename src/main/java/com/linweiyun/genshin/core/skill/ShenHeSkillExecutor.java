package com.linweiyun.genshin.core.skill;

import com.linweiyun.genshin.content.effect.character.CharacterEffectHelper;
import com.linweiyun.genshin.content.effect.character.CharacterEffectInstance;
import com.linweiyun.genshin.content.effect.character.shenhe.IcyQuillEffect;
import com.linweiyun.genshin.content.entities.area.TalismanSpiritArea;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.character.PGCharacterData;
import com.linweiyun.genshin.core.character.PGCharacterDefine;
import com.linweiyun.genshin.registry.register.CharacterEffectRegister;
import com.linweiyun.genshin.registry.register.EntityRegister;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ShenHeSkillExecutor implements CharacterSkillExecutor {
    @Override
    public int getTargetCharacterUUID() {
        return 135001;
    }
    @Override
    public void onElementalSkill(Player player, PGCharacterData character, PGCharacterDefine def) {
        Level level = player.level();
        PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
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
            PGCharacterData partyChar = attachment.getPartyCharacter(i);
            if (partyChar != null) {
                CharacterEffectInstance effect = new CharacterEffectInstance(CharacterEffectRegister.ICY_QUILL_EFFECT.get(), 200, 1);
                effect.setIntData(IcyQuillEffect.ICY_QUILL_COUNT_KEY, 7);
                CharacterEffectHelper.addEffect(player, partyChar, effect);
            }
        }
    }

    @Override
    public void onElementalBurst(Player player, PGCharacterData character, PGCharacterDefine def) {
        TalismanSpiritArea field = EntityRegister.FIELD_TALISMAN_SPIRIT.get()
                .create(player.level(), EntitySpawnReason.EVENT);
        if (field != null) {
            field.setPos(player.position());                  // 放置在玩家位置
            field.setOwner(player, character.getCharacterUUID()); // 设置拥有者+角色
            player.level().addFreshEntity(field);             // 生成实体
        }
    }
}