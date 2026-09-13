package com.linweiyun.genshin.core.attachment;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.content.entities.teyvat.TeyvatEntityStats;
import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class AttachmentRegistration {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Minegenshin.MOD_ID);
    public static final Supplier<AttachmentType<Integer>> PRIMOGEM_ATTACHMENT =
            ATTACHMENTS.register("player_primogem",
                    () -> AttachmentType.builder(() -> 0)
                            .serialize(Codec.INT.fieldOf("primogem"))
                            .sync(StreamCodec.of(
                                    FriendlyByteBuf::writeInt,
                                    FriendlyByteBuf::readInt
                            ))
                            .copyOnDeath()
                            .build()
            );
    public static final Supplier<AttachmentType<Boolean>> GENSHIN_MODE_ATTACHMENT =
            ATTACHMENTS.register("player_genshin_mode",
                    () -> AttachmentType.builder(() -> false)
                            .serialize(Codec.BOOL.fieldOf("genshin_mode"))
                            .sync(StreamCodec.of(
                                    FriendlyByteBuf::writeBoolean,
                                    FriendlyByteBuf::readBoolean
                            ))
                            .copyOnDeath()
                            .build()
            );


    public static final Supplier<AttachmentType<PlayerCharactersAttachment>>
            PLAYER_CHARACTERS_ATTACHMENT =
            ATTACHMENTS.register(
                    "player_characters",
                    () -> AttachmentType.serializable(PlayerCharactersAttachment::new).copyOnDeath().build());

    public static final Supplier<AttachmentType<StatusContainer>> CONTAINER =
            ATTACHMENTS.register("status_container",
                    () -> AttachmentType.serializable(StatusContainer::new).copyOnDeath().build());

    public static final Supplier<AttachmentType<AdventurerInfoAttachment>> ADVENTURER_INFO_ATTACHMENT =
            ATTACHMENTS.register("adventurer_info",
                    () -> AttachmentType.serializable(AdventurerInfoAttachment::new).copyOnDeath().build());

    public static final Supplier<AttachmentType<Backpack>> BACKPACK_ATTACHMENT =
            ATTACHMENTS.register(
                    "backpack",
                    () -> AttachmentType.serializable(Backpack::new)
                            .sync(Backpack.STREAM_CODEC)
                            .copyOnDeath().build()
            );

    public static final Supplier<AttachmentType<TeyvatEntityStats>> ENTITY_STATS = ATTACHMENTS.register(
            "entity_stats",
            () -> AttachmentType.builder(() -> TeyvatEntityStats.DEFAULT)
                    .serialize(TeyvatEntityStats.CODEC.fieldOf("entity_stats"))
                    .sync(TeyvatEntityStats.STREAM_CODEC)
                    .copyOnDeath()
                    .build()
    );

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}