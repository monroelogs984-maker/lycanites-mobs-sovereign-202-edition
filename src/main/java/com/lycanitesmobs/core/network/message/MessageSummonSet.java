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
import com.lycanitesmobs.core.entity.pets.SummonSet;

/** Both ways: one summon set's creature and behaviour. **/
public class MessageSummonSet implements CustomPacketPayload {
    public static final Type<MessageSummonSet> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, "summon_set"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MessageSummonSet> STREAM_CODEC = PacketManager.codec(MessageSummonSet::encode, MessageSummonSet::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public byte summonSetID;
    public int subpsecies;
    public int variant;
    public String summonType;
    public byte behaviour;

    public MessageSummonSet() {
    }

    public MessageSummonSet(ExtendedPlayer playerExt, byte summonSetID) {
        this.summonSetID = summonSetID;
        this.summonType = playerExt.getSummonSet(summonSetID).getSummonType();
        this.subpsecies = playerExt.getSummonSet(summonSetID).getSubspecies();
        this.variant = playerExt.getSummonSet(summonSetID).getVariant();
        this.behaviour = playerExt.getSummonSet(summonSetID).getBehaviourByte();
    }

    public static void handle(MessageSummonSet message, IPayloadContext context) {
        ExtendedPlayer playerExt = ExtendedPlayer.getForPlayer(context.player());
        if (playerExt == null) {
            return;
        }
        SummonSet summonSet = playerExt.getSummonSet(message.summonSetID);
        summonSet.readFromPacket(message.summonType, message.subpsecies, message.variant, message.behaviour);
    }

    public static MessageSummonSet decode(RegistryFriendlyByteBuf packet) {
        MessageSummonSet message = new MessageSummonSet();
        message.summonSetID = packet.readByte();
        message.summonType = packet.readUtf(256);
        message.subpsecies = packet.readInt();
        message.variant = packet.readInt();
        message.behaviour = packet.readByte();
        return message;
    }

    public static void encode(MessageSummonSet message, RegistryFriendlyByteBuf packet) {
        packet.writeByte(message.summonSetID);
        packet.writeUtf(message.summonType == null ? "" : message.summonType);
        packet.writeInt(message.subpsecies);
        packet.writeInt(message.variant);
        packet.writeByte(message.behaviour);
    }
}
