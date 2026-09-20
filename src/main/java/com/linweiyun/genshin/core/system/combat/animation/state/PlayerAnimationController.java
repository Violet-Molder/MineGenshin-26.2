package com.linweiyun.genshin.core.system.combat.animation.state;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationPoint;
import com.geckolib.animation.state.AnimationTest;
import com.linweiyun.genshin.core.system.combat.animation.action.CharacterActions;
import com.linweiyun.genshin.core.system.combat.animation.animatable.IPlayerAnimatableProxy;
import com.linweiyun.genshin.core.system.combat.animation.config.CharacterAnimations;
import com.linweiyun.genshin.core.system.combat.animation.config.FirstPersonAnims;
import com.linweiyun.genshin.core.system.combat.animation.config.LocomotionAnims;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 角色动画控制器 —— 决定「这一帧该播哪个动画」。
 *
 * <p>移植自参考2 的 {@code PlayerAnimationController}，角色差异全部走
 * {@link CharacterAnimations} 数据（动画名、特殊动画名单、过渡刻数）。
 *
 * <h2>两种状态来源</h2>
 * <ul>
 *   <li><b>本地玩家</b>：读 {@link ActionStateMachine#currentState}，按键当帧就出动画（0 延迟）。</li>
 *   <li><b>其他玩家</b>：读同步过来的动画状态附件。</li>
 * </ul>
 *
 * <h2>智能顺切</h2>
 * 常态动画之间平滑过渡 {@code exitTransitionTicks} 刻；只要涉及特殊动作动画，
 * 过渡时间归零硬切，避免动作姿势被插值混形。
 *
 * <h2>动画保护</h2>
 * 每一个要播的动画名都先过 {@link AnimationAvailability}：角色动画文件里没有这个名字时
 * <b>保持当前动画不动</b>，绝不切进空状态 —— 空状态会让模型露出原始姿态（部件、特效全露、
 * 角色呆站着），而且要等到下一个存在的动画才会恢复。常态动画缺失时退到 idle。
 *
 * <h2>与参考2 的差异</h2>
 * 参考2 在构造控制器时就把 {@code CharacterAnimations} 固定下来；本项目支持中途换角色，
 * 所以每帧按玩家当前角色现查（{@link CharacterActions#animationsFor(Player)}）。
 */
public final class PlayerAnimationController {

    private PlayerAnimationController() {
    }

    /**
     * 建一个角色动画控制器。
     *
     * @param animatable 角色动画代理对象
     */
    public static <T extends GeoAnimatable & IPlayerAnimatableProxy> AnimationController<T> create(T animatable) {
        return new AnimationController<T>("movement_controller", 5,
                state -> handle(state, animatable.getPlayerEntity()));
    }

    private static <T extends GeoAnimatable> PlayState handle(AnimationTest<T> state, @Nullable Player player) {
        if (player == null) {
            return PlayState.STOP;
        }

        AnimationController<T> controller = state.controller();
        CharacterAnimations animations = CharacterActions.animationsFor(player);

        // 1. 取目标状态：本地玩家读状态机，其他玩家读同步变量
        boolean isLocalPlayer = player == Minecraft.getInstance().player;
        String targetAnim = AnimationStateSync.stateOf(player);
        boolean hasActionState = targetAnim != null && !targetAnim.isEmpty()
                && !ActionStateMachine.DEFAULT_STATE.equals(targetAnim);

        // 2. 计算运动状态
        double movedX = player.getX() - player.xo;
        double movedY = player.getY() - player.yo;
        double movedZ = player.getZ() - player.zo;

        boolean isMoving;
        if (isLocalPlayer && player instanceof LocalPlayer localPlayer) {
            // 本地玩家用原始输入判定：更灵敏，且动作锁期间视为不动
            isMoving = ActionStateMachine.actionLockFrames <= 0
                    && localPlayer.input.getMoveVector().lengthSquared() > 1.0E-5f;
        } else {
            isMoving = (movedX * movedX + movedZ * movedZ) > 0.00005;
        }

        // 3. 选出这一帧该播的动画；null = 没有可用动画，保持当前帧不动
        RawAnimation target = hasActionState
                ? pickAction(player, targetAnim)
                : pickLocomotion(player, animations.locomotion(), isMoving, movedX, movedY, movedZ);
        if (target == null) {
            // 有东西在播就继续播，避免闪回原始姿态
            return controller.getCurrentAnimationPoint() == null ? PlayState.STOP : PlayState.CONTINUE;
        }

        // 突进期间：把动作暂停在起手那一帧，贴到目标再解冻继续播
        if (ActionStateMachine.isApproachFrozen() && targetName(target).equals(currentAnimationName(controller))) {
            return PlayState.PAUSE;
        }

        // 4. 智能顺切：决定本次切换用多少刻过渡
        //
        // GeckoLib 5 的非 triggered 路径会直接读 transitionTicks 字段
        // （AnimationController#initializeNewAnimation），所以这里设的值当帧就生效。
        boolean isTargetSpecial = animations.specialAnims().contains(targetName(target));
        String currentPlayingAnim = currentAnimationName(controller);
        boolean isCurrentlySpecial = animations.specialAnims().contains(currentPlayingAnim);

        // 还没有任何动画在播（首次渲染 / 控制器刚被重置）时也要硬切：
        // 带过渡的切换会从「原始姿态」插值进来，那几帧部件和特效会全部露出来。
        boolean nothingPlaying = controller.getCurrentAnimationPoint() == null
                || controller.getCurrentTimelineTime() < 0;

        if (isTargetSpecial || nothingPlaying || (isCurrentlySpecial && !hasActionState)) {
            // 进入动作 / 从定格姿势回常态：硬切，避免姿势被插值混形
            controller.setTransitionTicks(0);
        } else {
            controller.setTransitionTicks(animations.exitTransitionTicks());
        }

        return state.setAndContinue(target);
    }

    // ==================== 选动画 ====================

    /**
     * 动作动画：播一次，然后<b>停在最后一帧</b>等状态机接手。
     *
     * <p><b>为什么是 {@code thenPlayAndHold} 而不是 {@code thenPlay}</b>：
     * 动作长度和状态时长是对齐的（比如 {@code attack_1} 动画 2.0 秒、这一段也是 40 刻），
     * 用 {@code thenPlay} 的话动画会在最后一刻「播完即失效」，
     * 而状态机要到下一帧才切走 —— 中间那一帧模型没有任何动画在驱动，
     * 就露出<b>原始姿态</b>（所有部件与特效全开、角色呆站），闪一下很难看。
     *
     * <p>改成「播完停在最后一帧」后，最后一帧会一直撑到状态机切到下一个状态
     * （收尾动画 / 常态），中间不存在空档。副作用正好是作者想要的：
     * 「hold on last frame」型的动画（比如 {@code air_attack_long}）现在真的会保持住。
     *
     * <p>角色没有这个动画时返回 {@code null}（调用方会保持当前帧不动）。
     */
    @Nullable
    private static RawAnimation pickAction(Player player, String animationName) {
        // 第一人称优先试 fp_ 版；没写就照播普通版（复用模式，由机位把角度摆正）
        FirstPersonAnims firstPerson = CharacterActions.animationsFor(player).firstPerson();
        if (firstPerson.enabled() && isFirstPerson(player)) {
            String fpName = firstPerson.resolve(animationName, true);
            if (fpName != null && AnimationAvailability.existsFor(player, fpName)) {
                return RawAnimation.begin().thenPlayAndHold(fpName);
            }
        }

        if (!AnimationAvailability.existsFor(player, animationName)) {
            return null;
        }
        return RawAnimation.begin().thenPlayAndHold(animationName);
    }

    /** 这一帧是不是第一人称视角。 */
    public static boolean isFirstPerson(Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        return player == minecraft.player
                && minecraft.options != null
                && minecraft.options.getCameraType().isFirstPerson();
    }

    /** 当前玩家（本地）的第一人称配置；非本地或没开就返回 {@link FirstPersonAnims#DISABLED}。 */
    public static FirstPersonAnims firstPersonFor(Player player) {
        if (!isFirstPerson(player)) {
            return FirstPersonAnims.DISABLED;
        }
        return CharacterActions.animationsFor(player).firstPerson();
    }

    /**
     * 常态运动状态机。
     *
     * <p>选出来的动画同样要先确认存在；缺失时退到 idle，idle 也没有就返回 {@code null}
     * （保持当前动画），避免「冲刺结束/上岸瞬间」这类切换把模型打成原始姿态。
     */
    @Nullable
    private static RawAnimation pickLocomotion(Player player, LocomotionAnims loco,
                                               boolean isMoving,
                                               double movedX, double movedY, double movedZ) {
        RawAnimation picked = pickLocomotionRaw(player, loco, isMoving, movedX, movedY, movedZ);
        return existingOrIdle(player, loco, picked);
    }

    private static RawAnimation pickLocomotionRaw(Player player, LocomotionAnims loco,
                                                  boolean isMoving,
                                                  double movedX, double movedY, double movedZ) {
        if (player.isSleeping()) {
            return loco.sleep();
        }

        if (player.onClimbable()) {
            return loco.climb();
        }

        if (player.isInWater()) {
            if (player.isSwimming()) {
                return loco.swim();
            }

            if (isMoving) {
                // 用「移动方向 vs 视线方向」的点积区分前进/后退
                Vec3 look = player.getLookAngle();
                double dotProduct = movedX * look.x + movedZ * look.z;

                return dotProduct < -0.01 ? loco.waterWalkBack() : loco.waterWalk();
            }

            return loco.waterIdle();
        }

        if (!player.onGround()) {
            if (movedY > 0.01) {
                return loco.jump();
            }
            return loco.jumpDown();
        }

        if (isMoving) {
            Vec3 look = player.getLookAngle();
            double dotProduct = movedX * look.x + movedZ * look.z;

            if (player.isCrouching()) {
                return loco.crouchWalk();
            }
            if (player.isSprinting()) {
                return loco.run();
            }
            if (dotProduct < -0.01) {
                return loco.walkBack();
            }

            return loco.walk();
        }

        if (player.isCrouching()) {
            return loco.crouch();
        }

        return loco.idle();
    }

    /** 目标动画不存在时退回 idle；idle 也不存在就返回 {@code null}。 */
    @Nullable
    private static RawAnimation existingOrIdle(Player player, LocomotionAnims loco, RawAnimation wanted) {
        String name = targetName(wanted);
        if (name != null && AnimationAvailability.existsFor(player, name)) {
            return wanted;
        }

        String idleName = targetName(loco.idle());
        if (idleName != null && AnimationAvailability.existsFor(player, idleName)) {
            return loco.idle();
        }

        return null;
    }

    // ==================== 工具 ====================

    /** RawAnimation 第一段的动画名。 */
    @Nullable
    private static String targetName(@Nullable RawAnimation raw) {
        if (raw == null || raw.getAnimationStages().isEmpty()) {
            return null;
        }
        return raw.getAnimationStages().getFirst().animationName();
    }

    /** 当前控制器真正在播的动画名（GeckoLib 5：从 AnimationPoint 反查）。 */
    private static String currentAnimationName(AnimationController<?> controller) {
        AnimationPoint point = controller.getCurrentAnimationPoint();
        return point == null || point.animation() == null ? "" : point.animation().name();
    }
}
