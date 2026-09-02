package com.linweiyun.genshin;

import com.geckolib.renderer.GeoEntityRenderer;
import com.linweiyun.genshin.client.render.entity.FieldTalismanSpiritRender;
import com.linweiyun.genshin.core.system.registry.register.EntityRegister;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDebugEntriesEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;


@Mod(value = Minegenshin.MOD_ID, dist = Dist.CLIENT)

@EventBusSubscriber(modid = Minegenshin.MOD_ID, value = Dist.CLIENT)
public class MinegenshinClient {
    public MinegenshinClient(ModContainer container) {

        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
    }

    @SubscribeEvent
    public static void registerDebugEntries(RegisterDebugEntriesEvent event) {}

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                EntityRegister.FIELD_TALISMAN_SPIRIT.get(), FieldTalismanSpiritRender::new);
        event.registerEntityRenderer(
                EntityRegister.SLIME_CYRO.get(), context -> new GeoEntityRenderer<>(context, EntityRegister.SLIME_CYRO.get())
        );
    }

}
