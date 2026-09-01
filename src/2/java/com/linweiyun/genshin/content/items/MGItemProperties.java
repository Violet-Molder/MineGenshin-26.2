package com.linweiyun.genshin.content.items;

import com.linweiyun.genshin.content.items.components.DataComponentRegistryCharacter;
import com.linweiyun.genshin.core.food.CharacterFoodProperties;
import net.minecraft.world.item.Item;

public class MGItemProperties extends Item.Properties {
  public MGItemProperties characterFood(CharacterFoodProperties food) {
    this.component(DataComponentRegistryCharacter.CHARACTER_FOOD, food);
    return this;
  }
}
