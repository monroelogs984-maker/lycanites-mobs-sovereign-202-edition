# Equipment rework: imprinted parts

**DRAFT (2026-09-30).** Glenn's idea (`LYCANITES-REVIEW.txt`): *"Any tool that does damage can have an imprinted passive
effect from an infused equipment part, managed and leveled via the equipment forge."* Originally deferred past the
first release (2026-09-26); Glenn pulled it forward on 2026-09-30.

## Official system (what exists)

- **59 parts**, 7 slot types: head 15, base 11, jewel 10, axe 7, blade 6, pike 5, pommel 5. Levels 1-3; each part
  lists its own elements (old element set) and 1-3 features per level band.
- **Assembly:** the Equipment Forge (lesser / greater / master = max part level 1 / 2 / 3) snaps parts into one
  Lycanites weapon via `slot` features (e.g. a base with head + pommel slots).
- **Leveling:** the **Equipment Infuser** feeds charges into a part for experience -> levels 1-3. This is the *only* XP
  source (kills with the weapon give none).
- **Equipment Station:** repairs sharpness and mana, the weapon's two resources.
- In the port: parts, forges, infuser and station are registered as placeholders (`PortPlaceholder.EQUIPMENT`).

**Feature types across all parts:** effect 77, projectile 73, damage 57, harvest 50, slot 35, summon 10.
- `effect`: on-hit debuff to the target (poison, wither, slowness, paralysis, fear, bleed...) or a self buff
  (strength, resistance, invisibility, leech).
- `projectile`: mostly **secondary** (right-click fires, with a cooldown), some **primary** (fires on swing), 2 **hit**
  (proc on hit: sylphwing aetherwave, vapulacrystal crystalshard).
- `damage`: bonus attack damage. `summon`: chance to summon a minion on hit (10 parts).
- `harvest`: mining/tool types (pickaxe, hoe, shears...). `slot`: which part types attach (assembly only).

**Structural-only parts (9, all level 1, no creature):** woodenrod, woodenguard, woodenpaxel, ironrod, ironguard,
ironpaxel, ironaxehead, ironpikejoint, goldscepterhead. No passive to imprint.
**Parts from cut creatures:** bruchaquill (Brucha), eyewigeye (Eyewig).
**10 jewel parts are projectile-only** (afritlung, arixbrain, cherufecore, conbabutt, eyewigeye, gammasphere,
malwratheye, naxiriseye, sprigganheart, stryderheart).

## What carries over (proposal)

- Imprintable passives: `effect`, `damage`, `summon`, and `hit` projectiles.
- Dropped: `harvest` and `slot` (the host tool already defines both) and **sharpness** (the host tool's own durability
  replaces it). **Mana stays** (see below).
- Scrapped: the 9 structural parts, plus the 2 from cut creatures -> **48 imprintable parts**.
- Part elements remapped with the element rework (acid, fae, lightning, arbour, phase, light -> new set; Void kept).

## Decided (Glenn 2026-09-30)

- **Eligible items:** any item with an attack-damage attribute (swords, axes, tridents, maces, modded melee such as BMM
  weapons), plus a `lycanitesmobs:imprintable` item tag to add items (e.g. bows) or exclude them.
- **One imprint per tool.**
- **Extract at the forge:** the part comes back out at its current level (and mana), so it can move to a new weapon.
- **Passive or ability, per part:** some parts give a passive, others a right-click ability (draft table below).
- **Mana:** the imprint has the official second durability, mana (max 1500; each projectile shot or on-hit trigger
  costs 1). At 0 mana the imprint goes dormant until recharged; the tool itself keeps working. Official recharge
  items: low 50 (redstone, glowstone dust, slime ball), medium 100 (lapis, blaze powder, gunpowder, phantom membrane,
  frostyfur, poisongland, geistliver), high 500 (experience bottle, magma cream, any Lycanites charge), max (nether star).

## Per-part draft (edit me)

Rule used: a part whose features are *only* projectiles -> right-click ability; otherwise a passive (on-hit
effects, self buffs, bonus damage, summon chance, `hit` procs). The 6 parts marked _(drops right-click ...)_ have both:
pick passive or ability. 17 abilities, 31 passives, 2 cut. Note: some "abilities" are official `primary` triggers
(fire on swing: behemophethand, belphegorarm, gammasphere, malwratheye, sprigganheart, stryderheart), which could
instead be on-swing passive procs.

