package com.linweiyun.genshin.client.combat.state;

import com.linweiyun.genshin.core.system.combat.animation.action.CharacterActions;
import com.linweiyun.genshin.core.attachment.AnimationState;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 动画状态的读取入口。
 *
 * <p>本地玩家的状态直接读 {@link ActionStateMachine}（按键当帧就更新，0 延迟）；
 * 其他玩家读服务端同步下来的 {@link AttachmentRegistration#ANIMATION_STATE_ATTACHMENT}。
 *
 * <p>对应参考2 里「本地读 {@code ActionStateMachine}，远端读 {@code PlayerVariables.anime_state}」的分工。
 */
public final class AnimationStateSync {

    private AnimationStateSync() {
    }

    /** 某个玩家当前应该播的动作状态名；没有就是常态。 */
    public static String stateOf(Player player) {
        if (player == Minecraft.getInstance().player) {
            return ActionStateMachine.currentState;
        }

        AnimationState state = player.getData(AttachmentRegistration.ANIMATION_STATE_ATTACHMENT);
        if (state == null || state.state() == null || state.state().isEmpty()) {
            return ActionStateMachine.DEFAULT_STATE;
        }
        return state.state();
    }

    // ==================== 远端音效 ====================

    /** 玩家 UUID → 上一次看到的状态名，用来判断「这一 tick 是不是刚切的」。 */
    private static final Map<UUID, String> LAST_SEEN_STATE = new HashMap<>();

    /**
     * 其他玩家切进一个新动作状态时，按同一个状态名查音效并本地播放。
     *
     * <p>参考2 是把音效 id 塞进同步包；这里状态名本身就够查表了，
     * 所以同步包只带状态，音效在各自客户端查 {@code CharacterAnimations.soundForState}。
     */
    public static void tickRemoteStateSounds() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        for (Player other : minecraft.level.players()) {
            if (other == minecraft.player) {
                continue;
            }

            String state = stateOf(other);
            String previous = LAST_SEEN_STATE.put(other.getUUID(), state);

            if (Objects.equals(previous, state) || ActionStateMachine.DEFAULT_STATE.equals(state)) {
                continue;
            }

            ActionStateMachine.playLocalSound(other,
                    CharacterActions.animationsFor(other).soundForState(state));
        }

        // 玩家离开视野后清掉条目，防止内存表无限增长
        if (minecraft.level.players().size() + 4 < LAST_SEEN_STATE.size()) {
            LAST_SEEN_STATE.keySet().removeIf(uuid ->
                    minecraft.level.players().stream().noneMatch(p -> p.getUUID().equals(uuid)));
        }
    }
}
