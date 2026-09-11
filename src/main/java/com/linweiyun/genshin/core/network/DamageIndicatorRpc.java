package com.linweiyun.genshin.core.network;

import com.linweiyun.genshin.client.damage.DamageIndicator;
import com.linweiyun.genshin.client.damage.DamageIndicatorManager;
import com.linweiyun.genshin.client.damage.DamageIndicatorRenderer;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public final class DamageIndicatorRpc {
    public static final Logger LOGGER = LogUtils.getLogger();

    private DamageIndicatorRpc() {}

    @RPCPacket("genshin:damage_indicator")
    public static void damageIndicatorRPCPacket(
            RPCSender sender,
            double targetX, double targetY, double targetZ,
            double originX, double originY, double originZ,
            String text,
            int topColor,
            int bottomColor,
            byte style,
            float baseScale,
            float startScale,
            int durationMs
    ) {
        if (sender.isServer()) {
            DamageIndicator indicator = new DamageIndicator(
                    new Vec3(originX, originY, originZ),
                    new Vec3(targetX, targetY, targetZ),
                    text,
                    topColor,
                    bottomColor,
                    style,
                    baseScale,
                    startScale,
                    durationMs
            );
            DamageIndicatorManager.add(indicator);
            DamageIndicatorRenderer.onIndicatorAdded(indicator);
        } else {
            LOGGER.warn("[DI-RPC] packet came from client side, ignoring");
        }
    }

    public static void sendToPlayer(
            ServerPlayer player,
            double targetX, double targetY, double targetZ,
            double originX, double originY, double originZ,
            String text,
            int topColor,
            int bottomColor,
            byte style,
            float baseScale,
            float startScale,
            int durationMs
    ) {
        RPCPacketDistributor.rpcToPlayer(
                player,
                "genshin:damage_indicator",
                targetX, targetY, targetZ,
                originX, originY, originZ,
                text,
                topColor, bottomColor,
                style,
                baseScale, startScale,
                durationMs
        );
    }
}