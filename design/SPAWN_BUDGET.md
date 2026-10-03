# Spawn budget ("Mythic Beasts" density)

**DECIDED (Glenn 2026-10-03), BUILT 2026-10-03** (see "Built" at the end). Covers both density (how many) and fairness (which ones). LYCANITES-REVIEW asked for Lycanites to feel like a "Mythic Beasts mod
where you see a few of everything". The 2026-09-30 work only changed *which* creature spawns (climate ranges,
common/rare weights); the *volume* stayed official, which Glenn found "a moderate change from vanilla".

## Why it felt default (measured 2026-10-03, dev client, desert)

- World spawners fire per player on a timer: `land` and `water` every 600 ticks (chance 1), `sky` 600 (0.5),
  `land_underground` 600 (0.525), `land_deadly`/`sky_deadly` 800 (0.5), `water_floor` 800 (0.75).
- Each firing spawns the spawner's `mobCountMin`: 6 (land/sky/water/underground/deadly) or 16 (water_floor),
  regardless of the creature's own pack size.
- The only cap is `typeSpawnLimit` 64 of a creature type within `spawnLimitRange` 32, which never binds.
- Result: ~60+ Lycanites a minute per player (sweep: 142 joined in 90 s of day, 94 at night, 41 underground).

## Decisions

1. **Budget: 6-10 wild Lycanites within ~96 blocks of each player** (config: target and radius). The world
   spawners only fire while the player is under budget, and each firing adds one group.
2. **Group size comes from the creature** (`spawnGroupMin`/`spawnGroupMax`), not the spawner's flat 6 or 16. Packs
   stay packs.
3. **No variety rule** (Glenn chose this over "1 group per species"): the weighted common/rare pick decides.
4. **Separate from the budget:** trigger spawners (ore, tree, crop, kill, fishing, sleep, explosion, mix), mob
   events, dungeons, structure spawns, bosses. They're deliberate moments and neither count nor get blocked.
5. **Measuring tool:** `/lm spawning stats` (count near you vs budget, recent spawns by source and species), so
   tuning uses real numbers.

## Equal frequency across creatures (Glenn 2026-10-03)

Glenn: creatures that were common officially still spawn far more than ones that barely spawned (Bobeko vs
Concapede). Three official biases survived the 09-30 rework, which only equalised weights *inside* each spawner:
1. **Spawner membership:** per creature, world spawners fire at very different rates (5 `land_underground` creatures
   share ~1 firing/min, 31 `land` share 2/min, 9 `land_animal` share 0.09/min), up to ~20x apart.
2. **Batch size:** 6, 8 or 16 per firing depending on the spawner.
3. **Habitat breadth:** the climate bands kept each creature's original habitat size (Bobeko: temperature -1.10 to
   0.90, most non-hot biomes; Concapede: 0.55 to 1.35 and downfall 0.65 to 1.0, jungles only).

**Fix for 1 and 2: one shared pick.** When a player is under budget, a single pick runs over every creature that can
spawn at the candidate spot (dimension, climate, light, terrain type: land / sky / water / underground / lava),
weighted only by common/rare. World spawners no longer decide how often anything spawns; they only describe where a
creature is placed (their location rules).

**Fix for 3: equal climate bands, both directions** (Glenn: widen the narrow ones and narrow the broad ones). Every
creature's band becomes the same size, **temperature width 1.75 and downfall width 0.55** (the 09-30 medians),
centred on the middle of its original band and shifted to stay inside the real biome range. Narrowest today:
Concapede, Conba, Uvaraptor, Vespid, Vespid Queen (0.8 x 0.35); broadest: Barghest (3.5 x 1.0), Roc, Cockatrice,
Jabberwock, Naxiris.

**"Anywhere" creatures** (no climate band, 11 in the Overworld):
- **Cave creatures stay anywhere** (Chupacabra, Darkling, Gnekk, Grue, Shade, Wraamon): underground has no real
  climate; the cave itself is the habitat.
- **Water creatures get a band** of the same size (Abaia, Jengu, Silex, Stryder, Thresher), centre per creature TBD.
  Caveat: vanilla oceans almost all have temperature 0.5 (only frozen oceans differ), so in vanilla this mostly
  separates seas from frozen seas, rivers and swamps; biome mods with varied ocean climates make it matter more.

