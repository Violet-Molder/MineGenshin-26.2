package com.linweiyun.genshin.core.character.sword.vesna;

import com.linweiyun.genshin.config.character.ShenheAttributeConfig;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.character.IStellarHousehold;
import com.linweiyun.genshin.core.character.IStellarStateHolder;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.character.sword.SwordCharacter;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.system.reaction.StellarGlimmerBranch;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterRenderData;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterRenderRepository;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import com.linweiyun.genshin.enums.CharacterAscendAttribute;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.mojang.logging.LogUtils;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static net.minecraft.network.chat.Component.literal;
import static net.minecraft.network.chat.Component.translatable;

@Getter
public class Vesna extends SwordCharacter implements IStellarHousehold, IStellarStateHolder {
    public static final String ID = "vesna";    public static final int UID = 115001;
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final int WIND_RIDER_DURATION_TICKS = 15 * 20;
    /** 每层剑气 = 6 点能量 */
    public static final float ENERGY_PER_QIQI = 6f;
    /** 翔风剑一次消耗 1 层剑气 */
    public static final float SPECIAL_SKILL_ENERGY_COST = ENERGY_PER_QIQI;
    /** 开 E 获得两层剑气 */
    public static final float WIND_RIDER_ENTER_ENERGY = ENERGY_PER_QIQI * 2;
    /** 一次巡风列装最多能放几次最高境界（三阶）翔风剑（0 命）。命座 1 会 +1。 */
    public static final int BASE_LV3_USES_IN_WINDRIDER = 3;

    @DescSynced @Persisted(key = "vesnaEnergy")
    protected float vesnaEnergy;
    @DescSynced @Persisted(key = "vesnaMaxEnergy")
    protected float vesnaMaxEnergy;
    @DescSynced @Persisted(key = "windriderActive")
    protected boolean windriderActive;
    @DescSynced @Persisted(key = "windriderRemainingTicks")
    protected int windriderRemainingTicks;

    /** 下一次施放的翔风剑阶级（1~3）。talent 读取它当本次施放等级。 */
    @DescSynced @Persisted(key = "xiangfengJianLevel")
    protected int xiangfengJianLevel = 0;

    /** 本次巡风列装已施放的三阶次数，达到 {@link #maxLv3UsesInWindrider()} 次退出模式。 */
    @DescSynced @Persisted(key = "lv3UsesInWindrider")
    protected int lv3UsesInWindrider = 0;

    /**
     * 这一次施放是不是「进入巡风列装」的那一下。
     *
     * <p>在<b>触发那一刻</b>定下来（{@code VesnaTalent.onCastStart}），伤害点只读它。
     * 为什么不能等到伤害点再判断「模式开没开」：模式现在触发时就开了，
     * 到伤害点再看会把入门那一刀误当成翔风剑（扣剑气、打错倍率、推进阶级）。
     *
     * <p>纯服务端运行时状态，故意不加 {@code @Persisted/@DescSynced}。
     */
    protected boolean entryCast = false;

    // ==================== 整肃（大权区的来源） ====================

    /** 整肃最多叠几层。 */
    public static final int MAX_DECREE_STACKS = 6;
    /** 每层持续 20 秒，<b>每层独立计时</b>。 */
    public static final int DECREE_DURATION_TICKS = 20 * 20;
    /** 每层给大权区的加成（+10%）。 */
    public static final float DECREE_BONUS_PER_STACK = 0.10f;

    /**
     * 整肃：每一层自己的剩余刻数，{@code 0} = 这一层不存在。
     *
     * <p>用「每层一个计时器」而不是一个总时长 —— 文案写的是
     * 「每层独立计算持续时间」，一个总时长做不到「先叠的那层先掉」。
     */
    @DescSynced @Persisted(key = "decreeTicks")
    protected int[] decreeTicks = new int[MAX_DECREE_STACKS];

    /** 当前有几层整肃。 */
    public int decreeStacks() {
        int stacks = 0;
        for (int ticks : decreeTicks) {
            if (ticks > 0) {
                stacks++;
            }
        }
        return stacks;
    }

