# 把「索敌 / 吸附 / 转向 / 突进」移植到参考2（imaginary_branch）

> 这是一份**草案**，只给 diff，没有动参考2 的任何文件。
>
> **格式说明**：新文件给完整内容（直接照抄成文件）；修改处用**锚点 + `+/-` 块**表示 ——
> 锚点是要在文件里搜到的原文（一定唯一），下面跟一段 unified 风格的增删块。
> 之所以不用 `@@ -114,10 +116,14 @@` 那种行号格式：行号会随你们后续改动漂移，
> 锚点不会。（需要的话我可以再转成能被 `git apply` 直接吃掉的格式。）
>
> 代码里的 `net.luoshu.imaginarybranch.*` 包名、`ImaginaryBranchMod.LOGGER` 等都已按参考2 的风格改好；
> 注释里保留了「为什么这么做」的关键部分，历史踩坑细节我删掉了，需要的话在我们那份里有全文。

---

## 0. 移植后的整体形状

```
客户端每 tick（ActionStateMachine.onClientTick）
  └─ CombatTargeting.tick(player)      ← 锁的过期/宽限/闲置松锁
     AttackApproach.tick(player)       ← 转向推进 + 突进 + （已冻结的）解冻

按键（CharacterKeyMappings → ActionStateMachine.tryAttack → MiyabiComboClient.execute）
  ├─ ① 索敌：CombatTargeting.acquire(player, params) → lock
  ├─ ② 够不着且允许突进 → AttackApproach.begin(...)  动画冻在起手帧
  │      到位 → resumeFromApproach(段刻数) → 再走 ③ 的「切状态 + 发包」
  ├─ ③ 在范围内 → faceTarget（只转身体，镜头不动）+ stepToward（出手推一小步）
  └─ ④ 切动画（changeState）+ 发请求（带上目标 id）

服务端
  ├─ 收到请求 → CombatTargeting.lock(sp, 目标)
  ├─ 每 tick  CombatTargeting.tick(player)        ← 不补这句，锁永不过期（见 §2.6）
  └─ 下线   CombatTargeting.onPlayerRemoved(player)
```

---

## 1. 新文件（3 个，照抄）

### 1.1 `src/main/java/net/luoshu/imaginarybranch/combat/targeting/TargetPolicy.java`

```java
package net.luoshu.imaginarybranch.combat.targeting;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * 索敌筛选策略 —— 「这个东西能不能打」。
 *
 * <p>默认策略：原版 {@link Enemy}、主动攻击型 {@link Mob}（或已经把你当目标的）、
 * 以及 {@link #registerHostile} 登记过的生物。玩家 / 弹射物 / 非生物一律不行
 * （非生物除非用 {@link #registerTargetableNonLiving} 登记，比如元素方碑那种）。
 */
@FunctionalInterface
public interface TargetPolicy {

    /** 这个候选能不能被 {@code owner} 锁定。 */
    boolean isTargetable(Player owner, Entity candidate);

    /**
     * 打分：<b>越大越优先</b>。默认「越近越好 + 正前方加分」。
     *
     * @param distanceSq 候选到玩家的距离平方
     * @param facingDot  候选方向与玩家视线的点积（-1..1，1 = 正前方）
     */
    default double score(Player owner, Entity candidate, double distanceSq, double facingDot) {
        double distanceScore = 1.0 / (1.0 + distanceSq);
        return distanceScore + Math.max(0.0, facingDot) * 0.5;
    }

    // ==================== 登记表 ====================

    /** 「非生物但应该能锁定」的东西（元素方碑这类）。 */
    List<Predicate<Entity>> TARGETABLE_NON_LIVING = new CopyOnWriteArrayList<>();

    /**
     * 「虽然不继承 Enemy、但算敌人」的判定。
     *
     * <p>本工程自研的怪（不实现原版 {@code Enemy} 的那种）在这里登记一条即可：
     * <pre>
     * TargetPolicy.registerHostile(living -&gt; living instanceof MyBossEntity);
     * </pre>
     */
    List<Predicate<LivingEntity>> EXTRA_HOSTILE = new CopyOnWriteArrayList<>();

    static void registerTargetableNonLiving(Predicate<Entity> predicate) {
        if (predicate != null) {
            TARGETABLE_NON_LIVING.add(predicate);
        }
    }

    static void registerHostile(Predicate<LivingEntity> predicate) {
        if (predicate != null) {
            EXTRA_HOSTILE.add(predicate);
        }
    }

    static boolean isSpecialTargetableNonLiving(Entity entity) {
        for (Predicate<Entity> predicate : TARGETABLE_NON_LIVING) {
            if (predicate.test(entity)) {
                return true;
            }
        }
        return false;
    }

    // ==================== 默认策略 ====================

    TargetPolicy DEFAULT = (owner, candidate) -> {
        if (candidate == null || candidate == owner || !candidate.isAlive()) {
            return false;
        }
        if (candidate instanceof Player) {
            return false;
        }
        if (candidate instanceof Projectile) {
            return false;
        }

        if (candidate instanceof LivingEntity living) {
            return isHostile(owner, living);
        }

        return isSpecialTargetableNonLiving(candidate);
    };

    /** 敌对判定：原版 Enemy、主动攻击型 Mob、或登记过的自研怪。 */
    static boolean isHostile(Player owner, LivingEntity living) {
        if (living instanceof Enemy) {
            return true;
        }
        if (living instanceof Mob mob) {
            return mob.getTarget() == owner || mob.isAggressive();
        }
        for (Predicate<LivingEntity> extra : EXTRA_HOSTILE) {
            if (extra.test(living)) {
                return true;
            }
        }
        return false;
    }
}
```

### 1.2 `src/main/java/net/luoshu/imaginarybranch/combat/targeting/CombatTargeting.java`

