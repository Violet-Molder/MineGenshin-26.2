package com.linweiyun.genshin.core.system.about.block;

import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.status.StatusInstance;
import com.linweiyun.genshin.core.system.about.AttachmentProfile;
import com.linweiyun.genshin.core.system.about.AttachmentSource;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.genshin.core.system.about.host.BlockHost;
import net.minecraft.world.level.block.state.BlockState;

/**
 * <b>方块的环境自附着</b> —— 「冰一直给自己挂冰、水一直给自己挂水」。
 *
 * <p>它是不衰减的常驻附着（{@link AttachmentProfile#PERMANENT}）：不随时间掉量，
 * 被反应消耗掉之后由 {@link ElementalAttachmentInstance#tick()} 的补量机制周期补回。
 *
 * <p>为什么必须有它：反应需要「先手 + 后手」。水方块被打冰时，先手是<b>水自带的水</b>，
 * 后手才是这发冰；少了这层自附着，冻结反应根本无从发生 —— 那样就只能靠
 * 「方块状态和容器对不上就改状态」去凑表现，也就是用户说的「表现一样、内核不同」。
 *
 * <p>注意它是<b>内部附着</b>（{@link ElementalAttachmentHelper#attachInternal}）：
 * 自附着只是让方块「本来就是水/冰」，不触发反应。
 */
public final class BlockSelfAura {

    /** 自带元素的满量（U）。取 1U：正好够一次 1:1 的反应把它换掉（水+冰=冻结）。 */
    private static final float BASE_QUANTITY = 1.0f;

    private BlockSelfAura() {
    }

    /**
     * 确保这个方块的容器里有它自带的元素。
     *
     * <p>幂等：已经有就什么都不做；被反应消耗光了（实例被容器清掉）下次读容器时会补回来。
     */
    public static void ensure(BlockHost host, StatusContainer container, BlockState state) {
        if (host == null || container == null) {
            return;
        }
        GenshinElement aura = BlockElementRules.selfAura(state);
        if (aura == null) {
            return;
        }
        if (hasAlive(container, aura)) {
            return;
        }
        // ⚠️ host 传 null：自带元素是方块的<b>天性</b>，不经过「能被什么附着」那道外部筛查。
        //    水本来就不收水（那是给外来附着定的规则），但水当然自带水 —— 用 host 去筛会把
        //    自己的天性筛掉，结果就是「冰打水面不结冰」。
        //    用现成的容器写入，不再回头 host.container()（否则递归）。
        ElementalAttachmentHelper.attachInternalTo(container, null, aura,
                AttachmentSource.ENVIRONMENTAL, AttachmentProfile.permanent(BASE_QUANTITY));
    }

    /** 这个方块现在有没有自带元素（只读，用于迁移判断与诊断）。 */
    public static boolean hasAlive(StatusContainer container, GenshinElement element) {
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (inst instanceof ElementalAttachmentInstance ea && ea.getElement() == element) {
                return true;
            }
        }
        return false;
    }
}
