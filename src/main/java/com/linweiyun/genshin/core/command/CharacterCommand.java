package com.linweiyun.genshin.core.command;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.system.registry.register.ModCharacters;
import com.linweiyun.genshin.core.character.PGCharacter;
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

        PGCharacter character = ModCharacters.getByUUID(uuid);
        if (character == null) {
            context.getSource().sendFailure(Component.translatable("command.minegenshin.character.not_found", uuid));
            return 0;
        }

        PlayerCharactersAttachment attachment = target.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        if (attachment.hasCharacter(uuid)) {
            context.getSource().sendFailure(Component.translatable("command.minegenshin.character.already_owned"));
            return 0;
        }
        attachment.addCharacterToPlayer(target ,character);

        context.getSource().sendSuccess(
                () -> Component.translatable("command.minegenshin.character.add_success", target.getName().getString(), character.getName().getString()),
                true);
        return Command.SINGLE_SUCCESS;
    }

    private static int removeCharacter(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        int uuid = IntegerArgumentType.getInteger(context, "uuid");

        PlayerCharactersAttachment attachment = target.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        if (!attachment.hasCharacter(uuid)) {
            context.getSource().sendFailure(Component.translatable("command.minegenshin.character.not_owned"));
            return 0;
        }

        PGCharacter definition = ModCharacters.getByUUID(uuid);
        String characterName = definition != null ? definition.getName().getString() : String.valueOf(uuid);

        attachment.removeCharacterToPlayer(target, uuid);

        context.getSource().sendSuccess(
                () -> Component.translatable("command.minegenshin.character.remove_success", target.getName().getString(), characterName),
                true);
        return Command.SINGLE_SUCCESS;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }
}