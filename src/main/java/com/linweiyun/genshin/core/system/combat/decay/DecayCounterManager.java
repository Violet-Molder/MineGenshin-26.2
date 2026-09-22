package com.linweiyun.genshin.core.system.combat.decay;

import com.linweiyun.genshin.core.system.combat.damage.ModDamageSpec;
import net.minecraft.world.entity.LivingEntity;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 目标实体上的计时计数器管理器
 *
 * 设计特点：
 * - 使用ConcurrentHashMap保证线程安全
 * - 主线程调用processHit()处理攻击命中
 * - Worker线程调用workerScanTimeout()扫描超时
 * - CounterKey格式: "attackerUuid:characterUuid:decayTag:groupId"
 *
 * 使用方式：
 * 1. 每个LivingEntity持有自己的DecayCounterManager实例
 * 2. 通过IDecayCounterHolder接口获取
 * 3. 主线程在攻击命中时调用processHit()
 * 4. Worker线程定期调用workerScanTimeout()和workerCleanup()
 */
public class DecayCounterManager {

    // 计数器存储 - ConcurrentHashMap支持并发访问
    private final ConcurrentMap<String, DecayCounterData> counters = new ConcurrentHashMap<>();

    // 所属目标实体
    private final LivingEntity owner;

    // 统计信息（供Worker线程读取）
    private volatile long lastWorkerScanTick;
    private volatile int pendingResetCount;

    public DecayCounterManager(LivingEntity owner) {
        this.owner = owner;
        this.lastWorkerScanTick = 0;
    }

    // ========== 主线程API ==========

    /**
     * 处理一次攻击命中（主线程调用）
     *
     * @param attacker 攻击者实体
     * @param character 角色实例（可为null）
     * @param spec 伤害规格
     * @param currentTick 当前服务器tick
     * @return 衰减结果（包含三个系数）
     */
    public DecayResult processHit(LivingEntity attacker, Object character,
                                  ModDamageSpec spec, long currentTick) {
        // 1. 构建key并获取/创建计数器
        String counterKey = buildCounterKey(attacker, character, spec);
        if (counterKey == null) {
            return DecayResult.NONE;
        }

        DecayCounterData counter = counters.computeIfAbsent(counterKey,
                k -> createCounter(attacker, character, spec, currentTick));

        // 2. 检查并执行超时计数器重置
        counter.checkAndResetIfTimeout(currentTick);


        // 3. 读取当前计数对应的系数
        float elementCoef = counter.getElementCoefficient();
        float damageCoef = counter.getDamageCoefficient();
        float poiseCoef = counter.getPoiseCoefficient();

        // 4. 递增计数
        counter.incrementHitCount();

        return new DecayResult(elementCoef, damageCoef, poiseCoef);
    }

    /**
     * 获取或创建计数器（仅查询，不处理攻击流程）
     */
    public DecayCounterData getOrCreateCounter(LivingEntity attacker, Object character,
                                               ModDamageSpec spec, long currentTick) {
        String counterKey = buildCounterKey(attacker, character, spec);
        if (counterKey == null) return null;
        return counters.computeIfAbsent(counterKey,
                k -> createCounter(attacker, character, spec, currentTick));
    }

    // ========== Worker线程API ==========

    /**
     * Worker线程扫描超时计数器
     * 只设置pendingReset标记，不修改其他状态
     *
     * @param currentTick Worker线程获取的tick
     * @return 本次扫描设置了多少个重置标记
     */
    public int workerScanTimeout(long currentTick) {
        int count = 0;
        for (DecayCounterData counter : counters.values()) {
            if (counter.isPendingReset()) continue;
            counter.workerCheckTimeout(currentTick);
            if (counter.isPendingReset()) {
                count++;
            }
        }
        this.pendingResetCount = count;
        this.lastWorkerScanTick = currentTick;
        return count;
    }

    /**
     * Worker线程清理过期计数器
     *
     * @param currentTick Worker线程获取的tick
     * @return 清理了多少个计数器
     */
    public int workerCleanup(long currentTick) {
        int removed = 0;
        var iterator = counters.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().isExpiredForCleanup(currentTick)) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    // ========== 内部方法 ==========

    private String buildCounterKey(LivingEntity attacker, Object character, ModDamageSpec spec) {
        String decayTag = spec.getDecayTag();
        if (decayTag == null) return null;

        String attackerUuid = attacker.getUUID().toString();
        String characterUuid = character != null
                ? character.getClass().getSimpleName() + ":" + attackerUuid
                : "direct:" + attackerUuid;
        String groupId = groupKeyOf(spec.getEffectiveDecayGroup());

        return attackerUuid + ":" + characterUuid + ":" + decayTag + ":" + groupId;
    }

    /**
     * 衰减组的<b>身份</b>（进计数器 key 的那一段）。
     *
     * <p>⚠️ 原来是「是默认普攻组就 {@code "default"}，否则统统 {@code "custom"}」——
     * 于是风铃、E、Q 这些非默认组在同一个目标上<b>共用同一个计数器</b>，
     * 实际生效的三条序列取决于谁先把这个计数器建出来（见附录 A #11）。
     *
     * <p>现在每组各一份。{@code DecayGroup} 是 record，但里面的 {@code DecaySequence}
     * 没有重写 {@code equals}，所以用身份哈希当身份 —— 计数器只在运行时存活（不落存档），
     * 跨重启不需要稳定，这也正好和「同一个静态组实例」的用法对上。
     */
    private static String groupKeyOf(DecayGroup group) {
        if (group == DecayGroups.DEFAULT_NORMAL_ATTACK) return "default";
        return "g" + Integer.toHexString(System.identityHashCode(group));
    }

    private DecayCounterData createCounter(LivingEntity attacker, Object character,
                                           ModDamageSpec spec, long currentTick) {
        String attackerUuid = attacker.getUUID().toString();
        String characterUuid = character != null
                ? character.getClass().getSimpleName() + ":" + attackerUuid
                : "direct:" + attackerUuid;
        String decayTag = spec.getDecayTag();
        String groupId = groupKeyOf(spec.getEffectiveDecayGroup());

        return new DecayCounterData(attackerUuid, characterUuid,
                decayTag, groupId, spec.getEffectiveDecayGroup(), currentTick);
    }

    // ========== Getters ==========

    public LivingEntity getOwner() { return owner; }
    public int getCounterCount() { return counters.size(); }
    public int getPendingResetCount() { return pendingResetCount; }
    public long getLastWorkerScanTick() { return lastWorkerScanTick; }
}