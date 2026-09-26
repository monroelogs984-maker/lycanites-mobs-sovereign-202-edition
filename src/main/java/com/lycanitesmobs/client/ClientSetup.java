package com.lycanitesmobs.client;

import com.lycanitesmobs.client.loader.ModelReloadListener;
import com.lycanitesmobs.client.manager.ModelManager;
import com.lycanitesmobs.client.renderer.entity.creature.CreatureRenderer;
import com.lycanitesmobs.core.data.info.creature.CreatureInfo;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.manager.CreatureManager;
import com.lycanitesmobs.core.manager.ObjectManager;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/**
 * Client-only setup. Kept in its own class (only ever touched from a Dist.isClient() guard in
 * LycanitesMobs' constructor) so nothing in here can be classloaded on a dedicated server -
 * EntityRenderersEvent and friends don't exist there.
 */
public class ClientSetup {
    /**
     * Ties CreatureModel's actual OBJ/parts.json loading into the real resource-reload
     * lifecycle (initial load AND F3+T reloads) - see ModelReloadListener's class doc for why
     * this is required, not optional: EntityRenderersEvent.RegisterRenderers fires before the
     * mod's own resources are available, so every model's first load attempt in its constructor
     * is expected to fail; this listener is what makes it actually work afterward.
     */
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(ModelReloadListener.INSTANCE);
    }

    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Build every ported CreatureModel (currently just calpod) before resolving renderers -
        // CreatureRenderer's constructor needs ModelManager to already have a model to hand back.
        ModelManager.getInstance().createModels();

        for (CreatureInfo creatureInfo : CreatureManager.getInstance().getCreatures()) {
            EntityType<? extends BaseCreatureEntity> entityType =
                    (EntityType<? extends BaseCreatureEntity>) ObjectManager.getEntityType(creatureInfo.getName());
            if (entityType == null) {
                continue;
            }

            // Creatures with a real ported model class get the real OBJ renderer; everything
            // else still falls back to the pig placeholder (see PlaceholderCreatureRenderer).
            if (ModelManager.getInstance().getCreatureModel(creatureInfo, null) != null) {
                String creatureName = creatureInfo.getName();
                LMHelperClass.logDebug("Resources", "ClientSetup: registering real CreatureRenderer for " + creatureName);
                event.registerEntityRenderer(entityType, context -> new CreatureRenderer<>(creatureName, context, 1.0F));
            } else {
                event.registerEntityRenderer(entityType, PlaceholderCreatureRenderer::new);
            }
        }
    }
}
