package com.linweiyun.genshin.content.items.character.player_character.polearm_character;

import com.linweiyun.genshin.content.items.character.player_character.PlayerCharacter;
import com.linweiyun.genshin.content.items.weapon.PolearmItemGenshin;

public abstract class PolearmCharacter extends PlayerCharacter {
  protected PolearmItemGenshin weapon;

  public PolearmCharacter(Properties properties) {
    super(properties);
  }
}
