package com.linweiyun.genshin.registry.register;

import com.linweiyun.genshin.Config;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.character.catalyst.Columbina;
import com.linweiyun.genshin.core.character.polearm.Arlecchino;
import com.linweiyun.genshin.core.character.polearm.Shenhe;
import com.linweiyun.genshin.registry.ModRegistries;
import com.linweiyun.genshin.core.attribute.ModAttributes;
import com.linweiyun.genshin.enums.CharacterAscendAttribute;
import com.linweiyun.genshin.enums.ElementalsGIM;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class CharacterRegister {

    public static final DeferredRegister<PGCharacter> CHARACTERS = ModRegistries.CHARACTERS;

    public static final DeferredHolder<PGCharacter, Shenhe> SHENHE = CHARACTERS.register("shenhe", Shenhe::new);

    public static final DeferredHolder<PGCharacter, Arlecchino> ARLECCHINO = CHARACTERS.register("arlecchino", Arlecchino::new);

    public static final DeferredHolder<PGCharacter, Columbina> COLUMBINA = CHARACTERS.register("columbina",
            Columbina::new);

    public static PGCharacter getByUUID(int uuid) {
        for (PGCharacter prototype : CHARACTERS.getRegistry().get()) {
            if (prototype.getCharacterUUID() == uuid) {
                PGCharacter instance = prototype;
                // 从 statGrowthMap 中提取 1 级基础属性初始化 data
                Double baseHP = null, baseATK = null, baseDEF = null;
                for (Map.Entry<Identifier, Supplier<List<? extends Integer>>> entry : instance.getStatGrowthMap().entrySet()) {
                    List<? extends Integer> list = entry.getValue().get();
                    if (list != null && !list.isEmpty()) {
                        double first = list.get(0);
                        if (entry.getKey().equals(ModAttributes.MAX_HP.getId())) baseHP = first;
                        else if (entry.getKey().equals(ModAttributes.ATK.getId())) baseATK = first;
                        else if (entry.getKey().equals(ModAttributes.DEF.getId())) baseDEF = first;
                    }
                }
                if (baseHP != null && baseATK != null && baseDEF != null) {
                    instance.getData().initBaseStats(baseHP, baseATK, baseDEF);
                }
                return instance;
            }
        }
        return null;
    }

    public static Collection<PGCharacter> getAllCharacters() {
        return CHARACTERS.getRegistry().get().stream().toList();
    }
    public static void register(IEventBus bus) {
        CHARACTERS.register(bus);
    }
}
