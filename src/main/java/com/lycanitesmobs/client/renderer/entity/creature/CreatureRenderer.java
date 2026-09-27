package com.lycanitesmobs.client.renderer.entity.creature;

import com.lycanitesmobs.client.renderer.layer.creature.LayerCreatureBase;
import com.lycanitesmobs.client.manager.ModelManager;
import com.lycanitesmobs.client.model.creature.base.CreatureModel;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.manager.CreatureManager;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Heavily trimmed from the original: instead of overriding render() with a from-scratch
 * reimplementation of vanilla's pose-stack rotation/scale/translate setup (risky to hand-port -
 * see CreatureModel's class doc), this relies entirely on vanilla's own
 * LivingEntityRenderer.render() dispatch, which already calls into CreatureModel's
 * setupAnim()/renderToBuffer() hooks to drive the actual OBJ rendering. That also means:
 * NeoForge RenderLivingEvent/RenderNameTagEvent posting and per-entity subspecies model swapping
 * are NOT ported here (see PORT_PLAN.md).
 */
@OnlyIn(Dist.CLIENT)
public class CreatureRenderer<T extends BaseCreatureEntity> extends MobRenderer<T, CreatureModel<T>> {

    public CreatureRenderer(String entityID, EntityRendererProvider.Context context, float shadowSize) {
        super(context, ModelManager.getInstance().getCreatureModel(CreatureManager.getInstance().getCreature(entityID), null), shadowSize);
        // The official renderer adds these once its model is resolved; the port's model is fixed here.
        this.model.addCustomLayers(this);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return entity.getTexture();
    }

    /**
     * Kept for LayerCreatureBase (a currently-unused stub, see its class doc) which still
     * references this.renderer.getMainModel().
     */
    public CreatureModel<T> getMainModel() {
        return this.model;
    }

    /**
     * Gets the texture for a render pass, the base pass (null layer) uses the entity texture.
     */
    public ResourceLocation getEntityTexture(BaseCreatureEntity entity, LayerCreatureBase layer) {
        if (layer == null) {
            return entity.getTexture();
        }
        ResourceLocation layerTexture = layer.getLayerTexture(entity);
        return layerTexture != null ? layerTexture : entity.getTexture();
    }

    /**
     * Public so CreatureModel.addCustomLayers() can add effect layers (addLayer is protected).
     */
    public void addCreatureLayer(LayerCreatureBase<T> layer) {
        this.addLayer(layer);
    }
}
