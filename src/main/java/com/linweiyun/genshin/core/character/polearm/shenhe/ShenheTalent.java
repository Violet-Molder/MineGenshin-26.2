package com.linweiyun.genshin.core.character.polearm.shenhe;

import com.linweiyun.genshin.content.effect.character.CharacterEffectHelper;
import com.linweiyun.genshin.content.effect.character.CharacterEffectInstance;
import com.linweiyun.genshin.content.effect.character.shenhe.IcyQuillEffect;
import com.linweiyun.genshin.content.skill_node.AreaEntityCollector;
import com.linweiyun.genshin.content.skill_node.RushesForward;
import com.linweiyun.genshin.content.skill_node.SkillHelper;
import com.linweiyun.genshin.content.skill_node.math.HorizonEndVec3;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.combat.attack.HurtEntityHelper;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSpec;
import com.linweiyun.genshin.core.system.combat.decay.DecayGroups;
import com.linweiyun.genshin.core.system.registry.register.ModCharacterEffects;
import com.linweiyun.genshin.enums.AttachmentType;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.enums.AttackType;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import com.linweiyun.genshin.core.character.talent.TalentBase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ShenheTalent extends TalentBase {
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final float[] COMBO_MULTIPLIERS = {0.41f, 0.42f, 0.55f, 0.35f, 0.68f};

    public ShenheTalent() {
        super(5,
                List.of(3, 3, 3, 3, 3),
                List.of(20, 20, 10, 10, 12),
                List.of(15, 15, 15, 15, 0));
    }

    public void attack(Player player, PGCharacter character, int comboStage) {
        Level level = player.level();
        if (level.isClientSide()) return;

        int stage = comboStage % this.maxCombo;
        float multiplier = stage < COMBO_MULTIPLIERS.length ? COMBO_MULTIPLIERS[stage] : 1.0f;

        Vec3 startPos = player.position();
        Vec3 lookDir = player.getLookAngle();
        Vec3 endPos = startPos.add(lookDir.scale(2.5f));

        List<LivingEntity> targets = AreaEntityCollector.execute(level, startPos, endPos, 1.0f);

        for (LivingEntity target : targets) {
            if (target != player) {
                ModDamageSpec spec = ModDamageSpec.builder(AttackType.NORMAL_ATTACK, ModElements.CYRO.get())
                        .multiplier(multiplier)
                        .elementAmount(AttachmentType.WEAK.getInitialAmount())
                        .attackerCharacter(character)
                        .build();
                ModDamageSource source = ModDamageSource.from(spec, player);
                if (target.level() instanceof ServerLevel serverLevel) {
                    target.hurtServer(serverLevel, source, 0f);
                }
            }
        }
    }

    public void elementalSkill(Player player, PGCharacter character, int skillType) {
        Level level = player.level();
        PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        AtomicReference<Float> damageMultiplier = new AtomicReference<>(1.0f);
        int skillLevel = character.getData().getElementalSkillLevel();
        if (skillType < 1000) {
            Vec3 startPos = player.position();
            Vec3 rawEndPos  = startPos.add(HorizonEndVec3.execute(player, 2.2f));
            HitResult hit = level.clip(new ClipContext(startPos, rawEndPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 endPos = hit.getLocation();
            RushesForward.execute(player, 2.2f);
            List<LivingEntity> entities = new ArrayList<>();
            entities = AreaEntityCollector.execute(level, startPos, endPos, 0.5f);
            entities.forEach(
                    entity -> {
                        if (entity != player) {
                            damageMultiplier.set(0.0024f * skillLevel * skillLevel + 0.098f * skillLevel + 1.29f);
                            ModDamageSpec spec = ModDamageSpec.builder(AttackType.ELEMENTAL_SKILL, ModElements.CYRO.get())
                                    .multiplier(damageMultiplier.get())
                                    .elementAmount(AttachmentType.WEAK.getInitialAmount())
                                    .decayGroup(DecayGroups.SHENHE_SKILL)
                                    .attackerCharacter(character)
                                    .build();
                            ModDamageSource source = ModDamageSource.from(spec, player);
                            if (entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                                entity.hurtServer(serverLevel, source, 0f);
                            }
                        }

                    }
            );
            if (!level.isClientSide()) {
                for (int i = 0; i < 4; i++) {
                    PGCharacter partyChar = attachment.getPartyCharacter(i);
                    if (partyChar != null) {
                        CharacterEffectInstance effect = new CharacterEffectInstance(ModCharacterEffects.ICY_QUILL_EFFECT.get(), 350, 1);
                        effect.setIntData(IcyQuillEffect.ICY_QUILL_COUNT_KEY, 7);
                        CharacterEffectHelper.addEffect(player, partyChar, effect);
                    }
                }
            }
            // 短暂冻结按键输入
            SkillHelper.addStun(player, 10);
        } else {
            player.sendSystemMessage(Component.literal("长按"));
        }
    }
    public void elementalBurst(Player player, PGCharacter character) {
        int burstLevel = character.getData().getElementalBurstLevel();
        float damageMultiplier = 0.103f * burstLevel + 0.89f;
    }
 }