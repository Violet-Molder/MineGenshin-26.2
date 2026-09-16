package com.linweiyun.genshin.core.character.polearm.arlecchino;

import com.linweiyun.genshin.config.character.ArlecchinoAttributeConfig;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import com.linweiyun.genshin.core.character.polearm.PolearmCharacter;
import com.linweiyun.genshin.enums.CharacterAscendAttribute;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Arlecchino extends PolearmCharacter {
    public Arlecchino() {
        super(                   135002, 5, Component.translatable("character.name.arlecchino"),
                ModElements.PYRO.getId().toString(), CharacterAscendAttribute.ATK,
                20 * 20, 20 * 20, 80f, "arlecchino",
                Map.of(
                        ModAttributes.MAX_HP.getId(), ArlecchinoAttributeConfig::getAllHp,
                        ModAttributes.ATK.getId(), ArlecchinoAttributeConfig::getAllAtk,
                        ModAttributes.DEF.getId(), ArlecchinoAttributeConfig::getAllDef
                ));

    }

    @Override
    public Map<Identifier, Supplier<List<? extends Integer>>> getStatGrowthMap() {
        return Map.of(
                ModAttributes.MAX_HP.getId(), ArlecchinoAttributeConfig::getAllHp,
                ModAttributes.ATK.getId(), ArlecchinoAttributeConfig::getAllAtk,
                ModAttributes.DEF.getId(), ArlecchinoAttributeConfig::getAllDef
        );
    }
}