package com.lycanitesmobs.core.data.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ConfigCreatureSpawning {
	public static ConfigCreatureSpawning INSTANCE;

	public final ModConfigSpec.ConfigValue<Integer> typeSpawnLimit;
	public final ModConfigSpec.ConfigValue<Double> spawnLimitRange;
	public final ModConfigSpec.ConfigValue<Boolean> disableAllSpawning;
	public final ModConfigSpec.ConfigValue<Boolean> enforceBlockCost;
	public final ModConfigSpec.ConfigValue<Double> spawnWeightScale;
	public final ModConfigSpec.ConfigValue<Integer> spawnWeightCommon;
	public final ModConfigSpec.ConfigValue<Integer> spawnWeightRare;
	public final ModConfigSpec.ConfigValue<Boolean> useSurfaceLightLevel;
	public final ModConfigSpec.ConfigValue<Boolean> spawnBudgetEnabled;
	public final ModConfigSpec.ConfigValue<Integer> spawnBudgetMin;
	public final ModConfigSpec.ConfigValue<Integer> spawnBudgetMax;
	public final ModConfigSpec.ConfigValue<Integer> spawnBudgetBelowMin;
	public final ModConfigSpec.ConfigValue<Integer> spawnBudgetBelowMax;
	public final ModConfigSpec.ConfigValue<Double> spawnBudgetRange;
	public final ModConfigSpec.ConfigValue<Integer> spawnBudgetInterval;
	public final ModConfigSpec.ConfigValue<Double> climateTemperatureWidth;
	public final ModConfigSpec.ConfigValue<Double> climateDownfallWidth;
	public final ModConfigSpec.ConfigValue<Boolean> ignoreWorldGenSpawning;
	public final ModConfigSpec.ConfigValue<Boolean> controlVanillaSpawns;

	public final ModConfigSpec.ConfigValue<String> globalDimensionList;
	public final ModConfigSpec.ConfigValue<Boolean> globalDimensionWhitelist;

	public final ModConfigSpec.ConfigValue<Boolean> disableDungeonSpawners;
	public final ModConfigSpec.ConfigValue<Double> dungeonSpawnerWeightScale;

	public ConfigCreatureSpawning(ModConfigSpec.Builder builder) {
		builder.push("Global Spawning");
		builder.comment("These settings are used by everything. It is recommended to leave them as they are however low end machines might benefit from a few tweaks here");

		typeSpawnLimit = builder.comment("The limit of how many mobs of the same type (peaceful or not peaceful) can spawn within the limit search range. For individual creature species limits, see the creature json configs.")
				.translation(CoreConfig.CONFIG_PREFIX + "spawning.typeSpawnLimit")
				.define("typeSpawnLimit", 64);
		spawnLimitRange = builder.comment("When spawned from a vanilla spawner, this is how far a mob should search from in blocks when checking how many of its kind have already spawned. Custom Spawners have it defined in their json file instead.")
				.translation(CoreConfig.CONFIG_PREFIX + "spawning.spawnLimitRange")
				.define("spawnLimitRange", 32D);
		disableAllSpawning = builder.comment("If true, all mobs from this mod will not spawn at all.")
				.translation(CoreConfig.CONFIG_PREFIX + "spawning.disableAllSpawning")
				.define("disableAllSpawning", false);
		enforceBlockCost = builder.comment("If true, mobs will double check if their required blocks are nearby, such as Cinders needing so many blocks of fire.")
				.translation(CoreConfig.CONFIG_PREFIX + "spawning.enforceBlockCost")
				.define("enforceBlockCost", true);
		spawnWeightScale = builder.comment("Scales the spawn weights of all mobs from this mod. For example, you can use this to quickly half the spawn rates of mobs from this mod compared to vanilla/other mod mobs by setting it to 0.5.")
				.translation(CoreConfig.CONFIG_PREFIX + "spawning.spawnWeightScale")
				.define("spawnWeightScale", 1.0D);
		spawnWeightCommon = builder.comment("S202: the spawn weight of every creature whose json sets spawnRarity to common.")
				.translation(CoreConfig.CONFIG_PREFIX + "spawning.spawnWeightCommon")
				.defineInRange("spawnWeightCommon", 8, 0, 1000);
		spawnWeightRare = builder.comment("S202: the spawn weight of every creature whose json sets spawnRarity to rare. Keep it at 65% of the common weight or higher.")
				.translation(CoreConfig.CONFIG_PREFIX + "spawning.spawnWeightRare")
				.defineInRange("spawnWeightRare", 6, 0, 1000);
		spawnBudgetEnabled = builder.comment("S202 spawn budget: natural spawning keeps a target number of wild creatures around each player (or group of nearby players) instead of firing every world spawner on its own timer. Trigger spawners, mob events, dungeons and bosses are separate.")
				.translation(CoreConfig.CONFIG_PREFIX + "spawning.spawnBudgetEnabled")
				.define("spawnBudgetEnabled", true);
		spawnBudgetMin = builder.comment("S202 spawn budget: natural spawning adds a group whenever fewer than this many naturally spawned creatures are on the surface (open sky, not in water) within the budget range.")
				.translation(CoreConfig.CONFIG_PREFIX + "spawning.spawnBudgetMin")
				.defineInRange("spawnBudgetMin", 6, 0, 200);
		spawnBudgetMax = builder.comment("S202 spawn budget: a new surface group is cut down so the surface count never goes above this.")
				.translation(CoreConfig.CONFIG_PREFIX + "spawning.spawnBudgetMax")
				.defineInRange("spawnBudgetMax", 10, 1, 200);
		spawnBudgetBelowMin = builder.comment("S202 spawn budget: the same for caves and water (everything that isn't open-air surface), counted separately so it never fills the surface's share.")
				.translation(CoreConfig.CONFIG_PREFIX + "spawning.spawnBudgetBelowMin")
				.defineInRange("spawnBudgetBelowMin", 3, 0, 200);
		spawnBudgetBelowMax = builder.comment("S202 spawn budget: a new cave/water group is cut down so that count never goes above this.")
				.translation(CoreConfig.CONFIG_PREFIX + "spawning.spawnBudgetBelowMax")
				.defineInRange("spawnBudgetBelowMax", 5, 1, 200);
		spawnBudgetRange = builder.comment("S202 spawn budget: the radius in blocks around players that the budget counts.")
				.translation(CoreConfig.CONFIG_PREFIX + "spawning.spawnBudgetRange")
				.defineInRange("spawnBudgetRange", 96D, 16D, 512D);
		spawnBudgetInterval = builder.comment("S202 spawn budget: ticks between budget checks while an area is under budget (20 = 1 second). Each check adds at most one group.")
				.translation(CoreConfig.CONFIG_PREFIX + "spawning.spawnBudgetInterval")
				.defineInRange("spawnBudgetInterval", 60, 20, 72000);
		climateTemperatureWidth = builder.comment("S202: every creature with a biome/climate restriction gets a temperature band of this width, centred on its original habitat, so no creature is far more widespread than another.")
				.translation(CoreConfig.CONFIG_PREFIX + "spawning.climateTemperatureWidth")
				.defineInRange("climateTemperatureWidth", 1.75D, 0.1D, 10D);
		climateDownfallWidth = builder.comment("S202: the downfall (humidity) band width given to every creature with a biome/climate restriction.")
				.translation(CoreConfig.CONFIG_PREFIX + "spawning.climateDownfallWidth")
				.defineInRange("climateDownfallWidth", 0.55D, 0.05D, 2D);
		useSurfaceLightLevel = builder.comment("If true, when water mobs spawn, instead of checking the light level of the block the mob is spawning at, the light level of the surface (if possible) is checked. This stops mobs like Jengus from spawning at the bottom of deep rivers during the day, set to false for the old way.")
				.translation(CoreConfig.CONFIG_PREFIX + "spawning.useSurfaceLightLevel")
				.define("useSurfaceLightLevel", true);
		ignoreWorldGenSpawning = builder.comment("If true, when new world chunks are generated, no mobs from this mod will pre-spawn (mobs will still attempt to spawn randomly afterwards). Set this to true if you are removing mobs from vanilla dimensions as the vanilla WorldGen spawning ignores mob spawn conditions.")
				.translation(CoreConfig.CONFIG_PREFIX + "spawning.ignoreWorldGenSpawning")
				.define("ignoreWorldGenSpawning", false);
		controlVanillaSpawns = builder.comment("If true, some vanilla spawns in various biomes will be removed, note that vanilla mobs should still be easy to find, only they will be more biome specific.")
				.translation(CoreConfig.CONFIG_PREFIX + "spawning.controlVanillaSpawns")
				.define("controlVanillaSpawns", false);

		globalDimensionList = builder.comment("A global comma separated list of dimension ids that overrides every other spawn setting in both the configs and json spawners. Use this to quickly stop all mobs from spawning in certain dimensions, etc.")
				.translation(CoreConfig.CONFIG_PREFIX + "spawning.globalDimensionList")
				.define("globalDimensionList", "");
		globalDimensionWhitelist = builder.comment("If set to true the global dimension list acts as a whitelist, otherwise it is a blacklist.")
				.translation(CoreConfig.CONFIG_PREFIX + "spawning.globalDimensionWhitelist")
				.define("globalDimensionWhitelist", false);

		disableDungeonSpawners = builder.comment("If true, newly generated dungeons wont create spawners with mobs from this mod.")
				.translation(CoreConfig.CONFIG_PREFIX + "spawning.disableDungeonSpawners")
				.define("disableDungeonSpawners", false);
		dungeonSpawnerWeightScale = builder.comment("Scales the weight of dungeons using spawners from this mod. For example, you can half the chances all dungeons having spawners with mobs from this mod in them by setting this to 0.5.")
				.translation(CoreConfig.CONFIG_PREFIX + "spawning.dungeonSpawnerWeightScale")
				.define("dungeonSpawnerWeightScale", 1.0D);

		builder.pop();
	}
}