- **afritlung** (afrit): **Ability:** right-click scorchfireball
- **apollyonclaw** (apollyon): **Passive:** strength (self)
- **argustail** (argus): **Passive:** instability, summon argus _(drops right-click chaosorb; or make it the ability)_
- **arixbrain** (arix): **Ability:** right-click icefireball
- **astarothclaw** (astaroth): **Passive:** wither, +damage _(drops right-click devilstar; or make it the ability)_
- **bansheeeye** (banshee): **Passive:** fear, +damage, summon banshee
- **behemophethand** (behemophet): **Ability:** right-click hellfireball
- **belphegorarm** (belphegor): **Ability:** right-click doomfireball
- **bruchaquill** (brucha): **CUT** (creature cut)
- **cherufecore** (cherufe): **Ability:** right-click magma
- **cinderblade** (cinder): **Passive:** burning, +damage, summon cinder _(drops right-click ember; or make it the ability)_
- **clinkscythe** (clink): **Passive:** aphagia, +damage _(drops right-click throwingscythe; or make it the ability)_
- **conbabutt** (conba): **Ability:** right-click poop
- **darklingskull** (darkling): **Passive:** leech (self), +damage, summon darkling
- **eechetikarm** (eechetik): **Passive:** poison
- **entarm** (ent): **Passive:** leech (self)
- **epionwing** (epion): **Ability:** right-click bloodleech (+damage passive)
- **erepededrill** (erepede): **Ability:** right-click mudshot
- **ettinclub** (ettin): **Passive:** +damage
- **eyewigeye** (eyewig): **CUT** (creature cut)
- **frostweaverleg** (frostweaver): **Ability:** right-click frostweb
- **gammasphere** (wisp): **Ability:** right-click lightball
- **geonachfist** (geonach): **Passive:** weight, +damage
- **geonachspear** (geonach): **Passive:** resistance (self), +damage
- **grueclaw** (grue): **Passive:** blindness, invisibility (self), +damage, summon grue
- **ioraystinger** (ioray): **Ability:** right-click waterjet
- **lacedonhead** (lacedon): **Passive:** weight
- **malwratheye** (malwrath): **Ability:** right-click demonicspark/demonicblast
- **naxiriseye** (naxiris): **Ability:** right-click arcanelaserstorm
- **raidrablade** (raidra): **Passive:** paralysis, +damage, summon raidra
- **reaperclaw** (reaper): **Ability:** right-click spectralbolt (+damage passive)
- **reiverhorns** (reiver): **Passive:** slowness, +damage _(drops right-click frostbolt; or make it the ability)_
- **remobrawing** (remobra): **Passive:** poison, +damage, summon remobra _(drops right-click venomshot; or make it the ability)_
- **serpixmandible** (serpix): **Ability:** right-click blizzard (+damage passive)
- **spectretendril** (spectre): **Passive:** decay, +damage, summon spectre
- **sprigganheart** (spriggan): **Ability:** right-click summoningseed/lifedrain
- **sprigganroot** (spriggan): **Passive:** poison, summon spriggan
- **stryderheart** (stryder): **Ability:** right-click aquapulse
- **sutiramustinger** (sutiramu): **Passive:** slowness, hunger
- **sylphwing** (sylph): **Passive:** smited, aetherwave proc
- **uvaraptorskull** (uvaraptor): **Passive:** slowness, +damage
- **vapulacrystal** (vapula): **Passive:** aphagia, crystalshard proc, +damage
- **ventoraptorskull** (ventoraptor): **Passive:** weakness, +damage
- **vespidstinger** (vespid): **Passive:** poison, penetration
- **wargskull** (warg): **Passive:** paralysis, bleed, +damage
- **wendigoantler** (wendigo): **Passive:** slowness, +damage
- **wildkinarm** (wildkin): **Passive:** resistance (self)
- **wraithskull** (wraith): **Passive:** wither, +damage
- **xaphanspine** (xaphan): **Passive:** penetration
- **zephyrcloud** (zephyr): **Passive:** levitation, summon zephyr
