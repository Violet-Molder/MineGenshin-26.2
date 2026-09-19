package com.linweiyun.genshin.core.system.combat.action;

public enum InterruptReason {
    JUMP,             // 玩家跳跃（主动，可打断前摇）
    KNOCKBACK,        // 被击退
    DAMAGE,           // 受到伤害
    SWITCH_CHARACTER, // 切换角色（无视阶段强制打断）
    DEATH,            // 死亡（无视阶段强制打断）
    MANUAL            // 手动/程序触发
}