package com.linweiyun.genshin.client.damage;

import com.mojang.logging.LogUtils;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class DamageIndicator {
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final long MOVE_MS = 200;
    private static final long FADE_MS = 300;
    private static final double RISE_HEIGHT = 0.23;

    public final Vec3  origin;
    public final Vec3  target;
    public final String text;
    public final int   topColor;
    public final int   bottomColor;
    public final byte  style;

    public final float baseScale;
    public final float startScale;
    public final long  lifetimeMs;

    private final long spawnTime;

    public DamageIndicator(Vec3 origin, Vec3 target, String text,
                           int topColor, int bottomColor, byte style,
                           float baseScale, float startScale, long lifetimeMs) {
        this.origin = origin;
        this.target = target;
        this.text = text;
        this.topColor = topColor;
        this.bottomColor = bottomColor;
        this.style = style;
        this.baseScale = baseScale;
        this.startScale = startScale;
        this.lifetimeMs = lifetimeMs;
        this.spawnTime = System.currentTimeMillis();
        LOGGER.info("[DI-Data] created: text='{}' top=0x{} bottom=0x{}",
                text, Integer.toHexString(topColor), Integer.toHexString(bottomColor));
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - spawnTime >= lifetimeMs;
    }

    public Vec3 getCurrentPosition() {
        long elapsed = System.currentTimeMillis() - spawnTime;
        float tMove = Math.min(1f, elapsed / (float) MOVE_MS);
        Vec3 basePos = origin.lerp(target, easeOutCubic(tMove));
        float tRise = Math.min(1f, elapsed / (float) lifetimeMs);
        return basePos.add(0, RISE_HEIGHT * tRise, 0);
    }

    public float getScale() {
        long elapsed = System.currentTimeMillis() - spawnTime;
        float t = Math.min(1f, elapsed / (float) MOVE_MS);
        return startScale - (startScale - baseScale) * easeOutCubic(t);
    }

    public float getAlpha() {
        long elapsed = System.currentTimeMillis() - spawnTime;
        long fadeStart = lifetimeMs - FADE_MS;
        if (elapsed < fadeStart) return 1f;
        return Math.max(0f, 1f - (elapsed - fadeStart) / (float) FADE_MS);
    }

    /** 是否使用了渐变色 */
    public boolean isGradient() {
        return topColor != bottomColor;
    }

    private static float easeOutCubic(float t) {
        float p = 1f - t;
        return 1f - p * p * p;
    }
}