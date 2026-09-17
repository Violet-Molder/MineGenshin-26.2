package com.linweiyun.genshin.content.entities.area;

import com.linweiyun.genshin.config.reaction.ReactionConfig;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.system.about.AttachmentProfile;
import com.linweiyun.genshin.core.system.about.AttachmentSource;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSpec;
import com.linweiyun.genshin.enums.ElementalReactionType;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.*;

public class StellarVortexEntity extends AreaEntity {
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final int EXPLODE_TIMER = 60;
    private static final int MAX_LEVEL = 6;
    private static final float DEFAULT_HORIZONTAL_RANGE = 5.0f;
    private static final float LEVEL3_HORIZONTAL_RANGE = 7.0f;
    private static final int LEVEL3_THRESHOLD = 3;
    private static final float ICE_ATTACH_QUANTITY = 1.0f;

    @Persisted(key = "sv_level")
    private int vortexLevel = 1;

    @Persisted(key = "sv_explode_timer")
    private int explodeTimer = EXPLODE_TIMER;

    @Persisted(key = "sv_exploded")
    private boolean exploded;

    private final List<PGCharacter> contributors = new ArrayList<>();

    private transient PGCharacter lastTopContributor;

    public StellarVortexEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.duration = EXPLODE_TIMER + 10;
        this.shapeType = AreaShapeType.CYLINDER;
        this.horizontalRadius = DEFAULT_HORIZONTAL_RANGE;
        this.verticalRadius = 3.0f;
    }

    public PGCharacter getOwnerCharacter() {
        return lastTopContributor;
    }

    public Player resolveOwnerPlayer(ServerLevel level) {
        if (lastTopContributor == null) return null;
        int targetUuid = lastTopContributor.getCharacterUUID();
        for (Player p : level.players()) {
            PlayerCharactersAttachment att = p.getData(
                    AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            for (int i = 0; i < 4; i++) {
                PGCharacter c = att.getPartyCharacter(i);
                if (c != null && c.getCharacterUUID() == targetUuid) {
                    return p;
                }
            }
        }
        return null;
    }

    public void addContributor(PGCharacter character) {
        if (character != null && !contributors.contains(character)) {
            contributors.add(character);
            LOGGER.info("[星辉风旋] 添加贡献者 char={} total={}", character.getName(), contributors.size());
        } else {
            LOGGER.info("[星辉风旋] 跳过贡献者 char={} isNull={} contains={}",
                    character != null ? character.getName() : "null",
                    character == null,
                    character != null ? contributors.contains(character) : "N/A");
        }
    }

    public void addAllContributors(Collection<PGCharacter> chars) {
        for (PGCharacter c : chars) {
            addContributor(c);
        }
    }

    public List<PGCharacter> getContributors() {
        return Collections.unmodifiableList(contributors);
    }

    public void setLevel(int level) {
        this.vortexLevel = level;
    }

    public int getVortexLevel() {
        return vortexLevel;
    }

    public void incrementLevel() {
        this.vortexLevel = Math.min(MAX_LEVEL, vortexLevel + 1);
        if (this.vortexLevel >= LEVEL3_THRESHOLD) {
            this.horizontalRadius = LEVEL3_HORIZONTAL_RANGE;
            this.setHorizontalRadius(LEVEL3_HORIZONTAL_RANGE);
        }
        if (this.vortexLevel >= MAX_LEVEL) {
            explode();
        }
        LOGGER.info("[星辉风旋] 等级提升至 {} | pos={}", vortexLevel, this.position());
    }

    private void explode() {
        if (exploded) return;
        exploded = true;

        if (!(this.level() instanceof ServerLevel level)) return;

        double iceCoefficient = vortexLevel <= 2
                ? ReactionConfig.STELLAR_SWIRL_ICE_COEFFICIENT_LOW.get()
                : ReactionConfig.STELLAR_SWIRL_ICE_COEFFICIENT_HIGH.get();

        AABB area = getAreaOfInfluence();
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, area, e -> e.isAlive() && !e.equals(this));

        List<PGCharacter> contributorList = new ArrayList<>(contributors);
        if (contributorList.isEmpty()) {
            LOGGER.warn("[星辉风旋] 爆炸时无贡献者，跳过");
            this.discard();
            return;
        }
        this.lastTopContributor = contributorList.isEmpty() ? null : contributorList.get(0);
        Player ownerPlayer = resolveOwnerPlayer(level);
        if (ownerPlayer == null) {
            LOGGER.warn("[星辉风旋] 爆炸时无法解析拥有者玩家，跳过");
            this.discard();
            return;
        }

        for (LivingEntity target : targets) {
            ModDamageSpec spec = buildStellarSpec(
                    ElementalReactionType.STELLAR_SWIRL_ICE,
                    ModElements.CYRO.get(),
                    iceCoefficient, contributorList);
            ModDamageSource source = ModDamageSource.from(spec, ownerPlayer);
            target.hurtServer(level, source, 0f);

            StatusContainer container = target.getData(AttachmentRegistration.CONTAINER);
            ElementalAttachmentHelper.attach(
                    target, container, ModElements.CYRO.get(),
                    AttachmentSource.SPECIAL,
                    createIceAttachmentProfile());
        }

        LOGGER.info("[星辉风旋] 爆炸 level={} targets={} iceCoefficient={}",
                vortexLevel, targets.size(), iceCoefficient);

        this.discard();
    }

    public void triggerWindDamage() {
        if (!(this.level() instanceof ServerLevel level)) return;

        double windCoefficient = ReactionConfig.STELLAR_SWIRL_WIND_COEFFICIENT.get();

        AABB area = getAreaOfInfluence();
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, area, e -> e.isAlive() && !e.equals(this));

        List<PGCharacter> contributorList = new ArrayList<>(contributors);
        if (contributorList.isEmpty()) {
            LOGGER.warn("[星辉风旋] 风伤时无贡献者，跳过");
            return;
        }
        this.lastTopContributor = contributorList.isEmpty() ? null : contributorList.get(0);
        Player ownerPlayer = resolveOwnerPlayer(level);
        if (ownerPlayer == null) {
            LOGGER.warn("[星辉风旋] 风伤时无法解析拥有者玩家，跳过");
            return;
        }

        for (LivingEntity target : targets) {
            ModDamageSpec spec = buildStellarSpec(
                    ElementalReactionType.STELLAR_SWIRL_WIND,
                    ModElements.ANEMO.get(),
                    windCoefficient, contributorList);
            ModDamageSource source = ModDamageSource.from(spec, ownerPlayer);
            target.hurtServer(level, source, 0f);
        }

        LOGGER.info("[星辉风旋] 风伤 level={} targets={}", vortexLevel, targets.size());
    }

    private static ModDamageSpec buildStellarSpec(ElementalReactionType reactionType,
                                                   GenshinElement element,
                                                   double coefficient,
                                                   List<PGCharacter> contributors) {
        ModDamageSpec spec = ModDamageSpec.stellar(reactionType, element);

        try {
            java.lang.reflect.Field cField = ModDamageSpec.class.getDeclaredField("stellarCoefficient");
            cField.setAccessible(true);
            cField.set(spec, (float) coefficient);

            java.lang.reflect.Field bmField = ModDamageSpec.class.getDeclaredField("stellarBaseBonusMult");
            bmField.setAccessible(true);
            bmField.set(spec, 0f);

            java.lang.reflect.Field bfField = ModDamageSpec.class.getDeclaredField("stellarBaseBonusFlat");
            bfField.setAccessible(true);
            bfField.set(spec, 0f);
        } catch (Exception e) {
            LOGGER.error("[星辉风旋] 反射设置spec参数失败", e);
        }

        spec.setStellarContributors(contributors);
        return spec;
    }

    @Override
    protected void serverTick() {
        super.serverTick();
        if (this.isRemoved() || exploded) return;

        explodeTimer--;
        if (explodeTimer <= 0) {
            explode();
        }
    }

    public AABB getAreaOfInfluence() {
        double hw = horizontalRadius;
        double hh = verticalRadius;
        return new AABB(
                position().x - hw, position().y - hh, position().z - hw,
                position().x + hw, position().y + hh, position().z + hw);
    }

    public boolean containsPos(double x, double y, double z) {
        double hw = horizontalRadius;
        double hh = verticalRadius;
        return x >= position().x - hw && x <= position().x + hw
                && y >= position().y - hh && y <= position().y + hh
                && z >= position().z - hw && z <= position().z + hw;
    }

    public static StellarVortexEntity findExisting(ServerLevel level, double x, double y, double z, float range) {
        List<StellarVortexEntity> all = level.getEntitiesOfClass(
                StellarVortexEntity.class,
                new AABB(x - range, y - range, z - range, x + range, y + range, z + range),
                e -> e.isAlive() && !e.exploded);
        for (StellarVortexEntity v : all) {
            if (v.containsPos(x, y, z)) return v;
        }
        return null;
    }

    private static AttachmentProfile createIceAttachmentProfile() {
        return new AttachmentProfile(ICE_ATTACH_QUANTITY, 1.0f,
                ICE_ATTACH_QUANTITY / 9.5f, 9.5f);
    }
}