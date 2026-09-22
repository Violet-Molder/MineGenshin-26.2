package com.linweiyun.genshin.content.effect.character.impl;

import com.linweiyun.genshin.content.effect.character.CharacterEffectHelper;
import com.linweiyun.genshin.content.effect.character.CharacterEffectInstance;
import com.linweiyun.genshin.content.effect.character.ICharacterEffect;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import com.linweiyun.genshin.core.system.registry.register.ModCharacterEffects;
import net.minecraft.world.entity.player.Player;

/**
 * 漩流颂歌（{@code whirlflow_hymn}）武器被动的两个效果实例。
 *
 * <h2>「告真的蜜酿」拆成两个效果</h2>
 * 一次治疗要同时给<b>两个不同的人</b>东西，所以用两个效果承载：
 * <ul>
 *   <li>{@link Mead} —— 挂在<b>装备者</b>身上：每层生命值上限 +4%（最多 3 层、10 秒）；</li>
 *   <li>{@link MeadAtk} —— 挂在<b>当前场上角色</b>身上（可能不是装备者）：
 *       攻击力 = 每层值 × 层数，每层值 = 装备者「上一层生命值上限」超出 40000 的部分每 1000 点 → +0.4%
 *       （每层最多 8%）。生命值上限那 4%/层会喂进这个换算，所以层数越高每层值越大（见 {@link #applyAtkBonus}）。</li>
 * </ul>
 * 两边的层数各自记账（{@link #STACK_KEY}），符合「按受益人各自计层」的口径。
 *
 * <h2>1.75 倍率怎么同步</h2>
 * 附近的队伍成员触发冻结 / 星扩散时，由
 * {@code WhirlflowHymn#markReactionTriggers} 把窗口到期刻写进
 * {@code PGCharacterData#setWhirlflowReactionWindowEnd}，并调用
 * {@link #refreshMeadEffects} 立刻把倍率算进属性；窗口自然过期的降档由
 * {@link Mead#onEffectTick} / {@link MeadAtk#onEffectTick} 每刻重算兜底
 * （和仓库里其它有时长效果的 tick 逻辑一致）。
 *
 * <p>所有数值见 {@code CHARACTER_SYSTEM.md} 与武器文案，来源都在常量注释里写明。
 */
public final class WhirlflowHymnEffects {

    private WhirlflowHymnEffects() {
    }

    /** 层数存在效果实例的 intData 里（和沃雅妮莎的领唱/重唱同一做法）。 */
    public static final String STACK_KEY = "whirlflow_hymn_mead_stacks";
    /** 攻击力那一边要记住「蜜酿是谁酿的」—— 存装备者的角色 UUID。 */
    public static final String OWNER_KEY = "whirlflow_hymn_mead_owner";
    /** 「不算蜜酿」的装备者生命值上限：第一次换算时记下来，层数变化时用它重算（见 {@link #applyAtkBonus}）。 */
    public static final String BASE_HP_KEY = "whirlflow_hymn_mead_base_hp";

    /** 层数上限：文案「最多 3 层」。 */
    public static final int MAX_STACKS = 3;
    /** 持续时间：文案「10 秒」。 */
    public static final int DURATION_TICKS = 10 * 20;

    /** 每层生命值上限 +4%（文案「每层生命值上限提升 4%」）。 */
    public static final double HP_PERCENT_PER_STACK = 0.04;
    /** 攻击力换算的门槛：文案「超过 40000 点的部分」。 */
    public static final double HP_THRESHOLD = 40000.0;
    /** 每 1000 点生命值上限算一档。 */
    public static final double HP_PER_STEP = 1000.0;
    /** 每档攻击力 +0.4%（文案「每 1000 点 → 0.4%」）。 */
    public static final double ATK_PERCENT_PER_STEP = 0.004;
    /** 攻击力加成上限 8%（文案「至多 8%」；按<b>每层</b>算，3 层最多 24%，见 {@link #applyAtkBonus}）。 */
    public static final double ATK_PERCENT_CAP = 0.08;
    /** 冻结 / 星扩散触发后的强化倍率：文案「提升 75%」→ ×1.75。 */
    public static final double REACTION_AMPLIFY = 1.75;
    /** 强化窗口：文案「5 秒内」。 */
    public static final int REACTION_WINDOW_TICKS = 5 * 20;

    private static final String HP_SOURCE = "whirlflow_hymn_mead_hp";
    private static final String ATK_SOURCE = "whirlflow_hymn_mead_atk";

    // ==================== 装备者：生命值上限层数 ====================

    /** 挂在装备者身上的「告真的蜜酿」：每层生命值上限 +4%。 */
    public static class Mead implements ICharacterEffect {

        @Override
        public void onEffectAdded(Player holder, PGCharacter character, CharacterEffectInstance instance) {
            applyHpBonus(character, instance);
        }

