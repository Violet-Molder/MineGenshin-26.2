package com.linweiyun.genshin.render.gui.hud;

import com.linweiyun.genshin.content.entities.teyvat.ITeyvatBoss;
import com.linweiyun.genshin.content.entities.teyvat.NonTeyvatEntity;
import com.linweiyun.genshin.content.entities.teyvat.TeyvatFriendly;
import com.linweiyun.genshin.content.entities.teyvat.TeyvatHostile;
import com.linweiyun.genshin.content.entities.teyvat.TeyvatLiving;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(value = Dist.CLIENT)
public class MobHealthBarHud {

    private static final Identifier HP_BAR_BG_TEXTURE =
            Identifier.fromNamespaceAndPath("minegenshin", "textures/gui/short_character_hp_green.png");
    private static final Identifier HP_BAR_FILL_TEXTURE =
            Identifier.fromNamespaceAndPath("minegenshin", "textures/gui/short_character_hp_bar_white.png");

    private static final float BAR_WIDTH = 1.0f;
    private static final float BAR_HEIGHT = 0.08f;
    private static final float Y_OFFSET = 0.5f;
    private static final double MAX_DISTANCE = 24.0;

    private static final float FILL_Z_OFFSET = -0.001f;
    private static final float TRAIL_Z_OFFSET = -0.0005f;
    private static final float TRAIL_SPEED = 0.005f;

    private static final float LEVEL_TEXT_Y = 0.3f;
    private static final float LEVEL_TEXT_SCALE = 0.025f;
    private static final float LEVEL_TEXT_Y_BIG = 0.225f;
    private static final float LEVEL_TEXT_SCALE_BIG = 0.05f;

    private static final float RECENT_HIT_THRESHOLD = 0.8f;

    private static final float[] COLOR_HOSTILE = { 1.00f, 0.20f, 0.20f };
    private static final float[] COLOR_FRIENDLY = { 0.30f, 0.90f, 0.30f };
    private static final float[] COLOR_TRAIL = { 0.70f, 0.50f, 0.10f };

    @SubscribeEvent
    public static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        PoseStack poseStack = event.getPoseStack();

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 camPos = mc.gameRenderer.mainCamera().position();

