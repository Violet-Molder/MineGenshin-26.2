package com.linweiyun.genshin.core.system.combat.animation.state;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.client.combat.AttackApproach;
import com.linweiyun.genshin.core.system.combat.targeting.CombatTargeting;
import com.linweiyun.genshin.core.system.combat.animation.action.CharacterActionHandler;
import com.linweiyun.genshin.core.system.combat.animation.action.CharacterActions;
import com.linweiyun.genshin.core.system.combat.animation.config.CharacterAnimations;
import com.linweiyun.genshin.client.render.character.AttachmentHelper;
import com.linweiyun.genshin.config.character.CharacterSystemConfig;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.network.ActionServer;
import com.linweiyun.genshin.core.network.NetworkManager;
import com.linweiyun.genshin.core.system.combat.action.InterruptReason;
import com.linweiyun.genshin.core.system.combat.action.ActionManager;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端动作状态机 —— 动画控制系统的中枢，<b>只服务本地玩家</b>
 * （远程玩家的动画靠同步变量驱动，见 {@code AnimationStateSync}）。
 *
 * <p>移植自参考2 的 {@code ActionStateMachine}，本项目做了三处适配：
 * <ol>
 *   <li>状态机与动画时长仍由「资源 → 天赋」链路提供（{@code VesnaResources.ACTION_DATA} →
 *       {@code VesnaTalent} → {@code ActionSet}），动作类只负责把数值喂进来。</li>
 *   <li>音效 id 随动画同步包一起发出，服务端再广播给其他玩家。</li>
 *   <li>「动作系统开关」关闭时，{@code changeState} 不再设硬直 / 移动锁，{@code canInterrupt} 恒真
 *       —— 即「所有按键及时响应，不采用前后摇」。</li>
 * </ol>
 *
 * <h2>动画层级（优先级）</h2>
 * <ul>
 *   <li>1 = 常态（走跑跳蹲游）</li>
 *   <li>2 = 普攻 / 战技</li>
 *   <li>3 = 闪避</li>
 *   <li>4 = 大招（绝对霸体，不可打断）</li>
 * </ul>
 *
 * <h2>三个计时器</h2>
 * <ul>
 *   <li>{@link #animationTick} —— 动画总时长，归零即动作结束并自动回常态。</li>
 *   <li>{@link #actionLockFrames} —— 前摇 + 硬直，期间同级或更低优先级不能打断。</li>
 *   <li>{@link #movementLockFrames} —— 额外的移动封锁（定身）。</li>
 * </ul>
 */
@EventBusSubscriber(modid = Minegenshin.MOD_ID, value = Dist.CLIENT)
public final class ActionStateMachine {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 常态状态名。与 {@code ActionStep.animation} 的取值域不冲突。 */
    public static final String DEFAULT_STATE = "default";

    public static final int PRIO_NORMAL = 1;
    public static final int PRIO_ATTACK = 2;
    public static final int PRIO_DODGE = 3;
    public static final int PRIO_FINAL = 4;

    // ---- 状态机核心内存（只服务本地玩家）----

    /** 当前动作状态名；{@link #DEFAULT_STATE} 表示常态。 */
    public static String currentState = DEFAULT_STATE;

    /** 动画层级：1 常态 / 2 普攻·技能 / 3 闪避 / 4 大招。 */
    public static int currentPriority = PRIO_NORMAL;

    /** 当前动作剩余总刻数。 */
    public static int animationTick = 0;

    /** 前摇 + 硬直剩余刻数。 */
    public static int actionLockFrames = 0;

    /** 移动封锁剩余刻数。 */
    public static int movementLockFrames = 0;

    /** 当前连击段数（1 起）。 */
    public static int comboStage = 1;

    /** 连击保持窗口：硬直结束后还能接续下一段的剩余刻数。 */
    public static int comboWindowFrames = 0;

    /** 左键按住时长（用于重击蓄力判定）。 */
    public static int attackHoldTimer = 0;

    /** 左键是否处于按住状态。 */
    public static boolean isAttackButtonDown = false;

    /** 战技键是否处于按住状态（短按 / 长按有区别的角色才用得上）。 */
    public static boolean isSkillButtonDown = false;

    /** 战技键按住时长（刻）。 */
    public static int skillHoldTimer = 0;

    /** 长按战技是否已经触发过（避免按住期间反复触发）。 */
    private static boolean skillHoldTriggered = false;

    /** 重击是否已经因为这次按住触发过（松手才复位）。 */
    public static boolean chargedAttackTriggered = false;

    /** 长按战技的判定阈值（刻）：对齐旧的 1000ms 手感。 */
    public static final int SKILL_HOLD_TICKS = 20;

    /**
     * 收尾动画（例如普攻最后一段的 {@code xxx_end}）。
     *
     * <p>当前动作播完时如果这里排着一个名字，状态机不会回常态，而是直接续上它 ——
     * 否则模型会停在上一段的最后一帧，看起来像卡住。
     */
    @Nullable
    private static String followUpState = null;

    /** 收尾动画播多少刻。 */
    private static int followUpTicks = 0;

    /**
     * 突进冻结 —— 近战贴脸期间把动画暂停在起手那一帧。
     *
     * <p>由客户端 {@code AttackApproach} 开关；{@code PlayerAnimationController}
     * 看到它为 true 就返回 {@link com.geckolib.animation.object.PlayState#PAUSE}。
     */
    private static boolean approachFrozen = false;

    /** 当前动作的生效攻击距离，突进判定用（由动作编排在切状态时写入）。 */
    private static double currentAttackRange = 3.0;

    /**
     * 动作序号：每切一次状态 +1。
     *
     * <p>用来给「音效随机」做确定性种子 —— 同一刀在所有客户端算出同一个变体，
     * 不需要额外同步包。
     */
    private static int actionSequence = 0;

    public static int actionSequence() {
        return actionSequence;
    }

    /** 客户端延迟任务队列（1 tick 一步），被打断时整体清空。 */
    private static final List<ClientTask> CLIENT_TASKS = new ArrayList<>();

    private ActionStateMachine() {
    }

    // ---- 延迟任务队列 ----

    public static void queueClientWork(int delayTicks, Runnable action) {
        CLIENT_TASKS.add(new ClientTask(delayTicks, action));
    }

    public static final class ClientTask {
        public int delay;
        public final Runnable action;

        public ClientTask(int delay, Runnable action) {
            this.delay = delay;
            this.action = action;
        }
    }

    // ---- 每客户端 tick ----

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        restoreFrozenInput(player);
        runDelayedTasks();

        // 索敌校验 + 突进/转向（都在动画判定之前，保证当帧就生效）
        CombatTargeting.tick(player);
        AttackApproach.tick(player);

        // 左键按住 → 交给当前角色的动作编排做蓄力判定
        if (isAttackButtonDown) {
            attackHoldTimer++;

            // 大招期间吞掉蓄力指令；重击一次按住只触发一次
            if (currentPriority < PRIO_FINAL && !chargedAttackTriggered) {
                CharacterActionHandler handler = handlerFor(player);
                if (handler != null) {
                    handler.tickCharge(player, attackHoldTimer);
                }
            }
        } else {
            attackHoldTimer = 0;
        }

        // 战技按住 → 短按/长按有区别的角色等到阈值再放长按版
        if (isSkillButtonDown && !skillHoldTriggered) {
            skillHoldTimer++;
            if (skillHoldTimer >= SKILL_HOLD_TICKS && hasSkillHoldVariant(player)) {
                // 只有真的放出去了才算触发；被硬直挡下来的话下一 tick 继续试
                skillHoldTriggered = trySkill(player, true);
            }
        }

        // 倒计时：前摇/硬直、移动锁
        if (actionLockFrames > 0) {
            actionLockFrames--;
        }
        if (movementLockFrames > 0) {
            movementLockFrames--;
        }

        // 只有硬直彻底结束（进入后摇）时，连击窗口才开始流失
        if (actionLockFrames <= 0 && comboWindowFrames > 0) {
            comboWindowFrames--;

            if (comboWindowFrames <= 0) {
                comboStage = 1; // 超时才重置连击段数
            }
        }

        // 1 级常态输入打断高级动作的后摇。
        // 定身期间输入已经被换成 FrozenInput，这里再看一遍输入会读到「玩家其实按着 W」，
        // 所以定身没结束就不做打断判定 —— 否则一按攻击就被自己的移动输入取消。
        if (movementLockFrames <= 0 && actionLockFrames <= 0
                && animationTick > 0 && !DEFAULT_STATE.equals(currentState)) {
            ClientInput input = player.input;
            boolean isMoving = input.getMoveVector().lengthSquared() > 1.0E-5f;
            boolean isJumping = input.keyPresses.jump();
            boolean isCrouching = input.keyPresses.shift();

            if (isMoving || isJumping || isCrouching) {
                resetToDefault();
                // 只复位本地表现的话，服务端还会把剩下的 hits 打完（伤害没有动画配）
                ActionServer.interruptActionToServer(InterruptReason.JUMP.ordinal());
                return;
            }
        }

        // 被动捕获：格挡成功之类的「服务端置位 → 客户端播动画」
        CharacterActionHandler handler = handlerFor(player);
        if (handler != null) {
            handler.passiveTick(player);
        }

        // 其他玩家切动作时按状态名本地补音效
        AnimationStateSync.tickRemoteStateSounds();

        // 动画总时长倒数。
        //
        // ⚠️ 突进期间**整块跳过**：动画被冻在起手帧（渲染那边返回 PAUSE），时间轴就不该再走。
        //    否则「动画自然播完 → resetToDefault」会抢在到位之前发生，
        //    把这一刀连同还没发出去的服务端请求一起吞掉（攻击没伤害、冷却也不扣）。
        //    突进自己有 MAX_APPROACH_TICKS 兜底，冻结不会卡死。
        if (!approachFrozen) {
            if (animationTick > 0) {
                animationTick--;
            } else if (!DEFAULT_STATE.equals(currentState)) {
                if (followUpState != null) {
                    // 还有收尾动画要接（例如普攻最后一段 → xxx_end）：
                    // 不切回常态，直接续上，否则模型会停在上一段的最后一帧
                    startFollowUp();
                } else {
                    // 自然播完 → 回常态，并断开连击
                    resetToDefault();
                }
            }
        }
    }

    /**
     * 收尾动画至少要演多少刻才允许被常态输入打断。
     *
     * <p><b>为什么需要</b>：收尾状态原来是不带任何锁的（{@code lockFrames = 0}），
     * 于是「下一帧就结束」—— 玩家只要按着 WASD（打起来基本都按着），
     * 收尾动画<b>一帧都看不到</b>就被 {@code resetToDefault} 顶掉了，
     * 表现就是「第六段打完没有任何收尾」。
     *
     * <p>给一个短锁之后：这几刻内移动不会取消它（但输入没有被冻结，人照样能走），
     * 之后随便打断 —— 也就是「后摇可以被打断」，只是先演一下。
     * 闪避（优先级 3）和大招（4）不受这个锁限制，随时能取消。
     */
    public static final int FOLLOW_UP_LOCK_TICKS = 12;

    /** 把排队中的收尾动画接上。 */
    private static void startFollowUp() {
        String next = followUpState;
        int ticks = followUpTicks;
        int priority = currentPriority;

        followUpState = null;
        followUpTicks = 0;

        int lock = Math.min(ticks, FOLLOW_UP_LOCK_TICKS);
        LOGGER.info("[MineGenshin][收尾] 接上 '{}'（{} 刻，前 {} 刻不可被移动打断）", next, ticks, lock);
        changeState(next, priority, ticks, lock, 0);
    }

    private static void runDelayedTasks() {
        if (CLIENT_TASKS.isEmpty()) {
            return;
        }

        // 快照复制：允许任务在执行过程中继续安全地注册新任务
        List<ClientTask> tasksToRun = new ArrayList<>(CLIENT_TASKS);
        CLIENT_TASKS.clear();

        for (ClientTask task : tasksToRun) {
            task.delay--;

            if (task.delay <= 0) {
                try {
                    task.action.run();
                } catch (Exception exception) {
                    LOGGER.error("[MineGenshin] 客户端延迟任务执行失败", exception);
                }
            } else {
                CLIENT_TASKS.add(task);
            }
        }
    }

    // ---- 按键物理拦截（0 延迟定身）----

    /**
     * 定身用的输入替身：{@code moveVector} 恒为零向量，按键全空。
     *
     * <p>26.2 的 {@code Input} 已经变成 record，参考2 那种「就地改 forwardImpulse」的写法不再可行，
     * 所以这里换成一个只读的 {@link ClientInput} 子类，锁移动期间顶替玩家原本的输入实例。
     */
    private static final class FrozenInput extends ClientInput {
        @Override
        public Vec2 getMoveVector() {
            return Vec2.ZERO;
        }

        @Override
        public boolean hasForwardImpulse() {
            return false;
        }

        /**
         * 原版自动跳跃会在 {@code aiStep} 里对这个实例调 {@code makeJump()}，
         * 而 {@code ClientInput.makeJump()} 是「把 jump 置 true」的就地修改 ——
         * 共享的静态实例一旦被改过一次，之后每次定身都会自己起跳。所以这里直接吞掉。
         */
        @Override
        public void makeJump() {
        }
    }

    private static final ClientInput FROZEN_INPUT = new FrozenInput();

    @Nullable
    private static ClientInput savedInput;

    @SubscribeEvent
    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        if (movementLockFrames <= 0) {
            return;
        }

        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }

        // 清掉跳跃 / 潜行 / 疾跑，并把移动向量换成零
        event.getInput().keyPresses = Input.EMPTY;

        if (player.input != FROZEN_INPUT) {
            savedInput = player.input;
            player.input = FROZEN_INPUT;
        }
    }

    /**
     * 移动锁结束后把玩家原本的输入实例还回去（由每 tick 的状态机统一处理）。
     *
     * <p>{@code savedInput} 为空时<b>什么都不做</b>：宁可多冻一 tick，也不能塞一个
     * 不会被 tick 的空 {@code ClientInput} 进去 —— 那会让玩家这一局再也动不了。
     */
    private static void restoreFrozenInput(LocalPlayer player) {
        if (player.input != FROZEN_INPUT || savedInput == null) {
            return;
        }
        player.input = savedInput;
        savedInput = null;
    }

    /** 本地玩家重生时复位状态机（字段是静态的，不会随玩家实体重建，否则大招姿势会残留）。 */
    @SubscribeEvent
    public static void onClientPlayerRespawn(ClientPlayerNetworkEvent.Clone event) {
        resetToDefault();
    }

    // ---- 打断规则 ----

    public static boolean canInterrupt(int requestedPriority) {
        // 动作系统关闭：不做前摇/硬直判定，任何动作都能立刻接上
        if (!actionSystemEnabled()) {
            return true;
        }

        // 大招期间不可打断
        if (currentPriority >= PRIO_FINAL) {
            return false;
        }

        if (requestedPriority > currentPriority) {
            return true; // 高级打断低级
        }

        return actionLockFrames <= 0; // 同级或低级必须等前摇和硬直结束
    }

    // ---- 动作入口（供键位调用）----

    public static void tryAttack(Player player) {
        if (currentPriority >= PRIO_FINAL) {
            return;
        }

        if (!canInterrupt(PRIO_ATTACK)) {
            return;
        }

        handlerFor(player).attack(player);
    }

    /** @return 这次战技请求有没有真的放出去（被硬直挡住、或角色没配这个变体都算 false） */
    public static boolean trySkill(Player player, boolean longPress) {
        if (!canInterrupt(PRIO_ATTACK)) {
            return false;
        }

        return handlerFor(player).skill(player, longPress);
    }

    public static void tryDodge(Player player) {
        if (!canInterrupt(PRIO_DODGE)) {
            return;
        }

        CharacterActionHandler handler = handlerFor(player);
        if (handler != null) {
            handler.dodge(player);
        }
    }

    public static void tryUltimate(Player player) {
        if (!canInterrupt(PRIO_FINAL)) {
            return;
        }

        CharacterActionHandler handler = handlerFor(player);
        if (handler != null) {
            handler.ultimate(player);
        }
    }

    /** 左键松开：交回角色的蓄力判定。 */
    public static void releaseAttack(Player player) {
        isAttackButtonDown = false;
        int chargeTime = attackHoldTimer;
        attackHoldTimer = 0;
        chargedAttackTriggered = false;

        CharacterActionHandler handler = handlerFor(player);
        if (handler != null) {
            handler.releaseAttack(player, chargeTime);
        }
    }

    /**
     * 战技键按下。
     *
     * <p>短按 / 长按有区别的角色（短 CD ≠ 长 CD）先不出手，等松手或按住到
     * {@link #SKILL_HOLD_TICKS} 刻再决定；没有区别的角色按下即放，保证 0 延迟。
     */
    public static void pressSkill(Player player) {
        isSkillButtonDown = true;
        skillHoldTimer = 0;
        skillHoldTriggered = false;

        if (!hasSkillHoldVariant(player)) {
            trySkill(player, false);
        }
    }

    /** 战技键松开：没触发过长按就补一个短按。 */
    public static void releaseSkill(Player player) {
        boolean wasHoldVariant = hasSkillHoldVariant(player);

        isSkillButtonDown = false;

        if (wasHoldVariant && !skillHoldTriggered) {
            trySkill(player, false);
        }

        skillHoldTimer = 0;
        skillHoldTriggered = false;
    }

    /** 这个角色的短按和长按是不是两套（CD 不同即视为两套）。 */
    private static boolean hasSkillHoldVariant(Player player) {
        PGCharacter character = currentCharacter(player);
        if (character == null) {
            return false;
        }
        return character.getSkillShortMaxCooldownTick() != character.getSkillLongMaxCooldownTick();
    }

    @Nullable
    private static PGCharacter currentCharacter(Player player) {
        PlayerCharactersAttachment attachment =
                player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        return attachment == null ? null : attachment.getCurrentCharacter();
    }

    // ---- 状态切换 / 复位 ----

    /**
     * 给当前动作排一个收尾动画：这次动作播完时自动接上它，而不是切回常态。
     *
     * <p>用于普攻最后一段这类「打完要跟一个 {@code xxx_end}」的动作 ——
     * 不接的话模型会停在最后一帧。任何后续的 {@link #changeState} / {@link #resetToDefault}
     * 都会把它清掉，所以不会在别的动作后面乱接。
     */
    public static void queueFollowUpState(String stateName, int totalTicks) {
        if (stateName == null || stateName.isEmpty() || totalTicks <= 0) {
            followUpState = null;
            followUpTicks = 0;
            return;
        }
        followUpState = stateName;
        followUpTicks = totalTicks;
        LOGGER.info("[MineGenshin][收尾] 排队 '{}'（{} 刻）", stateName, totalTicks);
    }

    /** 取消排队中的收尾动画。 */
    public static void clearFollowUpState() {
        followUpState = null;
        followUpTicks = 0;
    }

    // ---- 突进冻结 / 攻击距离 ----

    /** 突进期间冻结动画（渲染控制器会返回 PAUSE）。 */
    public static void setApproachFrozen(boolean frozen) {
        approachFrozen = frozen;
    }

    public static boolean isApproachFrozen() {
        return approachFrozen;
    }

    /** 记录这次动作的生效攻击距离（索敌/吸附用它判断「够不够得着」）。 */
    public static void setCurrentAttackRange(double range) {
        currentAttackRange = range > 0 ? range : 3.0;
    }

    /** 这次动作的生效攻击距离。 */
    public static double currentAttackRange() {
        return currentAttackRange;
    }

    /**
     * 突进到位：解冻动画，并把总时长<b>从这一刻重新起算</b>。
     *
     * <p>这是「伤害延迟按到位时刻算」的关键 —— {@code hits[].delay} 本来是相对动画起点，
     * 突进把起点推后了，所以这里把 {@code animationTick} 拨回满值，
     * 动画则从冻结的那一帧继续往下播。
     */
    public static void resumeFromApproach(int totalTicks) {
        approachFrozen = false;
        animationTick = Math.max(1, totalTicks);
    }

    /** 切到新状态，<b>不动</b>移动锁（上一状态设下的定身会继续自然倒数完）。 */
    public static void changeState(String newState, int priority, int totalTicks, int lockFrames) {
        changeState(newState, priority, totalTicks, lockFrames, -1);
    }

    /**
     * 切到新状态：清理旧状态残留、写入状态与计时器、按需设置移动锁，并自动发同步包给服务端。
     *
     * @param movementLockTicks 移动封锁刻数；传负数表示保持当前值不变
     */
    public static void changeState(String newState, int priority, int totalTicks, int lockFrames,
                                   int movementLockTicks) {
        LocalPlayer player = Minecraft.getInstance().player;

        // 换动作 → 上一次出手的突进作废。
        // 不取消的话会出现「已经闪避走了，半秒后突然补一刀」——
        // 突进是「上一次出手」的延续，新状态一开始它就该结束。
        // （突进自己的起手是先 changeState 再 begin，所以不会误杀自己）
        AttackApproach.cancel();

        dispatchCleanup(currentState, player);

        // 换动作了 → 上一段排的收尾动画作废（收尾自己接自己时不走这里）
        // 打日志是因为「排了收尾却一直没播」最常见的原因就是被下一次出手顶掉，
        // 有这一行能一眼看出来（不然只能猜）。
        if (followUpState != null) {
            LOGGER.info("[MineGenshin][收尾] 排队中的 '{}' 被 '{}' 顶掉", followUpState, newState);
        }
        clearFollowUpState();

        currentState = newState;
        currentPriority = priority;
        animationTick = totalTicks;
        actionSequence++;

        // 动作系统关闭：不设硬直与定身，按键随时可以打断
        boolean actionSystem = actionSystemEnabled();
        actionLockFrames = actionSystem ? lockFrames : 0;

        if (movementLockTicks >= 0) {
            movementLockFrames = actionSystem ? movementLockTicks : 0;
        }

        if (priority >= PRIO_ATTACK) {
            comboWindowFrames = 30;
        }

        if (player != null) {
            // 本地 0 延迟播放；服务端会给其他玩家播且排除自己，所以不会重音
            // （远端玩家由各自的客户端按同一个状态名查 CharacterAnimations.soundForState）
            playLocalSound(player, soundForState(player, newState));
            sendStateToServer(newState, totalTicks);
        }
    }

    /** 回常态。清空延迟任务队列并通知服务端把远端表现也复位。 */
    public static void resetToDefault() {
        LocalPlayer player = Minecraft.getInstance().player;

        // 回常态 = 这次出手结束了：还在突进的话立刻停（不走到位回调，也不补发攻击请求）
        AttackApproach.cancel();

        dispatchCleanup(currentState, player);

        currentState = DEFAULT_STATE;
        currentPriority = PRIO_NORMAL;
        animationTick = 0;
        actionLockFrames = 0;
        movementLockFrames = 0;

        clearFollowUpState();
        CLIENT_TASKS.clear();

        if (player != null) {
            sendStateToServer(DEFAULT_STATE, 0);
        }
    }

    private static void sendStateToServer(String stateName, int totalTicks) {
        LocalPlayer player = Minecraft.getInstance().player;

        // 连接还没建立/已断开时跳过（例如重生瞬间被状态机复位触发）
        if (player == null || Minecraft.getInstance().getConnection() == null) {
            return;
        }

        NetworkManager.sendAnimationStateToServer(stateName, totalTicks);
    }

    /** 把状态被覆盖的消息交给当前角色的动作编排，用于清理残留。 */
    private static void dispatchCleanup(String stateToClean, @Nullable Player player) {
        if (player == null || DEFAULT_STATE.equals(stateToClean)) {
            return;
        }

        CharacterActionHandler handler = handlerFor(player);
        if (handler != null) {
            handler.onStateInterrupted(stateToClean, player);
        }
    }

    // ---- 音效 ----

    /** 本地瞬间播放音效（音量/音高默认 1）。 */
    public static void playLocalSound(@Nullable Player player, @Nullable String soundId) {
        playLocalSound(player, soundId, 1.0F, 1.0F);
    }

    /**
     * 本地瞬间播放音效。
     *
     * <p><b>不要求音效在注册表里</b>：角色音效是数据文件定义的
     * （{@code character/<角色id>/sounds.json}），没有走过 {@code DeferredRegister}，
     * 所以查不到时现造一个 {@link SoundEvent#createVariableRangeEvent} ——
     * 客户端按 location 去声音注册表找文件，能找到就响。
     * 两条路都没有时静默跳过，不会报错、也不会刷屏。
     *
     * @param volume 音量（0.01 起，0 会被原版当成不播）
     * @param pitch  音高（0.01 起）
     */
    public static void playLocalSound(@Nullable Player player, @Nullable String soundId,
                                      float volume, float pitch) {
        if (player == null || soundId == null || soundId.isEmpty()) {
            return;
        }

        Identifier id = Identifier.tryParse(soundId);
        if (id == null) {
            return;
        }

        SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.getValue(id);
        if (soundEvent == null) {
            soundEvent = SoundEvent.createVariableRangeEvent(id);
        }

        player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), soundEvent,
                SoundSource.PLAYERS, Math.max(0.01F, volume), Math.max(0.01F, pitch), false);
    }

    // ---- 内部工具 ----

    /** 取本地玩家当前角色的动作编排；没戴角色饰品时返回 null。 */
    @Nullable
    private static CharacterActionHandler handlerFor(Player player) {
        return CharacterActions.getFor(player);
    }

    /** 取本地玩家当前角色某个状态的音效 id。 */
    @Nullable
    private static String soundForState(Player player, String stateName) {
        CharacterAnimations animations = CharacterActions.animationsFor(player);
        return animations == null ? null : animations.soundForState(stateName);
    }

    /** 本地玩家当前角色是否启用了动作系统（前后摇 / 延迟伤害）。 */
    public static boolean actionSystemEnabled() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return true;
        }
        return CharacterSystemConfig.actionSystem(AttachmentHelper.getActiveCharacterId(player));
    }
}
