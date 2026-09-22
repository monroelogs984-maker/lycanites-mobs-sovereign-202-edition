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
- [x] **Phase 3 — Elements** (done 2026-09-22): `ElementManager`, `ElementInfo`, `EffectBase`,
      plus the `JSONLoader`/`JSONHelper` JSON-loading substrate they (and every later
      JSON-driven manager) sit on — that substrate turned out to belong here, not Phase 2,
      once actually read. All 28 element JSON definitions ported and loading (verified via
      `./gradlew runServer` — 0 errors, server reaches "Done").
      **Correction to the original plan:** `EffectManager` and `FluidManager` are NOT
      self-contained the way this phase assumed before the files were actually read — both
      call into `ObjectManager` (`addPotionEffect`/`addSound`/`addFluid`/`addItem`/`addBlock`),
      which is gated on Phase 4. They move to Phase 4, not this one.
      **Real bugs found by running it, not just compiling:** `Registry<T>.getValue()` doesn't
      exist in 1.21.1 (renamed to plain `.get()`, or `.getOptional()` when you need a true
      "not found" signal instead of a `DefaultedRegistry` falling back to its default value);
      `MobEffectInstance` effect params are now `Holder<MobEffect>`, not `MobEffect` directly,
      matching the same Holder-wrapping trend as `Attributes.MAX_HEALTH` in Phase 2;
      `LivingEntity.setSecondsOnFire(int)` → `igniteForSeconds(float)`; `Biome` is a fully
      dynamic (datapack) registry with no static access at all as of 1.21, so
      `JSONHelper.getBiomes(List<String>)` (no registry-access context) has no valid
      replacement and was dropped rather than ported wrong - flagged for whoever needs it in
      Phase 7 to add back with a `RegistryAccess`/`Level` parameter.
