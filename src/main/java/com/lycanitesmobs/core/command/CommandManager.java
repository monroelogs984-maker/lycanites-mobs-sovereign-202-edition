package com.lycanitesmobs.core.command;

import net.minecraft.commands.Commands;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Registers the /lm command tree. TODO(port): spawners, spawner, mobevents, mobevent, dungeons, equipment and debug
 * subcommands come with their systems.
 */
public class CommandManager {
    public static void register() {
        NeoForge.EVENT_BUS.addListener(CommandManager::registerCommands);
    }

    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("lm")
                        .then(CreaturesCommand.register())
                        .then(BeastiaryCommand.register())
        );
    }
}
