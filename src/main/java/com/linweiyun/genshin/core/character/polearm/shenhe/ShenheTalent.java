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
    public int getActiveTicks(int stage)   { return 1; }
    @Override
    public int getPostcastTicks(int stage) { return 15; } // 后摇即连招窗口

    @Override
    public int getChargedPrecastTicks()  { return 5; }
    @Override
    public int getChargedActiveTicks()   { return 1; }
    @Override
    public int getChargedPostcastTicks() { return 15; }

    @Override
    public int getSkillPrecastTicks()  { return 3; }
    @Override
    public int getSkillActiveTicks()   { return 1; }
    @Override
    public int getSkillPostcastTicks() {
        return super.getSkillPostcastTicks();
    }

    @Override
    public int getBurstPrecastTicks()  { return 10; }
    @Override
    public int getBurstActiveTicks()   { return 1; }
    @Override
    public int getBurstPostcastTicks() { return 20; }

    // ==================== 动作集 ====================

    @Override
    protected ActionSet buildDefaultActionSet(PGCharacter character) {
        ActionSet.Builder b = ActionSet.builder();

        int maxCombo = getMaxCombo();
        for (int i = 0; i < maxCombo; i++) {
            final int stage = i;
            b.addNormalAttack(
                    ActionDefinition.builder(ActionKind.NORMAL_ATTACK)
                            .comboIndex(i)
                            .precast(getPrecastTicks(i))
                            .active(getActiveTicks(i))
                            .postcast(getPostcastTicks(i))
                            .onActiveStart(ctx -> attack(ctx.player, ctx.character, stage))
                            .build()
            );
        }

        b.chargedAttack(
                ActionDefinition.builder(ActionKind.CHARGED_ATTACK)
                        .precast(getChargedPrecastTicks())
                        .active(getChargedActiveTicks())
                        .postcast(getChargedPostcastTicks())
                        .onActiveStart(ctx -> chargeAttack(ctx.player, ctx.character))
                        .build()
        );

        b.elementalSkillTap(
                ActionDefinition.builder(ActionKind.ELEMENTAL_SKILL_TAP)
                        .precast(getSkillPrecastTicks())
                        .active(getSkillActiveTicks())
                        .postcast(getSkillPostcastTicks())
                        .onActiveStart(ctx -> elementalSkill(ctx.player, ctx.character, 0))
                        .build()
        );

        b.elementalSkillHold(
                ActionDefinition.builder(ActionKind.ELEMENTAL_SKILL_HOLD)
                        .precast(getSkillPrecastTicks())
                        .active(getSkillActiveTicks())
                        .postcast(getSkillPostcastTicks())
                        .onActiveStart(ctx -> elementalSkill(ctx.player, ctx.character, 1000))
                        .build()
        );

        b.elementalBurst(
                ActionDefinition.builder(ActionKind.ELEMENTAL_BURST)
                        .precast(getBurstPrecastTicks())
                        .active(getBurstActiveTicks())
                        .postcast(getBurstPostcastTicks())
                        .onActiveStart(ctx -> elementalBurst(ctx.player, ctx.character))
                        .build()
        );

        return b.build();
    }

    // ==================== 普攻 ====================

    @Override
    public void attack(Player player, PGCharacter character, int comboStage) {
        Level level = player.level();
        if (level.isClientSide()) return;

        int stage = comboStage % this.getMaxCombo();
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
        // 只在服务端启动带伤害的冲刺；客户端靠位置同步跟随
        if (player.level().isClientSide()) return;

        Vec3 delta = new RushesForward(player, CHARGE_DASH_DISTANCE).execute();
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

    // ==================== E ====================

    @Override
    public void elementalSkill(Player player, PGCharacter character, int skillType) {
        Level level = player.level();
        if (level.isClientSide()) return;   // 只在服务端执行

        int skillLevel = character.getData().getElementalSkillLevel();

        if (skillType < 1000) {
            // ========== 点按 ==========

            // 突进 + 逐 tick 扫掠伤害
            float pressDamage = ShenheTalentConfig.getSkillPressDamage(skillLevel);
            Vec3 delta = new RushesForward(player, SKILL_DASH_DISTANCE).execute();
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

            PlayerCharactersAttachment attachment =
                    player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);

            // 冰凌：5 根，持续 10s
            for (int i = 0; i < 4; i++) {
                PGCharacter partyChar = attachment.getPartyCharacter(i);
                if (partyChar != null) {
                    CharacterEffectInstance effect = new CharacterEffectInstance(
                            ModCharacterEffects.ICY_QUILL_EFFECT.get(), 200, 1);
                    effect.setIntData(IcyQuillEffect.ICY_QUILL_COUNT_KEY, 5);
                    CharacterEffectHelper.addEffect(player, partyChar, effect);
                }
            }

            // 突破天赋 2：点按 - 队伍内所有角色元素战技和元素爆发伤害 +15%，持续 10s
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
            // ========== 长按（无冲刺，AOE 伤害）==========

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

            // 冰凌：7 根，持续 15s
            for (int i = 0; i < 4; i++) {
                PGCharacter partyChar = attachment.getPartyCharacter(i);
                if (partyChar != null) {
                    CharacterEffectInstance effect = new CharacterEffectInstance(
                            ModCharacterEffects.ICY_QUILL_EFFECT.get(), 300, 1);
                    effect.setIntData(IcyQuillEffect.ICY_QUILL_COUNT_KEY, 7);
                    CharacterEffectHelper.addEffect(player, partyChar, effect);
                }
            }

            // 突破天赋 2：长按 - 队伍内所有角色普通攻击、重击、下落攻击伤害 +15%，持续 15s
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