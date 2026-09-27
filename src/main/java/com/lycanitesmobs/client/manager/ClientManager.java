package com.lycanitesmobs.client.manager;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Trimmed to the render constants used by models and layers. The official ClientManager also holds GUI/keybind
 * state (Beastiary, pet GUIs) which isn't ported.
 */
@OnlyIn(Dist.CLIENT)
public class ClientManager {
    /** Packed full-bright light (block light 15), used by glowing model parts. **/
    public static int FULL_BRIGHT = 240;
    public static int GL_FULL_BRIGHT = 15728880;
}
