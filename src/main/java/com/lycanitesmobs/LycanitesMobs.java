package com.lycanitesmobs;

import com.lycanitesmobs.core.manager.EffectManager;
import com.lycanitesmobs.core.manager.ProjectileManager;
import com.lycanitesmobs.core.block.Material;
import com.lycanitesmobs.core.data.config.CoreConfig;
import com.lycanitesmobs.core.data.info.ModInfo;
import com.lycanitesmobs.core.data.info.ObjectLists;
import com.lycanitesmobs.core.data.loaders.FileLoader;
import com.lycanitesmobs.core.data.loaders.StreamLoader;
import com.lycanitesmobs.core.event.RegistryEvents;
import com.lycanitesmobs.core.manager.CreatureManager;
import com.lycanitesmobs.core.manager.SpawnerManager;
import com.lycanitesmobs.core.manager.ElementManager;
import com.lycanitesmobs.core.manager.ItemManager;
import com.lycanitesmobs.core.manager.FluidManager;
import com.lycanitesmobs.core.manager.ModAttributes;
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
import net.neoforged.fml.loading.FMLEnvironment;
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
    public static final com.lycanitesmobs.core.network.PacketManager PACKET_MANAGER = new com.lycanitesmobs.core.network.PacketManager();
    /** The client's own player, or null on a server. Set during client setup (replaces the original's PROXY.getClientPlayer()). **/
    public static java.util.function.Supplier<net.minecraft.world.entity.player.Player> CLIENT_PLAYER = () -> null;
    /** Opens a client screen by id (0 = Beastiary index, 1 = summoning), no-op on a server. Set during client setup. **/
    public static java.util.function.IntConsumer OPEN_SCREEN = screenId -> {};
    /** Starts/stops a mob event on the client (name, isWorldEvent); an empty name stops it. No-op on a server. Set during client setup. **/
    public static java.util.function.BiConsumer<String, Boolean> APPLY_MOB_EVENT = (mobEventName, worldEvent) -> {};
    public static final String versionMC = "1.21.1";

    // Lycanite's links (credit for the original mod, shown in the Beastiary index):
    public static final String twitter = "https://twitter.com/Lycanite05";
    public static final String patreon = "https://www.patreon.com/lycanite";
    public static final String guilded = "https://www.guilded.gg/i/jpLvd6J2";
    public static final String discord = "https://discord.gg/bFpV3z4";
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
        ModAttributes.ATTRIBUTES.register(modEventBus);
        ItemManager.register(modEventBus);
        FluidManager.register(modEventBus);
        com.lycanitesmobs.core.capabilities.entity.ExtendedPlayer.register(modEventBus);
        com.lycanitesmobs.core.capabilities.entity.ExtendedEntity.register(modEventBus);
        com.lycanitesmobs.core.container.block.SummoningPedestalContainer.init(); // adds its menu to CreatureContainer.MENUS
        com.lycanitesmobs.core.container.creature.CreatureContainer.MENUS.register(modEventBus);
        com.lycanitesmobs.core.block.blockentity.TileEntitySummoningPedestal.register(modEventBus);
        modEventBus.addListener(com.lycanitesmobs.core.network.PacketManager::registerPayloads);
        com.lycanitesmobs.core.manager.DeferredLevelActionManager.register();
        com.lycanitesmobs.core.event.PlayerEventListener.register();
        com.lycanitesmobs.core.event.EntityEventListener.register();
        com.lycanitesmobs.core.command.CommandManager.register();
        com.lycanitesmobs.core.capabilities.level.ExtendedWorld.register();
        com.lycanitesmobs.core.event.SpawnerEventListener.register();
        com.lycanitesmobs.core.manager.MobEventManager.register();
        com.lycanitesmobs.core.event.MobEventListener.register();
        com.lycanitesmobs.core.altar.BossAltar.registerListeners();
        com.lycanitesmobs.core.entity.spawner.StructureSpawnInjector.register(modEventBus);
        // Dungeons (Phase 7): the structure type, and the virtual datapack that turns the schematic JSONs into
        // structure/structure_set/biome tag JSONs.
        com.lycanitesmobs.core.worldgen.structure.ModStructureTypes.register(modEventBus);
        com.lycanitesmobs.core.worldgen.dungeon.DeferredBossSpawner.register();
        modEventBus.addListener(this::addPackFinders);

        // Forces ObjectManager's Lazy-deferred blocks/block-items to actually construct and
        // register while the registry is still open (fires on the mod event bus, after all
        // mod constructors run but before registries freeze). Without this, blocks silently
        // never register - see RegistryEvents' class comment for the crash this caused.
        modEventBus.addListener(RegistryEvents.getInstance()::registerBlocks);
        modEventBus.addListener(RegistryEvents.getInstance()::registerEntityTypes);
        modEventBus.addListener(RegistryEvents.getInstance()::registerEffects);
        modEventBus.addListener(RegistryEvents.getInstance()::registerEntityAttributes);

        // NOTE: guarded so com.lycanitesmobs.client.* (references EntityRenderersEvent, a
        // client-only class) is never classloaded on a dedicated server. Without ANY
        // registered EntityRenderer, NeoForge has no fallback and crashes the client the
        // moment a creature enters render range ("Cannot invoke ... because entityrenderer is
        // null") - see PORT_PLAN.md Phase 5f. Real per-creature model rendering is Phase 8;
        // this registers an invisible placeholder for every creature so they can be tested
        // (AI/combat/sounds) before models exist.
        if (FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(com.lycanitesmobs.client.ClientSetup::registerEntityRenderers);
            modEventBus.addListener(com.lycanitesmobs.client.ClientSetup::registerReloadListeners);
            modEventBus.addListener(com.lycanitesmobs.client.ClientSetup::registerItemColors);
            com.lycanitesmobs.client.ClientSetup.setClientPlayerSupplier();
            modEventBus.addListener(com.lycanitesmobs.client.ClientSetup::registerClientExtensions);
            modEventBus.addListener(com.lycanitesmobs.client.ClientSetup::registerMenuScreens);
            modEventBus.addListener(com.lycanitesmobs.client.event.OverlayEvents::registerGuiLayers);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.lycanitesmobs.client.event.OverlayEvents::onDebugText);
            modEventBus.addListener(com.lycanitesmobs.client.manager.KeyManager::registerKeyMappings);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.lycanitesmobs.client.manager.KeyManager::onClientTick);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.lycanitesmobs.client.manager.KeyManager::onKeyInput);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.lycanitesmobs.client.event.mobevent.ClientMobEventEvents::onClientUpdate);
            com.lycanitesmobs.client.event.FearClientEvents.register();
        }

        modEventBus.addListener(this::commonSetup);

        // TODO Phase 7: WorldGenManager (fluid pools, chunk spawn feature).
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
        EffectManager.getInstance().createEffects();
        ElementManager.getInstance().loadAllFromJson(modInfo);
        ObjectLists.createVanillaLists();
        ItemManager.getInstance().startup(modInfo);
        FluidManager.getInstance().defineFluids();
        // NOTE: does NOT call CreatureManager.loadConfig() here - config values can't be read
        // (ModConfigSpec$ConfigValue.get() throws IllegalStateException) until NeoForge's
        // ModConfigEvent.Loading has fired, which happens after the mod constructor. JSON
        // creature definitions don't need config values to parse (only to compute stats at
        // runtime), so startup() is still safe to call synchronously here alongside the other
        // registration-touching calls.
        // Equipment parts register items (so constructor time); they need elements loaded first.
        com.lycanitesmobs.core.manager.EquipmentPartManager.getInstance().loadAllFromJson(modInfo);
        CreatureManager.getInstance().startup(modInfo);
        ProjectileManager.getInstance().startup(modInfo);
    }

    /**
     * Registers a virtual datapack that generates the dungeon worldgen structure, structure_set and biome tag JSONs
     * from the dungeon schematic JSONs (so dungeon placement is configured through the schematics alone).
     */
    private void addPackFinders(final net.neoforged.neoforge.event.AddPackFindersEvent event) {
        if (event.getPackType() != net.minecraft.server.packs.PackType.SERVER_DATA) {
            return;
        }
        event.addRepositorySource(consumer -> {
            net.minecraft.server.packs.PackLocationInfo location = new net.minecraft.server.packs.PackLocationInfo(
                    "lycanitesmobs_dynamic_dungeons",
                    net.minecraft.network.chat.Component.literal("Lycanites Mobs Dynamic Dungeons"),
                    net.minecraft.server.packs.repository.PackSource.BUILT_IN,
                    java.util.Optional.empty());
            net.minecraft.server.packs.repository.Pack.ResourcesSupplier supplier = new net.minecraft.server.packs.repository.Pack.ResourcesSupplier() {
                @Override
                public net.minecraft.server.packs.PackResources openPrimary(net.minecraft.server.packs.PackLocationInfo info) {
                    return new com.lycanitesmobs.core.worldgen.structure.DungeonVirtualPack(info);
                }

                @Override
                public net.minecraft.server.packs.PackResources openFull(net.minecraft.server.packs.PackLocationInfo info, net.minecraft.server.packs.repository.Pack.Metadata metadata) {
                    return new com.lycanitesmobs.core.worldgen.structure.DungeonVirtualPack(info);
                }
            };
            net.minecraft.server.packs.repository.Pack pack = net.minecraft.server.packs.repository.Pack.readMetaAndCreate(
                    location, supplier, net.minecraft.server.packs.PackType.SERVER_DATA,
                    new net.minecraft.server.packs.PackSelectionConfig(true, net.minecraft.server.packs.repository.Pack.Position.TOP, false));
            if (pack != null) {
                consumer.accept(pack);
            }
        });
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        configReady = true;
        ObjectManager.setCurrentModInfo(modInfo);
        LMHelperClass.fixMaxHealth();
        Material.init();
        CreatureManager.getInstance().loadConfig();
        CreatureManager.getInstance().bindRegisteredValues();
        ProjectileManager.getInstance().bindRegisteredTypes();
        // Spawners resolve blocks/items/materials from the registries and creature ids from CreatureManager, so they
        // load here (registries frozen, Material lists built) rather than in loadContent() like the official.
        com.lycanitesmobs.core.manager.DungeonManager.getInstance().loadAllFromJson(modInfo);
        SpawnerManager.getInstance().loadAllFromJson(modInfo);
        // Mob events (after spawners, as in the official; their spawners load with SpawnerManager above):
        com.lycanitesmobs.core.manager.MobEventManager.getInstance().loadConfig();
        com.lycanitesmobs.core.manager.MobEventManager.getInstance().loadAllFromJson(modInfo);
        // Treat Lists: (resolve items/creature entity types, so after registration)
        com.lycanitesmobs.core.item.consumable.holiday.ItemHalloweenTreat.createObjectLists();
        com.lycanitesmobs.core.item.consumable.holiday.ItemWinterGift.createObjectLists();
        com.lycanitesmobs.core.entity.spawner.StructureSpawnInjector.getInstance().loadAllFromJson(modInfo);
    }
}
