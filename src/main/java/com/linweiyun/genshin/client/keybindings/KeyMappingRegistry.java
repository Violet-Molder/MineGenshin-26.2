package com.linweiyun.genshin.client.keybindings;

import com.linweiyun.genshin.Minegenshin;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(value = Dist.CLIENT)
public class KeyMappingRegistry {
  public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(Minegenshin.MOD_ID,"category"));
  public static final Lazy<KeyMapping> G_KEY = Lazy.of(() ->
          new KeyMapping(
                  "key.minegenshin.g_mode",
                  InputConstants.Type.KEYSYM,
                  GLFW.GLFW_KEY_G,
                  CATEGORY));
  public static final Lazy<KeyMapping> V_KEY = Lazy.of(() ->
          new KeyMapping(
                  "key.minegenshin.v_mode",
                  InputConstants.Type.KEYSYM,
                  GLFW.GLFW_KEY_V,
                  CATEGORY));
  public static final Lazy<KeyMapping> X_KEY = Lazy.of(() ->
          new KeyMapping(
                  "key.minegenshin.x_mode",
                  InputConstants.Type.KEYSYM,
                  GLFW.GLFW_KEY_X,
                  CATEGORY));
  public static final Lazy<KeyMapping> C_KEY = Lazy.of(() ->
          new KeyMapping(
                  "key.minegenshin.c_mode",
                  InputConstants.Type.KEYSYM,
                  GLFW.GLFW_KEY_C,
                  CATEGORY));
  public static final Lazy<KeyMapping> O_KEY = Lazy.of(() ->
          new KeyMapping(
                  "key.minegenshin.o_mode",
                  InputConstants.Type.KEYSYM,
                  GLFW.GLFW_KEY_O,
                  CATEGORY));
  public static final Lazy<KeyMapping> R_KEY = Lazy.of(() ->
          new KeyMapping(
                  "key.minegenshin.r_mode",
                  InputConstants.Type.KEYSYM,
                  GLFW.GLFW_KEY_R,
                  CATEGORY));
  public static final Lazy<KeyMapping> F_KEY = Lazy.of(() ->
          new KeyMapping(
                  "key.minegenshin.f_mode",
                  InputConstants.Type.KEYSYM,
                  GLFW.GLFW_KEY_F,
                  CATEGORY));

  public static final Lazy<KeyMapping> CHARACTER_INFO_SCREEN_KEY = Lazy.of(() ->
          new KeyMapping(
                  "key.minegenshin.character_info_screen_key",
                  InputConstants.Type.KEYSYM,
                  GLFW.GLFW_KEY_U,
                  CATEGORY));
  public static final Lazy<KeyMapping> ARTIFACT_EQUIP_SCREEN_KEY = Lazy.of(() ->
          new KeyMapping(
                  "key.minegenshin.artifact_equip_screen_key",
                  InputConstants.Type.KEYSYM,
                  GLFW.GLFW_KEY_B,
                  CATEGORY));
  public static final Lazy<KeyMapping> ARTIFACT_EQUIP_SCREEN_KEY_2 = Lazy.of(() ->
          new KeyMapping(
                  "key.minegenshin.artifact_equip_screen_key_2",
                  InputConstants.Type.KEYSYM,
                  GLFW.GLFW_KEY_N,
                  CATEGORY));
  //TEMP
  public static final Lazy<KeyMapping> CONFIG_SCREEN_KEY = Lazy.of(() ->
          new KeyMapping(
                  "key.minegenshin.config_key",
                  InputConstants.Type.KEYSYM,
                  GLFW.GLFW_KEY_K,
                  CATEGORY));
  @SubscribeEvent // on the mod event bus only on the physical client
  public static void registerBindings(RegisterKeyMappingsEvent event) {
    event.registerCategory(CATEGORY);
    event.register(G_KEY.get());
    event.register(V_KEY.get());
    event.register(X_KEY.get());
    event.register(C_KEY.get());
    event.register(O_KEY.get());
    event.register(R_KEY.get());
    event.register(F_KEY.get());
    event.register(CHARACTER_INFO_SCREEN_KEY.get());
    event.register(ARTIFACT_EQUIP_SCREEN_KEY.get());
    event.register(ARTIFACT_EQUIP_SCREEN_KEY_2.get());
    event.register(CONFIG_SCREEN_KEY.get());
  }
}