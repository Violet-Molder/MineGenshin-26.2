package com.linweiyun.genshin.core.system.combat.action;

public enum InterruptReason {
    JUMP,             // 玩家跳跃（主动；能打断准备阶段 / 后摇，执行期不行）
    KNOCKBACK,        // 被击退
    DAMAGE,           // 受到伤害
    SWITCH_CHARACTER, // 切换角色（无视阶段强制打断）
    DEATH,            // 死亡（无视阶段强制打断）
    MANUAL            // 手动/程序触发
}