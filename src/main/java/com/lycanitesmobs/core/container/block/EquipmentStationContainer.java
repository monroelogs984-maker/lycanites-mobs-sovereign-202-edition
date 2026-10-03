package com.lycanitesmobs.core.container.block;

import com.lycanitesmobs.core.container.creature.CreatureContainer;
import com.lycanitesmobs.core.item.equipment.ItemEquipment;
import com.lycanitesmobs.core.item.equipment.ItemEquipmentPart;
import com.lycanitesmobs.core.item.equipment.imprint.Imprints;
import com.lycanitesmobs.core.manager.ObjectManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Equipment Station (S202 rework): a part, or a weapon holding an imprint, plus mana items. As officially, mana items
 * are consumed right away until the mana is full. Sharpness is gone (the host tool's durability replaces it).
 */
public class EquipmentStationContainer extends EquipmentWorkstationContainer {
    public static final DeferredHolder<MenuType<?>, MenuType<EquipmentStationContainer>> TYPE = CreatureContainer.MENUS.register("equipment_station", () -> IMenuTypeExtension.create(EquipmentStationContainer::new));

    public static void init() {
    }

    /** Client Constructor **/
    public EquipmentStationContainer(int windowId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(windowId, playerInventory, ContainerLevelAccess.NULL);
    }

    /** Main Constructor **/
    public EquipmentStationContainer(int windowId, Inventory playerInventory, ContainerLevelAccess access) {
        super(TYPE.get(), windowId, playerInventory, access,
                stack -> stack.getItem() instanceof ItemEquipmentPart || Imprints.hasImprint(stack), 1,
                stack -> Imprints.getManaRecharge(stack) > 0, 64);
    }

    @Override
    protected Block getBlock() {
        return ObjectManager.getBlock("equipment_station");
    }

    @Override
    protected void onWorkSlotsChanged() {
        ItemStack target = this.getFirstStack();
        ItemStack fuel = this.getSecondStack();
        ItemStack part = Imprints.getPartOf(target).copy();
        int recharge = Imprints.getManaRecharge(fuel);
        if (!(part.getItem() instanceof ItemEquipmentPart partItem) || recharge <= 0) {
            return;
        }
        boolean recharged = false;
        while (!fuel.isEmpty() && partItem.getMana(part) < ItemEquipment.MANA_MAX) {
            partItem.addMana(part, recharge);
            fuel.shrink(1);
            recharged = true;
        }
        if (!recharged) {
            return;
        }
        this.workSlots.setItem(FIRST_SLOT, Imprints.setPartOf(target.copy(), part));
        this.workSlots.setItem(SECOND_SLOT, fuel.isEmpty() ? ItemStack.EMPTY : fuel);
        this.access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.0F));
    }
}
