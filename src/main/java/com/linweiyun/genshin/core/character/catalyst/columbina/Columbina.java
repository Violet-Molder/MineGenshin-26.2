package com.linweiyun.genshin.core.character.catalyst.columbina;

import com.linweiyun.genshin.config.character.ColumbinaAttributeConfig;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.enums.CharacterAscendAttribute;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Columbina extends PGCharacter {

    private final ColumbinaTalent columbinaTalent = new ColumbinaTalent();

    public Columbina() {
        super(145001, 5, Component.translatable("character.name.columbina"),
                "minegenshin:hydro", CharacterAscendAttribute.ATK,
                17 * 20, 20 * 20, 80f, "columbina",
                Map.of(
                        ModAttributes.MAX_HP.getId(), ColumbinaAttributeConfig::getAllHp,
                        ModAttributes.ATK.getId(), ColumbinaAttributeConfig::getAllAtk,
                        ModAttributes.DEF.getId(), ColumbinaAttributeConfig::getAllDef
                ));
        this.talent = columbinaTalent;
    }

    @Override
    protected void triggerElementalSkill(Player player, int skillTime) {
        if (skillTime == -1) {
            player.sendSystemMessage(Component.literal("已触发哥伦比娅元素技能"));
        }
    }

    @Override
    protected void triggerElementalBurst(Player player) {

    }

    @Override
    protected void triggerNormalAttack(Player player, int comboStage) {
        columbinaTalent.attack(player, this, comboStage);
    }

    @Override
    public Map<Identifier, Supplier<List<? extends Integer>>> getStatGrowthMap() {
        return Map.of(
                ModAttributes.MAX_HP.getId(), ColumbinaAttributeConfig::getAllHp,
                ModAttributes.ATK.getId(), ColumbinaAttributeConfig::getAllAtk,
                ModAttributes.DEF.getId(), ColumbinaAttributeConfig::getAllDef
        );
    }
}