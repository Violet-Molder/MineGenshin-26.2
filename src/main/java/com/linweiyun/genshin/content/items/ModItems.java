package com.linweiyun.genshin.content.items;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.content.items.artifact.ArtifactItem;
import com.linweiyun.genshin.content.items.artifact.ArtifactSet;
import com.linweiyun.genshin.content.items.artifact.crimson_witch.*;
import com.linweiyun.genshin.content.items.artifact.scarlet_proof.*;
import com.linweiyun.genshin.content.items.artifact.tenacity_of_the_millelith.*;
import com.linweiyun.genshin.content.items.development.AdviceBookItem;
import com.linweiyun.genshin.content.items.development.AdventurersExperienceItem;
import com.linweiyun.genshin.content.items.development.HerosWitItem;
import com.linweiyun.genshin.content.items.food.CharacterFoods;
import com.linweiyun.genshin.content.items.food.FoodItem;
import com.linweiyun.genshin.content.items.preicous.ItemPrimogem;
import com.linweiyun.genshin.content.items.weapon.catalyst.EverlastingMoonglow;
import com.linweiyun.genshin.content.items.weapon.catalyst.HymnTheMaelstrom;
import com.linweiyun.genshin.content.items.weapon.sword.BeyondTheChrysalis;
import com.linweiyun.genshin.core.system.registry.RegistryHelper;
import com.linweiyun.genshin.core.system.registry.register.ArtifactSets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ModItems {
  public static final DeferredRegister.Items ITEMS =
          DeferredRegister.createItems(Minegenshin.MOD_ID);

  public static final Supplier<ItemPrimogem> PRIMOGEM =
          RegistryHelper.registerOrdinaryItem("primogem", ItemPrimogem::new);

  public static final Supplier<CrimsonFlower> CRIMSON_FLOWER =
          RegistryHelper.registerOrdinaryItem("crimson_flower", CrimsonFlower::new);
  public static final Supplier<CrimsonPlume> CRIMSON_PLUME =
          RegistryHelper.registerOrdinaryItem("crimson_plume", CrimsonPlume::new);
  public static final Supplier<CrimsonSands> CRIMSON_SANDS =
          RegistryHelper.registerOrdinaryItem("crimson_sands", CrimsonSands::new);
  public static final Supplier<CrimsonGoblet> CRIMSON_GOBLET =
          RegistryHelper.registerOrdinaryItem("crimson_goblet", CrimsonGoblet::new);
  public static final Supplier<CrimsonCirclet> CRIMSON_CIRCLET =
          RegistryHelper.registerOrdinaryItem("crimson_circlet", CrimsonCirclet::new);

  // ---- 血红之证（Scarlet Proof）----
  public static final Supplier<ScarletFlower> SCARLET_FLOWER =
          RegistryHelper.registerOrdinaryItem("scarlet_flower", ScarletFlower::new);
  public static final Supplier<ScarletPlume> SCARLET_PLUME =
          RegistryHelper.registerOrdinaryItem("scarlet_plume", ScarletPlume::new);
  public static final Supplier<ScarletSands> SCARLET_SANDS =
          RegistryHelper.registerOrdinaryItem("scarlet_sands", ScarletSands::new);
  public static final Supplier<ScarletGoblet> SCARLET_GOBLET =
          RegistryHelper.registerOrdinaryItem("scarlet_goblet", ScarletGoblet::new);
  public static final Supplier<ScarletCirclet> SCARLET_CIRCLET =
          RegistryHelper.registerOrdinaryItem("scarlet_circlet", ScarletCirclet::new);

  // ---- 千岩牢固（Tenacity of the Millelith）----
  public static final Supplier<TenacityFlower> TENACITY_FLOWER =
          RegistryHelper.registerOrdinaryItem("tenacity_flower", TenacityFlower::new);
  public static final Supplier<TenacityPlume> TENACITY_PLUME =
          RegistryHelper.registerOrdinaryItem("tenacity_plume", TenacityPlume::new);
  public static final Supplier<TenacitySands> TENACITY_SANDS =
          RegistryHelper.registerOrdinaryItem("tenacity_sands", TenacitySands::new);
  public static final Supplier<TenacityGoblet> TENACITY_GOBLET =
          RegistryHelper.registerOrdinaryItem("tenacity_goblet", TenacityGoblet::new);
  public static final Supplier<TenacityCirclet> TENACITY_CIRCLET =
          RegistryHelper.registerOrdinaryItem("tenacity_circlet", TenacityCirclet::new);

  public static final Supplier<DarkFragment> DARK_FRAGMENT =
          RegistryHelper.registerOrdinaryItem("dark_fragment", DarkFragment::new);

  public static final Supplier<EverlastingMoonglow> EVERLASTING_MOONGLOW =
          RegistryHelper.registerOrdinaryItem("everlasting_moonglow", EverlastingMoonglow::new);
  /** 蝶变（五星单手剑，三种风轮换的武器被动）。模型暂时直接用钻石剑。 */
  public static final Supplier<BeyondTheChrysalis> BEYOND_THE_CHRYSALIS =
          RegistryHelper.registerOrdinaryItem(BeyondTheChrysalis.NAME, BeyondTheChrysalis::new);

  /** 漩流颂歌（五星法器，治疗触发的武器被动）。模型暂时借用书本。 */
  public static final Supplier<HymnTheMaelstrom> HYMN_OF_THE_MAELSTROM =
          RegistryHelper.registerOrdinaryItem(HymnTheMaelstrom.NAME, HymnTheMaelstrom::new);


  public static final Supplier<FoodItem> SWEET_MADAME =
          RegistryHelper.registerFoodItem("sweet_madame", props -> new FoodItem(
                  CharacterFoods.SWEET_MADAME, props));

  public static final Supplier<AdviceBookItem> WANDERERS_ADVICE =
          RegistryHelper.registerOrdinaryItem("wanderers_advice", AdviceBookItem::new);
  public static final Supplier<AdventurersExperienceItem> ADVENTURERS_EXPERIENCE =
          RegistryHelper.registerOrdinaryItem("adventurers_experience", AdventurersExperienceItem::new);
  public static final Supplier<HerosWitItem> HEROS_WIT =
          RegistryHelper.registerOrdinaryItem("heros_wit", HerosWitItem::new);

  private static final Map<DeferredHolder<ArtifactSet, ArtifactSet>,
          List<Supplier<? extends ArtifactItem>>> ARTIFACT_SET_ITEMS = new HashMap<>();

  static {
    ARTIFACT_SET_ITEMS.put(ArtifactSets.CRIMSON_WITCH,
            List.of(CRIMSON_FLOWER, CRIMSON_PLUME, CRIMSON_SANDS, CRIMSON_GOBLET, CRIMSON_CIRCLET));
    ARTIFACT_SET_ITEMS.put(ArtifactSets.SCARLET_PROOF,
            List.of(SCARLET_FLOWER, SCARLET_PLUME, SCARLET_SANDS, SCARLET_GOBLET, SCARLET_CIRCLET));
    ARTIFACT_SET_ITEMS.put(ArtifactSets.TENACITY_OF_THE_MILLELITH,
            List.of(TENACITY_FLOWER, TENACITY_PLUME, TENACITY_SANDS, TENACITY_GOBLET, TENACITY_CIRCLET));
  }

  public static Map<DeferredHolder<ArtifactSet, ArtifactSet>,
          List<Supplier<? extends ArtifactItem>>> getArtifactSetItemsMap() {
    return Collections.unmodifiableMap(ARTIFACT_SET_ITEMS);
  }

  public static void register(IEventBus modEventBus) {
    ITEMS.register(modEventBus);
  }
}