package com.linweiyun.genshin.core.system.reaction;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.character.catalyst.columbina.Columbina;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.status.StatusInstance;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentInstance;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class ReactionPriorityCalculator {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final GenshinElement[] DEFAULT_ORDER = {
            ModElements.ANEMO.get(), ModElements.CYRO.get(), ModElements.ELECTRO.get(),
            ModElements.HYDRO.get(), ModElements.FROZEN.get(), ModElements.PYRO.get(),
            ModElements.DENDRO.get(), ModElements.AGGRAVATE.get(), ModElements.GEO.get()
    };

    public static int computeFor(GenshinElement attackerElement,
                                 GenshinElement defenderElement,
                                 ElementalReaction reaction) {
        GenshinElement defMain = defenderElement.getMainElement();
        int baseIdx = indexOf(defMain);
        if (baseIdx < 0) baseIdx = 50;
        return baseIdx;
    }

    private static int indexOf(GenshinElement mainElement) {
        for (int i = 0; i < DEFAULT_ORDER.length; i++) {
            if (DEFAULT_ORDER[i] == mainElement) return i;
        }
        return -1;
    }

    public static boolean hasFrozen(StatusContainer container) {
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (inst instanceof ElementalAttachmentInstance ea
                    && ea.getElement() == ModElements.FROZEN.get()) return true;
        }
        return false;
    }

    public static boolean hasAggravate(StatusContainer container) {
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (inst instanceof ElementalAttachmentInstance ea
                    && ea.getElement() == ModElements.AGGRAVATE.get()) return true;
        }
        return false;
    }

    public static boolean hasColumbinaInParty(ReactionContext context) {
        Entity attacker = context.attackerEntity();
        if (!(attacker instanceof Player player)) return false;
        if (!(player.level() instanceof ServerLevel level)) return false;
        return hasColumbinaInParty(level);
    }

    public static boolean hasColumbinaInParty(ServerLevel level) {
        for (Player p : level.players()) {
            PlayerCharactersAttachment att = p.getData(
                    AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            for (int i = 0; i < 4; i++) {
                PGCharacter character = att.getPartyCharacter(i);
                if (character instanceof Columbina) return true;
            }
        }
        return false;
    }

    /**
     * 队伍里有没有<b>星扩散户口</b> —— 有的话冰扩散要转成星扩散。
     *
     * <p>注意查的是户口（转化 + 体系加成，写在同一个天赋里），
     * 不是「能进入星扩散状态」的角色（那个是 {@code IStellarStateHolder}，两回事）。
     */
    public static boolean hasStellarSwirlHousehold(ServerLevel level) {
        return StellarGlimmer.swirlHousehold(level) != null;
    }

    public static boolean hasStellarSwirlParticipant(ServerLevel level) {
        return hasStellarSwirlHousehold(level);
    }

    /** 队伍里能进入星烁状态的角色（和户口无关）。 */
    public static List<PGCharacter> getStellarStateHolders(ServerLevel level) {
        List<PGCharacter> result = new ArrayList<>();
        for (Player p : level.players()) {
            PlayerCharactersAttachment att = p.getData(
                    AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            for (int i = 0; i < 4; i++) {
                PGCharacter character = att.getPartyCharacter(i);
                if (character instanceof com.linweiyun.genshin.core.character.IStellarStateHolder holder
                        && holder.canHoldStellarState()) {
                    result.add(character);
                }
            }
        }
        return result;
    }
}