package com.lycanitesmobs.core.network.message;

import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.block.blockentity.TileEntitySummoningPedestal;
import com.lycanitesmobs.core.entity.pets.SummonSet;
import com.lycanitesmobs.core.network.PacketManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: the summon set picked in a summoning pedestal's screen. Port: only the pedestal's owner (within
 * reach) can change it - the official applied it for anyone who sent the packet.
 */
public class MessageSummoningPedestalSummonSet implements CustomPacketPayload {
    public static final Type<MessageSummoningPedestalSummonSet> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, "summoning_pedestal_summon_set"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MessageSummoningPedestalSummonSet> STREAM_CODEC = PacketManager.codec(MessageSummoningPedestalSummonSet::encode, MessageSummoningPedestalSummonSet::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public String summonType = "";
    public int subspecies;
    public int variant;
    public byte behaviour;
    public BlockPos pos = BlockPos.ZERO;

    public MessageSummoningPedestalSummonSet() {
    }

    public MessageSummoningPedestalSummonSet(SummonSet summonSet, BlockPos pos) {
        this.summonType = summonSet.getSummonType();
        this.subspecies = summonSet.getSubspecies();
        this.variant = summonSet.getVariant();
        this.behaviour = summonSet.getBehaviourByte();
        this.pos = pos;
    }

    public static void handle(MessageSummoningPedestalSummonSet message, IPayloadContext context) {
        Player player = context.player();
        if (!(player.level().getBlockEntity(message.pos) instanceof TileEntitySummoningPedestal pedestal)) {
            return;
        }
        if (!player.getUUID().equals(pedestal.getOwnerUUID()) || !pedestal.stillValid(player)) {
            return;
        }
        pedestal.applySummonSetPacket(message.summonType, message.subspecies, message.variant, message.behaviour);
    }

    public static MessageSummoningPedestalSummonSet decode(RegistryFriendlyByteBuf packet) {
        MessageSummoningPedestalSummonSet message = new MessageSummoningPedestalSummonSet();
        message.pos = packet.readBlockPos();
        message.summonType = packet.readUtf(256);
        message.subspecies = packet.readInt();
        message.variant = packet.readInt();
        message.behaviour = packet.readByte();
        return message;
    }

    public static void encode(MessageSummoningPedestalSummonSet message, RegistryFriendlyByteBuf packet) {
        packet.writeBlockPos(message.pos);
        packet.writeUtf(message.summonType);
        packet.writeInt(message.subspecies);
        packet.writeInt(message.variant);
        packet.writeByte(message.behaviour);
    }
}
