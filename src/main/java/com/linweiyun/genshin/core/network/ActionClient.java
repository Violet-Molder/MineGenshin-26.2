package com.linweiyun.genshin.core.network;

import com.linweiyun.genshin.core.system.combat.action.ClientActionStateMachine;

/**
 * 动作系统 —— 客户端 RPC 入口。
 * <p>
 * 接收服务端推送的动画名，转发到 ClientActionStateMachine 驱动 GeckoLib 动画。
 */
public final class ActionClient {

    /**
     * 服务端通知：播放指定动画。
     * 由 {@code ActionServer.syncAnimationRPCPacket} 调用。
     */
    public static void syncAnimationClientHandler(String animationName) {
        if (animationName == null || animationName.isEmpty()) return;
        ClientActionStateMachine.forceRequest(animationName);
    }
}