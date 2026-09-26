package com.lycanitesmobs.client.model.creature.base;

import com.lycanitesmobs.client.model.animation.IAnimationModel;
import com.lycanitesmobs.client.renderer.entity.creature.CreatureRenderer;
import com.lycanitesmobs.client.renderer.layer.creature.LayerCreatureBase;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector4f;

/**
 * Trimmed - addCustomLayers() is a no-op for now (the original adds LayerCreatureEquipment/
 * LayerCreatureSaddle here; neither the equipment system nor those layer classes are ported -
 * see PORT_PLAN.md).
 *
 * Architecture change vs. the original: instead of porting the original CreatureRenderer's own
 * from-scratch render() override (which reimplements pose-stack rotation/scale/translate setup
 * that 1.21.1's LivingEntityRenderer.render() already does - and whose reference source uses a
 * suspicious Mth.clamp() where modern vanilla uses Mth.rotLerp(), suggesting it predates a real
 * MC API and can't be trusted to hand-port blind), this model instead hooks into the two calls
 * vanilla's own LivingEntityRenderer.render() already makes on every model: setupAnim() (stash
 * the entity + animation params, then generate this frame's animation) and renderToBuffer()
 * (actually draw, using the stashed params). This gets vanilla's rotation/scale/translate/name-tag/
 * leash handling for free and correctly, at the cost of the original's per-model-instance
 * subspecies swapping (out of scope for now - CreatureRenderer resolves one fixed model at
 * construction). Since vanilla's render() already applies entity.getScale() to the whole pose
 * stack before calling setupAnim/renderToBuffer, CreatureObjModel must NOT re-apply
 * entity.getScale() itself (the original did, because its own render() never called
 * poseStack.scale(entity.getScale()) at all).
 */
@OnlyIn(Dist.CLIENT)
public abstract class CreatureModel<T extends BaseCreatureEntity> extends EntityModel<T> implements IAnimationModel {

    public PoseStack matrixStack;

    protected T currentEntity;
    protected float currentTime;
    protected float currentDistance;
    protected float currentLoop;
    protected float currentLookY;
    protected float currentLookX;

    public CreatureModel() {
        this(1.0F);
    }

    // NOTE: 1.21.1's Model base class now requires a RenderType lookup function in its
    // constructor - only used by vanilla's own getRenderType()/renderType() dispatch (which
    // CreatureRenderer relies on to pick a RenderType/VertexConsumer for renderToBuffer()).
    // entityCutoutNoCull (double-sided), not entityCutout (single-sided) - the official source's
    // own OBJ render types (CustomRenderStates.OBJ_CUTOUT) default to no-cull too, only opting
    // into backface culling per-part via each ObjPart's cullBackfaces flag (not ported - no
    // creature's _parts.json sets it yet). Single-sided culling here was hiding roughly half of
    // every thin part's faces depending on view angle/triangle winding, which is exactly what
    // made calpod look like a shattered mosaic instead of a solid insect body.
    public CreatureModel(float shadowSize) {
        super(RenderType::entityCutoutNoCull);
    }

    @Override
    public void setupAnim(T entity, float time, float distance, float loop, float lookY, float lookX) {
        this.currentEntity = entity;
        this.currentTime = time;
        this.currentDistance = distance;
        this.currentLoop = loop;
        this.currentLookY = lookY;
        this.currentLookX = lookX;
        this.generateAnimationFrames(entity, time, distance, loop, lookY, lookX, 1, 0);
    }

    @Override
    public void renderToBuffer(PoseStack matrixStack, VertexConsumer vertexBuilder, int packedLight, int packedOverlay, int color) {
        if (this.currentEntity == null) {
            return;
        }
        this.matrixStack = matrixStack;
        int fade = Math.max(this.currentEntity.hurtTime, 0);
        this.render(this.currentEntity, matrixStack, vertexBuilder, null, this.currentTime, this.currentDistance, this.currentLoop, this.currentLookY, this.currentLookX, 1, packedLight, fade);
        this.clearAnimationFrames();
    }

    public void generateAnimationFrames(BaseCreatureEntity entity, float time, float distance, float loop, float lookY, float lookX, float scale, int brightness) {
    }

    public void clearAnimationFrames() {
    }

    public abstract void render(T entity, PoseStack matrixStack, VertexConsumer vertexBuilder, LayerCreatureBase layer, float time, float distance, float loop, float lookY, float lookX, float scale, int brightness, int fade);

    public void addCustomLayers(CreatureRenderer renderer) {
    }

    public boolean canRenderPart(String partName, Entity entity, LayerCreatureBase layer, boolean trophy) {
        if (layer == null)
            return this.canBaseRenderPart(partName, entity, trophy);
        if (entity instanceof BaseCreatureEntity)
            return layer.canRenderPart(partName, (BaseCreatureEntity) entity, trophy);
        return false;
    }

