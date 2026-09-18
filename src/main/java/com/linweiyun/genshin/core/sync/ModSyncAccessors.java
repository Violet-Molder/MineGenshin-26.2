package com.linweiyun.genshin.core.sync;

import com.linweiyun.genshin.core.character.PGCharacter;
import com.lowdragmc.lowdraglib2.syncdata.AccessorRegistries;
import com.lowdragmc.lowdraglib2.syncdata.accessor.direct.CustomDirectAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public final class ModSyncAccessors {

    private static boolean registered = false;

    public static void register() {
        if (registered) return;
        registered = true;
        registerLivingEntityAccessor();
        registerPGCharacterAccessor();
    }

    private static HolderLookup.Provider getRegistryAccess() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) return server.registryAccess();
        var level = Minecraft.getInstance().level;
        if (level != null) return level.registryAccess();
        throw new IllegalStateException("No registry access available");
    }

    private static void registerLivingEntityAccessor() {
        var streamCodec = new StreamCodec<RegistryFriendlyByteBuf, LivingEntity>() {
            @Override
            public LivingEntity decode(RegistryFriendlyByteBuf buf) {
                int entityId = buf.readVarInt();
                var level = Minecraft.getInstance().level;
                if (level == null) return null;
                var entity = level.getEntity(entityId);
                return entity instanceof LivingEntity living ? living : null;
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, LivingEntity entity) {
                buf.writeVarInt(entity.getId());
            }
        };

        AccessorRegistries.registerAccessor(
                CustomDirectAccessor.builder(LivingEntity.class, true)
                        .codec(UUIDUtil.CODEC.xmap(
                                uuid -> {
                                    var server = ServerLifecycleHooks.getCurrentServer();
                                    if (server == null) return null;
                                    for (var level : server.getAllLevels()) {
                                        var entity = level.getEntity(uuid);
                                        if (entity instanceof LivingEntity living) {
                                            return living;
                                        }
                                    }
                                    return null;
                                },
                                entity -> entity.getUUID()
                        ))
                        .streamCodec(streamCodec)
                        .customMark(
                                LivingEntity::getUUID,
                                (uuid, entity) -> entity != null && uuid.equals(entity.getUUID())
                        )
                        .build(),
                100
        );
    }

    private static void registerPGCharacterAccessor() {
        var streamCodec = new StreamCodec<RegistryFriendlyByteBuf, PGCharacter>() {
            @Override
            public PGCharacter decode(RegistryFriendlyByteBuf buf) {
                var tag = buf.readNbt();
                if (tag == null || tag.isEmpty()) return null;
                var access = getRegistryAccess();
                var c = new PGCharacter();
                c.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, access, tag));
                return c;
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, PGCharacter c) {
                if (c == null) {
                    buf.writeNbt(null);
                    return;
                }
                var access = getRegistryAccess();
                var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, access);
                c.serialize(output);
                buf.writeNbt(output.buildResult());
            }
        };

        AccessorRegistries.registerAccessor(
                CustomDirectAccessor.builder(PGCharacter.class, true)
                        .codec(CompoundTag.CODEC.xmap(
                                tag -> {
                                    if (tag.isEmpty()) return null;
                                    var access = getRegistryAccess();
                                    var c = new PGCharacter();
                                    c.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, access, tag));
                                    return c;
                                },
                                c -> {
                                    if (c == null) return new CompoundTag();
                                    var access = getRegistryAccess();
                                    var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, access);
                                    c.serialize(output);
                                    return output.buildResult();
                                }
                        ))
                        .streamCodec(streamCodec)
                        .codecMark()
                        .build(),
                100
        );
    }
}