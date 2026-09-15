package com.linweiyun.genshin.core.character.polearm.shenhe;

import com.linweiyun.genshin.config.character.ShenheAttributeConfig;
import com.linweiyun.genshin.content.entities.area.TalismanSpiritArea;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.enums.CharacterAscendAttribute;
import com.linweiyun.genshin.core.system.registry.register.ModEntities;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Shenhe extends PGCharacter {
    private static final Logger LOGGER= LogUtils.getLogger();

    private final ShenheTalent shenheTalent = new ShenheTalent();

    public Shenhe() {
        super(135001, 5, Component.translatable("character.name.shenhe"),
                "minegenshin:cyro", CharacterAscendAttribute.ATK,
                10 * 20, 15*20, 10 * 20, 80f, "shenhe",
                Map.of(
                        ModAttributes.MAX_HP.getId(), ShenheAttributeConfig::getAllHp,
                        ModAttributes.ATK.getId(), ShenheAttributeConfig::getAllAtk,
                        ModAttributes.DEF.getId(), ShenheAttributeConfig::getAllDef
                ));
        this.talent = shenheTalent;
        data.setElementalSkillStacks(2);
    }

    @Override
    protected void triggerElementalSkill(Player player, int skillTime) {
        shenheTalent.elementalSkill(player, this, skillTime);
    }

    @Override
    protected void triggerElementalBurst(Player player) {
        PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        PGCharacter character = attachment.getCurrentCharacter();

        TalismanSpiritArea field = ModEntities.FIELD_TALISMAN_SPIRIT.get()
                .create(player.level(), EntitySpawnReason.EVENT);
        if (field != null) {
            field.setPos(player.position());
            if (character != null) {
                field.setOwner(player, character);
            }
            boolean added = player.level().addFreshEntity(field);
        }
    }

    @Override
    protected void triggerNormalAttack(Player player, int comboStage) {
        shenheTalent.attack(player, this, comboStage);
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