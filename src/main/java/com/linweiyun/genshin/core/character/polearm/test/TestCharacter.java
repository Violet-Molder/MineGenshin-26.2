package com.linweiyun.genshin.core.character.polearm.test;

import com.linweiyun.genshin.config.character.ShenheAttributeConfig;
import com.linweiyun.genshin.core.character.IStellarSwirlParticipant;
import com.linweiyun.genshin.core.character.polearm.PolearmCharacter;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import com.linweiyun.genshin.enums.CharacterAscendAttribute;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class TestCharacter extends PolearmCharacter implements IStellarSwirlParticipant {

    public TestCharacter() {
        super(135005, 5, Component.translatable("character.name.test"),
                ModElements.ANEMO.getId().toString(), CharacterAscendAttribute.ATK,
                10 * 20, 10 * 20, 80f, "test",
                Map.of(
                        ModAttributes.MAX_HP.getId(), ShenheAttributeConfig::getAllHp,
                        ModAttributes.ATK.getId(), ShenheAttributeConfig::getAllAtk,
                        ModAttributes.DEF.getId(), ShenheAttributeConfig::getAllDef
                ));
        this.talent = new TestTalent();
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