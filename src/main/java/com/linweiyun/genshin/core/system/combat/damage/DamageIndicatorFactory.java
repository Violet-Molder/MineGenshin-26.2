package com.linweiyun.genshin.core.system.combat.damage;

import com.linweiyun.genshin.config.DamageIndicatorConfig;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.network.DamageIndicatorRpc;
import com.linweiyun.genshin.enums.ElementalReactionType;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * 伤害飘字工厂 —— 所有飘字的统一入口。
 *
 * 三类颜色模式：
 *   - 默认：从 DamageIndicatorConfig 按元素取色
 *   - Custom：从参数读单一十六进制色
 *   - Gradient：从参数读顶部 + 底部两个十六进制色，客户端做垂直渐变
 *
 * 五个飘字类型：
 *   - damage / crit / reaction / heal / text
 */
public final class DamageIndicatorFactory {
    public static final Logger LOGGER = LogUtils.getLogger();

    private DamageIndicatorFactory() {}

    public enum Style {
        NORMAL, CRIT, REACTION, HEAL, TEXT
    }

    public static final class Options {
        public static final float DEFAULT_BASE_SCALE   = 2.2f;
        public static final float DEFAULT_START_SCALE  = 6.2f;
        public static final long  DEFAULT_DURATION_MS  = 950L;

        public static final Options DEFAULT = new Options(
                DEFAULT_BASE_SCALE, DEFAULT_START_SCALE, DEFAULT_DURATION_MS);

        public final float baseScale;
        public final float startScale;
        public final long  durationMs;

        public Options(float baseScale, float startScale, long durationMs) {
            this.baseScale = baseScale;
            this.startScale = startScale;
            this.durationMs = durationMs;
        }

        public static Options of(float baseScale, float startScale, long durationMs) {
            return new Options(baseScale, startScale, durationMs);
        }

        public static Builder builder() { return new Builder(); }

        public static final class Builder {
            private float baseScale = DEFAULT_BASE_SCALE;
            private float startScale = DEFAULT_START_SCALE;
            private long  durationMs = DEFAULT_DURATION_MS;

            public Builder baseScale(float v) { this.baseScale = v; return this; }
            public Builder startScale(float v) { this.startScale = v; return this; }
            public Builder durationMs(long v) { this.durationMs = v; return this; }
            public Options build() { return new Options(baseScale, startScale, durationMs); }
        }
    }

    private static final double BROADCAST_RADIUS     = 48.0;
    private static final double BROADCAST_RADIUS_SQR = BROADCAST_RADIUS * BROADCAST_RADIUS;

    // =====================================================================
    //  1. damage
    // =====================================================================

    /** 普通伤害 —— 默认元素色 */
    public static void damage(LivingEntity target, DamageSource source, float finalDamage, GenshinElement element) {
        damage(target, source, finalDamage, element, Options.DEFAULT);
    }

    public static void damage(LivingEntity target, DamageSource source, float finalDamage, GenshinElement element, Options options) {
        if (source == null) return;
        int color = DamageIndicatorConfig.getColorForElement(element);
        spawnDamage(target, source, finalDamage, color, color, Style.NORMAL, options);
    }

    /** 普通伤害 —— 自定义单色（十六进制） */
    public static void damageCustom(LivingEntity target, DamageSource source, float finalDamage, int color) {
        damageCustom(target, source, finalDamage, color, Options.DEFAULT);
    }

    public static void damageCustom(LivingEntity target, DamageSource source, float finalDamage, int color, Options options) {
        if (source == null) return;
        spawnDamage(target, source, finalDamage, color, color, Style.NORMAL, options);
    }

    /** 普通伤害 —— 渐变色（top → bottom） */
    public static void damageGradient(LivingEntity target, DamageSource source, float finalDamage, int topColor, int bottomColor) {
        damageGradient(target, source, finalDamage, topColor, bottomColor, Options.DEFAULT);
    }

    public static void damageGradient(LivingEntity target, DamageSource source, float finalDamage, int topColor, int bottomColor, Options options) {
        if (source == null) return;
        spawnDamage(target, source, finalDamage, topColor, bottomColor, Style.NORMAL, options);
    }

    // =====================================================================
    //  2. crit
    // =====================================================================

    public static void crit(LivingEntity target, DamageSource source, float finalDamage, GenshinElement element) {
        crit(target, source, finalDamage, element, Options.DEFAULT);
    }

    public static void crit(LivingEntity target, DamageSource source, float finalDamage, GenshinElement element, Options options) {
        if (source == null) return;
        int color = DamageIndicatorConfig.getColorForElement(element);
        spawnDamage(target, source, finalDamage, color, color, Style.CRIT, options);
    }

