package com.linweiyun.genshin.content.items.character.player_character.catalyst;

import com.linweiyun.genshin.content.items.character.player_character.PlayerCharacter;
import com.linweiyun.genshin.content.items.weapon.CatalystItemGenshin;
import com.linweiyun.genshin.enums.ElementalsGIM;
import net.neoforged.neoforge.common.property.Properties;

public abstract class CatalystCharacter extends PlayerCharacter {
  protected CatalystItemGenshin weapon;

  public CatalystCharacter(Properties properties) {
    super(properties);
  }

  public ElementalsGIM getElement() {
    return super.getElement();
  }
}
