package com.linweiyun.genshin.core.attribute;

import com.linweiyun.genshin.Minegenshin;
import net.minecraft.resources.Identifier;

public record AttributeType(Identifier id, String translationKey, float defaultValue) {

    public AttributeType(String path, String translationKey, float defaultValue) {
        this(Identifier.fromNamespaceAndPath(Minegenshin.MOD_ID, path), translationKey, defaultValue);
    }
}
