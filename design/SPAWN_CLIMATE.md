# Spawn climate (biome loosening)

**Method decided by Glenn (2026-09-30); supersedes the keyword draft below.** Biome conditions become **climate only**. Dimension conditions stay as they
are. Each creature's biome list is replaced by one climate group:

- **hot:** hot/sandy/savanna/badlands/dry biome tags
- **cold:** cold/snowy/icy/frozen/taiga/coniferous
- **wet:** swamp/ocean/river/beach/jungle/lush/mushroom
- **anywhere:** no biome condition (also used when the old list had no climate words, e.g. forest, plains,
  mountain or spooky only, or covered all three climates)

Two climates are allowed (e.g. `cold + wet`). **Edit the Climate column.** Excluded: creatures with no natural
spawner (bosses, parts, summon-only).
Water creatures still need water, and sky/underground spawners keep their placement. Climate only replaces the biome
filter. Count in this draft: anywhere 45, wet 24, hot 14, cold 8.

**Gotcha: dimension-proxy biome tags must become real dimension conditions first.** Some creatures stay in their
dimension only through a biome tag (`is_end`, `is_overworld`, `is_nether`), not a dimension condition. Dropping the
biome list would let them spawn everywhere:
- **Argus, Spectre:** dimension list is an empty blacklist (= any dimension); only `is_end` keeps them in the End.
- **Epion, Geist:** `is_end` or spooky: the End *plus* spooky Overworld biomes.
- **Astaroth, Trite, Kathoga:** whitelist Nether + End; `is_end` keeps them out of the Nether.
- **Wraamon:** `is_overworld` + `is_end`, not the Nether.
- **Wraith:** whitelist Nether only, but biomes are `is_overworld`/`is_end`/`is_forest`, which never match in the Nether;
  it likely never spawns naturally (official data bug). Needs a decision on where Wraith should spawn.
- The cold creatures (Arix, Bobeko, Frostweaver, Maug, Reiver, Serpix, Wendigo) exclude `-is_end`; harmless (the End is
  already blacklisted by dimension).
The apply pass converts these into dimension conditions, then applies the climate.

## Method (Glenn 2026-09-30)

Use the real climate of each creature's current biomes, not keywords:
1. Resolve the creature's biome tags/ids to the actual biomes (whatever the pack has: vanilla, BOP, Jagged...).
2. Read each biome's **temperature** and **downfall** (humidity) and take the min/max of both over that set.
3. **Loosen by about 15%:** widen each range by 15% of the full scale on both sides. Vanilla temperature runs about
   -0.7 to 2.0 (so +/-0.4); downfall 0 to 1 (so +/-0.15).
4. The spawn check becomes "biome temperature and downfall inside those ranges", so any biome with a matching climate
   qualifies, including modded biomes that were never tagged.
- Creatures with no biome condition stay "anywhere". Dimension conditions are unchanged. Dimension-proxy tags are
  converted first (below).
- Needs code: a climate condition on `CreatureSpawn` (temperature/downfall min/max), computed at load from the old
  biome lists, plus a dev dump command to review the resulting ranges per creature.

## Wraith (Glenn 2026-09-30)

Spawns in **any Nether biome, upper half of the Nether** (y >= 64 of 0-128; the Nether roof is at 128). This replaces
its broken biome list (see below). Needs a creature-level `minY` spawn field (not supported yet), or a dedicated
spawner with a y range.


## Superseded keyword draft (kept for reference)

