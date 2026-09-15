package com.linweiyun.genshin.core.character.talent;

import com.linweiyun.genshin.core.character.PGCharacter;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class TalentBase {
    protected final int maxCombo;
    protected final List<Integer> precastDurations;
    protected final List<Integer> postcastDurations;
    protected final List<Integer> windowDurations;

    protected int chargeTicks = 20;
    protected int chargedPrecastTicks = 5;
    protected int chargedPostcastTicks = 15;

    public TalentBase(int maxCombo, List<Integer> precastDurations,
                      List<Integer> postcastDurations, List<Integer> windowDurations) {
        this.maxCombo = maxCombo;
        this.precastDurations = precastDurations;
        this.postcastDurations = postcastDurations;
        this.windowDurations = windowDurations;
    }

    public TalentBase(int maxCombo, List<Integer> precastDurations,
                      List<Integer> postcastDurations, List<Integer> windowDurations,
                      int chargeTicks, int chargedPrecastTicks, int chargedPostcastTicks) {
        this.maxCombo = maxCombo;
        this.precastDurations = precastDurations;
        this.postcastDurations = postcastDurations;
        this.windowDurations = windowDurations;
        this.chargeTicks = chargeTicks;
        this.chargedPrecastTicks = chargedPrecastTicks;
        this.chargedPostcastTicks = chargedPostcastTicks;
    }

    public int getMaxCombo() {
        return maxCombo;
    }

    public int getPrecastTicks(int stage) {
        if (stage >= 0 && stage < precastDurations.size()) {
            return precastDurations.get(stage);
        }
        return 0;
    }

    public int getPostcastTicks(int stage) {
        if (stage >= 0 && stage < postcastDurations.size()) {
            return postcastDurations.get(stage);
        }
        return 0;
    }

    public int getWindowTicks(int stage) {
        if (stage >= 0 && stage < windowDurations.size()) {
            return windowDurations.get(stage);
        }
        return 0;
    }

    public int getChargeTicks() {
        return chargeTicks;
    }

    public int getChargedPrecastTicks() {
        return chargedPrecastTicks;
    }

    public int getChargedPostcastTicks() {
        return chargedPostcastTicks;
    }

    public void attack(Player player, PGCharacter character, int comboStage) {}

    public void chargeAttack(Player player, PGCharacter character) {}

    public void elementalSkill(Player player, PGCharacter character, int skillTime) {}

    public void elementalBurst(Player player, PGCharacter character) {}
}