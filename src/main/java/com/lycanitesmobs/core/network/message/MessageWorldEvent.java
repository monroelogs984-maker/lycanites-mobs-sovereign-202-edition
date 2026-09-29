package com.lycanitesmobs.core.network.message;

import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.network.PacketManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: The world event changed; an empty name stops it. The client plays the event's title, sound and chat message. **/
public class MessageWorldEvent implements CustomPacketPayload {
    public static final Type<MessageWorldEvent> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, "world_event"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MessageWorldEvent> STREAM_CODEC = PacketManager.codec(MessageWorldEvent::encode, MessageWorldEvent::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public String mobEventName = "";
    public BlockPos pos = BlockPos.ZERO;
    public int level = 1;
    public int subspecies = 1;

    public MessageWorldEvent() {
    }

    public MessageWorldEvent(String mobEventName, BlockPos pos, int level, int subspecies) {
        this.mobEventName = mobEventName;
        this.pos = pos;
        this.level = level;
        this.subspecies = subspecies;
    }

    public static void handle(MessageWorldEvent message, IPayloadContext context) {
        LycanitesMobs.APPLY_MOB_EVENT.accept(message.mobEventName, true);
    }

    public static MessageWorldEvent decode(RegistryFriendlyByteBuf packet) {
        MessageWorldEvent message = new MessageWorldEvent();
        message.mobEventName = packet.readUtf(256);
        message.pos = packet.readBlockPos();
        message.level = packet.readInt();
        message.subspecies = packet.readInt();
        return message;
    }

    public static void encode(MessageWorldEvent message, RegistryFriendlyByteBuf packet) {
        packet.writeUtf(message.mobEventName);
        packet.writeBlockPos(message.pos);
        packet.writeInt(message.level);
        packet.writeInt(message.subspecies);
    }
}
