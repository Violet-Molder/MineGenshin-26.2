package com.linweiyun.genshin.core.command;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.network.NetworkManager;
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
public class PrimogemCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("minegenshin")
                        .then(Commands.literal("primogem")
                                .then(Commands.literal("get")
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(PrimogemCommand::getPrimogem)
                                        )
                                )
                                .then(Commands.literal("set")
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                        .executes(PrimogemCommand::setPrimogem)
                                                )
                                        )
                                )
                                .then(Commands.literal("add")
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                        .executes(PrimogemCommand::addPrimogem)
                                                )
                                        )
                                )
                        )
        );
    }

    private static int getPrimogem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        int primogem = target.getData(AttachmentRegistration.PRIMOGEM_ATTACHMENT);

        context.getSource().sendSuccess(
                () -> Component.translatable("command.minegenshin.primogem.get", target.getName().getString(), primogem),
                false);
        return primogem;
    }

    private static int setPrimogem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        target.setData(AttachmentRegistration.PRIMOGEM_ATTACHMENT, amount);
        NetworkManager.setPrimogemToPlayer(target, amount);

        context.getSource().sendSuccess(
                () -> Component.translatable("command.minegenshin.primogem.set", target.getName().getString(), amount),
                true);
        return Command.SINGLE_SUCCESS;
    }

    private static int addPrimogem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        int current = target.getData(AttachmentRegistration.PRIMOGEM_ATTACHMENT);
        int newAmount = current + amount;
        target.setData(AttachmentRegistration.PRIMOGEM_ATTACHMENT, newAmount);
        NetworkManager.setPrimogemToPlayer(target, newAmount);

        int finalNewAmount = newAmount;
        context.getSource().sendSuccess(
                () -> Component.translatable("command.minegenshin.primogem.add", target.getName().getString(), amount, finalNewAmount),
                true);
        return Command.SINGLE_SUCCESS;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }
}