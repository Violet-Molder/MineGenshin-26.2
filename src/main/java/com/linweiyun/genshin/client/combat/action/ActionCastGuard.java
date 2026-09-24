package com.linweiyun.genshin.client.combat.action;

import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.combat.action.ActionKind;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 出手前置校验 —— 播动画<b>之前</b>问一次「这一招现在放得出来吗」，并给失败的玩家一条限流提示。
 *
 * <p>从 {@link ResourceDrivenActionHandler} 抽出：这一组只依赖 {@link PGCharacter#canCast} 的只读同步数据
 * 与玩家 UUID，和动作播放、索敌、音效都无耦合。
 */
public final class ActionCastGuard {

    /** 提示节流：长按会每刻重试，不节流会刷屏。 */
    private static final int FAIL_MESSAGE_COOLDOWN_TICKS = 20;

    private static final Map<UUID, Long> LAST_FAIL_MESSAGE = new HashMap<>();

    private ActionCastGuard() {
    }

    /**
     * 播动画<b>之前</b>问一次「这一招现在放得出来吗」。
     *
     * <p>不过就什么都不做（连动画都不播），只给一条提示 —— 这是「动画 → 逻辑」这个顺序
     * 唯一的例外口子：凡是服务端会拒绝的条件（能量不够、CD 没好、姿态不对…），
     * 都必须在客户端先用同一套判断拦下来，否则玩家看到的是「我放了，但没效果」。
     *
     * <p>判断本身在 {@link PGCharacter#canCast}（双端可用、只读同步数据），
     * 服务端那边 {@code ActionManager} 现在也走同一个方法 —— 一处规则，两端一致。
     */
    public static boolean canCast(Player player, PGCharacter character, ActionKind kind, int skillTime) {
        if (character.canCast(player, kind, skillTime)) {
            return true;
        }

        long now = player.level().getGameTime();
        Long last = LAST_FAIL_MESSAGE.get(player.getUUID());
        if (last == null || now - last >= FAIL_MESSAGE_COOLDOWN_TICKS) {
            LAST_FAIL_MESSAGE.put(player.getUUID(), now);
            character.sendCastFailedMessage(player, kind);
        }
        return false;
    }
}
