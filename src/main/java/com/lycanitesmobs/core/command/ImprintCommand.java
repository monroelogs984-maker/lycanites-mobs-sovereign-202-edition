package com.lycanitesmobs.core.command;

import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.item.equipment.ItemEquipmentPart;
import com.lycanitesmobs.core.item.equipment.imprint.Imprints;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * S202 dev/admin command for imprints on the held item (the Equipment Forge/Infuser/Station GUIs replace this for players):
 * /lm imprint set <part> [level], /lm imprint extract, /lm imprint mana <amount>.
 */
public class ImprintCommand {
	public static ArgumentBuilder<CommandSourceStack, ?> register() {
		return Commands.literal("imprint")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("set")
						.then(Commands.argument("part", StringArgumentType.word())
								.executes(context -> set(context, 1))
								.then(Commands.argument("level", IntegerArgumentType.integer(1, 3))
										.executes(context -> set(context, IntegerArgumentType.getInteger(context, "level"))))))
				.then(Commands.literal("extract").executes(ImprintCommand::extract))
				.then(Commands.literal("mana")
						.then(Commands.argument("amount", IntegerArgumentType.integer())
								.executes(ImprintCommand::mana)));
	}

	private static int set(CommandContext<CommandSourceStack> context, int level) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		ItemStack host = player.getMainHandItem();
		String partName = StringArgumentType.getString(context, "part");
		Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, "equipmentpart_" + partName));
		if (!(item instanceof ItemEquipmentPart partItem)) {
			context.getSource().sendFailure(Component.literal("Unknown equipment part: " + partName));
			return 0;
		}
		if (!Imprints.isEligible(host)) {
			context.getSource().sendFailure(Component.literal("The held item can't take an imprint."));
			return 0;
		}
		ItemStack part = new ItemStack(partItem);
		partItem.setLevel(part, level);
		Imprints.extract(host);
		Imprints.imprint(host, part);
		context.getSource().sendSuccess(() -> Component.literal("Imprinted " + partName + " (level " + level + ")."), false);
		return 1;
	}

	private static int extract(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		ItemStack part = Imprints.extract(player.getMainHandItem());
		if (part.isEmpty()) {
			context.getSource().sendFailure(Component.literal("The held item has no imprint."));
			return 0;
		}
		player.getInventory().placeItemBackInInventory(part);
		context.getSource().sendSuccess(() -> Component.literal("Extracted the imprinted part."), false);
		return 1;
	}

	private static int mana(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		ItemStack host = player.getMainHandItem();
		if (!Imprints.hasImprint(host)) {
			context.getSource().sendFailure(Component.literal("The held item has no imprint."));
			return 0;
		}
		Imprints.changeMana(host, IntegerArgumentType.getInteger(context, "amount") - Imprints.getMana(host));
		context.getSource().sendSuccess(() -> Component.literal("Imprint mana set to " + Imprints.getMana(host) + "."), false);
		return 1;
	}
}
