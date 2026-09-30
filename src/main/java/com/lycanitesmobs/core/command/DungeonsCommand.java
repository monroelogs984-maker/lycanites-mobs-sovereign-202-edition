package com.lycanitesmobs.core.command;

import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.data.config.ConfigDungeons;
import com.lycanitesmobs.core.manager.DungeonManager;
import com.lycanitesmobs.core.worldgen.dungeon.definition.DungeonSchematic;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;

/**
 * /lm dungeons reload|enable|disable|locate <name>
 *
 * Port: locate asks the chunk generator for the nearest placement of that dungeon's structure (as /locate structure
 * does). The official searched for the ExtendedWorld dungeon instances of the disabled legacy generator, then fell back
 * to recomputing a per-schematic grid that no longer matches the single combined structure set dungeons are placed on.
 */
public class DungeonsCommand {
    private static final SuggestionProvider<CommandSourceStack> DUNGEON_SUGGESTIONS = (context, builder) -> {
        String remaining = builder.getRemaining().toLowerCase();
        for (DungeonSchematic schematic : DungeonManager.getInstance().getSchematics()) {
            if (schematic.getName().startsWith(remaining)) {
                builder.suggest(schematic.getName());
            }
        }
        return builder.buildFuture();
    };

    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("dungeons")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload").executes(DungeonsCommand::reload))
                .then(Commands.literal("enable").executes(DungeonsCommand::enable))
                .then(Commands.literal("disable").executes(DungeonsCommand::disable))
                .then(Commands.literal("locate")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(DUNGEON_SUGGESTIONS)
                                .executes(DungeonsCommand::locate)));
    }

    public static int reload(final CommandContext<CommandSourceStack> context) {
        DungeonManager.getInstance().reload();
        context.getSource().sendSuccess(() -> Component.translatable("lyc.command.dungeons.reload"), true);
        return 1;
    }

    public static int enable(final CommandContext<CommandSourceStack> context) {
        ConfigDungeons.INSTANCE.dungeonsEnabled.set(true);
        ConfigDungeons.INSTANCE.dungeonsEnabled.save();
        context.getSource().sendSuccess(() -> Component.translatable("lyc.command.dungeons.enable"), true);
        return 1;
    }

    public static int disable(final CommandContext<CommandSourceStack> context) {
        ConfigDungeons.INSTANCE.dungeonsEnabled.set(false);
        ConfigDungeons.INSTANCE.dungeonsEnabled.save();
        context.getSource().sendSuccess(() -> Component.translatable("lyc.command.dungeons.disable"), true);
        return 1;
    }

    public static int locate(final CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        String name = StringArgumentType.getString(context, "name").toLowerCase();

        if (DungeonManager.getInstance().getSchematic(name) == null) {
            source.sendFailure(Component.literal("Unknown dungeon schematic: \"" + name + "\""));
            return 0;
        }

        ResourceKey<Structure> structureKey = ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, name));
        Optional<Holder.Reference<Structure>> structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(structureKey);
        if (structure.isEmpty()) {
            source.sendFailure(Component.literal("The \"" + name + "\" dungeon has no world generation structure (it is disabled or has no biomes)."));
            return 0;
        }

        BlockPos origin = BlockPos.containing(source.getPosition());
        Pair<BlockPos, Holder<Structure>> result = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, HolderSet.direct(structure.get()), origin, 100, false);
        if (result == null) {
            source.sendFailure(Component.literal("No \"" + name + "\" dungeon can generate near here in this dimension."));
            return 0;
        }

        BlockPos pos = result.getFirst();
        int distance = (int) Math.sqrt(pos.distSqr(new BlockPos(origin.getX(), pos.getY(), origin.getZ())));
        String tp = "/tp @s " + pos.getX() + " ~ " + pos.getZ();
        Component coords = Component.literal("[" + pos.getX() + ", ~, " + pos.getZ() + "]")
                .withStyle(style -> style
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, tp))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to teleport"))));
        source.sendSuccess(() -> Component.literal("Nearest " + name + " is at ").append(coords)
                .append(Component.literal(" (" + distance + " blocks away)")), false);
        return 1;
    }
}
