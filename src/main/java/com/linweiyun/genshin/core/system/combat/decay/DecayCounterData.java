package com.linweiyun.genshin.core.system.combat.decay;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 计时计数器数据 - 线程安全设计
 *
 * 线程模型：
 * - 主线程：读取计数、消费重置标记、递增hitCount
 * - Worker线程：检测超时、设置pendingReset标记
 *
 * 使用原子变量实现无锁设计：
 * - AtomicInteger hitCount: 命中计数（主线程独占写）
 * - AtomicLong startTimeTick: 计时开始时间（主线程写，Worker读）
 * - AtomicBoolean pendingReset: 待重置标记（Worker写，主线程读）
 *
 * 设计原则：
 * - 主线程不做任何超时检测计算，只消费Worker的标记
 * - Worker线程不修改hitCount和startTimeTick，只设置标记
 * - 避免ABA问题：使用单向标记（false→true→false），不会重复触发
 */
public class DecayCounterData {

    // ========== 不可变身份标识 ==========

    private final String attackerUuid;
    private final String characterUuid;
    private final String decayTag;
    private final String groupId;
    private final DecayGroup group;

    // ========== 线程安全的运行时状态 ==========

    // 命中计数 - 主线程独占写，主线程读
    private final AtomicInteger hitCount = new AtomicInteger(0);

    // 计时开始tick - 主线程写，Worker线程读
    private final AtomicLong startTimeTick = new AtomicLong(0);

    // 待重置标记 - Worker线程写，主线程读
    // 工作模式：Worker检测超时 → 设置true → 主线程消费 → 设回false
    private final AtomicBoolean pendingReset = new AtomicBoolean(false);

    // 最近访问tick - Worker线程读写（用于LRU清理）
    private volatile long lastAccessTick;

    // ========== 构造函数 ==========

    public DecayCounterData(String attackerUuid, String characterUuid,
                            String decayTag, String groupId,
                            DecayGroup group, long currentTick) {
        this.attackerUuid = attackerUuid;
        this.characterUuid = characterUuid;
        this.decayTag = decayTag;
        this.groupId = groupId;
        this.group = group;
        this.hitCount.set(0);
        this.startTimeTick.set(currentTick);
        this.lastAccessTick = currentTick;
    }

    // ========== 主线程API ==========

    /**
     * 主线程直接检测超时并重置计数器（不再依赖Worker线程标记）
     *
     * 计时逻辑：从第一次命中起算一个固定窗口（clearTimeTicks），
     * 不管中间命中多少次，窗口到期时 hitCount 归零、startTimeTick 刷新为当前 tick。
     * 后续命中开启新一轮计时。
     *
     * @param currentTick 当前服务器tick
     * @return true=刚执行了超时重置，false=未超时
     */
    public boolean checkAndResetIfTimeout(long currentTick) {
        long start = startTimeTick.get();
        lastAccessTick = currentTick;
        if (start > 0 && (currentTick - start) >= group.getClearTimeTicks()) {
            hitCount.set(0);
            startTimeTick.set(currentTick);
            return true;
        }
        return false;
    }

    /**
     * 递增命中次数（仅主线程调用）
     */
    public void incrementHitCount() {
        hitCount.incrementAndGet();
    }

    /**
     * 获取当前命中次数对应的元素量系数
     */
    public float getElementCoefficient() {
        return group.getElementCoefficient(hitCount.get());
    }

    /**
     * 获取当前命中次数对应的伤害系数
     */
    public float getDamageCoefficient() {
        return group.getDamageCoefficient(hitCount.get());
    }

    /**
     * 获取当前命中次数对应的削韧系数
     */
    public float getPoiseCoefficient() {
        return group.getPoiseCoefficient(hitCount.get());
    }

    // ========== Worker线程API ==========

    /**
     * 由Worker线程检测超时并设置标记
     * 不修改hitCount/startTimeTick，只设置pendingReset=true
     *
     * @param currentTick Worker线程获取的tick
     */
    public void workerCheckTimeout(long currentTick) {
        long start = startTimeTick.get();
        if (start > 0 && (currentTick - start) > group.getClearTimeTicks()) {
            pendingReset.set(true);
        }
    }

    /**
     * 由Worker线程判断是否可被清理
     * 条件：超时2倍周期以上，且30秒内无访问
     */
    public boolean isExpiredForCleanup(long currentTick) {
        return (currentTick - startTimeTick.get()) > (group.getClearTimeTicks() * 2L)
                && (currentTick - lastAccessTick) > 600L;
    }

    // ========== Getters ==========

    public int getHitCount() { return hitCount.get(); }
    public long getStartTimeTick() { return startTimeTick.get(); }
    public long getLastAccessTick() { return lastAccessTick; }
    public boolean isPendingReset() { return pendingReset.get(); }
    public String getAttackerUuid() { return attackerUuid; }
    public String getCharacterUuid() { return characterUuid; }
    public String getDecayTag() { return decayTag; }
    public String getGroupId() { return groupId; }
    public DecayGroup getGroup() { return group; }

    @Override
    public String toString() {
        return "DecayCounter{" +
                "attacker=" + attackerUuid.substring(0, 8) +
                ", char=" + characterUuid.substring(0, Math.min(characterUuid.length(), 8)) +
                ", tag=" + decayTag +
                ", group=" + groupId +
                ", count=" + hitCount.get() +
                ", reset=" + pendingReset.get() +
                '}';
    }
}