## Open (decide while building)

- Whether the chunk pre-spawn (`chunk_animal`, `chunk_water_animal`, on new chunk generation) counts toward the
  budget. Proposed: yes, it's natural spawning.
- Several players close together: count the budget per player over the union area, so two players don't double it.
- Tamed pets, minions and the player's own summons never count.

## Built (2026-10-03)

- `SpawnBudget` replaces the world spawners' own timers (config `spawnBudgetEnabled`, `spawnBudgetMin` 6,
  `spawnBudgetMax` 10, `spawnBudgetRange` 96, `spawnBudgetInterval` 60 ticks). Each check under budget runs one shared
  weighted pick over every creature any world spawner can place there, then spawns one group (the creature's own
  group size, cut to the room left). Chunk pre-spawns only run under budget and count toward it.
- Natural spawns are tagged (`NaturalSpawn` NBT) and always despawn; pets/minions/body segments don't count.
- **Interval 200 -> 60 ticks** (measured): at 200 the area sat at 2-4 because groups average ~1.3 and creatures
  despawned fast. At 60 it held 5-7 in plains by day.
- **Idle despawn for natural spawns: 2400 ticks** (vanilla 600). At 600, budget creatures vanished 10-40 s after
  spawning, 60-128 blocks out, before the player got near. Official light pressure (x3 idle in disliked light) and
  the vanilla 128-block instant despawn still apply.
- **Block-scan cache:** the 13 block-location world spawners (lava, fire, acid, ooze, portal, flower...) each read
  a 65-block cube; run every check it cost ~60 ms per check. Their positions are cached per area for 30-45 s
  (the official rate) and reused within 16 blocks.
- **Water creature climate centres** (`climateCenter` in the json; Glenn may retune): Abaia 0.8/0.9 (swamps,
  jungle water), Jengu 0.9/0.6 (warm water; `ignoreBiome` now false), Silex 0.5/0.5, Stryder 0.3/0.6,
  Thresher 0.2/0.4 (cold seas). Note: negative biome tags (`-minecraft:is_ocean`) are ignored by the climate
  conversion, so Silex/Stryder/Thresher used to be "anywhere".
- **Plains line (Glenn 2026-10-03):** "things that are clearly snowy (Bobeko, Reiver...) shouldn't spawn in plains
  or warmer, and vice versa for desert mobs." A creature whose original biome list was entirely colder than plains
  (0.8) has its band cut below 0.8; one entirely at/above 0.8 (desert set; beaches sit at 0.8) is cut above 0.8.
- Fairness measured (150 picks per biome): 35-40 species each in plains, jungle, snowy taiga, desert, beach, dark
  forest; no creature above ~9%. Concapede now appears outside jungles.
- Tools: `/lm spawning stats` (count vs budget, species, placement failures, last checks with ms), `/lm spawning tick`.
- **Two pools (Glenn 2026-10-03, "yes"):** caves and water were filling half the budget, so the surface felt thin.
  Surface (open sky, no fluid) keeps `spawnBudgetMin`/`Max` 6-10; caves and water get their own
  `spawnBudgetBelowMin`/`Max` 3-5. Each check fills the surface first, then caves/water.
- **Excluded biome tags apply again (Glenn: Silex/Stryder freshwater-only, "yes"):** `isValidBiome` now honours the
  json's `-` tags on top of the climate band, for every creature (e.g. Thresher stays out of swamps/jungles).
- **Chunk pre-spawns** only run within budget range of a player (they used to spawn at view distance and despawn
  instantly).
- **Block scans per check capped at 2**; expired scans are reused and unscanned spawners sit a check out. Checks
  measured 8-16 ms while travelling (were 60-70 ms).

## Related fix: /kill on bosses (2026-10-03)
Boss damage limits made `/kill` and the void do nothing (damage cap, per-second limit, and the health clamp
reviving a dying boss mid-animation; Amalgalich is also invulnerable while blocking). `/kill` and the void now
bypass the limits, a dying creature is never clamped, and `kill()` always kills. Verified on a dev server:
Amalgalich and Rahovart die to `/kill`.
