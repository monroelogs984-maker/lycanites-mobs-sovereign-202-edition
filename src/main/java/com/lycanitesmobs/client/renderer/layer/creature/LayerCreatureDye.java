package com.lycanitesmobs.client.renderer.layer.creature;

import com.lycanitesmobs.client.renderer.entity.creature.CreatureRenderer;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.creature.aberration.EntityYale;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector4f;

/**
 * Renders a dyeable part (Yale wool, collars) tinted with the creature's dye color.
 *
 * <p>1.21.1: DyeColor.getTextureDiffuseColors() (float[]) became getTextureDiffuseColor() (packed ARGB int).
 */
@OnlyIn(Dist.CLIENT)
public class LayerCreatureDye extends LayerCreatureBase {
    public String textureSuffix;
    public boolean subspecies = true;

    public LayerCreatureDye(CreatureRenderer renderer, String textureSuffix, boolean subspecies) {
        super(renderer);
        this.name = textureSuffix;
        this.textureSuffix = textureSuffix;
        this.subspecies = subspecies;
    }

    public LayerCreatureDye(CreatureRenderer renderer, String name, String textureSuffix, boolean subspecies) {
        super(renderer);
        this.name = name;
        this.textureSuffix = textureSuffix;
        this.subspecies = subspecies;
    }

    @Override
    public boolean canRenderLayer(BaseCreatureEntity entity, float scale) {
        if (!super.canRenderLayer(entity, scale))
            return false;
        if (!(entity instanceof EntityYale yale))
            return true;
        return yale.hasFur();
    }

    @Override
    public boolean canRenderPart(String partName, BaseCreatureEntity entity, boolean trophy) {
        return this.name.equals(partName);
    }

    @Override
    public Vector4f getPartColor(String partName, BaseCreatureEntity entity, boolean trophy) {
        int color = entity.getColor().getTextureDiffuseColor();
        return new Vector4f(FastColor.ARGB32.red(color) / 255F, FastColor.ARGB32.green(color) / 255F, FastColor.ARGB32.blue(color) / 255F, 1.0F);
    }

    @Override
    public ResourceLocation getLayerTexture(BaseCreatureEntity entity) {
        if (this.subspecies) {
            return entity.getTexture(this.textureSuffix);
        }
        return entity.getSubTexture(this.textureSuffix);
    }
}
