package com.linweiyun.genshin.core.attachment;

import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.network.NetworkManager;
import com.linweiyun.genshin.core.system.registry.register.ModCharacters;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.TagValueOutput;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class PlayerCharactersAttachment implements IPersistedSerializable {
    public static final Logger LOGGER = LogUtils.getLogger();
    @Persisted(key = "owned_characters")
    private List<PGCharacter> ownedCharacters = new ArrayList<>();

    @Persisted(key = "sheet_character_uuids")
    private List<Integer> sheetCharacterUUIDs = new ArrayList<>();

    @Persisted(key = "party_character_uuids")
    private List<Integer> partyCharacterUUIDs = new ArrayList<>(List.of(0, 0, 0, 0));

    @Persisted(key = "current_character_index")
    private int currentCharacterIndex = 0;

    public PlayerCharactersAttachment() {}

    public @Nullable PGCharacter getCharacterByUUID(int uuid) {
        for (PGCharacter character : ownedCharacters) {
            if (character.getCharacterUUID() == uuid) {
                return character;
            }
        }
        return null;
    }

    public List<PGCharacter> getOwnedCharacters() { return ownedCharacters; }

    public boolean hasCharacter(int uuid) {
        return getCharacterByUUID(uuid) != null;
    }

    //AI addCharacter 加 Player 参数，绑定所属玩家
    public void addCharacter(PGCharacter character, @Nullable Player player) {
        if (!hasCharacter(character.getCharacterUUID())) {
            if (player != null) {
                character.getData().setOwnerPlayer(player);
            }
            ownedCharacters.add(character);
            sheetCharacterUUIDs.add(character.getCharacterUUID());
        }
    }

    //AI 反序列化后统一给所有角色绑定 ownerPlayer（通过已有的 ownerUUID 验证）
    public void bindAllOwners(Player player) {
        for (PGCharacter c : ownedCharacters) {
            c.getData().setOwnerPlayer(player);
        }
    }
    public boolean removeCharacter(int uuid) {
        PGCharacter removed = getCharacterByUUID(uuid);
        if (removed == null) return false;
        ownedCharacters.remove(removed);
        sheetCharacterUUIDs.remove(Integer.valueOf(uuid));
        boolean wasInParty = false;
        for (int i = 0; i < partyCharacterUUIDs.size(); i++) {
            if (partyCharacterUUIDs.get(i) == uuid) {
                partyCharacterUUIDs.set(i, 0);
                wasInParty = true;
            }
        }
        if (wasInParty) sortParty();
        if (!ownedCharacters.isEmpty()) {
            if (partyCharacterUUIDs.get(currentCharacterIndex) == 0) {
                currentCharacterIndex = 0;
            }
        } else {
            currentCharacterIndex = 0;
        }
        return true;
    }

    // 在反序列化完成后调用（比如在客户端接收同步数据后）
    public void fixCharacterTypes() {
        for (int i = 0; i < ownedCharacters.size(); i++) {
            PGCharacter base = ownedCharacters.get(i);
            if (base == null) continue;
            // 从注册表获取正确的子类实例
            PGCharacter subclass = ModCharacters.getByUUID(base.getCharacterUUID());
            if (subclass != null && subclass.getClass() != base.getClass()) {
                // 保留已反序列化的数据，替换为子类实例
                subclass.setData(base.getData());
                ownedCharacters.set(i, subclass);
            }
        }
    }

    public void syncToPlayer(ServerPlayer player) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.registryAccess());
        serialize(output);
        NetworkManager.setPlayerCharactersToPlayer(player, output.buildResult());
    }

    public void syncToServer() {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING,
                net.minecraft.client.Minecraft.getInstance().player.registryAccess());
        serialize(output);
        NetworkManager.setPlayerCharactersToServer(output.buildResult());
    }

    // ========== 服务端触发 ==========

    public void addCharacterToPlayer(ServerPlayer player, PGCharacter character) {
        addCharacter(character, player); //AI 透传 Player 进行绑定
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.registryAccess());
        character.serialize(output);
        NetworkManager.addCharacterToPlayer(player, output.buildResult());
    }

    public void removeCharacterToPlayer(ServerPlayer player, int uuid) {
        removeCharacter(uuid);
        NetworkManager.removeCharacterToPlayer(player, uuid);
    }



    // ========== 客户端触发 ==========

    public void addCharacterToServer(PGCharacter character) {
        addCharacter(character, net.minecraft.client.Minecraft.getInstance().player); //AI 客户端也透传 Player
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING,
                net.minecraft.client.Minecraft.getInstance().player.registryAccess());
        character.serialize(output);
        NetworkManager.addCharacterToServer(output.buildResult());
    }

    public void removeCharacterToServer(int uuid) {
        removeCharacter(uuid);
        NetworkManager.removeCharacterToServer(uuid);
    }
    // ========== 序列化工具方法 ==========


