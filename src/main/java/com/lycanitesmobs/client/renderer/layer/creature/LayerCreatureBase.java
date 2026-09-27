package com.lycanitesmobs.client.renderer.layer.creature;

import com.lycanitesmobs.client.model.creature.base.CreatureModel;
import com.lycanitesmobs.client.renderer.entity.creature.CreatureRenderer;
import com.lycanitesmobs.client.renderer.util.CustomRenderStates;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2f;
import org.joml.Vector4f;

/**
 * An extra render pass of a creature model (glow, eyes, scrolling effects, dye). Each layer re-renders the model's
 * parts it allows with its own texture, blending, color and brightness.
 *
 * <p>Port note: the official CreatureRenderer.render() loops its layers itself. The port uses vanilla's
 * LivingEntityRenderer.render(), which calls every RenderLayer.render() with the model's pose stack right after the
 * base model is drawn - so the layer pass happens here, re-rendering the model with the animation state
 * CreatureModel.setupAnim() stashed for this frame.
 */
@OnlyIn(Dist.CLIENT)
public class LayerCreatureBase<T extends BaseCreatureEntity> extends RenderLayer<T, CreatureModel<T>> {
    public static final Vector4f WHITE = CustomRenderStates.WHITE;
    public static final Vector2f ZERO_TEXTURE_OFFSET = CustomRenderStates.ZERO_TEXTURE_OFFSET;

    public CreatureRenderer renderer;
    public String name;
    public boolean glow = false;
    public int blending = 0;

    public LayerCreatureBase(CreatureRenderer renderer) {
        super(renderer);
        this.renderer = renderer;
        this.name = "Layer";
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!this.canRenderLayer(entity, 1)) {
            return;
        }
        CreatureModel<T> model = this.getParentModel();
        ResourceLocation texture = this.renderer.getEntityTexture(entity, this);
        RenderType renderType = CustomRenderStates.getObjRenderType(texture, model.getBlending(entity, this), model.getGlow(entity, this));
        model.renderLayer(poseStack, bufferSource.getBuffer(renderType), this, packedLight);
    }

    /**
     * Returns if this layer should be rendered.
     **/
    public boolean canRenderLayer(BaseCreatureEntity entity, float scale) {
        if (entity == null)
            return false;
        if (entity.isInvisible() && entity.isInvisibleTo(Minecraft.getInstance().player))
            return false;
        return true;
    }

    /**
     * Gets the texture that this layer should use, or null to use the base texture.
     **/
    public ResourceLocation getLayerTexture(BaseCreatureEntity entity) {
        return null;
    }

    /**
     * Returns if this layer can render the provided model part. Official behaviour kept: the base check's result
     * is ignored, so layers render every part unless a subclass filters them.
     **/
    public boolean canRenderPart(String partName, BaseCreatureEntity entity, boolean trophy) {
        return true;
    }

    public Vector4f getPartColor(String partName, BaseCreatureEntity entity, boolean trophy) {
        return WHITE;
    }

    public Vector2f getTextureOffset(String partName, BaseCreatureEntity entity, boolean trophy, float loop) {
        return ZERO_TEXTURE_OFFSET;
    }

    public int getBrightness(String partName, BaseCreatureEntity entity, int brightness) {
        return brightness;
    }

    public int getBlending(BaseCreatureEntity entity) {
        return this.blending;
    }

    public boolean getGlow(BaseCreatureEntity entity) {
        return this.glow;
    }
}