        @Override
        public void onEffectOverride(Player holder, PGCharacter character,
                                     CharacterEffectInstance existingInstance,
                                     CharacterEffectInstance newInstance) {
            // 重新触发：刷新时长（10 秒）并加一层，封顶 3 层
            newInstance.setDuration(DURATION_TICKS);
            newInstance.setIntData(STACK_KEY,
                    Math.min(MAX_STACKS, existingInstance.getIntData(STACK_KEY) + 1));
            applyHpBonus(character, newInstance);
        }

        @Override
        public boolean onEffectTick(Player holder, PGCharacter character, CharacterEffectInstance instance) {
            // 窗口开 / 关都要把 1.75 倍率同步到属性上
            applyHpBonus(character, instance);
            return ICharacterEffect.super.onEffectTick(holder, character, instance);
        }

        @Override
        public void onEffectRemoved(Player holder, PGCharacter character, CharacterEffectInstance instance) {
            character.getData().removeAttributeModifier(ModAttributes.MAX_HP.value(), HP_SOURCE);
        }
    }

    // ==================== 当前场上角色：攻击力 ====================

    /** 挂在当前场上角色身上的「告真的蜜酿」攻击力部分。 */
    public static class MeadAtk implements ICharacterEffect {

        @Override
        public void onEffectAdded(Player holder, PGCharacter character, CharacterEffectInstance instance) {
            applyAtkBonus(holder, character, instance);
        }

        @Override
        public void onEffectOverride(Player holder, PGCharacter character,
                                     CharacterEffectInstance existingInstance,
                                     CharacterEffectInstance newInstance) {
            newInstance.setDuration(DURATION_TICKS);
            newInstance.setIntData(STACK_KEY,
                    Math.min(MAX_STACKS, existingInstance.getIntData(STACK_KEY) + 1));
            // 装备者以这一次带过来的为准（换人时蜜酿归新装备者）
            newInstance.setIntData(OWNER_KEY, newInstance.getIntData(OWNER_KEY) != 0
                    ? newInstance.getIntData(OWNER_KEY)
                    : existingInstance.getIntData(OWNER_KEY));
            // 「不算蜜酿的生命值上限」是这套蜜酿的基准，加层不能重新采样
            if (newInstance.getIntData(BASE_HP_KEY) <= 0) {
                newInstance.setIntData(BASE_HP_KEY, existingInstance.getIntData(BASE_HP_KEY));
            }
            applyAtkBonus(holder, character, newInstance);
        }

        @Override
        public boolean onEffectTick(Player holder, PGCharacter character, CharacterEffectInstance instance) {
            applyAtkBonus(holder, character, instance);
            return ICharacterEffect.super.onEffectTick(holder, character, instance);
        }

        @Override
        public void onEffectRemoved(Player holder, PGCharacter character, CharacterEffectInstance instance) {
            character.getData().removeAttributeModifier(ModAttributes.ATK.value(), ATK_SOURCE);
        }
    }

    // ==================== 触发入口 ====================

    /**
     * 装备者完成了一次治疗（服务端，由 {@code WeaponItem#notifyHeal} 分发过来）。
     *
     * @param equipper 治疗者（= 装备者）
     * @param onField  当前场上角色（可能不是装备者；没有就传 null）
     */
    public static void onEquipperHeal(Player player, PGCharacter equipper, PGCharacter onField) {
        if (player == null || player.level().isClientSide() || equipper == null) return;

        addMead(player, equipper, ModCharacterEffects.WHIRLFLOW_MEAD_EFFECT.get(),
                equipper.getCharacterUUID());
        if (onField != null) {
            addMead(player, onField, ModCharacterEffects.WHIRLFLOW_MEAD_ATK_EFFECT.get(),
                    equipper.getCharacterUUID());
        }
    }

    /** 记录「附近队友刚触发冻结 / 星扩散」的 5 秒窗口（绝对到期刻）。 */
    public static void markReactionWindow(PGCharacter equipper, long gameTime) {
        equipper.getData().setWhirlflowReactionWindowEnd(gameTime + REACTION_WINDOW_TICKS);
    }

