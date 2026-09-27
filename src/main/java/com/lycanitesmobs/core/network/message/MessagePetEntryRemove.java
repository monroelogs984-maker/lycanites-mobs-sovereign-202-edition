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
import com.lycanitesmobs.core.entity.pets.PetEntry;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;

import java.util.UUID;

/** Both ways: removes (releases) a pet entry. **/
public class MessagePetEntryRemove implements CustomPacketPayload {
    public static final Type<MessagePetEntryRemove> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, "pet_entry_remove"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MessagePetEntryRemove> STREAM_CODEC = PacketManager.codec(MessagePetEntryRemove::encode, MessagePetEntryRemove::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public UUID petEntryID;

    public MessagePetEntryRemove() {
    }

    public MessagePetEntryRemove(ExtendedPlayer playerExt, PetEntry petEntry) {
        this.petEntryID = petEntry.getPetEntryID();
    }

    public static void handle(MessagePetEntryRemove message, IPayloadContext context) {
        ExtendedPlayer playerExt = ExtendedPlayer.getForPlayer(context.player());
        if (playerExt == null) {
            return;
        }
        PetEntry petEntry = playerExt.getPetManager().getEntry(message.petEntryID);
        if (petEntry == null) {
            if (context.flow().isServerbound()) {
                LMHelperClass.logWarningMessage("Tried to remove a null PetEntry from server!");
            }
            return;
        }
        petEntry.remove();
    }

    public static MessagePetEntryRemove decode(RegistryFriendlyByteBuf packet) {
        MessagePetEntryRemove message = new MessagePetEntryRemove();
        message.petEntryID = packet.readUUID();
        return message;
    }

    public static void encode(MessagePetEntryRemove message, RegistryFriendlyByteBuf packet) {
        packet.writeUUID(message.petEntryID);
    }
}