    public static void critCustom(LivingEntity target, DamageSource source, float finalDamage, int color) {
        critCustom(target, source, finalDamage, color, Options.DEFAULT);
    }

    public static void critCustom(LivingEntity target, DamageSource source, float finalDamage, int color, Options options) {
        if (source == null) return;
        spawnDamage(target, source, finalDamage, color, color, Style.CRIT, options);
    }

    public static void critGradient(LivingEntity target, DamageSource source, float finalDamage, int topColor, int bottomColor) {
        critGradient(target, source, finalDamage, topColor, bottomColor, Options.DEFAULT);
    }

    public static void critGradient(LivingEntity target, DamageSource source, float finalDamage, int topColor, int bottomColor, Options options) {
        if (source == null) return;
        spawnDamage(target, source, finalDamage, topColor, bottomColor, Style.CRIT, options);
    }

    // =====================================================================
    //  3. reaction
    // =====================================================================

    /** 反应飘字（默认白色） */
    public static void reaction(LivingEntity target, ElementalReactionType type) {
        reaction(target, type, Options.DEFAULT);
    }

    public static void reaction(LivingEntity target, ElementalReactionType type, Options options) {
        if (type == null) return;
        int color = DamageIndicatorConfig.getColorForReaction(type);
        spawnRaw(target, null, type.getDisplayName(), color, color, Style.REACTION, options);
    }

    // 从玩家和目标实体中间飘出
    public static void reaction(LivingEntity target, Entity attacker, ElementalReactionType type) {
        reaction(target, attacker, type, Options.DEFAULT);
    }

    public static void reaction(LivingEntity target, Entity attacker, ElementalReactionType type, Options options) {
        if (type == null) return;
        int color = DamageIndicatorConfig.getColorForReaction(type);
        spawnRaw(target, attacker, type.getDisplayName(), color, color, Style.REACTION, options);
    }

    /** 反应飘字（自定义单色） */
    public static void reactionCustom(LivingEntity target, ElementalReactionType type, int color) {
        reactionCustom(target, type, color, Options.DEFAULT);
    }

    public static void reactionCustom(LivingEntity target, ElementalReactionType type, int color, Options options) {
        if (type == null) return;
        spawnRaw(target, null, type.getDisplayName(), color, color, Style.REACTION, options);
    }

    /** 反应飘字（渐变：top → bottom） */
    public static void reactionGradient(LivingEntity target, ElementalReactionType type, int topColor, int bottomColor) {
        reactionGradient(target, type, topColor, bottomColor, Options.DEFAULT);
    }

    public static void reactionGradient(LivingEntity target, ElementalReactionType type, int topColor, int bottomColor, Options options) {
        if (type == null) return;
        spawnRaw(target, null, type.getDisplayName(), topColor, bottomColor, Style.REACTION, options);
    }

    // =====================================================================
    //  4. heal
    // =====================================================================

    public static void heal(LivingEntity target, float amount) {
        heal(target, amount, Options.DEFAULT);
    }

    public static void heal(LivingEntity target, float amount, Options options) {
        int color = DamageIndicatorConfig.getColorForElement(ModElements.DENDRO.get());
        spawnValue(target, amount, color, color, Style.HEAL, options);
    }

    public static void healCustom(LivingEntity target, float amount, int color) {
        healCustom(target, amount, color, Options.DEFAULT);
    }

    public static void healCustom(LivingEntity target, float amount, int color, Options options) {
        spawnValue(target, amount, color, color, Style.HEAL, options);
    }

    public static void healGradient(LivingEntity target, float amount, int topColor, int bottomColor) {
        healGradient(target, amount, topColor, bottomColor, Options.DEFAULT);
    }

    public static void healGradient(LivingEntity target, float amount, int topColor, int bottomColor, Options options) {
        spawnValue(target, amount, topColor, bottomColor, Style.HEAL, options);
    }

    // =====================================================================
    //  5. text  （新增）
    // =====================================================================

    /** 纯文本飘字 —— 默认白色（物理色） */
    public static void text(LivingEntity target, String text) {
        text(target, text, Options.DEFAULT);
    }

    public static void text(LivingEntity target, String text, Options options) {
        int color = DamageIndicatorConfig.getColorForElement(ModElements.FYSIKOS.get());
        spawnRaw(target, null, text, color, color, Style.TEXT, options);
    }

    public static void text(LivingEntity target, Component text) {
        text(target, text, Options.DEFAULT);
    }

