package com.lycanitesmobs.client.renderer.layer.creature;

import com.lycanitesmobs.client.manager.ClientManager;
import com.lycanitesmobs.client.renderer.entity.creature.CreatureRenderer;
import com.lycanitesmobs.client.renderer.util.CustomRenderStates;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2f;
import org.joml.Vector4f;

/**
 * Renders the model again with a suffixed texture (e.g. "_glow"), optionally glowing and blended.
 */
@OnlyIn(Dist.CLIENT)
public class LayerCreatureEffect extends LayerCreatureBase {
	public String textureSuffix;
	public boolean subspecies = true;
	public Vector2f scrollSpeed;
	private final Vector2f textureOffsetScratch = new Vector2f();

	public LayerCreatureEffect(CreatureRenderer renderer, String textureSuffix) {
		super(renderer);
		this.name = textureSuffix;
		this.textureSuffix = textureSuffix;
	}

	public LayerCreatureEffect(CreatureRenderer renderer, String textureSuffix, boolean glow, int blending, boolean subspecies) {
		super(renderer);
		this.name = textureSuffix;
		this.textureSuffix = textureSuffix;
		this.glow = glow;
		this.blending = blending;
		this.subspecies = subspecies;
	}

	public LayerCreatureEffect(CreatureRenderer renderer, String name, String textureSuffix, boolean glow, int blending, boolean subspecies) {
		super(renderer);
		this.name = name;
		this.textureSuffix = textureSuffix;
		this.glow = glow;
		this.blending = blending;
		this.subspecies = subspecies;
	}

	@Override
	public Vector4f getPartColor(String partName, BaseCreatureEntity entity, boolean trophy) {
		return CustomRenderStates.WHITE;
	}

	@Override
	public ResourceLocation getLayerTexture(BaseCreatureEntity entity) {
		if (this.subspecies) {
			return entity.getTexture(this.textureSuffix);
		}
		return entity.getSubTexture(this.textureSuffix);
	}

	@Override
	public Vector2f getTextureOffset(String partName, BaseCreatureEntity entity, boolean trophy, float loop) {
		if (this.scrollSpeed == null || (this.scrollSpeed.x == 0.0F && this.scrollSpeed.y == 0.0F)) {
			return CustomRenderStates.ZERO_TEXTURE_OFFSET;
		}
		return this.textureOffsetScratch.set(loop * this.scrollSpeed.x, loop * this.scrollSpeed.y);
	}

	@Override
	public int getBrightness(String partName, BaseCreatureEntity entity, int brightness) {
		if (this.glow) {
			return ClientManager.FULL_BRIGHT;
		}
		return brightness;
	}
}
