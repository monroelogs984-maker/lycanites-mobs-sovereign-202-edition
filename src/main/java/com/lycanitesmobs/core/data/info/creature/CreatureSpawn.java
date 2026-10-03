package com.lycanitesmobs.core.data.info.creature;

import com.lycanitesmobs.core.data.config.ConfigCreatureSpawning;
import com.lycanitesmobs.core.entity.spawner.SpawnerMobRegistry;
import com.google.gson.JsonObject;
import com.lycanitesmobs.core.manager.CreatureManager;
import com.lycanitesmobs.core.util.helpers.JSONHelper;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contains default spawn conditions for a creature.
 **/
public class CreatureSpawn {

    // General:
    /**
     * If false, spawning of this creature is disabled, but this wont despawn existing creatures.
     **/
    protected boolean enabled = true;

    /**
     * If true, this mob wont naturally spawn as a subspecies.
     **/
    protected boolean disableVariants = false;


    // Spawners:
    /**
     * A list of Spawners that this creature should use.
     **/
    protected List<String> spawners = new ArrayList<>();

    /**
     * A list of Vanilla Creature Types to use.
     **/
    protected List<MobCategory> vanillaSpawnerTypes = new ArrayList<>();


    // Dimensions:
    /**
     * How the dimension ID list works. Can be whitelist or blacklist.
     **/
    protected String dimensionListType = "whitelist";

    /**
     * The dimension IDs that the world must or must not match depending on the list type.
     **/
    protected List<String> dimensionIds = new ArrayList<>();

    /**
     * The dimensions that the world must or must not match depending on the list type.
     **/
    protected List<DimensionType> dimensions = null;


    // Biomes:
    /**
     * The list of biome tags that this creature spawns in. Converts to a list of biomes on demand.
     **/
    protected List<String> biomeTags = new ArrayList<>();

    /**
     * The set of biomes generated from the list of biome tags. HashSet for O(1) lookup at spawn time.
     **/
    protected Set<String> biomesFromTags = null;

    protected List<String> biomeTagBlacklist = new ArrayList<>();
    protected Set<String> biomesFromTagBlacklist = null;

    /**
     * The list of specific biome ids that this creature spawns in.
     **/
    protected List<String> biomeIds = new ArrayList<>();

    /**
     * If true, the biome check will be ignored completely by this creature.
     **/
    protected boolean ignoreBiome = false;


    // Weights:
    /**
     * The chance of this mob spawning over others.
     **/
    protected int spawnWeight = 8;

    /** S202: "common" or "rare" uses the matching global weight from the spawning config instead of spawnWeight. **/
    protected String spawnRarity = "";

    /** S202: the lowest y level this creature can spawn at naturally (Integer.MIN_VALUE for no limit). **/
    protected int spawnMinY = Integer.MIN_VALUE;

    /** S202: plains' temperature, the line cold-only and hot-only creatures never cross (see getClimateRange). **/
    public static final float CLIMATE_TEMPERATURE_LINE = 0.8F;

    /** S202 climate range, computed on first use from the biome list. Null until computed. **/
    protected float[] climateRange;
    /** S202: optional {temperature, downfall} centre from the json ("climateCenter"), for creatures with no biome list
     * (e.g. water creatures) or to override the centre taken from the biome list. **/
    protected float[] climateCenter;
    protected boolean climateUnrestricted = false;

    /**
     * The chance of dungeons using this mob over others.
     **/
    protected int dungeonWeight = 200;


    // Limits:
    /**
     * The maximum arount of this mob allowed within the Spawn Area Search Limit.
     **/
    protected int spawnAreaLimit = 5;

    /**
     * The minimum number of this mob to group spawn at once.
     **/
    protected int spawnGroupMin = 1;

    /**
     * The maximum number of this mob to group spawn at once.
     **/
    protected int spawnGroupMax = 3;


    // Area Conditions:
    /**
     * Whether or not this mob can spawn in high light levels.
     **/
    protected boolean spawnsInLight = false;

    /**
     * Whether or not this mob can spawn in low light levels.
     **/
    protected boolean spawnsInDark = true;

    /**
     * The minimum world days that must have gone by, can accept fractions such as 5.25 for 5 and a quarter days.
     **/
    protected double worldDayMin = -1;


