package com.linweiyun.genshin.content.blocks;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.content.blocks.custom.FirstBlock;
import com.linweiyun.genshin.core.registry.RegistryHelperGIM;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BlocksGIM {
  public static final DeferredRegister.Blocks BLOCKS =
      DeferredRegister.createBlocks(Minegenshin.MOD_ID);

  public static final Supplier<FirstBlock> FIRST_BLOCK =
      RegistryHelperGIM.registerBlock("first_block", FirstBlock::new);

  public static void register(IEventBus modEventBus) {
    BLOCKS.register(modEventBus);
  }
}
