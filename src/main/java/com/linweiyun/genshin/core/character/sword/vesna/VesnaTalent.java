package com.linweiyun.genshin.core.character.sword.vesna;

import java.util.List;

import com.linweiyun.genshin.config.character.ShenheTalentConfig;
import com.linweiyun.genshin.content.effect.character.CharacterEffectContainer;
import com.linweiyun.genshin.content.effect.character.impl.RadianceStellarSwirlEffect;
import com.linweiyun.genshin.content.entities.teyvat.skill.vesna.VesnaAttackProjectile;
import com.linweiyun.genshin.content.entities.teyvat.skill.vesna.VesnaSpiritSwordEntity;
import com.linweiyun.genshin.content.skill_node.AreaEntityCollector;
import com.linweiyun.genshin.content.skill_node.TargetSeeker;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.character.talent.TalentBase;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.system.combat.action.ActionDefinition;
import com.linweiyun.genshin.core.system.combat.action.ActionKind;
import com.linweiyun.genshin.core.system.combat.action.ActionSet;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData;
import com.linweiyun.genshin.core.system.combat.damage.DecaySequence;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSpec;
import com.linweiyun.genshin.core.system.combat.decay.DecayGroup;
import com.linweiyun.genshin.core.system.combat.decay.DecayGroups;
import com.linweiyun.genshin.enums.AttachmentType;
import com.linweiyun.genshin.enums.AttackType;
import com.linweiyun.genshin.enums.ElementalReactionType;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class VesnaTalent extends TalentBase {
    public static final Logger LOGGER = LogUtils.getLogger();

    // ==================== 本角色独立衰减序列 ====================

    /**
     * 薇斯娜风铃独立衰减组别。
     * <p>弱附着（100 序列，每 3 次附着 1 次），与普通攻击 / 战技各自独立计数。
     */
    public static final DecayGroup VESNA_WIND_BELL_DECAY = new DecayGroup(
            50,
            DecaySequence.DEFAULT_ELEMENT,
            DecaySequence.DEFAULT_DAMAGE,
            DecaySequence.DEFAULT_POISE
    );

    // ==================== 数据表 ====================
    // 索引 = skillLevel - 1（skillLevel 1~15），倍率都是攻击力百分比（1.0 = 100%）

    public static final float[] SKILL_DAMAGE = {
            0.40f, 0.43f, 0.46f, 0.50f, 0.53f, 0.56f, 0.60f, 0.64f, 0.68f, 0.72f,
            0.76f, 0.80f, 0.85f, 0.90f, 0.95f
    };

    public static final float[] XFJ_LV1 = {
            0.40f, 0.43f, 0.46f, 0.50f, 0.53f, 0.56f, 0.60f, 0.64f, 0.68f, 0.72f,
            0.76f, 0.80f, 0.85f, 0.90f, 0.95f
    };

    public static final float[] XFJ_LV2_MAIN = {
            0.60f, 0.645f, 0.69f, 0.75f, 0.795f, 0.84f, 0.90f, 0.96f, 1.02f, 1.08f,
            1.14f, 1.20f, 1.275f, 1.35f, 1.425f
    };

    public static final float[] XFJ_LV2_SWORD = {
            1.12f, 1.204f, 1.288f, 1.40f, 1.484f, 1.568f, 1.68f, 1.792f, 1.904f, 2.016f,
            2.128f, 2.24f, 2.38f, 2.52f, 2.66f
    };

    public static final float[] XFJ_LV3_SWORD = {
            0.448f, 0.4816f, 0.5152f, 0.56f, 0.5936f, 0.6272f, 0.672f, 0.7168f, 0.7616f, 0.8064f,
            0.8512f, 0.896f, 0.952f, 1.008f, 1.064f
    };

    public static final float[] XFJ_LV3_FINAL = {
            1.568f, 1.6856f, 1.8032f, 1.96f, 2.0776f, 2.1952f, 2.352f, 2.5088f, 2.6656f, 2.8224f,
            2.9792f, 3.136f, 3.332f, 3.528f, 3.724f
    };

    public static final float[] WIND_BELL_DAMAGE = {
            0.104f, 0.1118f, 0.1196f, 0.13f, 0.1378f, 0.1456f, 0.156f, 0.1664f, 0.1768f, 0.1872f,
            0.1976f, 0.208f, 0.221f, 0.234f, 0.247f
    };

    // ==================== 工具方法 ====================

    private static float at(float[] table, int skillLevel) {
        int idx = Math.max(1, Math.min(table.length, skillLevel)) - 1;
        return table[idx];
    }

    public static float getWindBellDamageMultiplier(int skillLevel) {
        return at(WIND_BELL_DAMAGE, skillLevel);
    }

    // ==================== 参数覆盖 ====================

    @Override
    public int getMaxCombo() { return 6; }

    // ──── action set 构建 ────
    // 时序全部来自 CharacterActionData，这里只处理不同 stateKey 下的动画名差异。

    @Override
    public ActionSet buildActionSet(PGCharacter character, String stateKey) {
        ActionSet base = super.buildActionSet(character, stateKey);

        String skillAnim = switch (stateKey) {
            case "windrider_0" -> "skill_energy";
            case "windrider_1" -> "skill_energy_continue";
            case "windrider_2" -> "heavy_3";
            default -> null;
        };

        if (skillAnim == null) return base;

        CharacterActionData actionData = character.getActionData();
        if (actionData == null || actionData.skill() == null || actionData.skill().tap() == null)
            return base;

        CharacterActionData.ActionStep oldTap = actionData.skill().tap();
        CharacterActionData.ActionStep newTap = new CharacterActionData.ActionStep(
                skillAnim,
                oldTap.duration, oldTap.protectDuration, oldTap.priority,
                oldTap.moves, oldTap.hits, oldTap.sounds,
                oldTap.skillCharge, oldTap.finalCharge, oldTap.cooldown, oldTap.comboWindow
        );

        return setBuilder(base)
                .addSkillTap(
                        ActionDefinition.builder(ActionKind.ELEMENTAL_SKILL_TAP)
                                .step(newTap)
                                .onActiveStart(ctx -> elementalSkill(ctx.player, ctx.character, 0))
                                .build()
                )
                .build();
    }

    // ==================== 普攻 ====================

    @Override
    public void attack(Player player, PGCharacter character, int stage) {
        Level level = player.level();

        int naLevel = Math.max(1, character.getData().getNormalAttackLevel());
        float multiplier = (float) (
                ShenheTalentConfig.getNABase(stage)
                        + ShenheTalentConfig.getNAPerLevel(stage) * (naLevel - 1));

        Vec3 startPos = player.position();
        Vec3 lookDir = player.getLookAngle();
        Vec3 endPos = startPos.add(lookDir.scale(2.5f));

        List<LivingEntity> targets = new AreaEntityCollector(level, startPos, endPos, 1.0f).execute();

        for (LivingEntity target : targets) {
            if (target != player) {
                ModDamageSpec spec = ModDamageSpec.builder(AttackType.NORMAL_ATTACK, ModElements.ANEMO.get())
                        .multiplier(multiplier)
                        .elementAmount(AttachmentType.ULTRA_STRONG.getInitialAmount())
                        .attackerCharacter(character)
                        .build();
                ModDamageSource source = ModDamageSource.from(spec, player);
                if (target.level() instanceof ServerLevel serverLevel) {
                    target.hurtServer(serverLevel, source, 0f);
                    if (stage == 6) {
                        target.hurtServer(serverLevel, source, 0f);
                    }
                }
            }
        }

        if (level.isClientSide()) return;
        if (!(character instanceof Vesna vesna)) return;
        if (!vesna.isWindriderActive()) return;

        int bellCount = getBellCountForStage(stage);
        int skillLevel = character.getData().getElementalSkillLevel();
        for (int i = 0; i < bellCount; i++) {
            VesnaAttackProjectile projectile = VesnaAttackProjectile.create(
                    level, vesna, player.position(), skillLevel);
            if (projectile != null) {
                level.addFreshEntity(projectile);
                vesna.addEnergy(1);
            }
        }
    }

    private static int getBellCountForStage(int stage) {
        return switch (stage) {
            case 1, 2, 4, 5 -> 1;
            case 3 -> 2;
            case 6 -> 3;
            default -> 0;
        };
    }

    // ==================== 重击 ====================

    @Override
    public void chargeAttack(Player player, PGCharacter character) {
        Level level = player.level();
        if (level.isClientSide()) return;

        LivingEntity primaryTarget = new TargetSeeker(player, 10.0, TargetSeeker.TargetingType.LINE_OF_SIGHT).execute();
        if (primaryTarget == null) return;

        float aoeRange = 1.5f;
        Vec3 center = primaryTarget.position();
        List<LivingEntity> targets = new AreaEntityCollector(level,
                center.add(-aoeRange, -aoeRange, -aoeRange),
                center.add(aoeRange, aoeRange, aoeRange),
                aoeRange).execute();

        for (LivingEntity target : targets) {
            if (target != player) {
                ModDamageSpec spec = ModDamageSpec.stellarDirect(
                        ElementalReactionType.STELLAR_SWIRL_ICE, ModElements.CYRO.get(), 1.0f,
                        0.5f);
                spec.setStellarContributors(List.of(character));
                ModDamageSource source = ModDamageSource.from(spec, player);
                target.hurtServer((ServerLevel) level, source, 0f);
            }
        }

        if (!(character instanceof Vesna vesna)) return;
        if (!vesna.isWindriderActive()) return;

        int skillLevel = character.getData().getElementalSkillLevel();
        for (int i = 0; i < 2; i++) {
            VesnaAttackProjectile projectile = VesnaAttackProjectile.create(
                    level, vesna, player.position(), skillLevel);
            if (projectile != null) {
                level.addFreshEntity(projectile);
                vesna.addEnergy(1);
            }
        }
    }

    // ==================== E ====================

    @Override
    public void elementalSkill(Player player, PGCharacter character, int skillTime) {
        Level level = player.level();
        if (level.isClientSide()) return;
        if (!(character instanceof Vesna vesna)) return;

        int skillLevel = character.getData().getElementalSkillLevel();

        if (vesna.isWindriderActive()) {
            vesna.consumeEnergy(Vesna.SPECIAL_SKILL_ENERGY_COST);
            int castLevel = vesna.getXiangfengJianLevel();
            castXiangFengJian(player, vesna, skillLevel, castLevel);
            advanceAfterCast(vesna, castLevel);
            sendXiangfengMsg(player, castLevel);
        } else {
            castWindriderEnter(player, vesna, skillLevel);
            player.sendSystemMessage(Component.literal("§a进入巡风列装"));
        }
    }

    private void castWindriderEnter(Player player, Vesna vesna, int skillLevel) {
        vesna.activateWindriderMode();
        Vec3 center = player.position().add(player.getLookAngle().scale(2.0));
        float aoeRange = 2.5f;
        float mult = at(SKILL_DAMAGE, skillLevel);
        dealAoeAnemoDamage(player, vesna, center, aoeRange, mult,
                AttachmentType.WEAK.getInitialAmount(), false);
    }
    private static void sendXiangfengMsg(Player player, int castLevel) {
        String name = switch (castLevel) {
            case 0 -> "一阶";
            case 1 -> "二阶";
            case 2 -> "三阶";
            default -> "";
        };
        player.sendSystemMessage(Component.literal("§e翔风剑·" + name));
    }

    private void castXiangFengJian(Player player, Vesna vesna, int skillLevel, int castLevel) {
        boolean stellarSwirl = hasRadianceStellarSwirl(vesna);

        LivingEntity primaryTarget = new TargetSeeker(
                player, 10.0, TargetSeeker.TargetingType.LINE_OF_SIGHT).execute();
        if (primaryTarget == null) return;

        float aoeRange = 2.0f;
        Vec3 center = primaryTarget.position();

        switch (castLevel) {
            case 0 -> {
                float mult = at(XFJ_LV1, skillLevel);
                dealAoeAnemoDamage(player, vesna, center, aoeRange, mult,
                        AttachmentType.WEAK.getInitialAmount(), false);
            }
            case 1 -> {
                float mainMult = at(XFJ_LV2_MAIN, skillLevel);
                dealAoeAnemoDamage(player, vesna, center, aoeRange, mainMult,
                        AttachmentType.WEAK.getInitialAmount(), false);

                float swordMult = at(XFJ_LV2_SWORD, skillLevel);
                spawnSpiritSword(player, vesna, center, aoeRange, swordMult,
                        stellarSwirl, 0f);
            }
            case 2 -> {
                float swordMult = at(XFJ_LV3_SWORD, skillLevel);
                for (int i = 0; i < 4; i++) {
                    float attach = (i == 0) ? AttachmentType.WEAK.getInitialAmount() : 0f;
                    dealAoeAnemoDamage(player, vesna, center, aoeRange, swordMult,
                            attach, stellarSwirl);
                }

                float finalMult = at(XFJ_LV3_FINAL, skillLevel);
                spawnSpiritSword(player, vesna, center, aoeRange, finalMult,
                        stellarSwirl, 0f);
            }
            default -> LOGGER.warn("[VesnaTalent] unexpected castLevel={}", castLevel);
        }
    }

    private void advanceAfterCast(Vesna vesna, int castLevel) {
        if (castLevel >= 2) {
            int used = vesna.getLv3UsesInWindrider() + 1;
            if (used >= 3) {
                vesna.exitWindriderMode();
            } else {
                vesna.setLv3UsesInWindrider(used);
            }
        } else {
            vesna.setXiangfengJianLevel(castLevel + 1);
        }
    }

    // ==================== 星扩散判定 ====================

    private static boolean hasRadianceStellarSwirl(Vesna vesna) {
        CharacterEffectContainer container = vesna.getData().getEffectContainer();
        return container.getEffects().stream()
                .anyMatch(inst -> inst.getEffect() instanceof RadianceStellarSwirlEffect);
    }

    // ==================== 伤害工具 ====================

    private void dealAoeAnemoDamage(Player player, PGCharacter character,
                                    Vec3 center, float aoeRange, float multiplier,
                                    float elementAmount, boolean stellarSwirl) {
        Level level = player.level();
        if (level.isClientSide()) return;

        List<LivingEntity> targets = new AreaEntityCollector(level,
                center.add(-aoeRange, -aoeRange, -aoeRange),
                center.add(aoeRange, aoeRange, aoeRange),
                aoeRange).execute();

        for (LivingEntity target : targets) {
            if (target == player) continue;

            ModDamageSpec spec;
            if (stellarSwirl) {
                spec = ModDamageSpec.stellarDirect(
                        ElementalReactionType.STELLAR_SWIRL_WIND, ModElements.ANEMO.get(),
                        multiplier, 0.5f);
                spec.setStellarContributors(List.of(character));
            } else {
                spec = ModDamageSpec.builder(AttackType.ELEMENTAL_SKILL, ModElements.ANEMO.get())
                        .multiplier(multiplier)
                        .elementAmount(elementAmount)
                        .decayGroup(DecayGroups.DEFAULT_ELEMENTAL_SKILL)
                        .attackerCharacter(character)
                        .build();
            }
            ModDamageSource source = ModDamageSource.from(spec, player);
            if (target.level() instanceof ServerLevel serverLevel) {
                target.hurtServer(serverLevel, source, 0f);
            }
        }
    }

    // ==================== 灵剑实体生成 ====================

    private void spawnSpiritSword(Player player, Vesna vesna, Vec3 center, float aoeRange,
                                  float multiplier, boolean stellarSwirl, float elementAmount) {
        Level level = player.level();
        if (level.isClientSide()) return;

        Vec3 look = player.getLookAngle();
        Vec3 from = player.position().add(look.scale(1.5)).add(0, 2.5, 0);
        Vec3 to = center.add(0, 0.5, 0);

        VesnaSpiritSwordEntity sword = VesnaSpiritSwordEntity.create(
                level, vesna, from, to, multiplier, aoeRange, stellarSwirl, elementAmount);
        if (sword != null) {
            level.addFreshEntity(sword);
        }
    }
}