package com.linweiyun.genshin.content.skill_node;

import com.linweiyun.genshin.core.system.registry.register.ModMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

public class SkillHelper {

    private final Player player;
    private final int durationTicks;

    public SkillHelper(Player player, int durationTicks) {
        this.player = player;
        this.durationTicks = durationTicks;
    }

    public void addStun() {
        player.addEffect(new MobEffectInstance(ModMobEffects.STUN, durationTicks, 0, false, false));
    }
}