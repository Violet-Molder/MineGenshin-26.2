package com.linweiyun.genshin.enums;

public enum CharacterAscendAttribute {
    ATK("攻击力"),
    HP("生命值"),
    DEF("防御力");

    private final String displayName;

    CharacterAscendAttribute(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
