package com.linweiyun.genshin.content.skill_node;

import com.linweiyun.genshin.core.system.registry.register.ModMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

public class SkillHelper {
    public static void addStun(Player player, int durationTicks) {
        player.addEffect(new MobEffectInstance(ModMobEffects.STUN, durationTicks, 0, false, false));
    }
}
