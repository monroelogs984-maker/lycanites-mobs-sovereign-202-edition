# Changes from the original Lycanites Mobs

Lycanites Mobs - Sovereign 202 Edition is a from-scratch port of Lycanites Mobs by Lycanite (official source:
Forge 1.20.1, `gitlab.com/Lycanite/LycanitesMobs`) to **NeoForge 1.21.1**, reworked as the core creature mod of
the Sovereign 202 modpack. All creatures, models, textures and sounds are Lycanite's work. This list covers what
differs from the official 1.20.1 version.

The mod id stays `lycanitesmobs`, so addons and packs that reference it (Lycanites Kin, lycanoculus, Lycanite's
Companions) keep working.

## Platform

- Ported to NeoForge 1.21.1 (data components, new registries, payload networking, 1.21 data pack layout).
- Creatures render through vanilla render types and entity shaders instead of the official custom VBO renderer, so
  **Iris/Oculus shader packs work with no compat layer** (tested with Complementary Unbound).
- Same OBJ models and animations as the official mod (no GeckoLib conversion).

## Creatures

- **111 creatures** (10 cut): Geken, Triffid, Eyewig, Malwrath, Feradon, Brucha, Abtu, Cryptkeeper, Tpumpkyn,
  Dawon. Their equipment parts, dungeon spawns and event spawns are removed with them.
- **Creature levels are removed.** Every creature is level 1; dungeon bosses get the stat multipliers their old level
  gave, so they keep their strength. (In Sovereign 202, Power Scale handles scaling.)
- **Breeding is removed.** Food and cooking stay.
- **Types rebalanced** so every type has at least 3 creatures; the empty Angel and Slime types are now used.
  Moves: Lacedon (amphibian), Jabberwock (anthronian), Balayang and Cockatrice (avian), Clink and Vapula (golem),
  Darkling (arachnid), Grell, Abaia and Xaphan (slime), Wisp, Aegis, Banshee and Raidra (angel).
- **Variants:** base form 80%, each rare variant 2%, uncommon variants share the rest.
- **Wraith** now spawns naturally (any Nether biome, upper half). Officially its biome list could never match.
- **Kobolds** now actually steal items (broken upstream).

## Elements

- **15 elements** instead of 29: Fire, Water, Earth, Air, Order, Chaos, Shadow, Lava, Aether, Frost, Quake, Poison,
  Nether, Arcane, plus Void (End forms only).
- Arbour, Fae, Lightning, Phase, Acid and Light are merged into the remaining elements, and their effects move with
  them (for example, Air gains Static Aura and Paralysis, Shadow gains Speed and Fear, Chaos gains Bleed). The 7 empty
  placeholder elements are removed. No effect is lost. Element fusion works as before.
- Beastiary text rewritten for the new elements and types. Credit to Lycanite kept.

## Taming, summoning and pets

- **Every non-boss creature is either tameable or summonable** (57 tameable, 48 summonable; officially 37 were
  neither). Grell is summon-only now, so it can't be ridden.
- **Bond (replaces pet levels):** bound pets and mounts grow from Bond 1 to 3 through time out together (+1 per
  minute) and kills together (+5 each). Bond 2: health x1.25, damage x1.15. Bond 3: health x1.5, damage x1.3,
  defense x1.1. Bond survives death and respawn and shows in the Beastiary.
- **Summoning mastery:** a new Beastiary knowledge rank 3 (2000 more knowledge). Staff summons of a mastered
  creature get Bond 2 stats and 1.5x duration.
- Patreon familiars (online service) removed.
- Perched pets no longer block the owner's crosshair.

## Equipment (reworked)

- Assembled Lycanites weapons are replaced by **imprints**: any item with attack damage and attack speed (vanilla or
  modded weapons and tools) can carry one equipment part. Item tags `lycanitesmobs:imprintable` /
  `lycanitesmobs:not_imprintable` handle exceptions.
- 47 parts remain. Each gives a **passive** (on-hit effects, self buffs, bonus damage, summon chance, hit procs) or a
  **right-click ability** (its projectile). Parts use mana (max 1500, 1 per trigger). At 0 mana the imprint goes
  quiet and the tool keeps working.
- **Equipment Forge:** imprint or extract (the part comes back out with its level and mana); the forge tier caps the
  part level. **Infuser:** levels parts with charges. **Station:** recharges mana. All three have new screens with an
  info panel.
