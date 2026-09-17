package com.linweiyun.genshin.enums;

public enum ElementalReactionType {
  // 增幅反应
  MELT("reaction.minegenshin.melt"), // 火+冰
  VAPORIZE("reaction.minegenshin.vaporize"), // 火+水

  // 聚变反应
  SHATTERED("reaction.minegenshin.shattered"), // 冻+特殊
  SUPERCONDUCT("reaction.minegenshin.superconduct"), // 雷+冰
  SWIRL("reaction.minegenshin.swirl"), // 风元素相关
  ELECTRO_CHARGED("reaction.minegenshin.electro_charged"), // 水+雷
  OVERLOAD("reaction.minegenshin.overload"), // 火+雷
  BURNING("reaction.minegenshin.burning"), // 草+火
  // 聚变-绽放
  BLOOM("reaction.minegenshin.bloom"), // 水+草
  HYPERBLOOM("reaction.minegenshin.hyperbloom"), // 雷+绽放
  BURGEON("reaction.minegenshin.burgeon"), // 火+绽放
  // 激化反应
  QUICKEN("reaction.minegenshin.quicken"), // 草+雷
  AGGRAVATE("reaction.minegenshin.aggravate"), // 激+雷
  SPREAD("reaction.minegenshin.spread"), // 激+草
  // 星体系

  // 月体系
  LUNAR_CHARGED("reaction.minegenshin.lunar_charged"), // 雷+水 (月感电)
  LUNAR_BLOOM("reaction.minegenshin.lunar_bloom"), // 草+水 (月绽放) - 占位
  LUNAR_CRYSTALLIZE("reaction.minegenshin.lunar_crystallize"), // 岩+水 (月结晶) - 占位

  // 特殊反应
  FROZEN("reaction.minegenshin.frozen"), // 水+冰
  CRYSTALLIZE("reaction.minegenshin.crystallize"); // 岩与其他元素

  private final String displayName;

  ElementalReactionType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}