- [~] **Phase 4 — Items & Blocks + Effects/Fluids registration** (IN PROGRESS, batched like
      Phase 5's creatures will be — this phase alone touches ~60 item classes + ~55 block
      classes referenced from `ItemManager`/`BlockManager`, confirmed too big for one pass):
    - [x] **Phase 4a** (done 2026-09-22): substrate + proof-of-pipeline. Ported
          `ObjectManager` (containers/menus dropped, they're Phase 6/8), `Material`,
          `ItemConfig`, `ItemInfo`, `ObjectLists` (`addEntity` dropped, needs Phase 5's
          `CreatureManager`), `BaseItem`/`GenericItem` (reworked for DataComponents - see
          below), a trimmed `LMItemsGroup` (items tab only), a trimmed `ItemManager`
          (items tab + one real item, the rest of its ~40 hardcoded items deferred), and
          `ItemMobToken` as the first real, working item. **Verified with
          `./gradlew runServer` and a jar content check** — server loads clean through to
          "Done", `ItemMobToken.class` is actually in the deployed jar.
          **Two real bugs found by running it:**
          - **DataComponents hits the item base class itself, not just leaf items:**
            `BaseItem`'s `getTagCompound()`/`hasTag()`/`getTag()` NBT helper needed a full
            rework — item NBT moved to `DataComponents.CUSTOM_DATA` holding a `CustomData`
            wrapper. `ItemStack.canPerformAction()` still exists but takes `ItemAbility` now
            (`net.minecraftforge.common.ToolActions` → `net.neoforged.neoforge.common.ItemAbilities`).
            `Item.appendHoverText()`'s second param changed from `Level` to
            `Item.TooltipContext`. `FoodProperties.Builder`: `saturationMod`→`saturationModifier`,
            `alwaysEat`→`alwaysEdible`, `meat()` removed outright (no replacement),
            `effect(MobEffectInstance, float)` deprecated in favor of a `Supplier` overload.
          - **Registration timing, not an API rename — this one actually crashed the
            server:** content registration (`ObjectManager.addItem`/`addBlock`/etc, which
            call into the `DeferredRegister`s) MUST happen synchronously during mod
            construction, not in an `FMLCommonSetupEvent` listener — NeoForge throws
            `IllegalStateException: Cannot register new entries to DeferredRegister after
            RegisterEvent has been fired` if you get this wrong. The original Forge code
            called this from `loadContent()`, invoked directly at the end of the
            constructor; I'd initially wired the Phase 3/4a content loading into
            `commonSetup()` instead (matching where *later*, non-registration setup like
            `Material.init()` correctly belongs) and NeoForge's stricter check caught it
            immediately. Fixed by adding a `loadContent()` method back, called from the
            constructor, matching the original's structure — `LycanitesMobs.java` now has a
            code comment explaining this so it doesn't happen again in a later phase.
    - [x] **Phase 4b** (done 2026-09-22): the "lush" dungeon building block set, proving
          `BlockManager`'s registration path. Ported `BlockTypeGetter`, `BlockBase`,
          `BlockStairsCustom`/`BlockSlabCustom`/`BlockFenceCustom`/`BlockWallCustom`/
          `BlockPillar`, `BlockManager`, `LMBlocksGroup` (blocks tab), plus the two
          genuinely-standalone items found while re-checking Phase 4a's original item list
          (`ItemImmunizer`, `ItemCleansingCrystal` — only needed `ObjectManager`, already
          ported). **Note on scope:** re-checked the rest of the original ~40 hardcoded items
          (`ItemSoulgazer`, `ItemSoulContract`, `ItemSoulstone`, `ItemSoulkey`, the holiday
          items) before starting this batch — every one of them pulls in `ExtendedPlayer`
          (capability system), `CreatureManager`/`BaseCreatureEntity`/`PetEntry`, or
          `AltarInfo`, none of which exist before Phase 5/6. So the "~40 items" remaining
          isn't really Phase 4 work at all — it's Phase 5/6 work that happens to live in
          `ItemManager.loadItems()`. Blocks were different: `BlockBase` and the 5 building
          block classes only needed `BlockTypeGetter`/`LMBlocksGroup`, both self-contained.
          **Verified with `./gradlew runServer` + jar content check** — clean load, all 7
          new classes confirmed present in the deployed jar.
          **API changes found, none required a running check this time (compiler caught
          them all) but are worth recording:** `Block.appendHoverText()`'s second param
          changed from `BlockGetter` to `Item.TooltipContext` — same change as `Item`'s
          version in Phase 4a, confirmed by checking NeoForge's own patched vanilla source
          rather than guessing it'd match. `net.minecraftforge.api.distmarker.{Dist,OnlyIn}`
          → `net.neoforged.api.distmarker.{Dist,OnlyIn}` (same class names, NeoForge just
          didn't rename this particular package away from `net.neoforged.api.*`, so it's
          slightly inconsistent with `common`/`registries`/`fml` all moving to
          `net.neoforged.neoforge.*` or `net.neoforged.fml.*` — confirmed by checking actual
          usage in NeoForge's patched vanilla source rather than assuming a pattern held).
    - [ ] **Phase 4c+**: revisit once Phase 5 (Creatures) and Phase 6 (Altars, gameplay
          systems) exist — that's what actually unblocks `ItemSoulgazer`/`ItemSoulstone`/
          `ItemSoulkey`/the summoning staves/the holiday items. Still fully Phase-4-only and
          unblocked right now: the other 6 dungeon stone sets via `BlockManager`
          (`desert`/`shadow`/`demon`/`aberrant`/`ashen`/`stream`), the fire/cloud/web effect
          blocks (`block.fire.*`/`block.cloud.*`/`block.web.*`, ~721 lines, need checking for
          real dependencies), `JSONHelper.getJsonMaterials()` (needs `Material`, already
          ported). `EquipmentPartManager`/`ItemEquipmentPart`, `EffectManager`, `FluidManager`,
          the equipment/pedestal blocks, and `LMEquipmentPartsGroup`/`LMChargesGroup`/
          `LMBestEquipmentGroup`/`LMCreaturesGroup` are still blocked on Phase 5/6 too.
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

Phases 0–3 complete, Phase 4a+4b complete — all verified end-to-end (`./gradlew runServer`
loads clean, no errors). 3 real items (`mobtoken`, `immunizer`, `cleansingcrystal`) and a
full dungeon building block set (`lushstone` + 13 variants: stairs/slabs/bricks/tiles/
fence/wall/pillar/crystal) register successfully. Config subsystem is 11/13 files ported —
`ConfigCreatures` and `ConfigCreatureSubspecies` are deferred to Phase 5 (they depend on
`Variant`/`CreatureStats`/`CreatureManager`).

Real conclusion from Phase 4b: most of the *item* side of "Phase 4" was never actually
Phase-4-shaped work - it's Phase 5/6 (creatures, capabilities, altars) wearing an
`ItemManager.loadItems()` costume. The *block* side (dungeon stone sets, effect blocks) is
genuinely self-contained Phase 4 work and there's more of it available right now (Phase 4c).
Next up: Phase 4c (more self-contained blocks) or jump to Phase 5 (Creatures) to unblock
the rest of the items - Glenn's call.

## CI

GitHub Actions (`.github/workflows/build.yml`, came bundled with the NeoForge MDK template)
runs `./gradlew build` on every push. Caught a real mistake early: `gradle.properties` briefly
had `org.gradle.java.home=/usr/lib/jvm/java-21-openjdk` committed to it (this machine's local
JDK path) - broke CI immediately since GitHub's runners don't have that path. Fixed by
removing it; rely on `JAVA_HOME` from the environment instead (CI's `setup-java` action sets
it correctly; locally, pass `JAVA_HOME=/usr/lib/jvm/java-21-openjdk` explicitly per command,
same as this plan's other `./gradlew` examples do). Never commit a machine-local
`org.gradle.java.home` to this repo again.
