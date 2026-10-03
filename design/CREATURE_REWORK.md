# Creature rework: elements + types

**Approved by Glenn (2026-09-30)** - not applied to the mod yet. When this is final I apply it to the element and creature JSONs.

**Target: 14 elements** = 6 base (Fire, Water, Earth, Air, Order, Chaos) + Shadow, Lava, Aether, Frost, Quake, Poison, Nether, Arcane.
**Dropped (7 used):** Arbour, Fae, Lightning, Phase, Void, Acid, Light. **Deleted (7 empty placeholders):** Chrono, Fate, Flux, Gravity, Nova, Vortex, Xeno.

**How effects work:** a creature applies **all** of its element's debuffs on each elemental hit, and all buffs to itself/allies. So every relocated debuff raises that element's creatures' power. This draft keeps every effect (none dropped) with at most 2 debuffs per element. If an element feels too strong, move a debuff onto specific creatures' own attacks instead, or drop it.

## Elements (applied 2026-09-30, 99837b4)

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

## Creatures (all 121)

Format: `creature (type): old -> new`. **Bold** = changes; edit the part after the arrow.

- **abaia** (aquatic): lightning -> **water, air**. Electric eel: water, plus Air for Paralysis/Static Aura (from Lightning).
- abtu (aquatic): water -> water
- aegis (elemental): order -> order
- afrit (imp): fire -> fire
- aglebemu (amphibian): poison -> poison
- amalgalich (undead): nether -> nether
- apollyon (demon): fire -> fire
- argus (elemental): chaos -> chaos
- **arisaur** (reptile): arbour -> **earth**. Plant-covered sauropod; Arbour to Earth.
- arix (imp): frost -> frost
- asmodeus (aberration): nether -> nether
- aspid (reptile): poison -> poison
- astaroth (aberration): nether -> nether
- **balayang** (beast): shadow, fae -> **shadow**. Cyclops bat hunter; drops Fae. (Add Chaos if you want its Bleed.)
- **banshee** (elemental): phase -> **shadow**. "Phase Elemental" that sings: Shadow gets Phase's Fear + Speed.
- **barghest** (beast): fae -> **quake**. Leaping mountain predator; Quake's Jump Boost fits.
- behemophet (demon): nether -> nether
- belphegor (demon): nether -> nether
- bobeko (beast): frost -> frost
- brucha (beast): order -> order
- **calpod** (insect): arbour -> **earth**. Termite swarm; Arbour to Earth.
- cephignis (aquatic): lava -> lava
- cherufe (golem): lava -> lava
- chupacabra (beast): chaos -> chaos
- cinder (elemental): fire -> fire
- clink (imp): earth -> earth
- **cockatrice** (dragon): arcane, lightning -> **arcane, air**. Keeps Arcane; Lightning to Air.
- conba (beast): arcane -> arcane
- **concapede** (insect): arbour -> **earth**. Burrowing linked beetle; Arbour to Earth.
- **concapedesegment** (insect): arbour -> **earth**. Same as Concapede.
- crusk (worm): earth -> earth
- cryptkeeper (undead): arcane -> arcane
- darkling (insect): shadow -> shadow
- dawon (beast): quake, arcane -> quake, arcane
- eechetik (elemental): poison -> poison
- **ent** (plant): arbour -> **earth**. Forest protector; Arbour to Earth.
- epion (beast): shadow -> shadow
- erepede (insect): arcane -> arcane
- ettin (anthronian): quake -> quake
- eyewig (insect): poison -> poison
- feradon (beast): order -> order
- frostweaver (arachnid): frost -> frost
- geist (undead): shadow -> shadow
- geken (reptile): air, poison -> air, poison
- geonach (elemental): earth -> earth
- ghoul (undead): poison -> poison
- gnekk (imp): poison -> poison
- gorgomite (insect): earth -> earth
- **grell** (aberration): fae -> **poison**. Floating flesh that makes deadly acid.
- **grigori** (aberration): fae -> **chaos**. Swarming flesh (Asmodeus minion); Chaos now carries Bleed.
- grue (elemental): shadow -> shadow
- herma (aquatic): water -> water
- ignibus (dragon): lava -> lava
- ika (worm): earth -> earth
- ioray (aquatic): arcane, water -> arcane, water
- jabberwock (beast): earth -> earth
- jengu (elemental): water -> water
- jouste (insect): quake -> quake
- joustealpha (insect): quake -> quake
- kathoga (demon): quake -> quake
- khalk (reptile): lava -> lava
- kobold (beast): order, quake -> order, quake
- krake (aberration): arcane, quake -> arcane, quake
- lacedon (aquatic): water, earth -> water, earth
- lycosa (arachnid): poison -> poison
- maka (beast): order -> order
- makaalpha (beast): order -> order
- malwrath (demon): nether -> nether
- maug (beast): frost -> frost
- morock (dragon): quake, earth -> quake, earth
- naxiris (aberration): arcane -> arcane
- necrovore (undead): nether -> nether
- **ningen** (amphibian): arbour -> **water**. Water-bound amphibian; Arbour made no sense.
- **nymph** (elemental): fae -> **aether**. Healing "Fae Elemental"; Aether gets Regeneration.
- ostimien (insect): poison, chaos -> poison, chaos
- **pixen** (imp): fae -> **chaos**. Mischievous imp; Chaos. (Alt: Aether.)
- quetzodracl (dragon): air -> air
- rahovart (demon): nether -> nether
- **raidra** (elemental): lightning -> **air**. Electric Elemental; Air gets Static Aura + Paralysis.
- raiko (avian): earth -> earth
- **reaper** (undead): phase, nether -> **shadow, nether**. Death spirit; Phase to Shadow, keeps Nether.
- reiver (elemental): frost -> frost
- remobra (dragon): poison -> poison
- roa (aquatic): water -> water
- roc (avian): order -> order
- salamander (amphibian): lava -> lava
- serpix (worm): frost -> frost
- **shade** (aberration): void -> **shadow**. "Spawned from the very shadows"; Void to Shadow.
- **shambler** (plant): arbour -> **earth**. Overgrown shrub; Arbour to Earth. (Alt: Poison.)
- silex (aquatic): water -> water
- skylus (aquatic): shadow -> shadow
- **spectre** (elemental): void -> **chaos**. "Break reality into nothing"; Void to Chaos. (Alt: Nether, which gets Void's Leech/Decay.)
- **spriggan** (elemental): arbour -> **earth**. "Arbour Elemental" (Jengu+Geonach fusion); Earth.
- stryder (aquatic): water -> water
- sutiramu (arachnid): arcane, quake -> arcane, quake
- sylph (elemental): aether -> aether
- thresher (reptile): water -> water
- **tpumpkyn** (plant): phase -> **shadow**. Pumpkin ghost; Phase to Shadow.
- **treant** (plant): arbour -> **earth**. Big forest guardian; Arbour to Earth.
- tremor (elemental): quake -> quake
- triffid (plant): poison -> poison
- trite (aberration): nether -> nether
- troll (anthronian): earth, quake -> earth, quake
- umibas (worm): lava -> lava
- uvaraptor (avian): air, quake -> air, quake
- vapula (elemental): arcane -> arcane
- ventoraptor (avian): air -> air
- vespid (insect): poison -> poison
- vespidqueen (insect): poison -> poison
- volcan (elemental): lava -> lava
- **vorach** (demon): fae -> **shadow**. Stalks unseen, fast out of sight: Shadow (Invisibility + Speed).
- warg (beast): shadow, quake -> shadow, quake
- wendigo (undead): frost -> frost
- wildkin (anthronian): earth -> earth
- **wisp** (elemental): light -> **aether**. "Light Elemental" throwing radiation; Aether gets Light's Glowing + Night Vision.
- wraamon (beast): chaos -> chaos
- wraith (elemental): nether -> nether
- **xaphan** (elemental): acid -> **poison**. "Acid Elemental"; Poison gets Acid's Immunization.
- yale (aberration): earth -> earth
- zephyr (elemental): air -> air
- zoataur (dragon): earth -> earth

Fusion is defined by creature pairs, not elements, so it keeps working. But 7 elementals lose their namesake element (Banshee = Phase, Nymph = Fae, Raidra = Lightning, Spectre = Void, Spriggan = Arbour, Wisp = Light, Xaphan = Acid), so their Beastiary text needs updating.

## End forms (Glenn 2026-09-30)

Astaroth (Void form), Trite (Void form) and Kathoga (Moloch form) are End-only subspecies with their own models. They
always replace the base form in the End. **Kept as-is, including the Void element.** So **Void stays as a 15th
element**, used only by these three End forms. Shade and Spectre still move off Void as mapped above. Void keeps its
own effects (Leech, Decay); Nether also gains them per the table above. Variant rule per form: 80 / 10 / 10 (2
uncommon colours each).

## Creature type moves (approved by Glenn, 2026-09-30; applied 2026-09-30, 99837b4)

Goal: even out the groups, every type at least 3 members. Types don't affect fusion. Beastiary text for the moved "X Elementals" gets rewritten with the element rework. Taming/summoning is being reworked separately, so tameability is ignored here.

- lacedon: aquatic -> **amphibian**
- jabberwock: beast -> **anthronian**
- balayang: beast -> **avian**
- cockatrice: dragon -> **avian**
- clink: imp -> **golem**
- vapula: elemental -> **golem**
- eyewig: insect -> **arachnid** (on the cut list)
- darkling: insect -> **arachnid**
- grell: aberration -> **slime**
- abaia: aquatic -> **slime**
- xaphan: elemental -> **slime**
- wisp: elemental -> **angel**
- aegis: elemental -> **angel**
- banshee: elemental -> **angel**
- raidra: elemental -> **angel**

Group sizes, now -> after moves and cuts:

- elemental: 21 -> 15
- beast: 16 -> 12
- insect: 12 -> 10
- aberration: 9 -> 8
- aquatic: 10 -> 7
- undead: 7 -> 7
- avian: 4 -> 6
- demon: 7 -> 6
- dragon: 6 -> 5
- amphibian: 3 -> 4
- reptile: 5 -> 4
- arachnid: 3 -> 4
- anthronian: 3 -> 4
- imp: 5 -> 4
- angel: 0 -> 4
- plant: 5 -> 4
- worm: 4 -> 4
- slime: 0 -> 3
- golem: 1 -> 3

## Final cut list (10, Glenn 2026-09-30; removed 2026-09-30, 99837b4)

geken, triffid, eyewig, malwrath, feradon, brucha, abtu, cryptkeeper, tpumpkyn, dawon. 121 - 10 = **111 planned creatures**.
Every type keeps 3+ members (verified by script).

## Spawn rarity (Glenn 2026-09-30; FINAL, applied 2026-09-30, 99837b4)

Two global spawn rates (common, rare >= 65% of common). Not rated (no natural spawn): rahovart, asmodeus, amalgalich
(altar bosses), concapedesegment, joustealpha, makaalpha (parts), sylph (summon/fusion only).
**Aegis is rated common:** it already spawns naturally through `structurespawns/village.json` (all villages, weight 8,
1-3 per group, max 6 per village). The earlier draft wrongly listed it as unrated.

**Rare (36 of 104 rated, 35%):** eechetik, grue, barghest, conba, wraamon, erepede, ostimien, astaroth, naxiris, shade,
herma, ioray, stryder, cockatrice, apollyon, vorach, ignibus, morock, quetzodracl, zoataur, aglebemu, ningen, thresher,
sutiramu, ettin, jabberwock, troll, reaper, afrit, arix, raidra, serpix, umibas, grell, cherufe, treant.
Everything else rated is **common**.

Resolved flags (Glenn 2026-09-30): **Wildkin -> common** (Anthronian was 100% rare; Troll, Ettin and Jabberwock stay
rare). **Reaper -> rare** (Undead had no rare). **Aegis -> common** (village spawn above).