        Set<Integer> activeIds = new HashSet<>();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living instanceof Player) continue;
            if (living instanceof ITeyvatBoss) continue;
            if (!living.isAlive()) continue;
            if (!(living instanceof TeyvatLiving teyvat)) continue;
            if (!teyvat.isInCombat()) continue;

            float[] fillColor = resolveColor(living);
            if (fillColor == null) continue;

            Vec3 entPos = living.getPosition(partialTick);
            double distance = camPos.distanceTo(entPos);

            boolean recentlyHit = teyvat.getCombatTicks() > teyvat.getCombatDuration() * RECENT_HIT_THRESHOLD;
            boolean showBar = distance <= MAX_DISTANCE || recentlyHit;

            float maxHealth = living.getMaxHealth();
            if (maxHealth <= 0) continue;
            float healthRatio = Math.max(0f, Math.min(1f, living.getHealth() / maxHealth));

            float r = fillColor[0], g = fillColor[1], b = fillColor[2];

            Vec3 relative = entPos.subtract(camPos);
            double baseY = relative.y + living.getBbHeight() + Y_OFFSET;

            poseStack.pushPose();
            poseStack.translate(relative.x, baseY, relative.z);
            poseStack.mulPose(mc.gameRenderer.mainCamera().rotation());
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));

            int level = teyvat.getMonsterLevel();

            if (showBar) {
                activeIds.add(living.getId());

                float trailRatio = HealthBarTrail.get(living.getId(), healthRatio);
                HealthBarTrail.update(living.getId(), healthRatio, TRAIL_SPEED);

                // ============ 空槽背景 ============
                RenderType bgType = RenderTypes.entityTranslucent(HP_BAR_BG_TEXTURE);
                collector.submitCustomGeometry(poseStack, bgType, (pose, buffer) -> {
                    Matrix4f matrix = pose.pose();
                    drawTexturedQuad(buffer, matrix, 0.0f,
                            -BAR_WIDTH / 2, -BAR_HEIGHT / 2, BAR_WIDTH / 2, BAR_HEIGHT / 2,
                            0.0f, 0.0f, 1.0f, 1.0f,
                            1.0f, 1.0f, 1.0f, 1.0f);
                });

                // ============ 拖尾（反序 UV，左三角保留、右端收缩） ============
                if (trailRatio > healthRatio) {
                    float trailWidth = BAR_WIDTH * trailRatio;
                    RenderType trailType = RenderTypes.entityTranslucent(HP_BAR_FILL_TEXTURE);
                    collector.submitCustomGeometry(poseStack, trailType, (pose, buffer) -> {
                        Matrix4f matrix = pose.pose();
                        drawTexturedQuad(buffer, matrix, TRAIL_Z_OFFSET,
                                BAR_WIDTH / 2 - trailWidth, -BAR_HEIGHT / 2,
                                BAR_WIDTH / 2, BAR_HEIGHT / 2,
                                trailRatio, 0.0f, 0.0f, 1.0f,
                                COLOR_TRAIL[0], COLOR_TRAIL[1], COLOR_TRAIL[2], 1.0f);
                    });
                }

                // ============ 血条填充（反序 UV） ============
                float fillWidth = BAR_WIDTH * healthRatio;
                RenderType barType = RenderTypes.entityTranslucent(HP_BAR_FILL_TEXTURE);
                collector.submitCustomGeometry(poseStack, barType, (pose, buffer) -> {
                    Matrix4f matrix = pose.pose();
                    drawTexturedQuad(buffer, matrix, FILL_Z_OFFSET,
                            BAR_WIDTH / 2 - fillWidth, -BAR_HEIGHT / 2,
                            BAR_WIDTH / 2, BAR_HEIGHT / 2,
                            healthRatio, 0.0f, 0.0f, 1.0f,
                            r, g, b, 1.0f);
                });

                // ============ 等级文字 ============
                if (level > 0) {
                    submitLevelText(poseStack, collector, mc, level, LEVEL_TEXT_Y, LEVEL_TEXT_SCALE);
                }
            } else {
                if (level > 0) {
                    submitLevelText(poseStack, collector, mc, level, LEVEL_TEXT_Y_BIG, LEVEL_TEXT_SCALE_BIG);
                }
            }

            poseStack.popPose();
        }

        HealthBarTrail.cleanup(activeIds);
    }

    private static float[] resolveColor(LivingEntity living) {
        if (living instanceof TeyvatFriendly) return COLOR_FRIENDLY;
        if (living instanceof TeyvatHostile) return COLOR_HOSTILE;
        if (living instanceof NonTeyvatEntity) {
            MobCategory cat = living.getType().getCategory();
            if (cat == MobCategory.MISC) return null;
            return cat.isFriendly() ? COLOR_FRIENDLY : COLOR_HOSTILE;
        }
        return null;
    }

    private static void submitLevelText(PoseStack poseStack, SubmitNodeCollector collector,
                                        Minecraft mc, int level, float yOffset, float scale) {
        String text = "Lv." + level;
        int width = mc.font.width(text);

        poseStack.pushPose();
        poseStack.translate(0.0f, yOffset, 0.0f);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        poseStack.scale(scale, -scale, scale);

        collector.submitText(
                poseStack,
                -width / 2.0f,
                0.0f,
                Component.literal(text).getVisualOrderText(),
                true,
                Font.DisplayMode.SEE_THROUGH,
                0xF000F0,
                0xFFFFFFFF,
                0,
                0
        );

        poseStack.popPose();
    }

    private static void drawTexturedQuad(VertexConsumer consumer, Matrix4f matrix, float z,
                                         float x1, float y1, float x2, float y2,
                                         float u1, float v1, float u2, float v2,
                                         float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1, z).setColor(r, g, b, a).setUv(u1, v2)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x2, y1, z).setColor(r, g, b, a).setUv(u2, v2)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x2, y2, z).setColor(r, g, b, a).setUv(u2, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x1, y2, z).setColor(r, g, b, a).setUv(u1, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 0, 1);
    }
}