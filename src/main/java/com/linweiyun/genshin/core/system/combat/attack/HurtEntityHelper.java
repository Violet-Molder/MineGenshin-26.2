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

import java.util.Objects;

public class HurtEntityHelper {
    public static final Logger LOGGER = LogUtils.getLogger();

    // =====================================================================
    //  伤害计算主入口 —— 按反应类型分发到不同乘区公式
    // =====================================================================

    public static void hurtEntityForPlayer(ModDamageSource damageSource, PGCharacter attacker, LivingEntity target) {
        if (target.level().isClientSide()) return;

        ModDamageSpec spec = damageSource.getSpec();
        LOGGER.info("[伤害] === 攻击={} | 目标={} | 元素={} | 类型={} ===",
                attacker.getName(), target.getName().getString(),
                spec.getElement(), spec.getAttackType());

        // 角色身上的效果先跑（申鹤冰凌等会在这往 spec 里塞 flatBonus）
        for (CharacterEffectInstance effect : attacker.getData().getEffectContainer().getEffects()) {
            if (effect != null) {
                Objects.requireNonNull(effect.getEffect()).onAttacked(
                        (Player) damageSource.getEntity(), attacker, target, effect, damageSource);
            }
        }
        LOGGER.info("[效果处理后] spec={}", damageSource.getSpec());
        float baseDamage = calculateCharacterDamage(damageSource, attacker);

        LOGGER.info("[效果处理后] spec={}", damageSource.getSpec());
        hurtEntityForPlayer(damageSource, attacker, target, baseDamage);
    }

    // =====================================================================
    //  管线：接收一个已经算好的基础伤害，走 附着→反应→衰减→最终hurt
    //  （剧变反应、特殊固定伤害等直接调用这个方法，跳过 calculateCharacterDamage）
    // =====================================================================

    public static void hurtEntityForPlayer(ModDamageSource damageSource, PGCharacter attacker,
                                           LivingEntity target, float baseDamage) {
        if (target.level().isClientSide()) return;

        ModDamageSpec spec = damageSource.getSpec();
        LivingEntity sourceEntity = (LivingEntity) damageSource.getEntity(); // 攻击方实体
        PGCharacter targetCharacter = resolveCharacter(target);

        DecayResult decayResult = DecayResult.NONE;

        // 衰减系统
        if (spec.hasDecayTag()) {
            IDecayCounterHolder holder = (IDecayCounterHolder) target;
            DecayCounterManager manager = holder.getDecayCounterManager();
            long currentTick = target.level().getGameTime();
            manager.getOrCreateCounter(
                    (LivingEntity) damageSource.getEntity(), attacker, spec, currentTick);
            decayResult = manager.processHit(
                    (LivingEntity) damageSource.getEntity(), attacker, spec, currentTick);
        }
        float elementCoefficient = decayResult.getElementCoefficient();
        // 1. 附着
        AttachmentProfile profile = chooseProfile(spec.getElementAmount());
        boolean canAttach = spec.hasAuraPotential() && elementCoefficient > 0;
        if (canAttach) {
            ElementalAttachmentHelper.attach(
                    StatusAccessor.of(target), spec.getElement(),
                    AttachmentSource.NORMAL_ATTACK, profile);
        }

        // 2. 触发反应
        ReactionResult reactionResult = null;
        if (canAttach) {
            StatusContainer container = target.getData(AttachmentRegistration.CONTAINER);
            ReactionContext ctx = new ReactionContext(
                    spec.getElement(), spec.getElementAmount() * elementCoefficient,
                    AttachmentSource.NORMAL_ATTACK, profile, spec,
                    damageSource.getEntity(), container);
            reactionResult = ElementalReactionManager.tryReactAfterAttach(ctx);
        }

        // 关键改动：把 sourceEntity 和 targetCharacter 传进去
        float finalDamage = calculateFinalDamage(
                baseDamage, spec,
                sourceEntity, attacker,
                target, targetCharacter,
                reactionResult);
        // ↓↓↓ 加这里：hurt 之前
        LOGGER.info("[最终伤害] base={} → final={}", baseDamage, finalDamage);
        // ↑↑↑
        // 4. 最后乘衰减的伤害系数
        finalDamage *= decayResult.getDamageCoefficient();
        target.hurt(damageSource, finalDamage);
    }



    // =====================================================================
    //  基础伤害计算 —— 只算乘区，不管反应
    //  (ATK×atkMult + HP×hpMult + DEF×defMult + EM×emMult) × (1 + skillMultiplierBonus) + flatDamageBonus
    // =====================================================================