```java
package net.luoshu.imaginarybranch.combat.targeting;

import net.luoshu.imaginarybranch.ImaginaryBranchMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 软锁定索敌 —— 「玩家现在瞄着谁」。
 *
 * <h2>双阈值迟滞（防 A/B 互抖）</h2>
 * 索敌圈比保持圈小：已经锁上的目标有更宽的宽容度，新目标要明显更好才抢得走。
 * 所以站在两个怪中间不会一会儿锁 A 一会儿锁 B。
 *
 * <h2>丢锁（不会绑死）</h2>
 * 目标死亡/卸载立刻丢；违反保持条件累计 {@link #LOSE_GRACE_TICKS} 刻才丢；
 * 连续 {@link #IDLE_RELEASE_TICKS} 刻没出手自动松锁；也可以手动 {@link #release}。
 *
 * <h2>双端各一份</h2>
 * key 是 {@code "C:"|"S:" + uuid} —— 单人存档里客户端和服务端同 UUID，
 * <b>必须用 side 前缀区分</b>，否则两边互相覆盖。
 * 客户端那份负责转向/突进（要 0 延迟）；服务端那份由攻击请求包喂进来，
 * 供「召唤物问主人正在打谁」以及「锁着就不推额外位移」用。
 */
public final class CombatTargeting {

    // ==================== 迟滞参数 ====================

    /** 首次索敌的最大距离（格）。近战不需要很远，差额交给突进补。 */
    public static final double ACQUIRE_RANGE = 6.0;
    /** 保持锁定的最大距离（格）—— 比索敌远，目标稍微走开不会掉锁。 */
    public static final double KEEP_RANGE = 9.0;
    /** 索敌视角半角（度）。 */
    public static final double ACQUIRE_ANGLE = 55.0;
    /** 保持锁定的视角半角（度）。 */
    public static final double KEEP_ANGLE = 80.0;
    /** 换目标需要的分数优势比例（1.25 = 新目标要明显更好才换）。 */
    public static final double SWITCH_MARGIN = 1.25;
    /** 违反保持条件后还能撑几刻（防抖宽限）。 */
    public static final int LOSE_GRACE_TICKS = 10;
    /** 连续多少刻没攻击就自动松锁（5 秒）。 */
    public static final int IDLE_RELEASE_TICKS = 100;
    /** 远程招式保持圈的余量（格）—— 纯粹防抖，不参与「够不够得着」。 */
    public static final double RANGED_KEEP_MARGIN = 2.0;

    /**
     * 一次索敌/保持用的参数 —— <b>由招式带进来</b>，不是全局写死的。
     *
     * <pre>
     * 近战：acquire 6 / keep 9     → 先锁上，差额靠突进补
     * 远程：acquire = 攻击距离      → 够得着才锁，锁上也不突进
     * </pre>
     */
    public record Params(double acquireRange, double acquireAngle,
                         double keepRange, double keepAngle, TargetPolicy policy) {

        public static final Params DEFAULT = new Params(
                ACQUIRE_RANGE, ACQUIRE_ANGLE, KEEP_RANGE, KEEP_ANGLE, TargetPolicy.DEFAULT);

        public Params {
            // 保持圈比索敌圈小的话会「刚锁上就掉」，这里直接兜住
            keepRange = Math.max(keepRange, acquireRange);
            keepAngle = Math.max(keepAngle, acquireAngle);
            policy = policy == null ? TargetPolicy.DEFAULT : policy;
        }

        /** 远程招式用：索敌距离 = 攻击距离，保持圈只多留一点余量防抖。 */
        public static Params forRange(double attackRange) {
            double range = Math.max(0.5, attackRange);
            return new Params(range, ACQUIRE_ANGLE, range + RANGED_KEEP_MARGIN, KEEP_ANGLE,
                    TargetPolicy.DEFAULT);
        }
    }

    // ==================== 状态 ====================

    private static final Map<String, State> STATES = new ConcurrentHashMap<>();

    private static final class State {
        int targetId = 0;
        /** 连续多少刻不满足保持条件。 */
        int invalidTicks = 0;
        /** 最后一次发起攻击的游戏刻。 */
        long lastAttackTick = Long.MIN_VALUE;
        /** 当前目标的策略，换锁时跟着换。 */
        TargetPolicy policy = TargetPolicy.DEFAULT;
        /** 当前这一招的参数；保持判定用它，不用全局常量。 */
        Params params = Params.DEFAULT;
    }

    private CombatTargeting() {
    }

    // ==================== 查询 ====================

    /** 玩家当前锁定的目标；没有就返回 null。 */
    @Nullable
    public static LivingEntity current(Player player) {
        State state = states().get(key(player));
        if (state == null || state.targetId == 0) {
            return null;
        }
        Entity entity = player.level().getEntity(state.targetId);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    public static boolean isLocked(Player player) {
        return current(player) != null;
    }

    /** 到锁定目标的距离；没锁定返回 {@link Double#MAX_VALUE}。 */
    public static double distanceToTarget(Player player) {
        LivingEntity target = current(player);
        return target == null ? Double.MAX_VALUE : player.distanceTo(target);
    }

    /** 锁定目标的水平方向（已归一化）；没锁定返回 null。 */
    @Nullable
    public static Vec3 directionToTarget(Player player) {
        LivingEntity target = current(player);
        if (target == null) {
            return null;
        }
        Vec3 delta = target.position().subtract(player.position());
        Vec3 horizontal = new Vec3(delta.x, 0, delta.z);
        return horizontal.lengthSqr() < 1.0E-6 ? null : horizontal.normalize();
    }

    // ==================== 索敌 / 换锁 ====================

    /** 默认参数的索敌（近战那套）。 */
    @Nullable
    public static LivingEntity acquire(Player player) {
        return acquire(player, Params.DEFAULT);
    }

    /**
     * 按<b>这一招</b>的参数索敌：已经锁着且仍然有效时优先保持，
     * 只有别的目标明显更好（分数高出 {@link #SWITCH_MARGIN}）才换。
     *
     * <p>调用时会把这套参数记到玩家状态上，随后每刻的 {@link #tick(Player)} 也按它校验 ——
     * 所以「远程招式锁得近、近战招式锁得远」不会互相打架。
     *
     * @return 锁定的目标；范围内没有合适目标时返回 null（不会清掉已有锁）
     */
    @Nullable
    public static LivingEntity acquire(Player player, Params params) {
        Params p = params == null ? Params.DEFAULT : params;
        State state = states().computeIfAbsent(key(player), k -> new State());
        state.params = p;

        LivingEntity kept = current(player);
        if (kept != null && isValid(player, kept, state)) {
            return kept;
        }

        LivingEntity best = findBest(player, p.acquireRange(), p.acquireAngle(), p.policy());
        if (best == null) {
            return null;
        }

        // 已经有锁（虽然这一刻判定无效，但还没超宽限）时不轻易换人
        if (kept != null && state.invalidTicks <= LOSE_GRACE_TICKS) {
            double keptScore = scoreOf(player, kept, p.policy());
            double bestScore = scoreOf(player, best, p.policy());
            if (bestScore < keptScore * SWITCH_MARGIN) {
                return kept;
            }
        }

        lockOn(state, best, p.policy());
        return best;
    }

    /** 攻击瞬间把当前候选钉成锁定目标（钉多久由后续校验决定，不是死绑）。 */
    public static void lock(Player player, @Nullable Entity target) {
        lock(player, target, null);
    }

    /** 攻击瞬间钉住目标，并记下这一招的参数（保持校验用）。 */
    public static void lock(Player player, @Nullable Entity target, @Nullable Params params) {
        State state = states().computeIfAbsent(key(player), k -> new State());
        state.lastAttackTick = player.level().getGameTime();
        if (params != null) {
            state.params = params;
        }

        if (target instanceof LivingEntity living && living.isAlive()) {
            lockOn(state, living, state.policy);
        }
    }

    /** 手动丢锁。 */
    public static void release(Player player) {
        State state = states().get(key(player));
        if (state != null) {
            state.targetId = 0;
            state.invalidTicks = 0;
        }
    }

    /** 换角色 / 死亡 / 退出时清掉。 */
    public static void clear(Player player) {
        states().remove(key(player));
    }

    // ==================== 每刻校验 ====================

    /**
     * 每 tick 调一次：校验当前锁还成不成立，该丢就丢。
     *
     * <p><b>双端都要调</b>：客户端在 {@code ActionStateMachine.onClientTick}，
     * 服务端在 {@code ServerAnimationTicker.onPlayerTick}。
     * 服务端漏了这句的后果很隐蔽 —— 锁永不过期，见移植说明 §2.6。
     */
    public static void tick(Player player) {
        State state = states().get(key(player));
        if (state == null || state.targetId == 0) {
            return;
        }

        LivingEntity target = current(player);

        if (target == null) {
            state.targetId = 0;
            state.invalidTicks = 0;
            return;
        }

        if (!isValid(player, target, state)) {
            if (++state.invalidTicks > LOSE_GRACE_TICKS) {
                state.targetId = 0;
                state.invalidTicks = 0;
            }
            return;
        }

        state.invalidTicks = 0;

        long idle = player.level().getGameTime() - state.lastAttackTick;
        if (state.lastAttackTick != Long.MIN_VALUE && idle > IDLE_RELEASE_TICKS) {
            state.targetId = 0;
        }
    }

    /** 目标还满足「保持锁定」的条件吗。 */
    private static boolean isValid(Player player, LivingEntity target, State state) {
        if (!target.isAlive() || target.isRemoved()) {
            return false;
        }
        if (!state.policy.isTargetable(player, target)) {
            return false;
        }

        double distanceSq = player.distanceToSqr(target);
        if (distanceSq > state.params.keepRange() * state.params.keepRange()) {
            return false;
        }

        return angleTo(player, target) <= state.params.keepAngle();
    }

    // ==================== 内部工具 ====================

    @Nullable
    private static LivingEntity findBest(Player player, double range, double angle, TargetPolicy policy) {
        AABB box = player.getBoundingBox().inflate(range);
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> policy.isTargetable(player, e));

        LivingEntity best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (LivingEntity candidate : candidates) {
            double distanceSq = player.distanceToSqr(candidate);
            if (distanceSq > range * range) {
                continue;
            }
            if (angleTo(player, candidate) > angle) {
                continue;
            }

            double score = scoreOf(player, candidate, policy);
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        return best;
    }

    private static double scoreOf(Player player, LivingEntity target, TargetPolicy policy) {
        double distanceSq = player.distanceToSqr(target);
        Vec3 toTarget = target.position().subtract(player.position());
        Vec3 horizontal = new Vec3(toTarget.x, 0, toTarget.z);
        double dot = horizontal.lengthSqr() < 1.0E-6
                ? 1.0
                : horizontal.normalize().dot(player.getLookAngle().multiply(1, 0, 1).normalize());
        return policy.score(player, target, distanceSq, dot);
    }

    /** 目标方向与玩家视线的夹角（度，水平面上算）。 */
    private static double angleTo(Player player, Entity target) {
        Vec3 toTarget = target.position().subtract(player.position());
        Vec3 horizontal = new Vec3(toTarget.x, 0, toTarget.z);
        if (horizontal.lengthSqr() < 1.0E-6) {
            return 0;
        }

        Vec3 look = player.getLookAngle();
        Vec3 lookHorizontal = new Vec3(look.x, 0, look.z);
        if (lookHorizontal.lengthSqr() < 1.0E-6) {
            return 0;
        }

        double dot = Math.clamp(horizontal.normalize().dot(lookHorizontal.normalize()), -1.0, 1.0);
        return Math.toDegrees(Math.acos(dot));
    }

    private static void lockOn(State state, LivingEntity target, TargetPolicy policy) {
        state.targetId = target.getId();
        state.invalidTicks = 0;
        state.policy = policy;
    }

    private static Map<String, State> states() {
        return STATES;
    }

    private static String key(Player player) {
        UUID uuid = player.getUUID();
        return (player.level().isClientSide() ? "C:" : "S:") + uuid;
    }

    /** 调试：当前有几个玩家在锁定状态。 */
    public static int trackedCount() {
        return STATES.size();
    }

    /** 服务端：玩家下线时调用（{@code ServerAnimationTicker.onPlayerLoggedOut}）。 */
    public static void onPlayerRemoved(ServerPlayer player) {
        clear(player);
    }
}
```

