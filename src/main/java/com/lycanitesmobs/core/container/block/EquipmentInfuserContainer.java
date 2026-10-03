package com.lycanitesmobs.core.container.block;

import com.lycanitesmobs.core.container.creature.CreatureContainer;
import com.lycanitesmobs.core.item.consumable.entity.ChargeItem;
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
 * Equipment Infuser (S202 rework): a part, or a weapon holding an imprint, plus charges. As officially, charges that
 * share an element with the part are consumed right away for experience until the part reaches its max level.
 * The official dye/water recolouring is gone, since imprints never change how the weapon looks.
 */
public class EquipmentInfuserContainer extends EquipmentWorkstationContainer {
    public static final DeferredHolder<MenuType<?>, MenuType<EquipmentInfuserContainer>> TYPE = CreatureContainer.MENUS.register("equipment_infuser", () -> IMenuTypeExtension.create(EquipmentInfuserContainer::new));

    public static void init() {
    }

    /** Client Constructor **/
    public EquipmentInfuserContainer(int windowId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(windowId, playerInventory, ContainerLevelAccess.NULL);
    }

    /** Main Constructor **/
    public EquipmentInfuserContainer(int windowId, Inventory playerInventory, ContainerLevelAccess access) {
        super(TYPE.get(), windowId, playerInventory, access,
                stack -> stack.getItem() instanceof ItemEquipmentPart || Imprints.hasImprint(stack), 1,
                stack -> stack.getItem() instanceof ChargeItem, 64);
    }

    @Override
    protected Block getBlock() {
        return ObjectManager.getBlock("equipment_infuser");
    }

    @Override
    protected void onWorkSlotsChanged() {
        ItemStack target = this.getFirstStack();
        ItemStack charges = this.getSecondStack();
        ItemStack part = Imprints.getPartOf(target).copy();
        if (!(part.getItem() instanceof ItemEquipmentPart partItem) || !partItem.isLevelingChargeItem(charges)) {
            return;
        }
        int startLevel = partItem.getPartLevel(part);
        boolean infused = false;
        while (!charges.isEmpty() && !partItem.isAtMaxLevel(part)) {
            partItem.addExperience(part, partItem.getExperienceFromChargeItem(charges));
            charges.shrink(1);
            infused = true;
        }
        if (!infused) {
            return;
        }
        this.workSlots.setItem(FIRST_SLOT, Imprints.setPartOf(target.copy(), part));
        this.workSlots.setItem(SECOND_SLOT, charges.isEmpty() ? ItemStack.EMPTY : charges);
        boolean levelUp = partItem.getPartLevel(part) > startLevel;
        this.access.execute((level, pos) -> level.playSound(null, pos,
                levelUp ? SoundEvents.PLAYER_LEVELUP : SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F));
    }
}
