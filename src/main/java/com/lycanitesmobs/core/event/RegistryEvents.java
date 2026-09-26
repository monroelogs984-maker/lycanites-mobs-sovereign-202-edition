package com.lycanitesmobs.core.event;

import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.block.BlockTypeGetter;
import com.lycanitesmobs.core.data.info.creature.CreatureInfo;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.manager.CreatureManager;
import com.lycanitesmobs.core.manager.ObjectManager;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Trimmed down to just block/block-item registration for now - the original also handles
 * entities, general items, effects, fluids, tile entities, attributes, stats, and commands,
 * all of which need Phase 5/6 systems (CreatureManager, StatManager, command classes, etc.)
 * that don't exist yet. Port the rest back in alongside those phases.
 *
 * This exists because ObjectManager.addBlock() only stores blocks as a Lazy supplier in a
 * local map - unlike addItem(), it never calls a DeferredRegister itself. Something has to
 * force that Lazy to construct the Block AND register it to the real registry while the
 * registry is still open. RegisterEvent (fired on the mod event bus, before registries
 * freeze) is that something. Without this class, blocks silently never get registered - no
 * error at mod load, but the first thing that forces the Lazy (e.g. opening the creative
 * inventory, which calls ObjectManager.getBlock() to build the blocks tab) crashes with
 * "IllegalStateException: Registry is already frozen", because by then the registry really
 * is frozen and the Block constructor's intrusive-holder registration has nowhere valid to
 * go. Found by actually opening the inventory in Lycannots - runServer alone never exercises
 * this code path since a dedicated server never builds creative tab contents.
 */
public class RegistryEvents {
    private static RegistryEvents INSTANCE;

    public static RegistryEvents getInstance() {
        if (INSTANCE == null)
            INSTANCE = new RegistryEvents();
        return INSTANCE;
    }

    /**
     * Registers blocks and their corresponding block items.
     * <p>
     * Block items must be registered in the same Registry Event after blocks
     * to ensure that a non-null block is passed to the BlockItem constructor.
     * This sequence guarantees that the block references used in block items
     * are fully initialized and registered, avoiding any potential issues
     * with null references or unregistered blocks.
     * </p>
     *
     * @param event The register event for blocks and items.
     */
    public void registerBlocks(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            int count = 0;
            for (Lazy<? extends Block> block : ObjectManager.getBlockSuppliers()) {
                BlockTypeGetter getter = (BlockTypeGetter) block.get();
                helper.register(getter.getRegistryName(), block.get());
                count++;
            }
            for (Lazy<? extends LiquidBlock> lazy : ObjectManager.getLiquidBlockSuppliers()) {
                LiquidBlock lb = lazy.get();
                BlockTypeGetter getter = (BlockTypeGetter) lb;
                helper.register(getter.getRegistryName(), lb);
                count++;
            }
            LMHelperClass.logInfoMessage("Registered " + count + " blocks.");
        });
        event.register(Registries.ITEM,
                helper -> {
                    int count = 0;
                    for (Map.Entry<String, Lazy<? extends BlockItem>> entry : ObjectManager.getBlockItemEntries()) {
                        String name = entry.getKey();
                        Supplier<? extends BlockItem> blockItem = entry.getValue();
                        LMHelperClass.logDebug("Item", "Registering item block: " + name);
                        if (name == null) {
                            LMHelperClass.logWarningMessage("Block Item: " + name + " has no Registry Name!");
                            continue;
                        }
                        helper.register(ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, name), blockItem.get());
                        count++;
                    }
                    LMHelperClass.logInfoMessage("Registered " + count + " block items.");
                }
        );
    }

    /**
     * Forces ObjectManager's Lazy-deferred entity types to actually build and register while
     * the registry is still open - same bug class as registerBlocks() above.
     * ObjectManager.addEntityType() only ever stored a Lazy in a local map; nothing forced it
     * to resolve during the registration window until this was added.
     */
    public void registerEntityTypes(RegisterEvent event) {
        event.register(Registries.ENTITY_TYPE, helper -> {
            int count = 0;
            for (Map.Entry<String, Lazy<? extends EntityType<?>>> entry : ObjectManager.getEntityTypeEntries()) {
                helper.register(ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, entry.getKey()), entry.getValue().get());
                count++;
            }
            LMHelperClass.logInfoMessage("Registered " + count + " entity types.");
        });
    }

    /**
     * Attaches an AttributeSupplier (health/damage/speed/etc attribute map) to every
     * registered creature EntityType. Without this, spawning a creature throws
     * "Entity ... has no attributes" - getAttribute() calls in applyDynamicAttributes()
     * would NPE. Fires on the mod event bus after RegisterEvent, before common setup, so
     * entity types are already registered by the time this runs.
     */
    public void registerEntityAttributes(EntityAttributeCreationEvent event) {
        CreatureManager cm = CreatureManager.getInstance();
        for (var entry : BuiltInRegistries.ENTITY_TYPE.entrySet()) {
            ResourceLocation loc = entry.getKey().location();
            if (!loc.getNamespace().equals(LycanitesMobs.MODID)) continue;
            CreatureInfo creatureInfo = cm.getCreature(loc.getPath());
            if (creatureInfo == null) continue;
            event.put((EntityType<? extends LivingEntity>) entry.getValue(), BaseCreatureEntity.registerCustomAttributes().build());
        }
    }
}