    public boolean canBaseRenderPart(String partName, Entity entity, boolean trophy) {
        return true;
    }

    public Vector4f getPartColor(String partName, Entity entity, LayerCreatureBase layer, boolean trophy, float loop) {
        if (layer == null || !(entity instanceof BaseCreatureEntity))
            return this.getBasePartColor(partName, entity, trophy, loop);
        return layer.getPartColor(partName, (BaseCreatureEntity) entity, trophy);
    }

    public Vector4f getBasePartColor(String partName, Entity entity, boolean trophy, float loop) {
        return LayerCreatureBase.WHITE;
    }

    public Vector2f getPartTextureOffset(String partName, Entity entity, LayerCreatureBase layer, boolean trophy, float loop) {
        if (layer == null || !(entity instanceof BaseCreatureEntity))
            return this.getBaseTextureOffset(partName, entity, trophy, loop);
        return layer.getTextureOffset(partName, (BaseCreatureEntity) entity, trophy, loop);
    }

    public Vector2f getBaseTextureOffset(String partName, Entity entity, boolean trophy, float loop) {
        return LayerCreatureBase.ZERO_TEXTURE_OFFSET;
    }

    public int getBrightness(String partName, LayerCreatureBase layer, BaseCreatureEntity entity, int brightness) {
        if (layer != null) {
            return layer.getBrightness(partName, entity, brightness);
        }
        return brightness;
    }

    public int getBlending(BaseCreatureEntity entity, LayerCreatureBase layer) {
        if (layer != null) {
            return layer.getBlending(entity);
        }
        return 0;
    }

    public boolean getGlow(BaseCreatureEntity entity, LayerCreatureBase layer) {
        if (layer != null) {
            return layer.getGlow(entity);
        }
        return false;
    }

    @Override
    public void rotate(float rotX, float rotY, float rotZ) {
    }

    @Override
    public void angle(float rotation, float angleX, float angleY, float angleZ) {
    }

    @Override
    public void translate(float posX, float posY, float posZ) {
    }

    @Override
    public void scale(float scaleX, float scaleY, float scaleZ) {
    }

    @Override
    public void doRotate(float rotX, float rotY, float rotZ) {
        if (rotX != 0.0F) {
            this.matrixStack.mulPose(Axis.XP.rotationDegrees(rotX));
        }
        if (rotY != 0.0F) {
            this.matrixStack.mulPose(Axis.YP.rotationDegrees(rotY));
        }
        if (rotZ != 0.0F) {
            this.matrixStack.mulPose(Axis.ZP.rotationDegrees(rotZ));
        }
    }

    @Override
    public void doAngle(float rotation, float angleX, float angleY, float angleZ) {
        if (rotation != 0.0F && (angleX != 0.0F || angleY != 0.0F || angleZ != 0.0F)) {
            this.matrixStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(rotation), angleX, angleY, angleZ)));
        }
    }

    @Override
    public void doTranslate(float posX, float posY, float posZ) {
        if (posX != 0.0F || posY != 0.0F || posZ != 0.0F) {
            this.matrixStack.translate(posX, posY, posZ);
        }
    }

    @Override
    public void doScale(float scaleX, float scaleY, float scaleZ) {
        if (scaleX != 1.0F || scaleY != 1.0F || scaleZ != 1.0F) {
            this.matrixStack.scale(scaleX, scaleY, scaleZ);
        }
    }

    @Override
    public double rotateToPoint(double aTarget, double bTarget) {
        return rotateToPoint(0, 0, aTarget, bTarget);
    }

    @Override
    public double rotateToPoint(double aCenter, double bCenter, double aTarget, double bTarget) {
        if (aTarget - aCenter == 0)
            if (aTarget > aCenter) return 0;
            else if (aTarget < aCenter) return 180;
        if (bTarget - bCenter == 0)
            if (bTarget > bCenter) return 90;
            else if (bTarget < bCenter) return -90;
        if (aTarget - aCenter == 0 && bTarget - bCenter == 0)
            return 0;
        return Math.toDegrees(Math.atan2(aCenter - aTarget, bCenter - bTarget) - Math.PI / 2);
    }

    @Override
    public double[] rotateToPoint(double xCenter, double yCenter, double zCenter, double xTarget, double yTarget, double zTarget) {
        double[] rotations = new double[3];
        rotations[0] = this.rotateToPoint(yCenter, -zCenter, yTarget, -zTarget);
        rotations[1] = this.rotateToPoint(-zCenter, xCenter, -zTarget, xTarget);
        rotations[2] = this.rotateToPoint(yCenter, xCenter, yTarget, xTarget);
        return rotations;
    }
}
