package com.lycanitesmobs.core.manager;

import com.google.gson.JsonObject;
import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.data.info.item.ItemConfig;
import com.lycanitesmobs.core.data.info.item.ItemInfo;
import com.lycanitesmobs.core.data.info.ModInfo;
import com.lycanitesmobs.core.data.loaders.FileLoader;
import com.lycanitesmobs.core.data.loaders.JSONLoader;
import com.lycanitesmobs.core.data.loaders.StreamLoader;
import com.lycanitesmobs.core.item.consumable.utility.ItemCleansingCrystal;
import com.lycanitesmobs.core.item.consumable.utility.ItemImmunizer;
import com.lycanitesmobs.core.item.special.ItemMobToken;
import com.lycanitesmobs.core.tabs.LMBlocksGroup;
import com.lycanitesmobs.core.tabs.LMItemsGroup;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.*;

/**
 * Being ported in batches for Phase 4 of the NeoForge port - Phase 4a wired up the items tab
 * plus one proof item; Phase 4b adds the blocks tab plus the "lush" dungeon block set as proof
 * that BlockManager's registration path works. The original registered creatures/equipment
 * parts/charges/best-equipment tabs, ~40 more items, and ~50 more blocks (the other 6 dungeon
 * stone sets, effect blocks, equipment blocks) - those come back in later Phase 4 batches
 * alongside the item/block classes and systems (creature system, capabilities, altars) they
 * depend on. See PORT_PLAN.md.
 */
public class ItemManager extends JSONLoader {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LycanitesMobs.MODID);
    // Creative Tabs:
    public static final CreativeModeTab.Builder itemsGroup =
            LMItemsGroup.getBuilder();

    public static final CreativeModeTab.Builder blocksGroup =
            LMBlocksGroup.getBuilder();

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> itemTab = TABS.register(LycanitesMobs.MODID + ".items", itemsGroup::build);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> blockTab = TABS.register(LycanitesMobs.MODID + ".blocks", blocksGroup::build);
    public static final Map<String, Item.Properties> registryItems = new HashMap<>();
    protected static ItemManager INSTANCE;
    protected static final Map<String, ItemInfo> items = new HashMap<>();

    public static void register(IEventBus modEventBus) {
        TABS.register(modEventBus);
    }

    /**
     * A list of mod groups that have loaded with this manager.
     **/
    protected final List<ModInfo> loadedGroups = new ArrayList<>();
    /**
     * Handles all global item general config settings.
     **/
    protected ItemConfig config;

    /**
     * Returns the main Item Manager instance or creates it and returns it.
     **/
    public static ItemManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ItemManager();
        }
        return INSTANCE;
    }

    public static Collection<ItemInfo> getItemInfos() {
        return Collections.unmodifiableCollection(items.values());
    }

    public static boolean hasItemInfo(String name) {
        return items.containsKey(name);
    }

    /**
     * Called during startup and initially loads everything in this manager.
     *
     * @param modInfo The mod loading this manager.
     */
    public void startup(ModInfo modInfo) {
        loadItems();
        loadAllFromJson(modInfo);
    }

    /**
     * Loads all JSON Items.
     **/
    public void loadAllFromJson(ModInfo modInfo) {
        this.rememberLoadedGroup(modInfo);
        this.loadAllJson(modInfo, "Items", "items", "name", true, null, FileLoader.common(), StreamLoader.common());
        LMHelperClass.logDebug("Items", "Complete! " + this.items.size() + " JSON Items Loaded In Total.");
    }

    protected void rememberLoadedGroup(ModInfo modInfo) {
        if (!this.loadedGroups.contains(modInfo)) {
            this.loadedGroups.add(modInfo);
        }
    }

    @Override
    public void parseJson(ModInfo modInfo, String loadGroup, JsonObject json) {
        ItemInfo itemInfo = new ItemInfo(modInfo);
        itemInfo.loadFromJSON(json);
        if (Objects.equals(itemInfo.getName(), "unamed_item")) return;
        this.items.put(itemInfo.getName(), itemInfo);
    }

    /**
     * Called during early start up, loads all global configs into this manager.
     **/
    public void loadConfig() {
        ItemConfig.loadGlobalSettings();
    }


    /**
     * Called during early start up, loads all non-json items.
     **/
    public void loadItems() {
        Item.Properties itemProperties = new Item.Properties();

        ObjectManager.addItem("mobtoken", () -> new ItemMobToken(new Item.Properties()));
        ObjectManager.addItem("immunizer", () -> new ItemImmunizer(itemProperties));
        ObjectManager.addItem("cleansingcrystal", () -> new ItemCleansingCrystal(itemProperties));

        BlockManager.addDungeonBlocks("lush");

        // TODO Phase 4c+: the remaining ~38 hardcoded items (soulgazer, soulstone, equipment,
        // soulkeys, summoning staves - all gated on the creature system/ExtendedPlayer
        // capability/AltarInfo, none of which exist yet) and ~40 more blocks (the other 6
        // dungeon stone sets, fire/cloud/web effect blocks, the 5 special equipment/pedestal
        // blocks). Also EquipmentPartManager/LMEquipmentPartsGroup, LMChargesGroup,
        // LMBestEquipmentGroup, LMCreaturesGroup - batch these the same way Phase 5's 123
        // creatures will be batched, not all at once.
    }

    // TODO Phase 4b: getEquipmentSharpnessRepair/getEquipmentManaRepair were dropped here -
    // both depend on ItemEquipment, which isn't ported yet.
}
