package com.linweiyun.genshin.data.generators;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.core.system.registry.register.ModItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.data.PackOutput;

public class ModModeProvider extends ModelProvider {
  public ModModeProvider(PackOutput output) {
    super(output, Minegenshin.MOD_ID);
  }

  @Override
  protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
    itemModels.generateFlatItem(ModItems.PRIMOGEM.get(), ModelTemplates.FLAT_ITEM);
    itemModels.generateFlatItem(ModItems.CRIMSON_FLOWER.get(), ModelTemplates.FLAT_ITEM);
    itemModels.generateFlatItem(ModItems.CRIMSON_PLUME.get(), ModelTemplates.FLAT_ITEM);
    itemModels.generateFlatItem(ModItems.CRIMSON_SANDS.get(), ModelTemplates.FLAT_ITEM);
    itemModels.generateFlatItem(ModItems.CRIMSON_GOBLET.get(), ModelTemplates.FLAT_ITEM);
    itemModels.generateFlatItem(ModItems.CRIMSON_CIRCLET.get(), ModelTemplates.FLAT_ITEM);
  }
}
