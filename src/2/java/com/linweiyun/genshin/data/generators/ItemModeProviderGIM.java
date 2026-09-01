package com.linweiyun.genshin.data.generators;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.content.items.ItemsGIM;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ItemModeProviderGIM extends ItemModelProvider {
  public ItemModeProviderGIM(PackOutput output, ExistingFileHelper existingFileHelper) {
    super(output, Minegenshin.MOD_ID, existingFileHelper);
  }

  @Override
  protected void registerModels() {
    basicItem(ItemsGIM.PRIMOGEM.get());
    basicItem(ItemsGIM.SWEET_MADAME.get());
  }
}