// ========== 角色数据同步 ==========

    // 单个角色数据同步到服务端
    public void syncSingleCharacterToServer(PGCharacter character) {
        if (character == null) return;
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING,
                net.minecraft.client.Minecraft.getInstance().player.registryAccess());
        character.serialize(output);
        NetworkManager.setCharacterDataToServer(character.getCharacterUUID(), output.buildResult());
    }

    // 单个角色数据同步到客户端
    public void syncSingleCharacterToPlayer(ServerPlayer player, PGCharacter character) {
        if (character == null) return;
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.registryAccess());
        character.serialize(output);
        NetworkManager.setCharacterDataToPlayer(player, character.getCharacterUUID(), output.buildResult());
    }

    public List<Integer> getSheetCharacterUUIDs() { return sheetCharacterUUIDs; }

    //当前队伍角色
    public List<Integer> getPartyCharacterUUIDs() { return partyCharacterUUIDs; }

    public @Nullable PGCharacter getPartyCharacter(int index) {
        if (index < 0 || index >= partyCharacterUUIDs.size()) return null;
        int uuid = partyCharacterUUIDs.get(index);
        return uuid == 0 ? null : getCharacterByUUID(uuid);
    }

    public @Nullable PGCharacter getCurrentCharacter() {
        return getPartyCharacter(currentCharacterIndex);
    }

    public int getCurrentCharacterIndex() { return currentCharacterIndex; }

    public void setCurrentCharacterIndex(int index) {
        this.currentCharacterIndex = Math.max(0, Math.min(3, index));
    }

    public void setPartyCharacter(int index, int characterUUID) {
        if (index >= 0 && index < 4) {
            while (partyCharacterUUIDs.size() <= index) {
                partyCharacterUUIDs.add(0);
            }
            partyCharacterUUIDs.set(index, characterUUID);
        }
    }

    public boolean removePartyCharacter(int index) {
        if (index < 0 || index >= 4) return false;
        if (partyCharacterUUIDs.get(index) == 0) return false;

        long count = partyCharacterUUIDs.stream().filter(uuid -> uuid != 0).count();
        if (count <= 1) return false;

        partyCharacterUUIDs.set(index, 0);
        sortParty();

        if (currentCharacterIndex == index) {
            currentCharacterIndex = 0;
        } else if (currentCharacterIndex > index) {
            currentCharacterIndex--;
        }
        return true;
    }

    public void sortParty() {
        List<Integer> nonZero = new ArrayList<>();
        for (int uuid : partyCharacterUUIDs) {
            if (uuid != 0) nonZero.add(uuid);
        }
        for (int i = 0; i < 4; i++) {
            partyCharacterUUIDs.set(i, i < nonZero.size() ? nonZero.get(i) : 0);
        }
    }

    public boolean canRemovePartyCharacter(int index) {
        if (index < 0 || index >= 4) return false;
        if (partyCharacterUUIDs.get(index) == 0) return false;
        return partyCharacterUUIDs.stream().filter(uuid -> uuid != 0).count() > 1;
    }
    // ========== 队伍同步 - 服务端触发 ==========

    public void setPartyCharacterToPlayer(ServerPlayer player, int index, int characterUUID) {
        setPartyCharacter(index, characterUUID);
        NetworkManager.setPartyCharacterToPlayer(player, index, characterUUID);
    }

    public boolean removePartyCharacterToPlayer(ServerPlayer player, int index) {
        boolean removed = removePartyCharacter(index);
        if (removed) {
            NetworkManager.removePartyCharacterToPlayer(player, index);
            syncToPlayer(player);
        }
        return removed;
    }

    public void setCurrentCharacterToPlayer(ServerPlayer player, int index) {
        setCurrentCharacterIndex(index);
        NetworkManager.setCharacterSelectionToPlayer(player, index);
    }

    // ========== 队伍同步 - 客户端触发 ==========

    public void setPartyCharacterToServer(int index, int characterUUID) {
        setPartyCharacter(index, characterUUID);
        NetworkManager.setPartyCharacterToServer(index, characterUUID);
    }

    public boolean removePartyCharacterToServer(int index) {
        boolean removed = removePartyCharacter(index);
        if (removed) {
            NetworkManager.removePartyCharacterToServer(index);
        }
        return removed;
    }

    public void setCurrentCharacterToServer(int index) {
        setCurrentCharacterIndex(index);
        NetworkManager.setCharacterSelectionToServer( index);
    }


}