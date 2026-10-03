package com.lycanitesmobs.core.container.block;

import com.lycanitesmobs.core.container.creature.CreatureContainer;
import com.lycanitesmobs.core.item.equipment.ItemEquipmentPart;
import com.lycanitesmobs.core.item.equipment.imprint.Imprints;
import com.lycanitesmobs.core.manager.ObjectManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Equipment Forge (S202 rework): weapon slot + part slot and one button. With a part in, the button imprints it onto
 * the weapon; with an imprinted weapon and an empty part slot, it extracts the part. The forge's level caps the part
 * level it can work with either way.
 */
public class EquipmentForgeContainer extends EquipmentWorkstationContainer {
    public static final DeferredHolder<MenuType<?>, MenuType<EquipmentForgeContainer>> TYPE = CreatureContainer.MENUS.register("equipment_forge", () -> IMenuTypeExtension.create(EquipmentForgeContainer::new));

    public static void init() {
    }

    public static final int BUTTON_ACTION = 0;

    /** What the forge's button would do right now, or why it can't. Shared by the server action and the screen. **/
    public enum Action {
        EMPTY(false), INSERT_PART(false), INSERT_WEAPON(false), NOT_A_PART(false), LEVEL_TOO_HIGH(false),
        ALREADY_IMPRINTED(false), IMPRINT(true), EXTRACT(true), EXTRACT_LEVEL_TOO_HIGH(false);

        public final boolean possible;

        Action(boolean possible) {
            this.possible = possible;
        }
    }

    protected final int forgeLevel;

    /** Client Constructor **/
    public EquipmentForgeContainer(int windowId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(windowId, playerInventory, ContainerLevelAccess.NULL, extraData.readVarInt());
    }

    /** Main Constructor **/
    public EquipmentForgeContainer(int windowId, Inventory playerInventory, ContainerLevelAccess access, int forgeLevel) {
        super(TYPE.get(), windowId, playerInventory, access,
                stack -> Imprints.isEligible(stack) || Imprints.hasImprint(stack), 1,
                stack -> stack.getItem() instanceof ItemEquipmentPart, 1);
        this.forgeLevel = forgeLevel;
    }

    public int getForgeLevel() {
        return this.forgeLevel;
    }

    /** The part the panel describes: the one in the part slot, else the weapon's imprint. **/
    public ItemStack getShownPart() {
        if (!this.getSecondStack().isEmpty()) {
            return this.getSecondStack();
        }
        return Imprints.getPart(this.getFirstStack());
    }

    public Action getAction() {
        ItemStack weapon = this.getFirstStack();
        ItemStack part = this.getSecondStack();
        if (!part.isEmpty()) {
            if (!Imprints.isImprintablePart(part)) {
                return Action.NOT_A_PART;
            }
            if (((ItemEquipmentPart) part.getItem()).getPartLevel(part) > this.forgeLevel) {
                return Action.LEVEL_TOO_HIGH;
            }
            if (weapon.isEmpty()) {
                return Action.INSERT_WEAPON;
            }
            return Imprints.hasImprint(weapon) ? Action.ALREADY_IMPRINTED : Action.IMPRINT;
        }
        if (weapon.isEmpty()) {
            return Action.EMPTY;
        }
        if (!Imprints.hasImprint(weapon)) {
            return Action.INSERT_PART;
        }
        ItemStack imprint = Imprints.getPart(weapon);
        if (imprint.getItem() instanceof ItemEquipmentPart partItem && partItem.getPartLevel(imprint) > this.forgeLevel) {
            return Action.EXTRACT_LEVEL_TOO_HIGH;
        }
        return Action.EXTRACT;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_ACTION) {
            return false;
        }
        Action action = this.getAction();
        if (action == Action.IMPRINT) {
            ItemStack weapon = this.getFirstStack().copy();
            if (!Imprints.imprint(weapon, this.getSecondStack())) {
                return false;
            }
            this.workSlots.setItem(SECOND_SLOT, ItemStack.EMPTY);
            this.workSlots.setItem(FIRST_SLOT, weapon);
        }
        else if (action == Action.EXTRACT) {
            ItemStack weapon = this.getFirstStack().copy();
            ItemStack part = Imprints.extract(weapon);
            this.workSlots.setItem(FIRST_SLOT, weapon);
            this.workSlots.setItem(SECOND_SLOT, part);
        }
        else {
            return false;
        }
        this.access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F));
        return true;
    }

    @Override
    protected Block getBlock() {
        return ObjectManager.getBlock(switch (this.forgeLevel) {
            case 1 -> "equipmentforge_lesser";
            case 2 -> "equipmentforge_greater";
            default -> "equipmentforge_master";
        });
    }

    @Override
    protected void onWorkSlotsChanged() {
    }
}
