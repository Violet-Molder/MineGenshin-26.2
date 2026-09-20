package com.linweiyun.genshin.client.render.character;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterRenderData;
import com.mojang.logging.LogUtils;
import lombok.Getter;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

public class CharacterPlayerModel extends GeoModel<GenshinReplacedPlayer> {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Getter
    private CharacterRenderData renderData;
    private Identifier modelId;
    private Identifier textureId;
    private Identifier animationId;

    public CharacterPlayerModel() {
    }

    public void updateRenderData(CharacterRenderData data) {
        this.renderData = data;
        this.modelId = data.modelIdentifier(Minegenshin.MOD_ID);
        this.textureId = data.textureIdentifier(Minegenshin.MOD_ID);
        this.animationId = data.animationIdentifier(Minegenshin.MOD_ID);
        LOGGER.info("[CharacterPlayerModel] 更新路径: model={}, texture={}, animation={}",
                this.modelId, this.textureId, this.animationId);
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
    public Identifier getAnimationResource(GenshinReplacedPlayer animatable) {
        return animationId;
    }

}