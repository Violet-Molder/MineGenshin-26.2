package com.linweiyun.genshin.core.character;

import com.linweiyun.genshin.config.character.CharacterXpConfig;
import com.linweiyun.genshin.content.attribute.AttributeType;
import com.linweiyun.genshin.content.effect.character.CharacterEffectContainer;
import com.linweiyun.genshin.content.effect.character.CharacterEffectHelper;
import com.linweiyun.genshin.content.effect.character.CharacterEffectInstance;
import com.linweiyun.genshin.content.effect.character.ICharacterEffect;
import com.linweiyun.genshin.content.effect.character.artifact.ArtifactSetEffect;
import com.linweiyun.genshin.content.items.artifact.ArtifactItem;
import com.linweiyun.genshin.content.items.artifact.ArtifactSet;
import com.linweiyun.genshin.content.items.artifact.inventory.ArtifactInventory;
import com.linweiyun.genshin.content.items.artifact.type.ArtifactType;
import com.linweiyun.genshin.content.items.component.ArtifactStatsComponent;
import com.linweiyun.genshin.content.items.component.WeaponStatsComponent;
import com.linweiyun.genshin.content.items.weapon.WeaponItem;
import com.linweiyun.genshin.content.stat.TeyvatItemStat;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.network.NetworkManager;
import com.linweiyun.genshin.core.character.talent.TalentBase;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.system.combat.action.ActionKind;
import com.linweiyun.genshin.core.system.combat.action.ActionManager;
import com.linweiyun.genshin.core.system.combat.action.ActionSet;
import com.linweiyun.genshin.core.system.registry.ModRegistries;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import com.linweiyun.genshin.core.system.registry.register.ModDataComponents;
import com.linweiyun.genshin.enums.CharacterAscendAttribute;
import com.linweiyun.genshin.core.sync.ISyncCharacter;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import com.lowdragmc.lowdraglib2.syncdata.storage.IManagedStorage;
import com.mojang.logging.LogUtils;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PGCharacter implements IPersistedSerializable, ISyncCharacter {

    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Override public IManagedStorage getSyncStorage() { return syncStorage; }
    @Override public PGCharacter getSelfCharacter() { return this; }
    @Override public void notifyPersistence() { }

    @Getter
    @Setter
    @Persisted(key = "character_uuid")
    protected int characterUUID;
    @Getter
    @Setter
    @Persisted(key = "star_rating")
    protected int starRating;
    @Getter
    @Setter
    @Persisted(key = "name")
    protected Component name;
    @Persisted(key = "elemental")
    protected String elementalId;
    private transient GenshinElement elemental;
    @Getter
    @Setter
    @Persisted(key = "ascend_attribute")
    protected CharacterAscendAttribute ascendAttribute;
    @Getter
    @Setter
    @Persisted(key = "texture_id")
    protected String textureId;
    @Getter
    @Setter
    @Persisted(key = "data")
    protected PGCharacterData data;

    private static final String SOURCE_WEAPON = "weapon";

    protected transient TalentBase talent;

    private static final Logger LOGGER = LogUtils.getLogger();

    private final Map<String, ActionSet> actionSetCache = new HashMap<>();

    public PGCharacter() {
        this.data = new PGCharacterData();
        this.data.setParentCharacter(this);
    }

    public PGCharacter(
            int characterUUID, int starRating, Component name,
            String elementalId, CharacterAscendAttribute ascendAttribute,
            int skillMaxCooldownTick, int burstMaxCooldownTick,
            float maxObtainingEnergy, String textureId,
            Map<Identifier, Supplier<List<? extends Integer>>> statGrowthMap) {
        this.characterUUID = characterUUID;
        this.starRating = starRating;
        this.name = name;
        this.elementalId = elementalId;
        this.ascendAttribute = ascendAttribute;
        this.data = new PGCharacterData();
        this.data.setParentCharacter(this);
        this.data.setSkillShortMaxCooldownTick(skillMaxCooldownTick);
        this.data.setSkillLongMaxCooldownTick(skillMaxCooldownTick);
        this.data.setBurstMaxCooldownTick(burstMaxCooldownTick);
        this.data.setMaxObtainingEnergy(maxObtainingEnergy);
        this.textureId = textureId;
    }

    public PGCharacter(
            int characterUUID, int starRating, Component name,
            String elementalId, CharacterAscendAttribute ascendAttribute,
            int skillShortMaxCooldownTick, int skillLongMaxCooldownTick, int burstMaxCooldownTick,
            float maxObtainingEnergy, String textureId,
            Map<Identifier, Supplier<List<? extends Integer>>> statGrowthMap) {
        this.characterUUID = characterUUID;
        this.starRating = starRating;
        this.name = name;
        this.elementalId = elementalId;
        this.ascendAttribute = ascendAttribute;
        this.data = new PGCharacterData();
        this.data.setParentCharacter(this);
        this.data.setSkillShortMaxCooldownTick(skillShortMaxCooldownTick);
        this.data.setSkillLongMaxCooldownTick(skillLongMaxCooldownTick);
        this.data.setBurstMaxCooldownTick(burstMaxCooldownTick);
        this.data.setMaxObtainingEnergy(maxObtainingEnergy);
        this.textureId = textureId;
    }

    private AttributeType resolveType(Identifier id) {
        return ModAttributes.ATTRIBUTES.getRegistry().get().getValue(id);
    }

    public Class<? extends WeaponItem> getAllowedWeaponClass() {
        return WeaponItem.class;
    }

    // ============ 动作系统扩展点 ============

    public String getActionStateKey(Player player) {
        return "default";
    }

    /**
     * 客户端要不要在「这一招出手那一刻」<b>本地也跑一次</b>天赋钩子（默认不跑）。
     *
     * <p>角色天赋只在服务端执行，客户端只播动画 + 发包 —— 绝大多数招式这样就够了
     * （伤害在服务端、动画在客户端）。但有些招式的<b>表现层位移必须由技能自己算</b>：
     * 申鹤的 {@code DashSystem} 就是「客户端按格推位置、服务端沿途扫伤害」，
     * 客户端不跑天赋的话那段位移就成了死代码。
     *
     * <p>打开的角色的天赋必须在客户端安全：自己按 {@code level.isClientSide()} 分流，
     * 服务端专属逻辑（改数据、加效果、扣能量、生成实体）不要跑。
     */
    public boolean runsTalentOnClient() {
        return false;
    }

    /**
     * 辉映·星烁反应的伤害加成（反应加成区）—— <b>按分支给</b>。
     *
     * <p>覆写它就能做「天赋给星烁加成」的角色。写法有两种，对应文案的两种口径：
     * <pre>
     * // 文案写「星烁反应伤害提升 20%」→ 星扩散、星超导都给
     * public float getStellarGlimmerBonus(StellarGlimmerBranch branch) { return 0.20f; }
     *
     * // 文案只写「星扩散伤害提升 20%」（薇斯娜那一类）→ 只给星扩散
     * public float getStellarGlimmerBonus(StellarGlimmerBranch branch) {
     *     return branch == StellarGlimmerBranch.SWIRL ? 0.20f : 0f;
     * }
     * </pre>
     *
     * <p>效果/ buff 那边走 {@code ICharacterEffect.getStellarGlimmerBonus(分支)}，
     * 两边会在 {@code StellarGlimmer.bonusOf(...)} 里相加。
     */
    public float getStellarGlimmerBonus(com.linweiyun.genshin.core.system.reaction.StellarGlimmerBranch branch) {
        return 0f;
    }

    /**
     * 「擢升」加成（伤害公式里的<b>擢升区</b> = {@code 1 + 这个值}），按分支给。
     *
     * <p>和 {@link #getStellarGlimmerBonus} 的区别是<b>乘区不同</b>：
     * 那个落在反应加成区、和元素精通<b>加算</b>；
     * 这个落在擢升区、是<b>独立的乘区</b>（在暴击之后、大权之前）。
     * 文案写「擢升」的加成就走这里（例：薇斯娜满命的星扩散伤害擢升 20%）。
     */
    public float getElevationBonus(com.linweiyun.genshin.core.system.reaction.StellarGlimmerBranch branch) {
        // 两块相加：效果侧（例如队友身上沃雅妮莎 6 命的 Glimmer）+ 角色天赋自己那一份
        return data.getEffectContainer().getElevationBonus(branch) + getOwnElevationBonus(branch);
    }

    /**
     * 角色<b>天赋自己</b>给的擢升加成（默认 0；例：薇斯娜 6 命的星扩散擢升 20%）。
     *
     * <p>单独开一个方法是为了不被上面的容器聚合吞掉：覆写 {@link #getElevationBonus} 的话，
     * 队友给你的效果加成也会一起没了。
     */
    public float getOwnElevationBonus(com.linweiyun.genshin.core.system.reaction.StellarGlimmerBranch branch) {
        return 0f;
    }

    /**
     * 按<b>元素 / 反应类型</b>的额外暴击伤害（加在统一 CDG 之上），默认 0。
     *
     * <p>和 {@link #getStellarGlimmerBonus} / {@link #getElevationBonus} 同类，只是作用在暴击区：
     * 文案写「水元素伤害与冰元素伤害的暴击伤害提升 X%」这种就覆写它
     * （例：沃雅妮莎 2 命的「黑与白的双音」）。
     *
     * @param element         这次伤害的元素
     * @param stellarReaction 这次是不是星烁（星扩散/星超导）反应伤害
     */
    public float getCritDamageBonus(com.linweiyun.genshin.core.element.GenshinElement element,
                                    boolean stellarReaction) {
        // 效果侧聚合（和 getStellarGlimmerBonus 一样：效果 + 角色自己相加）
        return data.getEffectContainer().getCritDamageBonus(element, stellarReaction);
    }

    /**
     * 治疗加成（小数，{@code 0.04 = 4%}）—— 装备者实际治疗时乘进治疗量。
     *
     * <p>两块相加：效果侧（{@code ICharacterEffect#getHealingBonus} 的聚合，
     * 例如圣遗物/天赋给的治疗加成）+ 装备武器自己那一份
     * （{@code WeaponItem#getHealingBonus}，例如漩流颂歌的 +4%）。
     *
     * <p>武器那一份直接按「当前装着的武器」算，不落任何持久化状态 ——
     * 换武器即时生效，也不会被老存档的影子值盖掉。
     */
    public float getHealingBonus() {
        float total = data.getEffectContainer().getHealingBonus();
        ItemStack weapon = data.getWeapon();
        if (weapon != null && !weapon.isEmpty() && weapon.getItem() instanceof WeaponItem weaponItem) {
            total += weaponItem.getHealingBonus();
        }
        return total;
    }

    /**
     * 「大权」加成（伤害公式里的<b>大权区</b> = {@code 1 + 这个值}）。
     *
     * <p>由角色自己按层数/状态给，例如薇斯娜的「整肃」：每层 +10%，
     * 只作用在她召唤的灵剑（翔风剑二/三阶与大招那几段）上。
     * 没有这套机制的角色保持 0，大权区就是 1。
     */
    public float getSovereigntyBonus() {
        return 0f;
    }

    /**
     * 获取数据驱动的动作配置（来自角色资源的 CharacterActionData）。
     * 子类可覆盖以返回角色专属配置。
     */
    public com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData getActionData() {
        return null;
    }

    public final ActionSet getActionSet(Player player) {
        String key = getActionStateKey(player);
        return actionSetCache.computeIfAbsent(key, k -> {
            ActionSet built = (talent != null) ? talent.buildActionSet(this, k) : null;
            return built != null ? built : buildFallbackActionSet();
        });
    }

    protected ActionSet buildFallbackActionSet() {
        return ActionSet.builder().build();
    }

    protected void invalidateActionSetCache() {
        actionSetCache.clear();
    }

    /**
     * 调试用：返回 talent 是否已初始化。
     * <p>
     * 客户端从网络反序列化角色时不走子类构造函数，
     * {@code talent} 是 transient 字段 → 客户端永远 null。
     */
    public String getTalentDebugInfo() {
        return talent == null
                ? "null(class=" + this.getClass().getSimpleName() + ")"
                : talent.getClass().getSimpleName();
    }

    // ============ E / Q 钩子（由 ActionManager 调用） ============

    /**
     * 出手<b>前置判断</b> —— 「这一招现在放不放得出来」。
     *
     * <h2>为什么需要它</h2>
     * 本 MOD 的架构是「客户端管动画、服务端管结算」：按键那一帧客户端就把动画切了，
     * 请求才发给服务端。于是凡是<b>服务端会拒绝</b>的条件（能量不够、CD 没好、没子弹…），
     * 都会变成「动画播了、什么都没有」—— 玩家看到的是「我放了，但没效果」。
     *
     * <p>所以这条判断要在<b>播动画之前</b>问一次：
     * <ul>
     *   <li><b>客户端</b>（{@code ResourceDrivenActionHandler}）：不通过就<b>不播动画、不发请求</b>；</li>
     *   <li><b>服务端</b>（{@code ActionManager}）：照旧再判一次，它才是权威。</li>
     * </ul>
     *
     * <h2>写实现时的两条约束</h2>
     * <ol>
     *   <li><b>只读双端都有的数据</b>：这个方法会在客户端跑，
     *       而 {@code talent} 字段在客户端是 {@code null}（反序列化不走子类构造器），
     *       所以判断只能基于 {@code data}（同步过的角色数据）这类双端都有的状态，
     *       <b>不要</b>调 {@code getTalent()} 里的东西。</li>
     *   <li><b>不要有副作用</b>：它可能被每刻调用（长按重试）。
     *       提示消息走 {@link #sendCastFailedMessage(Player, ActionKind)}，那边有节流。</li>
     * </ol>
     *
     * <p>默认实现落到已有的 E/Q 钩子上：角色要加自己的条件（能量、姿态、弹药…）
     * 就覆盖 {@link #canUseElementalSkill} / {@link #canUseElementalBurst}，
     * 或者直接覆盖这个方法加全新的招式门槛。
     *
     * @param kind      这一招是什么（普攻/重击/战技/大招/闪避）
     * @param skillTime 战技的短按(0)/长按(1000)标记，其它招式忽略
     * @return true = 可以放
     */
    public boolean canCast(Player player, ActionKind kind, int skillTime) {
        return switch (kind) {
            case ELEMENTAL_SKILL_TAP, ELEMENTAL_SKILL_HOLD -> canUseElementalSkill(player, skillTime);
            case ELEMENTAL_BURST -> canUseElementalBurst(player);
            // 普攻 / 重击 / 闪避 / 下落攻击默认没有门槛
            default -> true;
        };
    }

    /**
     * 前置判断没过时的反馈（客户端与服务端共用同一套文案）。
     *
     * <p>默认按招式类别给出对应的提示；不需要提示就覆盖成空实现。
     */
    public void sendCastFailedMessage(Player player, ActionKind kind) {
        switch (kind) {
            case ELEMENTAL_SKILL_TAP, ELEMENTAL_SKILL_HOLD -> sendSkillCooldownMessage(player);
            case ELEMENTAL_BURST -> sendBurstCooldownMessage(player);
            default -> {
            }
        }
    }

    public boolean canUseElementalSkill(Player player, int skillTime) {
        return data.getElementalSkillCooldownTick() == 0;
    }

    public void applyElementalSkillCooldown(Player player, int skillTime) {
        data.setElementalSkillStacks(data.getElementalSkillStacks() - 1);
        if (skillTime < 1000) {
            data.setElementalSkillCooldownTick(data.getSkillShortMaxCooldownTick());
        } else {
            data.setElementalSkillCooldownTick(data.getSkillLongMaxCooldownTick());
        }
        syncRealtimeState();
    }

    public void sendSkillCooldownMessage(Player player) {
        player.sendSystemMessage(Component.translatable(
                "message.minegenshin.skill_cooldown",
                data.getElementalSkillCooldownTick()));
    }

    public boolean canUseElementalBurst(Player player) {
        if (data.getElementalBurstCooldownTick() > 0) return false;
        boolean canUse = data.getCurrentObtainingEnergy() >= data.getMaxObtainingEnergy();
        return true;
    }

    public void applyElementalBurstCooldown(Player player) {
        data.setCurrentObtainingEnergy(0);
        data.setElementalBurstCooldownTick(data.getBurstMaxCooldownTick());
        syncRealtimeState();
    }

    public void sendBurstCooldownMessage(Player player) {
        if (data.getElementalBurstCooldownTick() > 0) {
            player.sendSystemMessage(Component.translatable("message.minegenshin.skill_cooldown"));
        } else {
            player.sendSystemMessage(Component.translatable("message.minegenshin.not_enough_energy"));
        }
    }

    // ============ 普攻 / 重击 ============

    public void performNormalAttack(Player player, int comboStage) {
        if (talent != null) talent.attack(player, this, comboStage);
    }

    public void performChargedAttack(Player player) {
        if (talent != null) talent.chargeAttack(player, this);
    }

    public int getChargedAttackChargeTicks() {
        if (talent != null) return talent.getChargeTicks();
        return 20;
    }

    public void frontTick(Player player) {}

    public void backTick(Player player) {}
    /**
     * 两端都推进 ActionManager：
     * <ul>
     *   <li>客户端：本地跑动作状态机</li>
     *   <li>服务端：权威跑动作状态机</li>
     * </ul>
     */

    public void tick(Player player) {
        data.tick();
        recalculateDirtyArtifactSlots();
        frontTick(player);
        backTick(player);

        ActionManager.get(player).tick(player, this);

        if (!player.level().isClientSide()) {
            syncRealtimeState();
        }
    }

    // ============ 圣遗物 / 武器 ============

    public void equipArtifact(ArtifactType type, ItemStack artifactStack) {
        if (artifactStack.getItem() instanceof ArtifactItem artifactItem) {
            if (artifactItem.getType() != type) return;
            int slot = ArtifactInventory.typeToSlot(type);
            data.getArtifactInventory().setItem(slot, artifactStack.copy());
        }
    }

    public void unequipArtifact(ArtifactType type) {
        int slot = ArtifactInventory.typeToSlot(type);
        data.getArtifactInventory().setItem(slot, ItemStack.EMPTY);
    }

    public void recalculateDirtyArtifactSlots() {
        ArtifactInventory inv = data.getArtifactInventory();
        if (!inv.hasDirtySlots()) return;
        for (int i = 0; i < ArtifactInventory.SLOT_COUNT; i++) {
            if (inv.isDirty(i)) {
                recalculateArtifactSlot(i);
                inv.clearDirty(i);
            }
        }
        refreshArtifactSetEffects();
    }

    private void recalculateArtifactSlot(int slotIndex) {
        if (slotIndex == ArtifactInventory.SLOT_WEAPON) {
            recalculateWeaponSlot();
            return;
        }
        ArtifactType type = ArtifactInventory.slotToType(slotIndex);
        String source = null;
        if (type != null) {
            source = type.name().toLowerCase();
        }

        for (AttributeType attrType : ModRegistries.ATTRIBUTE_TYPE_REGISTRY) {
            data.removeAttributeModifier(attrType, source);
        }

        ItemStack stack = data.getArtifactInventory().getItem(slotIndex);
        if (!stack.isEmpty() && stack.getItem() instanceof ArtifactItem) {
            ArtifactStatsComponent stats = stack.getOrDefault(
                    ModDataComponents.ARTIFACT_STATS.get(),
                    ArtifactStatsComponent.DEFAULT
            );
            applyStatWithSet(stats.mainStat, source);
            for (TeyvatItemStat subStat : stats.subStats) {
                if (subStat.isUnlocked()) {
                    applyStatWithSet(subStat, source);
                }
            }
        }
    }

    public void recalculateWeaponSlot() {
        for (AttributeType attrType : ModRegistries.ATTRIBUTE_TYPE_REGISTRY) {
            data.removeAttributeModifier(attrType, SOURCE_WEAPON);
        }

        data.removeAttributeBaseValue(ModAttributes.ATK.get(), SOURCE_WEAPON);
        data.setWeaponBaseATK(0);

        ItemStack stack = data.getArtifactInventory().getItem(ArtifactInventory.SLOT_WEAPON);
        if (stack.isEmpty() || !(stack.getItem() instanceof WeaponItem weapon)) return;

        WeaponStatsComponent stats = stack.getOrDefault(
                ModDataComponents.WEAPON_STATS.get(), WeaponStatsComponent.DEFAULT);

        if (stats.mainStat != null && stats.mainStat.isInitialized()) {
            // 主词条修正：tier 只给整数基础攻击力，个别武器的主词条是小数
            //（例如蝶变 48 - 0.46 = 47.54）
            double mainValue = stats.mainStat.getValue() + weapon.getMainStatDelta();
            data.setWeaponBaseATK(mainValue);
            data.setAttributeBaseValue(ModAttributes.ATK.get(), SOURCE_WEAPON, mainValue);
        }

        if (stats.subStat != null && stats.subStat.isInitialized()) {
            AttributeType attr = stats.subStat.getAttribute();
            double value = stats.subStat.getValue();
            if (isBaseAttribute(attr)) {
                data.setAttributePercentModifier(attr, SOURCE_WEAPON, value);
            } else {
                data.setAttributeFlatModifier(attr, SOURCE_WEAPON, value);
            }
        }
    }

    private void applyStatWithSet(TeyvatItemStat stat, String source) {
        if (!stat.isInitialized()) return;
        AttributeType attr = stat.getAttribute();
        double value = stat.getValue();
        boolean isBaseAttr = isBaseAttribute(attr);
        if (isBaseAttr) {
            if (stat.getKind() == TeyvatItemStat.StatKind.FLAT) {
                data.setAttributeFlatModifier(attr, source, value);
            } else {
                data.setAttributePercentModifier(attr, source, value);
            }
        } else {
            data.setAttributeFlatModifier(attr, source, value);
        }
    }

    private static boolean isBaseAttribute(AttributeType attr) {
        if (attr == null || attr.id() == null) return false;
        AttributeType registryAttr = ModRegistries.ATTRIBUTE_TYPE_REGISTRY.getValue(attr.id());
        return registryAttr == ModAttributes.MAX_HP.get()
                || registryAttr == ModAttributes.ATK.get()
                || registryAttr == ModAttributes.DEF.get();
    }

    private void refreshArtifactSetEffects() {
        CharacterEffectContainer container = data.getEffectContainer();
        List<ICharacterEffect> toRemove = container.getEffects().stream()
                .map(CharacterEffectInstance::getEffect)
                .filter(effect -> effect instanceof ArtifactSetEffect)
                .toList();
        for (ICharacterEffect effect : toRemove) {
            CharacterEffectHelper.removeEffect(this.data.getOwnerPlayer(), this, effect);
        }
        Map<ArtifactSet, Integer> setCountMap = new HashMap<>();
        for (ItemStack stack : data.getAllArtifactsAsList()) {
            if (!(stack.getItem() instanceof ArtifactItem art)) continue;
            ArtifactSet set = art.getSet().get();
            setCountMap.merge(set, 1, Integer::sum);
        }
        setCountMap.forEach((set, count) -> {
            if (count >= 2) {
                CharacterEffectInstance inst = new CharacterEffectInstance(
                        set.twoPcEffect().get(),
                        CharacterEffectInstance.INFINITE, 1, true
                );
                CharacterEffectHelper.addEffect(this.data.getOwnerPlayer(), this, inst);
            }
            if (count >= 4 && set.hasFourPcEffect()) {
                CharacterEffectInstance inst = new CharacterEffectInstance(
                        set.fourPcEffect().get(),
                        CharacterEffectInstance.INFINITE, 1, true
                );
                CharacterEffectHelper.addEffect(this.data.getOwnerPlayer(), this, inst);
            }
            data.syncEffectsToTag();
        });
    }

    public void hurt(float amount) {
        double before = data.getCurrentHP();
        data.hurtHP(amount);
        syncRealtimeState();
        if (data.getCurrentHP() <= 0 && before > 0) {
            incapacitate();
        }
    }

    public void incapacitate() {
        Player player = data.getOwnerPlayer();
        if (player == null) return;
        PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        int currentIndex = attachment.getCurrentCharacterIndex();
        for (int offset = 1; offset <= 4; offset++) {
            int nextIndex = (currentIndex + offset) % 4;
            PGCharacter nextChar = attachment.getPartyCharacter(nextIndex);
            if (nextChar != null && nextChar.getData().getCurrentHP() > 0) {
                attachment.setCurrentCharacterIndex(nextIndex);
                if (player instanceof ServerPlayer sp) {
                    NetworkManager.setCharacterSelectionToPlayer(sp, nextIndex);
                }
                return;
            }
        }

        player.setData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT, false);
        if (player instanceof ServerPlayer sp) {
            NetworkManager.setGenshinModeToPlayer(sp, false);
        }
    }

    public void revive(int hp) {
        float maxHp = (float) data.getAttributeTotalValue(ModAttributes.MAX_HP.value());
        data.setCurrentHP(Math.min(hp, maxHp));
        enableDeployIfNeeded();
        syncRealtimeState();
    }

    public void revive(float percent) {
        float maxHp = (float) data.getAttributeTotalValue(ModAttributes.MAX_HP.value());
        data.setCurrentHP(Math.min(maxHp * percent, maxHp));
        enableDeployIfNeeded();
        syncRealtimeState();
    }

    public void revive(float percent, int extraHp) {
        float maxHp = (float) data.getAttributeTotalValue(ModAttributes.MAX_HP.value());
        data.setCurrentHP(Math.min(maxHp * percent + extraHp, maxHp));
        enableDeployIfNeeded();
        syncRealtimeState();
    }

    private void enableDeployIfNeeded() {
        if (data.getCurrentHP() <= 0) return;
        Player player = data.getOwnerPlayer();
        if (player == null) return;
        Boolean genshinMode = player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT);
        if (!genshinMode) {
            player.setData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT, true);
            if (player instanceof ServerPlayer sp) {
                NetworkManager.setGenshinModeToPlayer(sp, true);
            }
        }
    }

    public Map<Identifier, Supplier<List<? extends Integer>>> getStatGrowthMap() {
        return Map.of();
    }

    public int getStatAtLevel(AttributeType type, int levelIndex) {
        Supplier<List<? extends Integer>> supplier = getStatGrowthMap().get(type.id());
        if (supplier == null) return 0;
        List<? extends Integer> list = supplier.get();
        if (levelIndex < 0 || levelIndex >= list.size()) return 0;
        return list.get(levelIndex);
    }

    public double getBaseStat(AttributeType type) {
        Supplier<List<? extends Integer>> supplier = getStatGrowthMap().get(type.id());
        if (supplier == null) return type.defaultValue();
        List<? extends Integer> list = supplier.get();
        if (list.isEmpty()) return type.defaultValue();
        return list.getFirst();
    }

    public Set<AttributeType> getStatGrowthTypes() {
        return getStatGrowthMap().keySet().stream()
                .map(this::resolveType)
                .collect(Collectors.toSet());
    }

    public void addExp(int amount) {
        if (data.getLevel() >= 90) {
            return;
        }
        var expList = CharacterXpConfig.getAllXp();
        long totalMaxExp = 0;
        for (int i = 0; i < 89; i++) {
            totalMaxExp += expList.get(i);
        }
        long currentSpent = 0;
        for (int i = 0; i < data.getLevel() - 1; i++) {
            currentSpent += expList.get(i);
        }
        long remaining = totalMaxExp - currentSpent - data.getCurrentExp();
        if (amount > remaining) {
            data.setCurrentExp(data.getCurrentExp() + (int) remaining);
        } else {
            data.setCurrentExp(data.getCurrentExp() + amount);
        }
        tryLevelUp();
    }

    public void tryLevelUp() {
        var expList = CharacterXpConfig.getAllXp();
        int totalExpConsumed = 0;
        int levelsToGain = 0;
        int oldLevel = data.getLevel();
        int currentAscensionPhase = data.getAscensionPhase();
        while (oldLevel < 90) {
            int expNeeded = expList.get(oldLevel - 1);
            if (data.getCurrentExp() - totalExpConsumed < expNeeded) break;
            int maxLevelForPhase = currentAscensionPhase == 0
                    ? 20
                    : Math.min((currentAscensionPhase + 3) * 10, 90);
            if (oldLevel >= maxLevelForPhase) break;
            totalExpConsumed += expNeeded;
            levelsToGain++;
            oldLevel++;
        }
        if (levelsToGain == 0) return;
        data.setCurrentExp(data.getCurrentExp() - totalExpConsumed);
        data.addLevel(levelsToGain);
        int statIndex = data.getLevel() - 1 + data.getAscensionPhase();
        updateBaseStatsFromConfig(statIndex);
        data.setCurrentHP(data.getAttributeTotalValue(ModAttributes.MAX_HP.value()));

        if (data.getLevel() < 90) {
            data.setMaxExp(expList.get(data.getLevel() - 1));
        }
    }

    public void ascend() {
        int maxLevelForPhase = data.getAscensionPhase() == 0
                ? 20
                : Math.min((data.getAscensionPhase() + 3) * 10, 90);

        if (data.getLevel() != maxLevelForPhase) return;

        int newPhase = data.getAscensionPhase() + 1;
        data.setAscensionPhase(newPhase);
        int statIndex = maxLevelForPhase - 1 + newPhase;
        updateBaseStatsFromConfig(statIndex);

        int starRating = this.getStarRating();
        CharacterAscendAttribute ascendAttr = this.getAscendAttribute();
        AttributeType targetAttrType = getAscendAttributeType(ascendAttr);

        if (CharacterAscendAttribute.PERCENT_STATS.contains(ascendAttr)) {
            data.removeAttributeModifier(targetAttrType, "ascension_bonus");
        } else {
            data.removeAttributeBaseValue(targetAttrType, "character_ascend");
        }

        int bonusCount = getAscensionBonusCount(newPhase);
        if (bonusCount > 0) {
            applyAscendBonus(ascendAttr, targetAttrType, starRating, bonusCount);
        }

        tryLevelUp();
    }

    private void applyAscendBonus(CharacterAscendAttribute attr, AttributeType type,
                                  int starRating, int bonusCount) {
        int factor = starRating + 1;
        String baseKey = "character_ascend";
        switch (attr) {
            case ATK, HP:
                data.addAttributePercentModifier(type, "ascension_bonus", 0.012f * factor * bonusCount);
                break;
            case DEF:
                data.addAttributePercentModifier(type, "ascension_bonus", 0.015f * factor * bonusCount);
                break;
            case CR:
                data.setAttributeBaseValue(type, baseKey, 0.008 * factor * bonusCount);
                break;
            case CDG:
                data.setAttributeBaseValue(type, baseKey, 0.016 * factor * bonusCount);
                break;
            case HB:
                data.setAttributeBaseValue(type, baseKey, (starRating == 5 ? 0.056 : 0.047) * bonusCount);
                break;
            case ELEMENTAL_BONUS:
                data.setAttributeBaseValue(type, baseKey, 0.012 * factor * bonusCount);
                break;
            case EM:
                data.setAttributeBaseValue(type, baseKey, (double) (starRating == 5 ? 29 : 24) * bonusCount);
                break;
            case ER:
                data.setAttributeBaseValue(type, baseKey, (0.013333 * factor) * bonusCount);
                break;
        }
    }

    private static int getAscensionBonusCount(int phase) {
        return switch (phase) {
            case 0, 1 -> 0;
            case 2 -> 1;
            case 3, 4 -> 2;
            case 5 -> 3;
            case 6 -> 4;
            default -> 0;
        };
    }

    private AttributeType getAscendAttributeType(CharacterAscendAttribute ascendAttr) {
        return switch (ascendAttr) {
            case ATK -> ModAttributes.ATK.value();
            case HP -> ModAttributes.MAX_HP.value();
            case DEF -> ModAttributes.DEF.value();
            case CR -> ModAttributes.CR.value();
            case CDG -> ModAttributes.CDG.value();
            case HB -> ModAttributes.HB.value();
            case ELEMENTAL_BONUS -> getElementalDamageBonusType();
            case EM -> ModAttributes.ELEMENTAL_MASTERY.value();
            case ER -> ModAttributes.ER.value();
        };
    }

    private AttributeType getElementalDamageBonusType() {
        GenshinElement element = getElemental();
        if (element == ModElements.PYRO.get()) return ModAttributes.PYRO_BONUS.value();
        if (element == ModElements.HYDRO.get()) return ModAttributes.HYDRO_BONUS.value();
        if (element == ModElements.DENDRO.get()) return ModAttributes.DENDRO_BONUS.value();
        if (element == ModElements.ELECTRO.get()) return ModAttributes.ELECTRO_BONUS.value();
        if (element == ModElements.ANEMO.get()) return ModAttributes.ANEMO_BONUS.value();
        if (element == ModElements.CYRO.get()) return ModAttributes.CYRO_BONUS.value();
        if (element == ModElements.GEO.get()) return ModAttributes.GEO_BONUS.value();
        if (element == ModElements.FYSIKOS.get()) return ModAttributes.PHYSICAL_BONUS.value();
        return ModAttributes.PHYSICAL_BONUS.value();
    }

    public void upgradeNormalAttack() { data.upgradeNormalAttack(); }
    public void upgradeElementalSkill() { data.upgradeElementalSkill(); }
    public void upgradeElementalBurst() { data.upgradeElementalBurst(); }

    public GenshinElement getElemental() {
        if (elemental != null) return elemental;
        if (elementalId != null && !elementalId.isEmpty()) {
            String[] parts = elementalId.split(":", 2);
            Identifier id = Identifier.fromNamespaceAndPath(parts[0], parts[1]);
            elemental = ModRegistries.ELEMENT_REGISTRY.get(id).map(Holder.Reference::value).orElse(null);
            return elemental;
        }
        return ModElements.FYSIKOS.get();
    }

    public int getSkillShortMaxCooldownTick() { return data.getSkillShortMaxCooldownTick(); }
    public int getSkillLongMaxCooldownTick() { return data.getSkillLongMaxCooldownTick(); }
    public int getBurstMaxCooldownTick() { return data.getBurstMaxCooldownTick(); }
    public float getMaxObtainingEnergy() { return data.getMaxObtainingEnergy(); }

    public float getSkillDisplayCooldown() {
        return data.getElementalSkillCooldownTick();
    }

    public int getSkillDisplayMaxCooldown() {
        return data.getSkillShortMaxCooldownTick();
    }

    public float getBurstDisplayCooldown() {
        return data.getElementalBurstCooldownTick();
    }

    public int getBurstDisplayMaxCooldown() {
        return data.getBurstMaxCooldownTick();
    }

    private void updateBaseStatsFromConfig(int statIndex) {
        for (AttributeType type : this.getStatGrowthTypes()) {
            int value = this.getStatAtLevel(type, statIndex);
            data.setAttributeBaseValue(type, value);
        }
    }

    public void syncRealtimeState() {
        data.syncToClient();
        syncToClient();
    }

    // ==================== 命座 ====================

    /**
     * 命座等级（0 = 0 命，6 = 满命）。
     *
     * <p>字段本身住在 {@link PGCharacterData} 里（{@code @Persisted(key = "constellation")}，
     * 早就有了、只是以前没人用），这里只是给天赋代码一个门面。
     */
    public int getConstellation() {
        return data.getConstellation();
    }

    /**
     * 命座是否达到 {@code level}（1~6）。
     *
     * <p>天赋里判命座一律走这个方法（而不是直接比等级数字），
     * 「必须先解锁某个突破天赋」这类前置条件由天赋自己再判一次。
     */
    public boolean hasConstellation(int level) {
        return data.getConstellation() >= level;
    }

    /** 是否已满命（6 命）。 */
    public boolean isConstellationMax() {
        return data.getConstellation() >= PGCharacterData.MAX_CONSTELLATION;
    }

    /**
     * 提升一级命座 —— 抽到<b>已有</b>角色时调用。
     *
     * @return {@code true} = 这次真的升了一级；{@code false} = 已经满命，
     *         调用方应该改走满命补偿（随机一套圣遗物）
     */
    public boolean addConstellation() {
        if (!data.upgradeConstellation()) {
            return false;
        }
        syncRealtimeState();
        return true;
    }

    /** 直接设置命座等级（命令 / GM 用）。 */
    public void setConstellation(int level) {
        data.setConstellation(level);
        syncRealtimeState();
    }
}