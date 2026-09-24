package com.linweiyun.genshin.client.damage;

import com.linweiyun.genshin.Minegenshin;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Clip;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.mojang.logging.LogUtils;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(value = Dist.CLIENT)
public class DamageIndicatorRenderer {
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final String CLASS_DAMAGE_INDICATOR = "damage-indicator";
    public static final String CLASS_CRIT             = "crit";
    public static final String CLASS_REACTION         = "reaction";
    public static final String CLASS_HEAL             = "heal";
    public static final String CLASS_TEXT             = "text";
    /** wrapper 用：只在 mask 范围内显示（垂直渐变） */
    public static final String CLASS_GRADIENT_WRAPPER = "di-gradient-top";

    /** MC 字体默认高度 */
    private static final int FONT_HEIGHT = 9;

    private static final double NEAR_DISTANCE = 4.0;
    private static final double FAR_DISTANCE  = 32.0;
    private static final float  FAR_SCALE     = 0.25f;
    private static final double MAX_RENDER_DISTANCE = 128.0;

    private static UIElement hudRoot;
    private static final List<LabelEntry> LABEL_ENTRIES = new ArrayList<>();

    /**
     * 每个飘字对应：
     *   bottomLabel —— 底层完整显示 bottomColor
     *   topWrapper  —— 顶层 wrapper（带 mask），内部 topLabel 显示 topColor
     */
    private record LabelEntry(DamageIndicator indicator,
                              UIElement topWrapper,
                              Label topLabel,
                              Label bottomLabel) {}

    public static UIElement buildHudRoot() {
        hudRoot = new UIElement()
                .setId("damage-indicator-root")
                .layout(l -> l.widthPercent(100).heightPercent(100));
        return hudRoot;
    }

    public static void onIndicatorAdded(DamageIndicator indicator) {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        DamageIndicatorManager.tick();
    }

