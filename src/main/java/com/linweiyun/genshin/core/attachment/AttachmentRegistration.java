package com.linweiyun.genshin.core.attachment;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.content.entities.teyvat.TeyvatEntityStats;
import com.linweiyun.genshin.core.system.shield.ShieldState;
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
                    () -> AttachmentType.serializable(PlayerCharactersAttachment::new)
                            .sync(PlayerCharactersAttachment.STREAM_CODEC)
                            .copyOnDeath().build());

    public static final Supplier<AttachmentType<StatusContainer>> CONTAINER =
            ATTACHMENTS.register("status_container",
                    () -> AttachmentType.serializable(StatusContainer::new)
                            .sync(StatusContainer.STREAM_CODEC)
                            .copyOnDeath().build());

    public static final Supplier<AttachmentType<AdventurerInfoAttachment>> ADVENTURER_INFO_ATTACHMENT =
            ATTACHMENTS.register("adventurer_info",
                    () -> AttachmentType.serializable(AdventurerInfoAttachment::new)
                            .sync(AdventurerInfoAttachment.STREAM_CODEC)
                            .copyOnDeath().build());

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

    /**
     * 实体身上的护盾状态（还剩多少盾、什么分类、什么时候到期）。
     *
     * <p>用附件而不是 Attribute：盾除了「量」还有剩余时长、分类、朝向、最近受击刻，
     * Attribute 只能表示一个数。默认真造新实例（不是共享单例），
     * 免得一个实体改盾把别人也改了。
     *
     * <p>{@code sync} 是必须的 —— 客户端血条下面那条护盾条读的就是它。
     */
    //TEMP
    public static final Supplier<AttachmentType<ShieldState>> SHIELD = ATTACHMENTS.register(
            "shield",
            () -> AttachmentType.builder(ShieldState::new)
                    .serialize(ShieldState.CODEC.fieldOf("shield"))
                    .sync(ShieldState.STREAM_CODEC)
                    .build()
    );

    /**
     * 玩家当前动作动画状态（参考2 的 {@code anime_state}）。
     *
     * <p>{@code serialize} + {@code sync} 两个调用就够了：服务端 setData 后调一次
     * {@code syncData}，NeoForge 会把它推给所有能收到这个玩家的客户端（含本人）。
     */
    public static final Supplier<AttachmentType<AnimationState>> ANIMATION_STATE_ATTACHMENT =
            ATTACHMENTS.register(
                    "animation_state",
                    () -> AttachmentType.builder(AnimationState::new)
                            .serialize(AnimationState.CODEC.fieldOf("animation_state"))
                            .sync(AnimationState.STREAM_CODEC)
                            .build()
            );

    public static final Supplier<AttachmentType<LockedTargetData>> LOCKED_TARGET =
            ATTACHMENTS.register("locked_target",
                    () -> AttachmentType.builder(() -> LockedTargetData.EMPTY)
                            .serialize(LockedTargetData.CODEC.fieldOf("locked_target"))
                            .build()
            );

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}