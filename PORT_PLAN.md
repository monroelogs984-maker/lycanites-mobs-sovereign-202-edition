# Lycanites Mobs — NeoForge 1.21.1 Port Plan

## What this is

A from-scratch port of the official Lycanites Mobs source (Forge 1.20.1) to NeoForge 1.21.1,
for use as the core creature mod in Sovereign 202. Not a fork of EmeryTheModder's
"Lycanites Mobs Reloaded" — that port exists and runs, but per Glenn (2026-09-22) most
in-depth features past basic spawning/summoning are broken in it. It's kept as a
translation reference only (see `reference/`), not a starting point.

## Scale (measured 2026-09-22 from the official source)

- 135,508 lines of Java, 855 files
- 123 creature classes, 167 creature JSON configs, 190 custom OBJ models, 1,658 textures
- Custom in-house OBJ model/animation/rendering pipeline (not GeckoLib)
- Only 4 mixins (ReloadCommandMixin, LightTextureMixin, EntityTypeMixin,
  CreativeModeTabRegistryMixin) — small surface, low risk relative to the rest

This is not a weekend job. Treat each phase below as its own multi-session effort,
verified in-game before moving on — same discipline as BHB's "one weapon at a time" rule.

## Reference material (`reference/`, gitignored — not part of this repo)

- `reference/official-forge-1.20.1-dev/` — full snapshot of the official source
  (gitlab.com/Lycanite/LycanitesMobs, branch `1.20.1-dev`, as of 2026-09-22). This is
  the actual porting source.
- `reference/emery-neoforge-1.21.1-port/` — EmeryTheModder's existing NeoForge 1.21.1
  port (github.com/feetnuggets/lycanites-1.20.1, branch `1.21.1`). Useful to see how he
  mechanically resolved a given Forge→NeoForge API call, but do not trust its behavior —
  cross-check anything copied from it against the official source's intent, since it's
  the thing we're trying to do better.

Both are point-in-time snapshots (not live clones) — re-fetch if the upstream moves on.

## License

Inherited from the original per workspace convention (see CLAUDE.md licensing section).
The official repo's own `gradle.properties` labels it "GNU GPL" but the actual `LICENSE`
file content is the custom "Lycanite Mob Public License" (modification and derivation
explicitly permitted) — that's the file that's authoritative, and it's what's declared
in `gradle.properties` here.

## Testing

Dedicated CurseForge instance **Lycannots** (NeoForge 21.1.251, MC 1.21.1) —
`~/Documents/curseforge/minecraft/Instances/Lycannots/` — kept separate from the S202
instance so this WIP port doesn't touch the main modpack while it's unstable.
`./gradlew deploy` builds and copies the jar there. `./gradlew build` alone does not.

## Phase order

Ordered to match the mod's own bootstrap dependency graph (traced from
`LycanitesMobs.java`'s constructor / `loadContent()` / `commonSetup()` / `clientSetup()`).
Each phase depends on the ones above it being in place and registered.

- [x] **Phase 0 — Scaffold** (done 2026-09-22): NeoForge 1.21.1 MDK, `gradle.properties`
      / `neoforge.mods.toml` filled in with real Lycanites metadata (mod id kept as
      `lycanitesmobs` to stay compatible with Lycanites Kin, lycanoculus, and Lycanite's
      Companions), reference source trees pulled in, this plan.
- [x] **Phase 1 — Core bootstrap** (done 2026-09-22): mod entry class (`LycanitesMobs.java`)
      on NeoForge's constructor-injection lifecycle (no more
      `FMLJavaModLoadingContext`/`DistExecutor`/proxy split — those patterns are gone in
      NeoForge), empty `DeferredRegister`s for items/blocks/entity types, `CoreConfig` →
      `ModConfigSpec`. **Verified with `./gradlew runServer`** — mod loads cleanly, server
      reaches "Done". `LycanitesMobsClient` (Phase 8) not started yet.
