package com.lycanitesmobs.core.network.message;

import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.capabilities.entity.ExtendedEntity;
import com.lycanitesmobs.core.network.PacketManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: perchedByEntity (0 = none) now perches on perchedOnEntity. Keeps the client's ExtendedEntity in step for smooth carrying/perching. **/
public class MessageEntityPerched implements CustomPacketPayload {
    public static final Type<MessageEntityPerched> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, "entity_perched"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MessageEntityPerched> STREAM_CODEC = PacketManager.codec(MessageEntityPerched::encode, MessageEntityPerched::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public int perchedOnEntityID;
    public int perchedByEntityID;

    public MessageEntityPerched() {
    }

    public MessageEntityPerched(Entity perchedOnEntity, Entity perchedByEntity) {
        this.perchedOnEntityID = perchedOnEntity.getId();
        this.perchedByEntityID = perchedByEntity != null ? perchedByEntity.getId() : 0;
    }

    public static void handle(MessageEntityPerched message, IPayloadContext context) {
        Level world = context.player().level();
        Entity perchedOnEntity = world.getEntity(message.perchedOnEntityID);
        Entity perchedByEntity = message.perchedByEntityID != 0 ? world.getEntity(message.perchedByEntityID) : null;
        if (!(perchedOnEntity instanceof LivingEntity livingEntity)) {
            return;
        }
        ExtendedEntity extendedEntity = ExtendedEntity.getForEntity(livingEntity);
        if (extendedEntity != null) {
            extendedEntity.setPerchedByEntity(perchedByEntity);
        }
    }

    public static MessageEntityPerched decode(RegistryFriendlyByteBuf packet) {
        MessageEntityPerched message = new MessageEntityPerched();
        message.perchedOnEntityID = packet.readInt();
        message.perchedByEntityID = packet.readInt();
        return message;
    }

    public static void encode(MessageEntityPerched message, RegistryFriendlyByteBuf packet) {
        packet.writeInt(message.perchedOnEntityID);
        packet.writeInt(message.perchedByEntityID);
    }
}
