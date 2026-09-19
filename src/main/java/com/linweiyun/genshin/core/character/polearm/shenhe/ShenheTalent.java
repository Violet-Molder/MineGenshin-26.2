package com.linweiyun.genshin.core.character.polearm.shenhe;

import com.linweiyun.genshin.config.character.ShenheTalentConfig;
import com.linweiyun.genshin.content.effect.character.CharacterEffectHelper;
import com.linweiyun.genshin.content.effect.character.CharacterEffectInstance;
import com.linweiyun.genshin.content.effect.character.shenhe.IcyQuillEffect;
import com.linweiyun.genshin.content.effect.character.impl.DamageBonusEffect;
import com.linweiyun.genshin.content.entities.area.TalismanSpiritArea;
import com.linweiyun.genshin.content.skill_node.AreaEntityCollector;
import com.linweiyun.genshin.content.skill_node.DashSystem;
import com.linweiyun.genshin.content.skill_node.RushesForward;
import com.linweiyun.genshin.content.skill_node.SkillHelper;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.character.talent.TalentBase;
import com.linweiyun.genshin.core.system.combat.action.ActionDefinition;
import com.linweiyun.genshin.core.system.combat.action.ActionKind;
import com.linweiyun.genshin.core.system.combat.action.ActionSet;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSpec;
import com.linweiyun.genshin.core.system.combat.decay.DecayGroups;
import com.linweiyun.genshin.core.system.registry.register.ModCharacterEffects;
import com.linweiyun.genshin.content.entities.ModEntities;
import com.linweiyun.genshin.enums.AttachmentType;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.enums.AttackType;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.List;

public class ShenheTalent extends TalentBase {
    public static final Logger LOGGER = LogUtils.getLogger();

    // 冲刺参数
    private static final float CHARGE_DASH_DISTANCE = 10f;
    private static final int   CHARGE_DASH_TICKS = 10;
    private static final float SKILL_DASH_DISTANCE  = 10f;
    private static final int   SKILL_DASH_TICKS = 10;

    // ==================== 参数覆盖 ====================

    @Override
    public int getMaxCombo() { return 5; }

    @Override
    public int getPrecastTicks(int stage)  { return 1; }
    @Override
    public int getPostcastTicks(int stage) { return 15; } // 后摇即连招窗口


    @Override
    public int getSkillPrecastTicks()  { return 3; }
    @Override
    public int getSkillPostcastTicks() {
        return super.getSkillPostcastTicks();
    }

    @Override
    protected ActionSet buildDefaultActionSet(PGCharacter character) {
        return setBuilder()
                .normalCombo(
                        timing(15, 1, 17),
                        timing(1, 1, 17),
                        timing(1, 1, 17),
                        timing(1, 1, 17),
                        timing(1, 1, 2)
                )
                .charged(timing(0, 0, 0))
                .skillTap(timing(3, 1, 0))
                .skillHold(timing(0, 0, 0))
                .burst(timing(10, 1, 20))
                .build();
    }

    // ==================== 普攻 ====================

