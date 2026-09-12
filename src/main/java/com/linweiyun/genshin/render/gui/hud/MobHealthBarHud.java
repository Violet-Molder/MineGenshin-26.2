package com.linweiyun.genshin.render.gui.hud;

import com.linweiyun.genshin.content.entities.ai.ICombatTimer;
import com.linweiyun.genshin.content.entities.teyvat.boss.ITeyvatBoss;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(value = Dist.CLIENT)
public class MobHealthBarHud {

    private static final Identifier HP_BAR_BG_TEXTURE =
            Identifier.fromNamespaceAndPath("minegenshin", "textures/gui/short_character_hp_green.png");
    private static final Identifier HP_BAR_FILL_TEXTURE =
            Identifier.fromNamespaceAndPath("minegenshin", "textures/gui/short_character_hp_bar_white.png");

    private static final float BAR_WIDTH = 1.0f;
    private static final float BAR_HEIGHT = 0.08f;
    private static final float Y_OFFSET = 0.5f;
    private static final double MAX_DISTANCE = 32.0;

    private static final float FILL_Z_OFFSET = -0.001f;

    // 血条颜色（暂定红色）
    private static final float[] COLOR_FILL = { 1.00f, 0.20f, 0.20f };

    @SubscribeEvent
    public static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        PoseStack poseStack = event.getPoseStack();

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 camPos = mc.gameRenderer.mainCamera().position();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living instanceof Player) continue;
            if (living instanceof ITeyvatBoss) continue;
            if (!living.isAlive()) continue;

            // ---- 战斗状态过滤 ----
            if (!(living instanceof ICombatTimer timer)) continue;
            if (!timer.genshin$isInCombat()) continue;

            // ---- 距离过滤 ----
            Vec3 entPos = living.getPosition(partialTick);
            if (camPos.distanceTo(entPos) > MAX_DISTANCE) continue;

            // ---- 血量计算 ----
            float maxHealth = living.getMaxHealth();
            if (maxHealth <= 0) continue;
            float healthRatio = Math.max(0f, Math.min(1f, living.getHealth() / maxHealth));

            float r = COLOR_FILL[0], g = COLOR_FILL[1], b = COLOR_FILL[2];

            poseStack.pushPose();

            Vec3 relative = entPos.subtract(camPos);
            poseStack.translate(relative.x, relative.y + living.getBbHeight() + Y_OFFSET, relative.z);
            poseStack.mulPose(mc.gameRenderer.mainCamera().rotation());
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));

            // ---- 空槽背景 ----
            RenderType bgType = RenderTypes.entityTranslucent(HP_BAR_BG_TEXTURE);
            collector.submitCustomGeometry(poseStack, bgType, (pose, buffer) -> {
                Matrix4f matrix = pose.pose();
                drawTexturedQuad(buffer, matrix, 0.0f,
                        -BAR_WIDTH / 2, -BAR_HEIGHT / 2, BAR_WIDTH / 2, BAR_HEIGHT / 2,
                        0.0f, 0.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f);
            });

            // ---- 血条填充 ----
            float fillWidth = BAR_WIDTH * healthRatio;
            RenderType barType = RenderTypes.entityTranslucent(HP_BAR_FILL_TEXTURE);
            collector.submitCustomGeometry(poseStack, barType, (pose, buffer) -> {
                Matrix4f matrix = pose.pose();
                drawTexturedQuad(buffer, matrix, FILL_Z_OFFSET,
                        -BAR_WIDTH / 2, -BAR_HEIGHT / 2, -BAR_WIDTH / 2 + fillWidth, BAR_HEIGHT / 2,
                        0.0f, 0.0f, healthRatio, 1.0f,
                        r, g, b, 1.0f);
            });

            poseStack.popPose();
        }
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