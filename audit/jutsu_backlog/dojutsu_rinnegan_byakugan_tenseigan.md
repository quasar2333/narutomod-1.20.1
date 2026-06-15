# Jutsu Audit — Rinnegan + Byakugan + Tenseigan + Dojutsu Base

Faithful inventory of every jutsu/technique in the **Rinnegan / Byakugan / Tenseigan / Dojutsu-base** domain, comparing the original 1.12.2 Forge source to the in-progress 1.20.1 Forge port. Goal: a developer can restore EVERY technique (mechanics AND visuals) without missing any.

- ORIGINAL (source of truth): `D:\mcmodding\naruto\1.12.2\src\main\java\net\narutomod`
- PORT (WIP): `D:\mcmodding\naruto\1.20.1\src\main\java\net\narutomod`

---

## Framework / Notes

### Item / activation framework (1.12.2)
- **`ItemDojutsu.Base`** (`item/ItemDojutsu.java`) is the shared abstract base for all dojutsu HELMETS (Rinnegan, Tenseigan, Byakugan). It extends `ItemArmor`, slot HEAD. Provides:
  - Ownership: `isOwner` / `setOwner` via `ProcedureUtils.isOriginalOwner` / `setOriginalOwner`. Display name prefixed with `"<owner>'s "`.
  - `onArmorTick`: if a non-owner non-creative wears it, applies **Blindness 1200t** once per new foreign owner; stamps `NarutomodModVariables.MostRecentWornDojutsuTime` every tick.
  - Default armor model is the inner `ClientModel.ModelHelmetSnug` (snug 8×8×8 head box + headwear overlay + `highlight` plane + `forehead` plane, custom GL alpha/lightmap render).
- Key dispatch is NOT in the items; it is in three dispatcher procedures keyed by the helmet item and the `entity` sneak/`which_path` state:
  - **`ProcedureSpecialJutsu1OnKeyPressed`** (id 64) → key "special jutsu 1".
  - **`ProcedureSpecialJutsu2OnKeyPressed`** (id 66) → key "special jutsu 2".
  - **`ProcedureSpecialJutsu3OnKeyPressed`** (id 101) → key "special jutsu 3".
  - **`ProcedurePowerIncreaseOnKeyPressed`** (id 104) → "power up" key: cycles Rinnegan `which_path` 0..5, decreases Byakugan FOV, etc.
- Rinnegan/Tenseigan share the SAME jutsu set (both helmets are checked together everywhere). Tenseigan helmet is a Rinnegan-equivalent that additionally enables Chakra Mode.
- `which_path` (NBT double on helmet, set by `ProcedurePowerIncreaseOnKeyPressed`): 0=Deva (Shinra/Chibaku/Tengaishinsei), 1=Asura (passive armor+cannon), 2=Animal, 3=Preta, 4=Naraka, 5=Outer. Key2 behavior branches on it.
- **Rinnesharingan** activation: in `ProcedureRinneganHelmetTickEvent` when player is the Ten-Tails jinchuriki and XP ≥ 180 → sets NBT `RINNESHARINGAN_ACTIVATED`, grants advancement, gives buffs/Six Paths Senjutsu/black receiver, swaps in Rinnegan body+legs, etc.

### Item / activation framework (1.20.1 port)
- There is **no `ItemDojutsu` base port** and **no separate `TenseiganHelmetItem`**. Instead:
  - `RinneganHelmetItem(boolean tenseigan)` covers both Rinnegan and Tenseigan helmets (registry `rinneganhelmet` / `tenseiganhelmet`).
  - `ByakuganHelmetItem(boolean rinnesharinganVariant)` covers Byakugan and Byaku-Rinnesharingan helmets.
  - Ownership/blindness logic from `ItemDojutsu.Base` is **partially re-implemented** in each handler's tick; the standalone shared Base + its foreign-wearer Blindness penalty is NOT centralized (verify Blindness penalty exists — not seen in port tick code).
