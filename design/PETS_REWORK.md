# Pets, taming and summoning rework

**DECIDED (Glenn 2026-10-03):** Bond 1-3 grows from **time out + kills together** (not Charges); new knowledge
rank 3 = summon mastery; official 3-minute respawn; perching stays (it was already ported, see below) and the official
spirit costs/cap stay. **Bond and mastery implemented 2026-10-03** (see "Implementation" at the end). The tame/summon reallocation (`TAMING_SUMMONING.md`)
was made to work in-game on 2026-10-03 (PORT_PLAN "Tame/summon reallocation, implemented").

**Drafted 2026-10-03.** LYCANITES-REVIEW said pets and summoning stay untouched for the first version. On 2026-09-30
Glenn planned a rework ("nearly everything tameable or at least summonable"; picks in `TAMING_SUMMONING.md`, applied).
He pulled the rest of the rework forward on 2026-10-03. Creature data (tame/summon flags, costs, mount/perch) stays
in the creature JSONs, not code.

## What exists now (faithful port + Phase 10)

- **Roster:** 57 tameable, 48 summonable, every non-boss creature is one or the other, never both.
  22 tameables are mountable; 11 are perchable (afrit, aglebemu, arix, darkling, gnekk, herma, kobold, pixen, remobra,
  trite, wraamon). **Correction:** perching *is* ported (2026-09-28 audit, ExtendedEntity + the "Perch" right-click);
  this draft first said otherwise from an old PORT_PLAN TODO. Not yet verified in-game.
- **Taming:** needs rank-2 Beastiary knowledge of the creature (gained by being near it, killing it, feeding it). Then
  feed it its type's treat (`treat_<type>`) to build reputation up to `tamingReputation` (default 500, about 7
  treats). Rare variants can't be tamed.
- **Binding:** a soulstone used on your tamed creature binds it as a **pet** or **mount** entry (Beastiary, Pets tab).
  A bound creature reserves **spirit** equal to its `summonCost` while it's out. Spirit: 10 units max, comes back
  in about half a second. Costs: 1-2 (63 creatures), 3-4 (27), 6 (12), 8 (7), 10 (2: ignibus, quetzodracl).
  A bound pet that dies **respawns after 3 minutes** (config `petRespawnTime`), keeping its spirit reserved.
- **Summoning:** needs rank-2 knowledge too. A summoning staff fires a portal that spawns the creature from your active summon set (5 sets,
  picked in the Beastiary). Costs **focus** (10 units, about 6 s per unit to recharge). Minions are temporary:
  Summoning staff 60 s ×1, Savage 60 s ×2, Sturdy 120 s, Stable 180 s, Blood 60 s (also costs health, needs > 7 HP).
- **Summoning Pedestal:** keeps a summon set's creature summoned permanently near the block, fuelled by redstone.
  This is how a summon-only creature becomes a permanent guard.
- **Commands / GUI:** sit, follow, passive, stance (aggressive/defensive), PvP; sneak + right-click opens the creature
  GUI (inventory, saddle / bag / armor). Pets eat foods from their `diets` to heal.
- **Gone already:** creature levels (2026-09-30: everything is level 1, so **pets never get stronger**), Patreon
  familiars (online service), charge items as pet XP (cut with levels).

## The problems

1. **No pet growth.** With levels scrapped, a pet has exactly a wild creature's stats forever. Raising one is
   cosmetic, and nothing rewards keeping the same pet. This is the main hole.
2. **Tame vs summon play the same in the late game.** A bound pet is a permanent ally that reserves spirit; a
   summon is a short burst that costs focus. Both are fine, but summon-only creatures never grow either.
3. **Death cost:** a 3-minute respawn with spirit still reserved is a soft penalty. Fine for "hard but fair", but it
   should be a deliberate choice.
4. ~~Perching is unported~~ (wrong, it was ported 2026-09-28; untested in-game).

## Decisions (recommended pick first, edit freely)

