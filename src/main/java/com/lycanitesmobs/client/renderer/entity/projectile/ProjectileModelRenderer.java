package com.lycanitesmobs.client.renderer.entity.projectile;

import com.google.common.collect.Lists;
import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.client.manager.ModelManager;
import com.lycanitesmobs.client.model.projectile.base.ProjectileModel;
import com.lycanitesmobs.client.model.projectile.base.ProjectileObjModel;
import com.lycanitesmobs.client.renderer.util.CustomRenderStates;
import com.lycanitesmobs.client.renderer.layer.projectile.LayerProjectileBase;
import com.lycanitesmobs.core.entity.base.BaseProjectileEntity;
import com.lycanitesmobs.core.data.info.projectile.ProjectileInfo;
import com.lycanitesmobs.core.manager.ProjectileManager;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ProjectileModelRenderer extends EntityRenderer<BaseProjectileEntity> implements RenderLayerParent<BaseProjectileEntity, ProjectileModel> {
    protected final List<RenderLayer<BaseProjectileEntity, ProjectileModel>> renderLayers = Lists.newArrayList(); // TODO Layers for projectiles.
    protected ProjectileModel renderModel;
    protected ProjectileModel defaultModel;


    public ProjectileModelRenderer(EntityRendererProvider.Context renderManager, ProjectileInfo projectileInfo) {
        super(renderManager);
        // Port: the official also had a constructor for "old" (hardcoded) projectile models; none are registered upstream.
        this.renderModel = ModelManager.getInstance().getProjectileModel(projectileInfo);
        this.defaultModel = this.renderModel;
        this.renderModel.addCustomLayers(this);
    }

    @Override
    public void render(BaseProjectileEntity entity, float yaw, float partialTicks, PoseStack matrixStack, MultiBufferSource renderTypeBuffer, int brightness) { // Port: vanilla parameter order (the official had yaw and partialTicks swapped)
        // Model States:
        float time = 0;
        float distance = 0;
        float loop = (float) entity.tickCount + (Minecraft.getInstance().isPaused() ? 0 : (partialTicks % 1.0F));
        float lookYaw = 0;
        float lookPitch = 0;
        float scale = 1;
        boolean invisible = false;
        boolean allyInvisible = false;

        // Render Model and Layers:
        try {
            matrixStack.pushPose();
            matrixStack.translate(0, -0.25F, 0); // translate
            matrixStack.scale(0.25F, 0.25F, 0.25F); // scale
            matrixStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(entity.yRotO), 0.0F, 1.0F, 0.0F))); // rotate

            if (this.getModel() == null) {
                LMHelperClass.logWarningMessage("Missing Projectile Model: " + entity);
            } else if (!(this.getModel() instanceof ProjectileObjModel)) {
                ResourceLocation texture = this.getTextureLocation(entity);
                if (texture != null) {
                    RenderType renderType = CustomRenderStates.getObjRenderType(texture, this.renderModel.getBlending(entity, null), this.renderModel.getGlow(entity, null));
                    this.getModel().render(entity, matrixStack, renderTypeBuffer.getBuffer(renderType), null, 0, 0, loop, 0, 0, scale, brightness);
                }
            } else {
                this.getModel().generateAnimationFrames(entity, time, distance, loop, lookYaw, lookPitch, 1, brightness);
                this.renderModel(entity, matrixStack, renderTypeBuffer, null, time, distance, loop, lookYaw, lookPitch, 1, brightness, invisible, allyInvisible);
                for (RenderLayer<BaseProjectileEntity, ProjectileModel> layer : this.renderLayers) {
                    if (!(layer instanceof LayerProjectileBase layerProjectileBase)) {
                        continue;
                    }
                    if (!layerProjectileBase.canRenderLayer(entity, scale)) {
                        continue;
                    }
                    this.renderModel(entity, matrixStack, renderTypeBuffer, layerProjectileBase, time, distance, loop, lookYaw, lookPitch, scale, brightness, invisible, allyInvisible);
                }
                this.getModel().clearAnimationFrames();
            }
            matrixStack.popPose();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        super.render(entity, yaw, partialTicks, matrixStack, renderTypeBuffer, brightness);
    }

    /**
     * Renders the model for one pass (the base pass has a null layer).
     *
     * Port: draws into a buffered render type from CustomRenderStates.getObjRenderType, like the creature layers,
     * instead of the official VBO/Iris batching (VBOObjModel/OculusCompat).
     */
    protected void renderModel(BaseProjectileEntity entity, PoseStack matrixStack, MultiBufferSource renderTypeBuffer, LayerProjectileBase layer, float time, float distance, float loop, float lookY, float lookX, float scale, int brightness, boolean invisible, boolean allyInvisible) {
        ResourceLocation texture = this.getEntityTexture(entity, layer);
        if (texture == null || (invisible && !allyInvisible)) {
            return;
        }
        int blending = this.getModel().getBlending(entity, layer);
        boolean glow = this.getModel().getGlow(entity, layer);
        RenderType renderType = CustomRenderStates.getObjRenderType(texture, blending, glow);
        this.getModel().render(entity, matrixStack, renderTypeBuffer.getBuffer(renderType), layer, time, distance, loop, lookY, lookX, 1, brightness);
    }


    //@Override
    public ProjectileModel getModel() {
        return this.renderModel;
    }

    public final boolean addLayer(RenderLayer<BaseProjectileEntity, ProjectileModel> layer) {
        return this.renderLayers.add(layer);
    }

    /**
     * Gets the texture to use.
     *
     * @param entity The entity to get the texture from.
     * @param layer  The layer to get the texture for.
     * @return The texture to bind.
     */
    public ResourceLocation getEntityTexture(BaseProjectileEntity entity, LayerProjectileBase layer) {
        if (layer == null) {
            return this.getTextureLocation(entity);
        }
        ResourceLocation layerTexture = layer.getLayerTexture(entity);
        return layerTexture != null ? layerTexture : this.getTextureLocation(entity);
    }

    @Override
    public ResourceLocation getTextureLocation(BaseProjectileEntity entity) {
        return entity.getTexture();
    }

}
