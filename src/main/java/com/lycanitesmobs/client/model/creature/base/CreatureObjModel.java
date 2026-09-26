package com.lycanitesmobs.client.model.creature.base;

import com.google.gson.*;
import com.lycanitesmobs.client.model.animation.AnimationPart;
import com.lycanitesmobs.client.model.animation.Animator;
import com.lycanitesmobs.client.obj.model.ObjModel;
import com.lycanitesmobs.client.obj.geometry.ObjPart;
import com.lycanitesmobs.client.gui.screen.creature.RecolorDebug;
import com.lycanitesmobs.client.renderer.layer.creature.LayerCreatureBase;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.data.info.ModInfo;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Trimmed - uses plain ObjModel instead of VBOObjModel (no VBO batching/Iris integration, see
 * PORT_PLAN.md), and doesn't attempt to load a "_animation.json" (ModelAnimation/
 * TextureLayerAnimation/ModelPartAnimation aren't ported - none of the creatures ported so far
 * actually ship one of these files, only the mandatory "_parts.json").
 */
@OnlyIn(Dist.CLIENT)
public class CreatureObjModel extends CreatureModel {
    public static float MODEL_OFFSET_ROT_X = 180F;
    public static float MODEL_OFFSET_POS_Y = -1.5F;

    public ObjModel objModel;
    public List<ObjPart> objParts;
    public Map<String, AnimationPart> animationParts = new HashMap<>();

    public float lookHeadScaleX = 1;
    public float lookHeadScaleY = 1;
    public float lookNeckScaleX = 0;
    public float lookNeckScaleY = 0;
    public float lookBodyScaleX = 0;
    public float lookBodyScaleY = 0;
    public boolean bigChildHead = false;

    public boolean bodyIsTrophy = true;
    public float trophyScale = 1;
    public float[] trophyOffset = new float[0];
    public float[] trophyMouthOffset = new float[0];

    public boolean dontColor = false;

    public Animator animator;
    protected AnimationPart currentAnimationPart;
    protected Map<Entity, ModelObjState> modelStates = new HashMap<>();
    protected ModelObjState currentModelState;
    protected ModInfo modelModInfo;
    protected String modelName;
    protected String modelPath;

    public CreatureObjModel() {
        this(1.0F);
    }

    public CreatureObjModel(float shadowSize) {
    }

    public void reloadModel(ResourceManager resourceManager) {
        if (this.objModel != null) {
            this.objModel.dispose();
            this.objModel = null;
        }

        this.objParts = null;
        this.animationParts.clear();
        this.modelStates.clear();
        this.currentModelState = null;
        this.currentAnimationPart = null;

        if (this.modelName != null && this.modelModInfo != null && this.modelPath != null) {
            this.initModel(this.modelName, this.modelModInfo, this.modelPath, resourceManager);
        }
    }

    public CreatureObjModel initModel(String name, ModInfo modInfo, String path) {
        return initModel(name, modInfo, path, Minecraft.getInstance().getResourceManager());
    }