### 1. Pet growth: **Bond** (replaces levels)
- **(recommended) Bond 1-3, fed with matching-element Charges.** Right-click your bound pet with a Charge that shares
  one of its elements: the same numbers as equipment parts (50 bond per charge; 500 to Bond 2, 625 to Bond 3, so 10 +
  13 charges). No kill XP and no randomness, matching the imprint system and "low RNG". Each Bond tier adds a flat
  multiplier, proposed **Bond 2: +25% health, +15% damage; Bond 3: +50% health, +30% damage, +10% defense**. Bond is
  stored on the pet entry (survives death and respawn) and shown in the creature GUI and Beastiary.
- Bond grows by itself from time spent out + kills together (no items, slower, less control).
- No growth: pets stay at wild stats; pick a strong creature instead.

### 2. Summon-only creatures
- **(recommended) New knowledge rank 3 = mastery.** Knowledge currently stops at rank 2 (rank 1 = seen, rank 2 =
  1000 knowledge, which unlocks taming and summoning). Add a rank 3 (another 2000 knowledge, from kills/nearby/
  treats as now): your minions of that creature get Bond 2 stats and +50% summon duration. No per-minion progression
  (they're temporary).
- Summons stay as they are.

### 3. Pet death
- **(recommended) Keep the official 3-minute respawn**, spirit stays reserved. Bond is never lost.
- Harsher: the pet only comes back when you use a soulstone on its Beastiary entry (consumes the soulstone).
- Bond drops one tier on death.

### 4. Perching (already ported; Glenn: keep)
- **(recommended) Port it** (official: a small perchable pet rides on your shoulder, still attacks / shoots).
  Fits first-person (it sits at the edge of the view).
- Leave it unported and remove the flag.

### 5. Spirit and summon costs
- **(recommended) Keep the official costs and the 10-unit spirit cap.** A typical setup is one mount (4-8) plus a
  small pet (2). Ignibus / Quetzodracl (10) take the whole budget, which suits the strongest mounts.
- Raise the cap with Bond (e.g. +1 spirit per Bond-3 pet you own) or with a progression item.

### Unchanged (proposed)
Taming flow (rank-2 knowledge + treats), rare variants untameable, soulstone binding, 5 summon sets, staffs, the
Summoning Pedestal, pet commands, diets, mount controls, Soul Contract transfers, element fusion.

## Implementation (2026-10-03)

`core/entity/pets/PetBond` holds every number (tune there):
- **Bond experience:** Bond 2 at 500, Bond 3 at 1125 (the equipment-part steps). +1 per minute a bound pet/mount is
  out and alive; +5 per kill made together: the pet's own kill, or the owner's kill while the pet is within 16
  blocks. Kills count for hostile mobs, wild Lycanites creatures and players, never the owner's own pets. At roughly
  60 kills an hour that's Bond 2 in ~1.5 h and Bond 3 in ~3 h of play together.
- **Multipliers** (stack with everything else, applied where the scrapped level multiplier was): Bond 2 health x1.25,
  damage x1.15; Bond 3 health x1.5, damage x1.3, defense x1.1. A Bond-up re-applies stats and fully heals the pet.
- Stored on the PetEntry (`BondExperience`, survives death/respawn), synced in the pet entry message (the dead
  level/experience slot), shown in the Beastiary Pets page instead of the old Experience bar. Chat message on
  Bond-up. Only bound pets and mounts have Bond; tamed-but-unbound creatures stay at Bond 1.
- **Mastery:** knowledge rank 3 needs another 2000 knowledge after rank 2 (same sources as before). Staff summons of a
  mastered creature get Bond 2 and 1.5x duration. The Summoning Pedestal's minions are unaffected.

Verified with a dev-client harness (removed): tame + soulstone bind, kill credits, Bond 2/3 stat changes (Warg
20 -> 25 -> 30 health, 3.0 -> 3.45 damage), cap, NBT save, kept through death and respawn, time accrual, mastered vs
unmastered staff summons. Not checked by eye: the Beastiary Pets page Bond bar.