    /**
     * 叠一层整肃。
     *
     * <p>6 层是一个<b>队列</b>：满 6 层时第 7 次会<b>挤掉最早的那一层</b>
     * （最早 = 剩余刻数最少的那格），而不是叠不上去。
     */
    public void grantDecree() {
        int slot = 0;
        int lowest = Integer.MAX_VALUE;
        for (int i = 0; i < decreeTicks.length; i++) {
            if (decreeTicks[i] <= 0) {
                slot = i;                       // 有空位就用空位
                lowest = 0;
                break;
            }
            if (decreeTicks[i] < lowest) {
                lowest = decreeTicks[i];
                slot = i;                       // 满了就挤掉最早的那层
            }
        }
        decreeTicks[slot] = DECREE_DURATION_TICKS;
        LOGGER.info("[整肃] 叠 1 层 → 当前 {} 层（大权 +{}%）",
                decreeStacks(), (int) (getSovereigntyBonus() * 100));
        syncSkillState();
    }

    /** 清空所有整肃层数（进巡风列装 / 退场时调用；翔风剑不清）。 */
    public void clearDecree() {
        int before = decreeStacks();
        if (before <= 0) {
            return;
        }
        java.util.Arrays.fill(decreeTicks, 0);
        LOGGER.info("[整肃] 清空（原 {} 层）", before);
        syncSkillState();
    }

    /** 每刻递减（服务端）。 */
    private void tickDecree() {
        boolean changed = false;
        for (int i = 0; i < decreeTicks.length; i++) {
            if (decreeTicks[i] > 0 && --decreeTicks[i] == 0) {
                changed = true;
            }
        }
        if (changed) {
            syncSkillState();
        }
    }

    /**
     * 大权区加成 = 整肃层数 × 10%。
     *
     * <p>只作用在「灵剑」那几段（翔风剑二阶第二段 / 三阶两段 / 大招）——
     * 具体哪几段由天赋造伤害时决定，不是全局生效。
     */
    @Override
    public float getSovereigntyBonus() {
        return decreeStacks() * DECREE_BONUS_PER_STACK;
    }

    // ==================== 天赋：星扩散反应基础伤害提升 ====================

    /** 每满 100 点攻击力提升一档（<b>不足 100 完全不提升</b>）。 */
    public static final double SWIRL_BONUS_ATK_PER_STAGE = 100.0;
    /** 每档提升 0.7%。 */
    public static final float SWIRL_BONUS_PER_STAGE = 0.007f;
    /** 上限 +14%。 */
    public static final float SWIRL_BONUS_CAP = 0.14f;

    /**
     * 队伍里的角色触发星扩散时，按<b>薇斯娜自己的攻击力</b>给的基础伤害提升。
     *
     * <pre>
     * 提升 = floor(攻击力 / 100) × 0.7%，上限 14%
     * 例：攻击力 1234 → 12 档 → +8.4%；攻击力 99 → 0 档 → +0%
     * </pre>
     *
     * <p>（户口 {@link #stellarHousehold()} 会把这份加成带出去给全队。）
     */
    public float stellarSwirlBaseBonusMult() {
        double atk = getData().getAttributeTotalValue(ModAttributes.ATK.value());
        int stages = (int) Math.floor(atk / SWIRL_BONUS_ATK_PER_STAGE);
        return Math.min(SWIRL_BONUS_CAP, stages * SWIRL_BONUS_PER_STAGE);
    }

    /**
     * 薇斯娜的<b>星扩散户口</b>：冰扩散 → 星扩散，并按攻击力给全队基础伤害提升。
     *
     * <p>「转化」和「加成」是同一个天赋里的两半，所以一起放在这份户口里返回；
     * 她能<b>进入</b>星扩散状态是另一件事（见 {@link IStellarStateHolder}）。
     */
    @Override
    public IStellarHousehold.StellarHousehold stellarHousehold() {
        return new IStellarHousehold.StellarHousehold(
                StellarGlimmerBranch.SWIRL,
                ModElements.CYRO.get(),
                ModElements.ANEMO.get(),
                stellarSwirlBaseBonusMult());
    }

    // ==================== 突破 4：队伍元素构成加成 ====================

    /** 临时属性来源名（重复设置前先移除，避免叠上去）。 */
    private static final String A4_ATK_SOURCE = "vesna_a4_atk";
    private static final String A4_EM_SOURCE = "vesna_a4_em";
    /** 每有一位 冰/风 角色：攻击力 +6%。 */
    private static final float A4_ATK_PER_MEMBER = 0.06f;
    /** 每有一位其他元素角色：元素精通 +25。 */
    private static final float A4_EM_PER_MEMBER = 25f;
    /** 突破等级门槛。 */
    private static final int A4_ASCENSION = 4;

