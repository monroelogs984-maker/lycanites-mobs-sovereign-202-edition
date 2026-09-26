package com.lycanitesmobs.client.renderer.entity.projectile;

import com.lycanitesmobs.core.entity.base.BaseProjectileEntity;
import com.lycanitesmobs.core.entity.projectile.generic.CustomProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Phase 6a: ported from the official ProjectileSpriteRenderer. Renders projectiles as camera-facing sprites from
 * textures/item/charges/&lt;name&gt;.png (vertical animation strips supported), and lasers as a line of sprites.
 *
 * <p>1.21.1: the vertex builder API (vertex/uv/uv2/endVertex -> addVertex/setUv/setLight, no endVertex); the
 * official CustomRenderStates sprite render types are replaced with vanilla entity render types. TODO(port): OBJ
 * model projectiles (ProjectileModelRenderer - aetherwave, chaosorb, crystalshard, lightball, lobdarklings) render as
 * sprites for now; old laser-end entities are not registered yet.
 */
public class ProjectileSpriteRenderer extends EntityRenderer<BaseProjectileEntity> {

    public ProjectileSpriteRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BaseProjectileEntity entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int brightness) {
        if (entity instanceof CustomProjectileEntity customProjectile && !customProjectile.hasProjectileInfo()) {
            return;
        }

        float loop = (float) entity.tickCount + (Minecraft.getInstance().isPaused() ? 0 : Math.min(1, partialTicks));
        float scale = entity.getProjectileScale();

        if (entity instanceof CustomProjectileEntity customProjectile && customProjectile.getLaserEnd() != null) {
            poseStack.pushPose();
            this.renderLaser(customProjectile, poseStack, bufferSource, customProjectile.getLaserWidth() / 4, loop, brightness);
            poseStack.popPose();
            return;
        }

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate(0, entity.getTextureOffsetY(), 0);
        poseStack.scale(scale, scale, scale);
        boolean emissive = this.isEmissive(entity);
        this.renderSprite(entity, poseStack, bufferSource, this.getRenderType(this.getTextureLocation(entity), emissive),
                entity.getTextureScale(), emissive ? LightTexture.FULL_BRIGHT : brightness);
        poseStack.popPose();
    }

    protected RenderType getRenderType(ResourceLocation texture, boolean emissive) {
        return emissive ? RenderType.entityTranslucentEmissive(texture) : RenderType.entityTranslucent(texture);
    }

    public void renderSprite(BaseProjectileEntity entity, PoseStack poseStack, MultiBufferSource bufferSource, RenderType renderType, float scale, int brightness) {
        float textureWidth = 0.25F;
        float textureHeight = 0.25F;
        float minU = 0;
        float maxU = 1;
        float minV = 0;
        float maxV = 1;
        if (entity.getAnimationFrameMax() > 0) {
            minV = (float) entity.getAnimationFrame() / (float) entity.getAnimationFrameMax();
            maxV = minV + (1F / (float) entity.getAnimationFrameMax());
            textureWidth *= scale;
            textureHeight *= scale;
        }

        PoseStack.Pose pose = poseStack.last();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
        vertex(vertexConsumer, pose, -textureWidth, -textureHeight + (textureHeight / 2), minU, maxV, brightness);
        vertex(vertexConsumer, pose, textureWidth, -textureHeight + (textureHeight / 2), maxU, maxV, brightness);
        vertex(vertexConsumer, pose, textureWidth, textureHeight + (textureHeight / 2), maxU, minV, brightness);
        vertex(vertexConsumer, pose, -textureWidth, textureHeight + (textureHeight / 2), minU, minV, brightness);
    }

    private static void vertex(VertexConsumer vertexConsumer, PoseStack.Pose pose, float x, float y, float u, float v, int brightness) {
        vertexConsumer.addVertex(pose, x, y, 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(brightness)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    public void renderLaser(CustomProjectileEntity entity, PoseStack poseStack, MultiBufferSource bufferSource, float scale, float loop, int brightness) {
        double laserSize = entity.position().distanceTo(entity.getLaserEnd());
        if (laserSize <= 0) return;
        float spacing = 1;
        double factor = spacing / laserSize;

        boolean emissive = this.isEmissive(entity);
        RenderType renderType = this.getRenderType(this.getTextureLocation(entity), emissive);
        int spriteBrightness = emissive ? LightTexture.FULL_BRIGHT : brightness;
        Vec3 direction = entity.getLaserEnd().subtract(entity.position()).normalize();

        for (float segment = 0; segment <= laserSize; segment += (float) factor) {
            poseStack.pushPose();
            poseStack.translate(segment * direction.x() * spacing, segment * direction.y() * spacing, segment * direction.z() * spacing);
            poseStack.translate(0, entity.getTextureOffsetY(), 0);
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.scale(scale, scale, scale);
            this.renderSprite(entity, poseStack, bufferSource, renderType, scale, spriteBrightness);
            poseStack.popPose();
        }
    }

    protected boolean isEmissive(BaseProjectileEntity entity) {
        if (entity instanceof CustomProjectileEntity customProjectile && customProjectile.hasProjectileInfo()) {
            return customProjectile.getProjectileInfo().glows();
        }
        return entity.getBrightness() >= 1.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(BaseProjectileEntity entity) {
        ResourceLocation texture = entity.getTexture();
        return texture != null ? texture : MissingTextureAtlasSprite.getLocation();
    }
}
