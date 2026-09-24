package com.linweiyun.genshin.render.gui.hud;

import com.google.common.base.Suppliers;
import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.client.damage.DamageIndicatorRenderer;
import com.lowdragmc.lowdraglib2.gui.hud.ModularHudLayer;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = "minegenshin", value = Dist.CLIENT)
public class DamageIndicatorHudRegistration {

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        var damageHudCache = Suppliers.memoize(() -> {
            var stylesheet = StylesheetManager.INSTANCE.getStylesheetSafe(
                    Minegenshin.id("lss/hud/damage_indicator.lss"));
            var ui = UI.of(DamageIndicatorRenderer.buildHudRoot(), stylesheet);
            return ModularUI.of(ui);
        });

        event.registerAboveAll(
                Minegenshin.id("damage_indicator_hud"),
                (ModularHudLayer) damageHudCache::get);

    }
}