    /**
     * 命座 4：「仪典·冬之凯风」强化 —— 上面两项效果（攻击力 / 元素精通）改为<b>三倍</b>。
     */
    public static final float C4_WINTER_RITE_MULTIPLIER = 3f;

    /**
     * 「辉映·星扩散」天赋：<b>只在处于星扩散状态时</b>、按队伍元素构成给自己加属性。
     *
     * <pre>
     * 冰/风角色（含薇斯娜自己）：每人 攻击力 +6%
     * 其他元素角色           ：每人 元素精通 +25
     * </pre>
     *
     * <p>用临时属性修饰符实现；退出星扩散 / 突破不够时只移除自己那两个来源，
     * 不碰圣遗物之类别人加的修饰符。
     */
    private void updateA4Bonuses(Player player) {
        boolean active = getData().getAscensionPhase() >= A4_ASCENSION
                && com.linweiyun.genshin.core.system.reaction.StellarGlimmer.hasSwirl(this);

        if (!active) {
            getData().removeAttributeModifier(ModAttributes.ATK.value(), A4_ATK_SOURCE);
            getData().removeAttributeModifier(ModAttributes.ELEMENTAL_MASTERY.value(), A4_EM_SOURCE);
            return;
        }

        int windOrIce = 0;
        int others = 0;
        var attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        if (attachment != null) {
            for (int i = 0; i < 4; i++) {
                var member = attachment.getPartyCharacter(i);
                if (member == null) {
                    continue;
                }
                var element = member.getElemental();
                String id = element == null ? "" : element.getId();
                if (id.equals("anemo") || id.equals("cryo")) {
                    windOrIce++;
                } else {
                    others++;
                }
            }
        }

        getData().removeAttributeModifier(ModAttributes.ATK.value(), A4_ATK_SOURCE);
        getData().removeAttributeModifier(ModAttributes.ELEMENTAL_MASTERY.value(), A4_EM_SOURCE);
        // 命座 4：「仪典·冬之凯风」强化 —— 攻击力与元素精通的效果改为原本的三倍
        float scale = hasConstellation(4) ? C4_WINTER_RITE_MULTIPLIER : 1f;
        if (windOrIce > 0) {
            getData().addAttributeTempPercentModifier(ModAttributes.ATK.value(), A4_ATK_SOURCE,
                    A4_ATK_PER_MEMBER * windOrIce * scale);
        }
        if (others > 0) {
            getData().addAttributeTempFlatModifier(ModAttributes.ELEMENTAL_MASTERY.value(), A4_EM_SOURCE,
                    A4_EM_PER_MEMBER * others * scale);
        }
    }

    // ==================== 命座 ====================

    /** 「仪典·春之行列」= 突破天赋 1（给「大权」加成的那个）。命座 2 的前置。 */
    public static final int SPRING_RITE_ASCENSION = 1;
    /** 命座 1：巡风列装下最高境界翔风剑的次数上限 3 → 4。 */
    public static final int C1_EXTRA_LV3_USES = 1;
    /** 命座 1：巡风列装模式下造成的星扩散反应伤害 +20%。 */
    public static final float C1_STELLAR_SWIRL_BONUS = 0.20f;
    /** 命座 2：满层整肃时攻击力 +40%。 */
    public static final float C2_ATK_BONUS = 0.40f;
    private static final String C2_ATK_SOURCE = "vesna_c2_atk";

    /** 「仪典·春之行列」（突破天赋 1）是否已解锁。 */
    public boolean hasSpringRiteTalent() {
        return getData().getAscensionPhase() >= SPRING_RITE_ASCENSION;
    }

    /** 一次巡风列装最多能放几次最高境界（三阶）翔风剑 —— 0 命 3 次，命座 1 时 4 次。 */
    public int maxLv3UsesInWindrider() {
        return BASE_LV3_USES_IN_WINDRIDER + (hasConstellation(1) ? C1_EXTRA_LV3_USES : 0);
    }

    /**
     * 这一次施放的「最高境界翔风剑」是不是<b>免剑气</b>的那一次（命座 1）。
     *
     * <p>判据是「本次进入巡风列装以来还没放过三阶」（{@code lv3UsesInWindrider == 0}）——
     * 不用额外加字段，因为那个计数在 {@link #activateWindriderMode()} 里会归零。
     */
    public boolean lv3CastIsFreeNow() {
        return hasConstellation(1)
                && xiangfengJianLevel >= 2
                && lv3UsesInWindrider == 0;
    }