### 1.3 `src/main/java/net/luoshu/imaginarybranch/combat/AttackApproach.java`

```java
package net.luoshu.imaginarybranch.combat;

import net.luoshu.imaginarybranch.animation.state.ActionStateMachine;
import net.luoshu.imaginarybranch.combat.targeting.CombatTargeting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 近战贴上去 + 转向 + 吸附 —— 「锁到目标之后怎么打到人」。
 *
 * <h2>三件事</h2>
 * <ol>
 *   <li><b>转向</b>：衰减式追角，快但不瞬移，而且<b>只写 {@code yBodyRot}</b>（模型朝向），
 *       绝不碰 {@code yRot} —— 那是本机玩家的镜头，碰一下玩家就觉得画面被拽走了。</li>
 *   <li><b>突进</b>：目标在攻击距离外时冲过去，期间动画冻在起手帧，到位再解冻重新计时。
 *       方向是<b>全向</b>的（含 Y）—— 目标是飞的/在脚下的都能贴过去。</li>
 *   <li><b>吸附</b>：<b>出手那一瞬间</b>朝目标推一小步（一次一小段）。不是持续跟随 ——
 *       持续跟随就成磁铁了。</li>
 * </ol>
 *
 * <h2>到位判定留了宽容度</h2>
 * 冲刺时目标也在动，要求精确贴住会出现「人已经贴脸了却永远没到位」→ 贴着人鬼畜。
 * 所以进到「停靠距离 + {@link #ARRIVE_SLACK}」就算到位。
 */
public final class AttackApproach {

    // ==================== 转向 ====================

    /** 每刻消掉剩余夹角的比例；越大越急。 */
    public static final float TURN_GAIN = 0.45f;
    /** 最小角速度（度/刻）—— 小角度也保持一点惯性。 */
    public static final float TURN_MIN_SPEED = 6.0f;
    /** 最大角速度（度/刻，45 ≈ 900°/秒）：大角度也不会一帧到位。 */
    public static final float TURN_MAX_SPEED = 45.0f;
    /** 夹角小于这个度数就算转到了（度）。 */
    public static final float TURN_TOLERANCE = 1.5f;
    /** 一次动作最多持续转向多少刻（突进期间不消耗这份预算）。 */
    public static final int TURN_BUDGET_TICKS = 16;

    // ==================== 突进 ====================

    /** 突进终点：贴到目标身前多近就停（格）。 */
    public static final double STOP_DISTANCE = 1.4;
    /** 到位宽容度（格）—— 「差不多就行」，别死磕精确距离。 */
    public static final double ARRIVE_SLACK = 0.6;
    /** 突进速度（格/刻），全向。 */
    public static final double DASH_SPEED = 1.0;
    /** 兜底：最多突进多少刻，超时也要继续打。 */
    public static final int MAX_APPROACH_TICKS = 20;
    /**
     * 「已经够得着」连续多少刻就强制开打。
     *
     * <p>目标在到位线附近来回动时，光靠精确距离判定会让人一直跟着跑；
     * 够得着（进到这一招的攻击距离）连续这么多刻就直接出手 —— 反正已经打得到了。
     */
    public static final int ARRIVE_PATIENCE_TICKS = 3;

    // ==================== 吸附 ====================

    /** 吸附带（格）：目标在「攻击距离 + 这个」以内才在出手时推一小步。 */
    public static final double ADHESION_BAND = 1.5;
    /** 出手那一下的推力（冲量，≈ 半个格位移）。 */
    public static final double ADHESION_STEP_SPEED = 0.22;
    /** 冲量 → 位移的换算（地面摩擦 ≈0.4/刻），用来按缺口收力。 */
    public static final double ADHESION_TRAVEL_FACTOR = 2.5;
    /** 比这个还近就不再推（推了会穿模/穿过去丢锁）。 */
    public static final double ADHESION_MIN_DISTANCE = 0.9;

    // ==================== 招式形态 ====================

    /**
     * 一次出手的形态 —— <b>逐招式</b>决定近战/远程、要不要突进、索敌多远。
     *
     * <p>本工程没有 {@code ActionStep} 那套数据驱动（时序硬编码在 {@code Miyabi*Client} 里），
     * 所以这里给两个预设，直接用/按需改。
     *
     * <h2>字段里 {@code -1} / {@code 0} 的含义</h2>
     * 「不覆盖，用全局默认」。所以想自定义时<b>只需要写你关心的那几个数</b>，
     * 剩下的填 -1 即可。下面那组 {@code withXxx} 就是干这个的。
     */
    public record ApproachParams(boolean ranged, boolean dash,
                                 double acquireRange, double keepRange,
                                 double acquireAngle, double keepAngle,
                                 double stopDistance, double dashSpeed,
                                 int maxApproachTicks, float turnSpeed) {

        /** 近战默认：索敌 6 / 保持 9 / 攻击距离外突进贴脸。 */
        public static final ApproachParams MELEE =
                new ApproachParams(false, true, -1, -1, -1, -1, -1, -1, 0, -1f);

        /** 远程：不突进、索敌 = 攻击距离、只转向。 */
        public static final ApproachParams RANGED =
                new ApproachParams(true, false, -1, -1, -1, -1, -1, -1, 0, -1f);

        /** 这一招实际会不会突进。 */
        public boolean wantsDash() {
            return !ranged && dash;
        }

        // ==================== 自定义：链式改几个数 ====================
        //
        // 用法（只写你关心的，其余保持预设）：
        //     ApproachParams.MELEE.withRange(8, 11).withDashProfile(3.0, 0.9, 24)
        // 也可以直接 new 一个常量放在角色自己的类里（见文件末尾的注释）。

        public ApproachParams withDash(boolean enabled) {
            return new ApproachParams(ranged, enabled, acquireRange, keepRange, acquireAngle,
                    keepAngle, stopDistance, dashSpeed, maxApproachTicks, turnSpeed);
        }

        /** 索敌与保持距离（格）。传 -1 表示这一项用默认。 */
        public ApproachParams withRange(double acquire, double keep) {
            return new ApproachParams(ranged, dash, acquire, keep, acquireAngle,
                    keepAngle, stopDistance, dashSpeed, maxApproachTicks, turnSpeed);
        }

        /** 索敌与保持的视角半角（度）。传 -1 表示这一项用默认。 */
        public ApproachParams withAngles(double acquireAngle, double keepAngle) {
            return new ApproachParams(ranged, dash, acquireRange, keepRange, acquireAngle,
                    keepAngle, stopDistance, dashSpeed, maxApproachTicks, turnSpeed);
        }

        /** 突进手感三件套：停几格 / 冲多快 / 最多几刻。 */
        public ApproachParams withDashProfile(double stopDistance, double dashSpeed,
                                              int maxApproachTicks) {
            return new ApproachParams(ranged, dash, acquireRange, keepRange, acquireAngle,
                    keepAngle, stopDistance, dashSpeed, maxApproachTicks, turnSpeed);
        }

        /** 转向最大角速度（度/刻）：想让人转得更急/更从容就调它。 */
        public ApproachParams withTurnSpeed(float degreesPerTick) {
            return new ApproachParams(ranged, dash, acquireRange, keepRange, acquireAngle,
                    keepAngle, stopDistance, dashSpeed, maxApproachTicks, degreesPerTick);
        }

        /** 换算成索敌参数：<b>远程招式的索敌距离 = 攻击距离</b>（够得着才锁）。 */
        public CombatTargeting.Params targeting(double attackRange) {
            CombatTargeting.Params base = ranged
                    ? CombatTargeting.Params.forRange(acquireRange > 0 ? acquireRange : attackRange)
                    : CombatTargeting.Params.DEFAULT;

            double acquire = acquireRange > 0 ? acquireRange : base.acquireRange();
            double keep = keepRange > 0 ? keepRange : base.keepRange();

            // ⚠️ 这两行必须写 `this.`：
            //    局部变量名和 record 分量同名，而「局部变量的作用域包含它自己的初始化表达式」——
            //    裸写 `acquireAngle > 0` 会把右边解析成「还没初始化的那个局部变量」，
            //    编译报「变量 acquireAngle 可能尚未初始化」。
            //    （写成 e.acquireAngle 那种「带前缀的字段访问」不会有这个问题，
            //      差别就是：这里的方法就在 record 内部。）
            double acquireAngle = this.acquireAngle > 0 ? this.acquireAngle : base.acquireAngle();
            double keepAngle = this.keepAngle > 0 ? this.keepAngle : base.keepAngle();

            return new CombatTargeting.Params(acquire, acquireAngle, keep, keepAngle, base.policy());
        }
    }

    // ==================== 怎么不用预设 ====================
    //
    // 三档，按需要挑：
    //
    // ① 只改几个数（最常见）—— 用 withXxx 链式改，放在角色自己的客户端类里当常量：
    //      /** 长枪：够得着 5 格、索敌 8 格、冲到 3 格外停、转得从容一点。 */
    //      public static final AttackApproach.ApproachParams STYLE_LANCE =
    //              AttackApproach.ApproachParams.MELEE
    //                      .withRange(8, 11)
    //                      .withDashProfile(3.0, 0.9, 24)
    //                      .withTurnSpeed(28f);
    //
    // ② 整段自己写（连 ranged/dash 都要跟预设不一样）—— 直接 new：
    //      public static final AttackApproach.ApproachParams STYLE_BOW =
    //              new AttackApproach.ApproachParams(
    //                      true,        // ranged：不突进 + 索敌=攻击距离
    //                      false,       // dash
    //                      -1, -1,      // 索敌/保持距离：用默认（远程会被算成攻击距离）
    //                      40, 70,      // 索敌/保持视角半角：窄一点，只锁正前方
    //                      -1, -1, 0,   // 突进三件套：不用（远程不突进）
    //                      -1f);        // 转向速度默认
    //
    // ③ 完全不参与索敌系统（纯召唤/纯远程炮台，连转向都不想要）——
    //    <b>不调用那一块就行</b>，原来的 changeState + 发包照旧：
    //      ActionStateMachine.changeState("cast", 2, 40, 8);
    //      NetworkManager.sendActionRequestToServer(ActionKind.SKILL, 0, hasEnergy);
    //    索敌/转向/吸附/突进四件事是<b>调用才生效</b>的，没有全局开关，
    //    所以「不想要」= 不调用，不需要给它一个 DISABLED 预设。
    //
    // 另外：<b>逐段不同</b>也是一样的写法 —— 用 switch 返回不同的 ApproachParams 即可，
    // 例如第 4 段是突刺（突进强、停得近）、第 5 段是横扫（不突进、索敌宽）：
    //      private static AttackApproach.ApproachParams styleFor(int stage) {
    //          return switch (stage) {
    //              case 4 -> ApproachParams.MELEE.withDashProfile(1.2, 1.1, 26);
    //              case 5 -> ApproachParams.MELEE.withDash(false).withRange(7, 10);
    //              default -> ApproachParams.MELEE;
    //          };
    //      }

    // ==================== 状态 ====================

    private static Runnable onArrive;
    private static int targetId;

    // 转向（独立于突进：到位之后还要把最后几度转完）
    private static boolean facing;
    private static int facingTicksLeft;
    private static float facingTurnSpeed = TURN_MAX_SPEED;

    // 突进
    private static boolean dashing;
    private static int dashTicksLeft;
    private static double dashStopDistance = STOP_DISTANCE;
    private static double dashSpeed = DASH_SPEED;
    private static double dashAttackRange = 3.0;
    /** 连续多少刻「已经够得着」（用于 {@link #ARRIVE_PATIENCE_TICKS} 的兜底开打）。 */
    private static int nearTicks;

    private AttackApproach() {
    }

    /** 正在突进。 */
    public static boolean isActive() {
        return dashing;
    }

    /** 这次攻击需不需要突进（目标在攻击距离外）。 */
    public static boolean needsDash(Player player, @Nullable LivingEntity target, double attackRange) {
        return target != null && player.distanceTo(target) > attackRange;
    }

    // ==================== 开始 ====================

    /**
     * 开始贴脸流程：转向 + 突进，到位后回调。
     *
     * <p>调用方负责先用 {@link ApproachParams#wantsDash()} 判断要不要走这条路；
     * 只要走这条，就一定会突进。
     *
     * @param attackRange 这一招的生效攻击距离（决定到位线）
     * @param onArrive    到位后干什么（解冻动画 + 切状态 + 发服务端请求）
     */
    public static void begin(LocalPlayer player, LivingEntity target, ApproachParams params,
                             double attackRange, Runnable onArrive) {
        startFacing(player, target, params);

        AttackApproach.targetId = target.getId();
        AttackApproach.onArrive = onArrive;
        AttackApproach.dashing = true;
        AttackApproach.nearTicks = 0;
        AttackApproach.dashTicksLeft = params != null && params.maxApproachTicks() > 0
                ? params.maxApproachTicks()
                : MAX_APPROACH_TICKS;
        AttackApproach.dashStopDistance = params != null && params.stopDistance() > 0
                ? params.stopDistance()
                : STOP_DISTANCE;
        AttackApproach.dashSpeed = params != null && params.dashSpeed() > 0
                ? params.dashSpeed()
                : DASH_SPEED;
        AttackApproach.dashAttackRange = attackRange > 0 ? attackRange : 3.0;

        // 冻结动画：状态机把当前动作停在这一帧
        ActionStateMachine.setApproachFrozen(true);
    }

    /**
     * 只转向不位移 —— 目标已经在攻击距离内、或者这一招是远程招式时用这个。
     *
     * @return 有没有开始转向（没目标就返回 false）
     */
    public static boolean faceTarget(LocalPlayer player, @Nullable LivingEntity target,
                                     @Nullable ApproachParams params) {
        if (target == null) {
            return false;
        }
        startFacing(player, target, params);
        return true;
    }

    private static void startFacing(LocalPlayer player, LivingEntity target, @Nullable ApproachParams params) {
        targetId = target.getId();
        facing = true;
        facingTicksLeft = TURN_BUDGET_TICKS;
        facingTurnSpeed = params != null && params.turnSpeed() > 0 ? params.turnSpeed() : TURN_MAX_SPEED;
    }

    /**
     * 吸附：<b>出手那一瞬间</b>朝目标推一小段，一次一小步。
     *
     * <p>要的手感是「每一刀各自带一小步」：目标挪开半步 → 这一刀先不动，
     * 下一刀再贴回去。因为每次只走一小段、连招间隔又短，看起来就是攻击自带吸附，
     * 而不是像磁铁一样被持续拖着走。
     */
    public static void stepToward(LocalPlayer player, LivingEntity target, double attackRange) {
        double distance = player.distanceTo(target);
        if (distance > attackRange + ADHESION_BAND || distance <= ADHESION_MIN_DISTANCE) {
            return;
        }

        Vec3 delta = target.position().subtract(player.position());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (horizontal < 1.0E-6) {
            return;
        }

        // 按缺口收力：贴脸时不推、离得远时推满（也就半个格）
        double gap = Math.min(distance - ADHESION_MIN_DISTANCE, ADHESION_BAND);
        double speed = Math.min(ADHESION_STEP_SPEED, gap / ADHESION_TRAVEL_FACTOR);

        Vec3 nudge = new Vec3(delta.x, 0, delta.z).scale(speed / horizontal);
        player.setDeltaMovement(player.getDeltaMovement().add(nudge));
        player.hurtMarked = true;
    }

    /** 取消当前突进与转向（换动作 / 回常态时调用）。 */
    public static void cancel() {
        clearDash();
        facing = false;
        facingTicksLeft = 0;
        ActionStateMachine.setApproachFrozen(false);
    }

    // ==================== 每刻 ====================

    /** 每客户端 tick 调一次（在状态机 tick 之前）。 */
    public static void tick(LocalPlayer player) {
        if (!facing && !dashing) {
            return;
        }

        LivingEntity target = resolveTarget(player);

        // 目标没了 → 突进立刻收摊（保留原速度别把人定在半路），转向也停
        if (target == null) {
            if (dashing) {
                finish(player, false);
            }
            facing = false;
            facingTicksLeft = 0;
            return;
        }

        tickFacing(player, target);

        if (!dashing) {
            return;
        }

        // 超时 → 照样继续打，只是不位移了
        if (--dashTicksLeft <= 0) {
            finish(player, false);
            return;
        }

        double distance = player.distanceTo(target);

        // 到位判定：进到「停靠距离 + 宽容度」就出手（别死磕精确贴住）
        if (distance <= arriveDistance()) {
            finish(player, true);
            return;
        }

        // 兜底：已经够得着（进到这一招的攻击距离）却还没进到位线 → 连续几刻就强制开打。
        // 目标在到位线附近来回动时，距离会一直在线上弹，
        // 只靠精确判定会出现「人在旁边跟着跑，动画却一直冻着不出手」。
        if (distance <= dashAttackRange) {
            if (++nearTicks >= ARRIVE_PATIENCE_TICKS) {
                finish(player, true);
                return;
            }
        } else {
            nearTicks = 0;
        }

        // 全向突进：含 Y。目标是飞的 / 在脚下的，只推水平会永远差一段高度
        Vec3 delta = target.position().subtract(player.position());
        double remaining = delta.length();
        if (remaining < 1.0E-6) {
            finish(player, true);
            return;
        }

        // ⚠️ 这里**不按剩余距离收力**。
        //
        // 早期写的是 min(dashSpeed, distance - 到位线)：靠近到位线时力度会被压得很小，
        // 而目标正在往外走（怪的速度量级差不多就是 0.2 格/刻）时两边刚好抵消 →
        // 距离永远卡在到位线上一点点，人就「跟着目标跑」直到超时，动画全程冻着。
        // 现在全程满速，靠「不超过目标位置」这一条防冲过头：最多一步到目标身上，下一帧就判定到位。
        double step = Math.min(dashSpeed, remaining);
        Vec3 velocity = delta.scale(step / remaining);
        player.setDeltaMovement(velocity.x, velocity.y, velocity.z);
        player.hurtMarked = true;
    }

    /** 到位线 = min(攻击距离, 停靠距离 + 宽容度)。 */
    private static double arriveDistance() {
        double slack = Math.max(0.0, dashStopDistance + ARRIVE_SLACK);
        return Math.min(dashAttackRange, slack);
    }

    /**
     * 转向一步。
     *
     * <p><b>两个必须遵守的约束</b>（都踩过）：
     * <ol>
     *   <li><b>绝不过冲</b>：转动量夹在剩余夹角以内（用 {@code approachDegrees}）。
     *       早期版本直接相加，剩余 5° 时一步跨到对面、下一帧再跨回来 → 镜头左右乱晃（±5° 极限环）。</li>
     *   <li><b>只转身体</b>：写 {@code yBodyRot}，不写 {@code yRot} / {@code yHeadRot}。
     *       本机玩家的 {@code yRot} 就是镜头，碰一下就有「被拽」的观感。</li>
     * </ol>
     */
    private static void tickFacing(LocalPlayer player, LivingEntity target) {
        if (!facing) {
            return;
        }

        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        if (dx * dx + dz * dz < 1.0E-6) {
            facing = false;
            return;
        }

        float targetYaw = yawTo(dx, dz);
        float delta = Mth.wrapDegrees(targetYaw - player.yBodyRot);

        if (Math.abs(delta) <= TURN_TOLERANCE) {
            setBodyYaw(player, targetYaw);
            facing = false;
            return;
        }

        if (!dashing && --facingTicksLeft <= 0) {
            facing = false;
            return;
        }

        float minSpeed = Math.min(TURN_MIN_SPEED, facingTurnSpeed);
        float speed = Math.max(minSpeed, Math.min(Math.abs(delta) * TURN_GAIN, facingTurnSpeed));
        setBodyYaw(player, Mth.approachDegrees(player.yBodyRot, targetYaw, speed));
    }

    /**
     * 到位：急停 + 解冻动画并回调（回调里切状态、发请求）。
     *
     * @param hardStop true = 贴到目标的正常到位（水平速度清零做急停）；
     *                 false = 目标没了/超时（保留原速度，别把人定在半路）
     */
    private static void finish(LocalPlayer player, boolean hardStop) {
        Runnable callback = onArrive;
        clearDash();

        if (hardStop) {
            player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
            player.hurtMarked = true;
        }

        ActionStateMachine.setApproachFrozen(false);

        if (callback != null) {
            callback.run();
        }
    }

    private static void clearDash() {
        dashing = false;
        dashTicksLeft = 0;
        onArrive = null;
    }

    // ==================== 工具 ====================

    private static float yawTo(double dx, double dz) {
        return (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
    }

    private static void setBodyYaw(Player player, float yaw) {
        // 只写身体朝向：模型转过去对准目标，玩家的镜头一动不动
        player.setYBodyRot(yaw);
    }

    @Nullable
    private static LivingEntity resolveTarget(LocalPlayer player) {
        if (targetId == 0 || player.level() == null) {
            return null;
        }
        return player.level().getEntity(targetId) instanceof LivingEntity living && living.isAlive()
                ? living
                : null;
    }
}
```

