# Element rework: mapping draft

Draft for Glenn to edit (2026-09-30). Nothing in the mod has changed yet. When this is final I apply it to the element and creature JSONs.

**Target: 14 elements** = 6 base (Fire, Water, Earth, Air, Order, Chaos) + Shadow, Lava, Aether, Frost, Quake, Poison, Nether, Arcane.
**Dropped (7 used):** Arbour, Fae, Lightning, Phase, Void, Acid, Light. **Deleted (7 empty placeholders):** Chrono, Fate, Flux, Gravity, Nova, Vortex, Xeno.

**How effects work:** a creature applies **all** of its element's debuffs on each elemental hit, and all buffs to itself/allies. So every relocated debuff raises that element's creatures' power. This draft keeps every effect (none dropped) with at most 2 debuffs per element. If an element feels too strong, move a debuff onto specific creatures' own attacks instead, or drop it.

## Elements (proposed)

| Element | Fusion | Creatures now -> after | Buffs | Debuffs | Change |
|---|---|---|---|---|---|
| Fire | base | 3 -> 3 | fire_resistance | burning |  |
| Water | base | 9 -> 11 | water_breathing | mining_fatigue |  |
| Earth | base | 13 -> 21 | resistance, rejuvenation | weight | +Rejuvenation (Arbour) |
| Air | base | 5 -> 8 | fallresist, staticaura | levitation, paralysis | +Static Aura, +Paralysis (Lightning) |
| Order | base | 7 -> 7 | absorption | weakness |  |
| Chaos | base | 4 -> 7 | haste | instability, bleed | +Bleed (Fae) |
| Shadow | fire+chaos | 7 -> 12 | invisibility, speed | blindness, fear | +Speed, +Fear (Phase) |
| Lava | fire+earth | 7 -> 7 | heataura | smouldering |  |
| Aether | air+order | 1 -> 3 | cleansed, regeneration, night_vision | smited, glowing | +Regeneration (Fae), +Night Vision, +Glowing (Light) |
| Frost | water+air | 7 -> 7 | freezeaura | slowness |  |
| Quake | earth+chaos | 13 -> 14 | jump_boost | hunger, penetration | +Penetration (Acid) |
| Poison | water+chaos | 13 -> 15 | envenom, immunization | plague, poison | +Immunization (Acid) |
| Nether | air+chaos | 11 -> 11 | strength, leech | wither, decay | +Leech, +Decay (Void) |
| Arcane | earth+order | 10 -> 10 | saturation | aphagia |  |

No effect is dropped. Mind: Aether would carry 3 buffs; Air, Chaos, Shadow, Quake, Poison and Nether 2 debuffs each.

## Creatures that change (26): suggestions

