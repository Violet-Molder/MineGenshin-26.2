package com.linweiyun.genshin.content.items;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import net.minecraft.world.item.Item;

public class TeyvatItem extends Item implements IPersistedSerializable {
    protected int star = 0;

    public TeyvatItem(Properties properties) {
        super(properties);
    }

    public int getStar() {
        return star;
    }

    public void setStar(int star) {
        this.star = star;
    }
}
