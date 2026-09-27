package com.lycanitesmobs.core.network.message;

import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.network.PacketManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: a pet command button pressed in a creature's GUI. **/
public class MessageEntityGUICommand implements CustomPacketPayload {
    public static final Type<MessageEntityGUICommand> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, "entity_gui_command"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MessageEntityGUICommand> STREAM_CODEC = PacketManager.codec(MessageEntityGUICommand::encode, MessageEntityGUICommand::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    int entityID;
    public int guiCommandID;

    public MessageEntityGUICommand() {
    }

    public MessageEntityGUICommand(int guiCommandID, Entity entity) {
        this.entityID = entity.getId();
        this.guiCommandID = guiCommandID;
    }

    public static void handle(MessageEntityGUICommand message, IPayloadContext context) {
        Player player = context.player();
        Entity entity = player.getCommandSenderWorld().getEntity(message.entityID);
        if (entity instanceof TameableCreatureEntity pet) {
            pet.performGUICommand(player, message.guiCommandID);
        }
    }

    public static MessageEntityGUICommand decode(RegistryFriendlyByteBuf packet) {
        MessageEntityGUICommand message = new MessageEntityGUICommand();
        message.entityID = packet.readInt();
        message.guiCommandID = packet.readInt();
        return message;
    }

    public static void encode(MessageEntityGUICommand message, RegistryFriendlyByteBuf packet) {
        packet.writeInt(message.entityID);
        packet.writeInt(message.guiCommandID);
    }
}
