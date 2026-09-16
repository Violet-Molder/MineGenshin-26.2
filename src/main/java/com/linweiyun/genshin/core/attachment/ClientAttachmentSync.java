package com.linweiyun.genshin.core.attachment;

import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

public final class ClientAttachmentSync {
    private static Supplier<Player> clientPlayerSupplier = () -> {
        throw new IllegalStateException(
            "Client player supplier not initialized - are you on a dedicated server?");
    };

    private ClientAttachmentSync() {}

    public static void init(Supplier<Player> supplier) {
        clientPlayerSupplier = supplier;
    }

    public static Player getClientPlayer() {
        return clientPlayerSupplier.get();
    }
}
