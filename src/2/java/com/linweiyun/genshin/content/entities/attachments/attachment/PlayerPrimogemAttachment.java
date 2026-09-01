package com.linweiyun.genshin.content.entities.attachments.attachment;

import com.linweiyun.genshin.core.network.NetworkManager;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;

public class PlayerPrimogemAttachment implements INBTSerializable<CompoundTag> {
  @Persisted @DescSynced private long primogem = 0;

  public long getPrimogem() {
    return primogem;
  }

  public void changePrimogem(long amount, ServerPlayer player) {
    this.primogem = amount;
    NetworkManager.setPrimogemToPlayer(player, amount);
  }

  public void changeClientPrimogem(long amount) {
    this.primogem = amount;
    NetworkManager.setPrimogemToServer(amount);
  }

  public void changePacketPrimogem(long amount) {
    this.primogem = amount;
  }

  public void setPrimogem(long amount, ServerPlayer player) {
    changePrimogem(Math.max(0, amount), player);
  }

  public void setClientPrimogem(long amount) {
    changeClientPrimogem(Math.max(0, amount));
  }

  public void addPrimogem(long amount, ServerPlayer player) {
    changePrimogem(Math.max(0, this.primogem + amount), player);
  }

  public void addClientPrimogem(long amount) {
    changeClientPrimogem(Math.max(0, this.primogem + amount));
  }

  public void removePrimogem(long amount, ServerPlayer player) {
    changePrimogem(Math.max(0, this.primogem - amount), player);
  }

  public void removeClientPrimogem(long amount) {
    changeClientPrimogem(Math.max(0, this.primogem - amount));
  }

  @Override
  public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
    CompoundTag tag = new CompoundTag();
    tag.putLong("primogem", primogem);
    return tag;
  }

  @Override
  public void deserializeNBT(HolderLookup.@NotNull Provider provider, CompoundTag compoundTag) {
    this.primogem = compoundTag.getLong("primogem");
  }
}
