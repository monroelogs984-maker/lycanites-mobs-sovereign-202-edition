package com.lycanitesmobs.core.entity.effect;

import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.util.helpers.AssetHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class EffectBase extends MobEffect {
	private final String name;
	private ResourceLocation registryName;

	// ==================================================
	//                    Constructor
	// ==================================================
	public EffectBase(String name, boolean badEffect, int color) {
		super(badEffect ? MobEffectCategory.HARMFUL : MobEffectCategory.BENEFICIAL, color);
		this.name = name;
		this.setRegistryName(LycanitesMobs.MODID, name);
	}

	public ResourceLocation getRegistryName() {
		return registryName;
	}
	public ResourceLocation setRegistryName(String modID, String blockName) {
		return registryName = ResourceLocation.fromNamespaceAndPath(modID, blockName);
	}


	// ==================================================
	//                    Effects
	// ==================================================
	@Override
	public boolean isInstantenous() {
        return false;
    }

	public ResourceLocation getTexture() {
		return AssetHelper.texture("textures/mob_effect/" + this.name + ".png");
	}
}
