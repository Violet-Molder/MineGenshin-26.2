package com.linweiyun.genshin.core.character.sword.vesna;

import com.linweiyun.genshin.config.character.ShenheAttributeConfig;
import com.linweiyun.genshin.core.character.IStellarSwirlParticipant;
import com.linweiyun.genshin.core.character.polearm.PolearmCharacter;
import com.linweiyun.genshin.core.character.sword.SwordCharacter;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import com.linweiyun.genshin.enums.CharacterAscendAttribute;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Vesna extends SwordCharacter implements IStellarSwirlParticipant {
    public static final String ID = "vesna";
    public static final String VESNA_ENERGY = "vesna_energy";
    public static final int UID = 115001;
    private static final Logger LOGGER = LogUtils.getLogger();

    public Vesna() {
        super(UID, 5, Component.translatable("character.name.test"),
                ModElements.ANEMO.getId().toString(), CharacterAscendAttribute.ATK,
                10 * 20, 10 * 20, 80f, ID,
                Map.of(
                        ModAttributes.MAX_HP.getId(), ShenheAttributeConfig::getAllHp,
                        ModAttributes.ATK.getId(), ShenheAttributeConfig::getAllAtk,
                        ModAttributes.DEF.getId(), ShenheAttributeConfig::getAllDef
                ));
        this.talent = new VesnaTalent();
        this.getData().getAttachments().put(new VesnaEnergy());
    }

    @Override
    public Map<Identifier, Supplier<List<? extends Integer>>> getStatGrowthMap() {
        return Map.of(
                ModAttributes.MAX_HP.getId(), ShenheAttributeConfig::getAllHp,
                ModAttributes.ATK.getId(), ShenheAttributeConfig::getAllAtk,
                ModAttributes.DEF.getId(), ShenheAttributeConfig::getAllDef
        );
    }
    public void addEnergy(float value) {
        VesnaEnergy energy = this.getData().getAttachments().get(VESNA_ENERGY);
        if (energy == null) return;
        energy.addEnergy(1f);
        LOGGER.info("TestTalent attack energy: {}", energy.getEnergy());

    }
}