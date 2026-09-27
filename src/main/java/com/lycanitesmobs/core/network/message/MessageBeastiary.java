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
import com.lycanitesmobs.core.data.info.gui.Beastiary;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;

/** Server -> client: the whole Beastiary (all creature knowledge). **/
public class MessageBeastiary implements CustomPacketPayload {
    public static final Type<MessageBeastiary> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, "beastiary"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MessageBeastiary> STREAM_CODEC = PacketManager.codec(MessageBeastiary::encode, MessageBeastiary::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public int entryAmount = 0;
    public String[] creatureNames;
    public int[] ranks;
    public int[] experience;

    public MessageBeastiary() {
    }

    public MessageBeastiary(Beastiary beastiary) {
        this.entryAmount = Math.min(201, beastiary.getCreatureKnowledgeCount());
        if (this.entryAmount > 0) {
            this.creatureNames = new String[this.entryAmount];
            this.ranks = new int[this.entryAmount];
            this.experience = new int[this.entryAmount];
            int i = 0;
            for (CreatureKnowledge creatureKnowledge : beastiary.getCreatureKnowledgeValues()) {
                if (i >= this.entryAmount) {
                    break;
                }
                this.creatureNames[i] = creatureKnowledge.getCreatureName();
                this.ranks[i] = creatureKnowledge.getRank();
                this.experience[i] = creatureKnowledge.getExperience();
                i++;
            }
        }
    }

    public static void handle(MessageBeastiary message, IPayloadContext context) {
        ExtendedPlayer playerExt = ExtendedPlayer.getForPlayer(context.player());
        if (playerExt == null || message.entryAmount < 0) {
            return;
        }
        playerExt.getBeastiary().clearCreatureKnowledge();
        for (int i = 0; i < message.entryAmount; i++) {
            playerExt.getBeastiary().putCreatureKnowledge(new CreatureKnowledge(playerExt.getBeastiary(), message.creatureNames[i], message.ranks[i], message.experience[i]));
        }
    }

    public static MessageBeastiary decode(RegistryFriendlyByteBuf packet) {
        MessageBeastiary message = new MessageBeastiary();
        message.entryAmount = Math.min(300, packet.readInt());
        if (message.entryAmount == 300) {
            LMHelperClass.logWarningMessage("Received 300 or more creature entries, something went wrong with the Beastiary packet! Addition entries will be skipped to prevent OOM!");
        }
        if (message.entryAmount > 0) {
            message.creatureNames = new String[message.entryAmount];
            message.ranks = new int[message.entryAmount];
            message.experience = new int[message.entryAmount];
            for (int i = 0; i < message.entryAmount; i++) {
                message.creatureNames[i] = packet.readUtf(32767);
                message.ranks[i] = packet.readInt();
                message.experience[i] = packet.readInt();
            }
        }
        return message;
    }

    public static void encode(MessageBeastiary message, RegistryFriendlyByteBuf packet) {
        packet.writeInt(message.entryAmount);
        for (int i = 0; i < message.entryAmount; i++) {
            packet.writeUtf(message.creatureNames[i]);
            packet.writeInt(message.ranks[i]);
            packet.writeInt(message.experience[i]);
        }
    }
}