---

## 2. 要改的文件（按锚点套用）

### 2.1 `animation/state/ActionStateMachine.java`

**① 加 import**（锚点：`import ...capability.PlayerVariables;` 那一行后面）

```diff
 import net.luoshu.imaginarybranch.capability.PlayerVariables;
+import net.luoshu.imaginarybranch.combat.AttackApproach;
+import net.luoshu.imaginarybranch.combat.targeting.CombatTargeting;
 import net.luoshu.imaginarybranch.network.NetworkManager;
```

**② 加「突进冻结」字段与三个方法**（锚点：`/** 连击保持窗口：硬直结束后还能接续下一段的剩余刻数。 */` 那一段附近，放在字段区末尾即可）

```diff
     /** 客户端延迟任务队列（1 tick 一步），被打断时整体清空。 */
     private static final List<ClientTask> CLIENT_TASKS = new ArrayList<>();
 
+    /** 突进期间冻结动画：渲染控制器把动画速度设成 0，姿势定在起手帧。 */
+    private static boolean approachFrozen = false;
+
+    /** 刚刚解冻（给渲染控制器一次「还原速度 + 拉回第 0 帧」的机会，用完即清）。 */
+    private static boolean approachJustUnfrozen = false;
+
+    /** 突进期间冻结动画（渲染控制器会把动画速度设成 0）。 */
+    public static void setApproachFrozen(boolean frozen) {
+        approachFrozen = frozen;
+        if (frozen) {
+            approachJustUnfrozen = false;
+        }
+    }
+
+    public static boolean isApproachFrozen() {
+        return approachFrozen;
+    }
+
+    /**
+     * 突进到位：解冻动画，并把总时长<b>从这一刻重新起算</b>。
+     *
+     * <p>这是「伤害延迟按到位时刻算」的关键 —— 各段的刻数本来是相对动作起点的，
+     * 突进把起点推后了，所以这里把 {@code animationTick} 拨回满值，
+     * 动画则从冻结的那一帧继续往下播。
+     */
+    public static void resumeFromApproach(int totalTicks) {
+        approachFrozen = false;
+        approachJustUnfrozen = true;
+        animationTick = Math.max(1, totalTicks);
+    }
+
+    /**
+     * 取「刚解冻」标记（读一次就清）。
+     *
+     * <p>渲染控制器用它触发一次 {@code forceAnimationReset()}：
+     * GeckoLib 4 的冻结是「速度 0」，底层时钟仍在走，直接还原速度会让动画跳过整段突进时长。
+     */
+    public static boolean consumeApproachUnfrozen() {
+        boolean value = approachJustUnfrozen;
+        approachJustUnfrozen = false;
+        return value;
+    }
+
     private ActionStateMachine() {
     }
```

