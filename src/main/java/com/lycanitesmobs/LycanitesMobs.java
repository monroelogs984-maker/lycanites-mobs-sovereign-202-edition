package com.lycanitesmobs;

import com.lycanitesmobs.core.data.config.CoreConfig;
import com.lycanitesmobs.core.data.info.ModInfo;
import com.lycanitesmobs.core.data.loaders.FileLoader;
import com.lycanitesmobs.core.data.loaders.StreamLoader;
import com.lycanitesmobs.core.manager.ElementManager;
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

        modEventBus.addListener(this::commonSetup);

        // TODO Phase 4: FluidManager/EffectManager - both are gated on ObjectManager, which is
        // gated on ItemManager/EquipmentPartManager (registryObjects()); can't be separated from
        // Phase 4 the way ElementManager could. ObjectLists content loading is also Phase 4.
        // TODO Phase 4: ItemManager/EquipmentPartManager registration (registryObjects()).
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

        LOGGER.info("[Lycanites Mobs] NeoForge 1.21.1 port scaffold loaded - " + version);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        configReady = true;
        LMHelperClass.fixMaxHealth();
        ElementManager.getInstance().loadAllFromJson(modInfo);
    }
}
