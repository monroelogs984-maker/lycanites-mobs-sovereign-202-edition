package com.lycanitesmobs.client.renderer.util;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.function.Function;

/**
 * Render types for OBJ creature models and their effect layers.
 *
 * <p>Port note: the official class builds its own POS_TEX_NORMAL vertex format + shader for its VBO batcher (and a
 * pile of Iris-specific variants). The port renders OBJ parts immediately through vanilla's entity vertex format
 * instead, so these are vanilla entity-shader render types that reproduce the official state per blend mode:
 * NORMAL (no glow) = cutout, NORMAL (glow/layer) = alpha-translucent, ADD = additive (SRC_ALPHA, ONE), SUB =
 * subtractive (DST_COLOR, ONE_MINUS_SRC_ALPHA). All are double-sided and write depth, like the official OBJ types.
 * Glow itself is full-bright light, applied by the layers (getBrightness), not by the render type.
 */
@OnlyIn(Dist.CLIENT)
public class CustomRenderStates extends RenderType {
    public static final Vector4f WHITE = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
    public static final Vector2f ZERO_TEXTURE_OFFSET = new Vector2f(0.0F, 0.0F);

    public enum BLEND {
        NORMAL(0), ADD(1), SUB(2);
        public final int id;

        BLEND(int value) {
            this.id = value;
        }

        public int getValue() {
            return this.id;
        }
    }

    protected static final TransparencyStateShard ADDITIVE_TRANSPARENCY = new TransparencyStateShard("lm_additive_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });

    protected static final TransparencyStateShard SUBTRACTIVE_TRANSPARENCY = new TransparencyStateShard("lm_subtractive_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });

    private record LayerKey(ResourceLocation texture, int blending) {
    }

    private static final Function<LayerKey, RenderType> LAYER_TYPES = Util.memoize(key -> {
        TransparencyStateShard transparency = key.blending() == BLEND.ADD.id ? ADDITIVE_TRANSPARENCY
                : key.blending() == BLEND.SUB.id ? SUBTRACTIVE_TRANSPARENCY
                : TRANSLUCENT_TRANSPARENCY;
        CompositeState state = CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                .setTextureState(new TextureStateShard(key.texture(), false, false))
                .setTransparencyState(transparency)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .createCompositeState(true);
        return create("lycanitesmobs_obj_" + BLEND.values()[Math.max(0, Math.min(2, key.blending()))].name().toLowerCase(),
                DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true, state);
    });

    private CustomRenderStates(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    /**
     * Returns true if the blending/glow combination is the plain cutout render type (the official OBJ_CUTOUT).
     */
    public static boolean isObjCutout(int blending, boolean glow) {
        return blending == BLEND.NORMAL.id && !glow;
    }

    /**
     * Gets the render type for a model render pass.
     *
     * @param texture  The texture to render with.
     * @param blending The blend mode id (BLEND).
     * @param glow     True for glowing passes (rendered translucent, like the official OBJ glow types).
     * @return The render type to draw with.
     */
    public static RenderType getObjRenderType(ResourceLocation texture, int blending, boolean glow) {
        if (isObjCutout(blending, glow)) {
            return RenderType.entityCutoutNoCull(texture);
        }
        return LAYER_TYPES.apply(new LayerKey(texture, blending));
    }
}
