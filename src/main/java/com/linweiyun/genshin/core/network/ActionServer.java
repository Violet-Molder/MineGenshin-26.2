package com.linweiyun.genshin.core.network;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.combat.action.ActionManager;
import com.linweiyun.genshin.core.system.combat.action.InterruptReason;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import net.minecraft.server.level.ServerPlayer;

public final class ActionServer {

    private ActionServer() {}

    @RPCPacket("characterNormalAttackRPCPacket")
    public static void characterNormalAttackRPCPacket(RPCSender sender, int comboStage) {
        if (!sender.isServer()) {
            ServerPlayer sp = sender.asPlayer();
            if (sp == null) return;
            PlayerCharactersAttachment attachment =
                    sp.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            PGCharacter character = attachment.getCurrentCharacter();
            if (character != null) {
                ActionManager.get(sp).requestNormalAttack(sp, character);
            }
        }
    }

    public static void performNormalAttackToServer(int comboStage) {
        RPCPacketDistributor.rpcToServer("characterNormalAttackRPCPacket", comboStage);
    }

    @RPCPacket("characterChargedAttackRPCPacket")
    public static void characterChargedAttackRPCPacket(RPCSender sender) {
        if (!sender.isServer()) {
            ServerPlayer sp = sender.asPlayer();
            if (sp == null) return;
            PlayerCharactersAttachment attachment =
                    sp.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            PGCharacter character = attachment.getCurrentCharacter();
            if (character != null) {
                ActionManager.get(sp).requestChargedAttack(sp, character);
            }
        }
    }

    public static void performChargedAttackToServer() {
        RPCPacketDistributor.rpcToServer("characterChargedAttackRPCPacket");
    }

    /** E 短按/长按：直接传 int isLong 给 ActionManager（-1 或 <1000 短按，>=1000 长按） */
    @RPCPacket("characterActiveSkillRPCPacket")
    public static void characterActiveSkillRPCPacket(RPCSender sender, int isLong) {
        if (!sender.isServer()) {
            ServerPlayer sp = sender.asPlayer();
            if (sp == null) return;
            PlayerCharactersAttachment attachment =
                    sp.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            PGCharacter character = attachment.getCurrentCharacter();
            if (character != null) {
                ActionManager.get(sp).requestElementalSkill(sp, character, isLong);
            }
        }
    }

    public static void triggerCharacterSkill(int isLong) {
        RPCPacketDistributor.rpcToServer("characterActiveSkillRPCPacket", isLong);
    }

    @RPCPacket("characterActiveBurstRPCPacket")
    public static void characterActiveBurstRPCPacket(RPCSender sender) {
        if (!sender.isServer()) {
            ServerPlayer sp = sender.asPlayer();
            if (sp == null) return;
            PlayerCharactersAttachment attachment =
                    sp.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            PGCharacter character = attachment.getCurrentCharacter();
            if (character != null) {
                ActionManager.get(sp).requestElementalBurst(sp, character);
            }
        }
    }

    public static void triggerCharacterBurst() {
        RPCPacketDistributor.rpcToServer("characterActiveBurstRPCPacket");
    }

    @RPCPacket("characterInterruptRPCPacket")
    public static void characterInterruptRPCPacket(RPCSender sender, int reasonOrdinal) {
        if (!sender.isServer()) {
            ServerPlayer sp = sender.asPlayer();
            if (sp == null) return;
            InterruptReason[] all = InterruptReason.values();
            if (reasonOrdinal < 0 || reasonOrdinal >= all.length) return;
            ActionManager.get(sp).interrupt(all[reasonOrdinal]);
        }
    }

    public static void interruptActionToServer(int reasonOrdinal) {
        RPCPacketDistributor.rpcToServer("characterInterruptRPCPacket", reasonOrdinal);
    }

    // ========================================================================
    // 【服务端 → 客户端】播放挥剑动画
    // 由 ActionState 在服务端进入 ACTIVE 阶段时调用，让动画与伤害同 tick 触发。
    // ========================================================================

    @RPCPacket("actionSwingRPCPacket")
    public static void actionSwingRPCPacket(RPCSender sender) {
        if (sender.isServer()) {
            ActionClient.actionSwingClientHandler();
        }
    }

    /** 服务端主动调用：让指定玩家播放一次挥剑动画 */
    public static void actionSwingToPlayer(ServerPlayer player) {
        RPCPacketDistributor.rpcToPlayer(player, "actionSwingRPCPacket");
    }
}