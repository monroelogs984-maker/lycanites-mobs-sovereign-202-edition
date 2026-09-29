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

/** Server -> client: the entity (pickedUpEntity) is now carried by pickedUpByEntity (0 = dropped). Keeps the client's ExtendedEntity in step for smooth carrying/perching. **/
public class MessageEntityPickedUp implements CustomPacketPayload {
    public static final Type<MessageEntityPickedUp> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, "entity_picked_up"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MessageEntityPickedUp> STREAM_CODEC = PacketManager.codec(MessageEntityPickedUp::encode, MessageEntityPickedUp::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public int pickedUpEntityID;
    public int pickedUpByEntityID;

    public MessageEntityPickedUp() {
    }

    public MessageEntityPickedUp(Entity pickedUpEntity, Entity pickedUpByEntity) {
        this.pickedUpEntityID = pickedUpEntity.getId();
        this.pickedUpByEntityID = pickedUpByEntity != null ? pickedUpByEntity.getId() : 0;
    }

    public static void handle(MessageEntityPickedUp message, IPayloadContext context) {
        Level world = context.player().level();
        Entity pickedUpEntity = world.getEntity(message.pickedUpEntityID);
        Entity pickedUpByEntity = message.pickedUpByEntityID != 0 ? world.getEntity(message.pickedUpByEntityID) : null;
        if (!(pickedUpEntity instanceof LivingEntity livingEntity)) {
            return;
        }
        ExtendedEntity extendedEntity = ExtendedEntity.getForEntity(livingEntity);
        if (extendedEntity != null) {
            extendedEntity.setPickedUpByEntity(pickedUpByEntity);
        }
    }

    public static MessageEntityPickedUp decode(RegistryFriendlyByteBuf packet) {
        MessageEntityPickedUp message = new MessageEntityPickedUp();
        message.pickedUpEntityID = packet.readInt();
        message.pickedUpByEntityID = packet.readInt();
        return message;
    }

    public static void encode(MessageEntityPickedUp message, RegistryFriendlyByteBuf packet) {
        packet.writeInt(message.pickedUpEntityID);
        packet.writeInt(message.pickedUpByEntityID);
    }
}
