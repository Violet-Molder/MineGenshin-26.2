package com.linweiyun.genshin.content.entities.ai;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import net.minecraft.world.entity.LivingEntity;

public interface ICombatTimer {

    int DEFAULT_COMBAT_DURATION = 100;

    default int genshin$getCombatTicks() {
        return ((LivingEntity) this).getData(AttachmentRegistration.COMBAT_TIMER.get());
    }

    default void genshin$setCombatTicks(int ticks) {
        ((LivingEntity) this).setData(AttachmentRegistration.COMBAT_TIMER.get(), ticks);
    }

    default int genshin$getCombatDuration() {
        return DEFAULT_COMBAT_DURATION;
    }

    default boolean genshin$isInCombat() {
        return genshin$getCombatTicks() > 0;
    }

    default void genshin$resetCombat() {
        genshin$setCombatTicks(genshin$getCombatDuration());
    }
}