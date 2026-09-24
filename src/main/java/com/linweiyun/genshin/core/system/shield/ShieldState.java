package com.linweiyun.genshin.core.system.shield;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

/**
 * 实体身上<b>那一面盾的运行时状态</b> —— 还剩多少、什么时候到期、被打过没有。
 *
 * <p>和 {@link ShieldProfile}（不变的表）分开：这里只有几个数字，
 * 所以同步和存档都很便宜，元素表不用过网络。
 *
 * <h2>存在哪</h2>
 * NeoForge 数据附件 {@code AttachmentRegistration.SHIELD}，对所有 LivingEntity 有效
 * （怪物和玩家都能套盾）。不用 Attribute：Attribute 只能表示一个数，
 * 而盾有「剩余时长 / 分类 / 朝向 / 最近受击刻」好几项。
 *
 * <h2>同步</h2>
 * {@code @Persisted} + {@link PersistedParser}，和 {@code TeyvatEntityStats} 同一套机制。
 * 客户端的血条护盾条读的就是这里的 {@link #value()} / {@link #maxValue()}。
 */
public class ShieldState implements IPersistedSerializable {

    /** 有没有盾。 */
    @Persisted(key = "active")
    private boolean active;

    /** 用的是哪份模板（{@link ShieldProfiles} 里的 key）。 */
    @Persisted(key = "profile")
    private String profileKey = "";

    /** 当前盾量。 */
    @Persisted(key = "value")
    private float value;

    /** 上限，给血条下面的护盾条算比例用。 */
    @Persisted(key = "max_value")
    private float maxValue;

    /** 剩余刻数；{@link #FOREVER} 表示永续（元素盾、白盾这种）。 */
    @Persisted(key = "remaining")
    private int remainingTicks = FOREVER;

    /** 衰减型盾打到盾上的比例（从模板复制过来，允许逐个体覆盖）。 */
    @Persisted(key = "partial_ratio")
    private float partialRatio = 0.9f;

    /** 盾牌型盾的盾面朝向（度）。罩型忽略。 */
    @Persisted(key = "held_yaw")
    private float heldYaw;

    /** 最近一次被攻击命中的时刻，用来算「多久没挨打」。 */
    @Persisted(key = "last_hit")
    private long lastHitGameTime;

    /** 永续盾的剩余刻数标记。 */
    public static final int FOREVER = -1;

    public static final Codec<ShieldState> CODEC = PersistedParser.createCodec(ShieldState::new);
    public static final StreamCodec<ByteBuf, ShieldState> STREAM_CODEC =
            PersistedParser.createStreamCodec(ShieldState::new);

    public ShieldState() {
    }

    // ==================== 读写 ====================

    public boolean isActive() {
        return this.active && this.value > 0f;
    }

    public float value() {
        return this.value;
    }

    public float maxValue() {
        return this.maxValue;
    }

    /** 护盾条比例；没盾时 0。 */
    public float ratio() {
        return this.maxValue <= 0f ? 0f : Math.max(0f, Math.min(1f, this.value / this.maxValue));
    }

    public int remainingTicks() {
        return this.remainingTicks;
    }

    public boolean isForever() {
        return this.remainingTicks == FOREVER;
    }

    public float partialRatio() {
        return this.partialRatio;
    }

    public float heldYaw() {
        return this.heldYaw;
    }

    public long lastHitGameTime() {
        return this.lastHitGameTime;
    }

    /** 取模板；没注册过就返回 null（调用方要能处理）。 */
    @Nullable
    public ShieldProfile profile() {
        return ShieldProfiles.get(this.profileKey);
    }

    /** 盾还有没有「本体」那部分（模板丢了就当没有）。 */
    @Nullable
    public ShieldBreakType breakType() {
        ShieldProfile profile = profile();
        return profile == null ? null : profile.breakType();
    }

    // ==================== 写入（只由 ShieldService 调） ====================

    /** 套盾 / 换盾。 */
    void apply(ShieldProfile profile, float shieldValue, int durationTicks, long gameTime, float heldYaw) {
        this.active = true;
        this.profileKey = profile.key();
        this.maxValue = shieldValue;
        this.value = shieldValue;
        this.remainingTicks = durationTicks;
        this.partialRatio = profile.partialRatio();
        this.heldYaw = heldYaw;
        this.lastHitGameTime = gameTime;
    }

    void clear() {
        this.active = false;
        this.value = 0f;
        this.maxValue = 0f;
        this.remainingTicks = FOREVER;
    }

    /**
     * 扣盾量。
     *
     * @return 实际扣掉的数量（盾量不足时小于请求值）
     */
    float consume(float amount) {
        if (amount <= 0f || !this.active) {
            return 0f;
        }
        float actual = Math.min(amount, this.value);
        this.value -= actual;
        if (this.value <= 0f) {
            this.value = 0f;
            this.active = false;
        }
        return actual;
    }

    void markHit(long gameTime) {
        this.lastHitGameTime = gameTime;
    }

    /** 走时间：到期的盾自己消失。 */
    void tickDuration(long gameTime) {
        if (!this.active || this.remainingTicks == FOREVER) {
            return;
        }
        if (--this.remainingTicks <= 0) {
            clear();
        }
    }
}
