// package com.linweiyun.genshin;
//
// import com.google.common.reflect.TypeToken;
// import com.google.gson.Gson;
// import com.linweiyun.genshin.api.GsonMapCodecs;
// import com.linweiyun.genshin.core.attributes.AttributeGIM;
// import com.linweiyun.genshin.content.items.character.data.PlayerCharacterData;
// import com.lowdragmc.lowdraglib2.plugin.ILDLibPlugin;
// import com.lowdragmc.lowdraglib2.syncdata.AccessorRegistries;
// import com.lowdragmc.lowdraglib2.syncdata.accessor.direct.CustomDirectAccessor;
// import com.mojang.serialization.Codec;
// import io.netty.buffer.ByteBuf;
// import net.minecraft.network.codec.ByteBufCodecs;
// import net.minecraft.network.codec.StreamCodec;
// import net.minecraft.util.ExtraCodecs;
//
// import java.lang.reflect.Type;
// import java.util.HashMap;
// import java.util.Map;
//
// import static com.lowdragmc.lowdraglib2.LDLib2.GSON;
//
// public class LDLibPluginGIM implements ILDLibPlugin {
//    private static final Gson GSON = new Gson();
//    private static final TypeToken<Map<String, Integer>> INTEGER_MAP_TYPE = new
// TypeToken<Map<String, Integer>>() {};
//    private static final TypeToken<Map<String, Float>> FLOAT_MAP_TYPE = new TypeToken<Map<String,
// Float>>() {};
//    private static final TypeToken<Map<String, String>> STRING_MAP_TYPE = new
// TypeToken<Map<String, String>>() {};
//
//    @Override
//    public void onLoad() {
//        AccessorRegistries.registerAccessor(
//                CustomDirectAccessor.builder(AttributeGIM.class)
//                        .codec(AttributeGIM.CODEC)
//                        .streamCodec(AttributeGIM.STREAM_CODEC)
//                        .customMark(AttributeGIM::new, AttributeGIM::equals)  // 使用 copyMark
// 构造函数复制
//                        .build()
//        );
//
//        // 注册 PlayerCharacterData 类型支持
//        AccessorRegistries.registerAccessor(
//                CustomDirectAccessor.builder(PlayerCharacterData.class)
//                        .codec(PlayerCharacterData.CODEC)
//                        .streamCodec(PlayerCharacterData.STREAM_CODEC)
//                        .customMark(PlayerCharacterData::new, PlayerCharacterData::equals)  // 使用
// copyMark 构造函数复制
//                        .build()
//        );
//
//
//
//        AccessorRegistries.registerAccessor(
//                CustomDirectAccessor.builder(Map.class)
//                        .codec(GsonMapCodecs.MAP_CODEC)        // 类型完全一致：Codec<Map>
//                        .streamCodec(GsonMapCodecs.MAP_STREAM_CODEC)
//                        .copyMark(HashMap::new)
//                        .build()
//
//        );
//
//
//    }
// }
