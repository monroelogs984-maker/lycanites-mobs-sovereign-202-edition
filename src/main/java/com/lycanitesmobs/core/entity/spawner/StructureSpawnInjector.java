package com.lycanitesmobs.core.entity.spawner;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.data.info.ModInfo;
import com.lycanitesmobs.core.data.loaders.FileLoader;
import com.lycanitesmobs.core.data.loaders.JSONLoader;
import com.lycanitesmobs.core.data.loaders.StreamLoader;
import com.lycanitesmobs.core.entity.spawner.location.StructureSpawnLocation;
import com.lycanitesmobs.core.manager.SpawnerManager;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.ModifiableStructureInfo;
import net.neoforged.neoforge.common.world.StructureModifier;
import net.neoforged.neoforge.common.world.StructureSettingsBuilder;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Injects Lycanites Mobs into vanilla/modded structure spawn pools (configs from
 * {@code common/lycanitesmobs/structurespawns/}), letting vanilla's NaturalSpawner handle structure spawning.
 *
 * Port (1.21.1): the official rewrote each Structure's ModifiableStructureInfo through a mixin accessor on
 * ServerAboutToStartEvent. NeoForge has a supported hook for exactly this - structure modifiers - so this registers
 * one modifier type ({@link JsonStructureSpawnsModifier}) and a single datapack modifier file
 * ({@code data/lycanitesmobs/neoforge/structure_modifier/structure_spawns.json}) that applies every loaded config.
 * The per-structure spawn cap is unchanged, on MobSpawnEvent.PositionCheck.
 */
public class StructureSpawnInjector extends JSONLoader {

    private static StructureSpawnInjector INSTANCE;

