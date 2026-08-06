package com.linweiyun.genshin.enums;

public enum ElementalReactionType {
  // 增幅反应
  MELT("融化"), // 火+冰
  VAPORIZE("蒸发"), // 火+水

  // 聚变反应
  SHATTERED("碎冰"), // 冻+特殊
  SUPERCONDUCT("超导"), // 雷+冰
  SWIRL("扩散"), // 风元素相关
  ELECTRO_CHARGED("感电"), // 水+雷
  OVERLOAD("超载"), // 火+雷
  BURNING("燃烧"), // 草+火
  // 聚变-绽放
  BLOOM("绽放"), // 水+草
  HYPERBLOOM("超绽放"), // 雷+绽放
  BURGEON("烈绽放"), // 火+绽放
  // 激化反应
  QUICKEN("原激化"), // 草+雷
  AGGRAVATE("超激化"), // 激+雷
  SPREAD("蔓激化"), // 激+草

  // 特殊反应
  FROZEN("冻结"), // 水+冰
  CRYSTALLIZE("结晶"); // 岩与其他元素

  private final String displayName;

  ElementalReactionType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
