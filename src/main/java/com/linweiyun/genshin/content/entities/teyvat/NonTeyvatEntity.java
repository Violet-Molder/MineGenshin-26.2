package com.linweiyun.genshin.content.entities.teyvat;

import com.linweiyun.genshin.config.entity.EntityHealthConfig;
import com.linweiyun.genshin.config.entity.EntityAttackConfig;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface NonTeyvatEntity extends TeyvatLiving {

    Map<EntityType<?>, float[]> MULTIPLIER_CACHE = new ConcurrentHashMap<>();

    @Override
    default float getHealthMultiplier() {
        LivingEntity self = (LivingEntity) this;
        float[] m = MULTIPLIER_CACHE.computeIfAbsent(self.getType(), type -> {
            float h = (float) (self.getAttributeBaseValue(Attributes.MAX_HEALTH)
                    / EntityHealthConfig.getHealthBaseCoefficient());
            float a = (float) (self.getAttributeBaseValue(Attributes.ATTACK_DAMAGE)
                    / EntityAttackConfig.getReferenceAttack());
            return new float[]{h, a};
        });
        return m[0];
    }

    @Override
    default float getAttackMultiplier() {
        LivingEntity self = (LivingEntity) this;
        float[] m = MULTIPLIER_CACHE.computeIfAbsent(self.getType(), type -> {
            float h = (float) (self.getAttributeBaseValue(Attributes.MAX_HEALTH)
                    / EntityHealthConfig.getHealthBaseCoefficient());
            float a = (float) (self.getAttributeBaseValue(Attributes.ATTACK_DAMAGE)
                    / EntityAttackConfig.getReferenceAttack());
            return new float[]{h, a};
        });
        return m[1];
    }
}