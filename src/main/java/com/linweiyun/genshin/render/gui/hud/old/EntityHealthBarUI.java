package com.linweiyun.genshin.render.gui.hud.old;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.render.gui.components.state_bind_com.HPProgressBar;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

public class EntityHealthBarUI {

    private static final Identifier BAR_TEXTURE =
            Minegenshin.id("textures/gui/short_character_hp_bar_green.png");

    /**
     * 为指定实体构建一个血条 UI 元素
     * @param entity 目标生物
     * @return 配置好的 ProgressBar
     */
    public static HPProgressBar createHealthBar(LivingEntity entity) {
        HPProgressBar healthBar = new HPProgressBar();

        // 绑定数据源：实时获取实体当前生命值百分比
        healthBar.bindDataSource(SupplierDataSource.of(() -> {
            float health = entity.getHealth();
            float maxHealth = entity.getMaxHealth();
            if (maxHealth <= 0) return 0f;
            return health / maxHealth;
        }));

        // 布局尺寸（可参考 LSS 或直接代码设置）
        healthBar.layout(layout -> {
            layout.width(60);   // 血条宽度
            layout.height(6);   // 血条高度
        });

        // 设置进度条填充纹理（绿色基础血条）
        healthBar.barIcon
                .style(s -> s.background(SpriteTexture.of(BAR_TEXTURE)))
                .layout(l -> {
                    l.width(60);
                    l.height(6);
                });

        // 设置空槽背景纹理（可选，保持与玩家 HUD 一致）
        healthBar.barContainer(c -> c.style(s -> s.background(
                SpriteTexture.of(Minegenshin.id("textures/gui/short_character_hp_green.png")))));

        // 不显示数字标签
        healthBar.label.setText("");

        return healthBar;
    }
}