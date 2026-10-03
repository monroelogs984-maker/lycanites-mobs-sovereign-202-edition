# Spawn budget ("Mythic Beasts" density)

**DECIDED (Glenn 2026-10-03), not built yet.** Covers both density (how many) and fairness (which ones). LYCANITES-REVIEW asked for Lycanites to feel like a "Mythic Beasts mod
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
