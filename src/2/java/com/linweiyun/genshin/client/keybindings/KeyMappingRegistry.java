package com.linweiyun.genshin.client.keybindings;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyMappingRegistry {
  public static final List<KeyMapping> KEY_MAPPINGS = new ArrayList<>();
  // G键：切换原神模式
  public static final KeyMapping G_KEY =
      new KeyMapping(
          "key.minegenshin.g_mode",
          KeyConflictContext.IN_GAME,
          InputConstants.Type.KEYSYM,
          InputConstants.KEY_G,
          "key.categories.minegenshin");

  // V键：切换当前选择的角色
  public static final KeyMapping V_KEY =
      new KeyMapping(
          "key.minegenshin.v_character",
          KeyConflictContext.IN_GAME,
          InputConstants.Type.KEYSYM,
          InputConstants.KEY_V,
          "key.categories.minegenshin");

  // X键：触发当前选择角色的元素战技
  public static final KeyMapping X_KEY =
      new KeyMapping(
          "key.minegenshin.x_element_skill",
          KeyConflictContext.IN_GAME,
          InputConstants.Type.KEYSYM,
          GLFW.GLFW_KEY_X,
          "key.categories.minegenshin");

  // C键：触发当前选择角色的元素爆发
  public static final KeyMapping C_KEY =
      new KeyMapping(
          "key.minegenshin.c_element_burst",
          KeyConflictContext.IN_GAME,
          InputConstants.Type.KEYSYM,
          InputConstants.KEY_C,
          "key.categories.minegenshin");
  public static final KeyMapping O_KEY =
      new KeyMapping(
          "key.minegenshin.o_character_party",
          KeyConflictContext.IN_GAME,
          InputConstants.Type.KEYSYM,
          InputConstants.KEY_O,
          "key.categories.minegenshin");
  public static final KeyMapping R_KEY =
      new KeyMapping(
          "key.minegenshin.r_wish",
          KeyConflictContext.IN_GAME,
          InputConstants.Type.KEYSYM,
          InputConstants.KEY_R,
          "key.categories.minegenshin");
  public static final KeyMapping F_KEY =
          new KeyMapping(
                  "key.minegenshin.f_hold",
                  KeyConflictContext.IN_GAME,
                  InputConstants.Type.KEYSYM,
                  InputConstants.KEY_F,
                  "key.categories.minegenshin");

  static {
    KEY_MAPPINGS.add(G_KEY);
    KEY_MAPPINGS.add(V_KEY);
    KEY_MAPPINGS.add(X_KEY);
    KEY_MAPPINGS.add(C_KEY);
    KEY_MAPPINGS.add(O_KEY);
    KEY_MAPPINGS.add(R_KEY);
    KEY_MAPPINGS.add(F_KEY);
  }
}
