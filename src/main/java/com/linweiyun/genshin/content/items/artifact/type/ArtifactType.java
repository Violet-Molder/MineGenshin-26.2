package com.linweiyun.genshin.content.items.artifact.type;

public enum ArtifactType {
    FLOWER("artifact.minegenshin.type.flower"),
    PLUME("artifact.minegenshin.type.plume"),
    SANDS("artifact.minegenshin.type.sands"),
    GOBLET("artifact.minegenshin.type.goblet"),
    CIRCLET("artifact.minegenshin.type.circlet");



    private final String translationKey;
    ArtifactType(String translationKey) {
        this.translationKey = translationKey;
    }
    public String getTranslationKey() {
        return translationKey;
    }
}
