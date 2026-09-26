package com.lycanitesmobs.core.tabs;

import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.item.consumable.entity.ChargeItem;
import com.lycanitesmobs.core.manager.ObjectManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Creative tab with every projectile charge item.
 */
public class LMChargesGroup {
    public CreativeModeTab.Builder builder = CreativeModeTab.builder();

    public LMChargesGroup() {
        this.builder = this.builder.title(Component.translatable("itemGroup." + LycanitesMobs.MODID + ".charges"))
                .icon(this::getIconItem)
                .displayItems((enabledFeatures, entries) -> {
                    List<String> names = new ArrayList<>(LMItemsGroup.itemNames);
                    Collections.sort(names);
                    for (String name : names) {
                        Item item = ObjectManager.getItem(name);
                        if (item instanceof ChargeItem) {
                            entries.accept(item);
                        }
                    }
                });
    }

    public static CreativeModeTab.Builder getBuilder() {
        return new LMChargesGroup().builder;
    }

    public ItemStack getIconItem() {
        Item item = ObjectManager.getItem("hellfireballcharge");
        return item != null ? new ItemStack(item) : new ItemStack(Items.FIRE_CHARGE);
    }
}