    /** 命座 1：巡风列装模式下的星扩散反应伤害加成（反应加成区里的「星烁加成」）。 */
    @Override
    public float getStellarGlimmerBonus(StellarGlimmerBranch branch) {
        if (branch == StellarGlimmerBranch.SWIRL && windriderActive && hasConstellation(1)) {
            return C1_STELLAR_SWIRL_BONUS;
        }
        return 0f;
    }

    /** 命座 6：薇斯娜造成的星扩散反应伤害<b>擢升</b> 20%（走独立的擢升区，不是反应加成区）。 */
    public static final float C6_STELLAR_SWIRL_ELEVATION = 0.20f;

    @Override
    public float getOwnElevationBonus(StellarGlimmerBranch branch) {
        if (branch == StellarGlimmerBranch.SWIRL && hasConstellation(6)) {
            return C6_STELLAR_SWIRL_ELEVATION;
        }
        return 0f;
    }

    /** 命座 2：进入巡风列装时直接获得满层整肃（需要已解锁突破天赋 1）。 */
    public boolean c2GrantsMaxDecree() {
        return hasConstellation(2) && hasSpringRiteTalent();
    }

    /** 把整肃直接拉满（命座 2 用）。 */
    public void fillDecreeToMax() {
        java.util.Arrays.fill(decreeTicks, DECREE_DURATION_TICKS);
        LOGGER.info("[整肃] 命座 2：进入巡风列装 → 直接拉满 {} 层（大权 +{}%）",
                MAX_DECREE_STACKS, (int) (getSovereigntyBonus() * 100));
        syncSkillState();
    }

    /**
     * 命座 2：<b>满层整肃</b>时攻击力 +40%（{@code hasSpringRiteTalent()} 为前置）。
     *
     * <p>和突破 4 那套一样用临时修饰符，且每刻重算 —— 掉了层（自然掉或清空）
     * 下一次 tick 就会自己摘掉，不需要在清层的地方通知它。
     */
    private void updateC2Bonus() {
        getData().removeAttributeModifier(ModAttributes.ATK.value(), C2_ATK_SOURCE);
        if (c2GrantsMaxDecree() && decreeStacks() >= MAX_DECREE_STACKS) {
            getData().addAttributeTempPercentModifier(ModAttributes.ATK.value(), C2_ATK_SOURCE, C2_ATK_BONUS);
        }
    }

    public Vesna() {
        super(UID, 5, translatable("character.name.vesna"),
                ModElements.ANEMO.getId().toString(), CharacterAscendAttribute.ATK,
                18 * 20, 15 * 20, 60f, ID,
                Map.of(
                        ModAttributes.MAX_HP.getId(), ShenheAttributeConfig::getAllHp,
                        ModAttributes.ATK.getId(), ShenheAttributeConfig::getAllAtk,
                        ModAttributes.DEF.getId(), ShenheAttributeConfig::getAllDef
                ));
        CharacterRenderRepository.register(VesnaResources.RENDER_DATA);
        this.talent = new VesnaTalent();
        this.vesnaEnergy = 0;
        this.vesnaMaxEnergy = 18;
    }

    @Override
    public CharacterActionData getActionData() {
        return VesnaResources.ACTION_DATA;
    }

    @Override
    public Map<Identifier, Supplier<List<? extends Integer>>> getStatGrowthMap() {
        return Map.of(
                ModAttributes.MAX_HP.getId(), ShenheAttributeConfig::getAllHp,
                ModAttributes.ATK.getId(), ShenheAttributeConfig::getAllAtk,
                ModAttributes.DEF.getId(), ShenheAttributeConfig::getAllDef
        );
    }

    // ==================== 同步 ====================