    @Override
    public void attack(Player player, PGCharacter character, int comboStage) {
        LOGGER.info("RAW comboStage={}", comboStage);   // ← 加这一行
        Level level = player.level();
        if (level.isClientSide()) return;
        int stage = comboStage;
        int naLevel = Math.max(1, character.getData().getNormalAttackLevel());

        float multiplier = (float) (
                ShenheTalentConfig.getNABase(stage)
                        + ShenheTalentConfig.getNAPerLevel(stage) * (naLevel - 1));
                LOGGER.info("multiplier: {}, stage: {}", multiplier, stage);


        Vec3 startPos = player.position();
        Vec3 lookDir = player.getLookAngle();
        Vec3 endPos = startPos.add(lookDir.scale(2.5f));

        List<LivingEntity> targets = new AreaEntityCollector(level, startPos, endPos, 1.0f).execute();

        for (LivingEntity target : targets) {
            if (target != player) {
                ModDamageSpec spec = ModDamageSpec.builder(AttackType.NORMAL_ATTACK, ModElements.CYRO.get())
                        .multiplier(multiplier)
                        .elementAmount(AttachmentType.WEAK.getInitialAmount())
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

    // ==================== 重击：突进 + 逐 tick 扫掠伤害 ====================

    @Override
    public void chargeAttack(Player player, PGCharacter character) {
        Vec3 delta = new RushesForward(player, CHARGE_DASH_DISTANCE).execute();

        if (player.level().isClientSide()) {
            // 客户端：只做视觉位移，不带伤害
            DashSystem.startDash(player, delta, CHARGE_DASH_TICKS);
        } else {
            // 服务端：只做伤害扫掠
            DashSystem.startDamageDash(player, delta, CHARGE_DASH_TICKS, hitEntity -> {
                ModDamageSpec spec = ModDamageSpec.builder(
                                AttackType.ELEMENTAL_SKILL, ModElements.PYRO.get())
                        .multiplier(3.5f)
                        .elementAmount(AttachmentType.WEAK.getInitialAmount())
                        .decayGroup(DecayGroups.SHENHE_SKILL)
                        .attackerCharacter(character)
                        .build();
                ModDamageSource source = ModDamageSource.from(spec, player);
                if (hitEntity.level() instanceof ServerLevel serverLevel) {
                    hitEntity.hurtServer(serverLevel, source, 0f);
                }
            });
        }
    }

    // ==================== E ====================

    @Override
    public void elementalSkill(Player player, PGCharacter character, int skillType) {
        Level level = player.level();

        int skillLevel = character.getData().getElementalSkillLevel();

        if (skillType < 1000) {
            // ========== 点按：两端都启动冲刺 ==========
            float pressDamage = ShenheTalentConfig.getSkillPressDamage(skillLevel);
            Vec3 delta = new RushesForward(player, SKILL_DASH_DISTANCE).execute();

            if (level.isClientSide()) {
                DashSystem.startDash(player, delta, SKILL_DASH_TICKS);
            } else {
                DashSystem.startDamageDash(player, delta, SKILL_DASH_TICKS, hitEntity -> {
                    ModDamageSpec spec = ModDamageSpec.builder(
                                    AttackType.ELEMENTAL_SKILL, ModElements.CYRO.get())
                            .multiplier(pressDamage)
                            .elementAmount(AttachmentType.WEAK.getInitialAmount())
                            .decayGroup(DecayGroups.SHENHE_SKILL)
                            .attackerCharacter(character)
                            .build();
                    ModDamageSource source = ModDamageSource.from(spec, player);
                    if (hitEntity.level() instanceof ServerLevel serverLevel) {
                        hitEntity.hurtServer(serverLevel, source, 0f);
                    }
                });
            }

            // ========== 后续效果只在服务端 ==========
            if (level.isClientSide()) return;

            PlayerCharactersAttachment attachment =
                    player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);

            for (int i = 0; i < 4; i++) {
                PGCharacter partyChar = attachment.getPartyCharacter(i);
                if (partyChar != null) {
                    CharacterEffectInstance effect = new CharacterEffectInstance(
                            ModCharacterEffects.ICY_QUILL_EFFECT.get(), 200, 1);
                    effect.setIntData(IcyQuillEffect.ICY_QUILL_COUNT_KEY, 5);
                    CharacterEffectHelper.addEffect(player, partyChar, effect);
                }
            }

            if (character.getData().getAscensionPhase() >= 4) {
                Identifier tapBuffId = Identifier.parse("minegenshin:shenhe_ascend2_tap");
                for (int i = 0; i < 4; i++) {
                    PGCharacter partyChar = attachment.getPartyCharacter(i);
                    if (partyChar != null) {
                        DamageBonusEffect tapBuff = new DamageBonusEffect(
                                0.15f, AttackType.ELEMENTAL_SKILL, AttackType.ELEMENTAL_BURST);
                        CharacterEffectInstance tapInstance =
                                new CharacterEffectInstance(tapBuffId, tapBuff, 200, 0);
                        CharacterEffectHelper.addEffect(player, partyChar, tapInstance);
                    }
                }
            }

            new SkillHelper(player, 10).addStun();

        } else {
            // ========== 长按：只在服务端 ==========
            if (level.isClientSide()) return;

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

            PlayerCharactersAttachment attachment =
                    player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            for (int i = 0; i < 4; i++) {
                PGCharacter partyChar = attachment.getPartyCharacter(i);
                if (partyChar != null) {
                    CharacterEffectInstance effect = new CharacterEffectInstance(
                            ModCharacterEffects.ICY_QUILL_EFFECT.get(), 300, 1);
                    effect.setIntData(IcyQuillEffect.ICY_QUILL_COUNT_KEY, 7);
                    CharacterEffectHelper.addEffect(player, partyChar, effect);
                }
            }
            if (character.getData().getAscensionPhase() >= 4) {
                Identifier holdBuffId = Identifier.parse("minegenshin:shenhe_ascend2_hold");
                for (int i = 0; i < 4; i++) {
                    PGCharacter partyChar = attachment.getPartyCharacter(i);
                    if (partyChar != null) {
                        DamageBonusEffect holdBuff = new DamageBonusEffect(
                                0.15f, AttackType.NORMAL_ATTACK,
                                AttackType.CHARGED_ATTACK, AttackType.PLUNGING_ATTACK);
                        CharacterEffectInstance holdInstance =
                                new CharacterEffectInstance(holdBuffId, holdBuff, 300, 0);
                        CharacterEffectHelper.addEffect(player, partyChar, holdInstance);
                    }
                }
            }

            new SkillHelper(player, 10).addStun();
        }
    }
    // ==================== Q ====================

    @Override
    public void elementalBurst(Player player, PGCharacter character) {
        Level level = player.level();
        if (level.isClientSide()) return;

        int burstLevel = character.getData().getElementalBurstLevel();

        // 施放直伤 —— 大范围冰伤
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

        // 生成领域
        PlayerCharactersAttachment attachment =
                player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        PGCharacter currentChar = attachment.getCurrentCharacter();

        TalismanSpiritArea field = ModEntities.FIELD_TALISMAN_SPIRIT.get()
                .create(player.level(), EntitySpawnReason.EVENT);
        if (field != null) {
            field.setPos(player.position());
            if (currentChar != null) {
                field.setOwner(player, currentChar);
            }
            player.level().addFreshEntity(field);
        }
    }
}