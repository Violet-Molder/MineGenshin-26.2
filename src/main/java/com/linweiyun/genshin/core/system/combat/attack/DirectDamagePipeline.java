package com.linweiyun.genshin.core.system.combat.attack;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.status.StatusAccessor;
import com.linweiyun.genshin.core.system.about.AttachmentProfile;
import com.linweiyun.genshin.core.system.about.AttachmentSource;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.genshin.core.system.about.block.BlockElementHelper;
import com.linweiyun.genshin.core.system.combat.damage.CombatMath;
import com.linweiyun.genshin.core.system.combat.damage.DamageTrace;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSpec;
import com.linweiyun.genshin.core.system.combat.decay.DecayCounterManager;
import com.linweiyun.genshin.core.system.combat.decay.DecayResult;
import com.linweiyun.genshin.core.system.combat.decay.IDecayCounterHolder;
import com.linweiyun.genshin.core.system.reaction.ElementalReactionManager;
import com.linweiyun.genshin.core.system.reaction.ReactionContext;
import com.linweiyun.genshin.core.system.reaction.ReactionResult;
import com.linweiyun.genshin.core.system.shield.ShieldService;
import com.linweiyun.genshin.core.system.shield.ShieldService.AttachDecision;
import com.linweiyun.genshin.enums.ElementalReactionType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * <b>直伤管线</b> —— 角色技能打出来的那一下。
 *
 * <pre>
 * ① 衰减：算元素量与伤害系数（连续打同一个目标会递减）
 * ② 附着：把元素挂到目标身上
 * ③ 反应：挂上之后尝试触发反应（反应飘字由 ElementalReactionManager 内部处理）
 * ④ 乘区：伤害 = 基础区 × 倍率区 × 暴击区 × 增伤区 × 防御区 × 抗性区
 *                  ×（增幅反应）反应倍率 × 反应加成区 × 衰减系数
 * </pre>
 *
 * <p>日志里每个乘区一个【】，只写真正生效的项（见 {@code DamageTrace}）。
 */
final class DirectDamagePipeline {

    private DirectDamagePipeline() {
    }

