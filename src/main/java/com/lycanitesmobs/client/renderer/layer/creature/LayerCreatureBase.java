package com.lycanitesmobs.client.renderer.layer.creature;

import com.lycanitesmobs.client.model.creature.base.CreatureModel;
import com.lycanitesmobs.client.renderer.entity.creature.CreatureRenderer;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2f;
import org.joml.Vector4f;

/**
 * Base layer type only - none of the original's subclasses (equipment/saddle/dye/effect/
 * scrolling) are ported yet since CreatureModel.addCustomLayers() is a no-op for now (see
 * PORT_PLAN.md). This still needs to exist as a real type since CreatureModel/CreatureObjModel's
 * render() method signatures take a LayerCreatureBase parameter (always null - the base layer -
 * until real extra layers are ported).
 */
@OnlyIn(Dist.CLIENT)
public class LayerCreatureBase<T extends BaseCreatureEntity> extends RenderLayer<T, CreatureModel<T>> {
    public static final Vector4f WHITE = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
    public static final Vector2f ZERO_TEXTURE_OFFSET = new Vector2f(0.0F, 0.0F);

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
        // This isn't used by the custom renderer.
    }

    public boolean canRenderLayer(BaseCreatureEntity entity, float scale) {
        if (entity == null)
            return false;
        if (entity.isInvisible() && entity.isInvisibleTo(Minecraft.getInstance().player))
            return false;
        return true;
    }

    public ResourceLocation getLayerTexture(BaseCreatureEntity entity) {
        return null;
    }

    public boolean canRenderPart(String partName, BaseCreatureEntity entity, boolean trophy) {
        if (this.renderer.getMainModel() != null) {
            this.renderer.getMainModel().canBaseRenderPart(partName, entity, trophy);
        }
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
