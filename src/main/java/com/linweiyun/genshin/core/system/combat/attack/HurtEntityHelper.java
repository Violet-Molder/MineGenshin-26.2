package com.linweiyun.genshin.core.system.combat.attack;

import com.linweiyun.genshin.content.effect.character.CharacterEffectInstance;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.system.combat.damage.CombatEntityAccessor;
import com.linweiyun.genshin.core.system.combat.damage.CombatMath;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.status.StatusAccessor;
import com.linweiyun.genshin.core.system.about.AttachmentProfile;
import com.linweiyun.genshin.core.system.about.AttachmentSource;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSpec;
import com.linweiyun.genshin.core.system.combat.decay.DecayCounterManager;
import com.linweiyun.genshin.core.system.combat.decay.DecayResult;
import com.linweiyun.genshin.core.system.combat.decay.IDecayCounterHolder;
import com.linweiyun.genshin.core.system.reaction.ElementalReactionManager;
import com.linweiyun.genshin.core.system.reaction.ReactionContext;
import com.linweiyun.genshin.core.system.reaction.ReactionResult;
import com.linweiyun.genshin.enums.ElementalsGIM;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Objects;

public class HurtEntityHelper {
    public static final Logger LOGGER = LogUtils.getLogger();

    public static float calculateFinalModDamage(ModDamageSource damageSource,
                                                PGCharacter attacker,
                                                LivingEntity target) {
        ModDamageSpec spec = damageSource.getSpec();
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
            ElementalAttachmentHelper.attach(
                    StatusAccessor.of(target), spec.getElement(),
                    AttachmentSource.NORMAL_ATTACK, profile);
        }

        ReactionResult reactionResult = null;
        if (canAttach) {
            StatusContainer container = target.getData(AttachmentRegistration.CONTAINER);
            ReactionContext ctx = new ReactionContext(
                    spec.getElement(), spec.getElementAmount() * elementCoefficient,
                    AttachmentSource.NORMAL_ATTACK, profile, spec,
                    damageSource.getEntity(), container);
            reactionResult = ElementalReactionManager.tryReactAfterAttach(ctx);
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

        LOGGER.info("[基础伤害区] = {}", base);
        return base;
    }

    private static float calculateFinalDamage(float baseDamage, ModDamageSpec spec,
                                              LivingEntity attacker, PGCharacter attackerCharacter,
                                              LivingEntity target, PGCharacter targetCharacter,
                                              ReactionResult reaction) {
        float crit = critZone(attackerCharacter);
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
                    * emAndReactionBonus(attackerCharacter)
                    * bonus * def * res;
            case AGGRAVATE, SPREAD, QUICKEN -> baseDamage * crit * bonus * def * res;
            case OVERLOAD, SUPERCONDUCT, ELECTRO_CHARGED, BURNING, BLOOM, HYPERBLOOM, BURGEON
                    -> baseDamage * crit * bonus * def * res;
            default -> baseDamage * crit * bonus * def * res;
        };
    }

    private static float critZone(PGCharacter attacker) { return 1.0f; }
    private static float dmgBonusZone(PGCharacter attacker, ModDamageSpec spec) {
        float elementalBonus = CombatEntityAccessor.getDamageBonus(attacker, spec.getElement());
        return CombatMath.dmgBonusZone(elementalBonus); }

    private static float defenseZone(LivingEntity attacker, PGCharacter attackerCharacter,
                                     LivingEntity defender, PGCharacter defenderCharacter) {
        int attackerLevel = CombatEntityAccessor.getAttackerLevel(attacker, attackerCharacter);
        int defenderLevel = CombatEntityAccessor.getDefenderLevel(defender, defenderCharacter);
        double defenderDef = CombatEntityAccessor.getDefenderDefense(defender, defenderCharacter);
        double atkCoef = CombatMath.levelCoefficient(attackerLevel);
        LOGGER.info("[防御区] 攻方等级={} | 攻方等级系数={} | 被攻方等级={} | 被攻方防御={}",
                attackerLevel, atkCoef, defenderLevel, defenderDef);
        return CombatMath.defenseZone(attackerLevel, defenderDef);
    }

    private static float resistanceZone(ElementalsGIM element,
                                        LivingEntity defender, PGCharacter defenderCharacter) {
        float res = CombatEntityAccessor.getDefenderResistance(defender, defenderCharacter, element);
        LOGGER.info("[元素抗性区] {}抗性={}", element, res);
        return CombatMath.resistanceZone(res);
    }

    private static float emAndReactionBonus(PGCharacter attacker) { return 1.0f; }

    private static AttachmentProfile chooseProfile(float elementAmount) {
        if (elementAmount >= 4.0f) return AttachmentProfile.ULTRA_STRONG;
        if (elementAmount >= 2.0f) return AttachmentProfile.STRONG;
        if (elementAmount >= 1.5f) return AttachmentProfile.MEDIUM;
        return AttachmentProfile.WEAK;
    }

    private static PGCharacter resolveCharacter(LivingEntity entity) {
        if (entity instanceof Player player) {
            return player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT).getCurrentCharacter();
        }
        return null;
    }
}