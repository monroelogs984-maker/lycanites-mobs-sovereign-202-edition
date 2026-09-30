package com.lycanitesmobs.client.renderer.layer.projectile;

import com.lycanitesmobs.client.renderer.entity.projectile.ProjectileModelRenderer;
import com.lycanitesmobs.client.renderer.util.CustomRenderStates;
import com.lycanitesmobs.core.entity.base.BaseProjectileEntity;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class LayerProjectileEffect extends LayerProjectileBase {

    public String textureSuffix;
    public boolean subspecies = true;
    public Vector2f scrollSpeed;
    private final Vector2f textureOffsetScratch = new Vector2f();


    public LayerProjectileEffect(ProjectileModelRenderer renderer, String textureSuffix) {
        super(renderer);
        this.name = textureSuffix;
        this.textureSuffix = textureSuffix;
    }

    public LayerProjectileEffect(ProjectileModelRenderer renderer, String textureSuffix, boolean glow, int blending, boolean subspecies) {
        super(renderer);
        this.name = textureSuffix;
        this.textureSuffix = textureSuffix;
        this.glow = glow;
        this.blending = blending;
        this.subspecies = subspecies;
    }

    @Override
    public Vector4f getPartColor(String partName, BaseProjectileEntity entity) {
        return CustomRenderStates.WHITE;
    }

    @Override
    public ResourceLocation getLayerTexture(BaseProjectileEntity entity) {
        return super.getLayerTexture(entity);
    }

    @Override
    public Vector2f getTextureOffset(String partName, BaseProjectileEntity entity, float loop) {
        if (this.scrollSpeed == null) {
            return CustomRenderStates.ZERO_TEXTURE_OFFSET;
        }
        if (this.scrollSpeed.x == 0.0F && this.scrollSpeed.y == 0.0F) {
            return CustomRenderStates.ZERO_TEXTURE_OFFSET;
        }
        return this.textureOffsetScratch.set(loop * this.scrollSpeed.x, loop * this.scrollSpeed.y);
    }

    @Override
    public int getBrightness(String partName, BaseProjectileEntity entity, int brightness) {
        if (this.glow) {
            return 240;
        }
        return brightness;
    }
}