- The 9 structural parts (rods, guards, paxels...) and the 3 parts of cut creatures are removed. Sharpness is gone
  (the host tool's durability is used).

## Spawning

- **Spawn budget:** instead of each world spawner firing batches on its own timer, natural spawning keeps
  **6-10 wild Lycanites on the surface and 3-5 in caves and water** around each player (configurable). The aim is
  "a few of everything" rather than mobs everywhere.
- **Equal chances:** one weighted pick runs over every creature that can spawn at the spot, using only two global
  weights (common 8 / rare 6, 36 creatures rare). A creature's spawner and batch size no longer decide how common
  it is. Packs still spawn as packs, using the creature's own group size.
- **Climate instead of biome lists** (Overworld): each creature's biome list becomes a temperature/humidity band of
  equal size for every creature, so modded biomes work without tags. Creatures that were only in cold biomes never
  reach plains temperature or warmer, and desert creatures never reach plains temperature or colder. Excluded biomes
  still apply (Silex and Stryder stay freshwater). Nether and End use the dimension rule only.
- Water creatures have climates too (Abaia, Jengu, Silex, Stryder, Thresher).
- Natural spawns despawn normally and get 2 minutes of idle time before despawning (vanilla: 30 s), so you see them
  before they vanish.
- Old Forge biome tags (`forge:is_snowy` etc.), which matched nothing on NeoForge, are mapped to the `c:` tags.

## Trigger spawners

- Retuned chances: ore 3%, gem 1%, tree 1.5%, crop 2%, mix 0.25%, explosion 4% / 6% (player), fishing 3%,
  death 0.5%, undeath 2%, sleep 2%. The darkness check runs every 7.5 s.
- **Chaos** (Argus after killing elementals) is enabled with warnings and resets each dawn. Its official reset could
  never fire. Disruption and Pumpkin are disabled.
- Death only counts hostile kills. Mix needs a player nearby (no Xaphans from unattended cobblestone generators).
- Block triggers need a player harvesting the block (structures popping grass no longer spawn Spriggans). The tree
  trigger counts trunks properly (upstream bug).

## Mob events

- **12 events** in a fixed rotation per dimension, one every 45-50 minutes, each lasting 1 minute:
  - Overworld: Black Plague, Poop Party, Bamstorm, Winters Grasp, Root Riot, Winged Venom, Boulderdash, Primal Fury.
  - Nether: Cinderfall, Eruption, Hells Fury.
  - End: Shadow Games.
- 18 events disabled, including all seasonal/holiday events and the random boss events.
- Root Riot gains Ent and Treant. Shadow Games no longer forces night.

## Bosses and altars

- Altar block structures and the 8 rare-variant altars are removed.
- Each boss has **one pedestal and one soulkey**: using the key teleports you to the boss arena (Rahovart: Nether,
  Asmodeus: outer End, Amalgalich: Overworld 500+ blocks from spawn) and brings you back 10 s after the boss dies.
  The red soulkey is now Asmodeus's and is the ingredient of the other two; cyan is Rahovart's.
- `/kill` and the void now work on bosses (the damage limit used to block them).

## Dungeons and world

- All 7 official dungeons kept, generated as real 1.21 structures. `/lm dungeons locate` uses the vanilla locator.
  Nether towers no longer break through the bedrock roof. Fixed: Cherufe never spawned in the Ashen Mausoleum
  (mod id typo upstream).
- Acid, moglava, ooze and poison lakes and springs generate as before.

## Removed tools

- The recolor debug tool. The 32x texture pack is not bundled.

## Bug fixes over the official 1.20.1

- Water creatures were treated as air breathers by 5 AI goals (they floated instead of sinking).
- Creature saves no longer read the world (a Concapede could freeze the server on chunk unload).
- Projectiles spawned by `/summon` or dispensers had no texture. The projectile render had yaw and partial-tick
  parameters swapped.
- Plus the Kobold, Wraith, Chaos reset, tree trigger, block trigger, dungeon height and Ashen Mausoleum fixes above.

## Commands (new)

- `/lm spawning stats`: wild creatures near you against the budget, recent spawns and their cost.
- `/lm creatures climate`: each creature's climate band.
- `/lm imprint set|extract|mana`: admin tools for imprints.