**③ 每 tick 调索敌与突进**（锚点：`runDelayedTasks();`）

```diff
         runDelayedTasks();
 
+        // 索敌校验 + 突进/转向（都放在动画判定之前，保证当帧就生效）
+        CombatTargeting.tick(player);
+        AttackApproach.tick(player);
+
         // 左键按住 → 交给当前角色的动作编排做蓄力判定
         if (isAttackButtonDown) {
```

**④ 突进期间冻结动画时间轴**（锚点：`// 动画总时长倒数`）

```diff
-        // 动画总时长倒数
-        if (animationTick > 0) {
-            animationTick--;
-        } else if (!PlayerVariables.DEFAULT_ANIME_STATE.equals(currentState)) {
-            // 自然播完 → 回常态，并断开连击
-            resetToDefault();
-        }
+        // 动画总时长倒数。
+        //
+        // ⚠️ 突进期间整块跳过：动画被冻在起手帧，时间轴就不该再走。
+        //    否则动作刻数比突进耗时刻数短时，「自然播完 → resetToDefault」会抢在到位之前发生，
+        //    把这一刀连同还没发出去的服务端请求一起吞掉（有动画、没伤害、冷却也不扣）。
+        //    突进自己有 MAX_APPROACH_TICKS 兜底，冻结不会卡死。
+        if (!approachFrozen) {
+            if (animationTick > 0) {
+                animationTick--;
+            } else if (!PlayerVariables.DEFAULT_ANIME_STATE.equals(currentState)) {
+                // 自然播完 → 回常态，并断开连击
+                resetToDefault();
+            }
+        }
```

