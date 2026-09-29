package com.lycanitesmobs.core.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Items and blocks that are registered (so they exist, render and can be obtained) but whose system isn't ported
 * yet. Using one tells the player in chat instead of silently doing nothing. Remove each call as its system lands.
 */
public class PortPlaceholder {
    /** Systems a placeholder can be waiting on. The name is shown in the chat message. **/
    public static final String EQUIPMENT = "the equipment system";
    public static final String ALTARS = "altars";
    public static final String SUMMONING_PEDESTAL = "the summoning pedestal";

    /**
     * Sends "<thing> is not functional yet due to <system> not being ported." to the player. Call server side only,
     * so the message isn't shown twice.
     */
    public static void notifyNotFunctional(Player player, Component thing, String system) {
        if (player == null || player.level().isClientSide) {
            return;
        }
        player.displayClientMessage(Component.translatable("lyc.port.placeholder", thing, system).withStyle(ChatFormatting.YELLOW), false);
    }
}
