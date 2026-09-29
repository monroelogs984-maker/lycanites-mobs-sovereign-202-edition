package com.lycanitesmobs.client.event.mobevent;

import com.lycanitesmobs.client.util.helpers.DrawHelper;
import com.lycanitesmobs.core.event.mobevent.MobEvent;
import com.lycanitesmobs.core.manager.MobEventManager;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Client side mob event state per world: started/extended/finished chat messages, the event sound and the title
 * graphic (drawn by OverlayEvents). Port: fed by MessageMobEvent/MessageWorldEvent through LycanitesMobs.APPLY_MOB_EVENT
 * (was the ClientProxy), ticked from ClientTickEvent.Post, rendered with the overlay's DrawHelper.
 */
public class ClientMobEventEvents {
    private static class ClientWorldState {
        final Map<String, MobEventPlayerClient> mobEventPlayers = new HashMap<>();
        MobEventPlayerClient worldEventPlayer;
    }

    private static final Map<Level, ClientWorldState> WORLD_STATES = new WeakHashMap<>();

    public static void onClientUpdate(ClientTickEvent.Post event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        update(player.level());
    }

    private static ClientWorldState getState(Level world) {
        return WORLD_STATES.computeIfAbsent(world, ignored -> new ClientWorldState());
    }

    /** Network entry point (LycanitesMobs.APPLY_MOB_EVENT). **/
    public static void apply(String mobEventName, boolean worldEvent) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (worldEvent) {
            applyWorldEvent(player.level(), mobEventName);
        } else {
            applyMobEvent(player.level(), mobEventName);
        }
    }

    public static void applyMobEvent(Level world, String mobEventName) {
        if (world == null || !world.isClientSide) {
            return;
        }

        if ("".equals(mobEventName)) {
            stopMobEvent(world, mobEventName);
            return;
        }

        MobEvent mobEvent = MobEventManager.getInstance().getMobEvent(mobEventName);
        if (mobEvent == null) {
            LMHelperClass.logWarningMessage("Tried to start a client mob event with the invalid name: '" + mobEventName + "'.");
            return;
        }

        ClientWorldState state = getState(world);
        MobEventPlayerClient mobEventPlayerClient = state.mobEventPlayers.get(mobEvent.getName());
        boolean extended = mobEventPlayerClient != null && mobEventPlayerClient.mobEvent == mobEvent;
        if (!extended) {
            mobEventPlayerClient = new MobEventPlayerClient(mobEvent, world);
            state.mobEventPlayers.put(mobEvent.getName(), mobEventPlayerClient);
        }
        mobEventPlayerClient.extended = extended;

        Player clientPlayer = Minecraft.getInstance().player;
        if (clientPlayer != null) {
            mobEventPlayerClient.onStart(clientPlayer);
        }
    }

    public static void applyWorldEvent(Level world, String mobEventName) {
        if (world == null || !world.isClientSide) {
            return;
        }

        if ("".equals(mobEventName)) {
            stopWorldEvent(world);
            return;
        }

        MobEvent mobEvent = MobEventManager.getInstance().getMobEvent(mobEventName);
        if (mobEvent == null) {
            LMHelperClass.logWarningMessage("Tried to start a client world event with the invalid name: '" + mobEventName + "'.");
            return;
        }

        ClientWorldState state = getState(world);
        boolean extended = state.worldEventPlayer != null && state.worldEventPlayer.mobEvent == mobEvent;
        if (!extended) {
            state.worldEventPlayer = new MobEventPlayerClient(mobEvent, world);
        }
        state.worldEventPlayer.extended = extended;

        Player clientPlayer = Minecraft.getInstance().player;
        if (clientPlayer != null) {
            state.worldEventPlayer.onStart(clientPlayer);
        }
    }

    private static void stopMobEvent(Level world, String mobEventName) {
        ClientWorldState state = getState(world);
        MobEventPlayerClient mobEventPlayerClient = state.mobEventPlayers.remove(mobEventName);
        Player clientPlayer = Minecraft.getInstance().player;
        if (mobEventPlayerClient != null && clientPlayer != null) {
            mobEventPlayerClient.onFinish(clientPlayer);
        }
    }

    private static void stopWorldEvent(Level world) {
        ClientWorldState state = getState(world);
        Player clientPlayer = Minecraft.getInstance().player;
        if (state.worldEventPlayer != null && clientPlayer != null) {
            state.worldEventPlayer.onFinish(clientPlayer);
        }
        state.worldEventPlayer = null;
    }

    private static void update(Level world) {
        if (world == null || !world.isClientSide) {
            return;
        }

        ClientWorldState state = getState(world);
        for (MobEventPlayerClient mobEventPlayerClient : state.mobEventPlayers.values()) {
            mobEventPlayerClient.onUpdate();
        }
        if (state.worldEventPlayer != null) {
            state.worldEventPlayer.onUpdate();
        }
    }

    public static void render(Level world, GuiGraphics guiGraphics, DrawHelper drawHelper, int sWidth, int sHeight) {
        if (world == null || !world.isClientSide) {
            return;
        }

        ClientWorldState state = getState(world);
        for (MobEventPlayerClient mobEventPlayerClient : state.mobEventPlayers.values()) {
            mobEventPlayerClient.onGUIUpdate(guiGraphics, drawHelper, sWidth, sHeight);
        }
        if (state.worldEventPlayer != null) {
            state.worldEventPlayer.onGUIUpdate(guiGraphics, drawHelper, sWidth, sHeight);
        }
    }
}
