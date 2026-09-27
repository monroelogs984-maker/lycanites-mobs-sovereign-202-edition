package com.lycanitesmobs.client.manager;

import com.lycanitesmobs.core.capabilities.entity.ExtendedPlayer;
import com.lycanitesmobs.core.network.PlayerControlPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Trimmed - only the mount keys are ported. The original also registers Beastiary/index/pets/summoning/minion
 * keys (those screens aren't ported yet) and a dismount key that the original never actually reads (vanilla
 * sneak-to-dismount is used). The control-state tick is ported from ClientEventListener.onPlayerTick().
 */
public class KeyManager {
    public static final String CATEGORY = "lycanitesmobs";

    public static final KeyMapping descend = new KeyMapping("key.mount.descend", GLFW.GLFW_KEY_LEFT_ALT, CATEGORY);
    public static final KeyMapping mountAbility = new KeyMapping("key.mount.ability", GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyMapping mountInventory = new KeyMapping("key.mount.inventory", GLFW.GLFW_KEY_K, CATEGORY);

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(descend);
        event.register(mountAbility);
        event.register(mountInventory);
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
        if (mc.isWindowActive() && mc.screen == null) {
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
