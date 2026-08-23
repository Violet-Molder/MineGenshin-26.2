package com.linweiyun.genshin.core.command;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.attribute.ModAttributes;
import com.linweiyun.genshin.core.character.PGCharacterData;
import com.linweiyun.genshin.registry.register.CharacterRegister;
import com.linweiyun.genshin.core.character.PGCharacterDefine;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber
public class CharacterCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("pixel_genshin")
                        .then(Commands.literal("character")
                                .then(Commands.literal("add")
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .then(Commands.argument("uuid", IntegerArgumentType.integer())
                                                        .executes(CharacterCommand::addCharacter)
                                                )
                                        )
                                )
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .then(Commands.argument("uuid", IntegerArgumentType.integer())
                                                        .executes(CharacterCommand::removeCharacter)
                                                )
                                        )
                                )
                        )
        );
    }

    private static int addCharacter(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        int uuid = IntegerArgumentType.getInteger(context, "uuid");

        PGCharacterDefine definition = CharacterRegister.getByUUID(uuid);
        if (definition == null) {
            context.getSource().sendFailure(Component.literal("未找到UUID为 " + uuid + " 的角色"));
            return 0;
        }

        PlayerCharactersAttachment attachment = target.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        if (attachment.hasCharacter(uuid)) {
            context.getSource().sendFailure(Component.literal("目标玩家已拥有该角色"));
            return 0;
        }

        double baseHP = definition.getBaseStat(ModAttributes.MAX_HP.value());
        double baseATK = definition.getBaseStat(ModAttributes.ATK.value());
        double baseDEF = definition.getBaseStat(ModAttributes.DEF.value());
        PGCharacterData newChar = new PGCharacterData(uuid, baseHP, baseATK, baseDEF);
        attachment.addCharacterToPlayer(target ,newChar);

        context.getSource().sendSuccess(
                () -> Component.literal("已为 " + target.getName().getString() + " 添加角色 [" + definition.getName().getString() + "]"),
                true);
        return Command.SINGLE_SUCCESS;
    }

    private static int removeCharacter(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        int uuid = IntegerArgumentType.getInteger(context, "uuid");

        PlayerCharactersAttachment attachment = target.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        if (!attachment.hasCharacter(uuid)) {
            context.getSource().sendFailure(Component.literal("目标玩家没有该角色"));
            return 0;
        }

        PGCharacterDefine definition = CharacterRegister.getByUUID(uuid);
        String characterName = definition != null ? definition.getName().getString() : String.valueOf(uuid);

        attachment.removeCharacterToPlayer(target, uuid);

        context.getSource().sendSuccess(
                () -> Component.literal("已从 " + target.getName().getString() + " 移除角色 [" + characterName + "]"),
                true);
        return Command.SINGLE_SUCCESS;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }
}