    // Despawning:
    /**
     * Whether this mob should despawn or not by default (some mobs can override persistence, such as once farmed).
     **/
    protected boolean despawnNatural = true;

    /**
     * Whether this mob should always despawn no matter what.
     **/
    protected boolean despawnForced = false;

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean disablesVariants() {
        return this.disableVariants;
    }

    public List<String> getSpawners() {
        return Collections.unmodifiableList(this.spawners);
    }

    public List<MobCategory> getVanillaSpawnerTypes() {
        return Collections.unmodifiableList(this.vanillaSpawnerTypes);
    }

    public String getDimensionListType() {
        return this.dimensionListType;
    }

    public List<String> getDimensionIds() {
        return Collections.unmodifiableList(this.dimensionIds);
    }

    public List<String> getBiomeTags() {
        return Collections.unmodifiableList(this.biomeTags);
    }

    public Set<String> getResolvedBiomeTags() {
        if (this.biomesFromTags == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(this.biomesFromTags);
    }

    public List<String> getBiomeTagBlacklist() {
        return Collections.unmodifiableList(this.biomeTagBlacklist);
    }

    public Set<String> getResolvedBiomeTagBlacklist() {
        if (this.biomesFromTagBlacklist == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(this.biomesFromTagBlacklist);
    }

    public List<String> getBiomeIds() {
        return Collections.unmodifiableList(this.biomeIds);
    }

    public boolean ignoresBiome() {
        return this.ignoreBiome;
    }

    public int getSpawnMinY() {
        return this.spawnMinY;
    }

    public int getSpawnWeight() {
        if (ConfigCreatureSpawning.INSTANCE != null) {
            if ("common".equals(this.spawnRarity))
                return ConfigCreatureSpawning.INSTANCE.spawnWeightCommon.get();
            if ("rare".equals(this.spawnRarity))
                return ConfigCreatureSpawning.INSTANCE.spawnWeightRare.get();
        }
        return this.spawnWeight;
    }

    public int getDungeonWeight() {
        return this.dungeonWeight;
    }

    public int getSpawnAreaLimit() {
        return this.spawnAreaLimit;
    }

    public int getSpawnGroupMin() {
        return this.spawnGroupMin;
    }

    public int getSpawnGroupMax() {
        return this.spawnGroupMax;
    }

    public boolean spawnsInLight() {
        return this.spawnsInLight;
    }

    public boolean spawnsInDark() {
        return this.spawnsInDark;
    }

    public double getWorldDayMin() {
        return this.worldDayMin;
    }

    public boolean despawnsNaturally() {
        return this.despawnNatural;
    }

    public boolean forcesDespawn() {
        return this.despawnForced;
    }


    /**
     * Loads this element from a JSON object.
     */
    public void loadFromJSON(CreatureInfo creatureInfo, JsonObject json) {
        if (json.has("enabled"))
            this.enabled = json.get("enabled").getAsBoolean();
        if (json.has("disableSubspecies"))
            this.disableVariants = json.get("disableSubspecies").getAsBoolean();

        this.spawners.clear();
        this.vanillaSpawnerTypes.clear();
        if (json.has("spawners")) {
            this.spawners = JSONHelper.getJsonStrings(json.get("spawners").getAsJsonArray());
            for (String spawner : this.spawners) {
                LMHelperClass.logDebug("Creature", "Adding " + creatureInfo.getName() + " to " + spawner + " global spawn list.");
                SpawnerMobRegistry.createSpawn(creatureInfo, spawner);

                if ("monster".equalsIgnoreCase(spawner))
                    this.vanillaSpawnerTypes.add(MobCategory.MONSTER);
                else if ("creature".equalsIgnoreCase(spawner))
                    this.vanillaSpawnerTypes.add(MobCategory.CREATURE);
                else if ("watercreature".equalsIgnoreCase(spawner))
                    this.vanillaSpawnerTypes.add(MobCategory.WATER_CREATURE);
                else if ("ambient".equalsIgnoreCase(spawner))
                    this.vanillaSpawnerTypes.add(MobCategory.AMBIENT);
            }
        }

        if (json.has("dimensionIds")) {
            this.dimensionIds.clear();
            this.dimensions = null;
            this.dimensionIds = JSONHelper.getJsonStrings(json.get("dimensionIds").getAsJsonArray());
        }
        if (json.has("dimensionListType"))
            this.dimensionListType = json.get("dimensionListType").getAsString();

        if (json.has("ignoreBiome"))
            this.ignoreBiome = json.get("ignoreBiome").getAsBoolean();

        if (json.has("biomeTags")) {
            this.biomeTags.clear();
            this.biomeTagBlacklist.clear();
            this.biomesFromTags = null;
            this.biomesFromTagBlacklist = null;
            JSONHelper.clearBiomeTagCache();

            List<String> rawTags = JSONHelper.getJsonStrings(json.get("biomeTags").getAsJsonArray());
            for (String raw : rawTags) {
                if (raw != null && !raw.isEmpty() && raw.charAt(0) == '-') {
                    String stripped = raw.substring(1);
                    if (!stripped.isEmpty()) {
                        this.biomeTagBlacklist.add(stripped);
                    }
                }
                this.biomeTags.add(raw);
            }
        } else if (json.has("biomes")) {
            this.biomeTags.clear();
            this.biomeTagBlacklist.clear();
            this.biomesFromTags = null;
            this.biomesFromTagBlacklist = null;
            JSONHelper.clearBiomeTagCache();

            List<String> rawTags = JSONHelper.getJsonStrings(json.get("biomes").getAsJsonArray());
            for (String raw : rawTags) {
                if (raw != null && !raw.isEmpty() && raw.charAt(0) == '-') {
                    String stripped = raw.substring(1);
                    if (!stripped.isEmpty()) {
                        this.biomeTagBlacklist.add(stripped);
                    }
                }
                this.biomeTags.add(raw);
            }
        }

        if (json.has("biomeIds")) {
            this.biomeIds.clear();
            this.biomeIds = JSONHelper.getJsonStrings(json.get("biomeIds").getAsJsonArray());
        }

        if (json.has("spawnWeight"))
            this.spawnWeight = json.get("spawnWeight").getAsInt();
        if (json.has("climateCenter")) {
            var center = json.get("climateCenter").getAsJsonArray();
            this.climateCenter = new float[] {center.get(0).getAsFloat(), center.get(1).getAsFloat()};
        }
        if (json.has("spawnRarity"))
            this.spawnRarity = json.get("spawnRarity").getAsString();
        if (json.has("dungeonWeight"))
            this.dungeonWeight = json.get("dungeonWeight").getAsInt();

        if (json.has("spawnAreaLimit"))
            this.spawnAreaLimit = json.get("spawnAreaLimit").getAsInt();
        if (json.has("spawnGroupMin"))
            this.spawnGroupMin = json.get("spawnGroupMin").getAsInt();
        if (json.has("spawnGroupMax"))
            this.spawnGroupMax = json.get("spawnGroupMax").getAsInt();

        if (json.has("spawnsInLight"))
            this.spawnsInLight = json.get("spawnsInLight").getAsBoolean();
        if (json.has("spawnsInDark"))
            this.spawnsInDark = json.get("spawnsInDark").getAsBoolean();
        if (json.has("worldDayMin"))
            this.worldDayMin = json.get("worldDayMin").getAsDouble();

        if (json.has("spawnMinY"))
            this.spawnMinY = json.get("spawnMinY").getAsInt();
        this.climateRange = null;
        this.excludedBiomes = null;

        if (json.has("despawnNatural"))
            this.despawnNatural = json.get("despawnNatural").getAsBoolean();
        if (json.has("despawnForced"))
            this.despawnForced = json.get("despawnForced").getAsBoolean();
    }


    /**
     * Registers this mob to vanilla spawners and dungeons. Can only be done during startup.
     *
     * TODO Phase 6+: the original also called Forge's DungeonHooks.addDungeonMob() here to
     * register this creature with vanilla dungeon spawners. DungeonHooks doesn't exist in
     * NeoForge at all (removed, no direct replacement) - dropped rather than guessed at a
     * replacement. Revisit alongside the dungeon/worldgen phase if vanilla dungeon spawning
     * (as opposed to this mod's own Lycanites dungeons) turns out to matter.
     */
    public void registerVanillaSpawns(CreatureInfo creatureInfo, CreatureSpawnConfig spawnConfig) {
    }


    /**
     * Returns if this creature is allowed to spawn in the provided world dimension.
     *
     * @param world The world to check.
     * @return True if allowed, false if disallowed.
     */
    public boolean isAllowedDimension(Level world) {
        if (world == null) {
            LMHelperClass.logDebug("MobSpawns", "No world or dimension spawn settings were found, defaulting to valid.");
            return true;
        }

        // Global Check:
        if (!CreatureManager.getInstance().getSpawnConfig().isAllowedGlobal(world)) {
            return false;
        }

        // Default:
        if (this.dimensionIds.isEmpty()) {
            return true;
        }

        // Check IDs:
        String targetDimensionId = world.dimension().location().toString();
        for (String dimensionId : this.dimensionIds) {
            if (dimensionId.equals(targetDimensionId)) {
                return this.dimensionListType.equalsIgnoreCase("whitelist");
            }
        }
        return this.dimensionListType.equalsIgnoreCase("blacklist");
    }


    /**
     * Returns if the provided biome is valid for this creature to spawn in.
     *
     * @param biome The biome to check.
     * @return True if a valid biome.
     */
    public boolean isValidBiome(Level level, Biome biome) {
        if (this.ignoreBiome) {
            return true;
        }

        // S202: biome lists are loosened to climate. The Nether and End each have one climate, so outside the
        // Overworld only the dimension condition applies.
        if (level.dimension() != Level.OVERWORLD) {
            return true;
        }
        float[] range = this.getClimateRange(level);
        if (!this.climateUnrestricted && range != null) {
            float temperature = biome.getBaseTemperature();
            float downfall = biome.getModifiedClimateSettings().downfall();
            if (temperature < range[0] || temperature > range[1] || downfall < range[2] || downfall > range[3]) {
                return false;
            }
        }
        // Glenn 2026-10-03: the json's excluded tags ("-minecraft:is_ocean"...) still apply, e.g. Silex and Stryder
        // stay freshwater. (The climate conversion only used the positive tags.)
        var biomeId = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getKey(biome);
        return biomeId == null || !this.getExcludedBiomes(level).contains(biomeId.toString());
    }

    /** S202: biome ids from this creature's excluded ("-") biome tags, computed on first use. **/
    protected Set<String> excludedBiomes;

    private Set<String> getExcludedBiomes(Level level) {
        if (this.excludedBiomes == null) {
            List<String> excludedTags = new ArrayList<>();
            for (String tag : this.biomeTags) {
                if (tag.startsWith("-") && !tag.equalsIgnoreCase("-minecraft:is_end") && !tag.equalsIgnoreCase("-minecraft:is_nether")) {
                    excludedTags.add(tag.substring(1));
                }
            }
            this.excludedBiomes = excludedTags.isEmpty() ? Set.of() : new HashSet<>(JSONHelper.getBiomesFromTags(level, excludedTags));
        }
        return this.excludedBiomes;
    }

    /**
     * S202: the climate this creature spawns in: {temperature min, temperature max, downfall min, downfall max}, from the
     * Overworld biomes in its biome list, widened by the climate margins. Unrestricted when the list is empty or covers
     * every Overworld biome.
     */
    public float[] getClimateRange(Level level) {
        if (this.climateRange != null || this.climateUnrestricted) {
            return this.climateRange;
        }
        Set<String> biomeIdSet = new HashSet<>();
        if (!this.biomeTags.isEmpty()) {
            List<String> positiveTags = new ArrayList<>();
            for (String tag : this.biomeTags) {
                if (!tag.startsWith("-")) {
                    positiveTags.add(tag);
                }
            }
            biomeIdSet.addAll(JSONHelper.getBiomesFromTags(level, positiveTags));
        }
        biomeIdSet.addAll(this.biomeIds);

        var biomeRegistry = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
        float[] range = null;
        float[] world = null;
        int overworldBiomes = 0;
        int matchedOverworldBiomes = 0;
        for (var entry : biomeRegistry.entrySet()) {
            var holder = biomeRegistry.getHolderOrThrow(entry.getKey());
            if (!holder.is(net.minecraft.tags.BiomeTags.IS_OVERWORLD)) {
                continue;
            }
            overworldBiomes++;
            Biome biome = entry.getValue();
            float temperature = biome.getBaseTemperature();
            float downfall = biome.getModifiedClimateSettings().downfall();
            world = this.extendClimate(world, temperature, downfall);
            if (!biomeIdSet.contains(entry.getKey().location().toString())) {
                continue;
            }
            matchedOverworldBiomes++;
            range = this.extendClimate(range, temperature, downfall);
        }
        if (world == null) {
            this.climateUnrestricted = true;
            return null;
        }

        // S202 equal bands (design/SPAWN_BUDGET.md): every restricted creature gets the same band size, centred on its
        // original habitat, so no creature is far more widespread than another. Creatures with no biome list (or one
        // covering every biome) stay unrestricted unless the json gives a climateCenter.
        float centerTemperature;
        float centerDownfall;
        if (this.climateCenter != null) {
            centerTemperature = this.climateCenter[0];
            centerDownfall = this.climateCenter[1];
        } else if (range == null || matchedOverworldBiomes >= overworldBiomes) {
            this.climateUnrestricted = true;
            return null;
        } else {
            centerTemperature = (range[0] + range[1]) / 2;
            centerDownfall = (range[2] + range[3]) / 2;
        }
        float temperatureWidth = ConfigCreatureSpawning.INSTANCE.climateTemperatureWidth.get().floatValue();
        float downfallWidth = ConfigCreatureSpawning.INSTANCE.climateDownfallWidth.get().floatValue();
        // Glenn 2026-10-03: plains temperature is a hard line. A creature whose original habitat was all colder than plains
        // (snow creatures) never reaches plains or warmer, and one whose habitat was all warmer (desert creatures, beaches
        // aside) never reaches plains or colder. The band keeps its width where the world allows, then is cut at the line.
        float temperatureMin = world[0];
        float temperatureMax = world[1];
        if (this.climateCenter == null && range != null) {
            if (range[1] < CLIMATE_TEMPERATURE_LINE) {
                temperatureMax = Math.min(temperatureMax, CLIMATE_TEMPERATURE_LINE - 0.01F);
            } else if (range[0] >= CLIMATE_TEMPERATURE_LINE) {
                temperatureMin = Math.max(temperatureMin, CLIMATE_TEMPERATURE_LINE + 0.01F);
            }
        }
        float[] temperatureBand = fitBand(centerTemperature, temperatureWidth, temperatureMin, temperatureMax);
        float[] downfallBand = fitBand(centerDownfall, downfallWidth, world[2], world[3]);
        range = new float[] {temperatureBand[0], temperatureBand[1], downfallBand[0], downfallBand[1]};
        this.climateRange = range;
        return range;
    }

    private float[] extendClimate(float[] climate, float temperature, float downfall) {
        if (climate == null) {
            return new float[] {temperature, temperature, downfall, downfall};
        }
        climate[0] = Math.min(climate[0], temperature);
        climate[1] = Math.max(climate[1], temperature);
        climate[2] = Math.min(climate[2], downfall);
        climate[3] = Math.max(climate[3], downfall);
        return climate;
    }

    /** A band of the given width around the centre, shifted to stay inside the world's range (clamped if wider). **/
    private static float[] fitBand(float center, float width, float worldMin, float worldMax) {
        float min = center - width / 2;
        float max = center + width / 2;
        if (min < worldMin) {
            max += worldMin - min;
            min = worldMin;
        }
        if (max > worldMax) {
            min -= max - worldMax;
            max = worldMax;
        }
        return new float[] {Math.max(min, worldMin), max};
    }

    /** S202: a readable summary of this creature's spawn climate, for the dev dump command. **/
    public String describeClimate(Level level) {
        if (this.ignoreBiome) {
            return "anywhere (no biome condition)";
        }
        float[] range = this.getClimateRange(level);
        if (this.climateUnrestricted || range == null) {
            return "anywhere";
        }
        return String.format("temperature %.2f to %.2f, downfall %.2f to %.2f", range[0], range[1], Math.max(0, range[2]), Math.min(1, range[3]));
    }

}