**⑤ `changeState` 开头取消突进**（锚点：`dispatchCleanup(currentState, player);`，注意这行在 `changeState` 和 `resetToDefault` 里各出现一次 —— 两处都按 ⑥ 的方式改，但语义不同）

```diff
     public static void changeState(...) {
         LocalPlayer player = Minecraft.getInstance().player;
 
+        // 换动作 → 上一次出手的突进作废。
+        // 不取消的话会出现「已经闪避走了，半秒后突然补一刀」——
+        // 突进是「上一次出手」的延续，新状态一开始它就该结束。
+        // （突进自己的起手是先 changeState 再 begin，所以不会误杀自己）
+        AttackApproach.cancel();
+
         dispatchCleanup(currentState, player);
```

**⑥ `resetToDefault` 也取消突进**（锚点：`public static void resetToDefault() {` 之后的那句 `LocalPlayer player = ...;`）

```diff
     public static void resetToDefault() {
         LocalPlayer player = Minecraft.getInstance().player;
 
+        // 回常态 = 这次出手结束了：还在突进就立刻停（不走到位回调，也不补发攻击请求）
+        AttackApproach.cancel();
+
         dispatchCleanup(currentState, player);
```

### 2.2 `animation/state/PlayerAnimationController.java`（⚠️ GeckoLib 4 没有 `PlayState.PAUSE`）

**GeckoLib 4 的 `PlayState` 只有 `CONTINUE` / `STOP`**（我核对了 4.9.2 的源码），
所以「突进期间冻住动画」不能用 `PlayState.PAUSE`，要用 **`AnimationController#setAnimationSpeed(0)`**：

```java
// GeckoLib 4.9.2 AnimationController 的 adjustTick：
//     return animationSpeedModifier.apply(animatable) * Math.max(tick - tickOffset, 0);
// 速度设 0 → 恒返回 0 → 动画永远在第 0 帧求值 → 姿势定死在起手帧。
```

**锚点**：`boolean isLocalPlayer = player == Minecraft.getInstance().player;` 下面的 `currentPlayingAnim` 算完之后
（也就是原来的 `// 4. 动作动画优先` 之前）

```diff
+                    // 突进期间把动作冻在起手那一帧。
+                    //
+                    // GeckoLib 4 没有 PlayState.PAUSE（只有 CONTINUE / STOP），
+                    // 用「动画速度 0」实现同样效果：adjustTick 恒为 0 → 姿势定在第 0 帧。
+                    //
+                    // 只对**本机玩家**生效：动作冻结是全局状态，而其他玩家的控制器也走这里，
+                    // 不加 isLocalPlayer 的话，只要动画名撞上（比如双方都在 attack_1），旁边的人也会被冻住。
+                    boolean frozenHere = isLocalPlayer
+                            && ActionStateMachine.isApproachFrozen()
+                            && targetAnim != null && targetAnim.equals(currentPlayingAnim);
+
+                    if (frozenHere) {
+                        controller.setAnimationSpeed(0);
+                        return PlayState.CONTINUE;
+                    }
+
+                    // 刚解冻的那一帧：速度还原 + 把时间轴拉回第 0 帧。
+                    // 不 reset 的话会「跳到本应播到的位置」—— 因为速度 0 期间底层时钟仍在走，
+                    // 一还原速度就按 tick - tickOffset 的绝对值求值，动画会突然跳过整段突进时长。
+                    if (isLocalPlayer && ActionStateMachine.consumeApproachUnfrozen()) {
+                        controller.setAnimationSpeed(1);
+                        controller.forceAnimationReset();
+                    }
+
                     // 4. 动作动画优先：播一次即可，播完由状态机/服务端复位
                     if (targetAnim != null && !targetAnim.isEmpty() && !PlayerVariables.DEFAULT_ANIME_STATE.equals(targetAnim)) {
                         return state.setAndContinue(RawAnimation.begin().thenPlay(targetAnim));
                     }
```

> `controller` 在上面已经取过了（`AnimationController<T> controller = state.getController();`）✔
> 同包，不用加 import。`setAnimationSpeed` / `forceAnimationReset` 都是 GeckoLib 4 的公开方法；
> 你们工程里**没有**用过 `setAnimationSpeedHandler`，所以「还原成 1」不会覆盖掉谁的自定义速度。

### 2.3 `animation/sever/ServerAnimationTicker.java`

**① 服务端每刻校验锁**（锚点：`if (!(event.getEntity() instanceof ServerPlayer player)) {` 那个块的结尾）

```diff
     public static void onPlayerTick(PlayerTickEvent.Post event) {
         if (!(event.getEntity() instanceof ServerPlayer player)) {
             return;
         }
 
+        // 服务端也要每刻校验索敌锁！客户端那份由 ActionStateMachine 驱动，
+        // 服务端这份是「攻击请求包喂进来」的，没人校验它就会永远不松 ——
+        // 后果：任何「锁着就不推位移」的逻辑从此一直生效（首次攻击后服务端位移永久失效）。
+        CombatTargeting.tick(player);
+
         Integer remaining = REMAINING_TICKS.get(player.getUUID());
```

**② 下线清理**（锚点：`clear(player);`）

```diff
     public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
         if (event.getEntity() instanceof ServerPlayer player) {
             clear(player);
+            // 服务端那份索敌锁同样按 uuid 存在静态表里，玩家走了就得清
+            CombatTargeting.onPlayerRemoved(player);
         }
     }
```

**③ import**

```diff
+import net.luoshu.imaginarybranch.combat.targeting.CombatTargeting;
```

### 2.4 `network/NetworkManager.java`（请求带上目标 id）

**① 服务端收到就记锁**（锚点：`MiyabiServerActions.handle(player, kind, comboStage, hasEnergy);`）

```diff
     @RPCPacket("actionRequest")
-    public static void actionRequest(RPCSender sender, int kindOrdinal, int comboStage, boolean hasEnergy) {
+    public static void actionRequest(RPCSender sender, int kindOrdinal, int comboStage, boolean hasEnergy,
+                                     int targetEntityId) {
         if (sender.isServer()) {
             return;
         }
 
         ServerPlayer player = sender.asPlayer();
         if (player == null) {
             return;
         }
 
+        // 服务端也记一份锁：召唤物要问「主人正在打谁」，而主人可能已经切到后台。
+        // -1 表示这次没有目标，只会刷新「最近出手时间」，不会覆盖已有的锁。
+        LivingEntity target = player.level().getEntity(targetEntityId) instanceof LivingEntity living
+                ? living
+                : null;
+        CombatTargeting.lock(player, target);
+
         ActionKind kind = ActionKind.byOrdinal(kindOrdinal);
         String character = ImaginaryBranchModVariables.get(player).Character_Now;
```

