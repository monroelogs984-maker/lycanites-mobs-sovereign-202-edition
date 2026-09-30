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
    - [x] **Phase 4c** (done 2026-09-22): the other 6 dungeon stone sets
          (`desert`/`shadow`/`demon`/`aberrant`/`ashen`/`stream` - zero new files, just more
          `BlockManager.addDungeonBlocks()` calls) and the fire/cloud/web effect blocks:
          `BlockFireBase` (384 lines, the shared tick/spread/ignite logic all 7 fire blocks
          sit on), `LycanitesBlockTags`, `BlockFrostfire`/`BlockIcefire`/`BlockHellfire`/
          `BlockDoomfire`/`BlockPrimefire`/`BlockScorchfire`/`BlockSmitefire`,
          `BlockFrostCloud`/`BlockPoisonCloud`/`BlockPoopCloud`,
          `BlockFrostweb`/`BlockQuickWeb`. `BlockShadowfire` excluded - needs
          `BaseCreatureEntity` (Phase 5). Also restored `ItemManager.cutoutBlocks`/
          `registerCutoutBlock()`, dropped during Phase 4a's trim - every one of these blocks
          calls it in its constructor. **Verified with `./gradlew runServer` + jar content
          check** — clean load, all new classes present in the deployed jar.
          **This batch compiled clean on the first try** (no `runServer`-only bugs this time)
          because every risky vanilla API call was checked against NeoGradle's decompiled
          source *before* writing the port, not after hitting a compiler error: confirmed
          `Level.getBiome()` returns `Holder<Biome>` (needs `.value()`), that
          `isFireSource`/`isFlammable`/`getFlammability`/`getIgniteOdds` are deprecated but
          still present and callable (not removed), `TntBlock.explode()`,
          `FireBlock.AGE`/`Blocks.FIRE`, `LivingEntity.canBeAffected()`,
          `DamageSources.magic()`/`.inFire()`, `Entity.makeStuckInBlock()`, and
          `DustParticleOptions.REDSTONE` are all unchanged. Worth the extra verification
          time given `BlockFireBase` alone is 384 lines of tick-loop/fire-spread logic where
          a subtly wrong port (not a compile error) would be much harder to catch than a
          missing method.
- [~] **Phase 5 — Creatures** (IN PROGRESS, measured 2026-09-22 — categorically bigger than
      Phase 4, not just "more of the same"): the substrate needed before even ONE creature
      can spawn is ~23,000 lines, roughly 6x everything ported in Phases 0-4 combined:
        - `BaseCreatureEntity` (7,167 lines, one file) - the shared base every creature
          extends. 25 major sections (data sync, spawning, stats, movement, attacks, death,
          AI behaviour/targets, battle phases, taming, abilities, equipment, immunities, NBT,
          client/visuals/sounds, ...).
        - `TameableCreatureEntity` (1,494 lines) - turns out nearly every creature extends
          *this*, not `BaseCreatureEntity` directly, confirmed by checking 7 of the smallest
          creature classes and finding zero exceptions.
        - `core/entity/goals/` (8,446 lines, 64 files) - custom AI goal classes
          (`AttackMeleeGoal`, `FindAttackTargetGoal`, etc). Every creature's `registerGoals()`
          pulls from here.
        - `core/data/info/creature/` (3,941 lines, 11 files) - `CreatureInfo`,
          `CreatureType`, `CreatureGroup`, `CreatureConfig`, `CreatureSpawnConfig`,
          `Subspecies`, etc - the JSON-driven definition/config layer, analogous to
          `ItemInfo`/`ItemConfig` but far larger.
        - `CreatureManager` + 3 small helper classes (~780 lines) - registration/bootstrap,
          analogous to `ItemManager`.
        - `entity/util` (`CreatureStats`, ~1,000 lines).
      Sub-phase plan (lowest API-risk / most tractable first, since `BaseCreatureEntity`
      itself is AI/combat/rendering-heavy and best tackled once everything it depends on
      already exists and compiles):
        - [x] **5a**: `entity/util` (`CreatureStats`, `Targeting`) + `core/data/info/creature/`
              substrate - done 2026-09-22.
        - [x] **5b**: `CreatureManager` + bootstrap/registry/validation helpers, wired into
              `LycanitesMobs.loadContent()`/`commonSetup()` - done 2026-09-22. Two real
              registration-timing bugs found and fixed (see Status below): entity types
              needed the same `RegisterEvent`-forcing fix blocks got in Phase 4c, and
              `CreatureManager.loadConfig()` can't run in the constructor.
        - [x] **5c**: `core/entity/goals/` - done 2026-09-22, but trimmed hard: only 13 of 64
              files ported (`BaseGoal`, `GoalConditions`, `TargetSorterNearest`,
              `TargetingGoal`, `FindAttackTargetGoal`, `FindAvoidTargetGoal`, `RevengeGoal`,
              `AvoidIfHitGoal`, `RandomPositionGenerator`, `WanderGoal`, `LookIdleGoal`,
              `MoveRestrictionGoal`, `AttackMeleeGoal`). Group/pack goals (`FindGroupAttack/
              AvoidTargetGoal`, `FollowMasterGoal`), water/tempt/fuse goals (`PaddleGoal`,
              `StayByWaterGoal`, `AvoidGoal`, `TemptGoal`, `Find/FollowFuseGoal`), and
              `WatchClosestGoal` are NOT ported - `BaseCreatureEntity.registerGoals()` only
              wires up avoid-if-hit + revenge + wander + look-idle. All pure Java/vanilla-API,
              no Forge-specific calls needed translating.
        - [x] **5d**: `BaseCreatureEntity` - done 2026-09-22, but this is an aggressively
              trimmed port, not a faithful one - see "Phase 5d/5f status" below for exactly
              what was dropped and why. Also needed a full pass of 1.21.1 API changes beyond
              anything seen in Phases 0-4c (see below) - this file is where they all surfaced.
        - [x] **5e (taming core, done 2026-09-26 - see "Phase 5e" in Status)**: real
              `TameableCreatureEntity`; `RideableCreatureEntity` is still a stub (now on top of the
              real tameable class). Original note, kept for history: `TameableCreatureEntity`/`AgeableCreatureEntity`/`RideableCreatureEntity`
              exist ONLY as empty stub classes (just a constructor) so `CreatureInfo`'s
              `Class.isAssignableFrom()` checks compile - no taming/aging/riding behaviour is
              ported. ~69 of 123 creatures extend `TameableCreatureEntity` and will need the
              real thing eventually; deliberately deferred since it's another ~1,500+2,015
              lines and the goal was proving one creature spawns, not taming.
        - [x] **5f**: first real creature - `EntityCalpod`, chosen specifically because it's
              one of only 9 creatures extending `BaseCreatureEntity` directly (bypasses the
              TameableCreatureEntity stub entirely). Compiles, registers, mod loads clean via
              `runServer` with no "has no attributes" or Lazy-registration errors. NOT YET
              spawn-tested in Lycannots (client-side model/renderer for it doesn't exist -
              expect either an invisible/crashing entity, or a genuine spawn - unverified).
              Trimmed from the original: swarm minion-spawning, block-griefing-on-attack, bag
              equipment.
        - [ ] **5g+**: batch the remaining creatures. Given 5e is stubbed, the next tractable
              batch is more creatures that extend `BaseCreatureEntity` directly (8 left:
              `EntityFear`, `EntityAsmodeus`, `EntityAmalgalich`, `EntityRahovart`,
              `EntityTreant`, `EntityGorgomite`, `EntityCherufe`, `EntityWendigo` - though the
              last 3 in that list are bosses/large, so probably skip those first). Porting
              `TameableCreatureEntity` for real is its own sub-phase whenever taming actually
              matters. Same "one weapon at a time" discipline as BHB - never batch for speed.
      Do not attempt all 123 creatures, or even the full substrate, in one sitting.

      **Phase 5d/5f status - what was actually dropped from `BaseCreatureEntity` (2026-09-22):**
      Ported the constructor, `defineSynchedData`, `applyDynamicAttributes`/`refreshAttributes`,
      `registerGoals` (trimmed set above), `setupMob`/`loadItemDrops`, naming
      (`getFullName`/species/variant/subspecies/level name parts), all target accessors
      (master/parent/avoid/fixate/perch/rider + `TARGET_BITS`), level/experience, melee attack
      chain (`attackMelee`→`attackEntityAsMob`, pierce damage kept, enchant-knockback/fire-
      aspect/shield-interrupt dropped as dead code since `canInteruptShields()` is
      hard-`false`), pack-check (`isInPack`/`countAllies`), home/restriction (now delegates to
      vanilla `Mob.restrictTo`/`getRestrictCenter`/`getRestrictRadius` - see below), sounds
      (all of them - step/hurt/death/ambient/attack/jump/fly/swim), environmental immunity
      flags (`canBurn`/`waterDamage`/`canBreatheAir`/`canBreatheUnderwater`/
      `canBreatheUnderlava`/`lavaContact`), NBT persistence (progression fields only). A
      drastically simplified `aiStep()` (blocking-state + target-runtime ticking + tick
      counter only) replaces the original's ~15-subsystem update loop.

      **Dropped entirely** (deferred to later phases, not stubbed): capabilities
      (`ExtendedEntity`/`ExtendedPlayer`/`ExtendedWorld`), networking sync (`MessageCreature`/
      `queueSync`/`doSync` are no-ops), containers/inventory (`CreatureInventory`/
      `CreatureContainer` - no per-creature inventory exists), pets (`PetEntry`), summoning
      pedestals, equipment parts, projectiles, creature relationships/taming reputation
      (`getRelationshipEntry` always returns null), minions, boss health bar UI (`isBoss()`
      still affects damage scaling/sound volume, just no visible bar), battle-phase
      transform/fusion, elemental immunity checks (`hasElement`/`getElements` never ported, so
      `canBurn`/`canFreeze` only check the `ExtraMobBehaviour` override now), custom navigation
      - **`CreatureMoveController`/`CreaturePathNavigator`/`CreatureNodeProcessor` are NOT
      ported**; vanilla `GroundPathNavigation`/default `MoveControl` are used instead (no
      override of `createNavigation()`/`createMoveController()` at all), so there's no custom
      swim-bob/fly/climb pathing yet - only plain vanilla ground pathfinding. `DirectNavigator`
      exists (ported, self-contained) but is fully inert since `useDirectNavigator()` is
      hard-`false` - no flying "ghost" creature will work until real flight navigation is
      ported. Most of the natural-spawn eligibility chain (`checkSpawnVanilla`/
      `environmentSpawnCheck`/light-level/biome/group-limit checks) - `checkSpawnRules()`
      always returns `true` now, so natural spawning isn't gated the way it should be; fine
      for `/summon` or spawn-egg testing, not fine for real gameplay balance yet.

      **1.21.1 vanilla/NeoForge API changes found in this pass** (none of these showed up in
      Phases 0-4c, which never touched `LivingEntity`/`Mob` this deeply):
        - `Entity.onAddedToWorld()` → `onAddedToLevel()`.
        - `defineSynchedData()` takes a `SynchedEntityData.Builder` parameter now; use
          `builder.define(...)`, not `this.getEntityData().define(...)`.
        - `LivingEntity.getDimensions(Pose)` is now `final`; override
          `getDefaultDimensions(Pose)` instead. `EntityDimensions` is a record with a private
          constructor - use `EntityDimensions.scalable(w, h)`/`.fixed(w, h)`, and its `width`
          field needs `.width()` (record accessor), not `.width`.
        - `LivingEntity.getExperienceReward()` no longer exists to override - replaced by a
          `final getExperienceReward(ServerLevel, Entity)` routed through
          `EventHooks.getExperienceDrop`. Kept the scaling logic as a plain (non-override)
          helper for later use.
        - `Mob.finalizeSpawn(...)` dropped its `CompoundTag` parameter (4 params now, not 5).
        - `Entity.canChangeDimensions()` now takes `(Level oldLevel, Level newLevel)`.
        - Leashing was refactored into a `Leashable` interface that `Mob` implements directly
          - `tickLeash()`/`canBeLeashed(Player)` no longer exist to override (`canBeLeashed()`
          is now no-arg). Dropped the custom leash-restriction AI entirely; vanilla `Leashable`
          behaviour is used unmodified.
        - `Mob` now has its OWN `restrictCenter`/`restrictRadius`/`restrictTo()`/
          `getRestrictCenter()`/`getRestrictRadius()`/`hasRestriction()` - this is the exact
          system the 1.20.1 port's custom `homePosition`/`homeDistanceMax` fields duplicated,
          so those were deleted in favor of the vanilla ones (kept a `getHomeDistanceMax()`
          compat wrapper since the ported goal classes call it).
        - `BlockPathTypes` renamed to `PathType`.
        - `Entity.setMaxUpStep(float)` is gone - step height is now the `Attributes.STEP_HEIGHT`
          attribute (already included in `PathfinderMob.createMobAttributes()`'s base
          builder).
        - `LivingEntity.canBreatheUnderwater()` is now `final` and tag-driven
          (`EntityTypeTags.CAN_BREATHE_UNDER_WATER`, deprecated in favor of NeoForge's
          `canDrownInFluidType`) - renamed the custom override to
          `creatureCanBreatheUnderwater()` to avoid the collision.
        - Custom `Attribute` constants (`DEFENSE`, `RANGED_SPEED`) can no longer be bare
          `new RangedAttribute(...)` fields - `AttributeSupplier.Builder.add()`/
          `LivingEntity.getAttribute()` now take `Holder<Attribute>`. Added
          `core/manager/ModAttributes.java`, a proper `DeferredRegister<Attribute>`
          (`Registries.ATTRIBUTE`), registered on the mod event bus.
        - Missing `EntityAttributeCreationEvent` handler: NeoForge requires every `EntityType`
          extending `LivingEntity` to have an `AttributeSupplier` attached via this event
          (mod bus, fires after `RegisterEvent`/before common setup) or it throws "Entity ...
          has no attributes" the moment anything touches its attribute map. Added
          `RegistryEvents.registerEntityAttributes()`, mirroring the original's Forge-side
          `EntityAttributeCreationEvent` listener but iterating `BuiltInRegistries.ENTITY_TYPE`
          instead of `ForgeRegistries.ENTITY_TYPES`.
- [x] **Phase 6 — Gameplay systems on top of creatures**: `ProjectileManager`,
      `SpawnerManager`, `StructureSpawnInjector`, `AltarInfo` (redesigned, 6d), `MobEventManager`,
      `DungeonManager` (done with Phase 7, 2026-09-30).
