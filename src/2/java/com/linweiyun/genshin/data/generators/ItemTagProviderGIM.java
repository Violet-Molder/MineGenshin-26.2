package com.linweiyun.genshin.data.generators;

import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

public class ItemTagProviderGIM extends ItemTagsProvider {
  public ItemTagProviderGIM(
      PackOutput packOutput,
      CompletableFuture<HolderLookup.Provider> lookupProvider,
      CompletableFuture<TagLookup<Block>> blockTags,
      String modId,
      @Nullable ExistingFileHelper existingFileHelper) {
    super(packOutput, lookupProvider, blockTags, modId, existingFileHelper);
  }

  @Override
  protected void addTags(HolderLookup.@NotNull Provider provider) {}
}
