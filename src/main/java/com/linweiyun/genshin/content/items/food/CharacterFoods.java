package com.linweiyun.genshin.content.items.food;

public class CharacterFoods {
    public static final CharacterFoodProperties SWEET_MADAME =
            new CharacterFoodProperties.Builder()
                    .percentHeal(10.0F)
                    .eatSeconds(1.2F)
                    .fast()
                    .reviveFallen()
                    .build();

    public static final CharacterFoodProperties RADISH_VEGGIE_SOUP =
            new CharacterFoodProperties.Builder()
                    .fixedHeal(300.0F)
                    .eatSeconds(1.6F)
                    .build();
}