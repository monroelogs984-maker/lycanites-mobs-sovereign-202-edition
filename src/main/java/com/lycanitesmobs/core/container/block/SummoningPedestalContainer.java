package com.lycanitesmobs.core.container.block;

import com.lycanitesmobs.core.block.blockentity.TileEntitySummoningPedestal;
import com.lycanitesmobs.core.container.base.BaseContainer;
import com.lycanitesmobs.core.container.base.BaseSlot;
import com.lycanitesmobs.core.container.creature.CreatureContainer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;

public class SummoningPedestalContainer extends BaseContainer {
	/** Registered on CreatureContainer.MENUS; LycanitesMobs calls init() before that register is attached. **/
	public static final DeferredHolder<MenuType<?>, MenuType<SummoningPedestalContainer>> TYPE = CreatureContainer.MENUS.register("summoning_pedestal", () -> IMenuTypeExtension.create(SummoningPedestalContainer::new));

	public static void init() {
	}

	protected final TileEntitySummoningPedestal summoningPedestal;

	/** Client Constructor **/
	public SummoningPedestalContainer(int windowId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
		this(windowId, playerInventory, (TileEntitySummoningPedestal)playerInventory.player.getCommandSenderWorld().getBlockEntity(extraData.readBlockPos()));
	}

	/** Main Constructor **/
	public SummoningPedestalContainer(int windowId, Inventory playerInventory, TileEntitySummoningPedestal summoningPedestal) {
		super(TYPE.get(), windowId);
		this.summoningPedestal = summoningPedestal;
		this.inventoryStart = this.slots.size();

		// Player Inventory (hotbar only, as the official):
		this.playerInventoryStart = this.slots.size();
		this.addSlotGrid(playerInventory, 8, 171, 1, 0, 8);

		// Pedestal Inventory (the fuel slot):
		int slots = 0;
		if (summoningPedestal.getContainerSize() > 0) {
			this.addSlot(new BaseSlot(summoningPedestal, slots++, 93, 43));
		}
		this.inventoryFinish = this.inventoryStart + slots;
	}

	public TileEntitySummoningPedestal getSummoningPedestal() {
		return this.summoningPedestal;
	}

	@Override
	public boolean stillValid(Player player) {
		if (this.summoningPedestal == null || !this.summoningPedestal.stillValid(player))
			return false;
		return player.getUUID().equals(this.summoningPedestal.getOwnerUUID());
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotID) {
		return ItemStack.EMPTY;
	}
}
