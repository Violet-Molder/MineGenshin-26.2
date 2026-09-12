package com.linweiyun.genshin.render.gui.components.state_bind_com;

import com.linweiyun.genshin.Minegenshin;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Clip;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@KJSBindings
@LDLRegister(name = "mob-health-bar", group = "minegenshin", registry = "ldlib2:ui_element")
public class MobHealthBar extends ProgressBar {
    public final UIElement barIcon;

    private LivingEntity trackedEntity;

    public MobHealthBar() {
        this.barContainer.layout(layout -> {
            layout.paddingAll(0);
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        this.bar
                .addChild(barIcon = new UIElement())
                .layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE))
                .style(s -> {
                    s.background(SpriteTexture.of(
                            Identifier.fromNamespaceAndPath("minegenshin", "textures/empty.png")));
                    s.clip(Clip.SCISSOR);
                });

        // 填充纹理（绿色基础血条）
        this.barIcon.style(s -> s.background(SpriteTexture.of(
                Minegenshin.id("textures/gui/short_character_hp_bar_green.png"))));

        // 空槽背景纹理
        this.barContainer(c -> c.style(s -> s.background(
                SpriteTexture.of(Minegenshin.id("textures/gui/short_character_hp_green.png")))));

        // 不显示数字
        this.label.setText("");
    }

    /**
     * 绑定目标实体，并建立血量数据源。
     */
    public MobHealthBar track(LivingEntity entity) {
        this.trackedEntity = entity;
        this.bindDataSource(SupplierDataSource.of(() -> {
            if (trackedEntity == null || !trackedEntity.isAlive()) return 0f;
            float max = trackedEntity.getMaxHealth();
            if (max <= 0) return 0f;
            return Math.clamp(trackedEntity.getHealth() / max, 0f, 1f);
        }));
        return this;
    }

    /**
     * 每帧根据血量比例切换样式类，用于 LSS 中的颜色覆盖。
     */
    public void updateStyleClass() {
        if (trackedEntity == null) return;
        float ratio = Math.clamp(trackedEntity.getHealth() / trackedEntity.getMaxHealth(), 0f, 1f);
        removeClass("normal");
        removeClass("low");
        removeClass("critical");
        if (ratio < 0.3f) {
            addClass("critical");
        } else if (ratio < 0.5f) {
            addClass("low");
        } else {
            addClass("normal");
        }
    }

    public LivingEntity getTrackedEntity() {
        return trackedEntity;
    }
}