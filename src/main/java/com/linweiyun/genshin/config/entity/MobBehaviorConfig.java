package com.linweiyun.genshin.config.entity;

import com.linweiyun.genshin.config.util.StringDoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 怪物<b>行为</b>配置 —— 攻击欲望、技能冷却、护盾恢复这些「机制」参数。
 *
 * <p>为什么单独一段而不是散在实体类里：同一个怪物在不同环境里机制强度不同
 * （深渊里的冰史莱姆属性和机制都会强化），把这些做成配置就能一份代码跑两种环境。
 *
 * <p>命名约定：{@code <怪物id>-<参数>}。
 */
public class MobBehaviorConfig {

    // ==================== 大型冰史莱姆 ====================

    /** 全局攻击欲望倍率：越小越懒（乘在每次技能判定的概率上）。 */
    //TEMP
    public static StringDoubleValue SLIME_ATTACK_DESIRE;

    /** 撞击（普通攻击）的触发距离。 */
    //TEMP
    public static StringDoubleValue SLIME_COLLIDE_RANGE;

    /** 冰刺：每次发射几枚。 */
    //TEMP
    public static StringDoubleValue SLIME_SHARD_COUNT;

    /** 冰刺：冷却（秒）。 */
    //TEMP
    public static StringDoubleValue SLIME_SHARD_COOLDOWN;

    /** 冰雾：持续时间（秒）。 */
    //TEMP
    public static StringDoubleValue SLIME_MIST_DURATION;

    /** 冰雾：冷却（秒）。 */
    //TEMP
    public static StringDoubleValue SLIME_MIST_COOLDOWN;

    /** 跃起砸落：冷却（秒）。 */
    //TEMP
    public static StringDoubleValue SLIME_SLAM_COOLDOWN;

    /** 失去护盾后多久开始恢复（秒）。 */
    //TEMP
    public static StringDoubleValue SLIME_SHIELD_RESTORE_SECONDS;

    /** 多久没挨打就开始恢复护盾（秒）—— 比失去护盾更早生效。 */
    //TEMP
    public static StringDoubleValue SLIME_SHIELD_IDLE_RESTORE_SECONDS;

    /** 恢复护盾的抬手时长（秒）。 */
    //TEMP
    public static StringDoubleValue SLIME_SHIELD_CAST_SECONDS;

    /** 护盾值。 */
    //TEMP
    public static StringDoubleValue SLIME_SHIELD_VALUE;

    static void register(ModConfigSpec.Builder builder) {
        builder.push("large-cryo-slime");

        SLIME_ATTACK_DESIRE = StringDoubleValue.defineInRange(
                builder.translation("minegenshin.configuration.slime.attack-desire"),
                "attack-desire", 1.0, 0.0, 10.0);

        SLIME_COLLIDE_RANGE = StringDoubleValue.defineInRange(
                builder.translation("minegenshin.configuration.slime.collide-range"),
                "collide-range", 4.0, 0.0, 32.0);

        SLIME_SHARD_COUNT = StringDoubleValue.defineInRange(
                builder.translation("minegenshin.configuration.slime.shard-count"),
                "shard-count", 3.0, 1.0, 32.0);
        SLIME_SHARD_COOLDOWN = StringDoubleValue.defineInRange(
                builder.translation("minegenshin.configuration.slime.shard-cooldown"),
                "shard-cooldown", 5.0, 0.0, 300.0);

        SLIME_MIST_DURATION = StringDoubleValue.defineInRange(
                builder.translation("minegenshin.configuration.slime.mist-duration"),
                "mist-duration", 5.0, 0.1, 60.0);
        SLIME_MIST_COOLDOWN = StringDoubleValue.defineInRange(
                builder.translation("minegenshin.configuration.slime.mist-cooldown"),
                "mist-cooldown", 7.0, 0.0, 300.0);

        SLIME_SLAM_COOLDOWN = StringDoubleValue.defineInRange(
                builder.translation("minegenshin.configuration.slime.slam-cooldown"),
                "slam-cooldown", 9.0, 0.0, 300.0);

        SLIME_SHIELD_RESTORE_SECONDS = StringDoubleValue.defineInRange(
                builder.translation("minegenshin.configuration.slime.shield-restore-seconds"),
                "shield-restore-seconds", 30.0, 0.0, 600.0);
        SLIME_SHIELD_IDLE_RESTORE_SECONDS = StringDoubleValue.defineInRange(
                builder.translation("minegenshin.configuration.slime.shield-idle-restore-seconds"),
                "shield-idle-restore-seconds", 10.0, 0.0, 600.0);
        SLIME_SHIELD_CAST_SECONDS = StringDoubleValue.defineInRange(
                builder.translation("minegenshin.configuration.slime.shield-cast-seconds"),
                "shield-cast-seconds", 2.0, 0.0, 60.0);
        SLIME_SHIELD_VALUE = StringDoubleValue.defineInRange(
                builder.translation("minegenshin.configuration.slime.shield-value"),
                "shield-value", 8.0, 0.0, 10000.0);

        builder.pop();
    }

    // ==================== 读取（秒 → 刻） ====================

    //TEMP
    public static float attackDesire() {
        return SLIME_ATTACK_DESIRE.getFloat();
    }

    //TEMP
    public static double collideRange() {
        return SLIME_COLLIDE_RANGE.get();
    }

    //TEMP
    public static int shardCount() {
        return Math.max(1, SLIME_SHARD_COUNT.getInt());
    }

    //TEMP
    public static int shardCooldownTicks() {
        return seconds(SLIME_SHARD_COOLDOWN.get());
    }

    //TEMP
    public static int mistDurationTicks() {
        return seconds(SLIME_MIST_DURATION.get());
    }

    //TEMP
    public static int mistCooldownTicks() {
        return seconds(SLIME_MIST_COOLDOWN.get());
    }

    //TEMP
    public static int slamCooldownTicks() {
        return seconds(SLIME_SLAM_COOLDOWN.get());
    }

    //TEMP
    public static int shieldRestoreTicks() {
        return seconds(SLIME_SHIELD_RESTORE_SECONDS.get());
    }

    //TEMP
    public static int shieldIdleRestoreTicks() {
        return seconds(SLIME_SHIELD_IDLE_RESTORE_SECONDS.get());
    }

    //TEMP
    public static int shieldCastTicks() {
        return seconds(SLIME_SHIELD_CAST_SECONDS.get());
    }

    //TEMP
    public static float shieldValue() {
        return SLIME_SHIELD_VALUE.getFloat();
    }

    //TEMP
    private static int seconds(double value) {
        return (int) Math.max(0.0, Math.round(value * 20.0));
    }
}
