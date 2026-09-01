package com.linweiyun.genshin.core.system.combat.attack;

import com.linweiyun.genshin.content.effect.character.CharacterEffectInstance;
import com.linweiyun.genshin.core.attribute.ModAttributes;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSpec;
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
        target.hurt(damageSource, finalDamage);
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
