package com.lycanitesmobs.client.renderer.layer.creature;

import com.lycanitesmobs.client.renderer.entity.creature.CreatureRenderer;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector4f;

/**
 * Renders equipped armor (e.g. horse armor in the chest slot). 1.21.1: DyeableHorseArmorItem is gone, dye is the
 * DYED_COLOR data component on ItemTags.DYEABLE items - tinted the same way as vanilla's HorseArmorLayer.
 */
@OnlyIn(Dist.CLIENT)
public class LayerCreatureEquipment extends LayerCreatureBase {
    /** Vanilla's default leather horse armor color (HorseArmorLayer). */
    private static final int DEFAULT_LEATHER_COLOR = -6265536;

    public String equipmentSlot;

    public LayerCreatureEquipment(CreatureRenderer renderer, String equipmentSlot) {
        super(renderer);
        this.equipmentSlot = equipmentSlot;
    }

    @Override
    public boolean canRenderLayer(BaseCreatureEntity entity, float scale) {
        if (!super.canRenderLayer(entity, scale) || this.equipmentSlot == null)
            return false;
        return entity.getEquipmentName(this.equipmentSlot) != null;
    }

    @Override
    public ResourceLocation getLayerTexture(BaseCreatureEntity entity) {
        return entity.getEquipmentTexture(entity.getEquipmentName(this.equipmentSlot));
    }

    @Override
    public Vector4f getPartColor(String partName, BaseCreatureEntity entity, boolean trophy) {
        ItemStack equipmentStack = entity.getCreatureInventory().getEquipmentStack("chest");
        if (equipmentStack.is(ItemTags.DYEABLE)) {
            int color = DyedItemColor.getOrDefault(equipmentStack, DEFAULT_LEATHER_COLOR);
            float r = (float) (color >> 16 & 255) / 255.0F;
            float g = (float) (color >> 8 & 255) / 255.0F;
            float b = (float) (color & 255) / 255.0F;
            return new Vector4f(r, g, b, 1);
        }
        return new Vector4f(1, 1, 1, 1);
    }
}
