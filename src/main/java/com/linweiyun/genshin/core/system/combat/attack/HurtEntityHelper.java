package com.linweiyun.genshin.core.system.combat.attack;

import com.linweiyun.genshin.content.effect.character.CharacterEffectInstance;
import com.linweiyun.genshin.config.reaction.ReactionConfig;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.system.combat.damage.*;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.status.StatusAccessor;
import com.linweiyun.genshin.core.system.about.AttachmentProfile;
import com.linweiyun.genshin.core.system.about.AttachmentSource;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.genshin.core.system.combat.decay.DecayCounterManager;
import com.linweiyun.genshin.core.system.combat.decay.DecayResult;
import com.linweiyun.genshin.core.system.combat.decay.IDecayCounterHolder;
import com.linweiyun.genshin.core.system.reaction.ElementalReactionManager;
import com.linweiyun.genshin.core.system.reaction.ReactionContext;
import com.linweiyun.genshin.core.system.reaction.ReactionResult;
import com.linweiyun.genshin.enums.ElementalReactionType;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Objects;

public class HurtEntityHelper {
    public static final Logger LOGGER = LogUtils.getLogger();

    // ============================================================
    // 统一伤害入口 —— 所有伤害都从这里进，按 DamageType 分发
    // ============================================================

    public static float calculateFinalModDamage(ModDamageSource damageSource,
                                                PGCharacter attacker,
                                                LivingEntity target) {
        ModDamageSpec spec = damageSource.getSpec();

        // 非直伤 → 剧变/激化/月曜/星烁 独立管线
        if (spec.getDamageType() != ModDamageSpec.DamageType.DIRECT) {
            LivingEntity sourceEntity = (LivingEntity) damageSource.getEntity();
            float damage = switch (spec.getDamageType()) {
                case TRANSFORMATIVE -> calculateTransformativeDamage(
                        sourceEntity, target, spec.getTransformativeReactionType());
                case QUICKEN -> 0f;
                case LUNAR -> 0f;
                case STELLAR -> 0f;
                default -> 0f;
            };
            LOGGER.info("[{}管线] reaction={} | final={}",
                    spec.getDamageType(), spec.getTransformativeReactionType(), damage);
            return damage;
        }

        // 直伤 → 角色属性 → 衰减/附着/反应 → 直伤计算
//        LOGGER.info("[伤害管线] === 攻击={} | 目标={} | 元素={} | 类型={} ===",
//                attacker != null ? attacker.getName() : damageSource.getEntity(),
//                target.getName().getString(),
//                spec.getElement(), spec.getAttackType());

        if (attacker != null) {
            for (CharacterEffectInstance effect : new ArrayList<>(attacker.getData().getEffectContainer().getEffects())) {
                if (effect != null) {
                    Objects.requireNonNull(effect.getEffect()).onAttacked(
                            damageSource.getEntity() instanceof Player p ? p : null,
                            attacker, target, effect, damageSource);
                }
            }
        }
        spec = damageSource.getSpec();
//        LOGGER.info("[效果处理后] spec={}", spec);

        float baseDamage = attacker != null
                ? calculateCharacterDamage(spec, attacker)
                : 0.0f;

        return processPipeline(damageSource, attacker, target, baseDamage);
    }

    // ============================================================
    // 公用工具
    // ============================================================