    @SubscribeEvent
    public static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        updateHudLabels();
    }

    private static void updateHudLabels() {
        if (hudRoot == null) return;
        if (LABEL_ENTRIES.isEmpty() && DamageIndicatorManager.getActive().isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Camera camera = mc.gameRenderer.mainCamera();
        Vec3 camPos = camera.position();

        // 1. 清理过期
        Iterator<LabelEntry> it = LABEL_ENTRIES.iterator();
        while (it.hasNext()) {
            LabelEntry entry = it.next();
            if (entry.indicator().isExpired()) {
                hudRoot.removeChild(entry.topWrapper());
                hudRoot.removeChild(entry.bottomLabel());
                it.remove();
            }
        }

        // 2. 创建新飘字
        for (DamageIndicator indicator : DamageIndicatorManager.getActive()) {
            boolean exists = false;
            for (LabelEntry entry : LABEL_ENTRIES) {
                if (entry.indicator() == indicator) { exists = true; break; }
            }
            if (exists) continue;

            // 计算文字宽高
            int textW = Math.max(1, mc.font.width(I18n.get(indicator.text)));
            int textH = FONT_HEIGHT;

            // ===== 底层：bottomColor，完整显示 =====
            Style bottomStyle = Style.EMPTY.withColor(TextColor.fromRgb(indicator.bottomColor & 0xFFFFFF));
            if (indicator.italic) bottomStyle = bottomStyle.withItalic(true);
            Label bottomLabel = new Label();
            bottomLabel.setText(Component.translatable(indicator.text).withStyle(bottomStyle));
            applyStyleClasses(bottomLabel, indicator);
            bottomLabel.layout(l -> l
                    .positionType(TaffyPosition.ABSOLUTE)
                    .left(0).top(0)
                    .width(textW).height(textH));

            // ===== 顶层：wrapper + topLabel =====
            UIElement topWrapper = new UIElement();
            topWrapper.setId("di-gradient-wrapper");

            topWrapper.layout(l -> l
                    .positionType(TaffyPosition.ABSOLUTE)
                    .left(0).top(0)
                    .width(textW).height(textH));

            Style topStyle = Style.EMPTY.withColor(TextColor.fromRgb(indicator.topColor & 0xFFFFFF));
            if (indicator.italic) topStyle = topStyle.withItalic(true);
            Label topLabel = new Label();
            topLabel.setText(Component.translatable(indicator.text).withStyle(topStyle));
            applyStyleClasses(topLabel, indicator);
            topLabel.layout(l -> l.width(textW).height(textH));

            if (indicator.isGradient()) {
                // 挂 mask：wrapper 用垂直渐变压 mask，让顶层只在文字上半部分显示
                topWrapper.addClass(CLASS_GRADIENT_WRAPPER);
//                topWrapper.style(s -> {
//                    s.clip(Clip.MASK)
//                            .mask(SpriteTexture.of(Minegenshin.id("gui/damage_indicator_mask.png")));
//                });
            } else {
                // 非渐变：顶层完全透明，避免叠影
                topLabel.style(s -> s.opacity(0f));
            }

            topWrapper.addChild(topLabel);

            hudRoot.addChild(bottomLabel);
            hudRoot.addChild(topWrapper);
            LABEL_ENTRIES.add(new LabelEntry(indicator, topWrapper, topLabel, bottomLabel));
        }

        // 3. 每帧更新位置 / 缩放 / 透明度
        for (LabelEntry entry : LABEL_ENTRIES) {
            DamageIndicator indicator = entry.indicator();

            Vec3 worldPos = indicator.getCurrentPosition();
            double distance = camPos.distanceTo(worldPos);
            if (distance > MAX_RENDER_DISTANCE) {
                entry.topWrapper().style(s -> s.opacity(0f));
                entry.bottomLabel().style(s -> s.opacity(0f));
                continue;
            }

            Vec3 ndc = mc.gameRenderer.projectPointToScreen(worldPos);
            double windowWidth  = mc.getWindow().getWidth();
            double windowHeight = mc.getWindow().getHeight();
            double screenX = (ndc.x + 1.0) * 0.5 * windowWidth;
            double screenY = (1.0 - (ndc.y + 1.0) * 0.5) * windowHeight;
            int guiX = (int) (screenX / mc.getWindow().getGuiScale());
            int guiY = (int) (screenY / mc.getWindow().getGuiScale());

            float alpha = indicator.getAlpha();
            float distanceScale = computeDistanceScale(distance);
            float scale = indicator.getScale() * distanceScale;

            int textW = Math.max(1, mc.font.width(I18n.get(indicator.text)));
            int textH = FONT_HEIGHT;
            entry.bottomLabel().layout(l -> l
                    .positionType(TaffyPosition.ABSOLUTE)
                    .left(guiX).top(guiY)
                    .width(textW).height(textH));
            entry.bottomLabel().style(s -> s.opacity(alpha));
            entry.bottomLabel().transform(t -> t.scale(scale));

            // 顶层 wrapper
            entry.topWrapper().layout(l -> l
                    .positionType(TaffyPosition.ABSOLUTE)
                    .left(guiX).top(guiY)
                    .width(textW).height(textH));
            if (indicator.isGradient()) {
                entry.topWrapper().style(s -> s.opacity(alpha));
            }
            entry.topWrapper().transform(t -> t.scale(scale));
        }
    }

    private static void applyStyleClasses(Label label, DamageIndicator indicator) {
        label.addClass(CLASS_DAMAGE_INDICATOR);
        switch (indicator.style) {
            case 1 -> label.addClass(CLASS_CRIT);
            case 2 -> label.addClass(CLASS_REACTION);
            case 3 -> label.addClass(CLASS_HEAL);
            case 4 -> label.addClass(CLASS_TEXT);
            default -> { /* NORMAL */ }
        }
    }

    private static float computeDistanceScale(double distance) {
        if (distance <= NEAR_DISTANCE) return 1.0f;
        if (distance >= FAR_DISTANCE)  return FAR_SCALE;
        double t = (distance - NEAR_DISTANCE) / (FAR_DISTANCE - NEAR_DISTANCE);
        return (float) (1.0 - t * (1.0 - FAR_SCALE));
    }
}
