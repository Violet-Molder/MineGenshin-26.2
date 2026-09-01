package com.linweiyun.genshin.content.items;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.content.items.character.player_character.catalyst.Columbina;
import com.linweiyun.genshin.content.items.character.player_character.polearm_character.Arlecchino;
import com.linweiyun.genshin.content.items.character.player_character.polearm_character.RaidenShogun;
import com.linweiyun.genshin.content.items.character.player_character.polearm_character.ShenHe;
import com.linweiyun.genshin.content.items.custom.ItemPrimogem;
import com.linweiyun.genshin.content.items.food.FoodItem;
import com.linweiyun.genshin.core.food.CharacterFoods;
import com.linweiyun.genshin.core.registry.RegistryHelperGIM;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ItemsGIM {
  // 创建一个延迟注册器，用于注册物品
  public static final DeferredRegister.Items ITEMS =
      DeferredRegister.createItems(Minegenshin.MOD_ID);

  public static final Supplier<ItemPrimogem> PRIMOGEM =
      RegistryHelperGIM.registerOrdinaryItem("primogem", ItemPrimogem::new);
  public static final Supplier<Arlecchino> ARLECCHINO =
      RegistryHelperGIM.registerItem("arlecchino", Arlecchino::new);
  public static final Supplier<ShenHe> SHENHE =
      RegistryHelperGIM.registerItem("shenhe", ShenHe::new);
  public static final Supplier<RaidenShogun> RAIDEN_SHOGUN =
      RegistryHelperGIM.registerItem("raiden_shogun", RaidenShogun::new);
  public static final Supplier<Columbina> COLUMBINA =
      RegistryHelperGIM.registerItem("columbina", Columbina::new);

  public static final Supplier<FoodItem> SWEET_MADAME =
      RegistryHelperGIM.registerFoodItem(
          "sweet_madame",
          () -> new FoodItem(new MGItemProperties().characterFood(CharacterFoods.SWEET_MADAME)));

  public static void register(IEventBus modEventBus) {
    ITEMS.register(modEventBus);
  }
}
