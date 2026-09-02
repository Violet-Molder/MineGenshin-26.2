package com.linweiyun.genshin.core.system.combat.attack;

import com.linweiyun.genshin.content.effect.character.CharacterEffectInstance;
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

        damage *= decayResult.getDamageCoefficient();
        target.hurt(damageSource, damage);

        // 元素附着逻辑 —— 用衰减序列的元素系数
        if (spec.hasAuraPotential() && decayResult.getElementCoefficient() > 0) {
            AttachmentProfile profile = chooseProfile(spec.getElementAmount());
            ElementalAttachmentHelper.attach(
                    target,
                    spec.getElement(),
                    AttachmentSource.NORMAL_ATTACK,
                    profile
            );
        }
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
        LOGGER.info("" + damageSpec.getFlatDamageBonus());
        return baseDamageValue;
    }
}