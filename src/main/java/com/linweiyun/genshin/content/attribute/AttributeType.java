package com.linweiyun.genshin.content.attribute;

import com.linweiyun.genshin.Minegenshin;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import net.minecraft.resources.Identifier;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.core.system.registry.ModRegistries;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public class AttributeType implements IPersistedSerializable {
    public static final Codec<AttributeType> CODEC = PersistedParser.createCodec(AttributeType::new);
    public static final StreamCodec<ByteBuf, AttributeType> STREAM_CODEC = PersistedParser.createStreamCodec(AttributeType::new);

    //TEMP 让 PersistedParser 能识别这些字段
    @Persisted(key = "attr_id")
    private Identifier id;

    @Persisted(key = "attr_translation_key")
    private String translationKey;

    @Persisted(key = "attr_default_value")
    private float defaultValue;

    public AttributeType() {}

    public AttributeType(Identifier id, String translationKey, float defaultValue) {
        this.id = id;
        this.translationKey = translationKey;
        this.defaultValue = defaultValue;
    }

    public AttributeType(String path, String translationKey, float defaultValue) {
        this(Identifier.fromNamespaceAndPath(Minegenshin.MOD_ID, path), translationKey, defaultValue);
    }

    public Identifier id() { return id; }
    public String translationKey() { return translationKey; }
    public float defaultValue() { return defaultValue; }

    //TEMP 通过注册表按 Identifier 反查已注册的 AttributeType 单例（避免重复实例）
    //TEMP 通过注册表按 Identifier 反查已注册的 AttributeType 单例
    public static AttributeType byId(Identifier id) {
        return ModRegistries.ATTRIBUTE_TYPE_REGISTRY.getValue(id);
    }
}