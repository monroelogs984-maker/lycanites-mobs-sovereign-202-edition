package com.lycanitesmobs.core.network.message;

import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.capabilities.entity.ExtendedPlayer;
import com.lycanitesmobs.core.network.PacketManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;

/** Server -> client: an action bar (overlay) message. **/
public class MessageOverlayMessage implements CustomPacketPayload {
    public static final Type<MessageOverlayMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, "overlay_message"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MessageOverlayMessage> STREAM_CODEC = PacketManager.codec(MessageOverlayMessage::encode, MessageOverlayMessage::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public Component message;

    public MessageOverlayMessage() {
    }

    public MessageOverlayMessage(MutableComponent message) {
        this.message = message;
    }

    /** The original called gui.setOverlayMessage() - the client player's displayClientMessage(..., true) is the same call. **/
    public static void handle(MessageOverlayMessage message, IPayloadContext context) {
        context.player().displayClientMessage(message.message, true);
    }

    public static MessageOverlayMessage decode(RegistryFriendlyByteBuf packet) {
        MessageOverlayMessage message = new MessageOverlayMessage();
        message.message = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(packet);
        return message;
    }

    public static void encode(MessageOverlayMessage message, RegistryFriendlyByteBuf packet) {
        ComponentSerialization.TRUSTED_STREAM_CODEC.encode(packet, message.message);
    }
}
