package com.linweiyun.genshin.registry.register;

import com.linweiyun.genshin.Config;
import com.linweiyun.genshin.core.character.PGCharacterDefine;
import com.linweiyun.genshin.registry.ModRegistries;
import com.linweiyun.genshin.core.attribute.ModAttributes;
import com.linweiyun.genshin.enums.CharacterAscendAttribute;
import com.linweiyun.genshin.enums.ElementalsGIM;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collection;
import java.util.Map;

public class CharacterRegister {

    public static final DeferredRegister<PGCharacterDefine> CHARACTERS = ModRegistries.CHARACTERS;

    public static final DeferredHolder<PGCharacterDefine, PGCharacterDefine> SHENHE = CHARACTERS.register("shenhe",
            () -> new PGCharacterDefine(
                    135001, 5, Component.translatable("character.name.shenhe"),
                    ElementalsGIM.CYRO, CharacterAscendAttribute.ATK,
                    10 * 20f, 10 * 20f, 80f, "shenhe",
                    Map.of(
                            ModAttributes.MAX_HP.getId(), Config.SHENHE_HP,
                            ModAttributes.ATK.getId(), Config.SHENHE_ATK,
                            ModAttributes.DEF.getId(), Config.SHENHE_DEF
                    )
            ));

    public static final DeferredHolder<PGCharacterDefine, PGCharacterDefine> ARLECCHINO = CHARACTERS.register("arlecchino",
            () -> new PGCharacterDefine(
                    135002, 5, Component.translatable("character.name.arlecchino"),
                    ElementalsGIM.PYRO, CharacterAscendAttribute.ATK,
                    20 * 20f, 20 * 20f, 80f, "arlecchino",
                    Map.of(
                            ModAttributes.MAX_HP.getId(), Config.SHENHE_HP,
                            ModAttributes.ATK.getId(), Config.SHENHE_ATK,
                            ModAttributes.DEF.getId(), Config.SHENHE_DEF
                    )
            ));

    public static final DeferredHolder<PGCharacterDefine, PGCharacterDefine> COLUMBINA = CHARACTERS.register("columbina",
            () -> new PGCharacterDefine(
                    145001, 5, Component.translatable("character.name.columbina"),
                    ElementalsGIM.HYDRO, CharacterAscendAttribute.ATK,
                    17 * 20f, 20 * 20f, 80f, "columbina",
                    Map.of(
                            ModAttributes.MAX_HP.getId(), Config.COLUMBINA_HP,
                            ModAttributes.ATK.getId(), Config.COLUMBINA_ATK,
                            ModAttributes.DEF.getId(), Config.COLUMBINA_DEF
                    )
            ));

    public static PGCharacterDefine getByUUID(int uuid) {
        for (PGCharacterDefine character : CHARACTERS.getRegistry().get()) {
            if (character.getCharacterUUID() == uuid) return character;
        }
        return null;
    }

    public static Collection<PGCharacterDefine> getAllCharacters() {
        return CHARACTERS.getRegistry().get().stream().toList();
    }
    public static void register(IEventBus bus) {
        CHARACTERS.register(bus);
    }
}
