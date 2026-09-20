package com.linweiyun.genshin.core.system.combat.action;

import com.linweiyun.genshin.core.character.PGCharacter;
import lombok.Getter;
import net.minecraft.world.entity.player.Player;

/**
 * 动作运行时上下文。所有 hook 都能读到当前 tick 数和状态。
 */
public class ActionContext {
    public final Player player;
    public final PGCharacter character;
    public final ActionDefinition definition;
    public final long startTick;

    @Getter
    private int totalElapsed;
    @Getter
    private boolean interrupted;
    @Getter
    private InterruptReason interruptReason;

    public ActionContext(Player player, PGCharacter character, ActionDefinition definition) {
        this.player = player;
        this.character = character;
        this.definition = definition;
        this.startTick = player.level().getGameTime();
    }

    void tickTotal()                         { totalElapsed++; }
    void markInterrupted(InterruptReason r)  { interrupted = true; interruptReason = r; }
}