**② 发送端**（锚点：`public static void sendActionRequestToServer(ActionKind kind, int comboStage, boolean hasEnergy) {`）

```diff
+    /** 带目标的出手请求：服务端据此更新「主人正在打谁」。 */
+    public static void sendActionRequestToServer(ActionKind kind, int comboStage, boolean hasEnergy,
+                                                 @Nullable Entity target) {
+        RPCPacketDistributor.rpcToServer("actionRequest", kind.ordinal(), comboStage, hasEnergy,
+                target == null ? -1 : target.getId());
+    }
+
+    /** 旧签名保留：不带目标（服务端不会更新锁）。 */
     public static void sendActionRequestToServer(ActionKind kind, int comboStage, boolean hasEnergy) {
-        RPCPacketDistributor.rpcToServer("actionRequest", kind.ordinal(), comboStage, hasEnergy);
+        sendActionRequestToServer(kind, comboStage, hasEnergy, null);
     }
```

**③ import**：`net.luoshu.imaginarybranch.combat.targeting.CombatTargeting`、`net.minecraft.world.entity.Entity`、`net.minecraft.world.entity.LivingEntity`、`org.jetbrains.annotations.Nullable`

> ⚠️ 保留旧签名是**故意的**：这样 `MiyabiBlockClient` / `MiyabiSpAttackClient` 等暂时不改也能编译，
> 你可以一段一段地接。

### 2.5 `characters/miyabi/attack/MiyabiComboClient.java`（真正的接线点）

原来的 `execute` 主体改名成 `beginStage`（只切动画），新增一个 `execute` 做
「索敌 → 转向/吸附/突进 → 切动画 → 发包」：

```diff
     private MiyabiComboClient() {
     }
 
-    /** 近战默认形态；远程招式换成 {@link AttackApproach.ApproachParams#RANGED}。 */
-    private static final AttackApproach.ApproachParams STYLE = AttackApproach.ApproachParams.MELEE;
-
     /**
      * 这一段的生效攻击距离（格）。
      *
      * <p>本工程没有 {@code ActionStep} 那套数据驱动，所以写在这里；要按段细调就加 switch。
      */
     private static float attackRangeFor(int stage) {
         return 3.0f;
     }
 
+    /**
+     * 这一段的交战形态。
+     *
+     * <p>默认用近战预设；要逐段不同就写成 switch，要整段自定义就
+     * {@code ApproachParams.MELEE.withRange(8, 11).withDashProfile(3.0, 0.9, 24)} 这样链式改
+     * （见 §1.3 末尾的三种写法）。
+     */
+    private static AttackApproach.ApproachParams styleFor(int stage) {
+        return AttackApproach.ApproachParams.MELEE;
+    }
+
+    /** 出手编排：索敌 → 转向 / 吸附 / 突进 → 切动画 + 发包。 */
+    public static void execute(Player player) {
+        int stage = ActionStateMachine.comboStage;
+        float attackRange = attackRangeFor(stage);
+        AttackApproach.ApproachParams style = styleFor(stage);
+        CombatTargeting.Params params = style.targeting(attackRange);
+
+        // ① 索敌：按这一招的形态找目标（远程招式索敌 = 攻击距离）
+        LivingEntity target = CombatTargeting.acquire(player, params);
+        CombatTargeting.lock(player, target, params);
+
+        if (player instanceof LocalPlayer local && target != null) {
+            // ② 够不着 + 这一招允许突进 → 先起手，再冲过去
+            if (style.wantsDash() && AttackApproach.needsDash(local, target, attackRange)) {
+                // ⚠️ 顺序很重要：**先切动画**（起手姿势），它会被冻在那一帧，
+                //    人保持这个姿势冲过去，到位才解冻接着播 —— 这就是「突进期间冻结动画」的用法。
+                //    不要反过来（先冲再切动画），那样冻的是上一段动作，起手表现就没了。
+                beginStage(player, stage);
+
+                AttackApproach.begin(local, target, style, attackRange, () -> {
+                    // 到位：解冻 + 从这一刻重新计时（各段的刻数是相对动画起点的，
+                    // 突进把起点推后了，所以这里要拨回满值），然后才发包
+                    ActionStateMachine.resumeFromApproach(ticksFor(stage));
+                    NetworkManager.sendActionRequestToServer(
+                            NetworkManager.ActionKind.ATTACK, stage, false, target);
+                });
+                return;
+            }
+
+            // ③ 在范围内 → 只转身体（镜头不动）+ 出手推一小步（吸附）
+            AttackApproach.faceTarget(local, target, style);
+            AttackApproach.stepToward(local, target, attackRange);
+        }
+
+        beginStage(player, stage);
+        NetworkManager.sendActionRequestToServer(NetworkManager.ActionKind.ATTACK, stage, false, target);
+    }
+
+    /** 这一段的交战形态。逐段不同就写成 switch（见 §1.3 末尾的例子）。 */
+    private static AttackApproach.ApproachParams styleFor(int stage) {
+        return AttackApproach.ApproachParams.MELEE;
+    }
+
+    /** 这一段的动画刻数（突进到位后重新计时用）。和下面 switch 里的数字保持一致。 */
+    private static int ticksFor(int stage) {
+        return switch (stage) {
+            case 1 -> 40;
+            case 2 -> 48;
+            case 3 -> 25;
+            case 4 -> 30;
+            case 5 -> 50;
+            default -> 40;
+        };
+    }
+
     /**
      * 普攻。
      *
      * <p>第 5 段打完的段数回落规则：{@code Current_Sync_Depth >= 1} 时回到第 3 段（3→4→5→3 循环），
      * 否则回到第 1 段。
      */
-    public static void execute(Player player) {
-        int stage = ActionStateMachine.comboStage;
-
+    /**
+     * 只负责「切动画」这一半（原来 execute 的主体）。
+     *
+     * <p>发包<b>不在这里</b>做 —— 走突进的话要等到位那一刻才发，
+     * 好让服务端的伤害计时从到位起算。见 {@link #execute}。
+     */
+    private static void beginStage(Player player, int stage) {
         switch (stage) {
```

RPC 那一行从 `beginStage` 里**删掉**（改由 `execute` 在两处时机发）：

```diff
         // 通知服务端结算这一段的被动与伤害（服务端按段数决定给多少能量 / 豆 / 无敌帧）
-        NetworkManager.sendActionRequestToServer(NetworkManager.ActionKind.ATTACK, stage, false);
     }
```

**新增 import**：

```diff
+import net.luoshu.imaginarybranch.combat.AttackApproach;
+import net.luoshu.imaginarybranch.combat.targeting.CombatTargeting;
+import net.minecraft.client.player.LocalPlayer;
+import net.minecraft.world.entity.LivingEntity;
+import org.jetbrains.annotations.Nullable;
```

### 2.6 其余角色客户端（可选，同样套路）

`MiyabiSkillClient` / `MiyabiFinalClient` / `MiyabiSpAttackClient` 想同样手感的话，
照 §2.5 的形状改：**先索敌 → 再判断突进 → 切动画 → 发包**。技能一般不需要突进，所以最短版本是：

```java
        float attackRange = 3.0f;
        AttackApproach.ApproachParams style = AttackApproach.ApproachParams.MELEE;   // 远程招式换 RANGED
        CombatTargeting.Params params = style.targeting(attackRange);

        LivingEntity target = CombatTargeting.acquire(player, params);
        CombatTargeting.lock(player, target, params);
        if (player instanceof LocalPlayer local && target != null) {
            AttackApproach.faceTarget(local, target, style);       // 只转身体
            AttackApproach.stepToward(local, target, attackRange); // 出手推一小步
        }

        ActionStateMachine.changeState("skill_energy", 2, 25, 15);
        NetworkManager.sendActionRequestToServer(NetworkManager.ActionKind.SKILL, 0, hasEnergy, target);
```

**`MiyabiDodgeClient` 不要接** —— 闪避是「拉开距离」，自动转向会把「往后闪」变成「往怪身上闪」。

