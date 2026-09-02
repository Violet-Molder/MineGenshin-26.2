package com.linweiyun.genshin.core.system.combat.attack;

import com.linweiyun.genshin.content.effect.character.CharacterEffectInstance;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.attribute.ModAttributes;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.about.AttachmentProfile;
import com.linweiyun.genshin.core.system.about.AttachmentSource;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSpec;
import com.linweiyun.genshin.core.system.combat.decay.DecayCounterData;
import com.linweiyun.genshin.core.system.combat.decay.DecayCounterManager;
import com.linweiyun.genshin.core.system.combat.decay.DecayResult;
import com.linweiyun.genshin.core.system.combat.decay.IDecayCounterHolder;
import com.linweiyun.genshin.core.system.reaction.ElementalReactionManager;
import com.linweiyun.genshin.core.system.reaction.ReactionContext;
import com.linweiyun.genshin.core.system.reaction.ReactionResult;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.Objects;

public class HurtEntityHelper {
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void hurtEntityForPlayer(ModDamageSource damageSource, PGCharacter attacker, LivingEntity target) {
        if (target.level().isClientSide()) {
            return;
        }

        float finalDamage = calculateCharacterDamage(damageSource, attacker, target);
        hurtEntityForPlayer(damageSource, attacker, target, finalDamage);
    }

    public static void hurtEntityForPlayer(ModDamageSource damageSource, PGCharacter attacker, LivingEntity target, float damage) {
        if (target.level().isClientSide()) {
            return;
        }
        ModDamageSpec spec = damageSource.getSpec();
        DecayResult decayResult = DecayResult.NONE;

        if (spec.hasDecayTag()) {
            IDecayCounterHolder holder = (IDecayCounterHolder) target;
            DecayCounterManager manager = holder.getDecayCounterManager();
            long currentTick = target.level().getGameTime();
            DecayCounterData counter = manager.getOrCreateCounter(
                    (LivingEntity) damageSource.getEntity(), attacker, spec, currentTick);
            decayResult = manager.processHit((LivingEntity) damageSource.getEntity(), attacker, spec, currentTick);

            // TEST: 打印附着冷却信息
//            LOGGER.info("[附着冷却] 目标={} | 源实体={} | 源角色={} | 计数器={} | 计时器={} | 系数: 元素={} 伤害={} 削韧={}",
//                    target.getName().getString(),
//                    damageSource.getEntity().getName().getString(),
//                    attacker != null ? attacker.getName().getString() : "无",
//                    counter.getHitCount(),
//                    (currentTick - counter.getStartTimeTick()) + "tick",
//                    decayResult.getElementCoefficient(),
//                    decayResult.getDamageCoefficient(),
//                    decayResult.getPoiseCoefficient()
//            );
        }
        float elementCoefficient = decayResult.getElementCoefficient();
        damage *= decayResult.getDamageCoefficient();
        // 1. 先附着（全额 × 元素系数）
        AttachmentProfile profile = chooseProfile(spec.getElementAmount());
        boolean canAttach = spec.hasAuraPotential() && elementCoefficient > 0;
        if (canAttach) {
            ElementalAttachmentHelper.attach(
                    target, spec.getElement(),
                    AttachmentSource.NORMAL_ATTACK, profile);
        }

        // 2. 触发反应
        ReactionResult reactionResult = null;
        if (canAttach) {
            StatusContainer container = target.getData(AttachmentRegistration.CONTAINER);
            ReactionContext ctx = new ReactionContext(
                    target,
                    spec.getElement(),
                    spec.getElementAmount() * elementCoefficient,
                    AttachmentSource.NORMAL_ATTACK,
                    profile,
                    spec,
                    damageSource.getEntity(),
                    container
            );
            reactionResult = ElementalReactionManager.tryReactAfterAttach(ctx);
        }

        // 3. 增幅修正
        if (reactionResult != null && reactionResult.isAmplified()) {
            damage *= reactionResult.getAmplifyMultiplier();
        }
        target.hurt(damageSource, damage);

    }

    private static AttachmentProfile chooseProfile(float elementAmount) {
        if (elementAmount >= 4.0f) return AttachmentProfile.ULTRA_STRONG;
        if (elementAmount >= 2.0f) return AttachmentProfile.STRONG;
        if (elementAmount >= 1.5f) return AttachmentProfile.MEDIUM;
        return AttachmentProfile.WEAK;
    }

    private static float calculateCharacterDamage(ModDamageSource damageSource, PGCharacter attacker, LivingEntity target) {
        for (CharacterEffectInstance effect : attacker.getData().getEffectContainer().getEffects()) {
            if (effect != null) {
                Objects.requireNonNull(effect.getEffect()).onAttacked((Player) damageSource.getEntity(), attacker, target, effect, damageSource);
            }
        }
        ModDamageSpec damageSpec = damageSource.getSpec();
        float baseDamageValue = (float) ((attacker.getData().getAttributeTotalValue(ModAttributes.ATK.value()) + damageSpec.getFlatDamageBonus()) * damageSpec.getDamageMultiplier());
        return baseDamageValue;
    }
}