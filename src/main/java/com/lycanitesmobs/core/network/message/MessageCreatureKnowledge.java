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
import com.lycanitesmobs.core.data.info.creature.CreatureKnowledge;

/** Server -> client: one creature's Beastiary knowledge. **/
public class MessageCreatureKnowledge implements CustomPacketPayload {
    public static final Type<MessageCreatureKnowledge> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, "creature_knowledge"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MessageCreatureKnowledge> STREAM_CODEC = PacketManager.codec(MessageCreatureKnowledge::encode, MessageCreatureKnowledge::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public String creatureName;
    public int rank;
    public int experience;

    public MessageCreatureKnowledge() {
    }

    public MessageCreatureKnowledge(CreatureKnowledge creatureKnowledge) {
        this.creatureName = creatureKnowledge.getCreatureName();
        this.rank = creatureKnowledge.getRank();
        this.experience = creatureKnowledge.getExperience();
    }

    public static void handle(MessageCreatureKnowledge message, IPayloadContext context) {
        ExtendedPlayer playerExt = ExtendedPlayer.getForPlayer(context.player());
        if (playerExt == null) {
            return;
        }
        playerExt.getBeastiary().addCreatureKnowledge(new CreatureKnowledge(playerExt.getBeastiary(), message.creatureName, message.rank, message.experience), false);
    }

    public static MessageCreatureKnowledge decode(RegistryFriendlyByteBuf packet) {
        MessageCreatureKnowledge message = new MessageCreatureKnowledge();
        message.creatureName = packet.readUtf(256);
        message.rank = packet.readInt();
        message.experience = packet.readInt();
        return message;
    }

    public static void encode(MessageCreatureKnowledge message, RegistryFriendlyByteBuf packet) {
        packet.writeUtf(message.creatureName);
        packet.writeInt(message.rank);
        packet.writeInt(message.experience);
    }
}
