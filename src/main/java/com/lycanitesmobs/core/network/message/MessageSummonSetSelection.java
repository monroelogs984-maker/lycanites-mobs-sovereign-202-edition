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

/** Both ways: the selected summon set. **/
public class MessageSummonSetSelection implements CustomPacketPayload {
    public static final Type<MessageSummonSetSelection> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, "summon_set_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MessageSummonSetSelection> STREAM_CODEC = PacketManager.codec(MessageSummonSetSelection::encode, MessageSummonSetSelection::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public byte summonSetID;

    public MessageSummonSetSelection() {
    }

    public MessageSummonSetSelection(ExtendedPlayer playerExt) {
        this.summonSetID = (byte) playerExt.getSelectedSummonSetId();
    }

    public static void handle(MessageSummonSetSelection message, IPayloadContext context) {
        ExtendedPlayer playerExt = ExtendedPlayer.getForPlayer(context.player());
        if (playerExt != null) {
            playerExt.setSelectedSummonSet(message.summonSetID);
        }
    }

    public static MessageSummonSetSelection decode(RegistryFriendlyByteBuf packet) {
        MessageSummonSetSelection message = new MessageSummonSetSelection();
        message.summonSetID = packet.readByte();
        return message;
    }

    public static void encode(MessageSummonSetSelection message, RegistryFriendlyByteBuf packet) {
        packet.writeByte(message.summonSetID);
    }
}
