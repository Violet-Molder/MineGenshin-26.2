package com.linweiyun.genshin.core.character.polearm.raiden_shogun;

import com.linweiyun.genshin.config.character.ShenheAttributeConfig;
import com.linweiyun.genshin.core.character.polearm.PolearmCharacter;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import com.linweiyun.genshin.enums.CharacterAscendAttribute;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class RaidenShogun extends PolearmCharacter {
    public RaidenShogun() {
        super(135003, 5, Component.translatable("character.name.raiden_shogun"),
            ModElements.ELECTRO.getId().toString(), CharacterAscendAttribute.ATK,
            10 * 20,  10 * 20, 80f, "raiden_shogun",
            Map.of(
                    ModAttributes.MAX_HP.getId(), ShenheAttributeConfig::getAllHp,
                    ModAttributes.ATK.getId(), ShenheAttributeConfig::getAllAtk,
                    ModAttributes.DEF.getId(), ShenheAttributeConfig::getAllDef
            ));
        this.talent = new RaidenShogunTalent();
    }

    @Override
    public Map<Identifier, Supplier<List<? extends Integer>>> getStatGrowthMap() {
        return Map.of(
                ModAttributes.MAX_HP.getId(), ShenheAttributeConfig::getAllHp,
                ModAttributes.ATK.getId(), ShenheAttributeConfig::getAllAtk,
                ModAttributes.DEF.getId(), ShenheAttributeConfig::getAllDef
        );
    }
}
