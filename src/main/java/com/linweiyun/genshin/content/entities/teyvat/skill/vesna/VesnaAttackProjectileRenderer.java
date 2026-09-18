package com.linweiyun.genshin.content.entities.teyvat.skill.vesna;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ExperienceOrbRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * 薇斯娜攻击投射物渲染器 —— 渲染成绿色经验球样式。
 */
public class VesnaAttackProjectileRenderer extends EntityRenderer<VesnaAttackProjectile, ExperienceOrbRenderState> {

    /** 经验球纹理（使用原版经验球贴图） */
    private static final Identifier ORB_TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/experience/experience_orb.png");

    /** 渲染类型：半透明、可剔除、可被物品目标 */
    private static final RenderType RENDER_TYPE =
            RenderTypes.entityTranslucentCullItemTarget(ORB_TEXTURE);

    /** 绿色经验球颜色（RGB: 0x00FF00） */
    private static final int GREEN_COLOR = 0x00FF00;

    public VesnaAttackProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.15F;
        this.shadowStrength = 0.75F;
    }

    /**
     * 自定义渲染状态，携带颜色信息。
     */
    private static class ProjectileRenderState extends ExperienceOrbRenderState {
        int orbColor = GREEN_COLOR;
    }

    @Override
    protected int getBlockLightLevel(VesnaAttackProjectile entity, BlockPos blockPos) {
        // 经验球自带发光效果，+7亮度
        return Mth.clamp(super.getBlockLightLevel(entity, blockPos) + 7, 0, 15);
    }

    @Override
    public void submit(ExperienceOrbRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();

        // 计算纹理UV坐标
        int icon = state.icon;
        float u0 = (icon % 4 * 16) / 64.0F;
        float u1 = u0 + 16.0F / 64.0F;
        float v0 = (icon / 4 * 16) / 64.0F;
        float v1 = v0 + 16.0F / 64.0F;

        // 获取颜色
        int color = ((ProjectileRenderState) state).orbColor;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // 变换矩阵：上移、面向相机、缩放
        poseStack.translate(0.0F, 0.1F, 0.0F);
        poseStack.mulPose(camera.orientation);
        poseStack.scale(0.3F, 0.3F, 0.3F);

        // 提交自定义几何体（四边形面片）
        collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            vertex(buffer, pose, -0.5F, -0.25F, r, g, b, u0, v1, state.lightCoords);
            vertex(buffer, pose, 0.5F, -0.25F, r, g, b, u1, v1, state.lightCoords);
            vertex(buffer, pose, 0.5F, 0.75F, r, g, b, u1, v0, state.lightCoords);
            vertex(buffer, pose, -0.5F, 0.75F, r, g, b, u0, v0, state.lightCoords);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    /**
     * 添加顶点到缓冲区。
     */
    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose,
                               float x, float y, int r, int g, int b,
                               float u, float v, int light) {
        buffer.addVertex(pose, x, y, 0.0F)
                .setColor(r, g, b, 128)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ExperienceOrbRenderState createRenderState() {
        return new ProjectileRenderState();
    }

    @Override
    public void extractRenderState(VesnaAttackProjectile entity, ExperienceOrbRenderState state,
                                   float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        // 设置固定绿色
        ((ProjectileRenderState) state).orbColor = GREEN_COLOR;
    }
}