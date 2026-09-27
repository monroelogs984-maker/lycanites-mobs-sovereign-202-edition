package com.lycanitesmobs.client.manager;

import com.lycanitesmobs.core.capabilities.entity.ExtendedPlayer;
import com.lycanitesmobs.core.network.PlayerControlPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.lycanitesmobs.client.gui.screen.beastiary.CreaturesBeastiaryScreen;
import com.lycanitesmobs.client.gui.screen.beastiary.IndexBeastiaryScreen;
import com.lycanitesmobs.client.gui.screen.beastiary.PetsBeastiaryScreen;
import com.lycanitesmobs.client.gui.screen.beastiary.SummoningBeastiaryScreen;
import com.lycanitesmobs.client.gui.screen.creature.MinionSelectionScreen;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Mount keys plus the Beastiary / pets / summoning / minion selection screen keys. The original's dismount key isn't
 * registered (the original never reads it - vanilla sneak-to-dismount is used). The control-state tick and screen
 * keys are ported from ClientEventListener.onPlayerTick()/onKeyInput().
 */
public class KeyManager {
    public static final String CATEGORY = "lycanitesmobs";

    public static final KeyMapping descend = new KeyMapping("key.mount.descend", GLFW.GLFW_KEY_LEFT_ALT, CATEGORY);
    public static final KeyMapping mountAbility = new KeyMapping("key.mount.ability", GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyMapping mountInventory = new KeyMapping("key.mount.inventory", GLFW.GLFW_KEY_K, CATEGORY);

    public static final KeyMapping beastiary = new KeyMapping("key.beastiary", GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
    public static final KeyMapping index = new KeyMapping("key.index", GLFW.GLFW_KEY_B, CATEGORY);
    public static final KeyMapping pets = new KeyMapping("key.pets", GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
    public static final KeyMapping summoning = new KeyMapping("key.summoning", GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
    public static final KeyMapping minionSelection = new KeyMapping("key.minions", GLFW.GLFW_KEY_R, CATEGORY);

    /**
     * Closes the minion selection screen when its key is released.
     */
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && event.getKey() == minionSelection.getKey().getValue() && mc.screen instanceof MinionSelectionScreen && event.getAction() == GLFW.GLFW_RELEASE) {
            mc.player.closeContainer();
        }
    }

    /**
     * Opens a screen requested from common code (e.g. the summoning staff with no summon set selected).
     */
    public static void openScreen(int screenId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (screenId == SCREEN_SUMMONING) {
            mc.setScreen(new SummoningBeastiaryScreen(mc.player));
        } else {
            mc.setScreen(new IndexBeastiaryScreen(mc.player));
        }
    }

    public static final int SCREEN_BEASTIARY = 0;
    public static final int SCREEN_SUMMONING = 1;

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(descend);
        event.register(mountAbility);
        event.register(mountInventory);
        event.register(beastiary);
        event.register(index);
        event.register(pets);
        event.register(summoning);
        event.register(minionSelection);
    }

    /**
     * Builds this tick's control states and syncs them to the server when they change. The client keeps its own
     * copy too, since mounted movement (travel) runs on the client for the controlling player.
     */
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            return;
        }
        ExtendedPlayer playerExt = ExtendedPlayer.getForPlayer(mc.player);
        if (playerExt == null) {
            return;
        }

        byte controlStates = 0;
        if (mountInventory.consumeClick()) {
            controlStates += ExtendedPlayer.CONTROL_ID.MOUNT_INVENTORY.id();
        }
        // Beastiary Screens:
        if (index.consumeClick()) {
            mc.setScreen(new IndexBeastiaryScreen(mc.player));
        }
        if (beastiary.consumeClick()) {
            mc.setScreen(new CreaturesBeastiaryScreen(mc.player));
        }
        if (pets.consumeClick()) {
            mc.setScreen(new PetsBeastiaryScreen(mc.player));
        }
        if (summoning.consumeClick()) {
            mc.setScreen(new SummoningBeastiaryScreen(mc.player));
        }

        if (mc.isWindowActive() && mc.screen == null) {
            // Minion Selection (held open while the key is down, see onKeyInput):
            if (minionSelection.consumeClick()) {
                mc.setScreen(new MinionSelectionScreen(mc.player));
            }

            if (mc.options.keyJump.isDown())
                controlStates += ExtendedPlayer.CONTROL_ID.JUMP.id();
            if (descend.isDown())
                controlStates += ExtendedPlayer.CONTROL_ID.DESCEND.id();
            if (mountAbility.isDown())
                controlStates += ExtendedPlayer.CONTROL_ID.MOUNT_ABILITY.id();
            if (mc.options.keyAttack.isDown())
                controlStates += ExtendedPlayer.CONTROL_ID.ATTACK.id();
        }

        if (controlStates == playerExt.getControlStates()) {
            return;
        }
        PacketDistributor.sendToServer(new PlayerControlPayload(controlStates));
        playerExt.updateControlStates(controlStates);
    }
}