| Creature | Type | Current | Proposed | Why |
|---|---|---|---|---|
| abaia | aquatic | lightning | **water, air** | Electric eel: water, plus Air for Paralysis/Static Aura (from Lightning). |
| arisaur | reptile | arbour | **earth** | Plant-covered sauropod; Arbour to Earth. |
| balayang | beast | shadow, fae | **shadow** | Cyclops bat hunter; drops Fae. (Add Chaos if you want its Bleed.) |
| banshee | elemental | phase | **shadow** | "Phase Elemental" that sings: Shadow gets Phase's Fear + Speed. |
| barghest | beast | fae | **quake** | Leaping mountain predator; Quake's Jump Boost fits. |
| calpod | insect | arbour | **earth** | Termite swarm; Arbour to Earth. |
| cockatrice | dragon | arcane, lightning | **arcane, air** | Keeps Arcane; Lightning to Air. |
| concapede | insect | arbour | **earth** | Burrowing linked beetle; Arbour to Earth. |
| concapedesegment | insect | arbour | **earth** | Same as Concapede. |
| ent | plant | arbour | **earth** | Forest protector; Arbour to Earth. |
| grell | aberration | fae | **poison** | Floating flesh that makes deadly acid. |
| grigori | aberration | fae | **chaos** | Swarming flesh (Asmodeus minion); Chaos now carries Bleed. |
| ningen | amphibian | arbour | **water** | Water-bound amphibian; Arbour made no sense. |
| nymph | elemental | fae | **aether** | Healing "Fae Elemental"; Aether gets Regeneration. |
| pixen | imp | fae | **chaos** | Mischievous imp; Chaos. (Alt: Aether.) |
| raidra | elemental | lightning | **air** | Electric Elemental; Air gets Static Aura + Paralysis. |
| reaper | undead | phase, nether | **shadow, nether** | Death spirit; Phase to Shadow, keeps Nether. |
| shade | aberration | void | **shadow** | "Spawned from the very shadows"; Void to Shadow. |
| shambler | plant | arbour | **earth** | Overgrown shrub; Arbour to Earth. (Alt: Poison.) |
| spectre | elemental | void | **chaos** | "Break reality into nothing"; Void to Chaos. (Alt: Nether, which gets Void's Leech/Decay.) |
| spriggan | elemental | arbour | **earth** | "Arbour Elemental" (Jengu+Geonach fusion); Earth. |
| tpumpkyn | plant | phase | **shadow** | Pumpkin ghost; Phase to Shadow. |
| treant | plant | arbour | **earth** | Big forest guardian; Arbour to Earth. |
| vorach | demon | fae | **shadow** | Stalks unseen, fast out of sight: Shadow (Invisibility + Speed). |
| wisp | elemental | light | **aether** | "Light Elemental" throwing radiation; Aether gets Light's Glowing + Night Vision. |
| xaphan | elemental | acid | **poison** | "Acid Elemental"; Poison gets Acid's Immunization. |

Fusion is defined by creature pairs, not elements, so it keeps working. But 7 elementals lose their namesake element (Banshee = Phase, Nymph = Fae, Raidra = Lightning, Spectre = Void, Spriggan = Arbour, Wisp = Light, Xaphan = Acid), so their Beastiary text needs updating.

## All other creatures (unchanged unless you edit)

| Creature | Type | Element(s) |
|---|---|---|
| asmodeus | aberration | nether |
| astaroth | aberration | nether |
| krake | aberration | arcane, quake |
| naxiris | aberration | arcane |
| trite | aberration | nether |
| yale | aberration | earth |
| aglebemu | amphibian | poison |
| salamander | amphibian | lava |
| ettin | anthronian | quake |
| troll | anthronian | earth, quake |
| wildkin | anthronian | earth |
| abtu | aquatic | water |
| cephignis | aquatic | lava |
| herma | aquatic | water |
| ioray | aquatic | arcane, water |
| lacedon | aquatic | water, earth |
| roa | aquatic | water |
| silex | aquatic | water |
| skylus | aquatic | shadow |
| stryder | aquatic | water |
| frostweaver | arachnid | frost |
| lycosa | arachnid | poison |
| sutiramu | arachnid | arcane, quake |
| raiko | avian | earth |
| roc | avian | order |
| uvaraptor | avian | air, quake |
| ventoraptor | avian | air |
| bobeko | beast | frost |
| brucha | beast | order |
| chupacabra | beast | chaos |
| conba | beast | arcane |
| dawon | beast | quake, arcane |
| epion | beast | shadow |
| feradon | beast | order |
| jabberwock | beast | earth |
| kobold | beast | order, quake |
| maka | beast | order |
| makaalpha | beast | order |
| maug | beast | frost |
| warg | beast | shadow, quake |
| wraamon | beast | chaos |
| apollyon | demon | fire |
| behemophet | demon | nether |
| belphegor | demon | nether |
| kathoga | demon | quake |
| malwrath | demon | nether |
| rahovart | demon | nether |
| ignibus | dragon | lava |
| morock | dragon | quake, earth |
| quetzodracl | dragon | air |
| remobra | dragon | poison |
| zoataur | dragon | earth |
| aegis | elemental | order |
| argus | elemental | chaos |
| cinder | elemental | fire |
| eechetik | elemental | poison |
| geonach | elemental | earth |
| grue | elemental | shadow |
| jengu | elemental | water |
| reiver | elemental | frost |
| sylph | elemental | aether |
| tremor | elemental | quake |
| vapula | elemental | arcane |
| volcan | elemental | lava |
| wraith | elemental | nether |
| zephyr | elemental | air |
| cherufe | golem | lava |
| afrit | imp | fire |
| arix | imp | frost |
| clink | imp | earth |
| gnekk | imp | poison |
| darkling | insect | shadow |
| erepede | insect | arcane |
| eyewig | insect | poison |
| gorgomite | insect | earth |
| jouste | insect | quake |
| joustealpha | insect | quake |
| ostimien | insect | poison, chaos |
| vespid | insect | poison |
| vespidqueen | insect | poison |
| triffid | plant | poison |
| aspid | reptile | poison |
| geken | reptile | air, poison |
| khalk | reptile | lava |
| thresher | reptile | water |
| amalgalich | undead | nether |
| cryptkeeper | undead | arcane |
| geist | undead | shadow |
| ghoul | undead | poison |
| necrovore | undead | nether |
| wendigo | undead | frost |
| crusk | worm | earth |
| ika | worm | earth |
| serpix | worm | frost |
| umibas | worm | lava |
