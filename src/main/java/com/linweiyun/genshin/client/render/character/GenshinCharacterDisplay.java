package com.linweiyun.genshin.client.render.character;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.linweiyun.genshin.core.system.combat.action.ClientActionStateMachine;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterRenderData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.Map;

/**
 * 角色渲染 Display（GeckoLib GeoAnimatable）。
 * <p>
 * 按玩家移动状态播放常态动画（idle/walk/run...），
 * 优先播放特殊动画（攻击/技能，来自 ClientActionStateMachine）。
 * <p>
 * 每个角色实例一个 Display，缓存于 CharacterRenderDispatcher。
 */
public final class GenshinCharacterDisplay implements GeoAnimatable {

    /** 内置默认动画名（behavior 未配置时的兜底） */
    private static final Map<String, String> DEFAULT_ANIMS = Map.ofEntries(
            Map.entry("idle", "idle"),
            Map.entry("walk", "walk"),
            Map.entry("run", "run"),
            Map.entry("walk_back", "walk_back"),
            Map.entry("crouch", "crouch"),
            Map.entry("crouch_walk", "crouch_walk"),
            Map.entry("sleep", "sleep"),
            Map.entry("climb", "climb"),
            Map.entry("water", "water"),
            Map.entry("water_walk", "water_walk"),
            Map.entry("water_walk_back", "water_walk_back"),
            Map.entry("swim", "swim"),
            Map.entry("jump", "jump"),
            Map.entry("jump_down", "jump_down"),
            Map.entry("air_idle", "idle"),
            Map.entry("air_move", "walk"),
            Map.entry("air_sprint", "run")
    );

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final CharacterRenderData renderData;
    public Player playerEntity;

    public GenshinCharacterDisplay(CharacterRenderData renderData) {
        this.renderData = renderData;
    }

    public void setPlayerEntity(Player player) {
        this.playerEntity = player;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("movement", 5, state -> {
            if (this.playerEntity == null) return PlayState.STOP;

            String specialAnim = resolveSpecialAnim(this.playerEntity);
            if (specialAnim != null) {
                state.controller().setTransitionTicks(0);
                return state.setAndContinue(RawAnimation.begin().thenPlay(specialAnim));
            }

            state.controller().setTransitionTicks(5);
            String animName = resolveMovementAnim(this.playerEntity);
            return state.setAndContinue(RawAnimation.begin().thenLoop(animName));
        }));
    }

    /** 当前特殊动画名（优先于常态动画） */
    private static String resolveSpecialAnim(Player player) {
        String targetAnim;
        if (player.level().isClientSide() && player == Minecraft.getInstance().player) {
            targetAnim = ClientActionStateMachine.currentAnimation();
        } else {
            // 其他玩家：从暂存字段读取（后续通过 SyncAnimationMessage 设置）
            targetAnim = null;
        }
        if (targetAnim != null && !targetAnim.isEmpty() && !"default".equals(targetAnim)) {
            return targetAnim;
        }
        return null;
    }

    /** 根据玩家移动状态解析常态动画名 */
    private String resolveMovementAnim(Player player) {
        String state = resolveState(player);
        Map<String, String> mapping = renderData.animMapping();
        if (mapping != null) {
            String configured = mapping.get(state);
            if (configured != null && !configured.isEmpty()) return configured;
        }
        return DEFAULT_ANIMS.getOrDefault(state, "idle");
    }

    private static String resolveState(Player player) {
        boolean onGround = player.onGround();
        boolean crouching = player.isCrouching();
        boolean sprinting = player.isSprinting();

        double dx = player.getX() - player.xo;
        double dy = player.getY() - player.yo;
        double dz = player.getZ() - player.zo;

        boolean moving;
        if (player.level().isClientSide() && player instanceof LocalPlayer localPlayer) {
            moving = localPlayer.xxa != 0 || localPlayer.zza != 0;
        } else {
            moving = (dx * dx + dz * dz) > 0.00005;
        }

        Vec3 look = player.getLookAngle();
        double dot = dx * look.x + dz * look.z;
        boolean backward = dot < -0.01;

        if (player.isSleeping()) return "sleep";
        if (player.onClimbable()) return "climb";

        if (player.isInWater()) {
            if (player.isSwimming()) return "swim";
            if (moving) return backward ? "water_walk_back" : "water_walk";
            return "water";
        }


        if (!onGround) {
            if (dy > 0.01) return "jump";
            if (dy < -0.01) return "jump_down";
            if (moving) return sprinting ? "air_sprint" : "air_move";
            return "air_idle";
        }

        if (moving) {
            if (crouching) return "crouch_walk";
            if (sprinting) return "run";
            if (backward) return "walk_back";
            return "walk";
        }
        if (crouching) return "crouch";
        return "idle";
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}