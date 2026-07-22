package com.nuclyon.technicallycoded.inventoryrollback.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.nuclyon.technicallycoded.inventoryrollback.data.LogType;
import com.nuclyon.technicallycoded.inventoryrollback.events.EventLogs;
import com.nuclyon.technicallycoded.inventoryrollback.gui.BackupMenuProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "inventoryrollbackplus", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class IRCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("ir")
                .requires(source -> source.hasPermission(2)) // Equivalent to OP
                .then(Commands.literal("restore")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(IRCommands::executeRestore)))
                .then(Commands.literal("forcebackup")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(IRCommands::executeForceBackup)))
                .then(Commands.literal("help")
                        .executes(IRCommands::executeHelp))
        );
    }

    private static int executeRestore(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            CommandSourceStack source = context.getSource();
            
            if (source.getEntity() instanceof ServerPlayer execPlayer) {
                execPlayer.sendSystemMessage(Component.literal("Opening restore menu for " + targetPlayer.getName().getString() + "..."));
                BackupMenuProvider.openMenu(execPlayer, targetPlayer);
            } else {
                source.sendSuccess(() -> Component.literal("Only players can use this command."), false);
            }
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error executing command."));
            e.printStackTrace();
        }
        return 1;
    }

    private static int executeForceBackup(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            EventLogs.savePlayerInventory(targetPlayer, LogType.FORCE, "Forced by admin");
            context.getSource().sendSuccess(() -> Component.literal("Successfully created forced backup for " + targetPlayer.getName().getString()), true);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error executing command."));
        }
        return 1;
    }

    private static int executeHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("§6InventoryRollbackPlus Commands:"), false);
        source.sendSuccess(() -> Component.literal("§e/ir restore <player> §7- Open a menu to view backups"), false);
        source.sendSuccess(() -> Component.literal("§e/ir forcebackup <player> §7- Create a manual backup"), false);
        return 1;
    }
}
