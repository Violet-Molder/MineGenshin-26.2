package com.linweiyun.genshin.content.entities.attachments.attachment;

import com.linweiyun.genshin.core.network.NetworkManager;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import net.minecraft.server.level.ServerPlayer;

public class PlayerGenshinModeAttachment implements IPersistedSerializable {
  @Persisted(key = "genshin_mode")
  private boolean genshinMode = false;

  public boolean isGenshinMode() {
    return genshinMode;
  }

  public void setGenshinMode(boolean mode, ServerPlayer player) {
    this.genshinMode = mode;
    NetworkManager.setGenshinModeToPlayer(player, mode);
  }

  public void setClientGenshinMode(boolean mode) {
    this.genshinMode = mode;
    NetworkManager.setGenshinModeToServer(mode);
  }

  public void setPacketGenshinMode(boolean mode) {
    this.genshinMode = mode;
  }
}
