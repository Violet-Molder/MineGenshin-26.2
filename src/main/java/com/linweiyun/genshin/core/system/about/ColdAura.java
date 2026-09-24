package com.linweiyun.genshin.core.system.about;

import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.element.ColdElement;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.status.StatusInstance;
import com.linweiyun.genshin.core.system.about.host.ElementalHost;
import com.linweiyun.genshin.core.system.about.host.EntityHost;
import net.minecraft.world.entity.LivingEntity;

/**
 * <b>寒元素的伴随机制</b> —— 「寒往往伴随冰/冻存在」这句规则的落点。
 *
 * <pre>
 *   容器里有活的 冰 或 冻  → 补一条寒（走宿主筛查，冰史莱姆在这一步被挡住）
 *   冰和冻都没了           → 寒一起走
 *   然后按「有寒 + 有冰族 / 有冻」应用或撤销减速与禁 AI
 * </pre>
 *
 * <h2>为什么放在每 tick，而不是挂 attach 的 onAttach/onDetach 钩子</h2>
 * 这是两份实现里更可靠的那一份，原因是硬事实：
 * <ul>
 *   <li>{@code ElementalAttachmentHelper.doAttach} 里 {@code host.onElementAttached(element)} 是在
 *       {@code container.add(newInst)} <b>之前</b>调的；</li>
 *   <li>{@code StatusContainer.remove/removeFirst} 是先 {@code inst.onRemove()} 再把实例移出列表。</li>
 * </ul>
 * 也就是说钩子里读容器一定读到<b>旧状态</b>：冻附着上来时只会看到冰（该禁 AI 的只减速），
 * 寒被移除时又以为寒还在（修饰符撤不掉 → 永久减速）。
 * 而 {@code StatusTickHandler} 本来就每 tick 推进每个生物的状态容器，在那里多算一次
 * 「容器里有什么」既避开了时序陷阱，又只有一份判据（不另存「谁被减速了」这类影子状态）。
 *
 * <p>代价是效果最多晚 1 tick 生效/撤销 —— 对减速与禁 AI 没有可感差异。
 *
 * <p>注意本类<b>只对非玩家生物</b>做事：玩家与出战角色的减速由
 * {@code CharacterChillHandler} 按冰/冻附着逐 tick 重算（角色宿主不挂寒），方块宿主没有实体、
 * 寒也无效果可言 —— 这些都记在 {@link ColdElement} 的注释里。
 */
public final class ColdAura {

    /** 寒的量：常驻（不衰减），被反应消耗也好、被清掉也好，下次同步都会补回。 */
    private static final AttachmentProfile PROFILE = AttachmentProfile.permanent(1.0f);

    private ColdAura() {
    }

    /**
     * 每 tick 同步一次寒，并应用效果。
     *
     * @return true 表示此刻「有寒且有冻」—— 调用方据此锁住位移（冻结期间不为水流/浮力积累速度）
     */
    public static boolean tick(LivingEntity entity, StatusContainer container) {
        if (entity == null || container == null || container.isEmpty()) {
            return false;
        }

        boolean cryoFamily = false;
        boolean frozen = false;
        boolean cold = false;
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (!(inst instanceof ElementalAttachmentInstance ea)) continue;
            GenshinElement element = ea.getElement();
            if (element == null) continue;
            if (element == ModElements.FROZEN.get()) {
                frozen = true;
                cryoFamily = true;
            } else if (element == ModElements.CYRO.get()) {
                cryoFamily = true;
            } else if (element == ModElements.COLD.get()) {
                cold = true;
            }
        }

        // 既没有冰/冻也没有寒 —— 绝大多数生物每 tick 都走这条，什么都别做
        if (!cryoFamily && !cold) {
            return false;
        }

        if (cryoFamily && !cold) {
            // 补寒。走宿主筛查（attachInternal 只免掉「反应」，筛查照问）：
            // 元素生物在这里被挡下 —— 挂冰但不受冰影响，正是靠「不收寒」表达的。
            ElementalHost host = EntityHost.of(entity);
            cold = ElementalAttachmentHelper
                    .attachInternal(host, ModElements.COLD.get(), AttachmentSource.SPECIAL, PROFILE)
                    .attached();
        } else if (!cryoFamily && cold) {
            // 冰和冻都没了 → 寒一起走（这里不在容器回调里改列表，不存在迭代中改动的风险）
            container.removeFirst(ColdAura::isCold);
            cold = false;
        }

        ColdElement.applyEffects(entity, cold, cryoFamily, frozen);
        return cold && frozen;
    }

    private static boolean isCold(StatusInstance inst) {
        return inst instanceof ElementalAttachmentInstance ea
                && ea.getElement() == ModElements.COLD.get();
    }
}
