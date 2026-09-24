package com.linweiyun.genshin.core.system.about.host;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.system.about.AttachmentProfile;
import com.linweiyun.genshin.core.system.about.AttachmentSource;
import com.linweiyun.genshin.core.system.about.ElementalAttachable;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * <b>生物宿主</b> —— {@link ElementalHost} 在 {@link LivingEntity} 上的实现。
 *
 * <p>筛查委托给实体自己实现的 {@link ElementalAttachable}（由 mixin 注入到 {@code LivingEntity}，
 * 怪物可以覆盖成「只收自己那一种元素」之类的规则）；容器就是实体身上的
 * {@link AttachmentRegistration#CONTAINER 状态容器}。
 */
public final class EntityHost implements ElementalHost {

    private final LivingEntity entity;

    private EntityHost(LivingEntity entity) {
        this.entity = entity;
    }

    /** 包一个生物实体；{@code entity == null} 时返回 {@code null}。 */
    @Nullable
    public static EntityHost of(@Nullable LivingEntity entity) {
        return entity == null ? null : new EntityHost(entity);
    }

    @Override
    public boolean isValid() {
        return entity.isAlive() && !entity.isRemoved();
    }

    @Override
    public StatusContainer container() {
        return isValid() ? entity.getData(AttachmentRegistration.CONTAINER) : null;
    }

    @Override
    public String hostKey() {
        return "entity:" + entity.getUUID();
    }

    @Override
    public boolean acceptsElement(GenshinElement element, AttachmentSource source,
                                  AttachmentProfile profile) {
        if (!(entity instanceof ElementalAttachable attachable)) {
            return true;
        }
        return attachable.onAttachElement(element, source, profile);
    }

    @Override
    public boolean acceptsReaction(GenshinElement attackerElement, GenshinElement defenderElement,
                                   com.linweiyun.genshin.core.system.reaction.ElementalReactionType reactionType) {
        if (!(entity instanceof ElementalAttachable attachable)) {
            return true;
        }
        return attachable.onReactElement(attackerElement, defenderElement, reactionType);
    }

    @Override
    public void onElementAttached(GenshinElement element) {
        element.onAttach(this);
    }

    @Override
    public void onElementDetached(GenshinElement element) {
        element.onDetach(this);
    }

    @Override
    public LivingEntity entity() {
        return entity;
    }

    @Override
    public String toString() {
        return hostKey();
    }
}