    static float calculate(ModDamageSource damageSource, PGCharacter attacker, LivingEntity target) {
        // spec 要在角色效果钩子跑完之后重新读一次（钩子可能换掉整个 spec）
        ModDamageSpec spec = damageSource.getSpec();
        LivingEntity sourceEntity = (LivingEntity) damageSource.getEntity();
        PGCharacter targetCharacter = AttackerResolver.resolveCharacter(target);
        boolean hasAttacker = attacker != null;

        DamageTrace trace = DamageTrace.start("直伤");
        trace.headAttack(spec.getAttackType(), spec.getElement())
                .headEntities(sourceEntity, target, hasAttacker ? attacker.getName() : null);

        // ── ⓪ 基础伤害区 ──
        DamageZones.ZoneText baseZone = hasAttacker
                ? DamageZones.baseZoneText(attacker, spec)
                : new DamageZones.ZoneText("0", "0");
        float baseDamage = hasAttacker ? DamageZones.baseDamage(attacker, spec) : 0f;

        // ── ① 衰减 ──
        DecayResult decayResult = DecayResult.NONE;
        if (spec.hasDecayTag()) {
            IDecayCounterHolder holder = (IDecayCounterHolder) target;
            DecayCounterManager manager = holder.getDecayCounterManager();
            long currentTick = target.level().getGameTime();
            manager.getOrCreateCounter(sourceEntity, attacker, spec, currentTick);
            decayResult = manager.processHit(sourceEntity, attacker, spec, currentTick);
        }
        float elementCoefficient = decayResult.getElementCoefficient();

        // ── ② 附着 ──
        AttachmentProfile profile = chooseProfile(spec.getElementAmount());
        if (spec.getElement().isInstant()) {
            // 瞬发元素（风/岩这类）不参与常规衰减，附着量固定按 1.0 / 0.5 结算
            profile = new AttachmentProfile(
                    profile.getBaseQuantity(), profile.getLossMultiplier(), 1.0f, 0.5f);
        }
        boolean canAttach = spec.hasAuraPotential() && elementCoefficient > 0;

        // ── ②.5 过盾 ──
        // 护盾可以「吞掉」某些元素的附着（冰盾遇水、遇冰）。盾的判定优先于附着：
        //   BLOCK      → 不附着、不反应、不消耗元素量（这次攻击仍然算「受击」，伤害为 0）
        //   REACT_ONLY → 只反应不附着（盾自挂的元素和它反应，双方一起被吃掉）
        //   ALLOW      → 照常附着 + 反应
        boolean canReact = canAttach;
        if (canAttach) {
            AttachDecision decision = ShieldService.onElementalAttack(
                    target, spec.getElement(), spec.getElementAmount() * elementCoefficient, true);
            if (decision == AttachDecision.BLOCK) {
                canAttach = false;
                canReact = false;
            } else if (decision == AttachDecision.REACT_ONLY) {
                canAttach = false;
            }
        }

        if (canAttach && spec.getElement().isInstant()) {
            StatusContainer checkContainer = target.getData(AttachmentRegistration.CONTAINER);
            if (checkContainer == null
                    || !ElementalReactionManager.canElementReact(spec.getElement(), checkContainer)) {
                canAttach = false;
                canReact = false;
            }
        }
        StatusContainer container = target.getData(AttachmentRegistration.CONTAINER);
        if (canAttach) {
            long gameTime = target.level().getGameTime();
            if (hasAttacker && container != null) {
                ElementalAttachmentHelper.attach(target, container, spec.getElement(),
                        AttachmentSource.NORMAL_ATTACK, profile, attacker, gameTime);
            } else {
                ElementalAttachmentHelper.attach(target, StatusAccessor.of(target), spec.getElement(),
                        AttachmentSource.NORMAL_ATTACK, profile);
            }
        }

        // ── ③ 反应 ──
        // 反应飘字由 ElementalReactionManager.tryReactAfterAttach 内部统一处理，此处不再重复。
        ReactionResult reactionResult = null;
        if (canReact) {
            ReactionContext ctx = new ReactionContext(
                    spec.getElement(), spec.getElementAmount() * elementCoefficient,
                    AttachmentSource.NORMAL_ATTACK, profile, spec,
                    damageSource.getEntity(), container, target);
            reactionResult = ElementalReactionManager.tryReactAfterAttach(ctx);
        }
        ElementalReactionType reactionType =
                reactionResult != null && reactionResult.isReacted() ? reactionResult.getReactionType() : null;
        trace.head("反应", reactionType == null ? "无" : reactionType);

        boolean amplifying = reactionType == ElementalReactionType.MELT
                || reactionType == ElementalReactionType.VAPORIZE;

        // ── ④ 乘区 ──
        DamageZones.CritRoll critRoll = DamageZones.rollCrit(attacker, spec);
        spec.setCrit(critRoll.isCrit());
        float baseMultiplierBonus = hasAttacker ? spec.getSkillMultiplierBonus() : 0f;
        DamageZones.ZoneText bonusZone = hasAttacker
                ? DamageZones.damageBonusZoneText(attacker, spec)
                : new DamageZones.ZoneText("1", "1");
        float elementalBonus = hasAttacker ? DamageZones.elementalBonus(attacker, spec) : 0f;
        float effectBonus = hasAttacker ? DamageZones.effectBonus(attacker, spec) : 0f;
        // 按招式盖的增伤（也在增伤区；天赋自己算好的那一档）
        float specDamageBonus = hasAttacker ? spec.getDamageBonus() : 0f;
        int attackerLevel = DamageZones.attackerLevel(sourceEntity, attacker);
        double defenderDefense = DamageZones.defenderDefense(target, targetCharacter);
        float defenseZone = CombatMath.defenseZone(attackerLevel, defenderDefense);
        float rawResistance = DamageZones.rawResistance(spec.getElement(), target, targetCharacter);
        float resistanceZone = CombatMath.resistanceZone(rawResistance);
        float amplifyMultiplier = amplifying ? reactionResult.getAmplifyMultiplier() : 1f;
        float emBonus = amplifying ? DamageZones.emBonus(attacker, reactionType) : 0f;
        float decayCoefficient = decayResult.getDamageCoefficient();

        float finalDamage = baseDamage
                * (1f + baseMultiplierBonus)
                * critRoll.zone()
                * (1f + elementalBonus + effectBonus + specDamageBonus)
                * defenseZone
                * resistanceZone
                * (amplifying ? amplifyMultiplier * (1f + emBonus) : 1f)
                * damageBonusSovereignty(spec)
                * decayCoefficient;

        // ── 日志（每个乘区一个【】）──
        //
        // ⚠️ 数值行必须是「把数字代进公式」的写法，而不是乘区的计算结果：
        //     展开公式 伤害 = 【攻击力 × 攻击力倍率 + 附加伤害】 × 【1 + 暴击伤害】 × …
        //     数值　　 伤害 = 【1144.625 × 0.130 + 0】        × 【1 + 0.966】    × …
        //     （直接把 1.966 填进去的话，就没法把数字和公式里的名字对应起来了。）
        trace.zone("基础区", baseZone.formula(), baseZone.value());
        trace.zone("暴击区", "1 + 暴击伤害",
                critRoll.isCrit() ? "1 + " + DamageTrace.fmt(critRoll.critDamage()) : "1");
        trace.zone("倍率区", "1 + 倍率提升", "1 + " + DamageTrace.fmt(baseMultiplierBonus));
        trace.zone("增伤区", bonusZone.formula(), bonusZone.value());
        trace.zone("防御区", "(攻方等级×5+500)/(攻方等级×5+500+守方防御)",
                "(" + attackerLevel + "×5+500)/(" + attackerLevel + "×5+500+"
                        + DamageTrace.fmt(defenderDefense) + ")");
        trace.zone("抗性区", "1 - 抗性", "1 - " + DamageTrace.fmt(rawResistance));
        if (amplifying) {
            trace.zone("反应倍率区", "反应倍率", DamageTrace.fmt(amplifyMultiplier));
            float em = (float) DamageZones.elementalMastery(hasAttacker ? attacker : null);
            trace.zone("反应加成区", "1 + 2.78×元素精通/(元素精通+1400)",
                    "1 + 2.78×" + DamageTrace.fmt(em) + "/(" + DamageTrace.fmt(em) + "+1400)");
        }
        trace.zone("大权区", "1 + 大权加成", "1 + " + DamageTrace.fmt(spec.getSovereigntyBonus()));
        trace.zone("衰减区", "衰减伤害系数", DamageTrace.fmt(decayCoefficient));
        trace.result(finalDamage);
        trace.log();
        return finalDamage;
    }

    /** 大权区（按招式显式打开；没打开就是 1）。 */
    private static float damageBonusSovereignty(ModDamageSpec spec) {
        return 1f + spec.getSovereigntyBonus();
    }

    /** 按元素量选附着档次。 */
    private static AttachmentProfile chooseProfile(float amount) {
        if (amount <= 0f) return AttachmentProfile.WEAK;
        if (amount >= 4f) return AttachmentProfile.ULTRA_STRONG;
        if (amount >= 2f) return AttachmentProfile.MEDIUM;
        return AttachmentProfile.WEAK;
    }
}