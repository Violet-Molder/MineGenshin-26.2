package com.linweiyun.genshin.client.gui.hud;// package com.linweiyun.genshin.client.gui.hud;
//
// import com.linweiyun.genshin.Minegenshin;
// import com.linweiyun.genshin.core.combat.ElementalHurtHelperGIM;
// import com.linweiyun.genshin.content.entities.attachments.AttachmentRegistration;
// import
// com.linweiyun.genshin.content.entities.attachments.attachment.PlayerCharacterSelectionAttachment;
// import com.linweiyun.genshin.content.entities.attachments.attachment.PlayerGenshinModeAttachment;
// import com.linweiyun.genshin.content.items.character.data.PlayerCharacterData;
// import com.linweiyun.genshin.content.items.character.player_character.PlayerCharacter;
// import com.mojang.blaze3d.systems.RenderSystem;
// import com.mojang.blaze3d.vertex.*;
// import net.minecraft.client.Minecraft;
// import net.minecraft.client.gui.Font;
// import net.minecraft.client.gui.GuiGraphics;
// import net.minecraft.client.renderer.MultiBufferSource;
// import net.minecraft.client.renderer.texture.TextureManager;
// import net.minecraft.core.registries.BuiltInRegistries;
// import net.minecraft.network.chat.Component;
// import net.minecraft.resources.ResourceLocation;
// import net.minecraft.world.entity.player.Player;
// import net.minecraft.world.item.ItemStack;
// import net.neoforged.api.distmarker.Dist;
// import net.neoforged.bus.api.SubscribeEvent;
// import net.neoforged.fml.common.EventBusSubscriber;
// import net.neoforged.neoforge.client.event.RenderGuiEvent;
// @EventBusSubscriber(modid = Minegenshin.MOD_ID, value = Dist.CLIENT)
// public class CharacterPartyRenderHandler {
//    private static final ResourceLocation CUSTOM_ICON =
// ResourceLocation.fromNamespaceAndPath(Minegenshin.MOD_ID, "textures/skill/test1.png");
//    private static final boolean[] isVisible = new boolean[4];       // 当前是否可见
//    private static final boolean[] wasVisible = new boolean[4];      // 上一帧是否可见
//    private static final int ANIMATION_DURATION = 10;               // 动画持续时间（游戏刻数）
//    private static final int[] animationProgress = new int[4];       // 每个角色的动画帧进度
//    private static final float[] currentScales = new float[4];       // 存储每个角色当前的缩放比例
//    // ========== 新增：存储每个角色的「当前插值进度」（0~1，用于lerp计算） ==========
//    private static final float[] currentAnimationProgress = new float[4];
//
//    @SubscribeEvent
//    public static void onRenderHud(RenderGuiEvent.Post event) {
//        Minecraft minecraft = Minecraft.getInstance();
//        Player player = minecraft.player;
//        PlayerGenshinModeAttachment genshinModeAttachment =
// player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT);
//
//        int screenWidth = event.getGuiGraphics().guiWidth();
//        var characterParty =
// minecraft.player.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
//        PlayerCharacterSelectionAttachment playerCharacterSelectionAttachment =
// minecraft.player.getData(AttachmentRegistration.CHARACTER_SELECTION_ATTACHMENT);
//
//        int hudRightX = screenWidth - 100; // HUD右侧起始位置
//        int iconWidth = 30;
//        int iconHeight = 30;
//        int backgroundWidth = 30;
//        int backgroundHeight = 30;
//        int spacing = 5;
//        float switchAnimationSpeed = 0.05f; // 切换角色的动画速度
//        float expandAnimationSpeed = 0.1f; // 展开动画速度
//        float collapseAnimationSpeed = 0.1f; // 收缩动画速度
//        boolean isGenshinMode = genshinModeAttachment.isGenshinMode();
//
//        for (int i = 0; i < 4; i++) {
//            ItemStack stack = characterParty.getStackInSlot(i);
//            if (!stack.isEmpty() && stack.getItem() instanceof PlayerCharacter character) {
//                String characterID = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
//                ResourceLocation iconTexture =
// ResourceLocation.fromNamespaceAndPath(Minegenshin.MOD_ID, "textures/character_avatar/hud/" +
// characterID + ".png");
//
//                int iconX = hudRightX;
//                int iconY = 10 + i * (iconHeight + spacing);
//                int backgroundX = iconX - (backgroundWidth - iconWidth) / 2;
//                int backgroundY = iconY - (backgroundHeight - iconHeight) / 2;
//                boolean isSelected = playerCharacterSelectionAttachment.getCurrentCharacterSlot()
// - 1 == i;
//
//                // 动画逻辑：状态变化时重置所有进度
//                if (wasVisible[i] != isGenshinMode) {
//                    animationProgress[i] = 0; // 重置帧进度
//                    currentAnimationProgress[i] = 0.0f; // 重置插值进度（关键）
//                }
//                wasVisible[i] = isVisible[i];
//                isVisible[i] = isGenshinMode;
//
//                // 缩放动画：原有lerp逻辑保留
//                float targetScale = isSelected ? 0.8f : 1.0f;
//                float currentScale = lerp(getCurrentScale(i), targetScale, switchAnimationSpeed);
//                setCurrentScale(i, currentScale);
//
//                int backgroundColor = isSelected ? 0xFFAAAAAA : 0xFF555555;
//
//                if (isVisible[i]) {
//                    renderExpandAnimation(event, backgroundX, backgroundY, iconX, iconY,
// iconWidth, iconHeight, backgroundWidth, backgroundHeight, currentScale, backgroundColor,
// iconTexture, i, expandAnimationSpeed);
//                } else {
//                    renderCollapseAnimation(event, backgroundX, backgroundY, iconX, iconY,
// iconWidth, iconHeight, backgroundWidth, backgroundHeight, currentScale, backgroundColor,
// iconTexture, i, collapseAnimationSpeed);
//                }
//
//                // 更新动画帧进度（原有逻辑保留）
//                if (animationProgress[i] < ANIMATION_DURATION) {
//                    animationProgress[i]++;
//                }
//            }
//        }
//    }
//
//    private static void renderExpandAnimation(RenderGuiEvent.Post event, int backgroundX, int
// backgroundY, int iconX, int iconY, int iconWidth, int iconHeight, int backgroundWidth, int
// backgroundHeight, float currentScale, int backgroundColor, ResourceLocation iconTexture, int
// index, float expandAnimationSpeed) {
//        // 目标进度：展开动画最终要到1（完全显示）
//        float targetProgress = 1.0f;
//        // 用lerp计算当前进度（当前进度 → 目标进度，速度为expandAnimationSpeed）
//        currentAnimationProgress[index] = lerp(currentAnimationProgress[index], targetProgress,
// expandAnimationSpeed);
//        float progress = currentAnimationProgress[index];
//
//        int clipWidth = (int) (backgroundWidth * progress); // 裁剪宽度（随进度从0→backgroundWidth）
//        // 展开锚点为右侧，renderX = 背景右侧 - 裁剪宽度 → 从右侧开始往右展开
//        int renderX = backgroundX + (backgroundWidth - clipWidth);
//
//        PoseStack poseStack = event.getGuiGraphics().pose();
//        poseStack.pushPose();
//        poseStack.translate(renderX + clipWidth * (1 - currentScale) / 2, backgroundY +
// backgroundHeight * (1 - currentScale) / 2, 0);
//        poseStack.scale(currentScale, currentScale, 1);
//
//        // 渲染背景（仅渲染裁剪宽度部分，从右侧开始）
//        event.getGuiGraphics().fill(0, 0, clipWidth, backgroundHeight, backgroundColor);
//        poseStack.popPose();
//
//        poseStack.pushPose();
//        poseStack.translate(renderX + Math.min(clipWidth, iconWidth) * (1 - currentScale) / 2,
// iconY + iconHeight * (1 - currentScale) / 2, 0);
//        poseStack.scale(currentScale, currentScale, 1);
//
//        // 渲染图标（仅渲染裁剪宽度部分，从右侧开始往右显示）
//        event.getGuiGraphics().blit(iconTexture, 0, 0, 0, 0, Math.min(clipWidth, iconWidth),
// iconHeight, iconWidth, iconHeight);
//        poseStack.popPose();
//    }
//
//    // ========== 收缩动画：「右侧锚点，从左往右缩回」 ==========
//    private static void renderCollapseAnimation(RenderGuiEvent.Post event, int backgroundX, int
// backgroundY, int iconX, int iconY, int iconWidth, int iconHeight, int backgroundWidth, int
// backgroundHeight, float currentScale, int backgroundColor, ResourceLocation iconTexture, int
// index, float collapseAnimationSpeed) {
//        // 目标进度：收缩动画最终要到1（完全缩回）
//        float targetProgress = 1.0f;
//        currentAnimationProgress[index] = lerp(currentAnimationProgress[index], targetProgress,
// collapseAnimationSpeed);
//        float progress = currentAnimationProgress[index];
//        int clipWidth = (int) (backgroundWidth * (1 - progress));
//        // 收缩锚点为右侧，renderX 始终基于右侧锚点计算 → 从左往右缩回
//        int renderX = backgroundX + (backgroundWidth - clipWidth);
//
//        PoseStack poseStack = event.getGuiGraphics().pose();
//        poseStack.pushPose();
//        poseStack.translate(renderX + clipWidth * (1 - currentScale) / 2, backgroundY +
// backgroundHeight * (1 - currentScale) / 2, 0);
//        poseStack.scale(currentScale, currentScale, 1);
//
//        // 渲染背景（仅保留右侧未收缩的部分）
//        event.getGuiGraphics().fill(0, 0, clipWidth, backgroundHeight, backgroundColor);
//        poseStack.popPose();
//
//        poseStack.pushPose();
//        // 图标平移同步修正
//        poseStack.translate(renderX + Math.min(clipWidth, iconWidth) * (1 - currentScale) / 2,
// iconY + iconHeight * (1 - currentScale) / 2, 0);
//        poseStack.scale(currentScale, currentScale, 1);
//
//        // 渲染图标（仅保留右侧未收缩的部分，从左往右缩回）
//        event.getGuiGraphics().blit(iconTexture, 0, 0, 0, 0, Math.min(clipWidth, iconWidth),
// iconHeight, iconWidth, iconHeight);
//        poseStack.popPose();
//    }
//
//    private static float getCurrentScale(int index) {
//        return currentScales[index];
//    }
//
//    private static void setCurrentScale(int index, float scale) {
//        currentScales[index] = scale;
//    }
//
//    // 线性插值核心方法
//    private static float lerp(float start, float end, float t) {
//        return start + (end - start) * t;
//    }
//
//    @SubscribeEvent
//    public static void onRenderHudT1(RenderGuiEvent.Post event) {
//        Minecraft minecraft = Minecraft.getInstance();
//        if (minecraft.level == null || minecraft.screen != null) {
//            return;
//        }
//
//        PoseStack poseStack = event.getGuiGraphics().pose();
//        int screenWidth = event.getGuiGraphics().guiWidth();;
//        int screenHeight = event.getGuiGraphics().guiHeight();
//        Font font = minecraft.font;
//
//        int expBarY = screenHeight - 31;
//        int textY = expBarY - 12;
//        String text = "测试";
//        Component textComponent = Component.literal(text);
//        GuiGraphics guiGraphics = event.getGuiGraphics();
//        int textWidth = font.width(textComponent);
//        int textX = (screenWidth - textWidth) / 2;
//        MultiBufferSource bufferSource = event.getGuiGraphics().bufferSource();
//        font.drawInBatch(
//                textComponent,
//                (float) textX,
//                (float) textY,
//                0xFFFFFFFF,
//                false,
//                poseStack.last().pose(),
//                bufferSource,
//                Font.DisplayMode.NORMAL,
//                0,
//                1
//        );
//        int iconX = textX + textWidth + 10;
//        int iconY = textY - 50;
//        int iconWidth = 200;
//        int iconRenderWidth = 40;
//        int iconHeight = 150;
//        int iconRenderHeight = 30;
//        PlayerCharacterSelectionAttachment selectionAttachment =
// minecraft.player.getData(AttachmentRegistration.CHARACTER_SELECTION_ATTACHMENT);
//        ItemStack currentCharacterStack =
// selectionAttachment.getCurrentCharacter(minecraft.player);
//        PlayerCharacter currentCharacter = currentCharacterStack.getItem() instanceof
// PlayerCharacter character ? character : null;
//        PlayerCharacterData currentData =
// ElementalHurtHelperGIM.getCharacterData(currentCharacterStack);
//        float energyRatio = 0.0f;
//        if (currentData != null) {
//            double currentEnergy = currentData.getCurrentObtainingEnergy();
//            double maxEnergy = currentCharacter.getMaxObtainingEnergy();
//            energyRatio = (float) (currentEnergy / maxEnergy);
//        }
//        TextureManager textureManager = minecraft.getTextureManager();
//        textureManager.bindForSetup(CUSTOM_ICON);
//        RenderSystem.enableBlend();
//
//        guiGraphics.blit(
//                CUSTOM_ICON,
//                0, 0,
//                iconRenderWidth, iconRenderHeight,
//                0,0,
//                iconWidth, iconHeight,
//                iconWidth, iconHeight
//        );
//        if (energyRatio > 0) {
//            int fillHeight = (int) (iconRenderHeight * energyRatio);
//            guiGraphics.fill(
//                    0,
//                    iconRenderHeight - fillHeight,
//                    iconRenderWidth,
//                    iconRenderHeight,
//                    0x80FF0000
//            );
//        }
//
//        RenderSystem.disableBlend();
//    }
//
// }
