package com.lycanitesmobs.core.command;

import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.spawner.SpawnBudget;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.TreeMap;

/**
 * S202 spawn budget tools: /lm spawning stats (natural creatures around you vs the budget, by species, and the latest
 * budget spawns) and /lm spawning tick (runs one budget check at your position now).
 */
public class SpawningCommand {
	public static ArgumentBuilder<CommandSourceStack, ?> register() {
		return Commands.literal("spawning")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("stats").executes(SpawningCommand::stats))
				.then(Commands.literal("tick").executes(SpawningCommand::tick));
	}

	private static int stats(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		var natural = SpawnBudget.getNaturalCreatures(player.level(), player.blockPosition());
		Map<String, Integer> species = new TreeMap<>();
		for (BaseCreatureEntity creature : natural) {
			species.merge(creature.getCreatureInfo().getName(), 1, Integer::sum);
		}
		int surface = SpawnBudget.getCount(player.level(), player.blockPosition(), true);
		int below = SpawnBudget.getCount(player.level(), player.blockPosition(), false);
		context.getSource().sendSuccess(() -> Component.literal("Spawn budget " + (SpawnBudget.isEnabled() ? "on" : "OFF") + " within "
				+ (int) SpawnBudget.getRange() + " blocks: surface " + surface + " (adds below " + SpawnBudget.getMin(true) + ", max " + SpawnBudget.getMax(true)
				+ "), caves/water " + below + " (adds below " + SpawnBudget.getMin(false) + ", max " + SpawnBudget.getMax(false) + ")").withStyle(ChatFormatting.GOLD), false);
		context.getSource().sendSuccess(() -> Component.literal("Nearby: " + (species.isEmpty() ? "none" : species.toString())), false);
		context.getSource().sendSuccess(() -> Component.literal("Placement failures: " + SpawnBudget.getFailures()).withStyle(ChatFormatting.GRAY), false);
		long now = player.level().getGameTime();
		for (SpawnBudget.Record record : SpawnBudget.getHistory().subList(0, Math.min(8, SpawnBudget.getHistory().size()))) {
			context.getSource().sendSuccess(() -> Component.literal("  " + ((now - record.gameTime()) / 20) + "s ago: " + record.spawned() + "x "
					+ record.creature() + " via " + record.spawner() + " (count was " + record.countBefore() + ", " + String.format("%.1f", record.millis()) + " ms)").withStyle(ChatFormatting.GRAY), false);
		}
		return 1;
	}

	private static int tick(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		boolean spawned = SpawnBudget.attempt(player.level(), player.blockPosition());
		context.getSource().sendSuccess(() -> Component.literal(spawned ? "Budget spawned a group." : "Nothing spawned (at budget, or no candidates here)."), false);
		return 1;
	}
}
