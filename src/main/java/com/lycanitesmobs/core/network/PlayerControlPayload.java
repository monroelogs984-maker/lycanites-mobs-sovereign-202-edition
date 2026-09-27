package com.lycanitesmobs.core.network;

import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.capabilities.entity.ExtendedPlayer;
import com.lycanitesmobs.core.entity.base.RideableCreatureEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: the player's current Lycanites control keys (jump, descend, mount ability, mount inventory,
 * attack) as a bitmask. Port of MessagePlayerControl.
 */
public record PlayerControlPayload(byte controlStates) implements CustomPacketPayload {
    public static final Type<PlayerControlPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, "player_control"));
    public static final StreamCodec<ByteBuf, PlayerControlPayload> STREAM_CODEC = ByteBufCodecs.BYTE.map(PlayerControlPayload::new, PlayerControlPayload::controlStates);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Runs on the server main thread (NeoForge's default handler thread). */
    public static void handle(PlayerControlPayload payload, IPayloadContext context) {
        Player player = context.player();
        ExtendedPlayer playerExt = ExtendedPlayer.getForPlayer(player);
        if (playerExt == null) {
            return;
        }
        playerExt.updateControlStates(payload.controlStates());
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof RideableCreatureEntity rideableCreature
                && rideableCreature.getControllingPassenger() == player
                && rideableCreature.riderControl()) {
            rideableCreature.handleRiderControls(player, playerExt);
        }
    }
}
