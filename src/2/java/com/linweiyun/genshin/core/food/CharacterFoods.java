package com.linweiyun.genshin.core.food;

public class CharacterFoods {
  public static final CharacterFoodProperties SWEET_MADAME =
      new CharacterFoodProperties.Builder()
          .fixedHeal(300.0F) // 恢复25点生命值
          .eatSeconds(1.2F) // 食用1.2秒
          .build();
}