- [x] **Phase 2 — Data-loading substrate** (done 2026-09-22): `FileLoader`/`StreamLoader`,
      `ModInfo`, `LMHelperClass`, `AssetHelper`. `ObjectManager` deliberately deferred to
      Phase 4 — it pulls in `EffectBase` (Phase 3), containers (Phase 6/8), and
      `LMItemsGroup` (Phase 4), none of which exist yet; porting it now would mean stubbing
      three future phases just to make it compile.
      **Real bugs found and fixed by actually running it, not just compiling:**
      - `FileLoader` needs marker files at `assets/lycanitesmobs/.root`,
        `data/lycanitesmobs/.root`, `common/lycanitesmobs/.root` (empty files) to locate its
        classpath roots — without them, mod construction throws NPE and the server won't start.
      - `LMHelperClass.fixMaxHealth()`'s reflection hack broke silently: `Attributes.MAX_HEALTH`
        changed from a direct `RangedAttribute` to a `Holder<Attribute>` as of 1.21 — must
        unwrap via `.value()` before reflecting into it, or it throws (caught, non-fatal, but
        the max-health-uncap feature just didn't work).
      - `ChunkStatus` moved package: `net.minecraft.world.level.chunk` →
        `net.minecraft.world.level.chunk.status`.
      - Confirmed ground truth (not memory) for the full NeoForge/1.21.1 API surface this phase
        touched, by grepping the actual dependency jars and NeoGradle's decompiled sources in
        `build/neoForm/.../transformSource/transformed/`: `ResourceLocation` constructors are
        private now (use `.fromNamespaceAndPath()` / `.parse()`); `ForgeRegistries` doesn't
        exist in NeoForge — use `BuiltInRegistries.<SINGULAR_NAME>` for static registries
        (field names don't all match the old plural Forge names, e.g. `MENU` not `MENU_TYPES`,
        `CARVER` not `WORLD_CARVERS`, `BLOCKSTATE_PROVIDER_TYPE` not
        `BLOCK_STATE_PROVIDER_TYPES`); `Enchantment`/`PaintingVariant` became dynamic
        (datapack) registries with no static lookup, need `RegistryAccess`;
        `MinecraftForge`→`NeoForge`, `ForgeConfigSpec`→`ModConfigSpec` (API-identical, safe
        mechanical rename), `IPlantable` and `BiomeManager` were removed with no replacement,
        `DistExecutor` was removed (modern pattern: a separate `@Mod(dist = Dist.CLIENT)`
        class, not a runtime dist check).
- [ ] **Phase 3 — Elements, Effects, Fluids**: `ElementManager`, `EffectManager`,
      `FluidManager`. Small, mostly self-contained — good warm-up before the DataComponents
      fight in Phase 4.
- [ ] **Phase 4 — Items & Blocks**: `ItemManager`, `EquipmentPartManager`. This is where
      the 1.20.5+ DataComponents rewrite hits hardest (item NBT model changed fundamentally
      between 1.20.1 and 1.21.1) — expect this to be its own sub-project, not a quick pass.
- [ ] **Phase 5 — Creatures**: `CreatureManager` and the 123 creature classes / 167 JSON
      configs. The big one. Break this into batches (by creature family or tier), build +
      deploy + visually verify each batch in S202 before moving to the next — do not
      attempt all 123 at once.
- [ ] **Phase 6 — Gameplay systems on top of creatures**: `ProjectileManager`,
      `SpawnerManager`, `StructureSpawnInjector`, `AltarInfo`, `MobEventManager`,
      `DungeonManager`.
- [ ] **Phase 7 — World gen**: custom biome features/structures, the virtual dungeon
      datapack (`DungeonVirtualPack`, `WorldgenJsonDumper`).
- [ ] **Phase 8 — Client rendering**: custom OBJ model loader/renderer, animation system,
      block render types, the 5 custom GUIs (Creature Inventory, Summoning Pedestal,
      Equipment Forge/Infuser/Station).
- [ ] **Phase 9 — Compat + mixins**: Oculus/Iris shader compat (matters — S202 ships Iris
      + a custom shader edition), the 4 mixins. NeoForge 1.21 mixins run against Mojang
      mappings directly, no SRG remap step — simpler than the 1.12.2 mixin story in this
      workspace's CLAUDE.md.
- [ ] **Phase 10 — S202 tuning**: once it's a working straight port, apply the actual
      requested tweaks to fit S202 as the core creature mod.

## Status

Phases 0–2 complete and verified end-to-end (`./gradlew runServer` loads clean, no errors).
Config subsystem is 11/13 files ported — `ConfigCreatures` and `ConfigCreatureSubspecies`
are deferred to Phase 5 (they depend on `Variant`/`CreatureStats`/`CreatureManager`).
`ObjectManager` deferred to Phase 4. Next up: Phase 3 (Elements, Effects, Fluids).
