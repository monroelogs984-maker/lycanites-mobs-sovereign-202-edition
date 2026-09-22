package com.lycanitesmobs;

import com.lycanitesmobs.core.block.Material;
import com.lycanitesmobs.core.data.config.CoreConfig;
import com.lycanitesmobs.core.data.info.ModInfo;
import com.lycanitesmobs.core.data.info.ObjectLists;
import com.lycanitesmobs.core.data.loaders.FileLoader;
import com.lycanitesmobs.core.data.loaders.StreamLoader;
import com.lycanitesmobs.core.event.RegistryEvents;
import com.lycanitesmobs.core.manager.ElementManager;
import com.lycanitesmobs.core.manager.ItemManager;
import com.lycanitesmobs.core.manager.ObjectManager;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * NeoForge 1.21.1 port of Lycanites Mobs, ported from the official Forge 1.20.1-dev source
 * (gitlab.com/Lycanite/LycanitesMobs). See PORT_PLAN.md for phase status - this class currently
 * only wires up what's been ported so far (Phase 1/2: core bootstrap + data-loading substrate).
 * Everything content-related (items, blocks, creatures, spawners, worldgen, client rendering,
 * networking, event listeners) is deferred to later phases and deliberately not called yet.
 */
@Mod(LycanitesMobs.MODID)
public class LycanitesMobs {
    public static final Logger LOGGER = LogManager.getLogger();

    public static final String MODID = "lycanitesmobs";
    public static final String name = "Lycanites Mobs";
    public static final String versionNumber = "0.1.0-neoforge-alpha";
    public static final String versionMC = "1.21.1";
    public static final String version = versionNumber + " - MC " + versionMC;
    public static final String website = "https://lycanitesmobs.com";

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);

    public static ModInfo modInfo;
    public static boolean configReady = false;
    public static boolean earlyDebug = false;

    /**
     * Constructor - NeoForge injects the mod event bus and mod container directly, replacing
     * Forge's FMLJavaModLoadingContext.get().getModEventBus() / ModLoadingContext.get() lookups.
     */
    public LycanitesMobs(IEventBus modEventBus, ModContainer modContainer) {
        modInfo = new ModInfo(this, name, 1000);

        CoreConfig.buildSpec();
        modContainer.registerConfig(ModConfig.Type.COMMON, CoreConfig.SPEC);

        FileLoader.initAll(modInfo.modid);
        StreamLoader.initAll(modInfo.modid);

        ITEMS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        ItemManager.register(modEventBus);

        // Forces ObjectManager's Lazy-deferred blocks/block-items to actually construct and
        // register while the registry is still open (fires on the mod event bus, after all
        // mod constructors run but before registries freeze). Without this, blocks silently
        // never register - see RegistryEvents' class comment for the crash this caused.
        modEventBus.addListener(RegistryEvents.getInstance()::registerBlocks);

        modEventBus.addListener(this::commonSetup);

        // TODO Phase 4d+: EquipmentPartManager registration - needs ItemEquipmentPart.
        // TODO Phase 5: CreatureManager registration + bindRegisteredValues().
        // TODO Phase 6: ProjectileManager/SpawnerManager/StructureSpawnInjector/AltarInfo/
        //       MobEventManager/DungeonManager, and their NeoForge.EVENT_BUS listener registrations
        //       (SpawnerEventListener, CommandManager, GameEventListener, MobEventListener).
        // TODO Phase 6: PacketManager networking registration.
        // TODO Phase 7: WorldGenManager, ModStructureTypes/ModStructurePieceTypes, the dynamic
        //       dungeon datapack (addPackFinders/AddPackFindersEvent -> DungeonVirtualPack).
        // TODO Phase 8: client setup (see LycanitesMobsClient) - TextureManager/ModelManager/
        //       ClientManager, menu screen registration.
        // TODO Phase 9: Oculus/Iris compat (OculusCompat.init()), DyntopoLib dev-tooling hook.

        this.loadContent();

        LOGGER.info("[Lycanites Mobs] NeoForge 1.21.1 port scaffold loaded - " + version);
    }

    /**
     * Registers all content (elements, items, blocks, ...). Must run synchronously during mod
     * construction, NOT as an FMLCommonSetupEvent listener - NeoForge's DeferredRegister throws
     * IllegalStateException on any register() call after its RegisterEvent has fired, and that
     * fires well before common setup. This bit me once already (see PORT_PLAN.md Phase 4) -
     * anything that calls ObjectManager.addItem/addBlock/etc (directly or via a manager's
     * startup()) belongs here, in the constructor's call graph, not in commonSetup().
     */
    private void loadContent() {
        ElementManager.getInstance().loadAllFromJson(modInfo);
        ObjectLists.createVanillaLists();
        ItemManager.getInstance().startup(modInfo);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        configReady = true;
        ObjectManager.setCurrentModInfo(modInfo);
        LMHelperClass.fixMaxHealth();
        Material.init();
    }
}
