package com.linweiyun.genshin.core.skill;

import com.linweiyun.genshin.core.character.PGCharacterData;
import net.minecraft.server.level.ServerPlayer;
import com.linweiyun.genshin.core.character.PGCharacter;
import net.minecraft.world.entity.player.Player;

public interface CharacterSkillExecutor {
    int getTargetCharacterUUID();
    void onElementalSkill(Player player, PGCharacterData character, PGCharacter def);
    void onElementalBurst(Player player, PGCharacterData character, PGCharacter def);
}


