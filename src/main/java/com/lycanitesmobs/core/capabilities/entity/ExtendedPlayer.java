package com.lycanitesmobs.core.capabilities.entity;

import com.lycanitesmobs.LycanitesMobs;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Trimmed - only the rider control states are ported so far (used by RideableCreatureEntity). The original also
 * holds the Beastiary, pets/summoning, minion selection and more; those arrive with the pets system.
 *
 * The original was a Forge capability; this is a NeoForge data attachment. It isn't serialized since control
 * states are transient (both the client and the server keep their own copy, the client syncs it via
 * PlayerControlPayload).
 */
public class ExtendedPlayer {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, LycanitesMobs.MODID);
    public static final Supplier<AttachmentType<ExtendedPlayer>> EXTENDED_PLAYER = ATTACHMENT_TYPES.register("extended_player", () -> AttachmentType.builder(holder -> new ExtendedPlayer((Player) holder)).build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    public static ExtendedPlayer getForPlayer(Player player) {
        if (player == null) {
            return null;
        }
        return player.getData(EXTENDED_PLAYER);
    }

    public final Player player;

    // Action Controls:
    protected byte controlStates = 0;

    public enum CONTROL_ID {
        JUMP((byte) 1), MOUNT_DISMOUNT((byte) 2), MOUNT_ABILITY((byte) 4), MOUNT_INVENTORY((byte) 8), ATTACK((byte) 16), DESCEND((byte) 32);
        private final byte id;

        CONTROL_ID(byte i) {
            this.id = i;
        }

        public byte id() {
            return this.id;
        }
    }

    public ExtendedPlayer(Player player) {
        this.player = player;
    }

    public void updateControlStates(byte controlStates) {
        this.controlStates = controlStates;
    }

    public byte getControlStates() {
        return this.controlStates;
    }

    public boolean isControlActive(CONTROL_ID controlID) {
        return (this.controlStates & controlID.id()) > 0;
    }
}
