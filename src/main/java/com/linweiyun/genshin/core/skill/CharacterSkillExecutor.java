package com.linweiyun.genshin.core.skill;

import com.linweiyun.genshin.core.character.PGCharacterData;
import com.linweiyun.genshin.core.character.PGCharacterDefine;
import net.minecraft.world.entity.player.Player;

public interface CharacterSkillExecutor {
    int getTargetCharacterUUID();
    void onElementalSkill(Player player, PGCharacterData character, PGCharacterDefine def);
    void onElementalBurst(Player player, PGCharacterData character, PGCharacterDefine def);
}


