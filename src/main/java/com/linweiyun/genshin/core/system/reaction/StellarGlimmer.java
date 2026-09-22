package com.linweiyun.genshin.core.system.reaction;

import com.linweiyun.genshin.content.effect.character.CharacterEffectContainer;
import com.linweiyun.genshin.content.effect.character.impl.RadianceStellarConduceEffect;
import com.linweiyun.genshin.content.effect.character.impl.RadianceStellarSwirlEffect;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.IStellarHousehold;
import com.linweiyun.genshin.core.character.IStellarStateHolder;
import com.linweiyun.genshin.core.character.PGCharacter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 辉映·星烁的查询入口 —— 「这个角色现在处于哪个分支」「星烁加成了多少」。
 *
 * <pre>
 * 辉映·星烁（统称）
 *   ├─ 辉映·星超导（优先级高）
 *   └─ 辉映·星扩散（优先级低）
 * </pre>
 *
 * <p>同一角色身上两者互斥（互斥规则写在两个 effect 类里，见
 * {@link RadianceStellarConduceEffect} / {@link RadianceStellarSwirlEffect}），
 * 这里的 {@link #branchOf(PGCharacter)} 只是按优先级读出「现在是哪一个」。
 */
public final class StellarGlimmer {

    private StellarGlimmer() {
    }

    /** 当前身上的星烁分支；两个都没有就是 {@code null}。星超导优先。 */
    @Nullable
    public static StellarGlimmerBranch branchOf(@Nullable PGCharacter character) {
        if (character == null) {
            return null;
        }
        CharacterEffectContainer container = character.getData().getEffectContainer();
        if (container.hasEffectOfType(RadianceStellarConduceEffect.class)) {
            return StellarGlimmerBranch.CONDUCE;
        }
        if (container.hasEffectOfType(RadianceStellarSwirlEffect.class)) {
            return StellarGlimmerBranch.SWIRL;
        }
        return null;
    }

    /** 身上是不是这个分支。 */
    public static boolean has(@Nullable PGCharacter character, StellarGlimmerBranch branch) {
        return branchOf(character) == branch;
    }

    /** 快捷：是否处于辉映·星扩散（薇斯娜的天赋判定用它 —— 她的加成只认星扩散）。 */
    public static boolean hasSwirl(@Nullable PGCharacter character) {
        return has(character, StellarGlimmerBranch.SWIRL);
    }

    /** 快捷：是否处于辉映·星超导。 */
    public static boolean hasConduce(@Nullable PGCharacter character) {
        return has(character, StellarGlimmerBranch.CONDUCE);
    }

    /**
     * 星烁反应的伤害加成总和（反应加成区里那一项，和元素精通加算）。
     *
     * <p>来源两部分相加：
     * <ol>
     *   <li><b>效果/ buff</b>：{@code ICharacterEffect.getStellarGlimmerBonus(分支)} ——
     *       写「星烁加成」的两个分支都给，只写一个的只给那一个；</li>
     *   <li><b>角色自己</b>：{@code PGCharacter.getStellarGlimmerBonus(分支)} ——
     *       天赋类加成（同样是按分支给的）。</li>
     * </ol>
     */
    public static float bonusOf(@Nullable PGCharacter character, StellarGlimmerBranch branch) {
        if (character == null || branch == null) {
            return 0f;
        }
        float total = character.getData().getEffectContainer().getTotalStellarGlimmerBonus(branch);
        total += character.getStellarGlimmerBonus(branch);
        return total;
    }

    /**
     * 全队「星扩散户口」—— 谁的户口负责星扩散、能转化、给多少基础伤害提升。
     *
     * <p><b>户口 = 转化 + 体系加成</b>（写在同一个天赋里），两者是绑定的；
     * 和「能进入星扩散状态」是两回事（后者见 {@link IStellarStateHolder}）。
     *
     * @return 队伍里星扩散户口（取基础加成最高的那一份）；没有就返回 null
     */
    @Nullable
    public static IStellarHousehold.StellarHousehold swirlHousehold(@Nullable Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        IStellarHousehold.StellarHousehold best = null;
        for (Player player : serverLevel.players()) {
            PlayerCharactersAttachment attachment = player.getData(
                    AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            for (PGCharacter member : attachment.getOwnedCharacters()) {
                if (member instanceof IStellarHousehold provider) {
                    IStellarHousehold.StellarHousehold household = provider.stellarHousehold();
                    if (household != null && household.branch() == StellarGlimmerBranch.SWIRL
                            && (best == null || household.baseBonusMult() > best.baseBonusMult())) {
                        best = household;
                    }
                }
            }
        }
        return best;
    }

    /**
     * 全队「星扩散反应基础伤害提升」—— 就是星扩散户口给的那一份（没有户口就是 0）。
     *
     * <p>加在星扩散的基础区上（{@code 基础区 × (1 + 基础倍率提升)}）。
     */
    public static float swirlBaseBonusMult(@Nullable Level level) {
        IStellarHousehold.StellarHousehold household = swirlHousehold(level);
        return household == null ? 0f : household.baseBonusMult();
    }
}
