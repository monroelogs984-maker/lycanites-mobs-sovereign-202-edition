# Trigger spawners: S202 tuning

**Chances approved by Glenn (2026-09-30).** Not applied yet. Chances are plain doubles (`nextDouble() > chance`), so
fractional percentages like 0.25% work as-is.

| Spawner | Trigger | Spawns | Official | **S202** |
|---|---|---|---|---|
| ore | ore mined (drops a block: coal, iron, gold, nether gold) | Geonach | 5% | **3%** |
| gem | ore mined that drops an item (diamond, emerald, lapis, redstone, copper, quartz, most modded ores) | Vapula | 5% | **1%** |
| glowstone | glowstone broken | Raidra | 2% | 2% (same) |
| chorus | chorus plant/flower broken | Wraamon | 5% | 5% (same) |
| tree | log chopped | Ent | 2% | **1.5%** |
| crop | crop harvested | Spriggan | 0.5% | **2%** |
| mix | liquids mix into a block (e.g. cobble generators) | Xaphan | 1% | **0.25%** |
| explosion | any explosion / player-caused explosion | Tremor | 5% / 20% | **4% / 6%** |
| fishing | fish caught | Silex, Abaia, Roa, Skylus (+ Abtu, cut: remove) | 20% | **3%** |
| darkness | standing at light <= 3 (see below) | Grue, Darkling, Shade | 50% per 5 s check | **50% per 7.5 s check** |
| death | player kills any non-undead entity | Reaper | 2% | **0.5%** |
| undeath | player kills an undead (not Geist) | Geist | 3% | **2%** |
| sleep | player sleeps (1 min cooldown) | Reaper | 10% | **2%** |
| chaos | killing elementals, 3 hits (10% each), warnings at 1 and 2 | Argus | disabled | **enabled** |
| disruption | same as chaos, no warnings | Argus | enabled | **disable** (chaos replaces it) |
| pumpkin (world) | - | Tpumpkyn (cut) | enabled | **disable** |

## Notes

- **Darkness timing:** a check every 100 ticks (5 s) while at light <= 3, each with a 50% chance to add 1 to the count; it
  needs 3; any check at light >= 4 resets the count. Official: min 15 s, average ~30 s in the dark. Glenn wants 1.5x
  -> `tickRate: 150` (7.5 s) -> min 22.5 s, average ~45 s.
- **Chaos = Disruption + warnings.** The two JSONs are identical except Chaos has `triggerCountMessages` at counts 1, 2
  and 3 (Disruption only at 3). Enabling both would double-roll every elemental kill, so Chaos replaces Disruption.
- **Full moon: correction.** Neither spawner is actually full-moon conditional. The full-moon condition sits on a
  count-*reset* trigger (`count: 0`, `useWorldTime`, fires at dayTime 1) whose condition requires dayTime >= 10000, so
  it can never fire. Result: the count never resets, and the 3 strikes accumulate over the whole playthrough.
  **Decided (Glenn 2026-09-30): reset the count each dawn**, so a warning means "back off for today".
- **Death** fires on *any* non-undead kill, including animals (cows, chickens). At 0.5%, that's about 1 Reaper per 200
  kills. **Decided (Glenn 2026-09-30): hostile mob kills only.**
- **Mix** has no player (`trigger(world, null, ...)`), so cooldowns (per-player) can't apply, and it fires with no one
  nearby: an unattended loaded cobble generator still spawns Xaphans. At 0.25% that's about 1 per 400 blocks formed.
  **Decided (Glenn 2026-09-30): only fire with a player nearby** (needs code: find the nearest player in range).
- Creature IDs in the Chaos elemental list: moved creatures (Wisp/Aegis/Banshee/Raidra -> angel, Vapula -> golem,
  Xaphan -> slime) are listed by ID, so they still count as "elementals" there. Decide with the type rework.