- Key dispatch is `network/SpecialJutsuKeyMessage` → tries `ObitoKamuiHandler`, then `ByakuganHandler.handleSpecialJutsuKey`, then `RinneganSpecialJutsuHandler.handleSpecialJutsuKey`, then `BijuManager`.
  - **`RinneganSpecialJutsuHandler.handleSpecialJutsuKey` only handles `key == 2`.** Keys 1 and 3 for Rinnegan/Tenseigan are dropped (see Shinra Tensei / Ban Sho Ten'in below).
  - `ByakuganHandler.handleSpecialJutsuKey` handles keys 1/2/3, but key 1 only does Byakugan activation — the original key1+sneak Hakke Kushō branch is missing.
  - `PowerIncreaseKeyHandler` → `RinneganSpecialJutsuHandler.handlePowerIncreaseKey` (path cycle) and `ByakuganHandler.handlePowerIncreaseKey` (FOV).
- Per-path triggers live in `RinneganSpecialJutsuHandler.triggerPath(...)`: DEVA(meteor?Tengaishinsei:Chibaku), ASURA, ANIMAL, PRETA, NARAKA, OUTER.
- All required entities are ported: `ChibakuTenseiBallEntity`, `ChibakuSatelliteEntity`, `KingOfHellEntity`, `PretaShieldEntity`, `GiantDog2hEntity`, `GedoStatueEntity`, `EightTrigramsEntity`, `HakkeshoKeitenEntity`, `TenseiBakuSilverEntity`, `TenseiBakuGoldEntity`, `TenseiganOrbEntity`, `EarthBlocksEntity`.
- Sounds `SOUND_SHINRATENSEI` and `SOUND_BANSHOTENIN` ARE registered in `registry/ModSounds.java` but are **never referenced by any handler** — direct evidence Shinra Tensei and Ban Sho Ten'in are not wired.

### Shared chakra/cost helpers (1.12.2)
Cost helpers in `ItemRinnegan` return `usage` for the owner, `usage*2` for a non-owner, or ~`Double.MAX_VALUE*0.001` if not wearing a dojutsu helmet. Same pattern in `ItemByakugan`. Constants:
- Shinra Tensei = 10/power-tick · Chibaku Tensei = 5000 · Naraka = 100 · Preta = 10 · Animal = 200 · Outer = 2000 · Tengaishinsei = 5000 (`ItemRinnegan.java:58-64`).
- Byakugan = 10/half-sec · Rokujuyonsho = 100 · Kaiten = 4/tick · Kusho = 0.5×pressDuration (`ItemByakugan.java:46-49`).

---

# RINNEGAN / TENSEIGAN JUTSU

## Shinra Tensei (神羅天征 — Almighty Push)

| Field | Value |
|---|---|
| Index/Key | Special Jutsu **Key 1** (non-sneak path) |
| Chakra | `power * getShinratenseiChakraUsage` (10/owner, 20/non-owner) — charges while held |
| Level/XP/Rank | none (charge-based power 10→100) |
| Cooldown | `world_tick + power*10*chakraModifier`; window check `cd-400..cd+100` |
| Entities | none (AoE + `SpecialEvent.setSphericalExplosionEvent` when power>20) |
| Procedures | `ProcedureSpecialJutsu1OnKeyPressed` (`:62-73`) → `ProcedureShinratenseiOnKeyPressed` (id 21) |
| Particles | 1000× `Particles.Types.SMOKE` 0x10FFFFFF burst around caster (`ProcedureShinratenseiOnKeyPressed.java:90-93`) |
| Sounds | `narutomod:ShinraTensei` if power≥20 else `narutomod:BanshoTenin` (`:95-100`) |
| Visual | Radial repulsion shockwave; smoke dome; spherical terrain explosion at power>20; grants Flight + 60t InvulnerableTime; `purgeHarmfulEffects` + extinguish |
| 1.12.2 file:line | `procedure/ProcedureShinratenseiOnKeyPressed.java` (whole), dispatched at `procedure/ProcedureSpecialJutsu1OnKeyPressed.java:62-73` |

Mechanics detail: while key held, `power` ramps +0.1/tick (start 10) up to min(maxPower, 100), applies Flight, shows "Power N" actionbar. On release if power≥5: 60t invulnerability, smoke, sound, `ProcedureAoeCommand.set(...).damageEntities(power).knockback(2)`, consumes `power*usage`, sets cooldown.

**1.20.1 status: MISSING.** `RinneganSpecialJutsuHandler.handleSpecialJutsuKey` returns false for `key != 2`, so key 1 (Shinra Tensei) is never invoked for Rinnegan/Tenseigan. No `ProcedureShinratensei` port exists. `SOUND_SHINRATENSEI` is registered but unused. NEEDS: key-1 handler branch + charge/release power model + AoE knockback/damage + spherical explosion + smoke particles + Flight/invuln + ShinraTensei/BanshoTenin sounds.

---

## Chibaku Tensei (地爆天星 — planetary devastation ball)

| Field | Value |
|---|---|
| Index/Key | **Key 2**, `which_path==0` (Deva), NOT sneaking |
| Chakra | `getChibaukutenseiChakraUsage` = 5000 (×2 non-owner) |
| Level/XP/Rank | none |
| Cooldown | 6000t (`chibakutenseicd`) |
| Entities | `EntityChibakuTenseiBall.EntityCustom` |
| Procedures | `ProcedureSpecialJutsu2OnKeyPressed:110-119` → `ProcedureChibakutenseiOnKeyPressed` (id 29) |
| Particles | (entity-driven) |
| Sounds | `narutomod:ChibakuTensei` vol 10 (`:60-62`) |
| Visual | Spawns a giant gravity ball entity that pulls/accretes blocks into a black sphere |
| 1.12.2 file:line | `procedure/ProcedureChibakutenseiOnKeyPressed.java` |

**1.20.1 status: DONE.** `RinneganSpecialJutsuHandler.triggerChibakuTensei` (`:176-202`): 6000t cooldown via `chibakutenseicd` persistent data, consumes 5000 chakra (refund on fail), `ChibakuTenseiBallEntity.spawnFrom(player)`. Reachable via key2 path 0 non-meteor. Verify ball pull/accretion visuals match in `ChibakuTenseiBallEntity`.

---

## Tengaishinsei / Meteor Strike (天碍震星 — Chibaku-Tensei Meteor)

| Field | Value |
|---|---|
| Index/Key | **Key 2**, `which_path==0`, **SNEAKING** |
| Chakra | `getTengaishinseiChakraUsage` = 5000 (×0.2 if reusing existing satellite) |
| Level/XP/Rank | none (needs structure template `narutomod:meteor`) |
| Cooldown | none direct |
| Entities | `EntityChibakuTenseiBall.Satellite` (huge falling meteor of captured blocks) |
| Procedures | `ProcedureSpecialJutsu2OnKeyPressed:78-109` → `ProcedureMeteorStrike` (id 151) |
| Particles | none |
| Sounds | `narutomod:tengaishinsei` vol 5 |
| Visual | Loads `meteor` template ~90 blocks above target, spawns a giant `Satellite` made of non-air blocks that crashes down; if a Satellite already exists, redirect it toward look target |
| 1.12.2 file:line | `procedure/ProcedureMeteorStrike.java` |

**1.20.1 status: DONE (PARTIAL on visuals — verify).** `RinneganSpecialJutsuHandler.triggerTengaishinsei` (`:204-221`) + ported `procedure/ProcedureMeteorStrike.strike(...)` with `ChibakuSatelliteEntity`, reusable-satellite redirect (×0.2 cost), template `narutomod:meteor`, structured `MeteorStrikeResult`. Sound usage: confirm `tengaishinsei` sound is played in the port (not seen in handler — original played it; check `ProcedureMeteorStrike` port). Reachable via key2 path 0 + meteor=shift.

---

## Ban Shō Ten'in (万象天引 — Universal Pull)

| Field | Value |
|---|---|
| Index/Key | Special Jutsu **Key 3** |
| Chakra | 0.5/tick (`CHAKRA_USAGE`) while grabbing/holding |
| Level/XP/Rank | none |
| Cooldown | +100t after release (`BanshoTenin_cooldown`) |
| Entities | none (pulls existing entity, or dislodges blocks via `ProcedureGravityPower.dislodgeBlocks`, spawns `EntityEarthBlocks`) |
| Procedures | `ProcedureSpecialJutsu3OnKeyPressed:62-69` → `ProcedureBanShoTenin` (id 155) → `ProcedurePullAndHold` |
| Particles | none |
| Sounds | `narutomod:BanshoTenin` (grab); `narutomod:rocks` (sneak block-dislodge) |
| Visual | Ray-traces up to 50 blocks; pulls and holds the targeted entity toward caster (held via `ProcedurePullAndHold`); when sneaking + looking at block, dislodges a 5-radius chunk of terrain as an entity and flings it |
| 1.12.2 file:line | `procedure/ProcedureBanShoTenin.java` |

**1.20.1 status: MISSING.** Key 3 for Rinnegan/Tenseigan never reaches a handler (`RinneganSpecialJutsuHandler` only handles key 2; `ByakuganHandler` ignores non-Byakugan heads). No `ProcedureBanShoTenin` / `ProcedurePullAndHold` port found. `SOUND_BANSHOTENIN` registered but unused. NEEDS: key-3 handler branch + grab-and-hold-entity loop with 0.5/tick chakra + sneak block-dislodge (`ProcedureGravityPower.dislodgeBlocks` equivalent, `EarthBlocksEntity` exists) + cooldown + death-reset hook + sounds.

---

## Naraka Path — King of Hell (地獄道)

| Field | Value |
|---|---|
| Index/Key | **Key 2**, `which_path==4` |
| Chakra | `getNarakaPathChakraUsage` = 100 (×2 non-owner) |
| Level/XP/Rank | none |
| Cooldown | none (toggle) |
| Entities | `EntityKingOfHell.EntityCustom` (id stored in helmet NBT `KoH_id`) |
| Procedures | `ProcedureSpecialJutsu2OnKeyPressed:121-127` → `ProcedureNarakaPath` (id 223) |
| Particles | none | Sounds | none |
| Visual | Summons the King of Hell head entity bound to caster; pressing again kills/dismisses it. Helmet `onUpdate` validates `KoH_id` each 20t and clears tag if entity is gone (`ItemRinnegan.java:141-152`) |
| 1.12.2 file:line | `procedure/ProcedureNarakaPath.java` |

**1.20.1 status: DONE.** `triggerNarakaPath` (`:270-295`): toggles via `KoH_id` UUID on head stack, `KingOfHellEntity.spawnFrom`/`king.dismiss()`, 100 chakra, swing arm. `KingOfHellEntity` ported. Verify the periodic `KoH_id` validity cleanup (helmet onUpdate) has an equivalent in port tick.

---

## Preta Path — Absorption Shield (餓鬼道)

| Field | Value |
|---|---|
| Index/Key | **Key 2**, `which_path==3` |
| Chakra | `getPretaPathChakraUsage` = 10 (×2 non-owner) |
| Level/XP/Rank | none |
| Cooldown | none |
| Entities | `EntityPretaShield.EntityCustom` (player rides it) |
| Procedures | `ProcedureSpecialJutsu2OnKeyPressed:128-134` → `ProcedurePretaPath` (id 239) |
| Particles/Sounds | none |
| Visual | Spawns chakra-absorption shield the caster rides; if already riding one, does nothing |
| 1.12.2 file:line | `procedure/ProcedurePretaPath.java` |

**1.20.1 status: DONE.** `triggerPretaPath` (`:252-268`): 10 chakra, swing, `PretaShieldEntity.spawnFrom` with before/after `findActive` guard, refund on total failure. `PretaShieldEntity` ported.

---

## Animal Path — Giant Summon (畜生道)

| Field | Value |
|---|---|
| Index/Key | **Key 2**, `which_path==2` |
| Chakra | `getAnimalPathChakraUsage` = 200 (×2 non-owner) |
| Level/XP/Rank | none |
| Cooldown | none (toggle via helmet NBT `SummonedAnimal_id`) |
| Entities | `EntityGiantDog2h.EntityCustom` (two-headed giant dog) |
| Procedures | `ProcedureSpecialJutsu2OnKeyPressed:135-141` → `ProcedureAnimalPath` (id 224) |
| Particles | `Particles.Types.SEAL_FORMULA` ring + vanilla `EXPLOSION_HUGE` (×300) summon poof; dismiss poof `EXPLOSION_HUGE` ×200 |
| Sounds | `narutomod:kuchiyosenojutsu` vol 2 pitch 0.8 |
| Visual | Summoning-seal ring + explosion smoke; spawns/dismisses the giant 2-headed dog |
| 1.12.2 file:line | `procedure/ProcedureAnimalPath.java` |

**1.20.1 status: DONE.** `triggerAnimalPath` (`:223-250`): toggle via `SummonedAnimal_id` int on head, 200 chakra, swing, `GiantDog2hEntity.spawnFor` / `dog.dismissWithPoof()`, `spawnSummonEffects` plays kuchiyose sound + SEAL_FORMULA + `EXPLOSION_EMITTER`. `GiantDog2hEntity` ported. Minor: original used `EXPLOSION_HUGE`×300/×200; port uses a single `EXPLOSION_EMITTER` — cosmetic delta.

---

## Outer Path — Gedo Statue (外道魔像)

| Field | Value |
|---|---|
| Index/Key | **Key 2**, `which_path==5` |
| Chakra | `getOuterPathChakraUsage` = 2000 (×2 non-owner) |
| Level/XP/Rank | Ninja level ≥ 90; blocked if Ten-Tails already has a jinchuriki and you aren't it |
| Cooldown | none |
| Entities | `EntityGedoStatue.EntityCustom` |
| Procedures | `ProcedureSpecialJutsu2OnKeyPressed:142-160` → `ProcedureOuterPath` (id 253) |
| Particles | vanilla `EXPLOSION_HUGE` ×300 |
| Sounds | `narutomod:kuchiyosenojutsu` vol 2 pitch 0.9 |
| Visual | Summons the Gedo Mazo statue at look target; toggling removes it; grants 100t InvulnerableTime; messages for not-enough-XP / has-jinchuriki |
| 1.12.2 file:line | `procedure/ProcedureOuterPath.java` |

**1.20.1 status: DONE (delegated).** `triggerOuterPath` (`:307-309`) → `SixPathSenjutsuItem.triggerOuterPath(player, showMessages)`. `GedoStatueEntity` ported. Verify the SixPathSenjutsu port enforces ninja-level 90 + jinchuriki check + invuln + sound; this audit confirms the path is wired through key2 path 5 (and via `SixPathSenjutsuItem`).

---

## Asura Path (passive armor + Asura Cannon)

| Field | Value |
|---|---|
| Index/Key | **Key 2**, `which_path==1` (mostly passive); maintained in helmet tick |
| Chakra | n/a (passive) |
| Level/XP/Rank | none |
| Cooldown | none |
| Entities | none (equips `ItemAsuraPathArmor.body` chest + `ItemAsuraCanon` offhand) |
| Procedures | `ProcedureRinneganHelmetTickEvent:158-173` (auto-equip when `which_path==1`) |
| Particles/Sounds | none |
| Visual | Auto-swaps Asura mechanical chest armor + offhand Asura Cannon while path 1 selected; removes them otherwise. Cannon is its own ranged item |
| 1.12.2 file:line | `procedure/ProcedureRinneganHelmetTickEvent.java:158-173` |

**1.20.1 status: DONE.** `RinneganSpecialJutsuHandler.maintainAsuraPath` (`:118-140`) + `tickAsuraBody` (buff pulse) + `clearInactiveAsuraGear`, driven each player tick (`onPlayerTick`) and from `applyBaseRinneganBranch`. Key2 path 1 → `triggerAsuraPath` arms it. `ASURAPATHARMORBODY`/`ASURACANON` ported.

---

## Rinnegan passive buff suite (Six Paths body / Rinnesharingan mode)

| Field | Value |
|---|---|
| Index/Key | passive helmet tick |
| Effects | Speed/NightVision always; in Rinnesharingan: purge harmful, Flight, Saturation, Strength 10, +heal, swap Rinnegan body+legs, give Six Paths Senjutsu + Black Receiver, (+380 max-health attribute from helmet `getAttributeModifiers`) |
| Procedures | `ProcedureRinneganHelmetTickEvent` (id 39); attribute in `ItemRinnegan.java:165-172` |
| Sounds | `ui.toast.challenge_complete` on first Rinnesharingan activation |
| 1.12.2 file:line | `procedure/ProcedureRinneganHelmetTickEvent.java` (whole), `item/ItemRinnegan.java:112-190` |

**1.20.1 status: DONE.** `RinneganHelmetItem.applyLegacyTick` + `applyRinnesharinganBranch` / `applyBaseRinneganBranch` mirror the original closely (Flight effect from `ModEffects.FLIGHT`, Strength=DAMAGE_BOOST, body/legs swap, Six Paths Senjutsu, Black Receiver, NightVision, fallDistance reset, Ten-Tails-jinchuriki + XP≥180 Rinnesharingan unlock + advancement). +380 max-health on Rinnesharingan: present on the **Byakugan** port; CONFIRM the same modifier exists on the Rinnegan/Tenseigan helmet port (not seen in `RinneganHelmetItem.getAttributeModifiers` — original had it at `ItemRinnegan.java:167-170`). Limbo clone enable ported.

---

# TENSEIGAN (CHAKRA MODE) JUTSU — `ItemTenseiganChakraMode`

Three ranged jutsu selected by the power-up key while holding the Chakra-Mode rod (only while Tenseigan is worn). Base = `ItemJutsu.Base`, type `TENSEIGAN`. All default cooldown 0. (`item/ItemTenseiganChakraMode.java:57-59`).

## Tenseigangun — Chakra Orbs (転生眼グン)

| Field | Value |
|---|---|
| Index | `CHAKRAORBS` = 0, key `'S'`, registry `tenseigangun`, entity id 339 |
| Chakra | 10 |
| Rank | 'S' |
| Cooldown | 0 |
| Entities | `EntityOrbs` (`EntityScalableProjectile.Base`, scale 0.5, expl size 5, 30 dmg) |
| Procedures | inner `EntityOrbs.Jutsu` createJutsu |
| Particles | none on projectile; custom render | Sounds | `narutomod:throwpunch` at tick 5 |
| Visual | `RenderCustom`: glowing white_orb.png billboard, color RGB(0.592,0.984,0.91), fade-in over 10t, double-quad halo when f>0.5; explosion + 3-radius AoE 30 dmg on impact; drains 1 chakra from living hit |
| 1.12.2 file:line | `item/ItemTenseiganChakraMode.java:185-322` |

**1.20.1 status: DONE.** `TenseiganChakraModeItem.CHAKRA_ORBS` + `activateChakraOrbs` + `TenseiganOrbEntity.spawnFrom`. Verify the orb render (white_orb billboard, halo, glow color) matches in the ported renderer.

## Tensei Baku Silver — Silver Wheel Rebirth Blast

| Field | Value |
|---|---|
| Index | `SILVERBLAST` = 1, key `'S'`, registry `tensei_baku_silver` |
| Chakra | 50 × power |
| Rank | 'S' | Cooldown | 0 |
| Power | base 10, powerUp delay 20 (`getPower(...,10,20)`) |
| Entities | `EntityTenseiBakuSilver.EC` (+ `.Jutsu`) |
| Procedures | `EntityTenseiBakuSilver.EC.Jutsu` |
| 1.12.2 file:line | `item/ItemTenseiganChakraMode.java:58,100-101` + `entity/EntityTenseiBakuSilver.java` |

**1.20.1 status: DONE.** `SILVER_BLAST` (`.withPower(10,20)`) + `activateSilverBlast` + `TenseiBakuSilverEntity.spawnFrom(player, power)`, max power 20. Charge via `startUsingItem`/`releaseUsing` (BOW anim) with charging particles + charging-chakra sound. Entity ported.

## Tensei Baku Gold — Golden Wheel Rebirth Blast

| Field | Value |
|---|---|
| Index | `GOLDBLAST` = 2, key `'S'`, registry `tensei_baku_gold` |
| Chakra | 50 × power |
| Rank | 'S' | Cooldown | 0 |
| Power | base 10, powerUp delay 5 (`getPower(...,10,5)`) |
| Entities | `EntityTenseiBakuGold.EC` (+ `.Jutsu`) |
| 1.12.2 file:line | `item/ItemTenseiganChakraMode.java:59,102-103` + `entity/EntityTenseiBakuGold.java` |

**1.20.1 status: DONE.** `GOLD_BLAST` (`.withPower(10,5)`) + `activateGoldBlast` + `TenseiBakuGoldEntity.spawnFrom(player, power)`, max power 50. Entity ported.

### Tenseigan Chakra Mode item behavior (armor swap / flight)
Original `RangedItem.onUpdate` (`ItemTenseiganChakraMode.java:115-178`): while held with Tenseigan worn → Flight effect each tick; swaps Tenseigan body+legs in, with one-time 1000× SMOKE 0x20b5fff5 burst + `narutomod:charging_chakra`; tracks chest/leg armor damage in item NBT; when armor exhausted → 2400t cooldown + breaks armor. Plus `ProcedureTenseiganBodyTickEvent` on body/legs (extinguish + 20× SMOKE + Biju-cloak-tier-2 effects, shrink if not holding rod).
**1.20.1 status: DONE.** Ported as `TenseiganChakraModeItem.tickSelected` / `ensureArmorSlot` / `breakChakraArmorIfExhausted` / `removeChakraArmorAndItem`, Flight via `ModEffects.FLIGHT`, charging-chakra sound, 2400t cooldown, armor-damage NBT (`ChestArmorDamage`/`LegArmorDamage`). The body/legs `ProcedureTenseiganBodyTickEvent` SMOKE-emitter + biju-cloak-tier-2 application: VERIFY a port exists for the body/legs tick (extinguish + smoke + tier-2 cloak effects) — not seen in this item; may live on the body/legs items. Mark PARTIAL until that body/legs tick is confirmed.

---

# BYAKUGAN JUTSU — `ItemByakugan`

Helmet `byakuganhelmet`. Evolves to Tenseigan over time when `tenseiganEvolvedTime` NBT counts down (owner only), giving back the old Byakugan and a new Tenseigan; grants `narutomod:tenseigan_achieved`. ByakuganCount NBT starts at 1, needs ≥5 for Tenseigan Chakra Mode.

## Byakugan — Activate / X-Ray View

| Field | Value |
|---|---|
| Index/Key | **Key 1**, NOT sneaking |
| Chakra | `getByakuganChakraUsage` = 10 per half-second (×2 non-owner); activation needs ≥ usage×2 |
| Level/XP/Rank | none to activate |
| Cooldown | none (toggle) |
| Entities | none (HUD overlay) |
| Procedures | `ProcedureSpecialJutsu1OnKeyPressed:107-126` → `ProcedureByakuganActivate` (id 107); drain in `ProcedureByakuganHelmetTickEvent` (id 99); FOV down via `ProcedurePowerIncreaseOnKeyPressed:62-67` |
| Particles | none | Sounds | `narutomod:byakugan` on activate |
| Visual | `OverlayByakuganView` see-through/zoom HUD; FOV 110 default, decreasable; Night Vision always; 10 chakra/10t while active |
| 1.12.2 file:line | `procedure/ProcedureByakuganActivate.java`, `procedure/ProcedureByakuganHelmetTickEvent.java` |

**1.20.1 status: DONE.** `ByakuganHandler.handleActivationKey` (sound, FOV, chakra gate), `tick` (NightVision + 10/10t drain), `handlePowerIncreaseKey` (FOV down), synced via `ByakuganViewSyncMessage` + `network/ByakuganViewSyncMessage`. `projectedCameraDistance`/`targetRenderDistance` ported. Verify client `OverlayByakuganView` render exists.

## Hakke Kūshō (八卦空掌 — Eight Trigrams Vacuum Palm / air punch)

| Field | Value |
|---|---|
| Index/Key | **Key 1, SNEAKING** |
| Chakra | `getKushoChakraUsage` = 0.5 × pressDuration |
| Level/XP/Rank | XP ≥ 15 (owner); block-break needs XP ≥ 30 |
| Cooldown | none |
| Entities | spawns `EntityFallingBlock` for lifted blocks |
| Procedures | `ProcedureSpecialJutsu1OnKeyPressed:108-114` → `ProcedureHakkeKusho` (id 265) extends `ProcedureAirPunch` |
| Particles | (AirPunch base) | Sounds | `narutomod:HakkeKusho` on release |
| Visual | Ranged air-blast cone (range = dur/3+5, far radius = dur/20); damages entities `(strength*level/100+10)/sqrt(dist)`; breaks/launches blocks upward; swing-arm sync |
| 1.12.2 file:line | `procedure/ProcedureHakkeKusho.java` (+ base `ProcedureAirPunch`) |

**1.20.1 status: MISSING.** `ByakuganHandler` key 1 only handles activation; there is NO sneak branch and NO `ProcedureAirPunch`/`ProcedureHakkeKusho` port (none in `1.20.1/.../procedure`). `SekizoEntity` has an `applyAirPunch` but that is a different (Sekizo) technique, not Byakugan Hakke Kushō. NEEDS: port `ProcedureAirPunch` base + Hakke Kushō subclass (range/radius/damage/block-launch curves) + wire to Byakugan key1 while sneaking + `HakkeKusho` sound + XP gates 15/30.

## Hakke Rokujūyon Shō — Eight Trigrams 64 Palms

| Field | Value |
|---|---|
| Index/Key | **Key 2** (non-Rinnesharingan Byakugan) |
| Chakra | `getRokujuyonshoChakraUsage` = 100 (owner only) |
| Level/XP/Rank | XP ≥ 20 (owner) |
| Cooldown | `cooldownModifier * 1200t` (`HakkeRokujuuyonshouCD` on helmet) |
| Entities | `EntityEightTrigrams.EntityCustom` |
| Procedures | `ProcedureSpecialJutsu2OnKeyPressed:183-189` → `ProcedureEightTrigrams64Palms` (id 261) |
| Particles | none | Sounds | `narutomod:HakkeRokujuuyonShou` |
| Visual | Spawns the 64-Palms strike entity; grants Haste 3 (240t) to caster |
| 1.12.2 file:line | `procedure/ProcedureEightTrigrams64Palms.java` |

**1.20.1 status: DONE.** `ByakuganHandler.triggerEightTrigrams64Palms` (`:216-246`): XP≥20 owner gate, 1200t×cooldownModifier cooldown on helmet NBT, 100 chakra, `HakkeRokujuuyonShou` sound, Haste(DIG_SPEED) 3/240t, `EightTrigramsEntity.spawnFrom`, refund on fail. Entity ported.

## Hakkeshō Kaiten — Heavenly Spin (rotating shield)

| Field | Value |
|---|---|
| Index/Key | **Key 3** (non-Rinnesharingan Byakugan) |
| Chakra | `getKaitenChakraUsage` = 4/tick (owner only); upkeep drains while riding |
| Level/XP/Rank | XP ≥ 30 (owner) |
| Cooldown | `cooldownModifier * ticksExisted * 5` (`HakkeshoKaitenCD`); also costs food |
| Entities | `EntityHakkeshoKeiten.EntityCustom` (player rides it; scale = level/30 clamp 1..10) |
| Procedures | `ProcedureSpecialJutsu3OnKeyPressed:81-88` → `ProcedureHakkeshoKaiten` (id 263) |
| Particles | (entity render) | Sounds | `narutomod:HakkeshoKaiten` on start |
| Visual | `ModelKaiten`: 4 nested 16³ boxes, electric_armor.png, 30°/tick spin, translucent dome scaled by maturity×scale×2.5; knocks back + damages colliders (level/2+10), breaks blocks at maturity≥0.9, purges harmful effects |
| 1.12.2 file:line | `procedure/ProcedureHakkeshoKaiten.java` (non-Rinnesharingan branch) + `entity/EntityHakkeshoKeiten.java` |

**1.20.1 status: DONE.** `ByakuganHandler.handleHakkeshoKaiten` / `startHakkeshoKaiten` / `releaseHakkeshoKaiten` (`:248-288`): XP≥30 owner gate, cooldown on helmet NBT, 4/tick upkeep via `HakkeshoKeitenEntity`, food cost on release, `HakkeshoKaiten` sound (verify). `HakkeshoKeitenEntity` ported. Verify the rotating translucent dome render (`ModelKaiten`) is reproduced.

## Rinnesharingan Shockwave (Byaku-Rinnesharingan Key 3)

| Field | Value |
|---|---|
| Index/Key | **Key 3**, when Byakugan helmet has `RINNESHARINGAN_ACTIVATED` |
| Chakra | none (charge-time based) |
| Level/XP/Rank | requires Rinnesharingan |
| Cooldown | none |
| Entities | none |
| Procedures | `ProcedureSpecialJutsu3OnKeyPressed` (Byakugan branch) → `ProcedureHakkeshoKaiten` Rinnesharingan branch (`:49-71`) |
| Particles | 1000× SMOKE 0x10FFFFFF | Sounds | `narutomod:dojutsu_activate` |
| Visual | Charge `press_time` up to 200 (shows Power), on release radial knockback radius = press/2, purge harmful effects, extinguish |
| 1.12.2 file:line | `procedure/ProcedureHakkeshoKaiten.java:49-71` |

**1.20.1 status: DONE.** `ByakuganHandler.handleRinnesharinganShockwave` (`:290-318`): charge `press_time`≤200, release → SMOKE particles, `dojutsu_activate` sound, `knockbackAround(radius=press/2)`, purge + clearFire. Routed from key3 when `isRinnesharinganActivated(head)`.

## Yomotsu Hirasaka (黄泉比良坂 — paired portals)

| Field | Value |
|---|---|
| Index/Key | **Key 2**, when Byakugan helmet has `RINNESHARINGAN_ACTIVATED` |
| Chakra | none shown |
| Level/XP/Rank | requires Rinnesharingan |
| Cooldown | none |
| Entities | none (places `BlockPortalBlock` pair + TileEntities) |
| Procedures | `ProcedureSpecialJutsu2OnKeyPressed:176-182` → `ProcedureYomotsuHirasaka` (id 279) |
| Particles | none | Sounds | `block.portal.travel` |
| Visual | Creates a 2-tall portal in front of caster and a paired 2-tall portal at look target (entity-facing aware), links the TileEntities for teleport |
| 1.12.2 file:line | `procedure/ProcedureYomotsuHirasaka.java` |

**1.20.1 status: DONE (delegated).** `ByakuganHandler.handleEightTrigramsKey` routes to `AdvancedNatureJutsuItem.placeYomotsuHirasakaPortals(level, player, true)` when `isRinnesharinganActivated(head)`. Verify portal block + linked TE teleport behavior is faithful in `AdvancedNatureJutsuItem`.

## Byakugan → Tenseigan Evolution (passive)

| Field | Value |
|---|---|
| Trigger | helmet tick: `tenseiganEvolvedTime` NBT counts down (−20/20t); at ≤0 (owner, MP) → swap to Tenseigan, return old Byakugan, advancement `narutomod:tenseigan_achieved` |
| 1.12.2 file:line | `item/ItemByakugan.java:104-126`, `:128-132` (ByakuganCount=1 on setOwner) |

**1.20.1 status: DONE.** `ByakuganHelmetItem.tickTenseiganEvolution` / `evolveToTenseigan` / `finishTenseiganEvolution`, ByakuganCount default, stripEvolutionTags, advancement. Faithful.

## Byakugan passive (NightVision + chakra drain)
`ProcedureByakuganHelmetTickEvent` (id 99): NightVision always; while `byakugan_activated`, clamp FOV and drain 10/10t. **1.20.1 status: DONE** in `ByakuganHandler.tick`.

---

# DOJUTSU BASE & BYAKU-RINNESHARINGAN ARMOR

## ItemDojutsu.Base (shared dojutsu helmet base)
Foreign-wearer **Blindness 1200t** penalty, ownership stamping, `MostRecentWornDojutsuTime`, snug helmet model with headwear/highlight/forehead planes.
**1.20.1 status: PARTIAL.** No standalone Base port; ownership + `MOST_RECENT_WORN_DOJUTSU_TIME` re-implemented per-handler. The **foreign-wearer Blindness penalty was NOT found** in the Rinnegan/Byakugan port ticks — likely MISSING; restore it (apply Blindness 1200t once per new foreign owner uuid for non-owner non-creative wearers). Snug helmet model + highlight/forehead overlay render must be confirmed in client models.

## ItemByakuRinnesharingan (cosmetic byakurinnesharingan helmet)
Separate cosmetic helmet `byakurinnesharinganhelmet` (texture `byakurinnesharingan_helmet.png`), `ModelHelmetSnug` 8³ box. No jutsu of its own — purely the rendered head for the Byaku-Rinnesharingan state. Body/legs/boots are ObjectHolders but only the helmet is registered in `initElements`.
**1.20.1 status: PARTIAL/DONE.** `BYAKURINNESHARINGANHELMET` registered in `ModItems`; `ByakuganHelmetItem(rinnesharinganVariant=true)` provides the texture + implicit `RINNESHARINGAN_ACTIVATED` tag + 380 max-health. Cosmetic-only role appears covered; confirm its model/texture render.

---

# Cross-cutting GAPS (highest priority restore list)

1. **Shinra Tensei (Rinnegan/Tenseigan Key 1)** — MISSING entirely. No handler branch, no procedure, sound unused.
2. **Ban Shō Ten'in (Rinnegan/Tenseigan Key 3)** — MISSING entirely. No handler branch, no `ProcedurePullAndHold`, sound unused.
3. **Hakke Kūshō (Byakugan Key 1 + sneak)** — MISSING. No `ProcedureAirPunch` base, no sneak branch on Byakugan key1.
4. **Dojutsu foreign-wearer Blindness penalty** — likely MISSING (centralized `ItemDojutsu.Base` behavior not reproduced).
5. **Verify-only deltas:** Rinnegan/Tenseigan +380 max-health Rinnesharingan modifier on the helmet; Tenseigan body/legs `ProcedureTenseiganBodyTickEvent` (smoke + biju-cloak-tier-2); meteor `tengaishinsei` sound playback in port; Animal Path explosion particle fidelity (EXPLOSION_HUGE×N vs single EMITTER); Naraka `KoH_id` periodic validity cleanup; all custom renders (Orb halo, Kaiten dome, ChibakuTensei ball, snug helmet overlays, Byakugan overlay).

---

## Unresolved references
- `ProcedureAirPunch` (1.12.2 base for Hakke Kushō) was not opened in full here; its range/damage/block mechanics must be read from `1.12.2/.../procedure/ProcedureAirPunch.java` before porting Hakke Kushō.
- `ProcedurePullAndHold` (used by Ban Shō Ten'in) and `ProcedureGravityPower.dislodgeBlocks` must be read before porting Ban Shō Ten'in.
- `SpecialEvent.setSphericalExplosionEvent` (Shinra Tensei terrain explosion) must be read before porting Shinra Tensei.
- Entity render/model fidelity for ported entities was inferred from the 1.12.2 originals; confirm each ported `*Entity` renderer matches.
