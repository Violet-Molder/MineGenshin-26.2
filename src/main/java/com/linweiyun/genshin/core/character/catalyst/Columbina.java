package com.linweiyun.genshin.core.character.catalyst;

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

public class Columbina extends PGCharacter {
    public Columbina() {
        super(145001, 5, Component.translatable("character.name.columbina"),
                ElementalsGIM.HYDRO, CharacterAscendAttribute.ATK,
                17 * 20, 20 * 20, 80f, "columbina",
                Map.of(
                        ModAttributes.MAX_HP.getId(), Config.COLUMBINA_HP,
                        ModAttributes.ATK.getId(), Config.COLUMBINA_ATK,
                        ModAttributes.DEF.getId(), Config.COLUMBINA_DEF
                ));
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
    public Map<Identifier, Supplier<List<? extends Integer>>> getStatGrowthMap() {
        return Map.of(
                ModAttributes.MAX_HP.getId(), Config.COLUMBINA_HP,
                ModAttributes.ATK.getId(), Config.COLUMBINA_ATK,
                ModAttributes.DEF.getId(), Config.COLUMBINA_DEF
        );
    }
}