    /**
     * 把当前窗口状态立刻重算到蜜酿的属性上（触发的那一刻就吃到 1.75 倍）。
     */
    public static void refreshMeadEffects(Player player, PGCharacter equipper) {
        if (player == null || equipper == null) return;

        CharacterEffectInstance mead =
                CharacterEffectHelper.getEffectInstance(equipper.getData(),
                        ModCharacterEffects.WHIRLFLOW_MEAD_EFFECT.get());
        if (mead != null) {
            applyHpBonus(equipper, mead);
        }

        PlayerCharactersAttachment attachment = player.getData(
                AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        if (attachment == null) return;
        for (int i = 0; i < 4; i++) {
            PGCharacter member = attachment.getPartyCharacter(i);
            if (member == null) continue;
            CharacterEffectInstance atk =
                    CharacterEffectHelper.getEffectInstance(member.getData(),
                            ModCharacterEffects.WHIRLFLOW_MEAD_ATK_EFFECT.get());
            if (atk != null && atk.getIntData(OWNER_KEY) == equipper.getCharacterUUID()) {
                applyAtkBonus(player, member, atk);
            }
        }
    }

    // ==================== 工具 ====================

    /** 新增一层蜜酿（已有同效果就走 {@code onEffectOverride} 的加层逻辑）。 */
    private static void addMead(Player player, PGCharacter recipient, ICharacterEffect effect, int ownerUuid) {
        if (effect == null) return;
        CharacterEffectInstance instance = new CharacterEffectInstance(effect, DURATION_TICKS, 0, false);
        instance.setIntData(STACK_KEY, 1);
        instance.setIntData(OWNER_KEY, ownerUuid);
        CharacterEffectHelper.addEffect(player, recipient, instance);
    }

    /** 把「层数 × 4% × 强化倍率」写进装备者生命值上限的临时百分比修饰符。 */
    private static void applyHpBonus(PGCharacter character, CharacterEffectInstance instance) {
        int stacks = Math.max(1, Math.min(MAX_STACKS, instance.getIntData(STACK_KEY)));
        double value = HP_PERCENT_PER_STACK * stacks * reactionMultiplier(character);
        character.getData().setAttributeTempPercentModifier(ModAttributes.MAX_HP.value(), HP_SOURCE, value);
    }

    /**
     * 把「每层攻击力% × 层数 × 强化倍率」写进受益人的攻击力。
     *
     * <h2>为什么每层的值会随层数变，而且最后还要再乘层数</h2>
     * 蜜酿的生命值上限加成<b>会反过来喂给攻击力换算</b>，所以每层的攻击力值是「按上一层那一刻的生命值上限」
     * 现算出来的，再乘层数（口径由需求方给定，例子：基础 5 万生命值、2 层）
     * <pre>
     *   1 层：生命值上限 50000 → (50000-40000)/1000 × 0.4% = 4.0%/层 × 1 = <b>4.0%</b>
     *   2 层：上一层生命值上限 52000 → (52000-40000)/1000 × 0.4% = 4.8%/层 × 2 = <b>9.6%</b>
     * </pre>
     * 8% 的上限是<b>每层</b>的上限（所以 3 层理论最多 24%），否则 2 层就会被 8% 卡住、和上面的例子矛盾。
     */
    private static void applyAtkBonus(Player holder, PGCharacter recipient, CharacterEffectInstance instance) {
        PGCharacter equipper = findCharacter(holder, instance.getIntData(OWNER_KEY));
        if (equipper == null) return;

        int stacks = Math.max(1, Math.min(MAX_STACKS, instance.getIntData(STACK_KEY)));
        double maxHp = equipper.getData().getAttributeTotalValue(ModAttributes.MAX_HP.value());

        // 「不算蜜酿」的生命值上限：第一次换算时采样（那一刻正好是 stacks 层，除回去即可），
        // 之后一直用它当基准 —— 不然加层会把上一层的加成又吃一次，越叠越离谱。
        int baseHp = instance.getIntData(BASE_HP_KEY);
        if (baseHp <= 0) {
            baseHp = (int) Math.round(maxHp / (1.0 + HP_PERCENT_PER_STACK * stacks));
            instance.setIntData(BASE_HP_KEY, baseHp);
        }

        double hpForAtk = baseHp * (1.0 + HP_PERCENT_PER_STACK * (stacks - 1));
        double over = Math.max(0.0, hpForAtk - HP_THRESHOLD);
        double perStack = Math.min(ATK_PERCENT_CAP, (over / HP_PER_STEP) * ATK_PERCENT_PER_STEP);
        double percent = perStack * stacks * reactionMultiplier(equipper);

        recipient.getData().removeAttributeModifier(ModAttributes.ATK.value(), ATK_SOURCE);
        if (percent > 0) {
            recipient.getData().addAttributeTempPercentModifier(
                    ModAttributes.ATK.value(), ATK_SOURCE, percent);
        }
    }

    /** 这个角色现在是否处在「冻结 / 星扩散」后的 5 秒强化窗口内。 */
    private static double reactionMultiplier(PGCharacter character) {
        Player owner = character.getData().getOwnerPlayer();
        if (owner == null) return 1.0;
        return character.getData().isWhirlflowReactionWindowActive(owner.level().getGameTime())
                ? REACTION_AMPLIFY : 1.0;
    }

    private static PGCharacter findCharacter(Player player, int characterUUID) {
        if (player == null || characterUUID == 0) return null;
        PlayerCharactersAttachment attachment = player.getData(
                AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        return attachment == null ? null : attachment.getCharacterByUUID(characterUUID);
    }
}