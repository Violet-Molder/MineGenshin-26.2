package com.linweiyun.genshin.core.system.about.block;

import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.system.about.host.BlockHost;
import com.linweiyun.genshin.core.status.StatusInstance;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * <b>方块表现迁移注册表</b> —— 「元素挂上之后，方块该变成什么样」的登记处。
 *
 * <p>和 {@link BlockElementRules}（能不能被附着）成对：新增一种与元素有关的方块，
 * 只需要这两条注册，**不改任何核心代码**：
 * <pre>{@code
 * BlockElementRules.register(state -> state.is(MY_BLOCK), (state, el) -> el == PYRO, "my_block");
 * BlockElementMigrations.register(state -> state.is(MY_BLOCK),
 *         (host, container) -> host.level().setBlock(host.blockPos(), MELTED.get().defaultBlockState(), 3),
 *         "my_block");
 * }</pre>
 *
 * <p>迁移是「读容器里的事实 → 改方块状态」，所以任何来源（玩家攻击、怪物踩踏、相邻火焰）
 * 走的都是同一条路：附着是因，方块状态是果。
 */
public final class BlockElementMigrations {

    /** 一条迁移：给定宿主与它当前的容器，决定方块该变成什么。 */
    @FunctionalInterface
    public interface Migration {
        void apply(BlockHost host, StatusContainer container);
    }

    private record Entry(Predicate<BlockState> matcher, Migration migration, String name) {}

    private static final List<Entry> MIGRATIONS = new ArrayList<>();

    private BlockElementMigrations() {
    }

    /** 注册一条迁移；匹配到的方块在每次附着/推进之后都会跑一次。 */
    public static void register(Predicate<BlockState> matcher, Migration migration, String name) {
        MIGRATIONS.add(new Entry(matcher, migration, name));
    }

    /** 跑所有匹配当前方块状态的迁移。 */
    public static void runAll(BlockHost host, StatusContainer container) {
        BlockState state = host.state();
        for (Entry entry : MIGRATIONS) {
            if (entry.matcher().test(state)) {
                entry.migration().apply(host, container);
            }
        }
    }

    /** 已登记的迁移条数（诊断用）。 */
    public static int size() {
        return MIGRATIONS.size();
    }

    // ==================== 内置：水与冰族 ====================

    static {
        // 水 + 冻元素 → 浮冰
        register(state -> state.is(Blocks.WATER),
                (host, container) -> {
                    if (hasElement(container, ModElements.FROZEN.get())) {
                        // 记住冻之前的水位：化回来要还原成「原来的水」，不是完整水方块
                        BlockElementStore.putWaterLevel(host.level(), host.blockPos(), host.state().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL));
                        host.level().setBlock(host.blockPos(),
                                Blocks.FROSTED_ICE.defaultBlockState(), 3);
                        host.level().scheduleTick(host.blockPos(), Blocks.FROSTED_ICE, 1);
                        // 登记进集中式推进集合（不依赖原版方块 tick 链）
                        BlockElementHelper.trackFrozen(host.level(), host.blockPos());
                    }
                },
                "minecraft:water");

        // 冰族没了冰/冻 → 化回水
        register(BlockElementRules::isIceFamily,
                (host, container) -> {
                    // 只看冻元素：冰是「水为什么结冰」的原因，不是「冰还在不在」的依据。
                    if (!hasAliveFrozen(container)) {
                        BlockElementStore.clear(host.level(), host.blockPos());
                        host.level().setBlock(host.blockPos(),
                                Blocks.WATER.defaultBlockState(), 3);
                    }
                },
                "minecraft:ice_family");
    }

    // ==================== 内部：容器查询（迁移判断用） ====================

    private static boolean hasElement(StatusContainer container, com.linweiyun.genshin.core.element.GenshinElement target) {
        return sumElementQuantity(container, target) > 0f;
    }

    /** 这格身上还有没有「活的冻元素」—— 浮冰存亡的唯一依据（不看冰元素）。 */
    private static boolean hasAliveFrozen(StatusContainer container) {
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (inst instanceof ElementalAttachmentInstance ea
                    && ea.getElement() == ModElements.FROZEN.get()) {
                return true;
            }
        }
        return false;
    }

    private static float sumElementQuantity(StatusContainer container,
                                           com.linweiyun.genshin.core.element.GenshinElement target) {
        float sum = 0f;
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (inst instanceof ElementalAttachmentInstance ea && ea.getElement() == target) {
                sum += ea.getUnit();
            }
        }
        return sum;
    }
}