    public CreatureObjModel initModel(String name, ModInfo modInfo, String path, ResourceManager resourceManager) {
        this.modelName = name;
        this.modelModInfo = modInfo;
        this.modelPath = path;

        this.objModel = new ObjModel(
                ResourceLocation.fromNamespaceAndPath(modInfo.modid, "modelparts/" + path + ".obj"),
                resourceManager
        );
        this.objParts = this.objModel.objParts;
        if (this.objParts.isEmpty())
            LMHelperClass.logWarningMessage("Unable to load model obj for: " + name + "");
        else
            LMHelperClass.logDebug("Resources", "Loaded model obj for: " + name + " (" + this.objParts.size() + " parts)");

        this.animator = new Animator(this);

        ResourceLocation modelPartsLocation = ResourceLocation.fromNamespaceAndPath(modInfo.modid, "modelparts/" + path + "_parts.json");
        try {
            Gson gson = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
            InputStream in = resourceManager.getResource(modelPartsLocation).get().open();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            try {
                JsonArray jsonArray = GsonHelper.fromJson(gson, reader, JsonArray.class, false);
                Iterator<JsonElement> jsonIterator = jsonArray.iterator();
                while (jsonIterator.hasNext()) {
                    JsonObject partJson = jsonIterator.next().getAsJsonObject();
                    AnimationPart animationPart = new AnimationPart();
                    animationPart.loadFromJson(partJson);
                    this.addAnimationPart(animationPart);
                }
            } finally {
                IOUtils.closeQuietly(reader);
            }
        } catch (Exception e) {
            LMHelperClass.logWarningMessage("Unable to load model parts json for: " + name + "");
        }

        for (AnimationPart part : this.animationParts.values()) {
            part.addChildren(this.animationParts.values().toArray(new AnimationPart[this.animationParts.size()]));
        }
        this.applyAnimationPartRenderMetadata();

        return this;
    }

    public void addAnimationPart(AnimationPart animationPart) {
        if (this.animationParts.containsKey(animationPart.name)) {
            return;
        }
        if (animationPart.parentName != null) {
            if (animationPart.parentName.equals(animationPart.name))
                animationPart.parentName = null;
        }
        this.animationParts.put(animationPart.name, animationPart);
    }

    private void applyAnimationPartRenderMetadata() {
        if (this.objParts == null) {
            return;
        }
        for (ObjPart objPart : this.objParts) {
            AnimationPart animationPart = this.animationParts.get(objPart.getLowerName());
            objPart.setCullBackfaces(animationPart != null && animationPart.cullBackfaces);
        }
    }

    @Override
    public void generateAnimationFrames(BaseCreatureEntity entity, float time, float distance, float loop, float lookY, float lookX, float scale, int brightness) {
        boolean renderAsTrophy = false;
        this.young = false;
        if (entity != null) {
            this.young = entity.isBaby();
        }
        if (scale < 0) {
            renderAsTrophy = true;
            scale = -scale;
        }
        // NOTE: unlike the original, entity.getScale() is NOT re-applied here - vanilla's
        // LivingEntityRenderer.render() already scales the whole pose stack by entity.getScale()
        // before calling setupAnim()/renderToBuffer() (see CreatureModel's class doc).

        if (entity != null) {
            if (entity.getOnlyRenderTicks() >= 0) {
                loop = entity.getOnlyRenderTicks();
            }
        }

        this.currentModelState = this.getModelState(entity);

        if (this.currentModelState != null) {
            RecolorDebug.applyToState(this.currentModelState, entity);
        }

        if (entity != null && entity.hasPerchTarget()) {
            distance = 0;
        }

        for (ObjPart part : this.objParts) {
            String partName = part.getLowerName();
            this.currentAnimationPart = this.animationParts.get(partName);
            if (this.currentAnimationPart == null)
                continue;

            this.animatePart(partName, entity, time, distance, loop, -lookY, lookX, scale);

            if (renderAsTrophy) {
                if (partName.contains("head")) {
                    if (!partName.contains("left")) {
                        this.translate(-0.3F, 0, 0);
                        this.angle(5F, 0, 1, 0);
                    }
                    if (!partName.contains("right")) {
                        this.translate(0.3F, 0, 0);
                        this.angle(-5F, 0, 1, 0);
                    }
                }
                if (this.trophyOffset.length >= 3)
                    this.translate(this.trophyOffset[0], this.trophyOffset[1], this.trophyOffset[2]);
            }
        }
    }

