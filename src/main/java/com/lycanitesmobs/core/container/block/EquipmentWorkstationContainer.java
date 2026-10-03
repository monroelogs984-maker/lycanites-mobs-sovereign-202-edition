package com.lycanitesmobs.core.container.block;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.function.Predicate;

/**
 * Shared base of the S202 equipment workstations (Forge, Infuser, Station): two work slots in a temporary container
 * plus the player inventory. Like vanilla workstations there is no block entity, the work slots are returned to the
 * player when the menu closes. Subclasses react to slot changes server side in {@link #onWorkSlotsChanged()}.
 */
public abstract class EquipmentWorkstationContainer extends AbstractContainerMenu {
    /** Shared screen layout, the screens draw against these positions. **/
    public static final int WIDTH = 230;
    public static final int HEIGHT = 190;
    public static final int FIRST_SLOT_X = 14;
    public static final int SECOND_SLOT_X = 40;
    public static final int WORK_SLOT_Y = 24;
    public static final int INVENTORY_X = 35;
    public static final int INVENTORY_Y = 108;
    public static final int HOTBAR_Y = 166;

    public static final int FIRST_SLOT = 0;
    public static final int SECOND_SLOT = 1;
    protected static final int PLAYER_SLOTS_START = 2;
    protected static final int PLAYER_SLOTS_END = PLAYER_SLOTS_START + 36;

    protected final ContainerLevelAccess access;
    protected final Player player;
    protected final SimpleContainer workSlots;
    private boolean updating = false;

    protected EquipmentWorkstationContainer(MenuType<?> type, int windowId, Inventory playerInventory, ContainerLevelAccess access,
                                            Predicate<ItemStack> firstSlotValid, int firstSlotMax, Predicate<ItemStack> secondSlotValid, int secondSlotMax) {
        super(type, windowId);
        this.access = access;
        this.player = playerInventory.player;
        this.workSlots = new SimpleContainer(2) {
            @Override
            public void setChanged() {
                super.setChanged();
                EquipmentWorkstationContainer.this.slotsChanged(this);
            }
        };

        this.addSlot(new WorkSlot(this.workSlots, FIRST_SLOT, FIRST_SLOT_X, WORK_SLOT_Y, firstSlotValid, firstSlotMax));
        this.addSlot(new WorkSlot(this.workSlots, SECOND_SLOT, SECOND_SLOT_X, WORK_SLOT_Y, secondSlotValid, secondSlotMax));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, INVENTORY_X + column * 18, HOTBAR_Y));
        }
    }

    /** The block this menu belongs to, for stillValid. **/
    protected abstract Block getBlock();

    /** Server side, after either work slot changed. Changes made here don't re-trigger it. **/
    protected abstract void onWorkSlotsChanged();

    public ItemStack getFirstStack() {
        return this.workSlots.getItem(FIRST_SLOT);
    }

    public ItemStack getSecondStack() {
        return this.workSlots.getItem(SECOND_SLOT);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container != this.workSlots || this.updating || this.player.level().isClientSide()) {
            return;
        }
        this.updating = true;
        try {
            this.onWorkSlotsChanged();
        } finally {
            this.updating = false;
        }
        this.broadcastChanges();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.workSlots));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, this.getBlock());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < PLAYER_SLOTS_START) {
            if (!this.moveItemStackTo(stack, PLAYER_SLOTS_START, PLAYER_SLOTS_END, true)) {
                return ItemStack.EMPTY;
            }
        }
        else {
            // Into whichever work slot accepts it (moveItemStackTo respects mayPlace and the slot's max stack size).
            boolean moved = false;
            for (int workSlot = FIRST_SLOT; workSlot <= SECOND_SLOT && !stack.isEmpty(); workSlot++) {
                if (this.slots.get(workSlot).mayPlace(stack)) {
                    moved = this.moveItemStackTo(stack, workSlot, workSlot + 1, false) || moved;
                }
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    protected static class WorkSlot extends Slot {
        private final Predicate<ItemStack> valid;
        private final int max;

        public WorkSlot(Container container, int index, int x, int y, Predicate<ItemStack> valid, int max) {
            super(container, index, x, y);
            this.valid = valid;
            this.max = max;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.valid.test(stack);
        }

        @Override
        public int getMaxStackSize() {
            return this.max;
        }
    }
}
