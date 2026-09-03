package com.linweiyun.genshin.core.attachment;

import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.network.NetworkManager;
import com.linweiyun.genshin.core.system.registry.register.ModCharacters;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
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

    public void addCharacter(PGCharacter character) {
        if (!hasCharacter(character.getCharacterUUID())) {
            ownedCharacters.add(character);
            sheetCharacterUUIDs.add(character.getCharacterUUID());
        }
    }
    public boolean removeCharacter(int uuid) {
        PGCharacter removed = getCharacterByUUID(uuid);
        if (removed == null) return false;
        ownedCharacters.remove(removed);
        sheetCharacterUUIDs.remove(Integer.valueOf(uuid));
        for (int i = 0; i < partyCharacterUUIDs.size(); i++) {
            if (partyCharacterUUIDs.get(i) == uuid) {
                partyCharacterUUIDs.set(i, 0);
            }
        }
        if (!ownedCharacters.isEmpty()) {
            boolean currentStillValid = false;
            if (currentCharacterIndex < partyCharacterUUIDs.size() && partyCharacterUUIDs.get(currentCharacterIndex) != 0) {
                currentStillValid = true;
            }
            if (!currentStillValid) {
                for (int i = 0; i < partyCharacterUUIDs.size(); i++) {
                    if (partyCharacterUUIDs.get(i) != 0) {
                        currentCharacterIndex = i;
                        break;
                    }
                }
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
        addCharacter(character);
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
        addCharacter(character);
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

    public void removePartyCharacter(int index) {
        if (index >= 0 && index < 4 && index < partyCharacterUUIDs.size()) {
            partyCharacterUUIDs.set(index, 0);
            if (currentCharacterIndex == index) {
                for (int i = 0; i < partyCharacterUUIDs.size(); i++) {
                    if (partyCharacterUUIDs.get(i) != 0) {
                        currentCharacterIndex = i;
                        return;
                    }
                }
                currentCharacterIndex = 0;
            }
        }
    }
    // ========== 队伍同步 - 服务端触发 ==========

    public void setPartyCharacterToPlayer(ServerPlayer player, int index, int characterUUID) {
        setPartyCharacter(index, characterUUID);
        NetworkManager.setPartyCharacterToPlayer(player, index, characterUUID);
    }

    public void removePartyCharacterToPlayer(ServerPlayer player, int index) {
        removePartyCharacter(index);
        NetworkManager.removePartyCharacterToPlayer(player, index);
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

    public void removePartyCharacterToServer(int index) {
        removePartyCharacter(index);
        NetworkManager.removePartyCharacterToServer(index);
    }

    public void setCurrentCharacterToServer(int index) {
        setCurrentCharacterIndex(index);
        NetworkManager.setCharacterSelectionToServer( index);
    }


}
