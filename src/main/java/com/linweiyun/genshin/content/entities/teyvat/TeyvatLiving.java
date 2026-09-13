package com.linweiyun.genshin.content.entities.teyvat;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.enums.ElementalsGIM;
import net.minecraft.world.entity.LivingEntity;

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

    default int getDefense() {
        return getEntityStats().getDefense();
    }

    default float getElementResistance(ElementalsGIM element) {
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
}