package com.linweiyun.genshin.client.render.character;

import com.geckolib.renderer.GeoReplacedEntityRenderer;
import com.linweiyun.genshin.config.character.CharacterSystemConfig;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterRenderData;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterRenderRepository;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public class CharacterPlayerRenderer extends GeoReplacedEntityRenderer<GenshinReplacedPlayer, Player, AvatarRenderState> {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final CharacterPlayerModel characterModel;
    private final AvatarRenderer<AbstractClientPlayer> fallbackDefault;
    private final AvatarRenderer<AbstractClientPlayer> fallbackSlim;
    private String currentCharId;
    private boolean isGenshinMode;
    private int tickCounter;
    private boolean lastGenshinMode;

    public CharacterPlayerRenderer(EntityRendererProvider.Context context) {
        super(context, new CharacterPlayerModel(), new GenshinReplacedPlayer());
        this.characterModel = (CharacterPlayerModel) this.model;
        this.fallbackDefault = new AvatarRenderer<>(context, false);
        this.fallbackSlim = new AvatarRenderer<>(context, true);
        this.currentCharId = null;
        LOGGER.info("[CharacterPlayerRenderer] 构造完成, model={}, animatable={}",
                this.model.getClass().getSimpleName(), this.animatable.getClass().getSimpleName());
    }

    @Override
    public void extractRenderState(Player entity, AvatarRenderState renderState, float partialTick) {
        AbstractClientPlayer clientPlayer = (AbstractClientPlayer) entity;
        this.fallbackDefault.extractRenderState(clientPlayer, renderState, partialTick);

        boolean wasGenshin = this.isGenshinMode;
        this.isGenshinMode = AttachmentHelper.isGenshinMode(clientPlayer);

        tickCounter++;

        if (!this.isGenshinMode) {
            if (wasGenshin || tickCounter == 1) {
                LOGGER.debug("[CharacterPlayerRenderer] extractRenderState: isGenshinMode=false, "
                        + "玩家={}, tick={}", entity.getName().getString(), tickCounter);
            }
            this.animatable.setPlayerEntity(null);
            return;
        }

        if (!wasGenshin) {
            LOGGER.info("[CharacterPlayerRenderer] extractRenderState: 检测到 Genshin 模式, 玩家={}",
                    entity.getName().getString());
        }

        String charId = AttachmentHelper.getActiveCharacterId(clientPlayer);

        // 角色没有专属模型时，走原版渲染：模型替换和动画全部关掉
        if (charId == null || !CharacterSystemConfig.customModel(charId)) {
            this.animatable.setPlayerEntity(null);

            // 角色换了（或者这次才是第一次判定）→ 把模型和动画路径清掉，避免残留上一个角色的资源
            if (this.currentCharId != null) {
                this.currentCharId = null;
            }
            return;
        }

        if (!charId.equals(this.currentCharId)) {
            LOGGER.info("[CharacterPlayerRenderer] 角色切换: '{}' → '{}'", this.currentCharId, charId);
            CharacterRenderData data = CharacterRenderRepository.get(charId);
            if (data != null) {
                this.characterModel.updateRenderData(data);
                this.currentCharId = charId;
                this.animatable.setCharacterId(charId);
                LOGGER.info("[CharacterPlayerRenderer] 模型更新完成: model={}, texture={}, anim={}",
                        data.modelPath(), data.texturePath, data.animationPath);
            } else {
                LOGGER.error("[CharacterPlayerRenderer] CharacterRenderRepository 中找不到 '{}', "
                        + "已注册的角色: size={}", charId, CharacterRenderRepository.size());
                return;
            }
        }

        this.animatable.setPlayerEntity(entity);
        super.extractRenderState(entity, renderState, partialTick);
    }

    @Override
    public void submit(AvatarRenderState renderState, PoseStack poseStack,
                       SubmitNodeCollector renderTasks, CameraRenderState cameraState) {
        if (this.lastGenshinMode != this.isGenshinMode) {
            this.lastGenshinMode = this.isGenshinMode;
            LOGGER.info("[CharacterPlayerRenderer] submit: isGenshinMode={}, currentCharId={}",
                    this.isGenshinMode, this.currentCharId);
        }

        if (this.isGenshinMode) {
            super.submit(renderState, poseStack, renderTasks, cameraState);
        } else {
            AvatarRenderer<AbstractClientPlayer> fallback = isSlimSkin(renderState)
                    ? this.fallbackSlim : this.fallbackDefault;
            fallback.submit(renderState, poseStack, renderTasks, cameraState);
        }
    }

    private static boolean isSlimSkin(AvatarRenderState state) {
        if (state.skin == null) return false;
        try {
            Object model = state.skin.model();
            return model != null && model.toString().contains("SLIM");
        } catch (Exception e) {
            return false;
        }
    }
}