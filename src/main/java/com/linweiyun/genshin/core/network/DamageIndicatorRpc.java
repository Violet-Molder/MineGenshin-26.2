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
            boolean italic,
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
                    italic,
                    baseScale,
                    startScale,
                    durationMs
            );
            DamageIndicatorManager.add(indicator);
            DamageIndicatorRenderer.onIndicatorAdded(indicator);
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
            boolean italic,
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
                italic,
                baseScale, startScale,
                durationMs
        );
    }

    /**
     * 方块位置反应飘字 —— 目标位置和出现位置设为同一坐标（方块中心）。
     * 复用现有 RPC 通道。
     */
    public static void sendReactionAtPos(
            ServerPlayer player,
            String text,
            Vec3 pos,
            int topColor,
            int bottomColor,
            byte style,
            boolean italic,
            float baseScale,
            float startScale,
            int durationMs
    ) {
        RPCPacketDistributor.rpcToPlayer(
                player,
                "genshin:damage_indicator",
                pos.x, pos.y, pos.z,
                pos.x, pos.y, pos.z,
                text,
                topColor, bottomColor,
                style,
                italic,
                baseScale, startScale,
                durationMs
        );
    }
}