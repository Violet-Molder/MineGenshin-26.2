package com.linweiyun.genshin.core.character.talent;

import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.network.ActionServer;
import com.linweiyun.genshin.core.system.combat.action.ActionDefinition;
import com.linweiyun.genshin.core.system.combat.action.ActionKind;
import com.linweiyun.genshin.core.system.combat.action.ActionSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class TalentBase {

    public int getMaxCombo() { return 1; }

    public int getPrecastTicks(int stage)  { return 5; }
    public int getActiveTicks(int stage)   { return 1; }
    public int getPostcastTicks(int stage) { return 6; }

    public int getChargeTicks() { return 20; }
    public int getChargedPrecastTicks()  { return 5; }
    public int getChargedActiveTicks()   { return 1; }
    public int getChargedPostcastTicks() { return 15; }

    public int getSkillPrecastTicks()  { return 5; }
    public int getSkillActiveTicks()   { return 1; }
    public int getSkillPostcastTicks() { return 10; }

    public int getBurstPrecastTicks()  { return 10; }
    public int getBurstActiveTicks()   { return 1; }
    public int getBurstPostcastTicks() { return 20; }

    /** comboStage 从 1 开始 */
    public void attack(Player player, PGCharacter character, int comboStage) {}
    public void chargeAttack(Player player, PGCharacter character) {}
    public void elementalSkill(Player player, PGCharacter character, int skillTime) {}
    public void elementalBurst(Player player, PGCharacter character) {}

    public record Timing(int precast, int active, int postcast) {}

    protected Timing timing(int precast, int active, int postcast) {
        return new Timing(precast, active, postcast);
    }

    protected SetBuilder setBuilder() { return new SetBuilder(null); }
    protected SetBuilder setBuilder(ActionSet parent) { return new SetBuilder(parent); }

    public final class SetBuilder {
        private final ActionSet.Builder inner;

        private SetBuilder(ActionSet parent) {
            this.inner = (parent != null) ? ActionSet.deriveFrom(parent) : ActionSet.builder();
        }

        /** 服务端 swing 广播（仅玩家自己） */
        private void broadcastSwing(Player player) {
            if (player instanceof ServerPlayer sp) {
                ActionServer.sendSwingToPlayer(sp);
            }
        }

        public SetBuilder normalCombo(Timing... timings) {
            inner.clearNormalCombo();
            for (int i = 0; i < timings.length; i++) {
                final int stage = i + 1;
                Timing t = timings[i];
                inner.addNormalAttack(
                        ActionDefinition.builder(ActionKind.NORMAL_ATTACK)
                                .comboIndex(stage)
                                .precast(t.precast())
                                .active(t.active())
                                .postcast(t.postcast())
                                .onActiveStart(ctx -> {
                                    broadcastSwing(ctx.player);
                                    attack(ctx.player, ctx.character, stage);
                                })
                                .build()
                );
            }
            return this;
        }

        public SetBuilder charged(Timing t) {
            inner.chargedAttack(
                    ActionDefinition.builder(ActionKind.CHARGED_ATTACK)
                            .precast(t.precast()).active(t.active()).postcast(t.postcast())
                            .onActiveStart(ctx -> {
                                broadcastSwing(ctx.player);
                                chargeAttack(ctx.player, ctx.character);
                            })
                            .build()
            );
            return this;
        }

        public SetBuilder skillTap(Timing t) {
            inner.elementalSkillTap(
                    ActionDefinition.builder(ActionKind.ELEMENTAL_SKILL_TAP)
                            .precast(t.precast()).active(t.active()).postcast(t.postcast())
                            .onActiveStart(ctx -> ctx.character.performElementalSkill(ctx.player, 0))
                            .build()
            );
            return this;
        }

        public SetBuilder skillHold(Timing t) {
            inner.elementalSkillHold(
                    ActionDefinition.builder(ActionKind.ELEMENTAL_SKILL_HOLD)
                            .precast(t.precast()).active(t.active()).postcast(t.postcast())
                            .onActiveStart(ctx -> ctx.character.performElementalSkill(ctx.player, 1000))
                            .build()
            );
            return this;
        }

        public SetBuilder burst(Timing t) {
            inner.elementalBurst(
                    ActionDefinition.builder(ActionKind.ELEMENTAL_BURST)
                            .precast(t.precast()).active(t.active()).postcast(t.postcast())
                            .onActiveStart(ctx -> ctx.character.performElementalBurst(ctx.player))
                            .build()
            );
            return this;
        }

        public ActionSet build() { return inner.build(); }
    }

    public ActionSet buildActionSet(PGCharacter character, String stateKey) {
        return buildDefaultActionSet(character);
    }

    protected ActionSet buildDefaultActionSet(PGCharacter character) {
        int maxCombo = getMaxCombo();
        Timing[] comboTimings = new Timing[maxCombo];

        for (int stage = 1; stage <= maxCombo; stage++) {
            comboTimings[stage - 1] = timing(
                    getPrecastTicks(stage),
                    getActiveTicks(stage),
                    getPostcastTicks(stage));
        }

        return setBuilder()
                .normalCombo(comboTimings)
                .charged(timing(getChargedPrecastTicks(), getChargedActiveTicks(), getChargedPostcastTicks()))
                .skillTap(timing(getSkillPrecastTicks(), getSkillActiveTicks(), getSkillPostcastTicks()))
                .skillHold(timing(getSkillPrecastTicks(), getSkillActiveTicks(), getSkillPostcastTicks()))
                .burst(timing(getBurstPrecastTicks(), getBurstActiveTicks(), getBurstPostcastTicks()))
                .build();
    }
}