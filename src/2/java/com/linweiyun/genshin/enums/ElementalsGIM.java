package com.linweiyun.genshin.enums;

public enum ElementalsGIM {
  FYSIKOS("fysikos", "elemental.gim.fysikos"),
  PYRO("pyro", "elemental.gim.pyro"),
  HYDRO("hydro", "elemental.gim.hydro"),
  ANEMO("anemo", "elemental.gim.anemo"),
  ELECTRO("electro", "elemental.gim.electro"),
  DENDRO("dendro", "elemental.gim.dendro"),
  CYRO("cyro", "elemental.gim.cyro"),
  GEO("geo", "elemental.gim.geo");

  private final String id;
  private final String translationKey;

  ElementalsGIM(String id, String translationKey) {
    this.id = id;
    this.translationKey = translationKey;
  }

  public String getId() {
    return id;
  }

  public String getTranslationKey() {
    return translationKey;
  }

  public static ElementalsGIM getElementById(String id) {
    for (ElementalsGIM element : ElementalsGIM.values()) {
      if (element.getId().equals(id)) {
        return element;
      }
    }
    return null;
  }
}
