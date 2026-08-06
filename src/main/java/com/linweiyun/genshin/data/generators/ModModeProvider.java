package com.linweiyun.genshin.data.generators;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.registry.register.ItemsRegister;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.data.PackOutput;

public class ModModeProvider extends ModelProvider {
  public ModModeProvider(PackOutput output) {
    super(output, Minegenshin.MOD_ID);
  }

  @Override
  protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
    itemModels.generateFlatItem(ItemsRegister.PRIMOGEM.get(), ModelTemplates.FLAT_ITEM);
  }
}
