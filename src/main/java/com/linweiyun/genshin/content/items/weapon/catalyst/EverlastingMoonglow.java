package com.linweiyun.genshin.content.items.weapon.catalyst;

import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import net.minecraft.world.item.Item;

public class EverlastingMoonglow extends Catalyst {
    public EverlastingMoonglow(Item.Properties properties) {
        super(properties);
        this.star = 5;
        this.tier = 2;
        this.subStatAttribute = ModAttributes.MAX_HP;
    }
}