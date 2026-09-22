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

## Phase order

Ordered to match the mod's own bootstrap dependency graph (traced from
`LycanitesMobs.java`'s constructor / `loadContent()` / `commonSetup()` / `clientSetup()`).
Each phase depends on the ones above it being in place and registered.

- [x] **Phase 0 — Scaffold** (done 2026-09-22): NeoForge 1.21.1 MDK, `gradle.properties`
      / `neoforge.mods.toml` filled in with real Lycanites metadata (mod id kept as
      `lycanitesmobs` to stay compatible with Lycanites Kin, lycanoculus, and Lycanite's
      Companions), reference source trees pulled in, this plan.
- [ ] **Phase 1 — Core bootstrap**: mod entry class on NeoForge's constructor-injection
      lifecycle (no more `FMLJavaModLoadingContext`/`DistExecutor`/proxy split — those
      patterns are gone in NeoForge), empty `DeferredRegister`s for items/blocks/entity
      types, `CoreConfig` → `ModConfigSpec`. Goal: mod loads in a dev client with zero
      content and doesn't crash.
- [ ] **Phase 2 — Data-loading substrate**: `FileLoader`/`StreamLoader`, `ModInfo`,
      the `ObjectManager` base pattern every content manager extends. Every later phase
      depends on this being right — port it carefully.
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

Phase 0 complete. Nothing beyond the scaffold has been ported yet — `src/main/java` is
currently empty aside from what Phase 1 adds.
