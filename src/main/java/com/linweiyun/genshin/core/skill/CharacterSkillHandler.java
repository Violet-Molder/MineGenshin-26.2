package com.linweiyun.genshin.core.skill;

import com.linweiyun.genshin.core.character.PGCharacterData;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.registry.ModRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public class CharacterSkillHandler {

    private static CharacterSkillExecutor findExecutor(int characterUUID) {
        for (CharacterSkillExecutor executor : ModRegistries.SKILL_EXECUTOR_REGISTRY) {
            if (executor.getTargetCharacterUUID() == characterUUID) {
                return executor;
            }
        }
        return null;
    }

    public static void performElementalSkill(Player player, PGCharacterData character, PGCharacter def) {
        float cooldown = character.getElementalSkillCooldownTick();
//        if (cooldown > 0) {
//            player.sendSystemMessage(Component.literal("当前技能CD：" + cooldown));
//            return;
//        }
        CharacterSkillExecutor executor = findExecutor(def.getCharacterUUID());
        if (executor == null) {
            player.sendSystemMessage(Component.literal("§c角色 [" + def.getName().getString() + "] 的元素战技尚未实现"));
            return;
        }
        System.out.println(executor.getTargetCharacterUUID());
        executor.onElementalSkill(player, character, def);
        character.setElementalSkillCooldownTick(def.getSkillMaxCooldownTick());
    }

    public static void performElementalBurst(Player player, PGCharacterData character, PGCharacter def) {
        float cooldown = character.getElementalBurstCooldownTick();
        if (cooldown > 0) {
            player.sendSystemMessage(Component.literal("§e当前技能CD：" + cooldown));
            return;
        }
        if (character.getCurrentObtainingEnergy() < def.getMaxObtainingEnergy()) {
            player.sendSystemMessage(Component.literal("§e当前元素能量不足"));
            return;
        }
        CharacterSkillExecutor executor = findExecutor(def.getCharacterUUID());
        if (executor == null) {
            player.sendSystemMessage(Component.literal("§c角色 [" + def.getName().getString() + "] 的元素爆发尚未实现"));
            return;
        }
        executor.onElementalBurst(player, character, def);
        character.setCurrentObtainingEnergy(0);
        character.setElementalBurstCooldownTick(def.getBurstMaxCooldownTick());
    }
}