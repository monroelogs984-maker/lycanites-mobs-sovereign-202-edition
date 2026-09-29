package com.lycanitesmobs.core.network.message;

import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.network.PacketManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: open a Beastiary screen (e.g. summoning with no summon set selected). **/
public class MessageScreenRequest implements CustomPacketPayload {
    public static final Type<MessageScreenRequest> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, "screen_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MessageScreenRequest> STREAM_CODEC = PacketManager.codec(MessageScreenRequest::encode, MessageScreenRequest::decode);

    public enum GuiRequest {
        BEASTIARY((byte) 0), SUMMONING((byte) 1);
        public final byte id;

        GuiRequest(byte i) {
            this.id = i;
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public byte screenId;

    public MessageScreenRequest() {
    }

    public MessageScreenRequest(GuiRequest guiRequest) {
        this.screenId = guiRequest.id;
    }

    public static void handle(MessageScreenRequest message, IPayloadContext context) {
        LycanitesMobs.OPEN_SCREEN.accept(message.screenId);
    }

    public static MessageScreenRequest decode(RegistryFriendlyByteBuf packet) {
        MessageScreenRequest message = new MessageScreenRequest();
        message.screenId = packet.readByte();
        return message;
    }

    public static void encode(MessageScreenRequest message, RegistryFriendlyByteBuf packet) {
        packet.writeByte(message.screenId);
    }
}
