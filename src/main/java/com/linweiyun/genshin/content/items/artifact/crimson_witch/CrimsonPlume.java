package com.linweiyun.genshin.content.items.artifact.crimson_witch;

import com.linweiyun.genshin.content.items.artifact.PlumeArtifact;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.registry.register.ArtifactSets;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CrimsonPlume extends PlumeArtifact {
    public CrimsonPlume(Properties properties) {
        super(properties);
        this.set = ArtifactSets.CRIMSON_WITCH;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        boolean isGenshin = player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT);
        if(isGenshin){
            PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            PGCharacter character = attachment.getCurrentCharacter();
            if (character != null) {
                character.equipArtifact(this.type, new ItemStack(this));
            }
        }
        return super.use(level, player, hand);
    }
}
