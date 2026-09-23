package com.linweiyun.genshin.core.system.combat.damage;

import com.linweiyun.genshin.config.WorldTextColorConfig;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.network.DamageIndicatorRpc;
import com.linweiyun.genshin.enums.ElementalReactionType;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    private static final Map<UUID, Vec3> LAST_SPAWN_POS = new HashMap<>();

    private DamageIndicatorFactory() {}

    public static int getColorForElement(GenshinElement element) {
        if (element == ModElements.PYRO.get()) return parseColor(WorldTextColorConfig.PYRO_COLOR.get());
        if (element == ModElements.HYDRO.get()) return parseColor(WorldTextColorConfig.HYDRO_COLOR.get());
        if (element == ModElements.DENDRO.get()) return parseColor(WorldTextColorConfig.DENDRO_COLOR.get());
        if (element == ModElements.ELECTRO.get()) return parseColor(WorldTextColorConfig.ELECTRO_COLOR.get());
        if (element == ModElements.ANEMO.get()) return parseColor(WorldTextColorConfig.ANEMO_COLOR.get());
        if (element == ModElements.CYRO.get()) return parseColor(WorldTextColorConfig.CYRO_COLOR.get());
        if (element == ModElements.FROZEN.get()) return parseColor(WorldTextColorConfig.FROZEN_COLOR.get());
        if (element == ModElements.GEO.get()) return parseColor(WorldTextColorConfig.GEO_COLOR.get());
        return parseColor(WorldTextColorConfig.PHYSICAL_COLOR.get());
    }

    public static int getColorForReaction(ElementalReactionType type) {
        return switch (type) {
            case ELECTRO_CHARGED, LUNAR_CHARGED -> parseColor(WorldTextColorConfig.ELECTRO_CHARGED_COLOR.get());
            case SWIRL -> parseColor(WorldTextColorConfig.SWIRL_COLOR.get());
            case FROZEN -> parseColor(WorldTextColorConfig.FROZEN_COLOR.get());
            case STELLAR_SWIRL_WIND, STELLAR_SWIRL_ICE -> parseColor(WorldTextColorConfig.STELLAR_BOTTOM_WIND_COLOR.get());
            default -> parseColor(WorldTextColorConfig.VAPORIZE_COLOR.get());
        };
    }

    public static int getLunarTopColor() {
        return parseColor(WorldTextColorConfig.LUNAR_TOP_COLOR.get());
    }

    private static int parseColor(String hex) {
        try {
            return Integer.decode(hex.startsWith("#") ? hex : "#" + hex);
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }

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

    public static void damage(LivingEntity target, DamageSource source, float finalDamage, GenshinElement element) {
        damage(target, source, finalDamage, element, Options.DEFAULT);
    }

    public static void damage(LivingEntity target, DamageSource source, float finalDamage, GenshinElement element, Options options) {
        if (source == null) return;
        int color = getColorForElement(element);
        spawnDamage(target, source, finalDamage, color, color, Style.NORMAL, options);
    }

    public static void damageCustom(LivingEntity target, DamageSource source, float finalDamage, int color) {
        damageCustom(target, source, finalDamage, color, Options.DEFAULT);
    }

    public static void damageCustom(LivingEntity target, DamageSource source, float finalDamage, int color, Options options) {
        if (source == null) return;
        spawnDamage(target, source, finalDamage, color, color, Style.NORMAL, options);
    }

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
        int color = getColorForElement(element);
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

    public static void reaction(LivingEntity target, ElementalReactionType type) {
        reaction(target, type, Options.DEFAULT);
    }

    public static void reaction(LivingEntity target, ElementalReactionType type, Options options) {
        if (type == null) return;
        int color = getColorForReaction(type);
        spawnRaw(target, null, type.getDisplayName(), color, color, Style.REACTION, options);
    }

    public static void reaction(LivingEntity target, Entity attacker, ElementalReactionType type) {
        reaction(target, attacker, type, Options.DEFAULT);
    }

    public static void reaction(LivingEntity target, Entity attacker, ElementalReactionType type, Options options) {
        if (type == null) return;
        int color = getColorForReaction(type);
        spawnRaw(target, attacker, type.getDisplayName(), color, color, Style.REACTION, options);
    }

    public static void reactionCustom(LivingEntity target, ElementalReactionType type, int color) {
        reactionCustom(target, type, color, Options.DEFAULT);
    }

    public static void reactionCustom(LivingEntity target, ElementalReactionType type, int color, Options options) {
        if (type == null) return;
        spawnRaw(target, null, type.getDisplayName(), color, color, Style.REACTION, options);
    }

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
        int color = getColorForElement(ModElements.DENDRO.get());
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
    //  5. text
    // =====================================================================

    public static void text(LivingEntity target, String text) {
        text(target, text, Options.DEFAULT);
    }

    public static void text(LivingEntity target, String text, Options options) {
        int color = getColorForElement(ModElements.FYSIKOS.get());
        spawnRaw(target, null, text, color, color, Style.TEXT, options);
    }

    public static void text(LivingEntity target, Component text) {
        text(target, text, Options.DEFAULT);
    }

    public static void text(LivingEntity target, Component text, Options options) {
        text(target, text == null ? "" : text.getString(), options);
    }

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
    //  6. 方块位置反应飘字
    // =====================================================================

    /**
     * 在方块位置显示反应飘字。
     * 以方块中心为锚点，target 和 origin 设为同一位置，复用现有 RPC 通道。
     */
    public static void reactionAtBlock(ServerLevel level, BlockPos pos,
                                       ElementalReactionType type) {
        if (type == null) return;
        int color = getColorForReaction(type);
        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center) <= BROADCAST_RADIUS_SQR) {
                DamageIndicatorRpc.sendReactionAtPos(
                        player,
                        type.getDisplayName(),
                        center,
                        color, color,
                        (byte) Style.REACTION.ordinal(),
                        false,
                        Options.DEFAULT.baseScale, Options.DEFAULT.startScale,
                        (int) Options.DEFAULT.durationMs
                );
            }
        }
    }

    // =====================================================================
    //  内部汇聚点
    // =====================================================================

    private static void spawnDamage(LivingEntity target, DamageSource source, float finalDamage,
                                    int topColor, int bottomColor, Style style, Options options) {
        String text = String.valueOf(Math.round(finalDamage));
        spawnRaw(target, source == null ? null : source.getEntity(), text, topColor, bottomColor, style, options);
    }

    private static void spawnValue(LivingEntity target, float value,
                                   int topColor, int bottomColor, Style style, Options options) {
        String text = String.valueOf(Math.round(value));
        spawnRaw(target, null, text, topColor, bottomColor, style, options);
    }

    private static void spawnRaw(LivingEntity target, Entity attacker, String text,
                                 int topColor, int bottomColor, Style style, Options options,
                                 boolean italic) {
        spawnRawInternal(target, attacker, text, topColor, bottomColor, style, options, italic);
    }

    private static void spawnRaw(LivingEntity target, Entity attacker, String text,
                                 int topColor, int bottomColor, Style style, Options options) {
        spawnRawInternal(target, attacker, text, topColor, bottomColor, style, options, false);
    }

    private static void spawnRawInternal(LivingEntity target, Entity attacker, String text,
                                         int topColor, int bottomColor, Style style, Options options,
                                         boolean italic) {
        if (options == null) options = Options.DEFAULT;
        if (target == null) return;
        if (text == null || text.isEmpty()) return;
        if (!(target.level() instanceof ServerLevel level)) return;

        RandomSource rand = level.getRandom();

        Vec3 targetCenter = target.position()
                .add(0, target.getBbHeight() * 0.85, 0);

        double extraY = (style == Style.REACTION) ? 0.6 : 0.0;
        double spreadH = 1.5;
        double verticalBase = 0.35;
        double verticalRange = 0.6;

        Vec3 finalPos = null;
        int maxRetries = 8;
        UUID targetId = target.getUUID();
        Vec3 lastPos = LAST_SPAWN_POS.get(targetId);

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            Vec3 candidate = targetCenter.add(
                    (rand.nextDouble() - 0.5) * spreadH,
                    verticalBase + rand.nextDouble() * verticalRange + extraY,
                    (rand.nextDouble() - 0.5) * spreadH
            );
            if (lastPos == null || lastPos.distanceToSqr(candidate) > 0.25) {
                finalPos = candidate;
                LAST_SPAWN_POS.put(targetId, finalPos);
                break;
            }
        }
        if (finalPos == null) {
            finalPos = targetCenter.add(
                    (rand.nextDouble() - 0.5) * spreadH,
                    verticalBase + rand.nextDouble() * verticalRange + extraY,
                    (rand.nextDouble() - 0.5) * spreadH
            );
            LAST_SPAWN_POS.put(targetId, finalPos);
        }

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
                            italic,
                            options.baseScale, options.startScale,
                            (int) options.durationMs
                    );
                    sent++;
                } catch (Throwable t) {
                    LOGGER.error("[DI-Factory] sendToPlayer threw", t);
                }
            }
        }
    }

    // =====================================================================
    //  月感电 / 星扩散 —— 渐变 + 斜体
    // =====================================================================

    private static final int WHITE = 0xFFFFFF;

    public static void lunarDamageGradient(LivingEntity target, DamageSource source, float finalDamage) {
        lunarDamageGradient(target, source, finalDamage, Options.DEFAULT);
    }

    public static void lunarDamageGradient(LivingEntity target, DamageSource source, float finalDamage, Options options) {
        if (source == null) return;
        int topColor = getLunarTopColor();
        spawnRawInternal(target, source.getEntity(), String.valueOf(Math.round(finalDamage)),
                topColor, WHITE, Style.NORMAL, options, true);
    }

    public static void lunarReactionGradient(LivingEntity target, ElementalReactionType type) {
        lunarReactionGradient(target, type, Options.DEFAULT);
    }

    public static void lunarReactionGradient(LivingEntity target, ElementalReactionType type, Options options) {
        if (type == null) return;
        int topColor = getLunarTopColor();
        spawnRawInternal(target, null, type.getDisplayName(),
                topColor, WHITE, Style.REACTION, options, true);
    }

    public static void stellarWindDamageGradient(LivingEntity target, DamageSource source, float finalDamage) {
        stellarWindDamageGradient(target, source, finalDamage, Options.DEFAULT);
    }

    public static void stellarWindDamageGradient(LivingEntity target, DamageSource source, float finalDamage, Options options) {
        if (source == null) return;
        int bottomColor = parseColor(WorldTextColorConfig.STELLAR_BOTTOM_WIND_COLOR.get());
        spawnRawInternal(target, source.getEntity(), String.valueOf(Math.round(finalDamage)),
                WHITE, bottomColor, Style.NORMAL, options, true);
    }

    public static void stellarIceDamageGradient(LivingEntity target, DamageSource source, float finalDamage) {
        stellarIceDamageGradient(target, source, finalDamage, Options.DEFAULT);
    }

    public static void stellarIceDamageGradient(LivingEntity target, DamageSource source, float finalDamage, Options options) {
        if (source == null) return;
        int bottomColor = parseColor(WorldTextColorConfig.STELLAR_BOTTOM_ICE_COLOR.get());
        spawnRawInternal(target, source.getEntity(), String.valueOf(Math.round(finalDamage)),
                WHITE, bottomColor, Style.NORMAL, options, true);
    }

    public static void stellarWindReactionGradient(LivingEntity target, ElementalReactionType type) {
        stellarWindReactionGradient(target, type, Options.DEFAULT);
    }

    public static void stellarWindReactionGradient(LivingEntity target, ElementalReactionType type, Options options) {
        if (type == null) return;
        int bottomColor = parseColor(WorldTextColorConfig.STELLAR_BOTTOM_WIND_COLOR.get());
        spawnRawInternal(target, null, type.getDisplayName(),
                WHITE, bottomColor, Style.REACTION, options, true);
    }

    public static void stellarIceReactionGradient(LivingEntity target, ElementalReactionType type) {
        stellarIceReactionGradient(target, type, Options.DEFAULT);
    }

    public static void stellarIceReactionGradient(LivingEntity target, ElementalReactionType type, Options options) {
        if (type == null) return;
        int bottomColor = parseColor(WorldTextColorConfig.STELLAR_BOTTOM_ICE_COLOR.get());
        spawnRawInternal(target, null, type.getDisplayName(),
                WHITE, bottomColor, Style.REACTION, options, true);
    }
}