    public void animatePart(String partName, LivingEntity entity, float time, float distance, float loop, float lookY, float lookX, float scale) {
        float rotX = 0F;
        float rotY = 0F;
        float rotZ = 0F;

        if (partName.equals("head")) {
            rotX += (Math.toDegrees(lookX / (180F / (float) Math.PI)) * this.lookHeadScaleX);
            rotY += (Math.toDegrees(lookY / (180F / (float) Math.PI))) * this.lookHeadScaleY;
        }
        if (partName.contains("neck")) {
            rotX += (Math.toDegrees(lookX / (180F / (float) Math.PI)) * this.lookNeckScaleX);
            rotY += (Math.toDegrees(lookY / (180F / (float) Math.PI))) * this.lookNeckScaleY;
        }

        this.rotate(rotX, rotY, rotZ);
    }

    @Override
    public void clearAnimationFrames() {
        for (AnimationPart animationPart : this.animationParts.values()) {
            animationPart.animationFrames.clear();
        }
    }

    @Override
    public void render(BaseCreatureEntity entity, PoseStack matrixStack, VertexConsumer vertexBuilder, LayerCreatureBase layer, float time, float distance, float loop, float lookY, float lookX, float scale, int brightness, int fade) {
        this.matrixStack = matrixStack;

        boolean renderAsTrophy = false;
        this.young = false;
        if (entity != null) {
            this.young = entity.isBaby();
        }
        if (scale < 0) {
            renderAsTrophy = true;
            scale = -scale;
        }
        // NOTE: entity.getScale() is intentionally not re-applied here - see the note in
        // generateAnimationFrames() above and CreatureModel's class doc.

        for (ObjPart part : this.objParts) {
            String partName = part.getLowerName();
            if (!this.canRenderPart(partName, entity, layer, renderAsTrophy))
                continue;

            this.currentAnimationPart = this.animationParts.get(partName);
            if (this.currentAnimationPart == null) {
                continue;
            }

            matrixStack.pushPose();
            this.doAngle(MODEL_OFFSET_ROT_X, 1F, 0F, 0F);
            this.doTranslate(0F, MODEL_OFFSET_POS_Y, 0F);

            if (this.young && !renderAsTrophy) {
                this.childScale(partName);
                if (this.bigChildHead && (partName.contains("head") || partName.contains("mouth")))
                    this.doTranslate(-(this.currentAnimationPart.centerX / 2), -(this.currentAnimationPart.centerY / 2), -(this.currentAnimationPart.centerZ / 2));
            }

            if (renderAsTrophy)
                this.doScale(this.trophyScale, this.trophyScale, this.trophyScale);

            this.doScale(scale, scale, scale);

            this.currentAnimationPart.applyAnimationFrames(this.animator);

            if (this.objModel.getCreature() == null) {
                this.objModel.setCreature(entity);
            }

            this.objModel.renderPart(
                    vertexBuilder,
                    matrixStack.last().normal(),
                    matrixStack.last().pose(),
                    this.getBrightness(partName, layer, entity, brightness),
                    fade,
                    part,
                    this.getPartColor(partName, entity, layer, renderAsTrophy, loop),
                    this.getPartTextureOffset(partName, entity, layer, renderAsTrophy, loop)
            );
            matrixStack.popPose();
        }
    }

    @Override
    public boolean canRenderPart(String partName, Entity entity, LayerCreatureBase layer, boolean trophy) {
        if (partName == null)
            return false;
        partName = partName.toLowerCase();

        if (!this.animationParts.containsKey(partName))
            return false;

        if (trophy && !this.isTrophyPart(partName))
            return false;

        return super.canRenderPart(partName, entity, layer, trophy);
    }

    public boolean isTrophyPart(String partName) {
        if (partName == null)
            return false;
        partName = partName.toLowerCase();
        if (partName.contains("head") || partName.contains("mouth") || partName.contains("eye"))
            return true;
        if (this.bodyIsTrophy && partName.contains("body"))
            return true;
        return false;
    }

    public ModelObjState getModelState(Entity entity) {
        if (entity == null)
            return null;
        if (this.modelStates.containsKey(entity)) {
            if (!entity.isAlive()) {
                this.modelStates.remove(entity);
                return null;
            }
            return this.modelStates.get(entity);
        }
        ModelObjState modelState = new ModelObjState(entity);
        this.modelStates.put(entity, modelState);
        return modelState;
    }

