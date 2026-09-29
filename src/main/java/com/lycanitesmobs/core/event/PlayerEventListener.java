package com.lycanitesmobs.core.event;

import com.lycanitesmobs.core.block.base.BlockFireBase;
import com.lycanitesmobs.core.capabilities.entity.ExtendedPlayer;
import com.lycanitesmobs.core.capabilities.level.ExtendedWorld;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The player parts of the official GameEventListener: ExtendedPlayer updates, re-syncing on dimension change and
 * recording the block a player just broke (read by block spawn triggers), and boss block protection. Player cloning is handled by the
 * ExtendedPlayer attachment itself (copyOnDeath).
 */
public class PlayerEventListener {
    public static void register() {
        NeoForge.EVENT_BUS.addListener(PlayerEventListener::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(PlayerEventListener::onPlayerChangedDimension);
        NeoForge.EVENT_BUS.addListener(PlayerEventListener::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(PlayerEventListener::onBlockPlace);
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
        // Bosses protect their arena: non-creative players can't break blocks near one (fire blocks excepted).
        if (!event.getPlayer().isCreative() && event.getLevel() instanceof Level level && !(event.getState().getBlock() instanceof BlockFireBase)) {
            ExtendedWorld extendedWorld = ExtendedWorld.getForWorld(level);
            if (extendedWorld != null && extendedWorld.isBossNearby(Vec3.atLowerCornerOf(event.getPos()))) {
                event.setCanceled(true);
                event.getPlayer().displayClientMessage(Component.translatable("boss.block.protection.break"), true);
                return;
            }
        }
        ExtendedPlayer extendedPlayer = ExtendedPlayer.getForPlayer(event.getPlayer());
        if (extendedPlayer != null) {
            extendedPlayer.setJustBrokenBlock(event.getState());
        }
    }

    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide() || event.isCanceled() || !(event.getEntity() instanceof Player player) || player.isCreative()) {
            return;
        }
        if (event.getLevel() instanceof Level level) {
            ExtendedWorld extendedWorld = ExtendedWorld.getForWorld(level);
            if (extendedWorld != null && extendedWorld.isBossNearby(Vec3.atLowerCornerOf(event.getPos()))) {
                event.setCanceled(true);
                player.displayClientMessage(Component.translatable("boss.block.protection.place"), true);
            }
        }
    }
}