    public static final DeferredRegister<MapCodec<? extends StructureModifier>> STRUCTURE_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.STRUCTURE_MODIFIER_SERIALIZERS, LycanitesMobs.MODID);
    static {
        STRUCTURE_MODIFIER_SERIALIZERS.register("json_structure_spawns", () -> JsonStructureSpawnsModifier.CODEC);
    }

    private final List<StructureSpawnConfig> configs = new ArrayList<>();
    private final Map<EntityType<?>, List<StructureSpawnConfig>> cappedConfigsByType = new HashMap<>();

    public static StructureSpawnInjector getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new StructureSpawnInjector();
        }
        return INSTANCE;
    }

    public static void register(IEventBus modEventBus) {
        STRUCTURE_MODIFIER_SERIALIZERS.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(getInstance()::onMobSpawnPositionCheck);
    }

    /**
     * Loads the configs. Called in common setup, after the spawners (so replaced tick-based structure spawners can be
     * disabled) and before any server starts (structure modifiers are applied when a server loads its registries).
     */
    public void loadAllFromJson(ModInfo modInfo) {
        this.configs.clear();
        this.cappedConfigsByType.clear();
        this.loadAllJson(modInfo, "StructureSpawn", "structurespawns", "name", true, "structure_spawn", FileLoader.common(), StreamLoader.common());
        LMHelperClass.logDebug("structurespawners", "Loaded " + this.configs.size() + " structure spawn configs.");

        for (StructureSpawnConfig config : this.configs) {
            if (!config.enabled) {
                continue;
            }
            config.resolveTypes();

            // Auto-disable old tick-based structure spawners that are now replaced by injection.
            Spawner oldSpawner = SpawnerManager.getInstance().getSpawner(config.name);
            if (oldSpawner != null && oldSpawner.isDefinitionEnabled() && oldSpawner.hasLocationType(StructureSpawnLocation.class)) {
                oldSpawner.disableForStructureSpawnInjection();
                LMHelperClass.logDebug("structurespawners", "Auto-disabled old structure spawner '" + config.name + "', replaced by structure spawn injection.");
            }
        }
        this.indexCappedConfigsByType();
    }

    @Override
    public void parseJson(ModInfo modInfo, String loadGroup, JsonObject json) {
        StructureSpawnConfig config = new StructureSpawnConfig();
        config.loadFromJSON(json);
        this.configs.add(config);
    }

    /**
     * Adds this injector's spawns to one structure, called by the structure modifier for every structure.
     */
    void modifyStructure(Holder<Structure> structure, StructureSettingsBuilder settings) {
        ResourceLocation structureId = structure.unwrapKey().map(ResourceKey::location).orElse(null);
        if (structureId == null) {
            return;
        }
        for (StructureSpawnConfig config : this.configs) {
            if (!config.enabled || !config.hasStructure(structureId) || config.spawnerData.isEmpty()) {
                continue;
            }
            StructureSettingsBuilder.StructureSpawnOverrideBuilder overrides = settings.getSpawnOverrides(config.category);
            if (overrides == null) {
                // A new override uses the config's bounding box; an existing one keeps its own (official behaviour).
                overrides = settings.getOrAddSpawnOverrides(config.category);
                overrides.setBoundingBox(config.boundingBox);
            }
            for (MobSpawnSettings.SpawnerData data : config.spawnerData) {
                overrides.addSpawn(data);
            }
            LMHelperClass.logDebug("structurespawners", "Injected '" + config.name + "' spawns into " + structureId);
        }
    }

    /**
     * Enforces per-config caps on the number of creatures inside a structure's bounding box.
     * Fires on every natural spawn attempt after position/placement checks have passed but
     * before the mob is added to the world, so denying here cleanly cancels the spawn.
     */
    public void onMobSpawnPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL) return;
        if (this.cappedConfigsByType.isEmpty()) return;

        Mob mob = event.getEntity();
        List<StructureSpawnConfig> relevantConfigs = this.cappedConfigsByType.get(mob.getType());
        if (relevantConfigs == null || relevantConfigs.isEmpty()) return;

        ServerLevel level = event.getLevel().getLevel();
        BlockPos pos = mob.blockPosition();
        Map<Structure, LongSet> structuresAtPos = level.structureManager().getAllStructuresAt(pos);
        if (structuresAtPos.isEmpty()) return;

        Registry<Structure> structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (StructureSpawnConfig config : relevantConfigs) {
            for (Structure structure : structuresAtPos.keySet()) {
                ResourceLocation id = structureRegistry.getKey(structure);
                if (id == null || !config.hasStructure(id)) continue;

                StructureStart start = level.structureManager().getStructureAt(pos, structure);
                if (start == StructureStart.INVALID_START) continue;

                BoundingBox bb = start.getBoundingBox();
                int count = level.getEntitiesOfClass(Mob.class, AABB.of(bb), e -> config.resolvedTypes.contains(e.getType())).size();
                if (count >= config.maxInStructure) {
                    event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
                    return;
                }
            }
        }
    }

    private void indexCappedConfigsByType() {
        this.cappedConfigsByType.clear();
        for (StructureSpawnConfig config : this.configs) {
            if (!config.enabled || config.maxInStructure < 0) {
                continue;
            }
            for (EntityType<?> entityType : config.resolvedTypes) {
                this.cappedConfigsByType.computeIfAbsent(entityType, ignored -> new ArrayList<>()).add(config);
            }
        }
    }

    /**
     * The structure modifier type. Stateless: it reads the loaded configs, so one datapack entry covers all of them.
     */
    public static class JsonStructureSpawnsModifier implements StructureModifier {
        public static final JsonStructureSpawnsModifier INSTANCE = new JsonStructureSpawnsModifier();
        public static final MapCodec<JsonStructureSpawnsModifier> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public void modify(Holder<Structure> structure, Phase phase, ModifiableStructureInfo.StructureInfo.Builder builder) {
            if (phase == Phase.ADD) {
                StructureSpawnInjector.getInstance().modifyStructure(structure, builder.getStructureSettings());
            }
        }

        @Override
        public MapCodec<? extends StructureModifier> codec() {
            return CODEC;
        }
    }

    static class StructureSpawnConfig {
        String name;
        boolean enabled = true;
        MobCategory category = MobCategory.MONSTER;
        StructureSpawnOverride.BoundingBoxType boundingBox = StructureSpawnOverride.BoundingBoxType.PIECE;
        int maxInStructure = -1;
        List<ResourceLocation> structures = new ArrayList<>();
        Set<ResourceLocation> structureSet = new HashSet<>();
        List<MobEntryConfig> mobs = new ArrayList<>();
        final List<MobSpawnSettings.SpawnerData> spawnerData = new ArrayList<>();
        /** Cache of resolved entity types from this config's mob list, for fast membership checks during spawn events. */
        final Set<EntityType<?>> resolvedTypes = new HashSet<>();

        void loadFromJSON(JsonObject json) {
            if (json.has("name"))
                this.name = json.get("name").getAsString();
            if (json.has("enabled"))
                this.enabled = json.get("enabled").getAsBoolean();
            if (json.has("maxInStructure"))
                this.maxInStructure = json.get("maxInStructure").getAsInt();
            if (json.has("category")) {
                String catName = json.get("category").getAsString();
                for (MobCategory cat : MobCategory.values()) {
                    if (cat.getName().equalsIgnoreCase(catName)) {
                        this.category = cat;
                        break;
                    }
                }
            }
            if (json.has("boundingBox")) {
                String bb = json.get("boundingBox").getAsString();
                this.boundingBox = "structure".equalsIgnoreCase(bb)
                        ? StructureSpawnOverride.BoundingBoxType.STRUCTURE
                        : StructureSpawnOverride.BoundingBoxType.PIECE;
            }
            if (json.has("structures")) {
                this.structures.clear();
                this.structureSet.clear();
                for (JsonElement el : json.getAsJsonArray("structures")) {
                    ResourceLocation structureId = ResourceLocation.parse(el.getAsString());
                    this.structures.add(structureId);
                    this.structureSet.add(structureId);
                }
            }
            if (json.has("mobs")) {
                this.mobs.clear();
                for (JsonElement el : json.getAsJsonArray("mobs")) {
                    MobEntryConfig mob = new MobEntryConfig();
                    mob.loadFromJSON(el.getAsJsonObject());
                    this.mobs.add(mob);
                }
            }
        }

        boolean hasStructure(ResourceLocation structureId) {
            return this.structureSet.contains(structureId);
        }

        void resolveTypes() {
            this.spawnerData.clear();
            this.resolvedTypes.clear();
            for (MobEntryConfig mob : this.mobs) {
                // getOptional: the entity type registry is defaulted (an unknown id would return pig).
                EntityType<?> entityType = mob.mobId == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(mob.mobId).orElse(null);
                if (entityType == null) {
                    LMHelperClass.logWarning("structurespawners", "Entity type not found, skipping: " + mob.mobId);
                    continue;
                }
                this.spawnerData.add(new MobSpawnSettings.SpawnerData(entityType, mob.weight, mob.minCount, mob.maxCount));
                this.resolvedTypes.add(entityType);
            }
        }
    }

    static class MobEntryConfig {
        ResourceLocation mobId;
        int weight = 8;
        int minCount = 1;
        int maxCount = 3;

        void loadFromJSON(JsonObject json) {
            if (json.has("mobId"))
                this.mobId = ResourceLocation.parse(json.get("mobId").getAsString());
            if (json.has("weight"))
                this.weight = json.get("weight").getAsInt();
            if (json.has("minCount"))
                this.minCount = json.get("minCount").getAsInt();
            if (json.has("maxCount"))
                this.maxCount = json.get("maxCount").getAsInt();
        }
    }
}
