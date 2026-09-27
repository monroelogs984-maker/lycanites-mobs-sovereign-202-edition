package com.lycanitesmobs.core.network.message;

import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.network.PacketManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: the receiving player's reputation with a creature (shown by the taming bar). **/
public class MessageCreature implements CustomPacketPayload {
    public static final Type<MessageCreature> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, "creature"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MessageCreature> STREAM_CODEC = PacketManager.codec(MessageCreature::encode, MessageCreature::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    int entityID;
    int playerReputation = 0;

    public MessageCreature() {
    }

    public MessageCreature(BaseCreatureEntity creatureEntity, int playerReputation) {
        this.entityID = creatureEntity.getId();
        this.playerReputation = playerReputation;
    }

    public static void handle(MessageCreature message, IPayloadContext context) {
        Player player = context.player();
        Entity entity = player.level().getEntity(message.entityID);
        if (!(entity instanceof BaseCreatureEntity creatureEntity) || creatureEntity.getRelationships() == null) {
            return;
        }
        creatureEntity.getOrCreateRelationshipEntry(player).setReputation(message.playerReputation);
    }

    public static MessageCreature decode(RegistryFriendlyByteBuf packet) {
        MessageCreature message = new MessageCreature();
        message.entityID = packet.readInt();
        message.playerReputation = packet.readInt();
        return message;
    }

    public static void encode(MessageCreature message, RegistryFriendlyByteBuf packet) {
        packet.writeInt(message.entityID);
        packet.writeInt(message.playerReputation);
    }
}
