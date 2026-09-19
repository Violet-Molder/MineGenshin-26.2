package com.linweiyun.genshin.content.entities.teyvat.skill.vesna;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * 灵剑实体渲染器 —— 不画任何几何体，粒子由实体的 clientTick() 生成。
 * <p>作用仅是让 MC 客户端认为该 EntityType 有渲染器，避免被跳过。
 */
public class VesnaSpiritSwordRenderer
        extends EntityRenderer<VesnaSpiritSwordEntity, EntityRenderState> {

    public VesnaSpiritSwordRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}