- [x] **Phase 7 — World gen** (done 2026-09-30, see "Phase 7" in Status): dungeon structures + the
      virtual dungeon datapack (`DungeonVirtualPack`), fluid pools. `WorldgenJsonDumper` (dev tool) and the
      `chunkspawn` feature (superseded by 6b's `ChunkEvent.Load` hook) deliberately not ported.
- [ ] **Phase 8 — Client rendering**: custom OBJ model loader/renderer, animation system,
      block render types, the 5 custom GUIs (Creature Inventory, Summoning Pedestal,
      Equipment Forge/Infuser/Station).
- [x] **Phase 9 — Compat + mixins** (done 2026-09-30, see "Phase 9" in Status): Oculus/Iris shader compat (matters — S202 ships Iris
      + a custom shader edition), the 4 mixins. NeoForge 1.21 mixins run against Mojang
      mappings directly, no SRG remap step — simpler than the 1.12.2 mixin story in this
      workspace's CLAUDE.md.
- [ ] **Phase 10 — S202 tuning**: once it's a working straight port, apply the actual
      requested tweaks to fit S202 as the core creature mod.

## S202 redesign decisions (from `LYCANITES-REVIEW.txt`, 2026-09-26)

Glenn's review of the official mod. `LYCANITES-REVIEW.txt` (repo root) is the source of truth; this is the
port-facing digest. Overall target: cut ~1/3 of the mod. **Cuts = skip while porting; tuning = data, can wait
for Phase 10; new systems = own phases (below).**

**Cut (don't port):**
- ~20% of creatures. **Cut list so far (Glenn, 2026-09-30; not removed yet, cut later):** geken (reptile),
  triffid (plant), eyewig (insect), malwrath (demon), feradon + brucha (beast), abtu (aquatic), **cryptkeeper
  (undead), tpumpkyn (plant), dawon (beast) - final list of 10 (Glenn, 2026-09-30: keeping most creatures suits S202's
  "mystic creatures" identity)**. Trite and grigori
  were considered and **kept** (Asmodeus's fight summons them). Knock-ons to handle when cutting: dungeon mob lists
  (lushtomb: geken, eyewig; desertcrypts: triffid; demonictemple: malwrath; streamshrine: abtu), their mob event
  spawners, the fishing spawner (abtu), equipment parts (eyewig eye, malwrath eye, brucha quill; equipment is out of
  the first release anyway).
- **Even out creature group sizes** mainly by *moving* creatures between types rather than cutting: some elementals
  Glenn wants to keep move to other types (incl. the empty angel/slime), some beasts etc. too. Which ones: TBD by
  Glenn. Sizes now (121): elemental 21, beast 16, insect 12, aquatic 10, aberration 9, demon 7, undead 7, dragon 6,
  reptile 5, imp 5, plant 5, avian 4, worm 4, amphibian 3, anthronian 3, arachnid 3, golem 1 (some counts include
  linked entries: concapedesegment, makaalpha, joustealpha, vespidqueen).
- Creature levels (`levelPerDay`, level multipliers etc.) — Power Scale covers this in S202.
- Breeding (farming itself stays possible; food stays).
- Boss-channel *random* events and all holiday events (halloween, rudolph, satanclaws, poopparty).
- The 8 rare-variant altars (Royal Apollyon, Crimson Epion, Ebon Malwrath, Mottle Abaia, Phosphorescent
  Chupacabra, Lunar Grue, Celestial Geonach, Umber Cherufe) and altar block formations.
- Elements down to ~12 or fewer (fusion stays).
- Many trigger spawners (keep block-type ones like lava/ore; exact keep list TBD — flag `sleep` as a lottery death).

**Tuning (data):**
- Spawning: biome conditions loosened/cut, dimension conditions kept. Two global spawn-rate values, common and
  rare, rare >= 65% of common ("Mythic Beasts": see a few of everything). Packs preserved. Common/rare
  assignment TBD.
- Variants: base 80%, each rare variant 2%, uncommon variants share the rest (2 uncommon only -> 80/10/10;
  2 uncommon + 1 rare -> 80/9/9/2). 2-3 variants per mob. Open: astaroth/trite/kathoga have 2 subspecies
  (forms) x 2 colors — how to treat.
- Creature types: keep all, rebalance counts; give the currently empty `angel` and `slime` types a few mobs.
- Dungeons: rarer and smaller. Fluids: kept (key to spawning/attacks). Food: kept.
- Beastiary: rewritten to match, credit to Lycanite kept.

**New systems (new phases):**
- **Altars:** only Rahovart/Asmodeus/Amalgalich, summoned with that boss's own soulkey (one key per boss) on
  the pedestal, no block structure. Boss arenas are still built (the boss events' `StructureBuilder`s).
- **Mob events:** ~10 total, more varied spawns, more common. ~3 `world` channel (global, one at a time,
  may set rain/thunder/night via `WorldMobEventEffect`), rest `player` channel (area events, unused in the
  official data). Rarity half time-based (`MobEventSchedule`: worldDay/dayTime/dimension), half RNG
  (`RandomMobEventTrigger`) — could align with 202Tweeks' lunar cycle.
- **Equipment sockets — NOT in the first release (Glenn, 2026-09-26).** Ship the port without the equipment
  system; parts can exist as materials meanwhile. Planned later: instead of assembled Lycanites weapons, any damaging tool (any mod) gets an imprinted
  passive from an infused equipment part, managed and leveled at the Equipment Forge.

## Status

**2026-09-22 (later same day): Phase 5 substrate + first creature compiles, registers, and
loads clean.** `./gradlew compileJava` and `./gradlew runServer` both succeed with no
exceptions - log confirms "Registered 1 entity types" (`lycanitesmobs:calpod`) and no "has no
attributes" error. Deployed to Lycannots (`./gradlew deploy`). **Confirmed crash on summon (2026-09-22, same day):** Glenn tried `/summon lycanitesmobs:calpod`
in Lycannots - client crashed exactly as predicted:
`NullPointerException: Cannot invoke "EntityRenderer.shouldRender(...)" because
"entityrenderer" is null`, in `EntityRenderDispatcher.shouldRender`. Root cause: NeoForge has
no fallback renderer - every `EntityType<? extends LivingEntity>` needs one registered via
`EntityRenderersEvent.RegisterRenderers` (client-only mod-bus event) or it NPEs the instant the
entity enters render range. Fixed by adding `com.lycanitesmobs.client.PlaceholderCreatureRenderer`
(an `EntityRenderer<BaseCreatureEntity>` with no model - just `getTextureLocation()` pointing at
a harmless vanilla texture; draws nothing but still handles shadow/name-tag via the base class)
and `com.lycanitesmobs.client.ClientSetup.registerEntityRenderers()`, which registers it for
every creature in `CreatureManager`. Wired into `LycanitesMobs`'s constructor behind an
`FMLEnvironment.dist.isClient()` guard so the client-only `EntityRenderersEvent` class is never
referenced (and thus never classloaded) on a dedicated server. Creatures are invisible until
Phase 8 ports real models, but AI/hitbox/combat/sounds all work. Rebuilt, redeployed to
Lycannots - **not yet re-tested by Glenn after this fix.**

**Missing items/blocks investigated and mostly fixed (2026-09-22, same day):** Glenn noticed
"quite a few" items and a few blocks missing, plus asked specifically about "charges." Findings:
- **The 40 food items** (moss_pie, cooked/raw_*_meat, etc) were never actually blocked on any
  unported system - `ItemInfo`/`GenericItem`/`ItemManager.loadAllFromJson()` (the JSON-driven
  item pipeline, parallel to `CreatureInfo`) were already fully ported since Phase 4a. The
  entire gap was missing data: `common/lycanitesmobs/items/` (the JSON definitions) was never
  copied into our resources. Fixed by copying all 40 JSON files + their item models + textures
  (scripted, same referential-copy approach as the Phase 4c asset fix) + the 36 real
  campfire/furnace/smoker cooking recipes (converting 1.20.1's bare-string recipe `"result"` to
  1.21.1's required `{"id": ...}` object form - a real, verified format break, not an
  assumption). Skipped two upstream leftovers that aren't part of the real cooking chain
  (`joustmeatcooked.json`, `cephignismeatcooked.json` - nonsensical shapeless recipes converting
  a cake/taco into raw meat, clearly stale test data) and one filename-vs-internal-name mismatch
  (`raw_joust_meat.json`'s `"name"` field is actually `"raw_jouste_meat"` - resolved by reading
  the internal name, not the filename, same class of upstream quirk as the earlier mobtoken/
  soulgazer texture reference).
- **BlockShadowfire** was excluded in Phase 4c specifically because it calls
  `BaseCreatureEntity.hasElement()`, which didn't exist when BaseCreatureEntity itself didn't
  exist. Now that Phase 5d ported BaseCreatureEntity (but had dropped `hasElement()`/
  `getElements()` as part of the aggressive trim), restored both as thin wrappers around
  `CreatureInfo.getElements(Subspecies)` (already ported), and wired real element checks back
  into `canBurn()` to match upstream fidelity. Ported `BlockShadowfire` itself (mechanical,
  same pattern as the other 6 already-ported fire blocks) and added its block+sound
  registration to `ItemManager.loadItems()`. 118 blocks now register (was 117).
- **"Charges" (`ChargeItem`) are correctly NOT present** - Glenn's own instinct here was right.
  They're not a `loadItems()` omission at all; `ChargeItem` instances are created dynamically
  per-`ProjectileInfo` (`core/data/info/projectile/ProjectileInfo.java:548`,
  `this.chargeItem = Lazy.of(() -> new ChargeItem(properties, this))`), genuinely gated on
  Phase 6's `ProjectileManager`. Left a code comment in `ItemManager.loadItems()` explaining
  this so it's not mistaken for a gap again.
- **Still genuinely gated** (correctly, not a bug): the ~37 remaining hardcoded items
  (soulgazer, soulstone, equipment, soulkeys, summoning staves - creature-system/
  `ExtendedPlayer`/`AltarInfo`-dependent) and the 5 equipment-forge/pedestal blocks (need
  containers, Phase 6/8).

Rebuilt (118 blocks + 40 more items registering clean via `runServer`, no errors), redeployed
to Lycannots. **Not yet re-verified by Glenn in-game.**

**Second test creature - Concapede (2026-09-23):** Glenn asked for this one by name. Much
bigger lift than calpod - both `EntityConcapedeHead` and `EntityConcapedeSegment` extend
`AgeableCreatureEntity` for real (growth/breeding/multi-segment chaining), not just
`BaseCreatureEntity`, and that class was still just an empty stub. Ported, trimmed:
- **`AgeableCreatureEntity`** (real port now, ~300 of the original 470 lines) - kept growth
  ticking, love/breeding state, NBT persistence, `createChild`/`procreate`. Dropped: the
  right-click interact-command system (spawn-egg baby spawning, feed-to-breed - needs
  `ItemCustomSpawnEgg`, not ported, and BaseCreatureEntity never got a command-dispatch base
  to override), `MateGoal`/`FindParentGoal` (not ported, so breeding only happens if
  something calls `breed()`/`procreate()` directly, not via AI goals yet), and the
  `ExtendedPlayer` beastiary-study hook in `procreate()`.
- **`FollowGoal` + `FollowParentGoal`** ported (goal package now 15 files) - needed for
  segments to actually follow their parent segment/head.
- **`DeferredLevelActionManager`** ported, but drastically simplified: the original queues
  entity spawns to run on a later tick once the target chunk is confirmed loaded (~150 lines
  of queue/retry plumbing off a Forge tick event). Since every current caller spawns at an
  already-loaded, actively-ticking parent's position, this is now just an immediate
  `level.addFreshEntity()` call - port the real deferred-queue version if something ever
  needs to spawn at a possibly-unloaded position.
- **`EntityConcapedeHead`/`EntityConcapedeSegment`** ported keeping the actual "centipede
  body" mechanic (segment-chain spawning on first spawn, growing a new segment periodically,
  a tail segment growing into a new head when it matures) close to faithful, since that's the
  point of the creature. Dropped `TemptGoal` (not ported, consistent with dropping it from
  `BaseCreatureEntity`'s own `registerGoals()` earlier) and the bag/equipment overrides
  (`getNoBagSize`/`getBagSize` - that subsystem isn't ported).
- Found and fixed a real gap this surfaced: **`onFirstSpawn()` was never wired into
  `BaseCreatureEntity.aiStep()`** at all during the Phase 5d trim - the hook existed nowhere,
  so `EntityConcapedeHead`'s initial segment-spawning would silently never have fired. Added
  the hook + wired it into `aiStep()` (only calpod existed before this and doesn't use it, so
  it went unnoticed until a creature that actually needs it showed up).
- Also restored several small `BaseCreatureEntity` helpers this pulled back in:
  `getFacingPosition`/`getFacingPositionDouble` (trig helpers, needed for the segment's
  drag-to-parent positioning and for `FollowGoal`'s behind-target offset), `testLightLevel`/
  `isDaytime` (concapede is nocturnal-aggressive), `hasParent()`, `isPersistant()`,
  `inheritSpawnEventFrom()`, `getFallingMod()`, and a trivial base `canDropItem()` for
  `AgeableCreatureEntity`'s adult-only-drop override to have something to override. Also hit
  a real 1.21.1 rename: `Entity.portalTime` → `portalCooldown`, and it's `private` now (use
  `getPortalCooldown()`/`setPortalCooldown()`, not direct field access).
- **Also fixed a real regression found in the log while working on this:** `BlockShadowfire`
  had been registered (Java side, from the items/blocks gap fix above) without its client
  assets ever being copied - `latest.log` showed blockstate-variant and item-model load
  failures for it. Copied the full asset set (blockstate, 12 block model variants, item
  model, 2 block textures + mcmeta, sound, particle texture), matching the already-working
  fire blocks' file set.

Compiles clean, `runServer` shows 3 entity types registered (calpod, concapede,
concapedesegment), no errors. Redeployed to Lycannots. **Not yet tested in-game** - concapede
segment-chain spawning, growth, and the shadowfire fix are all unverified beyond
`runServer`/compile.

**Placeholder renderer upgraded from invisible to visible (2026-09-23):** Glenn tested
concapede in-game and (correctly, per the above) found it invisible - said he needs to
actually see creatures while testing to judge what's going on, so real per-creature model
rendering got pulled forward rather than waiting for a dedicated Phase 8 pass.
`PlaceholderCreatureRenderer` now extends `MobRenderer<BaseCreatureEntity, PigModel
<BaseCreatureEntity>>`, reusing vanilla's `PigModel` (generic over `Entity`, not `Pig`
specifically - confirmed no Pig-specific coupling in `QuadrupedModel.setupAnim`) baked via
vanilla's own already-registered `ModelLayers.PIG` layer (no need to register a new one) and
vanilla's pig texture. `MobRenderer`/`LivingEntityRenderer` already scale the model to each
entity's own `getScale()`/dimensions, so different creatures render at roughly their real
configured size, just as a pig shape. This is NOT the real model (Lycanites uses a custom
in-house OBJ model/animation format, not a standard one like GeckoLib - actually porting that
renderer is still real, separate Phase 8 work) - it's a deliberately cheap stand-in so combat/
movement/multi-entity behavior (e.g. concapede segments visibly following each other) can
actually be watched while testing. Compiles clean; not yet visually confirmed in-game (this
change can only be verified by actually looking at the game, `runServer` never exercises
client rendering code at all).

See the Phase 5 sub-phase
breakdown above for exactly what was ported vs. dropped vs. stubbed, and the 1.21.1 API changes
list - substantial new ground versus Phases 0-4c, since this is the first time the port
touched `LivingEntity`/`Mob` deeply enough to hit the leashing refactor, the `SynchedEntityData.
Builder` change, `getDimensions` becoming final, etc.

Phases 0–3 complete, Phase 4a+4b+4c complete — all verified end-to-end
(`./gradlew runServer` loads clean, no errors). 3 real items (`mobtoken`, `immunizer`,
`cleansingcrystal`), 7 full dungeon building block sets (105 blocks: 15 variants x
lush/desert/shadow/demon/aberrant/ashen/stream), and 12 effect blocks (7 fire, 3 cloud,
2 web) all register successfully - **117 blocks + 117 block items confirmed registered**
via a log line in `RegistryEvents`, not just inferred from a clean run. Config subsystem
is 11/13 files ported — `ConfigCreatures` and `ConfigCreatureSubspecies` are deferred to
Phase 5 (they depend on `Variant`/`CreatureStats`/`CreatureManager`).

**Post-4c crash fix (2026-09-22):** Glenn hit a real client-side crash opening the
creative inventory in Lycannots -
`IllegalStateException: Registry is already frozen`, traced through
`BlockManager`/`ObjectManager`/`LMBlocksGroup` into `Block.<init>`. Root cause:
`ObjectManager.addBlock()` only ever stored blocks as a `Lazy` supplier in a local map -
unlike `addItem()`, it never actually registered them to a `DeferredRegister`. Nothing
forced those `Lazy`s to resolve during the registration window, so blocks silently never
made it into the real block registry - no error at mod load (which is why `runServer`
never caught it: a dedicated server never builds creative tab contents), but the first
thing that forced the `Lazy` (opening the creative inventory, client-only) tried to
construct-and-self-register a `Block` into an already-frozen registry and crashed. Fixed
by porting a trimmed `RegistryEvents` class (just block/block-item registration - the
original's other responsibilities all need Phase 5/6 systems) and wiring it to
`RegisterEvent` on the mod event bus, matching the original Forge code's actual mechanism
(`RegistryEvents` was referenced in the constructor but I hadn't ported it). Verified with
the registration count log, not just a clean load. **Lesson for later phases:** `runServer`
alone doesn't exercise client-only code paths like creative tabs/GUIs - this class of bug
(a `Lazy`/deferred value that's never forced to resolve until something client-side touches
it) can hide behind a clean server run. Worth specifically checking anything
`ObjectManager`-Lazy-backed gets a real registration path, not just assuming symmetry with
how items work.

Phase 4 is now genuinely exhausted of self-contained work - everything left in
`ItemManager.loadItems()` (the ~38 remaining items, `BlockShadowfire`, the 5 equipment/
pedestal blocks) is gated on Phase 5 (creatures), Phase 6 (altars/capabilities), or
Phase 6/8 (containers). Next up has to be Phase 5 (Creatures) - there's no more Phase-4
runway left to burn through first.

**Post-4c asset fix (2026-09-22):** Glenn then reported missing textures and "descriptions
all bugged" after the crash fix. Expected in part - no client resources (lang, textures,
models, blockstates) had been ported at all yet, only Java code. Fixed by scope-copying
just the assets for what's currently registered (not the full 132MB mod - the official
`assets/lycanitesmobs/` tree covers all 123 creatures, almost none of which exist yet):
713 files (~3.8MB) - the full `en_us.json` lang file (2283 lines, safe to take wholesale
since unused keys for unregistered content are just inert data) plus every blockstate/
model/texture file matching a name we've actually registered. Verified by cross-checking
every `"lycanitesmobs:..."` reference inside the copied JSON against what was actually
copied (a Python pass over all 357 references) rather than assuming the name-matching copy
was complete - caught two real gaps: `mobtoken`'s item model references `soulgazer`'s
texture (an upstream quirk in the original mod, not a porting bug - copied it in) and one
truly-missing texture in the *original* mod itself (`smitefireballcharge`'s model points at
a texture that was never included upstream) - that one's for an item we haven't registered
yet, so it's inert, not worth chasing now. Same asset-scoping approach should repeat for
every future content batch: copy what you just registered, then verify referentially, don't
assume name-matching caught everything.

## Phase 5 continued: calpod/concapede content gaps, then the real OBJ renderer (2026-09-23)

**Calpod summon crash → invisible placeholder renderer.** Summoning calpod crashed with an
`NullPointerException` in `EntityRenderDispatcher.shouldRender` — NeoForge has no fallback
renderer, and nothing had registered one for any creature yet. Fixed with
`PlaceholderCreatureRenderer` (an `EntityRenderer<BaseCreatureEntity>`, initially invisible)
plus `ClientSetup.registerEntityRenderers()` looping every `CreatureManager` creature, wired
behind the existing `FMLEnvironment.dist.isClient()` guard so the client-only event class
never classloads on a dedicated server. Later upgraded `PlaceholderCreatureRenderer` to extend
`MobRenderer<BaseCreatureEntity, PigModel<BaseCreatureEntity>>`, reusing vanilla's `PigModel`
(confirmed generic over `Entity`, not `Pig`-specific) so creatures at least render as a
correctly-scaled pig shape instead of nothing, while real per-creature models are ported
incrementally.

**Missing items/blocks turned out to be a missing-data problem, not a missing-code one.**
Glenn pushed back on an earlier (pre-Phase-5) `PORT_PLAN.md` assumption that ~40 remaining
hardcoded items were "genuinely gated" on unported systems — specifically flagged food items
as something that should just work. Tracing `ItemManager.startup()`'s actual call graph
(`loadItems()` (hardcoded) + `loadAllFromJson()` (JSON-driven)) confirmed he was right: 40
food items are JSON-driven and just needed their JSON/model/texture files copied (one
filename/internal-name mismatch found: `raw_joust_meat.json`'s internal `"name"` is
`"raw_jouste_meat"` — fixed by deriving asset lookups from the JSON's internal name, not the
filename). Also found and fixed a real self-inflicted regression while restoring
`BlockShadowfire`: its Java registration existed with zero client assets copied
(blockstate/models/textures/sound/particle — 24 files, mirrored off the already-working
hellfire block's layout). Recipe JSON also needed a real 1.21.1 format fix, verified against
a genuine vanilla `cooked_beef.json`: `"result": "ns:item"` (bare string) →
`"result": {"id": "ns:item"}`.

**Concapede port.** Needed the real `AgeableCreatureEntity` (previously an empty stub), scoped
down to growth/breeding/segment-chain mechanics while dropping the interact-command system
(needs `ItemCustomSpawnEgg`, not ported), `MateGoal`/`FindParentGoal` AI (breeding only
triggers via direct calls for now), and the beastiary-study hook. Ported `FollowGoal`/
`FollowParentGoal` verbatim (self-contained) and a drastically simplified
`DeferredLevelActionManager` (just `level.addFreshEntity()` — the original's ~150 lines of
chunk-load-retry queueing isn't needed since every current caller spawns at an already-loaded
parent's position). Found and fixed a real gap in `BaseCreatureEntity`: `onFirstSpawn()` was
never wired into `aiStep()` at all — calpod never exercised that path, but concapede's
segment-chain spawning depends on it. Restored several other methods
`BaseCreatureEntity` had lost in the earlier aggressive Phase 5 trim, only discovered once a
creature actually needed them: `hasParent()`, `isPersistant()`, `inheritSpawnEventFrom()`,
`getFallingMod()`, `canDropItem()`, the `getFacingPosition`/`getFacingPositionDouble` family,
`testLightLevel()`/`isDaytime()`. Also fixed `Entity.portalTime` → the 1.21.1
`getPortalCooldown()`/`setPortalCooldown()` rename (field went private too).

### The real renderer

Glenn: *"How about you port the REAL renderer. That may not be in this step, but it's a
request for consistent testing."* — explicitly asked for actual OBJ model/animation
rendering instead of the pig placeholder, so combat/movement/AI work can be watched with real
visual feedback instead of everything looking like a pig.

**Scope-reduction finding that made this tractable:** the mod ships an extensive
Iris/Oculus shader-compatibility + custom VBO-batching layer on top of its base OBJ renderer
(`VBOObjModel`, `IrisStaticMeshBuffer`, `IrisComputeVboBatcher`, `IrisStaticVboBatcher`,
`VBOBatcher`, `CustomRenderStates`, `RecolorTextureCache` — 2377+ lines). `VBOObjModel
.renderPart()` falls back to plain immediate-mode `ObjModel.renderPart()` whenever a non-null
`VertexConsumer` is passed in — so the entire Iris/VBO system is cleanly skippable by using
plain `ObjModel` and always supplying a real `VertexConsumer`, without touching any of
`VBOObjModel`'s static-field "render context" pattern. Narrowed the estimate from ~9500 lines
down to ~4500.

**Two parallel rendering systems discovered mid-investigation:** a modern "template" system
(`ModelTemplate*` extending `CreatureObjModel`, covering 76/123 creatures — Biped 23,
Elemental 21, Quadruped 15, Insect 6, Dragon 5, Aquatic 4, Arachnid 2) and an older "legacy"
system (`CreatureObjModelOld`, covering the other 39/123 including concapede). Calpod uses
the template/Insect system, so this pass only ports that one — concapede and everything else
stays on the pig placeholder until `CreatureObjModelOld` gets ported separately.

**Architecture decision — reuse vanilla's render() dispatch instead of hand-porting
`CreatureRenderer.render()`:** the original `CreatureRenderer` overrides
`LivingEntityRenderer.render()` wholesale with its own pose-stack rotation/scale/translate
logic. Read it closely before porting and found a real red flag: it computes body/head yaw
via `Mth.clamp(yaw, entity.yBodyRotO, entity.yBodyRot)`, where 1.21.1 vanilla's own
equivalent uses `Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot)` — a clamp is
not a lerp, so either the reference source predates a real vanilla API this was written
against, or it's already subtly wrong upstream. Given the confusing swapped `partialTicks`/
`yaw` parameter naming found in the same method (Java doesn't care about parameter names, but
a human hand-porting it by name easily could), decided this method was too risky to hand-port
line-by-line. Instead, `CreatureModel` now hooks into the two calls vanilla's own
`LivingEntityRenderer.render()` already makes on every model — `setupAnim()` (stash entity +
animation params, then call `generateAnimationFrames()`) and `renderToBuffer()` (draw, using
the stashed params) — which gets vanilla's rotation/scale/translate/name-tag/leash handling
for free and verified-correct, at the cost of per-entity subspecies model swapping (out of
scope for now; `CreatureRenderer` resolves one fixed model at construction). One real
consequence: vanilla's `render()` already applies `entity.getScale()` to the whole pose stack
before calling into the model, so `CreatureObjModel` must NOT re-apply
`entity.getScale()` itself the way the original code did (the original's own `render()`
never called `poseStack.scale(entity.getScale())` at the top, since it never reused vanilla's
dispatch at all) — removed that line from both `generateAnimationFrames()` and `render()` to
avoid double-scaling every creature.

**Files ported, mostly verbatim, from the "no Iris/rendering-API risk" bottom of the
dependency chain up:**
- `core/util/math/Vector3o.java`, `HashMapWithDefault.java` — verbatim.
- `client/obj/material/Material.java`, `client/obj/geometry/{ObjPart,Vertex,IndexedModel}.java`
  — verbatim.
- `client/obj/geometry/Mesh.java` — trimmed: dropped `getVbo()`/`getIrisEntityBuffer()` and
  all VBO/Iris fields entirely; kept only `indices`/`vertices`/`normals` and
  `computeVertexNormalsIfNeeded()` (pure math).
- `client/obj/model/Model.java` — ported verbatim; turned out to be dead code once `ObjModel`
  was confirmed to not actually extend it, but harmless.
- `client/loader/OBJLoader.java` — verbatim (the real `.obj` text parser).
- `client/obj/model/ObjModel.java` — ported with the real 1.21.1 vertex-API fix (see below).
- `client/model/animation/{IAnimationModel,Animator,ModelObjAnimationFrame,AnimationPart}.java`
  — verbatim.
- `client/model/creature/base/ModelObjState.java`,
  `client/gui/screen/creature/RecolorDebug.java` — verbatim.
- `client/renderer/layer/creature/LayerCreatureBase.java` — ported with fixes: inlined the two
  `WHITE`/`ZERO_TEXTURE_OFFSET` constants directly (avoided porting the 475-line
  `CustomRenderStates.java` just for them), and fixed the abstract `render(...)` override to
  1.21.1's actual `RenderLayer` signature (reordered params, dropped trailing `float scale`).
  Its own `render()` body is a no-op — nothing calls into `LayerCreatureBase` instances via the
  vanilla-dispatch renderer (see architecture decision above), it only still needs to exist as
  a real type because `CreatureModel`/`CreatureObjModel` method signatures take one.
- `client/model/creature/base/CreatureModel.java` — the vanilla-dispatch bridge described
  above; fixed the `Model` base-class constructor (now needs a `RenderType` lookup function)
  and `renderToBuffer()`'s signature (packed light/overlay/color as one int, not 4 raw floats).
- `client/model/creature/base/CreatureObjModel.java` — the 628-line "meat" class trimmed to
  ~450: `ObjModel` instead of `VBOObjModel`, no Iris-variant-recolor branch, no
  `_animation.json` loading (calpod has none — `ModelAnimation`/`TextureLayerAnimation`/
  `ModelPartAnimation` don't need porting for this first pass), `entity.getScale()` no longer
  re-applied (see above).
- `client/model/template/ModelTemplateInsect.java`,
  `client/model/creature/insect/ModelCalpod.java` — verbatim (both self-contained, no Iris/
  layer deps).
- `client/manager/ModelManager.java` — trimmed from 241 lines: dropped everything projectile/
  equipment-part (`ProjectileManager`/`ProjectileInfo`/`ProjectileObjModel`/
  `EquipmentPartManager`/`ItemEquipmentPart`/`ModelEquipmentPart`/`EquipmentModel` — none of
  those exist in the port yet), and made `createModels()` resilient per-creature (log + skip a
  missing/broken model class instead of throwing and aborting every other creature's model).
- `client/renderer/entity/creature/CreatureRenderer.java` — reduced to ~30 lines: constructor
  resolves the model via `ModelManager`, `getTextureLocation()` returns `entity.getTexture()`,
  plus a `getMainModel()` accessor kept only because `LayerCreatureBase` still references it.
  No `render()` override at all (see architecture decision above).
- `core/entity/base/BaseCreatureEntity.java` — added `getTexture()`/`getTexture(String)`/
  `getTextureName()` (ported verbatim; builds a texture name from
  `creatureInfo.getName()` + subspecies/variant suffixes via the already-ported
  `AssetHelper.entityTexture()`).
- `client/ClientSetup.java` — now calls `ModelManager.getInstance().createModels()` once up
  front, then per creature registers the real `CreatureRenderer` when
  `ModelManager.getCreatureModel()` resolves a model (currently just calpod), falling back to
  `PlaceholderCreatureRenderer` otherwise.

**1.21.1 rendering API changes hit (none of this had any precedent anywhere else in the port
— everything before this was server-side):**
- `VertexConsumer`'s fluent chain changed: old
  `.vertex(matrix4f,x,y,z).color(...).uv(...).overlayCoords(u,v).uv2(light).normal(matrix3f,x,y,z)
  .endVertex()` → new `.addVertex(x,y,z).setColor(...).setUv(...).setUv1(u,v).setLight(packed)
  .setNormal(...)`. No matrix-transform overload on raw position/normal anymore — positions and
  normals must be transformed manually first via `matrix4f.transformPosition(Vector3f)` /
  `matrix3f.transform(Vector3f)` (JOML methods, mutate in place) before calling
  `addVertex`/`setNormal`. `setUv1(u,v)` and `setLight(packedLight)` are the direct 1:1
  replacements for the old `.overlayCoords(0, overlayV)` / `.uv2(brightness)`.
- `net.minecraft.client.model.Model` (vanilla — NOT this project's own
  `client.obj.model.Model`, same simple name, different package/purpose) now takes a
  `Function<ResourceLocation, RenderType>` constructor arg, and `EntityModel<T>` (which
  `CreatureModel` extends) forwards that through its own
  `protected EntityModel(Function<ResourceLocation, RenderType>)` constructor.
  `renderToBuffer()`'s abstract signature is now `(PoseStack, VertexConsumer, int packedLight,
  int packedOverlay, int color)` (color packed as one int).
- `RenderLayer<T,M>`'s abstract `render(...)` reordered its params vs. 1.20.1 and dropped the
  trailing `float scale` param entirely: 1.21.1 is `(PoseStack, MultiBufferSource, int
  packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTick, float
  ageInTicks, float netHeadYaw, float headPitch)`.
- `new ResourceLocation(namespace, path)` is private now — same break already known from
  earlier phases, must use `ResourceLocation.fromNamespaceAndPath(namespace, path)`.
- `com.mojang.math.Axis` confirmed still present/valid in 1.21.1 (`Axis.XP/YP/ZP
  .rotationDegrees(...)`), no change needed there.
- `LivingEntity.hurtTime` confirmed still a public field, used directly for the damage-fade
  calculation same as the original.

**Verification:** `./gradlew compileJava` clean. `./gradlew runServer` starts and reaches
"Done" with no errors — confirms nothing server-side broke and no client-only class leaked
into a codepath a dedicated server touches, but (as noted in the Phase 4c postmortem above)
this does **not** exercise any of the new rendering code itself, since a dedicated server
never renders. Deployed to Lycannots (`./gradlew deploy` — hardcoded to the Lycannots mods
folder only, confirmed via JAR timestamp). **Still needs an actual in-game look** to confirm
calpod renders with real geometry/animation rather than just "didn't crash" — that visual
confirmation from Glenn is the next real checkpoint, not this write-up.

## Phase 5 mass creature batch: 40 more creatures ported (2026-09-24)

Glenn: "Try porting the other creatures, at least 1/3 of them." Confirmed first that creature
registration is **100% automatic and JSON-driven** — `CreatureManager` loads every
`creatures/*.json` and `CreatureBootstrapHelper.registerEntityTypeSuppliers()`/
`bindRegisteredValues()` (entity type, attributes, sounds) already loops every loaded creature
generically by reflecting on the JSON's `entityClass` field. Creature groups/types are likewise
already fully present for every category. **A new creature needs zero shared-file edits** —
just its own JSON + entity Java class(es). This made the batch both tractable and safely
parallelizable.

Scoped down hard for this pass: no textures/lang/assets (the placeholder renderer hardcodes
vanilla's pig texture regardless of what's on disk, so there's zero visible payoff right now),
and no real OBJ model porting (stays on the pig placeholder, same as everything except calpod).
Just entity Java + creature JSON, aggressively trimmed the same way concapede was — keep core
stats/AI (movement, melee, target-finding), drop anything gated on unported infrastructure
(tame/master/mount/stamina, ranged projectile attacks needing `ProjectileManager`, equipment/
bag, entity-pickup-and-carry + its `ExtendedEntity`/`ExtendedPlayer` capabilities, interact-
commands), each with a one-line comment saying what and why.

**Execution:** split into 5 parallel forks (amphibian+aquatic, avian+reptile, beast×2, remaining
insects — calpod/concapede/concapedesegment already done), each briefed with the automatic-
registration finding, the trim rules, a hard boundary against editing any shared file
(`BaseCreatureEntity`, managers, `ClientSetup`, lang, etc. — since forks ran concurrently
against the same working tree with no git worktree isolation, only strictly additive new-file
work was safe to parallelize), and the already-known 1.21.1 gotchas list. 4 of 5 forks hit the
session's rate limit partway through and terminated early; picked up their unfinished creatures
(6: lacedon, roa, silex, skylus, stryder, wraamon) by hand afterward using the same approach.

**Result: 40 new creatures ported** (43 total now, up from 3) —
amphibian: aglebemu, ningen, salamander · aquatic: abaia, abtu, cephignis, herma, ioray,
lacedon, roa, silex, skylus, stryder · avian: raiko, roc, uvaraptor, ventoraptor · reptile:
arisaur, aspid, geken, khalk, thresher · beast: balayang, barghest, bobeko, brucha, chupacabra,
conba, dawon, epion, feradon, jabberwock, kobold, makaalpha, maka, maug, warg, wraamon ·
insect: darkling, erepede, eyewig, gorgomite, joustealpha, jouste, ostimien, vespid, vespidqueen.

**Real gaps found and fixed while integrating the forks' work (all generic, restored to
`BaseCreatureEntity`, not creature-specific hacks):**
- `waterContact()` and `getAISpeedModifier()` were missing entirely — used by nearly every
  aquatic creature for pathing/speed decisions. Restored both (simplified `waterContact()`'s
  underground check vs. the original, same trim style as `testLightLevel()`), and — this part
  wasn't just a compile fix — wired `getAISpeedModifier()` into a `setSpeed()` override, since
  without that a creature's own override of it would compile but silently do nothing at
  runtime, the same class of bug as `onFirstSpawn()` never being called before concapede.
- `hitAreaWidthScale`/`hitAreaHeightScale` fields were missing (used by `EntityStryder`).
- `IGroupBoss`/`IGroupHeavy` marker interfaces (`core/entity/IGroupBoss.java`/`IGroupHeavy.java`)
  didn't exist at all — trivial empty interfaces, ported both.
- `EntityDimensions` is a record in 1.21.1, not a class with public `width`/`height` fields —
  `.width`/`.height` field access must be `.width()`/`.height()` method calls now (hit in
  `EntityAbaia`/`EntityDarkling`, both doing particle-position math off an entity's hitbox size).
- `Entity.yRot` is private now — direct field access (`this.yRot = x`) must become
  `this.setYRot(x)` (hit in `EntityDarkling`'s latch-onto-target facing logic).
- `Entity.RemovalReason` is a nested enum on `Entity`, not `net.minecraft.world.entity
  .RemovalReason` — import fix (`EntityJouste`).
- `Entity.setMaxUpStep(float)` doesn't exist — 1.21.1 replaced it with an overridable
  `maxUpStep()` getter (already known from the concurrent avian/reptile fork, but several other
  creatures across other forks used the same old setter and needed the same fix).
- `Mob.canPickupItems()` doesn't exist — it's `canPickUpLoot()` (`EntityKobold`).

**Verification:** `./gradlew compileJava` clean across all 43 creatures. `./gradlew runServer`
starts clean and logs **"Registered 43 entity types"** (was 3) — a real registration count, not
just inferred from a clean load, matching the earlier Phase 4c lesson about verifying counts
explicitly. Deployed to Lycannots (JAR timestamp confirmed). All 40 new creatures render as the
pig placeholder, same as everything except calpod — no visual/rendering work was in scope here.

## Post-mass-batch bug fixes: calpod invisible, "no AI" report (2026-09-24)

Glenn: "Calpod rendering doesn't work (invisible) and the entities are all missing AIs; they
don't move or do anything." Checked logs first per usual — no exceptions anywhere, but two real
bugs surfaced, both from asset/data gaps rather than logic bugs:

**Calpod invisible - root cause confirmed in `latest.log`:** `Unable to load model:
lycanitesmobs:modelparts/entity/calpod.obj` / `Unable to load model obj for: calpod` / `Unable
to load model parts json for: calpod`. The real renderer's Java code was ported in the previous
session, but the actual `.obj`/`_parts.json` model asset files were never copied - same gap
class as the earlier `BlockShadowfire` asset regression. Fixed by copying `calpod.obj` and
`calpod_parts.json` from the official source's `modelParts/entity/` (note: capital P in the
official tree; copied to lowercase `modelparts/` to match what the Java code requests) into
`src/main/resources/assets/lycanitesmobs/modelparts/entity/`, plus `calpod.png`/
`calpod_verdant.png`/`calpod_violet.png` into `textures/entity/` (needed now that calpod uses
the real `CreatureRenderer`, which calls `entity.getTexture()` - unlike the pig placeholder,
which hardcodes vanilla's pig texture regardless of what's on disk, so no other creature needed
this yet).

**"No AI" - investigated by direct empirical testing, not just code reading.**
`./gradlew runServer`'s console doesn't forward interactive stdin through Gradle's process
wrapper (confirmed by testing both a raw FIFO redirect and a `tmux`-backed pty - neither
delivered typed commands to the dedicated server's command dispatcher, even a bare `list`).
Worked around it by enabling RCON in the dev sandbox's `run/server/server.properties` (gitignored,
reverted after) and writing a minimal ~40-line Python RCON client
(`Source/Binary Protocol` packets) to summon creatures and poll `/data get entity ... Pos` on a
delay, on a flat forceloaded test platform. This confirmed calpod, vespid, abaia, warg, kobold,
and maka all move normally under test conditions - `registerGoals()`'s construction-order
(`defineSynchedData()` runs during the `Entity` base constructor, before `Mob`'s constructor
body calls `registerGoals()`, so goal-index fields are already correctly set), `WanderGoal`,
`RandomPositionGenerator`, and the default `GroundPathNavigation` are all structurally sound -
no bug there. (Also confirmed `rollWanderChance()`'s size-based throttle - 0.0005/tick for
hitbox width >= 3 blocks, ~100 real seconds between wander rolls - is faithful to the original,
not a porting regression; large creatures are just meant to wander rarely.)

**The real bug this surfaced:** `warg`, `kobold`, `maka`, `maug`, `jabberwock`, `makaalpha`, and
`feradon` (7 of the "beast batch 2" mass-batch creatures) had their entity Java class written
and compiling, but **their creature JSON was never copied** - meaning
`CreatureManager`/`CreatureBootstrapHelper` never registered an `EntityType` for them at all
(`/summon lycanitesmobs:warg` failed outright: "Can't find element... of type
'minecraft:entity_type'"). Cross-checked every entity Java class against its expected JSON by
script (and the reverse direction too, JSON entityClass against Java file existence - clean, no
other gaps) - this exact family was the only one affected, consistent with that fork getting cut
off by the session rate limit mid-batch, after finishing all its Java files but before finishing
its JSON copies. Fixed by copying the 7 missing JSONs from the reference source. Registered
entity type count went 43 -> 50 confirmed via the startup log line, not just inferred.

So: Glenn was very likely seeing a mix of "calpod is invisible so I can't tell if it's moving"
and "an unknown chunk of creatures literally didn't exist to summon at all" rather than a
systemic AI failure - but this needed live testing to be sure, not just reading the goal code.
**Lesson for future creature batches:** when a fork reports "N of N ported, `compileJava` clean"
after finishing under time/rate pressure, that only proves the Java side landed - explicitly
verify the JSON side too (e.g. `find creatures/*.json` count against the batch's expected
list), don't take "compiles clean" as proof the batch is fully wired up end-to-end.

Verified: `./gradlew compileJava` clean, `runServer` logs "Registered 50 entity types", RCON
movement test confirms AI works for a representative sample across categories. Deployed to
Lycannots (JAR timestamp confirmed).

## Calpod still invisible after the asset fix - real root cause (2026-09-24)

Glenn: "Calpod still doesn't render." The asset-copy fix from the previous entry was necessary
but not sufficient - `latest.log` still showed the same `Unable to load model:
lycanitesmobs:modelparts/entity/calpod.obj` warning even though the files were confirmed
present in the deployed jar (`unzip -l` check). `latest.log`/`debug.log` only print the warning
line, not the actual exception (`ObjModel.initFromResource()` catches it and calls
`e.printStackTrace()`, which goes to real stderr, not the log4j-backed logs) - had to check
`<instance>/logs/stdout-logs.txt` instead to see it: `java.util.NoSuchElementException: No
value present` at `Optional.get()` inside `ObjModel.initFromResource()`, i.e.
`resourceManager.getResource(resourceLocation)` genuinely can't find the file at that point in
time, despite it existing in the jar.

**Root cause:** lifecycle ordering. `EntityRenderersEvent.RegisterRenderers` (where
`ClientSetup` calls `ModelManager.getInstance().createModels()`, which constructs every
`CreatureModel` and - inside its constructor - immediately tries to load its `.obj`/
`_parts.json`) fires *before* the mod's own resources are loaded into the client's
`ResourceManager`. Confirmed by log line order: the "Unable to load model" warning appears
*before* `[ReloadableResourceManager]: Reloading ResourceManager: vanilla, mod_resources,
mod/lycanitesmobs, mod/neoforge` in the same log. The official 1.20.1 source never hits this
because it calls `ModelManager.createModels()` from `FMLClientSetupEvent` (a later phase) *and*
separately registers a `ModelReloadListener` (a `PreparableReloadListener`) via
`RegisterClientReloadListenersEvent` that calls `ModelManager.reloadModels(resourceManager)` -
tying the actual OBJ data load to the real resource-reload lifecycle, not model-object
construction time. That reload listener was never ported in this port - the constructor's first
load attempt was destined to fail every time, and nothing ever retried it.

**Fix:** ported `ModelReloadListener` (`client/loader/ModelReloadListener.java`, trimmed - drops
the Iris/VBO cache-clearing calls, none of that exists in this port), registered via a new
`ClientSetup.registerReloadListeners(RegisterClientReloadListenersEvent)` method wired in
`LycanitesMobs.java` alongside the existing renderer registration. `ModelManager.reloadModels()`
(already ported, previously dead code since nothing called it) now actually runs once resources
are ready, re-invoking `initModel()` on the *same* already-constructed model instances
`CreatureRenderer` already references - self-healing, no changes needed to renderer
registration timing itself.

**Verified live, not just from logs** - `runServer` can't exercise any of this (dedicated
servers never load client resources or render), so used `./gradlew runClient` directly and
grepped its log for the fix taking effect. Added a temporary unconditional success log
(`CreatureObjModel.initModel()`, since downgraded to the existing gated `logDebug("Resources",
...)` pattern once confirmed) to prove it: log now shows `Loaded model obj for: calpod (11
parts)` immediately after the `Reloading ResourceManager` line. Confirmed via
`./gradlew compileJava` (clean) and `./gradlew runServer` (still clean, 50 entity types) that
nothing server-side broke. Deployed to Lycannots.

Glenn separately asked to eliminate all rendering/AI placeholders "it may be needed to test."
AI was already re-confirmed real (not placeholder) via the RCON movement testing in the
previous entry. Rendering is still the pig placeholder for 49/50 creatures (only calpod has a
real model) - Glenn's call: hold off on porting the rest for now, revisit as its own scoped
session. **Next real rendering step, when picked back up:** port the "legacy" model system
(`CreatureObjModelOld`, covers 39/123 creatures including concapede) generically, the same way
`CreatureObjModel`/`ModelTemplateInsect` were ported for the modern system - that unlocks real
rendering for a large chunk of already-ported creatures without per-creature model work.

## Calpod renders but as a shattered mosaic - backface culling bug (2026-09-24)

Glenn (with a screenshot, after the reload-listener fix above): calpod now shows real
geometry, but as a jumble of disconnected-looking angular shards rather than a solid insect
body - described as "textures don't render in the correct orientation, mosaic of random
parts." He'd initially said "concapede," but the log showed no concapede was ever summoned that
session (still on the pig placeholder the whole time, which structurally can't produce this)
while calpod had extensive say/step/hurt/death sound activity - confirmed with him it was
actually calpod.

Exhaustively diffed every piece of the OBJ pipeline against the official source before touching
anything (`OBJLoader`, `IndexedModel.toMesh()`, `Vertex`, `AnimationPart.applyAnimationFrames()`
bone-hierarchy math, `CreatureObjModel.render()`'s per-part transform sequence) - all identical
to official, byte-for-byte MD5-matched `calpod.png` and `calpod.obj` against the reference
source too. None of that was it.

**Real cause: single-sided backface culling.** `CreatureModel`'s constructor hardcoded
`RenderType::entityCutout` (culls backfaces based on triangle winding) as the render type for
every OBJ model. The official source's own OBJ render types
(`CustomRenderStates.OBJ_CUTOUT`/`getObjVBORenderType`) default to **double-sided**
(`entityCutoutNoCull`), only opting into backface culling per-part via each `ObjPart`'s
`cullBackfaces` flag - which calpod's `_parts.json` doesn't set on any part. With single-sided
culling, roughly half of every thin part's triangles (legs, mouth pieces - all thin angular
insect geometry) get discarded depending on view angle and the quad-to-triangle-fan winding
order, which is exactly what a "shattered mosaic missing random pieces" look is. Fixed by
switching `CreatureModel`'s `Function<ResourceLocation, RenderType>` to
`RenderType::entityCutoutNoCull` to match the official default; per-part opt-in culling (reading
`cullBackfaces` and switching render type per-part, matching `OBJ_CUTOUT_CULL`) isn't ported -
not needed until a creature's `_parts.json` actually sets the flag on some part.

**Not independently visually verified this time** - no computer-use/screenshot tooling
available in this session, so this is diagnosed from the screenshot Glenn shared plus a
line-by-line pipeline audit that ruled out every other stage, not confirmed working after the
fix. `compileJava` clean, `runServer` clean (50 entity types, unaffected - purely a client
render-type change), deployed to Lycannots. Needs Glenn to check again in-game.

## CI

GitHub Actions (`.github/workflows/build.yml`, came bundled with the NeoForge MDK template)
runs `./gradlew build` on every push. Caught a real mistake early: `gradle.properties` briefly
had `org.gradle.java.home=/usr/lib/jvm/java-21-openjdk` committed to it (this machine's local
JDK path) - broke CI immediately since GitHub's runners don't have that path. Fixed by
removing it; rely on `JAVA_HOME` from the environment instead (CI's `setup-java` action sets
it correctly; locally, pass `JAVA_HOME=/usr/lib/jvm/java-21-openjdk` explicitly per command,
same as this plan's other `./gradlew` examples do). Never commit a machine-local
`org.gradle.java.home` to this repo again.

## Unlogged Sep 24 creature batch found broken + repaired (2026-09-26)

A second mass batch (written 2026-09-24 16:50-16:52, after the last PORT_PLAN update) was never logged and
left the build **not compiling**: 23 creature classes (grigori, apollyon, remobra, astaroth, trite, ettin,
behemophet, zoataur, aegis, argus, afrit, arix, cryptkeeper, krake, troll, wildkin, belphegor, morock,
banshee, spectre, clink, gnekk, geist, ghoul) + 22 JSONs. Same failure class as the earlier beast-batch-2
cutoff. Repairs:
- `EntityBanshee` called `strafe(double,double)`, dropped from the trimmed `BaseCreatureEntity` - ported the
  official method verbatim.
- `EntityAstaroth`/`EntityTrite` referenced unported `EntityMalwrath`/`EntityAsmodeus` in `canAttack()` -
  those checks removed with `TODO(port)` comments to restore once both are ported.
- `gnekk.json`/`ghoul.json` were never copied (Java existed, JSON didn't) - copied from reference.
Verified: `compileJava --rerun-tasks` clean, `runServer` reaches Done with 0 ERROR/FATAL lines. Only warnings
are pre-existing "Unable to add food effect" for unported effects. 74 creature JSONs now in resources.
**Note:** several creatures in these batches were "rebased onto BaseCreatureEntity" because
`TameableCreatureEntity` is still a stub (5e). Porting the real `TameableCreatureEntity` will mean re-parenting
them back to their official superclass - track this when 5e is done.

## Phase 5e: TameableCreatureEntity + taming core (2026-09-26)

Ported (verified: `compileJava` clean, `runServer` + RCON summon/tick/save of 10 tameable and rideable
creatures incl. re-parented ones, 0 ERROR/FATAL; deployed to Lycannots; **taming itself needs an in-game
player test - not verified headless**):
- **Treat items** (`CreatureTypeItem`, `CreatureTreatItem`, `CreatureManager.registerItems()` - treats only;
  saddle/spawn egg/filled soulstone still TODO). 19 treat models + textures copied.
- **Base interaction chain** in `BaseCreatureEntity`: `mobInteract` -> `getInteractCommands`/
  `assessInteractCommand`/`performCommand` (Leash/Name Tag/Color), consume/replace item helpers,
  `COMMAND_PIORITIES`, `GUI_COMMAND`, `performGUICommand` stub. Soulgazer command omitted (TODO).
- **Relationships**: `CreatureRelationships`/`CreatureRelationshipEntry` copied verbatim; base field + NBT,
  `getTamingReputation`/`getFriendlyReputation`, relationship check in `canAttack` and
  `FindAttackTargetGoal`, and a trimmed `hurt()` override that lowers the attacker's reputation.
- **Owner goals**: `BegGoal`, `FollowOwnerGoal`, `StayGoal`, `CopyOwnerAttackTargetGoal` (ExtendedEntity
  fallback dropped), `DefendOwnerGoal`, `RevengeOwnerGoal`, plus `MinionEntityDamageSource`.
- **`TameableCreatureEntity`**: ownership (implements vanilla `OwnableEntity`), treat taming via reputation,
  sit/follow/passive/aggressive/assist/PvP bits, owner kill credit, pet teams/alliance/PvP rules, owner
  damage immunity, owner effects, feeding, hunger/stamina, NBT, sounds, tame particles via entity events.
- Base: `claimReactTargetGoalIndex`/`claimSpecialTargetGoalIndex`, `getDistanceFromHome()`, `strafe()`,
  and `requiresCustomPersistence()` -> `isPersistant()` so tamed pets don't despawn.

**Deviations from the official behaviour (all marked `TODO(port)`):**
- Taming does **not** yet require Beastiary knowledge rank >= 2 (needs `ExtendedPlayer`).
- The owner's sneak-right-click (empty hand) **toggles sitting** instead of opening the pet GUI (Phase 8).
- Leashing: any tamed creature can be leashed (1.21 `canBeLeashed()` has no player argument).
- Not ported: pets system hooks (PetEntry/SummonSet/temporary minions/soulstone), charge/equip commands,
  perching, ranged owner kill credit, mob-event spawn tracking, boss health bar hide, portal-time clamp,
  breeding owner copy (S202 cuts breeding).

**Superclass debt paid:** 44 ported creatures had been parked on `BaseCreatureEntity`/`AgeableCreatureEntity`
because the tameable class was a stub (PORT_PLAN previously undercounted this as 21). All 44 now extend their
official superclass (29 `TameableCreatureEntity`, 15 `RideableCreatureEntity`); a full audit shows **0
superclass mismatches** vs official, and every override of a tameable-relevant method calls `super`. Each file
got a "PHASE 5e UPDATE" note in its class Javadoc. `EntityDarkling.canAttackType` forced the tameable
override to use `EntityType<?>`.

**Next candidates:** `RideableCreatureEntity` (15 mounts), custom navigation (fly/swim/climb), or the pets
system (`ExtendedPlayer` + PetEntry + soulstones + Beastiary knowledge) which also restores the rank-2 rule.

## Phase 5g: custom navigation + movement layer (2026-09-26)

Ported `CreaturePathNavigator`, `CreatureNodeProcessor`, `CreatureMoveController` (+ `ICreatureNodeProcessor`) for
1.21.1 and the base-class movement layer. Arena node classes (`ArenaNode*`) deferred to the boss/arena phase.

**1.21.1 pathfinding API changes:** `NodeEvaluator.getGoal()` -> `getTarget()`; `getBlockPathType(BlockGetter,x,y,z,Mob)`
-> `getPathTypeOfMob(PathfindingContext,x,y,z,Mob)`; evaluator `level` field gone -> `currentContext.getBlockState()`;
`BlockPathTypes` -> `PathType`; `BlockState.isPathfindable(level,pos,type)` -> `isPathfindable(type)`; MoveControl's
`getBlockPathType(level,...)` -> `NodeEvaluator.getPathType(Mob, BlockPos)`. Vanilla has no `createMoveController()`
hook - assigned in the constructor like the official source.

**Base class:** `createNavigation()` -> `CreaturePathNavigator`, `createMoveController()`, `shouldSwim()`, `travel()` now
routes to `travelSwimming()`/`travelFlying()` (fliers bypass vanilla gravity, like the official), full water/lava
pathfinding malus in `initializePathing()`, `shouldFloat()`/`shouldDive()`, `daylightBurns()`, `canFreeze()`,
`onClimbable()` + `CLIMBING` synced flag + `setBesideClimbableBlock()`, trimmed `tickMovementRuntime()` (fire clear,
non-walker land-lock, climb flag), `causeFallDamage()` (fliers immune; fall resistance TODO).

**Pre-existing bug found + fixed - all water creatures drowned:** the earlier port renamed `canBreatheUnderwater()` to
`creatureCanBreatheUnderwater()` (1.21 made the vanilla one final/tag-driven) but never hooked the replacement, so
vanilla drowned every fish in water (Silex/Abaia died in ~20s). Fixed with NeoForge `canDrownInFluidType()` + the
official `increaseAirSupply()` override + trimmed `tickEnvironmentalState()` (water damage, suffocation on land for
non-air-breathers; daylight burning TODO).

**Verified headless (runServer + RCON, 0 ERROR/FATAL):** fliers (Vespid x3, Epion, Grigori) stay airborne and wander;
strong swimmers (Abaia, Ioray, Silex) survive in a sealed water tank with full air and move; walker (Warg) lands and
wanders; Cephignis (lava fish) correctly dies in water. **Not verified:** climbing (needs a target to path to), client
visuals (limb swing/animation).

**OPEN ISSUE - lava fish don't move in lava:** Cephignis survives in lava (breathing OK) but stays at its spawn point.
Two S202 fixes applied (not in the official source, which likely has the same bug): `isInSwimmableFluid()` so the
navigator/node processor treat lava as swimmable for lava creatures, and lava nodes accepted in
`isSwimmablePathNode()` (vanilla `LiquidBlock.isPathfindable()` is always false for lava). Still static - next
suspects: `WanderGoal`/`RandomPositionGenerator` target choice in lava, or path following. Only affects lava
swimmers (Cephignis); park unless Glenn keeps it.

Silex swims along the tank floor rather than mid-water - may be normal wander targeting, unconfirmed.

## Phase 6a: projectiles + creature ranged attacks (2026-09-26)

**Ported:** `ProjectileInfo` (JSON projectile definitions), 8 of 9 `ProjectileBehaviour`s (placeBlocks, explosion,
fireProjectiles, drainHealth, randomForce, laser, randomEffect, catch), `BaseProjectileEntity`, `CustomProjectileEntity`
(+ `CustomProjectileModelEntity`, `ModelProjectileEntity`), `ProjectileManager` (JSON part), `AttackRangedGoal`,
`FireProjectilesGoal`, and a 1.21 `ProjectileSpriteRenderer` (client). All 39 JSON projectiles registered as entity
types through `ObjectManager` (same path as creatures) - "Registered 113 entity types" = 74 creatures + 39 projectiles.
Assets: 39 projectile JSONs, 72 charge sprites (`textures/item/charges`, the sprite source) + 5 model-projectile
textures, 45 projectile sounds; **new `sounds.json`** (54 entries: projectile + existing block sounds).

**Base class additions:** `getRangedCooldown`, `getEffectDuration/Amplifier/Strength`, `applyDebuffs/applyBuffs`,
`attackRanged`, `doRangedDamage`, `fireProjectile` (by name / class / instance), `resetAttackCooldown`,
`nextAttackPhase`, `hasDirectNavigationTarget`, `current{Combat,Idle}GoalIndex`, boss `playerTargets`
(`addPlayerTarget`/`forEachPlayerTarget`, tracked in `hurt()`).

**Pre-existing bug fixed:** `applyContactAttackEffects()` was an empty stub, so **melee hits applied no element
debuffs** (no poison/burn/etc on hit). Restored from the official source.

**1.21 translation notes:** Forge `NetworkHooks` spawn packet dropped (vanilla); `defineSynchedData(Builder)` (with
explicit defaults since it runs before field init); `getGravity()` -> `getDefaultGravity()` (double); `portalTime` ->
`setPortalCooldown()`; `AttackEntityEvent` -> NeoForge's; `ForgeRegistries` -> `BuiltInRegistries`;
`new ResourceLocation(s)` -> `ResourceLocation.parse(s)`; effects need `Holder`s (`MOB_EFFECT.getHolder(...)`);
vertex API `vertex/uv/uv2/endVertex` -> `addVertex/setUv/setLight` (no endVertex).

**Creatures restored (17):** ranged-only (substitute melee replaced): Remobra (venomshot), Troll (boulderblast),
Afrit (scorchfireball), Arix (icefireball), Clink (throwingscythe), Erepede (mudshot), Epion (bloodleech), Belphegor
(doomfireball), Apollyon (doomfireball rain; its aura/chase/minion goals still unported). Ranged added alongside melee:
Argus (chaosorb), Astaroth (devilstar), Behemophet (hellfireball), Brucha (14-quill volley, melee disabled like official).
FireProjectilesGoal: Chupacabra (rare variant chaos orbs), Conba (poop). Lasers: Eyewig (poisonray), Ioray (waterjet).
Each got a "PHASE 6a UPDATE" class-doc note.

**Verified (runServer + RCON, night, no-AI pig as attacker to trigger revenge, temporary debug logging - removed):**
Remobra/Afrit/Epion/Brucha all called `attackRanged`, spawned their projectiles, and the projectiles hit (pig 10 ->
0.4 HP); Eyewig's laser spawned and damaged. 0 ERROR/FATAL. **Testing gotchas:** trolls petrify in daylight (test at
night); projectiles hit within 2-8 ticks so polling misses them - log instead; the port's targeting mostly only
hunts players, so villagers/pigs are only engaged via revenge.

**Not ported (TODO(port)):** charge items (ChargeItem - throwable player items + pet levelling, which S202 cuts),
dispenser behaviour, the `summon` behaviour (pets system), "old" hardcoded projectiles (lasers' end entities, rapid
fire, summoning portal, hellfire/devil gatling/hell shield boss projectiles), OBJ model projectile rendering (5 model
projectiles render as sprites), `ProjectileEquipmentFeature`.

**Known gap found:** creature sounds - the port had no `sounds.json`, so all creature sounds are silent; only
projectile/block entries were added here (creature .ogg files not copied yet, ~19 MB).

## Creature sounds (2026-09-26)

Server side was already fine: `CreatureBootstrapHelper.registerSounds()` adds every creature's say/hurt/death/step/
attack/jump/fly (+ tame/beg/eat/mount/phase) sound, and - like the official source, whose `registerSounds` RegisterEvent
is commented out - sound events aren't registry-registered; vanilla sends them to the client directly by id. The client
had nothing to play: no `sounds.json` and no creature `.ogg` files. Copied `sounds/entity` (819 files), `item`, `effect`
and the rest of `block`; `sounds.json` now has 884 entries, all resolving to real files (checked). Every ported creature
has say/hurt/death entries. Mob-event sounds (33 entries, 16 MB) intentionally left until the mob events phase.
Also restored the `isEntityClassAssignableTo(TameableCreatureEntity.class)` condition for tame/beg sounds (dropped
earlier because the class was a stub). Jar grows to ~22 MB. Not audibly verified (headless) - needs an in-game listen.

## Remaining creatures - batch 1 of 3 (2026-09-26, Glenn: "port the rest")

28 creatures, copied from official and translated (more faithfully than the earlier trimmed batches - missing base
helpers were added instead of cutting behaviour): tpumpkyn, necrovore, sutiramu, vorach, lycosa, frostweaver, reiver,
triffid, grell, shambler, raidra, tremor, reaper, nymph, sylph, ent, wendigo, eechetik, wraith, xaphan, treant, shade,
wisp, volcan, spriggan, vapula, naxiris, cockatrice.

**Custom mob effects now exist (fixes a big pre-existing gap):** the port never created or registered Lycanites' 26
effects (paralysis, fear, weight, plague, leech, ...), so every `lycanitesmobs:*` element debuff/buff silently did
nothing and food effects failed ("Unable to add food effect" warnings - now gone). Added `EffectManager.createEffects()`
(official list), `RegistryEvents.registerEffects()` (MOB_EFFECT RegisterEvent), `ObjectManager.getEffectHolder()` for
1.21 Holder APIs, and the 26 effect icons. **Effect behaviour is still TODO** - most of it lives in the ExtendedEntity
capability / FearHandler / client handlers in the official source (paralysis immobilising, fear fleeing, etc.).

**New base helpers:** `leap()` x3 (NeoForge `CommonHooks.onLivingJump`), `getFallResistance()` (now applied in
`causeFallDamage`), `onDamage()` + `getDamageModifier()` (wired into `hurt`), `getBrightness()`, `canBeTargetedBy()`,
`isLookingAtMe()`, `isPetType()` (false until pets), `getPickupEntity()`/`dropPickupEntity()`, `isSafeToLand()`,
`getGroundY()`/`getAirY()`/`restrictYHeightFromGround()`, `destroyArea()` x3 (spawner trigger TODO), `webProof()` +
`makeStuckInBlock()` web immunity, fields `fleeHealthPercent`/`solidCollision`/`damageTakenThisSec`. Rideable stub
gained `abilityToggled` + `isEntityPassenger()`.

**Translation conventions (scratchpad normalize.py, re-derive if needed):** drop `MobType` attribute lines;
`setMaxUpStep(x)` -> STEP_HEIGHT attribute; distmarker -> `net.neoforged.api.distmarker`; mount overrides
(mountAbility, getPassengersRidingOffset, getMountedZOffset, riderEffects, ...) and bag-size overrides keep their bodies
but lose `@Override` with a `TODO(port): restore @Override` comment - **restore these when RideableCreatureEntity is
ported**; `getDimensions(..).width/height` -> `getBbWidth/Height()`; `canBreatheUnderwater()` overrides ->
`creatureCanBreatheUnderwater()`; `setSecondsOnFire` -> `igniteForSeconds`; Forge AttackEntityEvent -> NeoForge's
(`.isCanceled()`); `ForgeHooks.onLivingAttack` pre-checks dropped (noted); spawnsInWater/Underground flags dropped
(JSON spawn config); `ObjectManager.getEffect()` in effect APIs -> `getEffectHolder()`.

Deferred inside batch 1: Shade's `EntityFear.spawnForPlayer` haunt entity (with effect behaviour); Spriggan uses
`BushBlock` instead of the removed `IPlantable`.

Verified: runServer - "Registered 26 mob effects", "Registered 141 entity types" (102 creatures + 39 projectiles),
0 food effect warnings; all 28 summoned and ticked 20s together at night (27 alive - Spriggan died in the melee brawl,
survives fine alone); paralysis applies to a villager; 0 ERROR/FATAL.

## Remaining creatures - batch 2 of 3 (2026-09-26)

15 creatures: crusk, ika, zephyr, kathoga, jengu, pixen, umibas, serpix, cherufe, cinder, grue, geonach, ignibus, yale,
quetzodracl. (Amalgalich and Malwrath moved to batch 3 - boss infrastructure / Asmodeus reference.)

**Shared pieces ported:** StealthGoal, TemptGoal, EatBlockGoal, FindNearbyPlayersGoal, CopyMasterAttackTargetGoal,
PlayerControlGoal, IFusable, CustomItemEntity, RapidFireProjectileEntity, WormBurrowTerrain.
**Old (hardcoded) projectile support:** `ProjectileManager` now registers old projectiles through ObjectManager and binds
their types after registration; `rapidfire` is the first (boss ones come in batch 3). Client sprite renderer registered.
Port improvement: rapidfire uses its fired projectile's launch sound (the official looked up an unregistered name ->
silent volley + "Null Sound" log).
**Base helpers:** onEat, applyDropEffects, dropItem, canStealth/startStealth, canBeTempted, shouldCreatureGroupFlee,
hasRiderTarget, clearPlayerTargets, getNearestEntity, getPickupOffset, pickupEntity/canPickupEntity (capability-carrying
TODO), transform() (solo + basic fusion state; minion/temporary/level copying TODO), spawnsInBlock. Rideable stub:
no-op riderEffects/mountAbility (mount creatures' overrides now genuinely override).
**1.21 notes:** Yale -> NeoForge IShearable (no fortune param), CraftingInput + RecipeHolder for dye mixing,
DyeColor.getTextureDiffuseColor() int; canBeLeashed(Player) -> canBeLeashed(); BlockPathTypes -> PathType.

Verified: runServer - "Registered 157 entity types" (117 creatures + 39 JSON projectiles + rapidfire); all 15 summon and
tick; Ignibus/Serpix triggered via pig revenge fire rapidfire without crashing; the 3 that died in the 15-creature brawl
(pixen, serpix, geonach) survive alone. 0 errors after the rapidfire sound fix (the only error was that null sound).

## Remaining creatures - batch 3 of 3: bosses (2026-09-26) - ALL 121 CREATURES NOW PORTED

Rahovart, Asmodeus, Amalgalich, Malwrath. Belphegor + Behemophet **re-ported from official** (the old trimmed versions
lacked the hellfire energy Rahovart feeds on). Only `fear` (dummy haunt entity for the fear effect) remains - it comes
with the effect-behaviour / ExtendedEntity work.

**Boss infrastructure added to the base:** minion system (summonMinion + prep, setMinion/isMinion, add/has/getMinions,
onMinionUpdate/onMinionDeath via die(), onTryToDamageMinion, tick pruning), arena centre, damage caps (`damageMax`
per hit in hurt(), `damageLimit` per second via enforceDamageLimit + isInvulnerableTo), battle phases
(updateBattlePhase hook each tick, get/setBattlePhase with phase sound + name), **boss health bar**
(ServerBossEvent: red for bosses, green for rare variants when enabled; added/removed on start/stopSeenByPlayer;
slow heal when no player targets), attackHitscan, nearbyCreatureCount, getPlayerTargetCount, TARGET_TYPES.
Tameable: tamed creatures hide the boss bar. Goals: FaceTargetGoal, HealWhenNoPlayersGoal, SummonMinionsGoal,
EffectAuraGoal (now Holder-based), ForceGoal, GrowGoal, SuicideGoal, BreakDoorGoal + DoorInteractGoal.
Arena nodes (ArenaNode/Network/Grid), SchismMath. **Boss projectiles registered** as old projectiles: hellfirewall,
hellfireorb, hellfirewave(+part), hellfirebarrier(+part), devilgatling. `DeferredLevelActionManager.spawnEntityNow`.
Astaroth/Trite `canAttack` exclusions for Malwrath/Asmodeus restored.

**BIG pre-existing bug fixed - `onFirstSpawn()` had been trimmed to one line.** Consequences until now: every creature
spawned at 20 HP regardless of max health (Troll 20/32, bosses ~20/3200), **no uncommon/rare variant ever spawned
naturally**, and sizes never varied. Restored the official version (starting level -> refreshAttributes -> full heal,
random subspecies/variant, random size); pet-entry check TODO. Verified: Troll spawns 83.2/83.2 (level-scaled), bosses
~3100-4160 HP, 40 Wargs -> 35 base / 3 Ashen / 2 Dark, all 40 with random sizes.

Verified (runServer): 168 entity types (121 creatures + 39 JSON projectiles + 8 old projectiles), all 4 bosses spawn,
tick and hold health, 0 ERROR/FATAL. **Not verified: boss fights** - boss AI targets players (nearby players,
arena, phases), so a headless pig doesn't engage them; needs an in-game fight. Boss hellfire textures other than
hellfireorb/hellfireball may render as missing-texture sprites (their own textures/models not checked).

**Leftover TODOs across the creature batches:** mount overrides marked `TODO(port): restore @Override`
(RideableCreatureEntity), effect behaviour (ExtendedEntity/FearHandler) incl. EntityFear, pickup carrying,
minion temporary/spawn-event state, transform fusion levels/minions, spawner trigger in destroyArea.

## Real models for all 121 creatures (2026-09-27)

Ported the 6 remaining templates (aquatic, arachnid, biped, dragon, elemental, quadruped), all 84 remaining
new-format model classes, and the legacy `CreatureObjModelOld` system plus its 39 models. The leaf models port
verbatim (only the distmarker import changes). `CreatureObjModelOld` got the same trims as `CreatureObjModel`: plain
`ObjModel` instead of `VBOObjModel`, `ResourceLocation.fromNamespaceAndPath`, and **no `entity.getScale()`
re-application** (vanilla's renderer already scales the pose stack). Legacy models animate immediately at render
time (doAngle/doRotate), so they needed no changes to the `setupAnim`/`renderToBuffer` hooks. `ModelManager
.reloadModels()` now reloads legacy models as well.

Verified: compile clean; 124/124 model class refs in creature/subspecies JSON resolve; every `initModel` path has its
`.obj` (only concapede segment lacks a `_parts.json`, which is optional for legacy models); every base/subspecies
texture exists. In `runClient` with the `resources`/`models` debug keys on, after the resource reload 85/85 new-format
and 39/39 legacy models loaded with zero failures (the constructor-time "Unable to load" warnings before the reload
are expected). **Glenn confirmed in-game that all creatures render with correct textures.** The placeholder
renderer now only covers creatures whose model fails to load.

Still not ported on the client side: equipment/saddle layers (`addCustomLayers` in CreatureModel), projectile OBJ
models (all projectiles render as sprites), `_animation.json` support, and per-entity subspecies model swapping
(CreatureRenderer resolves a single model when it's constructed).

## Mounts: RideableCreatureEntity, creature inventory, rider controls (2026-09-27)

**Ported:**
- `RideableCreatureEntity` (real, replaces the stub): owner-only mounting with a saddle ("Mount" interact command),
  steering/jumping/flying/swimming/lava movement, mount ability (G), rider effects every 10 ticks (debuff protection,
  fire resistance for fire-immune mounts, water breathing), mount melee, rider team/alliance, passenger damage
  immunity, dismount handling (sitting mounts re-home), `canBeControlledByRider()` (Lycanites' own check, now used by
  `CreatureMoveController` again). 1.21.1: player-ridden movement goes vanilla `travelRidden -> travel()` on the
  controlling client; `getPassengersRidingOffset()/getMountedZOffset()` are Lycanites methods on the base, applied in
  `positionRider()` with the 1.20.1 player offset (-0.35) so every creature's tuned offsets still apply;
  `setFlyingSpeed` dropped (vanilla's ridden `getFlyingSpeed()` equals the official glide default).
- `CreatureInventory` (no GUI yet): chest/saddle/bag slots, synced equipment data, NBT save/load, drops on death (not for
  bound pets), armor value, 1.21 ContainerHelper/armor-material Holders. "Equip Item" command in Tameable (saddles,
  horse armor, chests). Verified via RCON: saddle + horse armor round-trip in the right slots per creature type.
- Networking: first NeoForge payload (`PlayerControlPayload`, client -> server control bitmask), `PacketManager`.
- `ExtendedPlayer` (trimmed to control states) as a NeoForge data attachment, not serialized.
- `KeyManager`: mount descend (Left Alt), mount ability (G), mount inventory (K); client tick syncs control states.
  The official dismount key was dead code (vanilla sneak-dismount is used), not registered.
- `RevengeRiderGoal` (vanilla `getLastHurtMob` in place of ExtendedEntity's identical last-attacked tracking),
  `CopyRiderAttackTargetGoal`.
- Saddle + chest-armor render layers (`LayerCreatureSaddle`, `LayerCreatureEquipment`, 1.21 DYED_COLOR tint).
- All `TODO(port): restore @Override` markers restored (compiler-checked).

**Big finding: 18 of the 26 mounts were early heavy trims** (Sep 22-26, before the base classes existed): no mount
ability, rider effects, riding offsets, stamina, flight/pickup/aiStep logic. Re-ported all 18 from official (Maug, Roc,
Feradon, Warg, Barghest, Stryder, Raiko, Salamander, Ioray, Epion, Erepede, Eyewig, Morock, Roa, Thresher,
Ventoraptor, Uvaraptor, Zoataur). Per-mount method audit vs official now shows 0 gaps (bar the intentional
`canBreatheUnderwater -> creatureCanBreatheUnderwater` rename). Pickup carrying (`ExtendedEntity.setPickedUpByEntity`)
and "skip targets picked up by another mob" left as TODOs for the ExtendedEntity work. Added `hasSpawnEventType()` and
`hasMaster()` to the base (the latter server-side only - **target bits (TARGET data) are defined but never synced to
clients**, a separate gap).

**Two pre-existing bugs found and fixed along the way:**
- `CreatureModel.addCustomLayers()` was never called, so all 31 models' effect layers (glowing eyes, scrolling, dye
  etc.) were built but never attached. Now attached in `CreatureRenderer`'s constructor.
- **Custom fluids crashed the client on sight**: `FluidManager`'s doc said client extensions were registered in
  ClientSetup, but they never were, so NeoForge had no sprites (`FluidSpriteCache` NPE, "Tesselating liquid in
  world"). Added `ClientSetup.registerClientExtensions` (still/flowing textures, fog color + official 1-6 fog range;
  default white tint since the textures are pre-colored - the official passed the fluid color as tint with no alpha).

**Verified:** compile; `runServer` (168 entity types, inventory NBT round-trip); `runClient` into a staged world with
all 26 mounts saddled + the 23 non-boss layered creatures, auto-toured via a datapack (no input injection): no crash,
saddle harnesses and effect layers visible in screenshots, acid fluid renders. **Not verified: actually riding** -
mounting, steering, jump/fly/descend keys and mount abilities need Glenn in-game (Lycannots, deployed).

**Known gaps:** unequipping a saddle needs the creature GUI (not ported); mount inventory key does nothing yet;
no mount HUD/stamina bar.

## Pet system + GUI (2026-09-27)

Glenn: "port the rest... the pet system needs to be ported before I can even really test it". LYCANITES-REVIEW: pets
and summoning stay unchanged for the first version, so this is a faithful port.

**Core (commit 5aef874):** `ExtendedPlayer` as a NeoForge data attachment (serialized to player NBT, `copyOnDeath()`
replaces the capability + clone backup; the client player gets its own instance filled by sync messages): spirit,
summoning focus, 5 summon sets, Beastiary, `PetManager`. `PetEntry`/`SummonSet`/`CreatureKnowledge`/`Beastiary`.
Knowledge from proximity (10 blocks), kills and treats; **taming requires rank 2 knowledge again** (official).
Soulstone + per-type filled soulstones (spawn a random tameable of that type, already bound), Soulgazer (study), Soul
Contract (transfer pets), 5 summoning staffs + `PortalEntity` (as an old projectile, "summoningportal"). Bound-pet
lifecycle: orphaned bound pets (reloaded without their entry) are discarded, the entry respawns its own; temporary
minions count down and despawn (the official despawnCheck isn't ported, so this runs in aiStep); IsMinion/IsTemporary/
IsBoundPet NBT. Real `DeferredLevelActionManager` queue (spawns wait for loaded chunks, e.g. pets on login).
**Dropped deliberately:** `PlayerFamiliars` (official online Patreon familiar service: per-player fetch from
service.lycanitesmobs.com with SSL certificate checks disabled) and the online `VersionChecker` (same pattern) -
replaced by an offline stand-in that only shows the running version.

**GUI:** Beastiary (index, creatures, pets, summoning, elements), minion selection (R, held), creature GUI (owner
sneak-right-click - replaces the port's temporary sneak-to-sit) with `CreatureContainer` menu (inventory, saddle/bag/
armor slots, pet commands), HUD layer (summoning focus, mount stamina via the 1.21 jump-bar sprites + controls hint,
taming reputation bar), F3 creature debug text (config). Keys: B index, R minions, unbound beastiary/pets/summoning.
`/lm beastiary complete|clear|add`, `/lm creatures reload`. 1.21 changes: `BaseList` is an `ObjectSelectionList`
(`AbstractSelectionList.Entry` is only reachable through it; widget position replaces x0/x1/y0/y1; panel/scrollbar drawn
with GuiGraphics fills), `DrawHelper` on the 1.21 tessellator API, `Player.openMenu(provider, buf)`,
`IMenuTypeExtension`, `PET_COMMAND_ID` moved to `BaseCreatureEntity` with the official numbering (the port's Tameable
copy was renumbered, which would have broken GUI command ids). Official inventory tabs (TabManager) not ported: they
reflect on an obfuscated Screen field and official only invoked them when `screen.getClass() == Screen.class`, so they
never ran.

**Client sync pass (big pre-existing gap):** the port defined SUBSPECIES/VARIANT/TARGET/ATTACK_PHASE/ANIMATION_STATE data
slots but never wrote them - clients never saw a creature's variant (base textures only) or size, and had no target/
animation state. Ported official `onSyncUpdate()` (+ SIZE slot, client-side target-bit getters) and the per-player
reputation message (`MessageCreature`) the taming bar needs. ARENA isn't synced (TODO).

**Also fixed:** recipes lived in `data/lycanitesmobs/recipes/` (1.20 path) so none of the 36 cooking recipes ever
loaded - moved to `recipe/`; added the pet recipes (1.21 `id` result format).

**Verified in a real client** (temporary test driver, removed): with rank-2 knowledge, 7 treats tamed a Warg; saddle
equipped (synced); soulstone bound it as a mount (server + client entry); all 6 screens open; sneak-click opened the
creature GUI; mounted, rode ~24 blocks, dismounted; staff summoned 3 Geken from a summon set. After a save/reload the
entry respawned the Warg tamed + saddled + bound (orphan discarded), and GUI/riding worked again. Screens render
correctly at a normal GUI scale (screenshots checked); at the minimum 320-wide GUI the Beastiary is cramped. The
summoning screen's variant list overlaps the action labels - identical coordinates to official.

**Remaining pet-adjacent TODOs:** summoning pedestal (block entity + screen), perching, ExtendedEntity (pickup carrying,
fear), Charge items, mob-event titles in the HUD.

## Phase 6b: world spawning - spawners, triggers, structure spawns (2026-09-28)

Faithful port of the official spawner system; S202 spawn tuning (common/rare rates, loosened biomes, trigger keep-list)
is still data work for later - all 47 official spawner JSONs are copied unchanged. Before this, **no creature ever
spawned naturally** (official Lycanites uses only its own spawners, never vanilla biome spawn lists).

**Ported:** `SpawnerManager` (+ `globalspawner.json`), `Spawner`, `MobSpawn`, `SpawnerMobRegistry` (creature JSON
`"spawners"` lists re-enabled in `CreatureSpawn`), `SpawnerTriggerDispatcher`, all triggers except `mobEvent` (world,
player, kill, entitySpawned, chunk, block, ore, crop, tree, mix, sleep, fishing, explosion), all conditions (world,
player, event, date, group), all locations (base, random, block, material, structure), `SpawnerEventListener`,
`StructureSpawnInjector`, `/lm spawners reload|list|creative`, `/lm spawner test <name> <level>|lighttest`.
`ExtendedWorld` (trimmed SavedData: spawner tick, day base time, boss tracking, saved world-event fields) + `BossEntry`.
`BaseCreatureEntity`: the official spawn-check chain (`checkSpawnRules` -> light/collision/group limit/dimension/
liquid/underground/boss proximity, `checkSpawnLimits`), persistence (`isPersistenceRequired`/`canDespawnNaturally`/
`forceNoDespawn`), the full `despawnCheck()` (disabled creature, temporary, peaceful, stale event spawns) replacing the
temporary-only version, boss arena tracking + boss block break/place protection (`PlayerEventListener`), and
`destroyArea` now fires block spawn triggers. Restored `spawnsInWater/OnLand/Underground` on 39 creatures and the
Vespid/Vespid Queen `isPersistant()` overrides that the early trims dropped.

**1.21 / NeoForge changes:** load in common setup (spawner JSON resolves blocks/items/Material lists, which only exist
after registries freeze) not the constructor; `LevelTickEvent.Pre`/`PlayerTickEvent.Post`; harvest trigger on
`BlockDropsEvent` (carries the tool; fortune/silk holders looked up from the enchantment registry); sleep on
`CanPlayerSleepEvent.setProblem`; entitySpawned on `FinalizeSpawnEvent`; fresh chunks from `ChunkEvent.Load#isNewChunk`
(no ChunkSpawnFeature); `MobType` -> entity type tags (undead/arthropod/aquatic/illager, "undefined" = none);
`IPlantable` -> BushBlock/Cactus/SugarCane (+vine); `IFluidBlock` check dropped (no block ever implemented it);
`Explosion.getDirectSourceEntity()/center()`; `Level.getSharedSpawnPos()`. Structure spawns use a real NeoForge
**structure modifier** (`lycanitesmobs:json_structure_spawns`, one datapack entry in `data/lycanitesmobs/neoforge/
structure_modifier/`) instead of the official's mixin accessor hack.

**Deliberate deviations:** (1) the official built the `SpawnPlacementCheck` event but never posted it; it is posted
now, so other mods can veto/force. (2) The official never called `finalizeSpawn` for spawner spawns; the port calls
`EventHooks.finalizeMobSpawn` for every spawn, so other mods' `FinalizeSpawnEvent` hooks (mob scaling, spawn control)
see Lycanites spawns, and a cancel from them is honoured. (3) Non-Lycanites `mobId`s in spawner JSON are resolved
lazily (official only resolved them for dungeons, so they never spawned). (4) `applySpawnerSpawnState(forceNoDespawn)`
also sets vanilla's saved persistence flag (official's field was lost on reload). (5) `/lm spawner test` from a console
uses the source position, not 0,0,0.

**Deferred:** mob event spawners (`mobevents/` spawner entries, `MobEventSpawnTrigger`, `applySpawnEvent` tagging,
`MobEvent.onSpawn`), dungeon state in `ExtendedWorld`. `ExtendedWorld.getWorldEvent()`/`getMobEventPlayerServer()`
return null until mob events exist, so event-gated spawners stay off. Official quirks kept: `BlockSpawnLocation` never
scans below y=0 unless `yMin` is set; `isBlockUnderground` only counts plant blocks as cover; `chunk_animal`/
`chunk_water_animal` spawners have no creatures assigned in official data (inert).

**Verified headless** (runServer + RCON): server loads clean; `/lm spawner test land 1` spawned 3 Herma (the only
biome-valid land mob at spawn) including a Russet variant; 40 TNT -> 1 explosion trigger (5%) -> 1 Tremor; structure
modifier injected Sylph into desert/jungle pyramids + stronghold and Aegis into the 5 vanilla villages; `/lm spawners
reload` then another trigger worked. Note the dev test world has `doMobSpawning=false` (spawners respect it; restored
after testing). **Not verified**: the `world` trigger (it needs real players - the main natural spawn path), player/
kill/block/ore/fishing/sleep triggers, and actual structure spawning in-game. Deployed to Lycannots.
Found: `/kill` doesn't remove bosses (damage caps) - old Rahovarts from boss tests are still in the dev world.

## Remaining items + blocks, with placeholders (2026-09-28)

Glenn: "port the rest of the items before we start removing anything and keeping them non functional as a
placeholder (displays message in chat)". Every official item and block now exists. Items/blocks whose system isn't
ported call `PortPlaceholder.notifyNotFunctional(...)` when used, which sends "`<name>` is not functional yet due to
`<system>` not being ported." to chat (`lyc.port.placeholder`). Remove each call when its system lands.

**Real ports:** 3 soulcubes (plain blocks, now with a self-drop loot table; the official had none, so they dropped
nothing), Halloween Treat / Winter Gift / Large Winter Gift (give random loot or spawn a "trick" creature; restored
`ObjectLists.addEntity`; fixed the official lists' stale food ids like `mosspie` -> `moss_pie`, which silently left
every Lycanites food out of the treats), 59 **equipment parts** (JSON data, levels/experience/sharpness/mana on
CUSTOM_DATA, elements, all 7 feature types parsed; drop from their creature at `dropChance` via `setupMob`, like the
official; own creative tab), and their **OBJ item renderer** (`EquipmentPartRenderer` + `ItemObjModel`/
`ModelEquipmentPart`, official per-part transforms unchanged, plain buffered rendering instead of the VBO/Iris path;
also ported `ModelAnimation`/`ModelPartAnimation`/`TextureLayerAnimation`/`LayerItem`/`LayerItemDye`, which the
creature pipeline had skipped - 12 parts ship animation/glow layers). Part tooltip is plain text (level/xp, slot,
elements, feature lines); the official's `[[BAR:..]]` markup needs the equipment tooltip handler.

**Placeholders:** `equipment` (assembled item; no parts, so invisible like the official, `/give` only; not in any tab,
as official), 3 soulkeys (altars), 59 parts on right-click (equipment), 3 Equipment Forges + Station + Infuser
(`PlaceholderFacingBlock`: facing + official models, no block entity/menu), Summoning Pedestal (keeps the `owner`
state for its 3 models; the pedestal block entity isn't ported). Equipment feature *behaviour* (projectiles need
ExtendedEntity cooldowns, harvest/summon/effect on hit) is ported but only reachable from assembled equipment.

**Assets/data:** item/block models + blockstates + textures copied referentially; `modelParts/equipment` -> lowercase
`modelparts/equipment` (189 files), `textures/equipment` (65), 59 part JSONs, 21 recipes (1.21 `id` results, `recipe/`),
6 block loot tables (`loot_table/`), `tags/block/equipment_harvest`. **Asset bug found + fixed:** the earlier
referential copy script stopped at a same-named *model* when a texture shared its path (`block/soulcubeundead`), so
the soulcubes, `propolis`, `veswax` and 2 pedestal textures were never copied (missing-texture blocks). Re-verified
every `textures` entry in every port model: only `smitefireballcharge` is missing, and it is missing in the official
mod too.

**Verified:** runServer loads clean (137 blocks, 129 block items, recipes/loot load without errors); a real client
(temporary test driver, removed) showed parts rendering as 3D models in the hotbar, the part tooltip, holiday items,
soulcubes, forges, the pedestal and the chat message on a forge.

## Creature method audit (unlogged session 2026-09-28, finished + logged 2026-09-29)

A session after the items/placeholders work above ran the Sep 27 lesson ("audit method sets against reference/")
across every creature, then ended before logging it. Everything below was found uncommitted on 2026-09-29, compiled
clean and was re-audited. Scope: 59 creature classes (53 carry a "Restored from official 2026-09-28" marker) regained
trimmed methods, fields, constructor values and goals, for example Calpod's swarm allies and block chewing, Maka's
tempt and alpha-following, and bag sizes. `BaseCreatureEntity` gained about 1,000 lines of the hooks those methods
need. Also added: **ExtendedEntity** (NeoForge data attachment, ticked from `EntityTickEvent.Post`; pickup carrying,
perching, projectile cooldowns, safe position and forced removal; not serialized because the official NBT methods were
no-ops), `EntityEventListener` (the entity half of the official GameEventListener), sync messages for picked-up and
perched entities plus `MessageScreenRequest`, 25 AI goals the early trims had dropped (all used), and laser
projectiles (`LaserProjectileEntity`/`LaserEndProjectileEntity`, Hell Laser/End/Shield, Shadowfire Barrier).

**Finished 2026-09-29:** the audit script (method names per class, `comm -23` official vs port) still flagged 9
`getDamageModifier` overrides (Troll pickaxe x3, Vespid/Vespid Queen fire x2, Afrit/Khalk/Cephignis fire x0, Skylus
shell x0.25, Maka Alpha x2 vs other alphas, Aegis x2 when not blocking). The early ports had dropped them as "not a
real hook", but `BaseCreatureEntity.hurt()` applies the modifier now, so they were restored and the stale header notes
fixed. Everything else the audit reports is a rename or dead code: `canBreatheUnderwater` -> `creatureCanBreatheUnderwater`,
`canPickupItems` -> `canPickUpLoot`, Kobold `onRemovedFromWorld` -> `remove()`, `getBrightnessForRender` (no caller),
Yale's anonymous crafting-menu methods. Only `fear` (dummy entity) and its two goals remain unported.

**Verified headless** (runServer + RCON): loads clean (174 entity types, 137 blocks, 129 block items); Vespid took 1.5
from 1.5 generic and 3.0 from 1.5 fire; Skylus took 1.2 from an 8 hit at full health. Afrit still takes 1 from fire:
the official `getDamageAfterDefense` floors every hit at 1, so that is faithful.

**Official quirks kept:** Calpod's wood chewing calls `destroyAreaBlock(..., WoodType.class, ...)`. `WoodType` is not
a block class, so it never matches and Calpods never break wood, in the official mod as well. Worth fixing (e.g. a
`BlockTags.LOGS` check) if S202 wants the behaviour.

**Dev world note:** the Asmodeus and Rahovart from earlier boss tests survive `/kill` (damage caps) and keep firing.
Asmodeus's devilstar stream piles up about 85 frozen projectiles per burst at z=-64, the edge of the entity-ticking
spawn chunks when no player is online, which had reached 81,807 entities. That is a headless artifact (vanilla freezes
any projectile outside ticking chunks), not a lifetime bug, but it slows the dev server; remove those bosses when
convenient.

## Phase 6c: mob events + custom effect behaviour (2026-09-29)

Faithful port of the official mob event system with all 33 official events and 40 event spawners unchanged. The
S202 redesign (about 10 events, no boss-random/holiday events, 3 world events, half time-based/half RNG) is data work
for later. The official `MobEventListener` also holds the behaviour of most custom effects, which the port never had,
so **every Lycanites debuff was cosmetic until now**. That is ported here too.

**Ported:** `MobEvent`, `MobEventPlayerServer`, `MobEventSchedule`, triggers (random, tick, altar), effects (world,
structure, command), `StructureBuilder` + the Rahovart/Asmodeus/Amalgalich arena builders, `MobEventManager`
(+ `globalmobevent.json`, `mobeventschedule.json`), `MobEventListener`, `MobEventSpawnTrigger` (+ dispatcher and
`Spawner` hooks: `applySpawnEvent` tagging and `MobEvent.onSpawn`), mob event spawners loaded by `SpawnerManager`,
`ExtendedWorld` runtime (world event + per-area players, restore of a saved world event, client sync),
`clearSpawnEventTracking` on taming, minions inheriting the spawn event, `/lm mobevents reload|enable|disable|creative|list`,
`/lm mobevent start|random|stop`, `MessageMobEvent`/`MessageWorldEvent`, client `ClientMobEventEvents`/
`MobEventPlayerClient`/`MobEventSound` (chat messages, looping event music, the 12-second title graphic drawn in the
overlay layer). Assets: 33 sounds.json entries + 42 oggs (16 MB), 33 title textures, 73 event JSONs.
Effect behaviour: paralysis, weight, instability, plague (poison + spread), smited, bleed, smouldering, swiftswimming,
immunization, cleansed, lifeleak, fallresist, penetration, leech, repulsion, rejuvenation, decay, insomnia, aphagia.

**1.21 / NeoForge changes:** events load in common setup after the spawners; the global/schedule JSON moved to
`common/` like `globalspawner.json`; the arena builders register in `MobEventManager` (officially `AltarInfo`, not ported);
`AltarMobEventTrigger` keeps a by-altar-name registry (`getTriggers(name)`) for the altar phase to use. Client dispatch
goes through `LycanitesMobs.APPLY_MOB_EVENT`, set in `ClientSetup` (the ClientProxy pattern this port already uses).
Effects: LivingAttack+LivingHurt -> one `LivingIncomingDamageEvent` listener; `PlayerSleepInBedEvent` ->
`CanPlayerSleepEvent`; item use -> the cancellable `Start`/`Tick` phases; swiftswimming modifiers use
ResourceLocation ids.

**Official bugs fixed:** (1) `WorldMobEventEffect` checked `world instanceof ServerLevelData`, which is never true, so
thunder never started; started rain/thunder now also get a 5-minute duration so vanilla's countdown can't flip them
straight back. (2) `MobEvent.getTitle()` used `mobevent.<title>.name`, but the lang keys are `mobevent.<title>`, so
chat showed raw keys. (3) `CommandMobEventEffect` ran `performCommand(null, ...)` (a crash; no official event uses
it); it now runs as the server in the event's level.

**Not ported:** the fear effect's haunting and its login cleanup (both need `EntityFear`, the dummy creature); the
jump cancel for paralysis/weight (`LivingJumpEvent` was never cancellable, so it was dead code upstream too); the extra
random `MessageEntityVelocity` for instability (the vanilla motion packet the official sent first still syncs it).
**Official behaviour kept:** random events are **off by default** (`random.enabled = false` in the config);
events only tick in worlds with a player online; a stopped per-area event isn't broadcast to clients (the title
simply times out).

**Verified:** headless (runServer + RCON): all 33 events load; decay halved healing; penetration x1.5 at amplifier 1;
fall damage cancelled under fallresist; plague spread one amplifier lower to a neighbour. Real client
(scripted save `run/client/saves/eventtest` with an `evt` datapack; temporary `--quickPlaySingleplayer` args, removed):
`bamstorm` started rain + thunder and spawned about 10 Lycanites every 10 s for its 60 s, then finished with the chat
message; the Rahovart boss event in the Nether built the arena and spawned Rahovart with Belphegor minions (boss bar
visible in a screenshot). Not verified: the title graphic and the event music by eye/ear, bleed (needs a
moving target), schedules (none in official data), tick triggers (unused in official data), world-event restore after
a restart. Asmodeus/Amalgalich arenas untested.

## Phase 6d: boss altars, S202 redesign (2026-09-29)

Glenn's design: no block formations, the 8 rare-variant altars cut, one pedestal and one soulkey per boss using the
old textures, and the soulkey takes the player straight to the boss fight. Implemented in `core/altar/BossAltar`
(the official `AltarInfo*` classes are not ported).

- **Pedestals** are the old soulcubes (ids and textures kept, renamed in lang): Demonic -> **Rahovart Pedestal**,
  Aberrant -> **Asmodeus Pedestal**, Undead -> **Amalgalich Pedestal**.
- **Soulkeys**, matched by colour (ids kept, renamed): `soulkey` (red) -> **Rahovart Soulkey**, `soulkeydiamond` (cyan)
  -> **Asmodeus Soulkey**, `soulkeyemerald` (green) -> **Amalgalich Soulkey**. The official tier/variant meaning is gone;
  the boss rolls its variant normally. A key on the wrong pedestal fails with a message.
- **Activation** teleports the player into the arena, onto a small obsidian arrival pocket 20 blocks from the arena
  centre facing it (the floor is only built a few seconds in), then fires the boss event's `AltarMobEventTrigger`
  there. The event's own StructureBuilder builds the arena and spawns the boss as upstream (20-25 s intro). Arena
  location: Rahovart in the Nether at the pedestal's coordinates / 8 (y 64); Asmodeus in the outer End 1000 blocks out
  in the pedestal's direction (y 64; the event needs 500+ from the centre); Amalgalich in the pedestal's dimension at
  least 520 blocks from world spawn (surface height; the event needs 500+). A pedestal already in a valid spot puts
  the arena 90 blocks ahead of the player so the pedestal survives. If the event refuses to start (e.g. that boss is
  already being fought: `message.soulkey.busy`), the player is sent back and keeps the key.
- **Return trip:** 10 s after the boss dies, every player who came through a pedestal to that arena (and is still
  within 150 blocks of it) is teleported back to where they used the key. Return points are in memory only, so a
  server restart mid-fight loses them (the player can walk/portal home as normal).
- Recipes unchanged from official: the Asmodeus and Amalgalich keys are still crafted by upgrading a Rahovart key
  (soulkey + 8 diamonds / 8 emeralds). Needs a tuning decision.

**Verified** with a real client (temporary test driver calling the real `useItemOn` on each pedestal, removed after):
all three keys teleported the player to the right dimension (Rahovart Nether (4,65,1) from an overworld pedestal at
(36,137,10); Asmodeus End (943,65,331); Amalgalich overworld (234,64,546)), the arenas built and the bosses spawned
(21-31 s), and after each boss was killed the player was returned to the pedestal. Screenshots in
`run/client/screenshots/altartest_*.png`. **Not verified:** key consumption (the test player was in creative, which
skips it, as upstream), multiplayer (several players using one pedestal), and a real fight to the death. The first
teleport took ~10 s (fresh Nether chunk generation).

## Summoning Pedestal (2026-09-29)

The pet system's last placeholder block. Ported `TileEntitySummoningPedestal`, the real `BlockSummoningPedestal`
(an EntityBlock with a ticker), `SummoningPedestalContainer` + provider, `SummoningPedestalScreen` +
`SummoningPedestalList`, `MessageSummoningPedestalStats` (server -> nearby clients) and
`MessageSummoningPedestalSummonSet` (client -> server), the pedestal branches of `PortalEntity`, and
`BaseCreatureEntity.bindSummoningPedestal`/`hasSummoningPedestal`/`getSummonCost` (fused minions re-register with
their pedestal).

**1.21 / NeoForge changes:** a real registered `BlockEntityType` (the official `TileEntityBase` reported
`BlockEntityType.CHEST` and swapped it in `getType()`); ticking via `getTicker`; registry-aware NBT; block entity
sync via `getUpdateTag`/`handleUpdateTag`; `Player.openMenu` with the block pos; the menu type is added to
`CreatureContainer.MENUS`. The screen registers its list as a renderable widget, since 1.21's `Screen.render` draws
the background a second time and covered a manually rendered list.

**Official bugs fixed:** the `summoningpedestal.redstonetime` config only set the fuel bar's initial max; each redstone
still burned a hardcoded 10 minutes. It now sets the burn time. The summon-set packet was applied for anyone who sent
it; it's now owner-only and needs the owner within reach.

**Verified** with a real client (temporary driver, removed): placing binds it to the placer (`owner=2` model state);
right-clicking opens the screen with all 33 summonable creatures listed and the behaviour buttons; picking one through
the screen reaches the server; with redstone in the fuel slot it burned one dust, summoned a Geonach about every 10 s,
each bound to the pedestal and owned by the player, and stopped at 5 when capacity hit 10/10. A water creature (Lacedon)
was also selectable; whether it summons sensibly out of water is untested.

## Phase 7: dungeons + fluid pools (2026-09-30)

**Dungeons.** Ported the official 1.20.1 structure path: `LMDungeonStructure` (a `lycanitesmobs:lm_dungeon`
structure type) + `LMDungeonPiece`, the full definition set (`DungeonSchematic`/`Sector`/`Structure`/`SectorSegment`/
`SectorLayer`, themes now wired to `SectorInstance` for light/torch/stair/pit blocks), the layout generator
(`DungeonLayout`, `SectorInstance`, `SectorConnector`, `SectorBounds`, `SectorBuildSequence`), `DeferredBossSpawner`
and `DungeonVirtualPack`, the built-in datapack that turns each schematic's `world` condition into a structure, a
biome tag (`has_structure/<name>`) and one combined `lycanitesmobs:dungeons` structure set. All 7 schematics, 14
sectors and 8 themes copied unchanged. Command: `/lm dungeons reload|enable|disable|locate <name>`.

Not ported (dead upstream): the legacy ExtendedWorld-tracked generator (`DungeonFeature` is commented out in the
official `WorldGenManager`), so `DungeonInstance` is trimmed to what the structure path uses and there are no async
build plans or dungeon NBT in `ExtendedWorld`; `StructureSpawnEvents` (debug right-click, disabled upstream);
`WorldgenJsonDumper` (dev-only JSON dump).

**1.21 / NeoForge changes:** structure + piece types via `DeferredRegister` (official `Registry.register`d them
directly); `PackResources.location()`/`PackLocationInfo`/`PackSelectionConfig`, pack_format 48; schematics read
through `FileLoader` instead of Forge's `IModFile`; `LevelData.getSpawnPos()`; chest loot as
`ResourceKey<LootTable>`; `LevelTickEvent.Post`.

**Fixes over official:**
- **`forge:` biome tags.** The official JSONs (creatures, spawners, dungeons) use Forge 1.20 tags like
  `forge:is_snowy`, which don't exist on NeoForge 1.21 (they became `c:` tags), so they silently matched nothing:
  **this also affected creature spawning since Phase 5/6b.** `JSONHelper.normalizeBiomeTag` maps `forge:X` -> `c:X`
  (`is_coniferous` -> `c:is_tree/coniferous`, `is_dense` -> `c:is_dense_vegetation`), applied in
  `getBiomesFromTags` (every spawn/biome check) and the dungeon pack. Generated dungeon biome tag entries are all
  `required: false`, since one missing required reference fails the tag and the world load.
- **Height cap.** Towers were capped at a hardcoded y 255, so a Nether temple's tower went through the bedrock roof
  (verified: sections up to y 224). Now capped at `min(255, generator minY + genDepth - 6)` (Nether: 122), stored in
  the piece NBT so regenerated layouts match; dungeon blocks never replace bedrock.
- The `dungeons.enabled` config (and `/lm dungeons enable|disable`) was only read by the dead legacy feature; it now
  gates `findGenerationPoint`.
- `locate` used a per-schematic spacing/salt grid that no longer matched the combined structure set; it now uses the
  chunk generator's `findNearestMapStructure` like `/locate structure`.

**Verified** headless (fresh worlds, RCON + a small region-file reader): the pack loads and is enabled; `locate` finds
all 6 overworld/nether dungeons; a Lush Tomb 925+ blocks out built across 96 chunks (lush stone set, crystals, poison
clouds, 56 spawners set to the schematic's mobs, 46 chests with level-scaled loot tables, 12 named bosses: Pong Kong,
Princess, Malevolent Observer); a Demonic Temple in the Nether (demonstone, hellfire, doomfire; after the cap, highest
section y 112-127); a waterlogged Stream Shrine (water inside). **Gotchas:** a dungeon whose 113-block footprint
comes within `minDistanceFromSpawn` (500) of spawn is registered as a structure (and shows in `locate`) but skipped at
build time - that's the official runtime check - so test 1000+ blocks out. `/forceload add` caps at 256 chunks.
**Not verified:** a player actually walking/fighting through one, End placement (aberrantstation), the midnight
dimension. **Tuning pending (Glenn: "rarer and smaller"):** a dungeon currently spans y 0-255 with up to 10 levels,
spacing 32-38 chunks.

**Fluid pools.** Copied the official acid/moglava/ooze/poison lake + spring configured/placed features, biome tags and
biome modifiers (`forge:add_features` -> `neoforge:add_features`, `neoforge/biome_modifier/`). Verified natural acid
lakes generating in a desert (rare: lakes 1 in 120 chunks, as upstream).


## Fear effect (2026-09-30)

Before this the Fear effect did nothing (only the Shade applies it). Ported the official 1.20.1 system:
- **`EntityFear`** (dummy creature `fear.json`): invisible, intangible ghosts, one per fear level, that haunt a feared
  survival/adventure player for 15-30 s (`HauntPlayerGoal` orbits; `FearMoveGoal` pushes the player away with wobble
  when close, erratic "ghost tugs" when far, +0.5 step height). Spawned from `MobEventListener.handleFear` (fear added
  to the tick-effect gate) and by the Shade's attack; stale ghosts removed on login; creative/spectator immune.
- **Client** (`client/effect/`, `FearClientEvents`): the lightmap dims in two phases (block light, then sky light;
  `LightTextureMixin`, the port's first mixin, `lycanitesmobs.mixins.json` declared in neoforge.mods.toml), flickering
  with the heartbeat's decoded amplitude envelope; all other sounds muffled (volume + pitch, distance-scaled via
  `MuffledSoundInstance`, which also delegates 1.21's stream methods); a looping heartbeat. Config in the existing
  `ConfigClient` fear values.
- Dummy creatures now get a `NoneRenderer` (was the pig placeholder).

**Not ported (dead upstream):** `FearHandler` (nothing calls it), the ghost mesh renderer (`FearRenderer`'s render body
is commented out, so `FearMesh`/`FearMeshProfile`/`GhostTendrilMesh` are unused) and the `FearRedGlow` shader uniform
(part of the unported VBO/Iris renderer). Same for the blocky models (`registerBlocky = false` upstream).
1.21 changes: step height is the vanilla `Attributes.STEP_HEIGHT` with a `ResourceLocation` modifier id; the extra
`MessageEntityVelocity` push packet is dropped (vanilla motion packet only, as for instability).

**Verified** with a real client (temporary driver, removed): Fear IV on a survival player -> 4 ghosts, player pushed
0.6-6 blocks per 2 s, block dim ~0.99 / sky dim 0.6 with heartbeat flicker, muffle 1.0, heartbeat decoded (27.4 s
envelope); when the effect ended the ghosts were discarded and light/audio returned to normal. Screenshots
`run/client/screenshots/feartest_*.png`. **Gotcha:** the old test saves' player is in creative, which is immune.

## Projectile OBJ models (2026-09-30)

The 5 JSON projectiles with a `modelClass` (aetherwave, chaosorb, crystalshard, lightball, lobdarklings) now render
their OBJ models instead of sprites: `ProjectileModel`/`ProjectileObjModel` + the 5 model classes, `ProjectileModelRenderer`,
`LayerProjectileBase`/`LayerProjectileEffect`, `ModelAnimation.addProjectileLayers` +
`TextureLayerAnimation.createProjectileLayer`, and projectile models in `ModelManager` (created with the creature
models, reloaded by the same reload listener). Renderer picked per projectile in `ClientSetup`.

**Port changes:** plain `ObjModel` + buffered render types from `CustomRenderStates.getObjRenderType` instead of the
official `VBOObjModel`/`VBOBatcher`/Iris path (same approach as the creatures). The "old projectile" model constructor
dropped (none registered upstream). **Fixes:** the official `render()` had the yaw and partial-tick parameters swapped
(the animation loop used the yaw as the partial tick); projectiles created by the entity type's factory (`/summon`,
dispensers) never got their `ProjectileInfo`, so they synced no name and the client had no texture - now resolved
from the entity type id (this affected sprite projectiles too).

**Verified** with a real client: all 5 summoned and rendered with their models and textures (screenshot
`run/client/screenshots/projtest_1.png`).

## Phase 9: shader compat + mixins (2026-09-30)

**Iris/shader compat needs no port code.** The official ~3,000-line Iris layer (`OculusCompat`, `IrisHelper`,
`IrisDrawIds`, `VBOObjModel`, the VBO/Iris batchers, `RecolorTextureCache`) exists because the official draws OBJ models
from its own VBOs with a custom shader and vertex format, which Iris can't intercept. The port draws everything
through `RenderType`s using vanilla entity shaders and `NEW_ENTITY` (see `CustomRenderStates`), which Iris remaps to the
pack's entity programs by itself. **Verified** in the dev client with Lycannots' Iris 1.8.14-beta.1 + Sodium 0.8.13 and
Complementary Unbound r5.9.3 (the S202 base): creatures (cinder, wisp, geonach, volcan, spectre, reiver), glow layers
at night and all 5 model projectiles rendered correctly with shadows; no shader errors from Lycanites. Screenshots
`run/client/screenshots/shadertest_*.png`. Iris/Sodium were only dropped into `run/client/mods` for the test and removed.

**Known gap: Fear's light dimming doesn't show under shader packs.** Packs compute lighting themselves and ignore the
vanilla lightmap texture that `LightTextureMixin` dims (the muffle, heartbeat and ghost push still work). The official
has the same gap. Possible fix, needs Glenn's call: while a shader pack is active, feed fear into the vanilla Darkness
effect's blend factor, which Iris exposes as `darknessFactor` and Complementary already reacts to.

**The other official mixins:** `EntityTypeMixin` (registry name holder: the port uses the registry) and the two structure
accessors (replaced by 6b's structure modifier) aren't needed; `ReloadCommandMixin` became an `AddReloadListenerEvent`
listener that clears the log-once caches; `CreativeModeTabRegistryMixin` (groups Lycanites' tabs) isn't needed, since
NeoForge already keeps a mod's tabs together in registration order.


## Leftover audit + creature fixes (2026-09-30)

Re-ran the creature method audit (official vs port methods per creature class, with the port's renames mapped) and
went through every remaining unported file and `TODO(port)`:
- **Water breathers treated as air breathers in 5 goals.** The port renamed the hook to `creatureCanBreatheUnderwater()`
  (vanilla's is final in 1.21), but `StayByWaterGoal`, `PaddleGoal`, `StayGoal`, `StayByHomeGoal` and
  `FollowOwnerGoal` still called vanilla `canBreatheUnderwater()`, so aquatic creatures floated instead of sinking
  and pets misjudged water. Fixed; verified a Lacedon now stays on the pool floor.
- **Kobold thievery never worked** (upstream too): it overrode vanilla `canPickUpLoot()` instead of `canPickupItems()`,
  and even with that fixed an unbagged Kobold had no slots, and `CreatureInventory.onInventoryChanged()` drops the bag
  slots when no bag is equipped, so anything picked up was dropped straight away. Kobolds now get their bag size
  without a bag. Verified headless: a Kobold walked to a diamond and kept it.
- Restored from Phase 5g trims: fly sounds and the flyer attack leap (`tickMovementRuntime`), the mount inventory key
  opening the creature GUI, and a sitting pet dropping its leash past 10 blocks (`handleLeashAtDistance`, like vanilla's
  TamableAnimal).
- Not needed / dead upstream: `GameEventListener` (split into the port's listeners; clone handled by the `copyOnDeath`
  attachment; left-click only served equipment; entity-mount handler disabled upstream), `getBrightnessForRender`
  (overrides nothing), the navigator's fluid element check (only gated vanilla water/lava), Kobold's
  `onRemovedFromWorld` bag drop (fired on chunk unload - the port drops on discard only).
- Still open, minor: the boss ARENA sync slot, the Soulgazer interact-command priority, pet GUI refresh scheduling.

## Hellforged vs Incursion (design input, 2026-09-30)

Compared the latest Hellforged sources (official GitLab: `Minecraft-1.12.2` = 2.0.8.10, last commit 2025-09-23;
`community-minecraft-1.16.5` = 2.3.3.8, 2025-12-05) against Incursion (`1.20.1-dev`, this port's reference): creature,
projectile, event, equipment, item, element, spawner and dungeon JSON sets, lang keys, config keys, and per-class methods.
Incursion carries essentially all Hellforged content (same 122 creatures, 73 events, 59 equipment parts, 40 items, all
101 config options); 1.12.2 used older names (pinky = kathoga, cacodemon = malwrath, archvile = apollyon, behemoth =
behemophet, belph = belphegor, beholder = naxiris, lobber = cherufe, gorger = umibas, dweller = ningen, lurker = ostimien,
quillbeast = brucha, strider = stryder, tarantula = lycosa, joust = jouste). **Not in Incursion:**
- ~~Djinn~~ **correction:** the 1.12.2 Djinn was not removed, it was **renamed Zephyr** in 1.16 (identical model,
  texture, stats and code - verified by file hashes), and the 1.12.2 Zephyr became **Raidra** (identical texture). So no
  creature is missing; the Djinn is in this port as the Zephyr. Glenn's "Orphani" angel plan (repurposed Djinn) therefore
  meant repurposing or cloning the Zephyr - **the Orphani idea was scrapped (Glenn, 2026-09-30)**.
- **Raidra** (lightning elemental, the 1.12.2 Zephyr) is in Incursion and this port and **does spawn naturally**: the
  `storm` spawner (world trigger), the `glowstone` spawner (2% when breaking glowstone) and the Bamstorm, Tsunami and
  Windstorm mob events (spawners list it as `lycanitesmobs:raidra`; an earlier bare-name search missed that). It can
  also come from fusing a Zephyr with a Cinder. The 1.12.2 lightning/fire transformation into it broke in 1.16.5 and
  was removed in Incursion.
- **True sight** (1.12.2 only, added 2022-12): 17 creatures (triffid, serpix, reaper, rahovart, krake, jouste,
  joustealpha, grell, ostimien, grigori, umibas, epion, naxiris, crusk, asmodeus, banshee, amalgalich) ignore
  invisibility and the sneaking range reduction when targeting (`TargetingGoal`).
- **Random-placed dungeon variants** (1.12.2): `aberrantstation_random` / `shadowlabyrinth_random` placed at random
  y 64-80 in the Nether/End via a random placer, plus a per-schematic `canGenerateAsTower` toggle.
- **Smitefire fireball** projectile (1.12.2; Incursion keeps only an unused charge item model).
- 1.16.5: five elements (bose, coda, glasma, mote, murati) that Incursion replaced with chrono, fate, flux, gravity,
  nova, vortex (+ xeno); temple/village spawners became Incursion's structure spawns (`structurespawns/`).
- Incursion-new, not in Hellforged: Stream Shrine dungeon, the stick recolor (`CreatureRecolorScreen`, unported here).
