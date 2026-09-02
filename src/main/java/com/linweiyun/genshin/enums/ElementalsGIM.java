package com.linweiyun.genshin.enums;

public enum ElementalsGIM {
  FYSIKOS("fysikos", "elemental.gim.fysikos", false, false, null),
  PYRO("pyro", "elemental.gim.pyro", true, false, null),
  HYDRO("hydro", "elemental.gim.hydro", false, false, null),
  ANEMO("anemo", "elemental.gim.anemo", false, true, null),
  ELECTRO("electro", "elemental.gim.electro", false, false, null),
  DENDRO("dendro", "elemental.gim.dendro", false, false, null),
  CYRO("cyro", "elemental.gim.cyro", false, false, null),
  GEO("geo", "elemental.gim.geo", false, true, null),

  // ========== 类元素（新增，作为主元素的补充附着状态）==========
  // 冻结反应生成，类冰
  FROZEN("frozen", "elemental.gim.frozen", false, false, CYRO),
  // 原激化反应生成，类草，覆盖时替换衰减速率（和火一样的特性）
  AGGRAVATE("aggravate", "elemental.gim.aggravate", true, false, DENDRO),
  // 燃烧反应生成，类火，覆盖时替换衰减速率
  BURNING("burning", "elemental.gim.burning", true, false, PYRO),
  // 丘丘人木盾自带，类草
  WOOD("wood", "elemental.gim.wood", false, false, DENDRO);

  private final String id;
  private final String translationKey;
  /** 覆盖同种元素附着时是否能替换衰减速率（火/激/燃=true，其他=false） */
  private final boolean canOverrideDecay;
  /** 是否为瞬时附着（风/岩=true，附着后立即消失，只能参与后手反应） */
  private final boolean instant;
  /** 类元素关联的主元素，主元素自身为 null */
  private final ElementalsGIM mainElement;

  ElementalsGIM(String id, String translationKey,
                boolean canOverrideDecay, boolean instant,
                ElementalsGIM mainElement) {
    this.id = id;
    this.translationKey = translationKey;
    this.canOverrideDecay = canOverrideDecay;
    this.instant = instant;
    this.mainElement = mainElement;
  }

  public String getId() { return id; }
  public String getTranslationKey() { return translationKey; }
  public boolean canOverrideDecay() { return canOverrideDecay; }
  public boolean isInstant() { return instant; }

  /** 类元素返回对应的主元素，主元素返回自身；用于反应配对判断（冻和冰都算冰系） */
  public ElementalsGIM getMainElement() {
    return mainElement != null ? mainElement : this;
  }

  /** 判断是不是主元素（非类元素） */
  public boolean isMainElement() { return mainElement == null && this != FYSIKOS; }

  /** 判断是不是类元素 */
  public boolean isSubElement() { return mainElement != null; }

  public static ElementalsGIM getElementById(String id) {
    for (ElementalsGIM e : values()) {
      if (e.id.equals(id)) return e;
    }
    return null;
  }
}
