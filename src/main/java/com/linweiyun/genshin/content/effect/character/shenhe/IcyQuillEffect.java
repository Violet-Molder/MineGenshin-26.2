package com.linweiyun.genshin.content.effect.character.shenhe;

import com.linweiyun.genshin.content.effect.character.CharacterEffectInstance;
import com.linweiyun.genshin.content.effect.character.ICharacterEffect;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSpec;
import com.linweiyun.genshin.core.system.registry.register.ModCharacters;
import com.linweiyun.genshin.enums.ElementalsGIM;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public class IcyQuillEffect implements ICharacterEffect {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String ICY_QUILL_COUNT_KEY = "icy_quill_count";

    @Override
    public void onAttacked(Player holder, PGCharacter character, LivingEntity target, CharacterEffectInstance instance, ModDamageSource damageSource) {
        ModDamageSpec oldDamageSpec = damageSource.getSpec();
        if (oldDamageSpec.getElement() == ElementalsGIM.CYRO) {
            PlayerCharactersAttachment charactersAttachment = holder.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            PGCharacter shenhe = charactersAttachment.getCharacterByUUID(ModCharacters.SHENHE.get().getCharacterUUID());
            if (shenhe != null) {
                float flatDamageBonus = (float) (shenhe.getData().getAttributeTotalValue(ModAttributes.ATK.value()) * 0.776f);
                ModDamageSpec newDamageSpec = oldDamageSpec.withFlatDamageBonus(flatDamageBonus);
                damageSource.setSpec(newDamageSpec);
            }
        }
    }

    @Override
    public void onEffectOverride(
            Player holder,
            PGCharacter character,
            CharacterEffectInstance existingInstance,
            CharacterEffectInstance newInstance) {

        // 1. 持续时间：取较大值
        int existingDuration = existingInstance.getDuration();
        int newDuration = newInstance.getDuration();
        int maxDuration = Math.max(existingDuration, newDuration);
        newInstance.setDuration(maxDuration);

        // 2. 冰凌数量：取较大值
        int existingCount = existingInstance.getIntData(ICY_QUILL_COUNT_KEY);
        int newCount = newInstance.getIntData(ICY_QUILL_COUNT_KEY);
        int maxCount = Math.max(existingCount, newCount);
        newInstance.setIntData(ICY_QUILL_COUNT_KEY, maxCount);
    }
}
