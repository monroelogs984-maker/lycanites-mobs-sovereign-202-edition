# Mob events: S202 redesign

**Picks approved by Glenn (2026-09-30).** Not applied yet.

## Keep list (12 of 30), fixed rotation per dimension

Each dimension keeps its own rotation index. It runs the next event in its list each time its timer fires, and loops back to the start after the last one.
Official behaviour (weighted random, repeats possible) is replaced. The rotation index needs to live in `ExtendedWorld` (saved).

| Dimension | Rotation order |
|---|---|
| End | Shadow Games (**End only now**) |
| Nether | Cinderfall -> Eruption -> Hells Fury |
| Overworld | Black Plague (**Overworld only now**) -> Poop Party -> Bamstorm -> Winters Grasp -> Root Riot -> Winged Venom -> Boulderdash -> Primal Fury |

Every event gets a dimension whitelist for its own dimension, replacing the current Nether/End blacklists
(and the missing restriction on Shadow Games, Cinderfall, Eruption, Hells Fury, Black Plague).

**Disabled (18):** aberrant_assimilation, arachnophobia, bladeflurry, dragonsroar, marchofthegorgomites, raptorrampage,
reptileruckus, seastorm, sharknado, subzero, theswarm, tsunami, windstorm, and the 5 seasonal ones (halloween, roasting,
rudolph, saltytree, satanclaws). Seasonal events are open: keep them outside the rotation, or turn them off.

## Timing

- Cycle: an event roughly every **45-50 min** of gameplay -> `random.enabled = true`, `random.ticks.min = 54000`,
  `random.ticks.max = 60000` (S202 config). Each dimension has its own timer.
- Event duration: official is 1 min (1200 ticks) for all of them; event mobs force-despawn after 10 min
  (`Mob Events.duration = 12000`). **Open:** keep short bursts or lengthen (e.g. 5 min siege / ~10 min whole night).

## Fixes needed for the picks (proposed, awaiting Glenn)

- **Shadow Games:** remove its `dayTime: 20000` effect. `WorldMobEventEffect` sets the clock on *all* levels, so in
  the End it would force night in the Overworld. The End has no day cycle anyway.
- **Root Riot:** loses Triffid + Tpumpkyn (cut) -> only Shambler (land) + Spriggan (sky). Proposal: add Ent + Treant.
- **Primal Fury:** loses Dawon + Feradon (cut) -> Warg, Barghest, Maug. Fine as-is.
- **Hells Fury:** sky wave loses Malwrath (cut) -> Wraith only.
- Thin rosters: Poop Party (Conba), Cinderfall (Cinder), Winters Grasp (Wendigo, Serpix).
- Events ignore biomes (`ignoreBiomes: true`), e.g. Winters Grasp can spawn Wendigos in deserts. Optional: biome conditions.
- Nether weather effects (Cinderfall stops rain, Eruption starts thunder) likely do nothing there: Nether level data
  is derived from the Overworld. Verify in testing.

## Reference: current roster of the kept events

| Event | Land / sky mobs | World effect |
|---|---|---|
| Shadow Games | Darkling, Shade / Grue, Spectre | night (to remove) |
| Cinderfall | Cinder | stop rain |
| Eruption | Cherufe, Umibas, Volcan, Tremor | stop rain, thunder |
| Hells Fury | Belphegor, Behemophet, Apollyon / Wraith, ~~Malwrath~~ | - |
| Black Plague | Geist, Ghoul, Necrovore / Reaper, Banshee | night |
| Poop Party | Conba | - |
| Bamstorm | Kobold, Conba, Belphegor, Aglebemu / Zephyr, Raidra, Balayang, Tremor | rain + thunder |
| Winters Grasp | Wendigo, Serpix | - |
| Root Riot | Shambler, ~~Triffid~~, ~~Tpumpkyn~~ / Spriggan | - |
| Winged Venom | Remobra, Eechetik, Xaphan | - |
| Boulderdash | Geonach, Vapula | - |
| Primal Fury | Warg, Barghest, Maug, ~~Dawon~~, ~~Feradon~~ | - |
