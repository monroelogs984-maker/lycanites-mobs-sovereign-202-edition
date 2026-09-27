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

/** Server -> client: spirit, summoning focus and creature study cooldown. **/
public class MessagePlayerStats implements CustomPacketPayload {
    public static final Type<MessagePlayerStats> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, "player_stats"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MessagePlayerStats> STREAM_CODEC = PacketManager.codec(MessagePlayerStats::encode, MessagePlayerStats::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public int spirit;
    public int summonFocus;
    public int creatureStudyCooldown;

    public MessagePlayerStats() {
    }

    public MessagePlayerStats(ExtendedPlayer playerExt) {
        this.spirit = playerExt.getSpirit();
        this.summonFocus = playerExt.getSummonFocus();
        this.creatureStudyCooldown = playerExt.getCreatureStudyCooldown();
    }

    public static void handle(MessagePlayerStats message, IPayloadContext context) {
        ExtendedPlayer playerExt = ExtendedPlayer.getForPlayer(context.player());
        if (playerExt != null) {
            playerExt.applyNetworkStats(message.spirit, message.summonFocus, message.creatureStudyCooldown);
        }
    }

    public static MessagePlayerStats decode(RegistryFriendlyByteBuf packet) {
        MessagePlayerStats message = new MessagePlayerStats();
        message.spirit = packet.readInt();
        message.summonFocus = packet.readInt();
        message.creatureStudyCooldown = packet.readInt();
        return message;
    }

    public static void encode(MessagePlayerStats message, RegistryFriendlyByteBuf packet) {
        packet.writeInt(message.spirit);
        packet.writeInt(message.summonFocus);
        packet.writeInt(message.creatureStudyCooldown);
    }
}
