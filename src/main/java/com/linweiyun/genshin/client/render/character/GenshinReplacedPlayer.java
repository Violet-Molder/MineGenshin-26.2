package com.linweiyun.genshin.client.render.character;

import com.geckolib.animatable.GeoReplacedEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.linweiyun.genshin.core.system.combat.action.ClientActionStateMachine;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterRenderData;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.Map;

public class GenshinReplacedPlayer implements GeoReplacedEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

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
            Map.entry("air_sprint", "run"),
            Map.entry("elytra", "elytra"),
            Map.entry("fly", "fly")
    );

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Player playerEntity;
    public CharacterRenderData renderData;

    private boolean registeredControllers;
    private boolean loggedNullPlayer;

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        registeredControllers = true;
        controllers.add(new AnimationController<>("movement", 5, state -> {
            if (this.playerEntity == null) {
                if (!loggedNullPlayer) {
                    loggedNullPlayer = true;
                    LOGGER.error("[GenshinReplacedPlayer] playerEntity 为 null! renderData={}",
                            this.renderData);
                }
                return PlayState.STOP;
            }
            loggedNullPlayer = false;

            String specialAnim = resolveSpecialAnim(this.playerEntity);
            if (specialAnim != null) {
                state.controller().setTransitionTicks(0);
                return state.setAndContinue(RawAnimation.begin().thenPlay(specialAnim));
            }

            state.controller().setTransitionTicks(5);
            String animName = resolveMovementAnim(this.playerEntity);
            return state.setAndContinue(RawAnimation.begin().thenLoop(animName));
        }));
        LOGGER.info("[GenshinReplacedPlayer] AnimationController 已注册");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private static String resolveSpecialAnim(Player player) {
        if (player.level().isClientSide() && player == Minecraft.getInstance().player) {
            String targetAnim = ClientActionStateMachine.currentAnimation();
            if (targetAnim != null && !targetAnim.isEmpty() && !"default".equals(targetAnim)) {
                return targetAnim;
            }
        }
        return null;
    }

    private String resolveMovementAnim(Player player) {
        String state = resolveState(player);
        Map<String, String> mapping = renderData != null ? renderData.animMapping() : null;
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

        double dy = player.getY() - player.yo;
        if (!onGround) {
            if (player.getAbilities().flying) {
                return sprinting ? "fly" : "elytra";
            }
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
}