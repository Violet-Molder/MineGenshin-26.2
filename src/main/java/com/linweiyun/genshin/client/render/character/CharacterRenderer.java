package com.linweiyun.genshin.client.render.character;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoObjectRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.world.entity.player.Player;

public class CharacterRenderer extends GeoObjectRenderer<GenshinReplacedPlayer, Player, GeoRenderState> {

    public CharacterRenderer(GeoModel<GenshinReplacedPlayer> model) {
        super(model);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<GeoRenderState> renderPassInfo) {
        renderPassInfo.poseStack().translate(0.5f, 0.0f, 0.5f);
    }
}