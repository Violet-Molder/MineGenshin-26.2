package com.linweiyun.genshin.content.entities.teyvat;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.element.GenshinElement;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public interface TeyvatLiving {

    default TeyvatEntityStats getEntityStats() {
        return ((LivingEntity) this).getData(AttachmentRegistration.ENTITY_STATS.get());
    }

    default void setEntityStats(TeyvatEntityStats stats) {
        ((LivingEntity) this).setData(AttachmentRegistration.ENTITY_STATS.get(), stats);
    }

    default int getMonsterLevel() {
        return getEntityStats().level();
    }

    default void setMonsterLevel(int level) {
        setEntityStats(getEntityStats().withLevel(level));
    }

    default float getEnvironmentMultiplier() {
        return getEntityStats().environmentMultiplier();
    }

    default void setEnvironmentMultiplier(float multiplier) {
        setEntityStats(getEntityStats().withEnvironmentMultiplier(multiplier));
    }

    default float getHealthMultiplier() {
        return 1.0f;
    }

    default float getAttackMultiplier() {
        return 1.0f;
    }

    default float getMonsterAttack() {
        return getEntityStats().attack();
    }

    default void setMonsterAttack(float attack) {
        setEntityStats(getEntityStats().withAttack(attack));
    }

    default int getDefense() {
        return getEntityStats().getDefense();
    }

    default float getElementResistance(GenshinElement element) {
        return getEntityStats().getElementResistance(element);
    }

    default float getPhysicalResistance() {
        return getEntityStats().physicalResistance();
    }

    default int getCombatTicks() {
        return getEntityStats().combatTicks();
    }

    default void setCombatTicks(int ticks) {
        setEntityStats(getEntityStats().withCombatTicks(ticks));
    }

    default boolean isTargeting() {
        return getEntityStats().targeting();
    }

    default void setTargeting(boolean targeting) {
        setEntityStats(getEntityStats().withTargeting(targeting));
    }

    default int getCombatDuration() {
        return 100;
    }

    default boolean isInCombat() {
        return isTargeting() || getCombatTicks() > 0;
    }

    default void resetCombat() {
        setCombatTicks(getCombatDuration());
    }

    default void setAiEnabled(boolean enabled) {
        if (this instanceof Mob mob) {
            mob.setNoAi(!enabled);
        }
    }
}