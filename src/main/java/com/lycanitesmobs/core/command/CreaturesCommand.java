package com.lycanitesmobs.core.command;

import com.lycanitesmobs.core.data.info.creature.CreatureInfo;
import com.lycanitesmobs.core.data.info.creature.CreatureSpawn;
import com.lycanitesmobs.core.manager.CreatureManager;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CreaturesCommand {
	public static ArgumentBuilder<CommandSourceStack, ?> register() {
		return Commands.literal("creatures")
				.then(Commands.literal("reload").executes(CreaturesCommand::reload))
				.then(Commands.literal("climate").executes(CreaturesCommand::climate));
	}

	public static int reload(final CommandContext<CommandSourceStack> context) {
		if (!context.getSource().hasPermission(2)) {
			return 0;
		}
		CreatureManager.getInstance().reload();
		context.getSource().sendSuccess(() -> Component.translatable("lyc.command.creatures.reload"), true);
		return 0;
	}

	/** S202: lists every naturally spawning creature's dimensions and Overworld spawn climate (also written to the log). **/
	public static int climate(final CommandContext<CommandSourceStack> context) {
		if (!context.getSource().hasPermission(2)) {
			return 0;
		}
		Level level = context.getSource().getServer().overworld();
		List<CreatureInfo> creatures = new ArrayList<>(CreatureManager.getInstance().getCreatures());
		creatures.sort(Comparator.comparing(CreatureInfo::getName));
		int count = 0;
		for (CreatureInfo creatureInfo : creatures) {
			CreatureSpawn spawn = creatureInfo.getCreatureSpawn();
			if (spawn == null || spawn.getSpawners().isEmpty()) {
				continue;
			}
			String dimensions = spawn.getDimensionIds().isEmpty() ? "all dimensions" : spawn.getDimensionListType() + " " + spawn.getDimensionIds();
			String line = creatureInfo.getName() + ": " + spawn.describeClimate(level) + " | " + dimensions
					+ (spawn.getSpawnMinY() != Integer.MIN_VALUE ? " | y >= " + spawn.getSpawnMinY() : "");
			LMHelperClass.logInfoMessage("[Climate] " + line);
			context.getSource().sendSuccess(() -> Component.literal(line), false);
			count++;
		}
		final int total = count;
		context.getSource().sendSuccess(() -> Component.literal(total + " creatures listed (also in the server log)."), false);
		return total;
	}
}
