package com.lycanitesmobs.client;

import com.lycanitesmobs.client.loader.ModelReloadListener;
import com.lycanitesmobs.client.manager.ModelManager;
import com.lycanitesmobs.client.renderer.entity.creature.CreatureRenderer;
import com.lycanitesmobs.core.data.info.creature.CreatureInfo;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.manager.CreatureManager;
import com.lycanitesmobs.core.manager.ObjectManager;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import com.lycanitesmobs.client.renderer.entity.projectile.ProjectileSpriteRenderer;
import com.lycanitesmobs.core.data.info.projectile.ProjectileInfo;
import com.lycanitesmobs.core.entity.base.BaseProjectileEntity;
import com.lycanitesmobs.core.manager.ProjectileManager;
import net.minecraft.world.entity.EntityType;
import com.lycanitesmobs.client.item.ItemColorCustomSpawnEgg;
import com.lycanitesmobs.core.data.info.creature.CreatureType;
import com.lycanitesmobs.core.manager.FluidManager;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.joml.Vector3f;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
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

    public static void registerMenuScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(com.lycanitesmobs.core.container.creature.CreatureContainer.TYPE.get(), com.lycanitesmobs.client.gui.screen.creature.CreatureInventoryScreen::new);
    }

    public static void setClientPlayerSupplier() {
        com.lycanitesmobs.client.manager.TextureManager.getInstance().createTextures(com.lycanitesmobs.LycanitesMobs.modInfo);
        com.lycanitesmobs.LycanitesMobs.CLIENT_PLAYER = () -> net.minecraft.client.Minecraft.getInstance().player;
        com.lycanitesmobs.LycanitesMobs.OPEN_SCREEN = com.lycanitesmobs.client.manager.KeyManager::openScreen;
    }

    /**
     * Spawn egg tints (one egg item per creature type, colored per creature).
     */
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        ItemColorCustomSpawnEgg spawnEggColor = new ItemColorCustomSpawnEgg();
        for (CreatureType creatureType : CreatureManager.getInstance().getCreatureTypes()) {
            if (creatureType.getSpawnEggItem() != null) {
                event.register(spawnEggColor, creatureType.getSpawnEggItem());
            }
        }
    }

    /**
     * Fluid rendering (textures + fog) for the 8 custom fluids. Without these NeoForge has no sprites for the fluid
     * types and the client crashes the moment one is in view. The textures are already colored, so the default
     * white tint is kept (the official passed the fluid color as tint with no alpha byte); the fluid color is used
     * for the underwater fog, with the official's short fog range.
     */
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        for (FluidManager.FluidEntry fluid : FluidManager.getInstance().getFluids()) {
            ResourceLocation stillTexture = fluid.stillTexture();
            ResourceLocation flowingTexture = fluid.flowingTexture();
            int color = fluid.color();
            Vector3f fogColor = new Vector3f(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f);
            event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return stillTexture;
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return flowingTexture;
                }

                @Override
                public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
                    return new Vector3f(fogColor);
                }

                @Override
                public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance, float partialTick, float nearDistance, float farDistance, FogShape shape) {
                    RenderSystem.setShaderFogStart(1f);
                    RenderSystem.setShaderFogEnd(6f);
                }
            }, fluid.type().get());
        }
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

        // Projectiles (Phase 6a): every JSON projectile renders as a sprite (model projectiles too, for now).
        for (ProjectileInfo projectileInfo : ProjectileManager.getInstance().getProjectiles()) {
            EntityType<? extends BaseProjectileEntity> entityType =
                    (EntityType<? extends BaseProjectileEntity>) (EntityType<?>) ObjectManager.getEntityType(projectileInfo.getName());
            if (entityType != null) {
                event.registerEntityRenderer(entityType, ProjectileSpriteRenderer::new);
            }
        }
        // Old (hardcoded) sprite projectiles, e.g. rapidfire.
        for (java.util.Map.Entry<String, Class<? extends net.minecraft.world.entity.Entity>> entry : ProjectileManager.getInstance().getOldSpriteProjectileEntries()) {
            EntityType<? extends BaseProjectileEntity> entityType =
                    (EntityType<? extends BaseProjectileEntity>) (EntityType<?>) ObjectManager.getEntityType(entry.getKey());
            if (entityType != null) {
                event.registerEntityRenderer(entityType, ProjectileSpriteRenderer::new);
            }
        }
    }
}