    private static float calculateCharacterDamage(ModDamageSource damageSource, PGCharacter attacker) {
        ModDamageSpec spec = damageSource.getSpec();
        var data = attacker.getData();

        double atk = data.getAttributeTotalValue(ModAttributes.ATK.value());
        double hp  = data.getAttributeTotalValue(ModAttributes.MAX_HP.value());
        double def = data.getAttributeTotalValue(ModAttributes.DEF.value());
        double em  = data.getAttributeTotalValue(ModAttributes.ELEMENTAL_MASTERY.value());

        // ↓↓↓ 加这里：算出 atk/hp/def/em 之后
        LOGGER.info("[属性快照] ATK={} | HP={} | DEF={} | EM={}", atk, hp, def, em);
        LOGGER.info("[倍率快照] atkMult={} | hpMult={} | defMult={} | emMult={} | skillMultBonus={} | flatBonus={}",
                spec.getAtkMultiplier(), spec.getHpMultiplier(), spec.getDefMultiplier(), spec.getEmMultiplier(),
                spec.getSkillMultiplierBonus(), spec.getFlatDamageBonus());
        // ↑↑↑

        float base = (float) ((
                atk * spec.getAtkMultiplier() +
                        hp  * spec.getHpMultiplier() +
                        def * spec.getDefMultiplier() +
                        em  * spec.getEmMultiplier()
        ) * (1 + spec.getSkillMultiplierBonus()) + spec.getFlatDamageBonus());

        LOGGER.info("[基础伤害区] = {}", base);

        return base;
    }

    // =====================================================================
    //  乘区分发 —— 在已经算好的 baseDamage 上乘对应乘区
    // =====================================================================

    private static float calculateFinalDamage(float baseDamage, ModDamageSpec spec,
                                              LivingEntity attacker, PGCharacter attackerCharacter,
                                              LivingEntity target, PGCharacter targetCharacter,
                                              ReactionResult reaction) {
        float crit = critZone(attackerCharacter);
        float bonus = dmgBonusZone(attackerCharacter, spec);
        float def = defenseZone(attacker, attackerCharacter, target, targetCharacter);
        float res = resistanceZone(spec.getElement(), target, targetCharacter);
        LOGGER.info("[最终伤害区] crit={} | bonus={} | def={} | res={}",
                crit, bonus, def, res);

        // 无反应
        if (reaction == null || !reaction.isReacted()) {
            return baseDamage * crit * bonus * def * res;
        }

        return switch (reaction.getReactionType()) {
            // 增幅
            case MELT, VAPORIZE -> baseDamage
                    * crit
                    * reaction.getAmplifyMultiplier()
                    * emAndReactionBonus(attackerCharacter)
                    * bonus * def * res;

            // TODO: 激化
            case AGGRAVATE, SPREAD, QUICKEN -> baseDamage * crit * bonus * def * res;

            // TODO: 剧变（剧变伤害不乘防御区，但主伤害还是要的）
            case OVERLOAD, SUPERCONDUCT, ELECTRO_CHARGED, BURNING, BLOOM, HYPERBLOOM, BURGEON
                    -> baseDamage * crit * bonus * def * res;

            default -> baseDamage * crit * bonus * def * res;
        };
    }

    // =====================================================================
    //  各乘区独立计算方法（目前占位，逐个填）
    // =====================================================================

    /** 暴击区：(1 + CR × CDG)  TODO */
    private static float critZone(PGCharacter attacker) { return 1.0f; }

    /** 增伤区：对应元素 DMGB 属性 TODO */
    private static float dmgBonusZone(PGCharacter attacker, ModDamageSpec spec) { return 1.0f; }
    private static float defenseZone(LivingEntity attacker, PGCharacter attackerCharacter,
                                     LivingEntity defender, PGCharacter defenderCharacter) {
        int attackerLevel = CombatEntityAccessor.getAttackerLevel(attacker, attackerCharacter);
        double defenderDef = CombatEntityAccessor.getDefenderDefense(defender, defenderCharacter);
        return CombatMath.defenseZone(attackerLevel, defenderDef);
    }
    private static float resistanceZone(ElementalsGIM element,
                                        LivingEntity defender, PGCharacter defenderCharacter) {
        float res = CombatEntityAccessor.getDefenderResistance(defender, defenderCharacter, element);
        return CombatMath.resistanceZone(res);
    }
    /** 元素精通+反应伤害加成 TODO */
    private static float emAndReactionBonus(PGCharacter attacker) { return 1.0f; }

    // =====================================================================
    private static AttachmentProfile chooseProfile(float elementAmount) {
        if (elementAmount >= 4.0f) return AttachmentProfile.ULTRA_STRONG;
        if (elementAmount >= 2.0f) return AttachmentProfile.STRONG;
        if (elementAmount >= 1.5f) return AttachmentProfile.MEDIUM;
        return AttachmentProfile.WEAK;
    }

    /** 尝试从 LivingEntity 上解析出对应的 PGCharacter（如果是玩家角色） */
    private static PGCharacter resolveCharacter(LivingEntity entity) {
        if (entity instanceof Player player) {
            // 怪物打玩家 → 从 PlayerCharactersAttachment 拿当前角色
            return player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT).getCurrentCharacter();
        }
        return null;
    }

}