    /**
     * 技能状态变化后调它：常规同步 + 多标一次「整包同步」。
     *
     * <p>为什么不能只靠 {@code syncRealtimeState()}：那是 LDLib2 的增量包，
     * 客户端收不到（客户端没绑 ownerPlayer，处理函数直接返回）。
     * 真正到得了客户端的是 {@code CharacterTickEvent} 里「{@code data.isDirty()} → 整包同步」
     * 那条路，而它只认 {@code PGCharacterData.markDirty()}。
     *
     * <p>不标的话：没有 CD 在转的时候 {@code PGCharacterData.tick()} 不会标 dirty，
     * 于是「进入巡风列装 / 剑气变化」永远传不到客户端 ——
     * 表现就是能量条不出现、图标不显示技能状态。
     *
     * <p><b>只在这些「事件型」的改状态方法里调</b>，不要在每 tick 的路径上调：
     * 整包同步发的是整份角色数据，每 tick 发一次没必要。
     */
    private void syncSkillState() {
        syncRealtimeState();
        getData().markDirty();
    }

    // ==================== 能量 ====================

    public void addEnergy(float value) {
        this.vesnaEnergy = Math.min(vesnaEnergy + value, vesnaMaxEnergy);
        syncSkillState();
    }

    public void consumeEnergy(float value) {
        this.vesnaEnergy = Math.max(vesnaEnergy - value, 0);
        syncSkillState();
    }

    // ==================== 模式切换 ====================

    public void activateWindriderMode() {
        this.windriderActive = true;
        this.windriderRemainingTicks = WIND_RIDER_DURATION_TICKS;
        this.vesnaEnergy = WIND_RIDER_ENTER_ENERGY;
        this.xiangfengJianLevel = 0;
        this.lv3UsesInWindrider = 0;
        this.entryCast = false;   // 进门这一刀由 talent 紧接着打标记
        syncSkillState();

        // 命座 2：进入巡风列装直接拿满整肃（在 talent 的清层之后跑，所以清完就填满）
        if (c2GrantsMaxDecree()) {
            fillDecreeToMax();
        }
    }

    public void exitWindriderMode() {
        this.windriderActive = false;
        this.windriderRemainingTicks = 0;
        this.vesnaEnergy = 0f;
        this.xiangfengJianLevel = 0;
        this.lv3UsesInWindrider = 0;
        this.entryCast = false;
        syncSkillState();
    }

    // ==================== 字段 setter（供 talent 修改） ====================

    public void setXiangfengJianLevel(int level) {
        this.xiangfengJianLevel = Math.max(0, Math.min(3, level));
        syncSkillState();
    }

    public void setLv3UsesInWindrider(int n) {
        this.lv3UsesInWindrider = Math.max(0, n);
        syncSkillState();
    }

    // ==================== 入门那一刀 ====================

    /** 标记「这一刀是进入巡风列装的那一下」（触发那一刻调用）。 */
    public void markEntryCast() {
        this.entryCast = true;
    }

    /** 取出并清掉入门标记；{@code true} = 这一刀是入门刀。 */
    public boolean consumeEntryCast() {
        boolean value = this.entryCast;
        this.entryCast = false;
        return value;
    }

    // ==================== action state ====================

    /**
     * 命座 6：三阶翔风剑之后的「变移」窗口还剩多少刻（{@value #BIANYI_WINDOW_TICKS} 刻 = 5 秒）。
     *
     * <p>用<b>绝对到期时刻</b>而不是倒计时：这样客户端不用等服务端每刻同步，
     * 自己拿本地游戏时间比一下就知道窗口还在不在（{@link #hasBianyiWindow}）。
     */
    public static final int BIANYI_WINDOW_TICKS = 5 * 20;

    @DescSynced @Persisted(key = "bianyiWindowEndTick")
    protected long bianyiWindowEndTick;

    /** 命座 6：是否处于「变移」窗口内（双端都能判，用各自的游戏时间比）。 */
    public boolean hasBianyiWindow(Player player) {
        return bianyiWindowEndTick > 0L && player.level().getGameTime() < bianyiWindowEndTick;
    }

    /** 命座 6：打开「变移」窗口（放完三阶翔风剑那一下调用）。 */
    public void openBianyiWindow(Player player) {
        if (!hasConstellation(6)) return;
        this.bianyiWindowEndTick = player.level().getGameTime() + BIANYI_WINDOW_TICKS;
        LOGGER.info("[薇斯娜] 满命：变移窗口开启（{} 刻）", BIANYI_WINDOW_TICKS);
        syncSkillState();
    }

    /** 命座 6：消费掉窗口 —— 放过一次变移之后，普攻/战技恢复正常队列。 */
    public void consumeBianyiWindow() {
        if (bianyiWindowEndTick == 0L) return;
        this.bianyiWindowEndTick = 0L;
        syncSkillState();
    }