    public static void text(LivingEntity target, Component text, Options options) {
        text(target, text == null ? "" : text.getString(), options);
    }

    /** 纯文本 —— 自定义单色 */
    public static void textCustom(LivingEntity target, String text, int color) {
        textCustom(target, text, color, Options.DEFAULT);
    }

    public static void textCustom(LivingEntity target, String text, int color, Options options) {
        spawnRaw(target, null, text, color, color, Style.TEXT, options);
    }

    public static void textCustom(LivingEntity target, Component text, int color) {
        textCustom(target, text, color, Options.DEFAULT);
    }

    public static void textCustom(LivingEntity target, Component text, int color, Options options) {
        textCustom(target, text == null ? "" : text.getString(), color, options);
    }

    /** 纯文本 —— 渐变 */
    public static void textGradient(LivingEntity target, String text, int topColor, int bottomColor) {
        textGradient(target, text, topColor, bottomColor, Options.DEFAULT);
    }

    public static void textGradient(LivingEntity target, String text, int topColor, int bottomColor, Options options) {
        spawnRaw(target, null, text, topColor, bottomColor, Style.TEXT, options);
    }

    public static void textGradient(LivingEntity target, Component text, int topColor, int bottomColor) {
        textGradient(target, text, topColor, bottomColor, Options.DEFAULT);
    }

    public static void textGradient(LivingEntity target, Component text, int topColor, int bottomColor, Options options) {
        textGradient(target, text == null ? "" : text.getString(), topColor, bottomColor, options);
    }

    // =====================================================================
    //  内部汇聚点
    // =====================================================================

    /** 伤害类：从最终伤害取整作为文本 */
    private static void spawnDamage(LivingEntity target, DamageSource source, float finalDamage,
                                    int topColor, int bottomColor, Style style, Options options) {
        String text = String.valueOf(Math.round(finalDamage));
        spawnRaw(target, source == null ? null : source.getEntity(), text, topColor, bottomColor, style, options);
    }

    /** 数值类（反应/治疗）：从数值取整作为文本，无攻击者 */
    private static void spawnValue(LivingEntity target, float value,
                                   int topColor, int bottomColor, Style style, Options options) {
        String text = String.valueOf(Math.round(value));
        spawnRaw(target, null, text, topColor, bottomColor, style, options);
    }

    /** 核心 spawn —— 所有方法最终汇聚到这里 */
    private static void spawnRaw(LivingEntity target, Entity attacker, String text,
                                 int topColor, int bottomColor, Style style, Options options) {
        if (options == null) options = Options.DEFAULT;
        if (target == null) return;
        if (text == null || text.isEmpty()) return;
        if (!(target.level() instanceof ServerLevel level)) return;

        RandomSource rand = level.getRandom();

        // 目标上方（落点中心）
        Vec3 targetCenter = target.position()
                .add(0, target.getBbHeight() * 0.85, 0);

        // 飘字散布范围
        // 反应名统一抬高一点，让它和伤害数字在垂直方向错开
        double extraY = (style == Style.REACTION) ? 0.2 : 0.0;
        Vec3 finalPos = targetCenter.add(
                (rand.nextDouble() - 0.5) * 1.6,
                0.35 + rand.nextDouble() * 0.60 + extraY,
                (rand.nextDouble() - 0.5) * 1.6
        );

        // 出现位置：攻击者身体与目标之间
        Vec3 originPos;
        if (attacker != null && attacker != target && attacker.level() == level) {
            Vec3 attackerCenter = attacker.position()
                    .add(0, attacker.getBbHeight() * 0.7, 0);
            originPos = attackerCenter.lerp(targetCenter, 0.3);
        } else {
            originPos = targetCenter.add(0, 0.6, 0);
        }

        int sent = 0;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(target) <= BROADCAST_RADIUS_SQR) {
                try {
                    DamageIndicatorRpc.sendToPlayer(
                            player,
                            finalPos.x, finalPos.y, finalPos.z,
                            originPos.x, originPos.y, originPos.z,
                            text,
                            topColor, bottomColor,
                            (byte) style.ordinal(),
                            options.baseScale, options.startScale,
                            (int) options.durationMs
                    );
                    sent++;
                } catch (Throwable t) {
                    LOGGER.error("[DI-Factory] sendToPlayer threw", t);
                }
            }
        }
        LOGGER.info("[DI-Factory] spawn() sent to {} players. text='{}' top=0x{} bottom=0x{} style={}",
                sent, text, Integer.toHexString(topColor), Integer.toHexString(bottomColor), style);
    }
}