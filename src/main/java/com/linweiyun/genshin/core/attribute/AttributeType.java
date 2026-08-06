package com.linweiyun.genshin.core.attribute;

import com.linweiyun.genshin.Minegenshin;
import net.minecraft.resources.Identifier;

public class AttributeType {
    private final Identifier id;
    private final String translationKey;
    private final double defaultValue;

    public AttributeType(Identifier id, String translationKey, double defaultValue) {
        this.id = id;
        this.translationKey = translationKey;
        this.defaultValue = defaultValue;
    }

    public AttributeType(String path, String translationKey, double defaultValue) {
        this(Identifier.fromNamespaceAndPath(Minegenshin.MOD_ID, path), translationKey, defaultValue);
    }

    public Identifier getId() { return id; }
    public String getTranslationKey() { return translationKey; }
    public double getDefaultValue() { return defaultValue; }
}
