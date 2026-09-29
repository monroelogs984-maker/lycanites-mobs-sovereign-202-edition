package com.lycanitesmobs.core.network;

import com.lycanitesmobs.core.network.message.MessageEntityPerched;
import com.lycanitesmobs.core.network.message.MessageEntityPickedUp;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import com.lycanitesmobs.core.network.message.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers the mod's network payloads (replaces the original's Forge SimpleChannel) and keeps the original's
 * sendToPlayer/sendToServer call shape (LycanitesMobs.PACKET_MANAGER), so ported code sends messages unchanged.
 * Each message is a CustomPacketPayload with a static handle(message, IPayloadContext); handlers run on the main
 * thread (NeoForge's default) and use context.player() on both sides, so no client-only classes are referenced.
 */
public class PacketManager {
    public static final String PROTOCOL_VERSION = "1";

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        // Client -> Server:
        registrar.playToServer(PlayerControlPayload.TYPE, PlayerControlPayload.STREAM_CODEC, PlayerControlPayload::handle);
        registrar.playToServer(MessageEntityGUICommand.TYPE, MessageEntityGUICommand.STREAM_CODEC, MessageEntityGUICommand::handle);

        // Server -> Client:
        registrar.playToClient(MessagePlayerStats.TYPE, MessagePlayerStats.STREAM_CODEC, MessagePlayerStats::handle);
        registrar.playToClient(MessageBeastiary.TYPE, MessageBeastiary.STREAM_CODEC, MessageBeastiary::handle);
        registrar.playToClient(MessageCreatureKnowledge.TYPE, MessageCreatureKnowledge.STREAM_CODEC, MessageCreatureKnowledge::handle);
        registrar.playToClient(MessageCreature.TYPE, MessageCreature.STREAM_CODEC, MessageCreature::handle);
        registrar.playToClient(MessageOverlayMessage.TYPE, MessageOverlayMessage.STREAM_CODEC, MessageOverlayMessage::handle);
        registrar.playToClient(MessageEntityPickedUp.TYPE, MessageEntityPickedUp.STREAM_CODEC, MessageEntityPickedUp::handle);
        registrar.playToClient(MessageEntityPerched.TYPE, MessageEntityPerched.STREAM_CODEC, MessageEntityPerched::handle);
        registrar.playToClient(MessageScreenRequest.TYPE, MessageScreenRequest.STREAM_CODEC, MessageScreenRequest::handle);

        // Both ways:
        registrar.playBidirectional(MessagePetEntry.TYPE, MessagePetEntry.STREAM_CODEC, MessagePetEntry::handle);
        registrar.playBidirectional(MessagePetEntryRemove.TYPE, MessagePetEntryRemove.STREAM_CODEC, MessagePetEntryRemove::handle);
        registrar.playBidirectional(MessageSummonSet.TYPE, MessageSummonSet.STREAM_CODEC, MessageSummonSet::handle);
        registrar.playBidirectional(MessageSummonSetSelection.TYPE, MessageSummonSetSelection.STREAM_CODEC, MessageSummonSetSelection::handle);
    }

    public void sendToPlayer(CustomPacketPayload message, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, message);
    }

    /** Sends to every player in the level's dimension (official sendToWorld). **/
    public void sendToWorld(CustomPacketPayload message, Level level) {
        if (level instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersInDimension(serverLevel, message);
        }
    }

    public void sendToServer(CustomPacketPayload message) {
        PacketDistributor.sendToServer(message);
    }

    /** Builds a payload type + codec pair from the original encode/decode methods. **/
    public static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> codec(Encoder<T> encoder, Decoder<T> decoder) {
        return StreamCodec.of((buf, message) -> encoder.encode(message, buf), decoder::decode);
    }

    public interface Encoder<T> {
        void encode(T message, RegistryFriendlyByteBuf buf);
    }

    public interface Decoder<T> {
        T decode(RegistryFriendlyByteBuf buf);
    }
}
