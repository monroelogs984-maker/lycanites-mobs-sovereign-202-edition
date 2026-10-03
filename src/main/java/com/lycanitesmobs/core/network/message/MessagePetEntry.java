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
import com.lycanitesmobs.core.entity.pets.SummonSet;
import com.lycanitesmobs.core.manager.PetManager;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

/** Both ways: a pet entry's state (server -> client) or the player's changes to it from the GUI (client -> server). **/
public class MessagePetEntry implements CustomPacketPayload {
    public static final Type<MessagePetEntry> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, "pet_entry"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MessagePetEntry> STREAM_CODEC = PacketManager.codec(MessagePetEntry::encode, MessagePetEntry::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public UUID petEntryID;
    public String petEntryType;
    public boolean spawningActive;
    public boolean teleportEntity;
    public String summonType;
    public int subspecies;
    public int variant;
    public byte behaviour;
    public int petEntryEntityID = -1;
    public String petEntryEntityName;
    public int respawnTime;
    public int respawnTimeMax;
    /** S202: the pet's Bond experience (this slot carried the scrapped level + experience). **/
    public int bondExperience;
    public boolean isRespawning;

    public MessagePetEntry() {
    }

    public MessagePetEntry(ExtendedPlayer playerExt, PetEntry petEntry) {
        this.petEntryID = petEntry.getPetEntryID();
        this.petEntryType = petEntry.getType();
        this.spawningActive = petEntry.isSpawningActive();
        this.teleportEntity = petEntry.isTeleportRequested();
        SummonSet summonSet = petEntry.getSummonSet();
        this.summonType = summonSet.getSummonType();
        this.subspecies = petEntry.getSubspeciesIndex();
        this.variant = petEntry.getVariantIndex();
        this.behaviour = summonSet.getBehaviourByte();
        this.petEntryEntityID = petEntry.getEntity() != null ? petEntry.getEntity().getId() : -1;
        this.petEntryEntityName = petEntry.getEntityName();
        this.respawnTime = petEntry.getRespawnTime();
        this.respawnTimeMax = petEntry.getRespawnTimeMax();
        this.bondExperience = petEntry.getBondExperience();
        this.isRespawning = petEntry.isRespawning();
    }

    public static void handle(MessagePetEntry message, IPayloadContext context) {
        Player player = context.player();
        ExtendedPlayer playerExt = ExtendedPlayer.getForPlayer(player);
        if (playerExt == null) {
            return;
        }
        PetManager petManager = playerExt.getPetManager();
        PetEntry petEntry = petManager.getEntry(message.petEntryID);

        // Server Side (GUI changes from the client):
        if (context.flow().isServerbound()) {
            if (petEntry == null)
                return;
            petEntry.setSpawningActive(message.spawningActive);
            if (message.teleportEntity) {
                petEntry.requestTeleport();
            }
            SummonSet summonSet = petEntry.getSummonSet();
            summonSet.readFromPacket(message.summonType, message.subspecies, message.variant, message.behaviour);
            petEntry.onBehaviourUpdate();
            return;
        }

        // Client Side:
        if (petEntry == null) {
            petEntry = new PetEntry(message.petEntryID, message.petEntryType, player, message.summonType);
            petManager.addEntry(petEntry);
        }
        petEntry.setSpawningActive(message.spawningActive);
        if (message.teleportEntity) {
            petEntry.requestTeleport();
        }
        petEntry.setEntitySubspecies(message.subspecies);
        petEntry.setEntityVariant(message.variant);
        petEntry.getSummonSet().readFromPacket(message.summonType, message.subspecies, message.variant, message.behaviour);
        Entity entity = message.petEntryEntityID != -1 ? player.level().getEntity(message.petEntryEntityID) : null;
        petEntry.applyClientSync(entity, message.petEntryEntityName, message.respawnTime, message.respawnTimeMax, message.bondExperience, message.isRespawning);
    }

    public static MessagePetEntry decode(RegistryFriendlyByteBuf packet) {
        MessagePetEntry message = new MessagePetEntry();
        message.petEntryID = packet.readUUID();
        message.petEntryType = packet.readUtf(512);
        message.spawningActive = packet.readBoolean();
        message.teleportEntity = packet.readBoolean();
        message.summonType = packet.readUtf(512);
        message.subspecies = packet.readInt();
        message.variant = packet.readInt();
        message.behaviour = packet.readByte();
        message.petEntryEntityID = packet.readInt();
        message.petEntryEntityName = packet.readUtf(1024);
        message.respawnTime = packet.readInt();
        message.respawnTimeMax = packet.readInt();
        message.bondExperience = packet.readInt();
        message.isRespawning = packet.readBoolean();
        return message;
    }

    public static void encode(MessagePetEntry message, RegistryFriendlyByteBuf packet) {
        packet.writeUUID(message.petEntryID);
        packet.writeUtf(message.petEntryType);
        packet.writeBoolean(message.spawningActive);
        packet.writeBoolean(message.teleportEntity);
        packet.writeUtf(message.summonType == null ? "" : message.summonType);
        packet.writeInt(message.subspecies);
        packet.writeInt(message.variant);
        packet.writeByte(message.behaviour);
        packet.writeInt(message.petEntryEntityID);
        packet.writeUtf(message.petEntryEntityName == null ? "" : message.petEntryEntityName);
        packet.writeInt(message.respawnTime);
        packet.writeInt(message.respawnTimeMax);
        packet.writeInt(message.bondExperience);
        packet.writeBoolean(message.isRespawning);
    }
}
