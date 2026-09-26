package com.lycanitesmobs.client;

import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Temporary stand-in renderer used until each creature's real OBJ model/animation pipeline is
 * ported (see PORT_PLAN.md Phase 8 - Lycanites uses an in-house OBJ model/animation format, not
 * a standard one like GeckoLib, so that's real, separate work, not a quick swap).
 * <p>
 * Reuses vanilla's PigModel + pig texture, scaled to each creature's own configured hitbox size
 * (MobRenderer/LivingEntityRenderer already scale by the entity's getScale()/dimensions). This
 * looks nothing like the real creature, but it's a real, moving, oriented, animated 3D body -
 * good enough to visually confirm AI/movement/combat/multi-entity chaining (e.g. concapede
 * segments following each other) while testing, which an invisible entity (the previous
 * version of this class) couldn't give you at all.
 */
public class PlaceholderCreatureRenderer extends MobRenderer<BaseCreatureEntity, PigModel<BaseCreatureEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/pig/pig.png");

    public PlaceholderCreatureRenderer(EntityRendererProvider.Context context) {
        super(context, new PigModel<>(context.bakeLayer(ModelLayers.PIG)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(BaseCreatureEntity entity) {
        return TEXTURE;
    }
}
