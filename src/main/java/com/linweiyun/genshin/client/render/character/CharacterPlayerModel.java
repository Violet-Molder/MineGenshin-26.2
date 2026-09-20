package com.linweiyun.genshin.client.render.character;

import com.linweiyun.genshin.client.render.geo.GenshinGeoModel;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterRenderData;
import com.mojang.logging.LogUtils;
import lombok.Getter;
import org.slf4j.Logger;

/**
 * 角色模型 —— 一个角色一份，路径全部来自 {@link CharacterRenderData}。
 *
 * <p>三个资源方法的实现、路径规则链、以及「先查本 MOD 自己的缓存」都在
 * {@link GenshinGeoModel} 里，这里只负责把渲染数据和角色 id 递进去。
 *
 * <p>布局：{@code assets/minegenshin/character/<角色id>/} 下的
 * {@code <基名>.geo.json} / {@code <基名>.animation.json} / {@code <基名>.png}。
 */
public class CharacterPlayerModel extends GenshinGeoModel<GenshinReplacedPlayer> {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Getter
    private CharacterRenderData renderData;

    public CharacterPlayerModel() {
        super();
    }

    /** 切换角色：换 id 的同时按新角色重算三个默认路径。 */
    public void updateRenderData(CharacterRenderData data) {
        if (data == null) {
            return;
        }
        this.renderData = data;

        // 用 id 挂上，让默认路径规则能把这个模型认成「哪个角色的」
        setCharacterId(data.id());
        // 再用角色数据里声明的文件名覆盖（允许和 id 不同名）
        setPaths(data.modelIdentifier(), data.textureIdentifier(), data.animationIdentifier());
        // 额外动画文件（第一人称、动作包……）
        setAnimationFallbackPaths(data.extraAnimationPaths());

        LOGGER.info("[CharacterPlayerModel] 角色 '{}' 路径: model={}, texture={}, animation={}, 额外动画={}",
                data.id(), defaultModelResource(), defaultTextureResource(), defaultAnimationResource(),
                data.extraAnimationPaths());
    }
}