    @Override
    public String getActionStateKey(Player player) {
        // 满命窗口内：普攻 / E 点按都换成「翔风剑·变移」，放完窗口关掉、键自然回到普通队列
        if (hasBianyiWindow(player)) return "bianyi";
        if (!windriderActive) return "default";
        return "windrider_" + xiangfengJianLevel;
    }

    @Override
    public float getSkillDisplayCooldown() {
        if (windriderActive) return 0f;
        return super.getSkillDisplayCooldown();
    }

    // ==================== 大招的「权威数值」====================
    //
    // 大招能量 60 / 冷却 15 秒。这两个值也会被写进角色数据（HUD 读的就是它们），
    // 而老存档里存的是旧值（80 能量 / 10 秒冷却）—— 数据是持久化的，
    // 光改构造器对**已经存在的角色**无效，所以这里做一次自愈：
    // 读的时候发现和常量不一致就改回去。

    public static final float BURST_ENERGY_COST = 60f;
    public static final int BURST_COOLDOWN_TICKS = 15 * 20;

    @Override
    public int getBurstMaxCooldownTick() {
        if (getData().getBurstMaxCooldownTick() != BURST_COOLDOWN_TICKS) {
            getData().setBurstMaxCooldownTick(BURST_COOLDOWN_TICKS);
        }
        return BURST_COOLDOWN_TICKS;
    }

    @Override
    public float getMaxObtainingEnergy() {
        if (getData().getMaxObtainingEnergy() != BURST_ENERGY_COST) {
            getData().setMaxObtainingEnergy(BURST_ENERGY_COST);
        }
        return BURST_ENERGY_COST;
    }

    // ==================== E 钩子覆写 ====================

    @Override
    public boolean canUseElementalSkill(Player player, int skillTime) {
        if (windriderActive) {
            // 命座 1：本次进入模式后的第一次最高境界翔风剑免剑气 —— 没剑气也放得出来
            if (lv3CastIsFreeNow()) {
                return true;
            }
            return vesnaEnergy >= SPECIAL_SKILL_ENERGY_COST;
        }
        return super.canUseElementalSkill(player, skillTime);
    }

    @Override
    public void applyElementalSkillCooldown(Player player, int skillTime) {
        if (player.level().isClientSide()) return;

        // ⚠️ 判断依据是「这一刀是不是进入巡风列装的那一下」（entryCast），
        //    不能再用 windriderActive —— 模式现在是在「触发那一刻」就开的
        //    （VesnaTalent.onCastStart，跑在 request() 里面，早于本方法），
        //    到这里它已经是 true。之前就是这么漏掉 CD 的：
        //    CD 没设 → PGCharacterData 不标 dirty → 整包同步不触发 →
        //    客户端连模式/剑气都收不到（能量条不出现、图标不显示 CD、能直接再开一次）。
        if (entryCast) {
            getData().setElementalSkillCooldownTick(18 * 20);
            syncRealtimeState();
        }
    }

    @Override
    public void sendSkillCooldownMessage(Player player) {
        if (windriderActive) {
            player.sendSystemMessage(literal("§4剑气不足！"));
        } else {
            super.sendSkillCooldownMessage(player);
        }
    }

    // ==================== tick ====================

    @Override
    public void tick(Player player) {
        super.tick(player);

        if (player.level().isClientSide()) return;

        if (windriderActive && windriderRemainingTicks > 0) {
            windriderRemainingTicks--;
            if (windriderRemainingTicks <= 0) {
                exitWindriderMode();
            }
        }

        if (windriderActive) {
            syncRealtimeState();
        }

        // 整肃每层独立倒计时
        tickDecree();
        // 退场（切到别的角色）时清空整肃。
        // 比 UUID 而不是比对象：角色列表在反序列化后可能换实例，
        // 比对象引用会把「自己还在场上」误判成退场，每刻清一次 → 大权永远是 1。
        var partyAttachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        if (partyAttachment != null) {
            PGCharacter current = partyAttachment.getCurrentCharacter();
            if (current == null || current.getCharacterUUID() != getCharacterUUID()) {
                clearDecree();
            }
        }
        // 突破 4 的「辉映·星扩散」队伍加成（只在星扩散状态下生效）
        updateA4Bonuses(player);
        // 命座 2 的「满层整肃 → 攻击力 +40%」
        updateC2Bonus();
    }
}