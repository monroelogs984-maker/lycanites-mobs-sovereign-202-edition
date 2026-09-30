package com.lycanitesmobs.client.renderer;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Draws nothing. Used for dummy creatures such as the Fear ghost (the official FearRenderer's mesh rendering is
 * commented out upstream, so it renders nothing either).
 */
public class NoneRenderer<T extends Entity> extends EntityRenderer<T> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    public NoneRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(T entity, net.minecraft.client.renderer.culling.Frustum camera, double camX, double camY, double camZ) {
        return false;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TEXTURE;
    }
}