| Creature | Old biome words | Climate |
|---|---|---|
| abaia | (none) | **anywhere** |
| afrit | (none) | **anywhere** |
| aglebemu | spooky, swamp | **wet** |
| apollyon | (none) | **anywhere** |
| argus | end | **anywhere** |
| arisaur | forest, plains | **anywhere** |
| arix | cold/overworld, coniferous, snowy | **cold** |
| aspid | mushroom, spooky, swamp | **wet** |
| astaroth | end | **anywhere** |
| balayang | badlands, sandy, wasteland | **hot** |
| banshee | spooky | **anywhere** |
| barghest | mountain, savanna | **hot** |
| behemophet | (none) | **anywhere** |
| belphegor | (none) | **anywhere** |
| bobeko | cold, coniferous, snowy | **cold** |
| calpod | forest | **anywhere** |
| cephignis | (none) | **anywhere** |
| cherufe | (none) | **anywhere** |
| chupacabra | (none) | **anywhere** |
| clink | badlands, sandy, wasteland | **hot** |
| cockatrice | jungle, mountain | **wet** |
| conba | jungle | **wet** |
| concapede | jungle | **wet** |
| crusk | badlands, sandy, wasteland | **hot** |
| darkling | (none) | **anywhere** |
| ent | forest | **anywhere** |
| epion | end, spooky | **anywhere** |
| erepede | badlands, sandy, wasteland | **hot** |
| ettin | spooky, swamp | **wet** |
| frostweaver | cold, coniferous, snowy | **cold** |
| geist | end, spooky | **anywhere** |
| ghoul | spooky, swamp | **wet** |
| gnekk | (none) | **anywhere** |
| gorgomite | badlands, sandy, wasteland | **hot** |
| grell | (none) | **anywhere** |
| grigori | (none) | **anywhere** |
| grue | (none) | **anywhere** |
| herma | beach, ocean | **wet** |
| ignibus | all | **anywhere** |
| ika | beach, ocean | **wet** |
| ioray | ocean | **wet** |
| jabberwock | mountain | **anywhere** |
| jengu | (none) | **anywhere** |
| jouste | badlands, sandy, wasteland | **hot** |
| kathoga | end | **anywhere** |
| khalk | (none) | **anywhere** |
| kobold | plains, savanna | **hot** |
| krake | river | **wet** |
| lacedon | beach, ocean | **wet** |
| lycosa | jungle, swamp | **wet** |
| maka | plains, savanna | **hot** |
| maug | cold, coniferous, snowy | **cold** |
| morock | plains, savanna | **hot** |
| naxiris | mountain | **anywhere** |
| necrovore | spooky | **anywhere** |
| ningen | jungle, spooky, swamp | **wet** |
| ostimien | forest, spooky, swamp | **wet** |
| quetzodracl | ocean | **wet** |
| raiko | ocean | **wet** |
| reaper | spooky | **anywhere** |
| reiver | cold, coniferous, snowy | **cold** |
| remobra | spooky, swamp | **wet** |
| roa | beach, ocean | **wet** |
| roc | forest, plains, savanna | **hot** |
| salamander | (none) | **anywhere** |
| serpix | cold, coniferous, snowy | **cold** |
| shade | (none) | **anywhere** |
| shambler | forest, jungle | **wet** |
| silex | all | **anywhere** |
| skylus | beach, ocean | **wet** |
| spectre | end | **anywhere** |
| stryder | all | **anywhere** |
| sutiramu | badlands, sandy, wasteland | **hot** |
| thresher | all | **anywhere** |
| treant | forest | **anywhere** |
| trite | end | **anywhere** |
| troll | mountain | **anywhere** |
| umibas | (none) | **anywhere** |
| uvaraptor | jungle | **wet** |
| ventoraptor | plains, savanna | **hot** |
| vespid | jungle | **wet** |
| vespidqueen | jungle | **wet** |
| volcan | (none) | **anywhere** |
| vorach | (none) | **anywhere** |
| warg | forest | **anywhere** |
| wendigo | cold, coniferous, snowy | **cold** |
| wildkin | cold, mountain, snowy | **cold** |
| wraamon | end, overworld | **anywhere** |
| wraith | end, forest, overworld | **anywhere** |
| yale | mountain | **anywhere** |
| zoataur | badlands, plains, sandy, savanna, wasteland | **hot** |
