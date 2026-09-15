package com.linweiyun.genshin.core.character.polearm.arlecchino;

import com.linweiyun.genshin.config.character.ArlecchinoAttributeConfig;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.enums.CharacterAscendAttribute;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Arlecchino extends PGCharacter {
    public Arlecchino() {
        super(                    135002, 5, Component.translatable("character.name.arlecchino"),
                "minegenshin:pyro", CharacterAscendAttribute.ATK,
                20 * 20, 20 * 20, 80f, "arlecchino",
                Map.of(
                        ModAttributes.MAX_HP.getId(), ArlecchinoAttributeConfig::getAllHp,
                        ModAttributes.ATK.getId(), ArlecchinoAttributeConfig::getAllAtk,
                        ModAttributes.DEF.getId(), ArlecchinoAttributeConfig::getAllDef
                ));
    }

    @Override
    protected void triggerElementalSkill(Player player, int skillTime) {
        if (skillTime == -1) {
            player.sendSystemMessage(Component.literal("已触发阿蕾奇诺元素技能"));
        }

    }

    @Override
    protected void triggerElementalBurst(Player player) {

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