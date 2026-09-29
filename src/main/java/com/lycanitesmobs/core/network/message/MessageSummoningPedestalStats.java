package com.lycanitesmobs.core.network.message;

import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.block.blockentity.TileEntitySummoningPedestal;
import com.lycanitesmobs.core.network.PacketManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: a summoning pedestal's capacity, summon progress and fuel, for its screen. **/
public class MessageSummoningPedestalStats implements CustomPacketPayload {
    public static final Type<MessageSummoningPedestalStats> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, "summoning_pedestal_stats"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MessageSummoningPedestalStats> STREAM_CODEC = PacketManager.codec(MessageSummoningPedestalStats::encode, MessageSummoningPedestalStats::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public int capacity;
    public int progress;
    public int fuel;
    public int fuelMax;
    public BlockPos pos = BlockPos.ZERO;

    public MessageSummoningPedestalStats() {
    }

    public MessageSummoningPedestalStats(TileEntitySummoningPedestal.SummoningPedestalStats stats, BlockPos pos) {
        this.capacity = stats.capacity();
        this.progress = stats.summonProgress();
        this.fuel = stats.summoningFuel();
        this.fuelMax = stats.summoningFuelMax();
        this.pos = pos;
    }

    public static void handle(MessageSummoningPedestalStats message, IPayloadContext context) {
        if (context.player().level().getBlockEntity(message.pos) instanceof TileEntitySummoningPedestal pedestal) {
            pedestal.applyNetworkStats(message.capacity, message.progress, message.fuel, message.fuelMax);
        }
    }

    public static MessageSummoningPedestalStats decode(RegistryFriendlyByteBuf packet) {
        MessageSummoningPedestalStats message = new MessageSummoningPedestalStats();
        message.pos = packet.readBlockPos();
        message.capacity = packet.readInt();
        message.progress = packet.readInt();
        message.fuel = packet.readInt();
        message.fuelMax = packet.readInt();
        return message;
    }

    public static void encode(MessageSummoningPedestalStats message, RegistryFriendlyByteBuf packet) {
        packet.writeBlockPos(message.pos);
        packet.writeInt(message.capacity);
        packet.writeInt(message.progress);
        packet.writeInt(message.fuel);
        packet.writeInt(message.fuelMax);
    }
}
