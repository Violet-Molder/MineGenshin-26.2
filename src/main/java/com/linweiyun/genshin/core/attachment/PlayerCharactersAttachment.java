package com.linweiyun.genshin.core.attachment;

import com.linweiyun.genshin.core.character.PGCharacterData;
import com.linweiyun.genshin.core.network.NetworkManager;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class PlayerCharactersAttachment implements IPersistedSerializable {

    @Persisted(key = "owned_characters")
    private List<PGCharacterData> ownedCharacters = new ArrayList<>();

    @Persisted(key = "sheet_character_uuids")
    private List<Integer> sheetCharacterUUIDs = new ArrayList<>();

    @Persisted(key = "party_character_uuids")
    private List<Integer> partyCharacterUUIDs = new ArrayList<>(List.of(0, 0, 0, 0));

    @Persisted(key = "current_character_index")
    private int currentCharacterIndex = 0;

    public PlayerCharactersAttachment() {}

    public @Nullable PGCharacterData getCharacterByUUID(int uuid) {
        for (PGCharacterData character : ownedCharacters) {
            if (character.getCharacterUUID() == uuid) {
                return character;
            }
        }
        return null;
    }

    public List<PGCharacterData> getOwnedCharacters() { return ownedCharacters; }

    public boolean hasCharacter(int uuid) {
        return getCharacterByUUID(uuid) != null;
    }

    public void addCharacter(PGCharacterData character) {
        if (!hasCharacter(character.getCharacterUUID())) {
            ownedCharacters.add(character);
            sheetCharacterUUIDs.add(character.getCharacterUUID());
        }
    }
    public boolean removeCharacter(int uuid) {
        PGCharacterData removed = getCharacterByUUID(uuid);
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
    // ========== 服务端触发 ==========

    public void addCharacterToPlayer(ServerPlayer player, PGCharacterData character) {
        addCharacter(character);
        syncToPlayer(player);
    }

    public void removeCharacterToPlayer(ServerPlayer player, int uuid) {
        removeCharacter(uuid);
        syncToPlayer(player);
    }

    // ========== 客户端触发 ==========

    public void addCharacterToServer(PGCharacterData character) {
        addCharacter(character);
        syncToServer();
    }

    public void removeCharacterToServer(int uuid) {
        removeCharacter(uuid);
        syncToServer();
    }

    // ========== 序列化工具方法 ==========


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

    public List<Integer> getSheetCharacterUUIDs() { return sheetCharacterUUIDs; }

    //当前队伍角色
    public List<Integer> getPartyCharacterUUIDs() { return partyCharacterUUIDs; }

    public @Nullable PGCharacterData getPartyCharacter(int index) {
        if (index < 0 || index >= partyCharacterUUIDs.size()) return null;
        int uuid = partyCharacterUUIDs.get(index);
        return uuid == 0 ? null : getCharacterByUUID(uuid);
    }

    public @Nullable PGCharacterData getCurrentCharacter() {
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
        syncToPlayer(player);
    }

    public void removePartyCharacterToPlayer(ServerPlayer player, int index) {
        removePartyCharacter(index);
        syncToPlayer(player);
    }

    public void setCurrentCharacterToPlayer(ServerPlayer player, int index) {
        setCurrentCharacterIndex(index);
        NetworkManager.setCharacterSelectionToPlayer(player, index);
    }

    // ========== 队伍同步 - 客户端触发 ==========

    public void setPartyCharacterToServer(int index, int characterUUID) {
        setPartyCharacter(index, characterUUID);
        syncToServer();
    }

    public void removePartyCharacterToServer(int index) {
        removePartyCharacter(index);
        syncToServer();
    }

    public void setCurrentCharacterToServer(int index) {
        setCurrentCharacterIndex(index);
        NetworkManager.setCharacterSelectionToServer( index);
    }


}
