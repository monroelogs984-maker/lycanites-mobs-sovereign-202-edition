package com.lycanitesmobs.core.network.message;

import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.network.PacketManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: A mob event (player/boss channel) started or is still running; an empty name stops it. The client plays the event's title, sound and chat message. **/
public class MessageMobEvent implements CustomPacketPayload {
    public static final Type<MessageMobEvent> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, "mob_event"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MessageMobEvent> STREAM_CODEC = PacketManager.codec(MessageMobEvent::encode, MessageMobEvent::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public String mobEventName = "";
    public BlockPos pos = BlockPos.ZERO;
    public int level = 1;
    public int subspecies = 1;

    public MessageMobEvent() {
    }

    public MessageMobEvent(String mobEventName, BlockPos pos, int level, int subspecies) {
        this.mobEventName = mobEventName;
        this.pos = pos;
        this.level = level;
        this.subspecies = subspecies;
    }

    public static void handle(MessageMobEvent message, IPayloadContext context) {
        LycanitesMobs.APPLY_MOB_EVENT.accept(message.mobEventName, false);
    }

    public static MessageMobEvent decode(RegistryFriendlyByteBuf packet) {
        MessageMobEvent message = new MessageMobEvent();
        message.mobEventName = packet.readUtf(256);
        message.pos = packet.readBlockPos();
        message.level = packet.readInt();
        message.subspecies = packet.readInt();
        return message;
    }

    public static void encode(MessageMobEvent message, RegistryFriendlyByteBuf packet) {
        packet.writeUtf(message.mobEventName);
        packet.writeBlockPos(message.pos);
        packet.writeInt(message.level);
        packet.writeInt(message.subspecies);
    }
}
