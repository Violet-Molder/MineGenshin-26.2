package com.linweiyun.genshin.client.action;

import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.combat.action.ActionDefinition;
import com.linweiyun.genshin.core.system.combat.action.ActionSet;
import net.minecraft.world.entity.player.Player;

/**
 * 客户端本地动作状态。
 * <ul>
 *   <li><b>移动锁</b>：前摇 + 执行期锁 WASD。</li>
 *   <li><b>输入阻塞</b>：前摇/执行期拒绝非连招输入（E/Q）。</li>
 * </ul>
 * swing 由服务端 RPC 驱动（{@code ActionServer.actionSwingRPCPacket}）。
 */
public final class ClientActionLock {

    private ClientActionLock() {}

    private static int movementLockTicks = 0;

    public static void lock(int precastTicks, int activeTicks) {
        int total = Math.max(0, precastTicks) + Math.max(1, activeTicks);
        movementLockTicks = Math.max(movementLockTicks, total);
    }

    public static void tick() {
        if (movementLockTicks > 0) movementLockTicks--;
    }

    public static boolean isMovementBlocked() {
        return movementLockTicks > 0;
    }

    public static boolean isActionInputBlocked() {
        return movementLockTicks > 0;
    }

    public static void clear() {
        movementLockTicks = 0;
    }

    public static void lockForNormalAttack(Player player, PGCharacter character) {
        ActionDefinition def = firstNormalAttack(player, character);
        if (def != null) lock(def.precastTicks, def.activeTicks);
    }

    public static void lockForChargedAttack(Player player, PGCharacter character) {
        ActionDefinition def = chargedAttack(player, character);
        if (def != null) lock(def.precastTicks, def.activeTicks);
    }

    public static void lockForSkill(Player player, PGCharacter character, boolean longPress) {
        ActionDefinition def = skillDef(player, character, longPress);
        if (def != null) lock(def.precastTicks, def.activeTicks);
    }

    public static void lockForBurst(Player player, PGCharacter character) {
        ActionDefinition def = burstDef(player, character);
        if (def != null) lock(def.precastTicks, def.activeTicks);
    }

    private static ActionDefinition firstNormalAttack(Player player, PGCharacter character) {
        ActionSet set = character.getActionSet(player);
        return set != null ? set.getNormalAttack(1) : null;
    }

    private static ActionDefinition chargedAttack(Player player, PGCharacter character) {
        ActionSet set = character.getActionSet(player);
        return set != null ? set.getChargedAttack() : null;
    }

    private static ActionDefinition skillDef(Player player, PGCharacter character, boolean longPress) {
        ActionSet set = character.getActionSet(player);
        if (set == null) return null;
        return longPress ? set.getElementalSkillHold() : set.getElementalSkillTap();
    }

    private static ActionDefinition burstDef(Player player, PGCharacter character) {
        ActionSet set = character.getActionSet(player);
        return set != null ? set.getElementalBurst() : null;
    }
}