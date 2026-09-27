package com.lycanitesmobs.core.event;

import com.lycanitesmobs.core.capabilities.entity.ExtendedPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The player parts of the official GameEventListener: ExtendedPlayer updates, re-syncing on dimension change and
 * recording the block a player just broke (read by block spawn triggers). Player cloning is handled by the
 * ExtendedPlayer attachment itself (copyOnDeath).
 */
public class PlayerEventListener {
    public static void register() {
        NeoForge.EVENT_BUS.addListener(PlayerEventListener::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(PlayerEventListener::onPlayerChangedDimension);
        NeoForge.EVENT_BUS.addListener(PlayerEventListener::onBlockBreak);
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        ExtendedPlayer playerExt = ExtendedPlayer.getForPlayer(event.getEntity());
        if (playerExt != null) {
            playerExt.onUpdate();
        }
    }

    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        if (player.getCommandSenderWorld().isClientSide) {
            return;
        }
        ExtendedPlayer extendedPlayer = ExtendedPlayer.getForPlayer(player);
        if (extendedPlayer != null) {
            extendedPlayer.sendFullStateToClient();
        }
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide() || event.isCanceled() || event.getPlayer() == null) {
            return;
        }
        // TODO(port): the official also cancels breaking blocks near a boss (ExtendedWorld.isBossNearby).
        ExtendedPlayer extendedPlayer = ExtendedPlayer.getForPlayer(event.getPlayer());
        if (extendedPlayer != null) {
            extendedPlayer.setJustBrokenBlock(event.getState());
        }
    }
}
