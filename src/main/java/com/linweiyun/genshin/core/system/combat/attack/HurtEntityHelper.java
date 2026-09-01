package com.linweiyun.genshin.core.system.combat.attack;

import com.linweiyun.genshin.content.effect.character.CharacterEffectInstance;
import com.linweiyun.genshin.core.attribute.ModAttributes;
import com.linweiyun.genshin.core.character.PGCharacter;
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
    //玩家伤害实体
    public static void hurtEntityForPlayer(ModDamageSource damageSource, PGCharacter attacker, LivingEntity target) {
        if (target.level().isClientSide()) {
            return;
        }


        float finalDamage = calculateCharacterDamage(damageSource, attacker, target);
        hurtEntityForPlayer(damageSource, attacker, target, finalDamage);

        // TODO: 如果有元素附着能力且有元素系数，施加元素附着
    }
    public static void hurtEntityForPlayer(ModDamageSource damageSource, PGCharacter attacker, LivingEntity target, float damage) {
        ModDamageSpec spec = damageSource.getSpec();
        DecayResult decayResult = DecayResult.NONE;

        // ========== 衰减系统集成 ==========
        if (spec.hasDecayTag()) {
            IDecayCounterHolder holder = (IDecayCounterHolder) target;
            DecayCounterManager manager = holder.getDecayCounterManager();
            long currentTick = target.level().getGameTime();
            DecayCounterData counter = manager.getOrCreateCounter(
                    (LivingEntity) damageSource.getEntity(), attacker, spec, currentTick);
            decayResult = manager.processHit((LivingEntity) damageSource.getEntity(), attacker, spec, currentTick);

            // TEST: 打印附着冷却信息
            LOGGER.info("[附着冷却] 目标={} | 源实体={} | 源角色={} | 计数器={} | 计时器={} | 系数: 元素={} 伤害={} 削韧={}",
                    target.getName().getString(),                           // 目标
                    damageSource.getEntity().getName().getString(),         // 源实体
                    attacker != null ? attacker.getName().getString() : "无",  // 源角色
                    counter.getHitCount(),                                  // 计数器当前值
                    (currentTick - counter.getStartTimeTick()) + "tick",   // 计时器已运行时间
                    decayResult.getElementCoefficient(),                   // 元素系数
                    decayResult.getDamageCoefficient(),                    // 伤害系数
                    decayResult.getPoiseCoefficient()                      // 削韧系数
            );
        }
        // ==================================

        damage *= decayResult.getDamageCoefficient();
        target.hurt(damageSource, damage);

    }

    private static float calculateCharacterDamage(ModDamageSource damageSource, PGCharacter attacker, LivingEntity target) {
        for (CharacterEffectInstance effect : attacker.getData().getEffectContainer().getEffects()) {
            if (effect != null) {
                Objects.requireNonNull(effect.getEffect()).onAttacked((Player) damageSource.getEntity() ,attacker, target, effect, damageSource);
            }

        }
        ModDamageSpec damageSpec = damageSource.getSpec();
        float baseDamageValue = (float) ((attacker.getData().getAttributeTotalValue(ModAttributes.ATK.value()) + damageSpec.getFlatDamageBonus() )* damageSpec.getDamageMultiplier());
        LOGGER.info("" + damageSpec.getFlatDamageBonus());
        return baseDamageValue;
    }
}