    private static PGCharacter resolveCharacter(LivingEntity entity) {
        if (entity instanceof Player player) {
            return player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT).getCurrentCharacter();
        }
        return null;
    }

    private static AttachmentProfile chooseProfile(float elementAmount) {
        if (elementAmount >= 4.0f) return AttachmentProfile.ULTRA_STRONG;
        if (elementAmount >= 2.0f) return AttachmentProfile.STRONG;
        if (elementAmount >= 1.5f) return AttachmentProfile.MEDIUM;
        return AttachmentProfile.WEAK;
    }

    // ============================================================
    // 直伤管线 — 入口 → 衰减/附着/反应 → 基础伤害区 + 暴击区 + 增伤区 + 防御区 + 抗性区 + 反应乘区
    // ============================================================

    private static float processPipeline(ModDamageSource damageSource, PGCharacter attacker,
                                         LivingEntity target, float baseDamage) {
        ModDamageSpec spec = damageSource.getSpec();
        LivingEntity sourceEntity = (LivingEntity) damageSource.getEntity();
        PGCharacter targetCharacter = resolveCharacter(target);

        DecayResult decayResult = DecayResult.NONE;
        if (spec.hasDecayTag()) {
            IDecayCounterHolder holder = (IDecayCounterHolder) target;
            DecayCounterManager manager = holder.getDecayCounterManager();
            long currentTick = target.level().getGameTime();
            manager.getOrCreateCounter(sourceEntity, attacker, spec, currentTick);
            decayResult = manager.processHit(sourceEntity, attacker, spec, currentTick);
        }
        float elementCoefficient = decayResult.getElementCoefficient();

        AttachmentProfile profile = chooseProfile(spec.getElementAmount());
        boolean canAttach = spec.hasAuraPotential() && elementCoefficient > 0;
        if (canAttach) {
            ElementalAttachmentHelper.attach(target,
                    StatusAccessor.of(target), spec.getElement(),
                    AttachmentSource.NORMAL_ATTACK, profile);
        }

        ReactionResult reactionResult = null;
        if (canAttach) {
            StatusContainer container = target.getData(AttachmentRegistration.CONTAINER);
            ReactionContext ctx = new ReactionContext(
                    spec.getElement(), spec.getElementAmount() * elementCoefficient,
                    AttachmentSource.NORMAL_ATTACK, profile, spec,
                    damageSource.getEntity(), container, target);
            reactionResult = ElementalReactionManager.tryReactAfterAttach(ctx);
        }

        if (reactionResult != null && reactionResult.isReacted()
                && reactionResult.getReactionType() != null) {
            DamageIndicatorFactory.reaction(target, reactionResult.getReactionType());
        }

        float finalDamage = calculateFinalDamage(
                baseDamage, spec, sourceEntity, attacker, target, targetCharacter, reactionResult);

        LOGGER.info("[最终伤害] base={} → final={}", baseDamage, finalDamage);
        finalDamage *= decayResult.getDamageCoefficient();
        return finalDamage;
    }

    private static float calculateCharacterDamage(ModDamageSpec spec, PGCharacter attacker) {
        var data = attacker.getData();

        double atk = data.getAttributeTotalValue(ModAttributes.ATK.value());
        double hp  = data.getAttributeTotalValue(ModAttributes.MAX_HP.value());
        double def = data.getAttributeTotalValue(ModAttributes.DEF.value());
        double em  = data.getAttributeTotalValue(ModAttributes.ELEMENTAL_MASTERY.value());

        LOGGER.info("[属性快照] ATK={} | HP={} | DEF={} | EM={}", atk, hp, def, em);
        LOGGER.info("[倍率快照] atkMult={} | hpMult={} | defMult={} | emMult={} | skillMultBonus={} | flatBonus={}",
                spec.getAtkMultiplier(), spec.getHpMultiplier(), spec.getDefMultiplier(), spec.getEmMultiplier(),
                spec.getSkillMultiplierBonus(), spec.getFlatDamageBonus());

        float base = (float) ((
                atk * spec.getAtkMultiplier() +
                        hp  * spec.getHpMultiplier() +
                        def * spec.getDefMultiplier() +
                        em  * spec.getEmMultiplier()
        ) * (1 + spec.getSkillMultiplierBonus()) + spec.getFlatDamageBonus());
//
//        LOGGER.info("[基础伤害区] = {}", base);
        return base;
    }

    private static float calculateFinalDamage(float baseDamage, ModDamageSpec spec,
                                              LivingEntity attacker, PGCharacter attackerCharacter,
                                              LivingEntity target, PGCharacter targetCharacter,
                                              ReactionResult reaction) {
        float crit = critZone(attackerCharacter, spec);
        float bonus = dmgBonusZone(attackerCharacter, spec);
        float def = defenseZone(attacker, attackerCharacter, target, targetCharacter);
        float res = resistanceZone(spec.getElement(), target, targetCharacter);
        LOGGER.info("[最终伤害区] crit={} | bonus={} | def={} | res={}", crit, bonus, def, res);

        if (reaction == null || !reaction.isReacted()) {
            return baseDamage * crit * bonus * def * res;
        }

        return switch (reaction.getReactionType()) {
            case MELT, VAPORIZE -> baseDamage
                    * crit * reaction.getAmplifyMultiplier()
                    * reactionBonusZone(attackerCharacter, reaction.getReactionType())
                    * bonus * def * res;
            case AGGRAVATE, SPREAD, QUICKEN -> baseDamage * crit * bonus * def * res;
            case OVERLOAD, SUPERCONDUCT, ELECTRO_CHARGED, BURNING, BLOOM, HYPERBLOOM, BURGEON
                    -> baseDamage * crit * bonus * def * res;
            default -> baseDamage * crit * bonus * def * res;
        };
    }

    private static float critZone(PGCharacter attacker, ModDamageSpec spec) {
        if (attacker == null) return 1.0f;
        var data = attacker.getData();
        float critRate = (float) data.getAttributeTotalValue(ModAttributes.CR.value());
        float critDmg = (float) data.getAttributeTotalValue(ModAttributes.CDG.value());
        boolean isCrit = Math.random() < critRate;
        spec.setCrit(isCrit);
        return isCrit ? (1f + critDmg) : 1.0f;
    }

    private static float dmgBonusZone(PGCharacter attacker, ModDamageSpec spec) {
        float elementalBonus = CombatEntityAccessor.getDamageBonus(attacker, spec.getElement());
        float effectBonus = attacker.getData().getEffectContainer().getTotalDamageBonus(spec.getAttackType(), spec.getElement());
        return CombatMath.dmgBonusZone(elementalBonus + effectBonus);
    }

    private static float defenseZone(LivingEntity attacker, PGCharacter attackerCharacter,
                                     LivingEntity defender, PGCharacter defenderCharacter) {
        int attackerLevel = CombatEntityAccessor.getAttackerLevel(attacker, attackerCharacter);
        int defenderLevel = CombatEntityAccessor.getDefenderLevel(defender, defenderCharacter);
        double defenderDef = CombatEntityAccessor.getDefenderDefense(defender, defenderCharacter);
        double atkCoef = CombatMath.levelCoefficient(attackerLevel);
//        LOGGER.info("[防御区] 攻方等级={} | 攻方等级系数={} | 被攻方等级={} | 被攻方防御={}",
//                attackerLevel, atkCoef, defenderLevel, defenderDef);
        return 1 - CombatMath.defenseZone(attackerLevel, defenderDef);
    }

    // ============================================================
    // 公用乘区 —— 直伤和剧变都用的抗性区、EM乘区、反应加成区
    // ============================================================

    private static float resistanceZone(GenshinElement element,
                                        LivingEntity defender, PGCharacter defenderCharacter) {
        float res = CombatEntityAccessor.getDefenderResistance(defender, defenderCharacter, element);
        float resZone = CombatMath.resistanceZone(res);
        LOGGER.debug("[抗性区] element={} | 原始抗性={} | resZone={}", element, res, resZone);
        return resZone;
    }

    private static float emBonusZone(PGCharacter attacker, ElementalReactionType reactionType) {
        if (attacker == null) {
            LOGGER.info("[EM乘区] attacker=null → emBonus=0");
            return 0f;
        }
        double em = attacker.getData().getAttributeTotalValue(ModAttributes.ELEMENTAL_MASTERY.value());
        float bonus = switch (reactionType) {
            case MELT, VAPORIZE -> (float) ((2.78 * em) / (em + 1400.0));
            case OVERLOAD, SUPERCONDUCT, ELECTRO_CHARGED, BURNING, BLOOM, HYPERBLOOM, BURGEON
                    -> (float) ((16.0 * em) / (em + 2000.0));
            default -> 0f;
        };
        LOGGER.info("[EM乘区] EM={} | reaction={} | emBonus={}", em, reactionType, bonus);
        return bonus;
    }

    private static float reactionBonusZone(PGCharacter attacker, ElementalReactionType reactionType) {
        return 1.0f + emBonusZone(attacker, reactionType);
    }

    // ============================================================
    // 剧变反应管线 — 等级系数区 × 反应倍率区 × 反应加成区 × 抗性区（无基础伤害/暴击/增伤/防御）
    // ============================================================

    public static float calculateTransformativeDamage(LivingEntity attacker, LivingEntity target,
                                                       ElementalReactionType reactionType) {
        PGCharacter character = resolveCharacter(attacker);
        int level = CombatEntityAccessor.getAttackerLevel(attacker, character);
        double levelCoef = ReactionConfig.getReactionFusion(level);

        float reactionMult = switch (reactionType) {
            case ELECTRO_CHARGED -> ReactionConfig.ELECTROCHARGED.getFloat();
            default -> 1.0f;
        };

        float reactionBonus = reactionBonusZone(character, reactionType);

        GenshinElement dmgElement = switch (reactionType) {
            case ELECTRO_CHARGED -> ModElements.ELECTRO.get();
            default -> ModElements.FYSIKOS.get();
        };
        PGCharacter targetChar = resolveCharacter(target);
        float resZone = resistanceZone(dmgElement, target, targetChar);

        float damage = (float) (levelCoef * reactionMult * reactionBonus * resZone);

        LOGGER.info("[剧变伤害] attackerLevel={} | levelCoef={} | reactionMult={} | reactionBonus={} | resZone={} | final={}",
                level, levelCoef, reactionMult, reactionBonus, resZone, damage);
        return damage;
    }
}