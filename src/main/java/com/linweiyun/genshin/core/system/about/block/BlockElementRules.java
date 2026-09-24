package com.linweiyun.genshin.core.system.about.block;

import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.element.ModElements;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * <b>「这个方块能被什么附着」的注册表</b> —— 取代原来写死在 {@code BlockElementHelper} 里的
 * {@code if (state.is(Blocks.WATER)) ...} 判定链。
 *
 * <p>新增一种「与元素有关的方块」时，不再改这里的判断逻辑，而是注册一条规则：
 * <pre>{@code
 * BlockElementRules.register(
 *         state -> state.is(MyBlocks.FIRE_CRYSTAL.get()),
 *         (state, element) -> element == ModElements.PYRO.get());
 * }</pre>
 *
 * <p>规则按注册顺序匹配，先命中先返回。内置的原版规则在静态块里注册。
 */
public final class BlockElementRules {

    /** 一条规则：给定方块状态与来袭元素，回答收不收。 */
    @FunctionalInterface
    public interface Rule {
        boolean accepts(BlockState state, GenshinElement element);
    }

    private record Entry(Predicate<BlockState> matcher, Rule rule, String name) {}

    private static final List<Entry> RULES = new ArrayList<>();

    private BlockElementRules() {
    }

    /**
     * 注册一条规则。
     *
     * @param matcher 匹配哪些方块状态
     * @param rule    匹配上之后，能不能接收这个元素
     * @param name    规则名（仅用于排查）
     */
    public static void register(Predicate<BlockState> matcher, Rule rule, String name) {
        RULES.add(new Entry(matcher, rule, name));
    }

    /** 这个方块状态收不收这个元素。没有任何规则命中 → 不收。 */
    public static boolean accepts(BlockState state, GenshinElement element) {
        if (state == null || element == null) {
            return false;
        }
        for (Entry entry : RULES) {
            if (entry.matcher().test(state)) {
                return entry.rule().accepts(state, element);
            }
        }
        return false;
    }

    /** 已登记的规则条数（诊断用）。 */
    public static int size() {
        return RULES.size();
    }

    // ==================== 内置的原版方块规则 ====================

    static {
        // 水：收冰（冰遇水 → 冻结反应 → 浮冰 + 冻元素）、也收冻（冻元素直接落到水上同样成冰）
        //
        // ⚠️ 一定要收 FROZEN：冻结反应的生成物就是冻元素，而生成物走的是同一个宿主筛查
        // （反应内部的 attachInternal 也会问宿主收不收）。只收 CYRO 的话，水会「先允许冰挂上来、
        // 再在生成冻元素那一步把自己拒掉」，结果就是反应发生了、容器里却没有冻元素，
        // 迁移表看不到 FROZEN，水永远结不成冰。
        // 只认「完整水源」（level=0）：流动水不该被一发冰冻成浮冰。
                // 只有「完整水源」（level=0）参与元素体系：流动水既不自带水、也不收冰，永远不会结冰
        register(state -> isSourceWater(state),
                (state, element) -> element == ModElements.CYRO.get()
                        || element == ModElements.FROZEN.get(),
                "minecraft:water");

        // 冰族（浮冰/冰/浮冰砖/蓝冰）：收火（融化）、收冰（叠冰）、收冻（刷新冻元素）
        register(state -> isIceFamily(state),
                (state, element) -> element == ModElements.PYRO.get()
                        || element == ModElements.CYRO.get()
                        || element == ModElements.FROZEN.get(),
                "minecraft:ice_family");
    }

    /** 浮冰 / 冰 / 浮冰砖 / 蓝冰 —— 冰族方块。 */
    /** 完整水源（level=0）：水方块、且不是流动水。 */
    public static boolean isSourceWater(BlockState state) {
        return state.is(Blocks.WATER)
                && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL) == 0;
    }

    public static boolean isIceFamily(BlockState state) {
        return state.is(Blocks.FROSTED_ICE) || state.is(Blocks.ICE)
                || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE);
    }

    /**
     * <b>这个方块自带什么元素</b>（环境自附着）—— 不衰减、一直挂在自己身上，
     * 被反应消耗完后由 {@code BlockSelfAura} 周期补回。
     *
     * <p>这是「冰相当于一直给自己挂冰、水一直给自己挂水」的落点：
     * 冰打水面之所以能冻结，是因为水这边有 <b>先手</b> 的水元素；
     * 火打冰之所以能融化，是因为冰这边有先手冰元素。少了这层，反应就无从发生，
     * 只能靠「方块状态对不上就改状态」的粗规则去凑表现 —— 那就是内核不同。
     *
     * @return 自带元素；没有则 {@code null}
     */
    public static GenshinElement selfAura(BlockState state) {
        if (isSourceWater(state)) {
            return ModElements.HYDRO.get();
        }
        // 浮冰不额外挂冰气场：它的元素是冻结反应写进去的 FROZEN；
        // 给它挂一份永久 CYRO 会让「冰族没有冰/冻就化水」这条融化规则永远不成立 → 永不化。
        if (state.is(Blocks.FROSTED_ICE)) {
            return null;
        }
        if (isIceFamily(state)) {
            return ModElements.CYRO.get();
        }
        return null;
    }
}
