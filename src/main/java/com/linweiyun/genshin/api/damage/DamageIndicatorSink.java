package com.linweiyun.genshin.api.damage;

/**
 * 飘字表现的注入点：客户端在 setup 时安装实现，公共侧只调用 {@link #show}。
 *
 * <p>专用服务器上没有任何实现，{@link #show} 是空操作——这正是把客户端渲染从公共侧摘出去的目的。
 */
public final class DamageIndicatorSink {

    public interface Impl {
        void show(DamageIndicatorData data);
    }

    private static volatile Impl impl;

    private DamageIndicatorSink() {}

    /** 由客户端入口调用（{@code MinegenshinClient.onClientSetup}）。 */
    public static void install(Impl implementation) {
        impl = implementation;
    }

    public static void show(DamageIndicatorData data) {
        Impl target = impl;
        if (target != null) {
            target.show(data);
        }
    }
}