**某个角色完全不想参与索敌**（纯召唤、纯炮台、或者手感上就不该被带）：`execute` 里那一整块都不写，
只保留原来的 `changeState` + 发包即可 —— 索敌/转向/吸附/突进四件事**调用才生效**，
没有全局开关，所以「不想要」= 不调用。

### 2.7 服务端「锁着就不推位移」（可选，但强烈建议）

如果你们近战有服务端冲量（对应我们的 `ActionManager.scheduleStepMovement`），
在给冲量的地方加一句：

```java
        // 锁着目标 → 不推额外位移：贴上去交给客户端的突进/吸附，
        // 否则每段都往前推固定距离，打两下就穿到怪背后、丢目标
        if (CombatTargeting.isLocked(player)) {
            return;
        }
```

---

## 3. 兼容性核对（结论：基本零障碍）

| 项 | 结论 |
|---|---|
| `Mth.approachDegrees` / `wrapDegrees` / `lerp` | ✅ **1.21.1 都有，而且 `approachDegrees` 的实现和 26.2 逐行一致** —— 我解了两个版本的 `Mth.java` 对比：1.21.1 `approachDegrees(angle, limit, stepSize)`（第 328 行）= `approach(angle, angle + degreesDifference(angle, limit), stepSize)`，26.2 `approachDegrees(current, target, increment)`（第 274 行）同一套写法 → 转向代码原样抄没问题 |
| `player.setYBodyRot` / `hurtMarked` / `distanceTo` / `getLookAngle` / `getEntitiesOfClass` / `AABB` / `Vec3` | ✅ 1.21.1 同一套 API |
| `ResourceLocation` vs `Identifier` | ✅ 这三个新文件**一个都没直接用**标识符，所以不受影响 |
| `ServerPlayer`（只在 `onPlayerRemoved` 用） | ✅ 1.21.1 一样 |
| 事件与注册 | ✅ 你们已经在用 `@EventBusSubscriber(modid, value = Dist.CLIENT)` + `ClientTickEvent.Post` / `PlayerTickEvent.Post` / `PlayerEvent.PlayerLoggedOutEvent`，落点都一样 |
| 网络 | ✅ 都是 LDLib2 `@RPCPacket` + `RPCPacketDistributor`，只是给 `actionRequest` 加一个 int 参数 |
| Java | ✅ 都是 Java 21：`record`、switch 表达式、`Math.clamp`、`List.getFirst()` 都能用 |
| GeckoLib 4 / 5 | ✅ 索敌/吸附/转向**完全不碰 GeckoLib**。⚠️ 但「突进期间冻结动画」两边写法不同：**GeckoLib 4 的 `PlayState` 只有 `CONTINUE`/`STOP`，没有 `PAUSE`** → 用 `controller.setAnimationSpeed(0)` 冻结（`adjustTick` 恒返回 0 = 定在第 0 帧），解冻那帧再 `setAnimationSpeed(1)` + `forceAnimationReset()`。见 §2.2 |
| `TargetSeeker` | ✅ 新版索敌**不依赖**它（旧版才用） |
| 锁存哪 | ✅ `CombatTargeting` 自带静态表（`"C:"/"S:" + uuid`）→ **不用**接你们的 `PlayerVariables`；但**必须保留 side 前缀**，否则单机下客户端/服务端两份会互相覆盖 |
| `PlayerVariables.DEFAULT_ANIME_STATE` | ✅ 只在状态机内部用，`AttackApproach` 不引用它 |

---

## 4. 建议套用顺序

1. **§1.1 + §1.2**（两个索敌文件）→ 加个临时按键打印 `CombatTargeting.current(player)`，验证迟滞/丢锁。
2. **§2.1 ①②③** → 让状态机每 tick 跑索敌（此时还没人索敌，等于空跑，安全）。
3. **§1.3 + §2.1 ④⑤⑥ + §2.2**（突进/转向 + 冻结）→ 先只调 `faceTarget`：确认**模型转向、镜头纹丝不动**。
4. **§2.5**（普攻接线）→ 验证：远距离出手先冲过去、动画冻在起手帧、到位继续；退半步再打时第二刀自己补上。
5. **§2.3 + §2.4**（服务端锁 + 每 tick + 下线清理）→ 验证召唤物能跟主人目标；**重点验证「服务端位移不会在首次攻击后永久失效」**（这条最容易漏，症状很隐蔽）。
6. 可选：§2.6 技能/大招、§2.7 服务端位移跳过。

## 5. 需要注意的两个坑（我们踩过）

1. **服务端那份锁必须每 tick 校验**（§2.3 ①）。少这一句 → 锁永不过期 → 所有「锁着就不推位移」的逻辑在**第一次攻击之后永久生效**，而且单人存档里客户端 100 刻就松锁了，两端行为分叉，很难查。
2. **收尾动画的状态要带一个短锁**（如果你们也要做「打完接 xxx_end」）：`changeState(收尾, 优先级, 刻数, 0, 0)` 会被「1 级常态输入打断后摇」在**下一帧**顶掉 —— 玩家按着 WASD 时收尾动画一帧都看不到。我们改成前 12 刻不可被移动打断（输入不冻结、人照样能走），之后随便打断。

---

## 6. 排查：「加了代码但索敌不生效」

### 6.1 先分清是哪一种「不生效」

索敌**本身没有画面**，它的效果是间接的（转向 / 一小步吸附 / 突进），近身时非常不明显。
所以第一步是打一行日志，确认**到底锁没锁到**：

```java
        LivingEntity target = CombatTargeting.acquire(player, params);
        CombatTargeting.lock(player, target, params);
        LOGGER.info("[索敌] stage={} → target={} 距离={}", stage, target,
                target == null ? -1 : player.distanceTo(target));
```

| 日志结果 | 说明 | 下一步 |
|---|---|---|
| `target=null`，附近**没有**怪 | 测试环境问题 | 放几只原版僵尸再试 |
| `target=null`，附近**有**怪 | **策略把它判成「不能打」** → 见 6.2 | 登记你的怪 |
| `target=某怪`，但你没看出效果 | 索敌是好的，只是近身时转向/吸附太轻微 | 站到 **5~6 格外**出手，应该能看到「冲过去」；或按 6.3 加一个 HUD 标记 |

### 6.2 最常见的原因：默认策略不认你们的怪

`TargetPolicy.DEFAULT` 只接受三类：原版 `Enemy`（僵尸骷髅那种）、
`Mob.isAggressive()` 为 true 的、以及**用 `registerHostile` 登记过的**。

⚠️ **注意两个陷阱**：
- `Mob.isAggressive()` 在 `Mob` 里默认就是 `return false`，**不是「会追着打」的意思** ——
  自己写的怪继承 `PathfinderMob`/`Mob` 也不会自动满足这一条；
- 原版 `Monster` 是 `Enemy` ✔，但**你们自研的怪如果不是继承 `Monster`**，就不算。

解法就是一行（放在客户端初始化里，比如 `FMLClientSetupEvent`）：

```java
        // 你们的怪基类（换成实际类名）
        TargetPolicy.registerHostile(living -> living instanceof ImaginaryMonsterBase);
```

**临时验证**（一行，不改策略文件）—— 用「什么都算」的策略出手，如果这下就生效了，说明就是策略问题：

```java
        CombatTargeting.Params params = new CombatTargeting.Params(
                6.0, 55.0, 9.0, 80.0,
                (owner, candidate) -> candidate instanceof LivingEntity);   // ← 什么都锁
```

### 6.3 第二种：锁到了但「看不出来」

近身时的效果确实轻微（身体转过去 + 推半个格）。想确认，最直接的是把锁定目标画出来，
或者干脆先站远一点（5~6 格）打一下 —— 那时应该能看到**明显的一次突进**。

⚠️ 如果**远距离出手后人物僵住不动、也不冲过去**，那是 `ActionStateMachine.onClientTick` 里少了这句：

```java
        AttackApproach.tick(player);   // §2.1 ③
```

因为 `begin()` 会把动画冻结（`setApproachFrozen(true)`），没人 tick 就永远不解冻 →
表现就是「卡在起手帧」。这一条很好认：**动画冻住不动**。

### 6.4 一个容易漏的原始缺陷：`beginStage` 里那行 RPC 要删掉

改造后发包由 `execute` 在「到位那一刻」做，所以 `beginStage`（原来的 `execute` 主体）
**结尾那行 `NetworkManager.sendActionRequestToServer(...)` 必须删掉**，否则每次出手发两次包
（伤害/能量翻倍）。


