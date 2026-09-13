package com.linweiyun.genshin.core.world;

import com.linweiyun.genshin.Minegenshin;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class TeyvatWorldInvasion extends SavedData {

    public static final SavedDataType<TeyvatWorldInvasion> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Minegenshin.MOD_ID, "teyvat_invasion"),
            TeyvatWorldInvasion::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.BOOL.fieldOf("invaded").forGetter(d -> d.invaded)
            ).apply(instance, TeyvatWorldInvasion::new))
    );

    private boolean invaded;

    public TeyvatWorldInvasion() {
        this(false);
    }

    public TeyvatWorldInvasion(boolean invaded) {
        this.invaded = invaded;
    }

    public boolean isInvaded() {
        return invaded;
    }

    public void setInvaded(boolean invaded) {
        this.invaded = invaded;
        this.setDirty();
    }

    public static TeyvatWorldInvasion get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private static boolean clientInvaded = false;

    public static boolean isClientInvaded() {
        return clientInvaded;
    }

    public static void setClientInvaded(boolean invaded) {
        clientInvaded = invaded;
    }
}