    public void removeModelState(Entity entity) {
        if (entity == null) {
            return;
        }
        this.modelStates.remove(entity);
        if (this.currentModelState != null && this.currentModelState.entity == entity) {
            this.currentModelState = null;
        }
    }

    public void childScale(String partName) {
        if (this.bigChildHead && (partName.contains("head") || partName.contains("mouth")))
            return;
        this.animator.doScale(0.5F, 0.5F, 0.5F);
    }

    public void updateAttackProgress(Entity entity) {
        if (this.currentModelState == null || !(entity instanceof BaseCreatureEntity))
            return;
        BaseCreatureEntity entityCreature = (BaseCreatureEntity) entity;

        if (this.currentModelState.attackAnimationPlaying) {
            if (this.currentModelState.attackAnimationIncreasing) {
                this.currentModelState.attackAnimationProgress = Math.min(this.currentModelState.attackAnimationProgress + this.currentModelState.attackAnimationSpeed, 1F);
                if (this.currentModelState.attackAnimationProgress >= 1)
                    this.currentModelState.attackAnimationIncreasing = false;
            } else {
                this.currentModelState.attackAnimationProgress = Math.max(this.currentModelState.attackAnimationProgress - this.currentModelState.attackAnimationSpeed, 0F);
                if (this.currentModelState.attackAnimationProgress <= 0) {
                    this.currentModelState.attackAnimationPlaying = false;
                }
            }
        } else if (entityCreature.isAttackOnCooldown()) {
            this.currentModelState.attackAnimationPlaying = true;
            this.currentModelState.attackAnimationIncreasing = true;
            this.currentModelState.attackAnimationProgress = 0;
        }
    }

    public float getAttackProgress() {
        if (this.currentModelState == null)
            return 0;
        return this.currentModelState.attackAnimationProgress;
    }

    @Override
    public void angle(float rotation, float angleX, float angleY, float angleZ) {
        this.currentAnimationPart.addAnimationFrame(new com.lycanitesmobs.client.model.animation.ModelObjAnimationFrame("angle", rotation, angleX, angleY, angleZ));
    }

    @Override
    public void rotate(float rotX, float rotY, float rotZ) {
        this.currentAnimationPart.addAnimationFrame(new com.lycanitesmobs.client.model.animation.ModelObjAnimationFrame("rotate", 1, rotX, rotY, rotZ));
    }

    @Override
    public void translate(float posX, float posY, float posZ) {
        this.currentAnimationPart.addAnimationFrame(new com.lycanitesmobs.client.model.animation.ModelObjAnimationFrame("translate", 1, posX, posY, posZ));
    }

    @Override
    public void scale(float scaleX, float scaleY, float scaleZ) {
        this.currentAnimationPart.addAnimationFrame(new com.lycanitesmobs.client.model.animation.ModelObjAnimationFrame("scale", 1, scaleX, scaleY, scaleZ));
    }

    public void shiftOrigin(String fromPartName, String toPartName) {
        AnimationPart fromPart = this.animationParts.get(fromPartName);
        AnimationPart toPart = this.animationParts.get(toPartName);
        float offsetX = toPart.centerX - fromPart.centerX;
        float offsetY = toPart.centerY - fromPart.centerY;
        float offsetZ = toPart.centerZ - fromPart.centerZ;
        this.translate(offsetX, offsetY, offsetZ);
    }

    public void shiftOriginBack(String fromPartName, String toPartName) {
        AnimationPart fromPart = this.animationParts.get(fromPartName);
        AnimationPart toPart = this.animationParts.get(toPartName);
        float offsetX = toPart.centerX - fromPart.centerX;
        float offsetY = toPart.centerY - fromPart.centerY;
        float offsetZ = toPart.centerZ - fromPart.centerZ;
        this.translate(-offsetX, -offsetY, -offsetZ);
    }
}
