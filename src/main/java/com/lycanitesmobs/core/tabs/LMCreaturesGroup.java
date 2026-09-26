package com.lycanitesmobs.core.tabs;

import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.data.info.creature.CreatureInfo;
import com.lycanitesmobs.core.data.info.creature.CreatureType;
import com.lycanitesmobs.core.item.consumable.entity.ItemCustomSpawnEgg;
import com.lycanitesmobs.core.manager.CreatureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Creative tab with one spawn egg stack per creature, grouped by creature type.
 */
public class LMCreaturesGroup {
    public CreativeModeTab.Builder builder = CreativeModeTab.builder();

    public LMCreaturesGroup() {
        this.builder = this.builder.title(Component.translatable("itemGroup." + LycanitesMobs.MODID + ".creatures"))
                .icon(this::getIconItem)
                .displayItems((enabledFeatures, entries) -> {
                    List<CreatureType> creatureTypes = new ArrayList<>(CreatureManager.getInstance().getCreatureTypes());
                    creatureTypes.sort(Comparator.comparing(CreatureType::getName));
                    for (CreatureType creatureType : creatureTypes) {
                        if (!(creatureType.getSpawnEggItem() instanceof ItemCustomSpawnEgg spawnEgg)) {
                            continue;
                        }
                        List<CreatureInfo> creatures = new ArrayList<>(creatureType.getCreatures());
                        creatures.sort(Comparator.comparing(CreatureInfo::getName));
                        for (CreatureInfo creatureInfo : creatures) {
                            if (creatureInfo.isDummy()) {
                                continue;
                            }
                            ItemStack itemStack = new ItemStack(spawnEgg, 1);
                            spawnEgg.applyCreatureInfoToItemStack(itemStack, creatureInfo);
                            entries.accept(itemStack);
                        }
                    }
                });
    }

    public static CreativeModeTab.Builder getBuilder() {
        return new LMCreaturesGroup().builder;
    }

    public ItemStack getIconItem() {
        CreatureType beast = CreatureManager.getInstance().getCreatureType("beast");
        if (beast != null && beast.getSpawnEggItem() != null)
            return new ItemStack(beast.getSpawnEggItem());
        return new ItemStack(Items.CREEPER_SPAWN_EGG);
    }
}
