package com.linweiyun.genshin.core.events;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.client.keybindings.KeyMappingRegistry;
import com.linweiyun.genshin.content.entities.attachments.AttachmentRegistration;
import com.linweiyun.genshin.content.entities.attachments.attachment.PlayerPrimogemAttachment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientEvents {
  @SubscribeEvent
  public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
    for (KeyMapping keyMapping : KeyMappingRegistry.KEY_MAPPINGS) {
      event.register(keyMapping);
    }
  }

  @SubscribeEvent
  public static void onScreenRender(ScreenEvent.Render.Post event) {
    int screenWidth = event.getScreen().width;
    int screenHeight = event.getScreen().height;

    if (event.getScreen() instanceof InventoryScreen inventoryScreen) {
      renderPrimogemOnInventory(
          event.getGuiGraphics(), Minecraft.getInstance().player, false, screenWidth, screenHeight);
    } else if (event.getScreen() instanceof CreativeModeInventoryScreen creativeScreen) {
      if (creativeScreen.isInventoryOpen()) {
        renderPrimogemOnInventory(
            event.getGuiGraphics(),
            Minecraft.getInstance().player,
            true,
            screenWidth,
            screenHeight);
      }
    }
  }

  private static void renderPrimogemOnInventory(
      GuiGraphics guiGraphics,
      Player player,
      boolean isCreativeInventory,
      int screenWidth,
      int screenHeight) {
    if (player == null) return;
    PlayerPrimogemAttachment primogem = player.getData(AttachmentRegistration.PRIMOGEM_ATTACHMENT);
    long primogemCount = primogem.getPrimogem();
    int x;
    int y;
    if (isCreativeInventory) {
      x = screenWidth / 2 + 30;
      y = screenHeight / 2 - 35;
    } else {
      x = screenWidth / 2 + 40;
      y = screenHeight / 2 - 20;
    }
    ResourceLocation iconLocation =
        ResourceLocation.fromNamespaceAndPath(Minegenshin.MOD_ID, "icon/primogem_icon.png");

    String text = ":";
    String count = String.valueOf(primogemCount);
    Font font = Minecraft.getInstance().font;
    guiGraphics.blit(iconLocation, x, y, 0, 0, 16, 16, 16, 16);
    guiGraphics.drawString(font, text, x + 18, y + 5, 0x000000, false);
    guiGraphics.drawString(font, count, x + 21, y + 6, 0x000000, false);
  }
}
