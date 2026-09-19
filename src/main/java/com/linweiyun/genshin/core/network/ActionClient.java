package com.linweiyun.genshin.core.network;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

/**
 * 动作系统 —— 客户端 RPC 入口。
 * <p>
 * 目前动作系统是纯"客户端请求 → 服务端权威执行"模式，无需服务端向客户端广播，
 * 因此暂时没有任何 {@code @RPCPacket} 方法。
 * <p>
 * 将来若需要（例如：服务端通知客户端播放某动作动画 / 特效），
 * 在此添加 {@code @RPCPacket} 并只处理 {@code sender.isServer() == true} 分支。
 */
public final class ActionClient {

    /**
     * 服务端通知：播放挥剑动画。
     * 由 {@code ActionServer.actionSwingRPCPacket} 调用。
     */
    public static void actionSwingClientHandler() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !mc.player.isRemoved()) {
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }
}