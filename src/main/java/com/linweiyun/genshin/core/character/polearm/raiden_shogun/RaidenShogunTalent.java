package com.linweiyun.genshin.core.character.polearm.raiden_shogun;

import com.linweiyun.genshin.config.character.ShenheTalentConfig;
import com.linweiyun.genshin.content.effect.character.CharacterEffectHelper;
import com.linweiyun.genshin.content.effect.character.CharacterEffectInstance;
import com.linweiyun.genshin.content.effect.character.impl.DamageBonusEffect;
import com.linweiyun.genshin.content.effect.character.shenhe.IcyQuillEffect;
import com.linweiyun.genshin.content.entities.area.TalismanSpiritArea;
import com.linweiyun.genshin.content.skill_node.AreaEntityCollector;
import com.linweiyun.genshin.content.skill_node.RushesForward;
import com.linweiyun.genshin.content.skill_node.SkillHelper;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.character.talent.TalentBase;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSpec;
import com.linweiyun.genshin.core.system.combat.decay.DecayGroups;
import com.linweiyun.genshin.core.system.registry.register.ModCharacterEffects;
import com.linweiyun.genshin.content.entities.ModEntities;
import com.linweiyun.genshin.enums.AttachmentType;
import com.linweiyun.genshin.enums.AttackType;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class RaidenShogunTalent extends TalentBase {
    public static final Logger LOGGER = LogUtils.getLogger();

    public RaidenShogunTalent() {
        super(5,
                List.of(1, 1, 1, 1, 1),
                List.of(2, 2, 2, 2, 2),
                List.of(15, 15, 15, 15, 0));
    }

    @Override
    public void attack(Player player, PGCharacter character, int comboStage) {
        Level level = player.level();
        if (level.isClientSide()) return;


        int stage = comboStage % this.maxCombo;
        // 配置的 getNABase/getNAPerLevel 是 1~5 段
        int naLevel = Math.max(1, character.getData().getNormalAttackLevel());

        float multiplier = (float) (
                ShenheTalentConfig.getNABase(stage)
                        + ShenheTalentConfig.getNAPerLevel(stage) * (naLevel - 1)
        );

        Vec3 startPos = player.position();
        Vec3 lookDir = player.getLookAngle();
        Vec3 endPos = startPos.add(lookDir.scale(2.5f));

        List<LivingEntity> targets = new AreaEntityCollector(level, startPos, endPos, 1.0f).execute();

        for (LivingEntity target : targets) {
            if (target != player) {
                ModDamageSpec spec = ModDamageSpec.builder(AttackType.NORMAL_ATTACK, ModElements.CYRO.get())
                        .multiplier(multiplier)
                        .elementAmount(AttachmentType.ULTRA_STRONG.getInitialAmount())
                        .attackerCharacter(character)
                        .build();
                ModDamageSource source = ModDamageSource.from(spec, player);
                if (target.level() instanceof ServerLevel serverLevel) {
                    target.hurtServer(serverLevel, source, 0f);
                    if (stage == 4) {
                        target.hurtServer(serverLevel, source, 0f);
                    }
                }
            }
        }
    }

    @Override
    public void chargeAttack(Player player, PGCharacter character) {
        Level level = player.level();
        Vec3 startPos = player.position();
        Vec3 dashTotal = new RushesForward(player, 10).execute();
        Vec3 rawEndPos = startPos.add(dashTotal);
        HitResult hit = level.clip(new ClipContext(startPos, rawEndPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 endPos = hit.getLocation();
        List<LivingEntity> entities = new ArrayList<>();
        entities = new AreaEntityCollector(level, startPos, endPos, 0.5f).execute();
        entities.forEach(
                entity -> {
                    if (entity != player) {
                        ModDamageSpec spec = ModDamageSpec.builder(AttackType.ELEMENTAL_SKILL, ModElements.PYRO.get())
                                .multiplier(3.5f)
                                .elementAmount(AttachmentType.WEAK.getInitialAmount())
                                .decayGroup(DecayGroups.SHENHE_SKILL)
                                .attackerCharacter(character)
                                .build();
                        ModDamageSource source = ModDamageSource.from(spec, player);
                        if (entity.level() instanceof ServerLevel serverLevel) {
                            entity.hurtServer(serverLevel, source, 0f);
                        }
                    }

                }
        );
    }

    @Override
    public void elementalSkill(Player player, PGCharacter character, int skillType) {
        Level level = player.level();
        PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        int skillLevel = character.getData().getElementalSkillLevel();

        if (skillType < 1000) {
            // ========== 点按 ==========

            // 伤害：向前冲刺并伤害路径上的敌人
            Vec3 startPos = player.position();
            Vec3 dashTotal = new RushesForward(player, 10).execute();
            Vec3 rawEndPos = startPos.add(dashTotal);
            HitResult hit = level.clip(new ClipContext(startPos, rawEndPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 endPos = hit.getLocation();
            List<LivingEntity> entities = new ArrayList<>();
            entities = new AreaEntityCollector(level, startPos, endPos, 0.5f).execute();

            float pressDamage = ShenheTalentConfig.getSkillPressDamage(skillLevel);

            entities.forEach(entity -> {
                if (entity != player) {
                    ModDamageSpec spec = ModDamageSpec.builder(AttackType.ELEMENTAL_SKILL, ModElements.CYRO.get())
                            .multiplier(pressDamage)
                            .elementAmount(AttachmentType.WEAK.getInitialAmount())
                            .decayGroup(DecayGroups.SHENHE_SKILL)
                            .attackerCharacter(character)
                            .build();
                    ModDamageSource source = ModDamageSource.from(spec, player);
                    if (entity.level() instanceof ServerLevel serverLevel) {
                        entity.hurtServer(serverLevel, source, 0f);
                    }
                }
            });

            // 冰凌：5根，持续10s
            if (!level.isClientSide()) {
                for (int i = 0; i < 4; i++) {
                    PGCharacter partyChar = attachment.getPartyCharacter(i);
                    if (partyChar != null) {
                        CharacterEffectInstance effect = new CharacterEffectInstance(ModCharacterEffects.ICY_QUILL_EFFECT.get(), 200, 1);
                        effect.setIntData(IcyQuillEffect.ICY_QUILL_COUNT_KEY, 5);
                        CharacterEffectHelper.addEffect(player, partyChar, effect);
                    }
                }

                // 突破天赋2：点按 - 队伍内所有角色元素战技和元素爆发伤害+15%，持续10s
                if (character.getData().getAscensionPhase() >= 4) {
                    Identifier tapBuffId = Identifier.parse("minegenshin:shenhe_ascend2_tap");
                    for (int i = 0; i < 4; i++) {
                        PGCharacter partyChar = attachment.getPartyCharacter(i);
                        if (partyChar != null) {
                            DamageBonusEffect tapBuff = new DamageBonusEffect(0.15f, AttackType.ELEMENTAL_SKILL, AttackType.ELEMENTAL_BURST);
                            CharacterEffectInstance tapInstance = new CharacterEffectInstance(tapBuffId, tapBuff, 200, 0);
                            CharacterEffectHelper.addEffect(player, partyChar, tapInstance);
                        }
                    }
                }
            }
            new SkillHelper(player, 10).addStun();

        } else {
            // ========== 长按 ==========

            // 伤害：收集周围5x5x4范围（y=4）内的非玩家实体并造成伤害
            float holdDamage = ShenheTalentConfig.getSkillHoldDamage(skillLevel);

            AABB holdBox = new AABB(
                    player.getX() - 2.5, player.getY() - 2, player.getZ() - 2.5,
                    player.getX() + 2.5, player.getY() + 2, player.getZ() + 2.5);

            List<LivingEntity> holdTargets = level.getEntitiesOfClass(LivingEntity.class, holdBox,
                    e -> e != player && !(e instanceof Player));

            for (LivingEntity target : holdTargets) {
                ModDamageSpec spec = ModDamageSpec.builder(AttackType.ELEMENTAL_SKILL, ModElements.CYRO.get())
                        .multiplier(holdDamage)
                        .elementAmount(AttachmentType.WEAK.getInitialAmount())
                        .decayGroup(DecayGroups.SHENHE_SKILL)
                        .attackerCharacter(character)
                        .build();
                ModDamageSource source = ModDamageSource.from(spec, player);
                if (target.level() instanceof ServerLevel serverLevel) {
                    target.hurtServer(serverLevel, source, 0f);
                }
            }

            // 冰凌：7根，持续15s
            if (!level.isClientSide()) {
                for (int i = 0; i < 4; i++) {
                    PGCharacter partyChar = attachment.getPartyCharacter(i);
                    if (partyChar != null) {
                        CharacterEffectInstance effect = new CharacterEffectInstance(ModCharacterEffects.ICY_QUILL_EFFECT.get(), 300, 1);
                        effect.setIntData(IcyQuillEffect.ICY_QUILL_COUNT_KEY, 7);
                        CharacterEffectHelper.addEffect(player, partyChar, effect);
                    }
                }

                // 突破天赋2：长按 - 队伍内所有角色普通攻击、重击、下落攻击伤害+15%，持续15s
                if (character.getData().getAscensionPhase() >= 4) {
                    Identifier holdBuffId = Identifier.parse("minegenshin:shenhe_ascend2_hold");
                    for (int i = 0; i < 4; i++) {
                        PGCharacter partyChar = attachment.getPartyCharacter(i);
                        if (partyChar != null) {
                            DamageBonusEffect holdBuff = new DamageBonusEffect(0.15f, AttackType.NORMAL_ATTACK, AttackType.CHARGED_ATTACK, AttackType.PLUNGING_ATTACK);
                            CharacterEffectInstance holdInstance = new CharacterEffectInstance(holdBuffId, holdBuff, 300, 0);
                            CharacterEffectHelper.addEffect(player, partyChar, holdInstance);
                        }
                    }
                }
            }
            new SkillHelper(player, 10).addStun();
        }
    }

    public void elementalBurst(Player player, PGCharacter character) {
        Level level = player.level();
        int burstLevel = character.getData().getElementalBurstLevel();

        // 施放直伤 —— 大范围冰伤
        if (!level.isClientSide()) {
            float castDamage = ShenheTalentConfig.getBurstCastDamage(burstLevel);
            AABB castBox = new AABB(
                    player.getX() - 6.0, player.getY() - 2.0, player.getZ() - 6.0,
                    player.getX() + 6.0, player.getY() + 2.0, player.getZ() + 6.0);

            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, castBox,
                    e -> e != player);
            for (LivingEntity target : targets) {
                ModDamageSpec spec = ModDamageSpec.builder(AttackType.ELEMENTAL_BURST, ModElements.CYRO.get())
                        .multiplier(castDamage)
                        .elementAmount(1.0f)
                        .attackerCharacter(character)
                        .build();
                ModDamageSource source = ModDamageSource.from(spec, player);
                if (target.level() instanceof ServerLevel serverLevel) {
                    target.hurtServer(serverLevel, source, 0f);
                }
            }
        }

        // 生成领域
        PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        PGCharacter currentChar = attachment.getCurrentCharacter();

        TalismanSpiritArea field = ModEntities.FIELD_TALISMAN_SPIRIT.get()
                .create(player.level(), EntitySpawnReason.EVENT);
        if (field != null) {
            field.setPos(player.position());
            if (currentChar != null) {
                field.setOwner(player, currentChar);
            }
            boolean added = player.level().addFreshEntity(field);
        }
    }
 }