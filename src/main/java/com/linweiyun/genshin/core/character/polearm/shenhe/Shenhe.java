package com.linweiyun.genshin.core.character.polearm.shenhe;

import com.linweiyun.genshin.config.character.ShenheAttributeConfig;
import com.linweiyun.genshin.core.character.IStellarSwirlParticipant;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import com.linweiyun.genshin.core.character.polearm.PolearmCharacter;
import com.linweiyun.genshin.enums.CharacterAscendAttribute;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Shenhe extends PolearmCharacter implements IStellarSwirlParticipant {
    private static final Logger LOGGER= LogUtils.getLogger();

    public Shenhe() {
        super(135001, 5, Component.translatable("character.name.shenhe"),
                ModElements.CYRO.getId().toString(), CharacterAscendAttribute.ATK,
                10 * 20, 15*20, 10 * 20, 80f, "shenhe",
                Map.of(
                        ModAttributes.MAX_HP.getId(), ShenheAttributeConfig::getAllHp,
                        ModAttributes.ATK.getId(), ShenheAttributeConfig::getAllAtk,
                        ModAttributes.DEF.getId(), ShenheAttributeConfig::getAllDef
                ));
        this.talent = new ShenheTalent();
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