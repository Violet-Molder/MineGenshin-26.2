package com.linweiyun.genshin.core.character.polearm;

import com.linweiyun.genshin.Config;
import com.linweiyun.genshin.core.attribute.ModAttributes;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.enums.CharacterAscendAttribute;
import com.linweiyun.genshin.enums.ElementalsGIM;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Arlecchino extends PGCharacter {
    public Arlecchino() {
        super(                    135002, 5, Component.translatable("character.name.arlecchino"),
                ElementalsGIM.PYRO, CharacterAscendAttribute.ATK,
                20 * 20, 20 * 20, 80f, "arlecchino",
                Map.of(
                        ModAttributes.MAX_HP.getId(), Config.SHENHE_HP,
                        ModAttributes.ATK.getId(), Config.SHENHE_ATK,
                        ModAttributes.DEF.getId(), Config.SHENHE_DEF
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
                ModAttributes.MAX_HP.getId(), Config.SHENHE_HP,
                ModAttributes.ATK.getId(), Config.SHENHE_ATK,
                ModAttributes.DEF.getId(), Config.SHENHE_DEF
        );
    }
}
