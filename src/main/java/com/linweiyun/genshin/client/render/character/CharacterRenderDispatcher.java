package com.linweiyun.genshin.client.render.character;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterRenderData;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterRenderRepository;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = Minegenshin.MOD_ID, value = Dist.CLIENT)
public final class CharacterRenderDispatcher {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<String, CharacterPlayerModel> MODELS = new HashMap<>();
    private static final Map<String, CharacterRenderer> RENDERERS = new HashMap<>();
    private static final Map<Player, GenshinReplacedPlayer> ANIMATABLES = new WeakHashMap<>();

    private static int renderCount;

    private CharacterRenderDispatcher() {}

    public static boolean handleSubmit(AvatarRenderState state, PoseStack poseStack,
                                        SubmitNodeCollector submitNodeCollector,
                                        CameraRenderState cameraState) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;

        Player player = mc.level.getEntity(state.id) instanceof Player p ? p : null;
        if (player == null) return false;

        if (!AttachmentHelper.isGenshinMode(player)) return false;

        String charId = AttachmentHelper.getActiveCharacterId(player);
        if (charId == null || charId.isEmpty()) return false;

        CharacterRenderData data = CharacterRenderRepository.get(charId);
        if (data == null) {
            if (renderCount <= 3 || renderCount % 200 == 0) {
            }
            return false;
        }

        renderCount++;
        if (renderCount <= 5 || renderCount % 200 == 0) {
        }

        try {
            doRender(poseStack, submitNodeCollector, cameraState, player, charId, data);
        } catch (Exception e) {
        }

        return true;
    }

    private static void doRender(PoseStack poseStack, SubmitNodeCollector bufferSource,
                                  CameraRenderState cameraState, Player player,
                                  String charId, CharacterRenderData data) {
        CharacterPlayerModel model = MODELS.computeIfAbsent(charId, k -> {
            CharacterPlayerModel m = new CharacterPlayerModel();
            m.updateRenderData(data);
            return m;
        });

        CharacterRenderer renderer = RENDERERS.computeIfAbsent(charId, k ->
                new CharacterRenderer(model)
        );

        GenshinReplacedPlayer animatable = ANIMATABLES.computeIfAbsent(player, k -> new GenshinReplacedPlayer());
        animatable.playerEntity = player;
        animatable.renderData = data;

        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);

        poseStack.pushPose();

        float bodyScale = data.bodyScale();
        if (bodyScale != 1.0f) {
            poseStack.scale(bodyScale, bodyScale, bodyScale);
        }

        float bodyYaw = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        poseStack.mulPose(Axis.YP.rotationDegrees(180));

        renderer.performRenderPass(animatable, player, poseStack, bufferSource, cameraState,
                15728880, partialTick);

        poseStack.popPose();
    }
}