package com.linweiyun.genshin.core.attachment;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * 一个玩家当前正在播的动作动画状态。
 *
 * <p>对应参考2 的 {@code PlayerVariables.anime_state} + {@code anime_ticks}：本地玩家自己驱动，
 * 其余玩家靠服务端把这份数据同步过来。字段很简单，所以直接交给 LDLib2 的
 * {@code @Persisted} + {@code PersistedParser} 做序列化/同步，不用手写编解码。
 */
public class AnimationState implements IPersistedSerializable {

    /** 常态。与 {@code ActionStateMachine.DEFAULT_STATE} 保持一致。 */
    public static final String DEFAULT_STATE = "default";

    public static final AnimationState DEFAULT = new AnimationState(DEFAULT_STATE, 0);

    @Persisted(key = "state")
    private String state = DEFAULT_STATE;

    @Persisted(key = "total_ticks")
    private int totalTicks;

    public AnimationState() {
    }

    public AnimationState(String state, int totalTicks) {
        this.state = (state == null || state.isEmpty()) ? DEFAULT_STATE : state;
        this.totalTicks = totalTicks;
    }

    public static final Codec<AnimationState> CODEC = PersistedParser.createCodec(AnimationState::new);
    public static final StreamCodec<ByteBuf, AnimationState> STREAM_CODEC =
            PersistedParser.createStreamCodec(AnimationState::new);

    public String state() {
        return state == null || state.isEmpty() ? DEFAULT_STATE : state;
    }

    public int totalTicks() {
        return totalTicks;
    }

    public boolean isDefault() {
        return DEFAULT_STATE.equals(state());
    }
}
