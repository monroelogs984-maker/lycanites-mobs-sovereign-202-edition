package com.lycanitesmobs.core.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers the mod's network payloads (replaces the original's Forge SimpleChannel PacketHandler).
 * Only the messages needed by ported systems exist so far.
 */
public class PacketManager {
    public static final String PROTOCOL_VERSION = "1";

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(PlayerControlPayload.TYPE, PlayerControlPayload.STREAM_CODEC, PlayerControlPayload::handle);
    }
}
