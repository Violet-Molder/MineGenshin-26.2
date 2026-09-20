package com.linweiyun.genshin.client.render.character;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterRenderData;
import lombok.Getter;
import net.minecraft.resources.Identifier;


/**
 * GeckoLib GeoModel 实现，根据 CharacterRenderData 返回模型/纹理/动画路径。
 * <p>
 * 所有角色共用 default 模型和纹理，动画按角色 ID 加载各自文件。
 */
public class GenshinGeoModel extends GeoModel<GenshinCharacterDisplay> {

    @Getter
    private final CharacterRenderData renderData;
    private final Identifier modelId;
    private final Identifier textureId;
    private final Identifier animationId;

    public GenshinGeoModel(CharacterRenderData renderData) {
        this.renderData = renderData;
        this.modelId = renderData.modelIdentifier(Minegenshin.MOD_ID);
        this.textureId = renderData.textureIdentifier(Minegenshin.MOD_ID);
        this.animationId = renderData.animationIdentifier(Minegenshin.MOD_ID);
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return modelId;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return textureId;
    }

    @Override
    public Identifier getAnimationResource(GenshinCharacterDisplay display) {
        return animationId;
    }

}