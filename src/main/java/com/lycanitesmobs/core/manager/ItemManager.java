package com.lycanitesmobs.core.manager;

import com.lycanitesmobs.core.item.summoningstaff.*;
import com.lycanitesmobs.core.item.special.ItemSoulgazer;
import com.lycanitesmobs.core.item.special.ItemSoulContract;
import com.lycanitesmobs.core.item.consumable.utility.ItemSoulstone;
import com.lycanitesmobs.core.block.building.HiveBlock;
import com.lycanitesmobs.core.item.block.ItemBlockPlacer;
import com.google.gson.JsonObject;
import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.data.info.item.ItemConfig;
import com.lycanitesmobs.core.data.info.item.ItemInfo;
import com.lycanitesmobs.core.data.info.ModInfo;
import com.lycanitesmobs.core.data.loaders.FileLoader;
import com.lycanitesmobs.core.data.loaders.JSONLoader;
import com.lycanitesmobs.core.data.loaders.StreamLoader;
import com.lycanitesmobs.core.block.cloud.BlockFrostCloud;
import com.lycanitesmobs.core.block.cloud.BlockPoisonCloud;
import com.lycanitesmobs.core.block.cloud.BlockPoopCloud;
import com.lycanitesmobs.core.block.fire.BlockDoomfire;
import com.lycanitesmobs.core.block.fire.BlockFrostfire;
import com.lycanitesmobs.core.block.fire.BlockHellfire;
import com.lycanitesmobs.core.block.fire.BlockIcefire;
import com.lycanitesmobs.core.block.fire.BlockPrimefire;
import com.lycanitesmobs.core.block.fire.BlockScorchfire;
import com.lycanitesmobs.core.block.fire.BlockShadowfire;
import com.lycanitesmobs.core.block.fire.BlockSmitefire;
import com.lycanitesmobs.core.block.web.BlockFrostweb;
import com.lycanitesmobs.core.block.web.BlockQuickWeb;
import com.lycanitesmobs.core.item.consumable.utility.ItemCleansingCrystal;
import com.lycanitesmobs.core.item.consumable.utility.ItemImmunizer;
import com.lycanitesmobs.core.item.special.ItemMobToken;
import com.lycanitesmobs.core.tabs.LMBlocksGroup;
import com.lycanitesmobs.core.tabs.LMChargesGroup;
import com.lycanitesmobs.core.tabs.LMCreaturesGroup;
import com.lycanitesmobs.core.tabs.LMItemsGroup;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
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
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> creaturesTab = TABS.register(LycanitesMobs.MODID + ".creatures", LMCreaturesGroup.getBuilder()::build);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> chargesTab = TABS.register(LycanitesMobs.MODID + ".charges", LMChargesGroup.getBuilder()::build);
    public static final Map<String, Item.Properties> registryItems = new HashMap<>();
    protected static ItemManager INSTANCE;
    protected static final Map<String, ItemInfo> items = new HashMap<>();

    public static void register(IEventBus modEventBus) {
        TABS.register(modEventBus);
    }

    /**
     * A list of blocks that need to use the cutout renderer.
     **/
    protected final List<Block> cutoutBlocks = new ArrayList<>();
    /**
     * A list of mod groups that have loaded with this manager.
     **/
    protected final List<ModInfo> loadedGroups = new ArrayList<>();
    /**
     * Handles all global item general config settings.
     **/
    protected ItemConfig config;

    public List<Block> getCutoutBlocks() {
        return Collections.unmodifiableList(this.cutoutBlocks);
    }

    public void registerCutoutBlock(Block block) {
        if (!this.cutoutBlocks.contains(block)) {
            this.cutoutBlocks.add(block);
        }
    }

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

        // Pets:
        Item.Properties itemPropertiesNoStack = new Item.Properties().stacksTo(1);
        ObjectManager.addItem("soulgazer", () -> new ItemSoulgazer(itemPropertiesNoStack));
        ObjectManager.addItem("soul_contract", () -> new ItemSoulContract(itemPropertiesNoStack));
        ObjectManager.addItem("soulstone", () -> new ItemSoulstone(itemProperties, null));

        // Summoning Staffs:
        Item.Properties summoningStaffProperties = new Item.Properties().stacksTo(1).durability(500);
        ObjectManager.addItem("summoningstaff", () -> new ItemStaffSummoning(summoningStaffProperties, "summoningstaff", "summoningstaff"));
        ObjectManager.addItem("stablesummoningstaff", () -> new ItemStaffStable(summoningStaffProperties, "stablesummoningstaff", "staffstable"));
        ObjectManager.addItem("bloodsummoningstaff", () -> new ItemStaffBlood(summoningStaffProperties, "bloodsummoningstaff", "staffblood"));
        ObjectManager.addItem("sturdysummoningstaff", () -> new ItemStaffSturdy(summoningStaffProperties, "sturdysummoningstaff", "staffsturdy"));
        ObjectManager.addItem("savagesummoningstaff", () -> new ItemStaffSavage(summoningStaffProperties, "savagesummoningstaff", "staffsavage"));

        // Special (place their effect block on use):
        ObjectManager.addItem("frostyfur", () -> new ItemBlockPlacer(itemProperties, "frostyfur", "frostcloud"));
        ObjectManager.addItem("poisongland", () -> new ItemBlockPlacer(itemProperties, "poisongland", "poisoncloud"));
        ObjectManager.addItem("geistliver", () -> new ItemBlockPlacer(itemProperties, "geistliver", "shadowfire"));

        BlockManager.addDungeonBlocks("lush");
        BlockManager.addDungeonBlocks("desert");
        BlockManager.addDungeonBlocks("shadow");
        BlockManager.addDungeonBlocks("demon");
        BlockManager.addDungeonBlocks("aberrant");
        BlockManager.addDungeonBlocks("ashen");
        BlockManager.addDungeonBlocks("stream");
        ObjectManager.addBlock("propolis", () -> new HiveBlock(Block.Properties.of().mapColor(MapColor.CLAY).sound(SoundType.WET_GRASS).strength(0.6F).randomTicks(), "propolis"), false);
        ObjectManager.addBlock("veswax", () -> new HiveBlock(Block.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(0.6F).randomTicks(), "veswax"), false);

        // Effect Blocks (BlockShadowfire excluded - needs BaseCreatureEntity, Phase 5):
        Block.Properties fireProperties = Block.Properties.of().mapColor(MapColor.FIRE).randomTicks().noCollission().dynamicShape().sound(SoundType.WOOL).noOcclusion();
        Block.Properties brightFireProperties = Block.Properties.of().mapColor(MapColor.FIRE).randomTicks().noCollission().dynamicShape().sound(SoundType.WOOL).noOcclusion().lightLevel((BlockState blockState) -> 15);
        ObjectManager.addSound("frostfire", "block.frostfire");
        ObjectManager.addBlock("frostfire", () -> new BlockFrostfire(fireProperties), false);
        ObjectManager.addSound("icefire", "block.icefire");
        ObjectManager.addBlock("icefire", () -> new BlockIcefire(fireProperties), false);
        ObjectManager.addSound("hellfire", "block.hellfire");
        ObjectManager.addBlock("hellfire", () -> new BlockHellfire(brightFireProperties), false);
        ObjectManager.addSound("doomfire", "block.doomfire");
        ObjectManager.addBlock("doomfire", () -> new BlockDoomfire(brightFireProperties), false);
        ObjectManager.addSound("primefire", "block.primefire");
        ObjectManager.addBlock("primefire", () -> new BlockPrimefire(brightFireProperties), false);
        ObjectManager.addSound("scorchfire", "block.scorchfire");
        ObjectManager.addBlock("scorchfire", () -> new BlockScorchfire(brightFireProperties), false);
        ObjectManager.addSound("smitefire", "block.smitefire");
        ObjectManager.addBlock("smitefire", () -> new BlockSmitefire(brightFireProperties), false);
        ObjectManager.addSound("shadowfire", "block.shadowfire");
        ObjectManager.addBlock("shadowfire", () -> new BlockShadowfire(fireProperties), false);

        Block.Properties cloudProperties = Block.Properties.of().mapColor(MapColor.NONE).randomTicks().noCollission().dynamicShape().sound(SoundType.WOOL).noOcclusion();
        ObjectManager.addSound("frostcloud", "block.frostcloud");
        ObjectManager.addBlock("frostcloud", () -> new BlockFrostCloud(cloudProperties), false);
        ObjectManager.addSound("poisoncloud", "block.poisoncloud");
        ObjectManager.addBlock("poisoncloud", () -> new BlockPoisonCloud(cloudProperties), false);
        ObjectManager.addSound("poopcloud", "block.poopcloud");
        ObjectManager.addBlock("poopcloud", () -> new BlockPoopCloud(cloudProperties), false);

        Block.Properties webProperties = Block.Properties.of().mapColor(MapColor.WOOL).randomTicks().noCollission().dynamicShape().sound(SoundType.WOOL).noOcclusion();
        ObjectManager.addBlock("quickweb", () -> new BlockQuickWeb(webProperties), false);
        ObjectManager.addBlock("frostweb", () -> new BlockFrostweb(webProperties), false);

        ObjectManager.addDamageType("ooze");
        ObjectManager.addDamageType("acid");

        // Not ported (S202 review, PORT_PLAN.md "S202 redesign decisions"): the 8 rare-variant altars + their
        // soulkeys/soulcubes are cut (boss soulkeys return with the new pedestal altar phase), holiday items
        // (halloweentreat, wintergift) are cut with the holiday events, equipment waits for the post-release
        // socket system. Pet items (soulgazer, soulstone, soul_contract, summoning staves) come with the pets
        // system (ExtendedPlayer). Charges, spawn eggs, saddles and treats are generated by ProjectileInfo /
        // CreatureManager, fluids + buckets by FluidManager.
    }

    // TODO Phase 4b: getEquipmentSharpnessRepair/getEquipmentManaRepair were dropped here -
    // both depend on ItemEquipment, which isn't ported yet.
}
