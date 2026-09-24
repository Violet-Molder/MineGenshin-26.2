package com.linweiyun.genshin.core.system.about.host;

import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.system.about.AttachmentProfile;
import com.linweiyun.genshin.core.system.about.AttachmentSource;
import com.linweiyun.genshin.core.system.about.block.BlockElementRules;
import com.linweiyun.genshin.core.system.about.block.BlockElementStore;
import com.linweiyun.genshin.core.system.about.block.BlockSelfAura;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * <b>方块宿主</b> —— {@link ElementalHost} 在「一个坐标处的方块」上的实现。
 *
 * <p>它的出现意味着方块不再是一条支线：附着写进 {@link BlockElementStore} 提供的容器、
 * 能不能附着由 {@link BlockElementRules} 的规则表回答、之后的反应与实体侧走同一个
 * {@code ElementalReactionManager}。{@code BlockElementHelper} 退化成
 * 「宿主适配 + 反应结果驱动的方块状态迁移」。
 *
 * <p>注意：本宿主刻意拿的是 {@link BlockState}（不是 {@code BlockEntity}）——
 * 绝大多数可附着方块（水、冰）根本没有方块实体。
 */
public final class BlockHost implements ElementalHost {

    private final ServerLevel level;
    private final BlockPos pos;

    private BlockHost(ServerLevel level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    /** 包一个方块坐标；{@code level == null} 时返回 {@code null}。 */
    @Nullable
    public static BlockHost of(@Nullable ServerLevel level, @Nullable BlockPos pos) {
        return (level == null || pos == null) ? null : new BlockHost(level, pos);
    }

    @Override
    public boolean isValid() {
        return level.isLoaded(pos);
    }

    @Override
    public StatusContainer container() {
        if (!isValid()) {
            return null;
        }
        StatusContainer container = BlockElementStore.container(level, pos);
        // 环境自附着：这个方块「本来就是水/冰」——反应要靠它当先手，缺了反应就无从发生
        BlockSelfAura.ensure(this, container, state());
        return container;
    }

    /**
     * <b>只读</b>取容器：不建、不补自附着、不落盘。
     *
     * <p>和 {@link #container()} 分开是刻意的：
     * <ul>
     *   <li>{@link #container()} 是<b>写路径</b>——会现建容器、会补自带元素（水/冰的天性）；</li>
     *   <li>本方法是<b>读路径</b>——只看现有事实，扫一片石头不会给每格造一份空容器，
     *       也不会在查询时顺手往方块身上写东西。</li>
     * </ul>
     * 推进衰减、判断状态这类高频只读逻辑一律用它。
     */
    @Nullable
    public StatusContainer peekContainer() {
        return isValid() ? BlockElementStore.peek(level, pos) : null;
    }

    @Override
    public String hostKey() {
        return "block:" + level.dimension().identifier() + "@" + pos.asLong();
    }

    @Override
    public boolean acceptsElement(GenshinElement element, AttachmentSource source,
                                  AttachmentProfile profile) {
        return BlockElementRules.accepts(state(), element);
    }

    /**
     * 方块侧元素钩子 —— 目前是空实现。
     *
     * <p>方块吃元素之后的「样子变化」（水结冰、冰化水）不由元素本体决定，
     * 而是由状态迁移消费者统一读容器后决定（见 {@code BlockElementHelper}）：
     * 方块的状态是它的表现，元素附着是原因，两者不该互相写死。
     */
    @Override
    public void onElementAttached(GenshinElement element) {
    }

    @Override
    public void onElementDetached(GenshinElement element) {
    }

    /** 当前坐标上的方块状态。 */
    public BlockState state() {
        return level.getBlockState(pos);
    }

    /** 把容器改动写回 Chunk 数据（相当于实体宿主的 {@code setData}）。 */
    public void commit(StatusContainer container) {
        BlockElementStore.commit(level, pos, container);
    }

    @Override
    public ServerLevel level() {
        return level;
    }

    @Override
    public BlockPos blockPos() {
        return pos;
    }

    @Override
    public String toString() {
        return hostKey();
    }
}
