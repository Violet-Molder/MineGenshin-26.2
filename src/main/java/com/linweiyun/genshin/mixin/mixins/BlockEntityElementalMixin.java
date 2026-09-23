package com.linweiyun.genshin.mixin.mixins;

import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.system.about.AttachmentProfile;
import com.linweiyun.genshin.core.system.about.AttachmentSource;
import com.linweiyun.genshin.core.system.about.ElementalAttachable;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockEntity.class)
public class BlockEntityElementalMixin implements ElementalAttachable {

    @Override
    public boolean onAttachElement(GenshinElement element, AttachmentSource source, AttachmentProfile profile) {
        return true;
    }
}