# Jutsu Backlog — Sharingan / Mangekyo (Eternal / Obito) / Uchiha Domain

Audit of every technique reachable from the Sharingan helmet, the three Mangekyo helmets
(Sasuke / Obito / Eternal) and the Uchiha armor set. Compares the 1.12.2 source of truth to
the in-progress 1.20.1 port so a developer can restore EVERY mechanic and visual.

- ORIGINAL base: `D:\mcmodding\naruto\1.12.2\src\main\java\net\narutomod`
- PORT base: `D:\mcmodding\naruto\1.20.1\src\main\java\net\narutomod`

---

## Framework / Architecture notes (read this first)

The "tome" item classes (`ItemSharingan`, `ItemMangekyoSharingan`, `ItemMangekyoSharinganEternal`,
`ItemMangekyoSharinganObito`, `ItemUchiha`) contain very little jutsu logic themselves. They are
`ItemArmor` helmets/armor whose `onArmorTick` sets passive effects and durability, and whose tooltip
advertises which keybind triggers which jutsu. The actual jutsu live in **procedures dispatched by
keybinds** and in **entity classes**.

Dispatch chain (1.12.2):
- Keybinds `KeyBindingSpecialJutsu1/2/3` (default **R / F / C**) send a server packet that runs
  `ProcedureSpecialJutsu1OnKeyPressed` / `...2...` / `...3...`.
- Those dispatchers branch on the equipped HEAD item:
  - **Key1 (R)** + Mangekyo Sasuke / Eternal → `ProcedureAmaterasu`.
  - **Key1 (R)** + Obito → `ProcedureKamuiJikukanIdo` (or `ProcedureGrabEntity` inside Kamui dim).
  - **Key2 (F)** + any Mangekyo (Sasuke / Obito / Eternal) → `ProcedureSusanoo` (summon/dismiss).
  - **Key3 (C)** + Eternal → `ProcedureKamuiJikukanIdo`.
- Passive logic runs every tick from `ItemSharingan.Base.onArmorTick` →
  `ProcedureSharinganHelmetTickEvent` (Mangekyo awakening roll, glow invisibles, blinded-eye penalty,
  Susanoo skeleton body tick). `ItemDojutsu.Base.onArmorTick` adds the foreign-owner blindness penalty.
- Entity-data boolean flags coordinate state across ticks: `amaterasu_active`, `susanoo_activated`,
  `kamui_teleport`, `kamui_intangible`; plus `amaterasu_cd`, `susanoo_cd`, `susanoo_ticks`, `kamui_timer`.

Shared base classes (1.12.2):
- `ItemDojutsu.Base` (`item/ItemDojutsu.java:35`) — abstract HEAD `ItemArmor`; foreign-owner
  blindness penalty; owner NBT; `ModelHelmetSnug` armor model with sharingan highlight + forehead overlay.
- `ItemSharingan.Base` (`item/ItemSharingan.java:50`) — extends `ItemDojutsu.Base`; adds random eye
  `color` NBT, `sharingan_blinded` flag, durability damage gate (`canDamage`), `PlayerHook`
  (50% melee dodge + slow/fatigue on attacker), `SusanooStats` NBT helper.
- `EntitySusanooBase` (`entity/EntitySusanooBase.java:36`) — rideable Susanoo mob; flame particle aura,
  chakra-drain, BXP thresholds, Amaterasu immunity, rider-steered combat.

Dispatch chain (1.20.1 PORT):
- `client/ClientSpecialJutsuKeys.java` registers **R/F/C** (specialjutsu1/2/3) AND a NEW
  **Up-arrow "powerincrease"** key.
- `network/SpecialJutsuKeyMessage.handle` (key 1/2/3) routes ONLY to, in order:
  `ObitoKamuiHandler` → `ByakuganHandler` → `RinneganSpecialJutsuHandler` → `BijuManager`.
  **There is no Amaterasu branch and no Susanoo branch here.**
- `item/PowerIncreaseKeyHandler.handle` (Up-arrow) routes to held `JutsuItem`, then
  `ByakuganHandler` → `SusanooPowerIncreaseHandler` → `RinneganSpecialJutsuHandler` → biju cloak.
  **Susanoo summon/upgrade was MOVED from key2 (F) to the new power-increase key.**
- Passive legacy logic: `procedure/ProcedurePlayerLegacyDojutsuPostTick.java` (Mangekyo/dojutsu
  awakening roll, blinded blindness, Susanoo auto-cleanup). Runs every 20 ticks.

### KEY PORT GAP (highest risk)
**Amaterasu activation is not wired to any key in the port.** `SpecialJutsuKeyMessage.handle` has no
Mangekyo-Sasuke / Eternal branch, and `NetworkHandler` registers no Amaterasu message. The flame DOT
(`effect/AmaterasuFlameEffect`), the black-flame block (`ModBlocks.AMATERASUBLOCK`) and the
Kagutsuchi-sword-applied flame exist, but the player-facing **"press R to ignite Amaterasu on what you
look at / place black flame / extinguish on sneak"** jutsu is MISSING. See the Amaterasu sections below.

---

## Jutsu 1 — Sharingan (passive dojutsu, base helmet)

| Field | Value |
|---|---|
| Display name | Sharingan helmet (`sharinganhelmet`) |
| Index/registry | `narutomod:sharinganhelmet`; ModElement id 56 |
| Chakra | none (passive) |
| Level/XP/rank | Equippable from awakening; foreign-owner penalty unless `experienceLevel >= 10` AND `BATTLEXP >= 300` (`ProcedureSharinganHelmetTickEvent.java:94`) |
| Cooldown | n/a |
| Entities | none |
| Procedures | `ProcedureSharinganHelmetTickEvent` (`procedure/...:46`); `ProcedureUchihaBodyTickEvent` (set-bonus) |
| Particles | none |
| Sounds | `ui.toast.challenge_complete` on Mangekyo awakening |
| Visual | `textures/sharinganhelmet.png`; `ModelHelmetSnug` snug helmet with glowing highlight + forehead overlay (`ItemDojutsu.java:98-181`); eye `color` randomized per-owner (`ItemSharingan.java:102`) |
| 1.12.2 file:line | `item/ItemSharingan.java:42-269` |

Passive effects bundled into the Sharingan helmet:
- **Dodge + disorient** (`ItemSharingan.PlayerHook.onAttacked`, `:149-166`): 50% chance to cancel a
  living attacker's hit and dash sideways; always applies SLOWNESS II + MINING_FATIGUE II (300t) to attacker.
- **Reveal invisibles** (`ProcedureSharinganHelmetTickEvent.java:125-141`): every 80 ticks (client),
  set GLOWING (75t) on invisible entities within 20-block AABB.
- **Foreign-eye penalty** (`ItemDojutsu.Base.onArmorTick:43-49`): BLINDNESS 1200t if not owner.
- **Blinded eye** (`ProcedureSharinganHelmetTickEvent.java:83-90`): when durability ≤3 from max,
  set `sharingan_blinded`; then permanent BLINDNESS + disables all eye jutsu.
- **Mangekyo awakening roll** (`:142-181`): SHARINGAN holder with `expLvl>30 && BATTLEXP>600` AND a
  recent nearby player death (`PlayerTracker.Deaths.hasRecentNearby`, 40 blocks/6000t) → 50/50 upgrade
  to Mangekyo Sasuke or Obito helmet, grants `mangekyosharinganopened` advancement, plays challenge sound.
- **Set bonus** (`ProcedureUchihaBodyTickEvent`): Uchiha legs+boots + any Sharingan head → STRENGTH II + HASTE III.

**1.20.1 status: PARTIAL.**
- Helmet item registered (`ModItems.SHARINGANHELMET`) but there is **no dedicated SharinganHelmetItem
  class**; passive jutsu logic is split into `ProcedurePlayerLegacyDojutsuPostTick`.
- DONE: foreign-eye BLINDNESS (`EternalMangekyoHelmetItem`/`ObitoMangekyoHelmetItem` carry it for
  Mangekyos; legacy post-tick adds it generically), recent-death penalty, awakening roll
  (`grantCatchupOrRandomAwakeningIfNeeded` / `randomAwakeningStack`), set bonus
  (`UchihaArmorItem.inventoryTick` → DAMAGE_BOOST II + DIG_SPEED III).
- MISSING/UNCONFIRMED: the `PlayerHook` 50% melee dodge + SLOWNESS/MINING_FATIGUE on attacker; the
  "reveal invisibles via GLOWING in a 20-block radius" sweep; the random per-owner eye `color` NBT used
  to tint Susanoo flame; the exact `sharingan_blinded` durability threshold (≤3 from max) — port uses a
  generic `isBlinded` tag check but the auto-set-on-low-durability path needs confirmation.
- Verify the snug helmet highlight/forehead overlay render is reproduced (no `ModelHelmetSnug` analog found).

---

## Jutsu 2 — Mangekyo Sharingan (Sasuke) helmet (passive carrier)

| Field | Value |
|---|---|
| Display name | Mangekyo Sharingan (Sasuke) — red name (`mangekyosharinganhelmet`) |
| Index/registry | `narutomod:mangekyosharinganhelmet`; ModElement id 69 |
| Chakra | Amaterasu base usage `AMATERASU_CHAKRA_USAGE = 100` (x2 if not owner) (`ItemMangekyoSharingan.java:35,41`) |
| Level/XP/rank | n/a passive |
| Cooldown | drains durability while `amaterasu_active` or `susanoo_activated` (1/tick, halved for owner) (`:57-62`) |
| Entities | enables Susanoo (Jutsu 7) and Amaterasu (Jutsu 5) |
| Procedures | `onArmorTick` adds SPEED III pulse; Amaterasu/Susanoo via key dispatch |
| Particles/Sounds | see Amaterasu/Susanoo |
| Visual | `textures/mangekyosharinganhelmet_sasuke.png` |
| 1.12.2 file:line | `item/ItemMangekyoSharingan.java:32-88` |

Tooltip advertises specialjutsu1 = Amaterasu. Passive: SPEED III pulse each tick; durability damage
while a Mangekyo jutsu is active. Key host for Amaterasu (key1) and Susanoo (key2).

**1.20.1 status: PARTIAL.**
- Item registered (`ModItems.MANGEKYOSHARINGANHELMET`) but **no dedicated item class** — there is no
  `MangekyoSharinganHelmetItem.java`. The Obito/Eternal got bespoke item classes; the Sasuke Mangekyo
  did not, so its passive SPEED III pulse and active-jutsu durability drain are unconfirmed/likely MISSING.
- Amaterasu (its headline jutsu) is MISSING (see Jutsu 5).
- Susanoo summon works but via the Up-arrow power-increase key, not key2/F.

---

## Jutsu 3 — Mangekyo Sharingan (Obito) helmet (passive carrier)

| Field | Value |
|---|---|
| Display name | Mangekyo Sharingan (Obito) — red name (`mangekyosharinganobitohelmet`) |
| Index/registry | `narutomod:mangekyosharinganobitohelmet`; ModElement id 118 |
| Chakra | Kamui intangible `1/tick`, teleport `8/tick` (x2 if not owner) (`ItemMangekyoSharinganObito.java:35-36`) |
| Level/XP/rank | n/a passive |
| Cooldown | durability drain while `kamui_teleport` or `susanoo_activated` (`:69-74`) |
| Entities | enables Susanoo + Kamui |
| Procedures | `onArmorTick`: allowFlying in Kamui dim/creative; consume chakra for teleport/intangible; set `InvulnerableTime=2` while intangible (`:78-81`) |
| Visual | `textures/mangekyosharinganhelmet_obito.png` |
| 1.12.2 file:line | `item/ItemMangekyoSharinganObito.java:32-108` |

Tooltip advertises specialjutsu1 = Kamui, specialjutsu2 = Susanoo. Host for Kamui (key1) + Susanoo (key2).

**1.20.1 status: DONE (helmet) / see Kamui + Susanoo.**
- `item/ObitoMangekyoHelmetItem.java` is a faithful bespoke port: flight in Kamui dim/creative
  (`updateKamuiFlight`), durability drain while teleporting/susanoo (`damageForActiveKamuiOrSusanoo`),
  foreign-owner penalty, blinded check, chakra-usage scaling. Invuln-while-intangible handled in
  `ObitoKamuiHandler.setIntangible` (`invulnerableTime`) + `onLivingAttack` cancel.

---

## Jutsu 4 — Mangekyo Sharingan Eternal helmet (passive carrier, no durability)

| Field | Value |
|---|---|
| Display name | Eternal Mangekyo Sharingan — red name (`mangekyosharinganeternalhelmet`) |
| Index/registry | `narutomod:mangekyosharinganeternalhelmet`; ModElement id 204 |
| Chakra | shares Amaterasu/Kamui usages (owner-rate) |
| Level/XP/rank | n/a passive |
| Cooldown | indestructible: `getMaxDamage()=0`, `isDamageable()=false` (`ItemMangekyoSharinganEternal.java:70-77`) |
| Entities | enables Amaterasu + Susanoo + Kamui (all three) |
| Procedures | `onArmorTick`: SPEED III; allowFlying in Kamui dim; consume teleport/intangible chakra (`:45-61`) |
| Visual | `textures/mangekyosharinganhelmet_eternal.png` |
| 1.12.2 file:line | `item/ItemMangekyoSharinganEternal.java:32-98` |

Tooltip advertises specialjutsu1 = Amaterasu, specialjutsu2 = Susanoo, specialjutsu3 = Kamui. The only
helmet that grants all three Mangekyo jutsu. Indestructible (no durability).

**1.20.1 status: PARTIAL.**
- `item/EternalMangekyoHelmetItem.java` is a faithful passive port: SPEED III, indestructible
  (durability 0), Kamui flight (`ObitoMangekyoHelmetItem.updateKamuiFlight`), foreign-owner penalty.
- DONE: Kamui (key3 → `ObitoKamuiHandler` handles `key==3 && hasEternalHelmet`), Susanoo (power-increase key).
- MISSING: Amaterasu (key1) — no handler, same root gap as Jutsu 5.

---

## Jutsu 5 — Amaterasu (天照, black inextinguishable flame) — ACTIVATION

| Field | Value |
|---|---|
| Display name | Amaterasu (tooltip `tooltip.mangekyo.amaterasu.jutsu1`) |
| Trigger | specialjutsu1 (R), Mangekyo Sasuke or Eternal helmet |
| Chakra | `ItemMangekyoSharingan.getAmaterasuChakraUsage` = 100 (x2 non-owner); requires `chakra >= usage*1.25`; consumes `usage` on activate + `usage*0.25` per repeat tick (`ProcedureAmaterasu.java:73-91`) |
| Level/XP/rank | flame amplifier = `experienceLevel / 30` (`:94`) |
| Cooldown | `amaterasu_cd = world_tick + cd_modifier*300`; extends by `cd_modifier*10` while held (`:83-89`); on release: WEAKNESS III + NAUSEA for `(cd - world_tick)*0.5` ticks (`:135-141`) |
| Entities | none (uses block + potion) |
| Procedures | `ProcedureAmaterasu` (`procedure/ProcedureAmaterasu.java:29`), calls `ProcedureAmaterasuPlaceBlock`, `ProcedureAmaterasuExtinguishEntities` |
| Particles | flame particles from the burning potion (`Particles.Types.FLAME`, color `0xA0000000`) |
| Sounds | `narutomod:amaterasu2` on ignite (when not sneaking) (`:80-81`) |
| Visual | black-flame `BlockAmaterasuBlock` placed on looked-at block face; `PotionAmaterasuFlame` DOT on looked-at entity; raytrace 30 blocks |
| 1.12.2 file:line | `procedure/ProcedureAmaterasu.java:29-145` |

Behavior detail:
- **Press (held)**: raytrace 30 blocks (`ProcedureUtils.objectEntityLookingAt`). If hit an entity →
  apply `PotionAmaterasuFlame` amp `expLvl/30`, duration 10000t. If hit a block → place
  `BlockAmaterasuBlock` on the struck face (`ProcedureAmaterasuPlaceBlock.java:38-40`).
- **Release while sneaking**: `ProcedureAmaterasuExtinguishEntities` — clears FIRE + Amaterasu blocks in
  a 15-block cube and removes the flame potion / extinguishes all entities there
  (`ProcedureAmaterasuExtinguishEntities.java:23-75`), raytraced up to 50 blocks.
- **Release not sneaking** while active → WEAKNESS III + NAUSEA self-penalty.
- Blocked entirely if `sharingan_blinded`.

**1.20.1 status: MISSING (activation jutsu).**
- The flame DOT EFFECT is DONE: `effect/AmaterasuFlameEffect.java` (hurts via `ModDamageTypes.ninjutsuFire`,
  spawns colored flame particles `NarutoParticleKind.FLAME_COLORED 0xA0000000`, and self-extinguishes when
  the victim wears a Mangekyo Sasuke/Eternal helmet — faithful to
  `ProcedureAmaterasuFlameOnPotionActiveTick.java`).
- The black-flame BLOCK is DONE: `ModBlocks.AMATERASUBLOCK` applies the flame effect on contact.
- Kagutsuchi sword applies the flame (`item/KagutsuchiSwordItem.java:95`) — separate Susanoo-Winged path.
- **MISSING**: the player-pressing-R ignition jutsu itself. No `ProcedureAmaterasu` analog;
  `SpecialJutsuKeyMessage.handle` has no Mangekyo-Sasuke/Eternal branch; `NetworkHandler` registers no
  Amaterasu message. Therefore: no raytrace-ignite-on-target, no place-black-flame-on-block, no
  `amaterasu_active`/`amaterasu_cd` state, no `narutomod:amaterasu2` ignite sound, no held-to-extend
  cooldown, no WEAKNESS/NAUSEA self-penalty.
- **MISSING**: sneak-extinguish (`ProcedureAmaterasuExtinguishEntities`) — no port equivalent. Only the
  per-victim helmet auto-protect removes flames.
- To restore: add an `AmaterasuHandler.handleSpecialJutsuKey(player, 1, pressed)` wired into
  `SpecialJutsuKeyMessage.handle` (and key3 path for Eternal already exists for Kamui — note Eternal's
  Amaterasu is key1). Needs: 30-block raytrace, apply `ModEffects.AMATERASUFLAME` or place
  `ModBlocks.AMATERASUBLOCK`, `amaterasu_active`/`amaterasu_cd` NBT, sound `ModSounds.SOUND_AMATERASU2`
  (verify it exists), and the sneak-release extinguish sweep.

---

## Jutsu 5b — Amaterasu Flame (potion/effect DOT) — supporting effect

| Field | Value |
|---|---|
| Display name | `effect.amaterasuflame` |
| Registry | `narutomod:amaterasuflame` (ModElement id 175) |
| Behavior | instant, every-tick: `attackEntityFrom(AMATERASU, amplifier+1)`; spawns colored flame particles; auto-removed if victim wears Mangekyo Sasuke/Eternal helmet (then extinguish) |
| 1.12.2 file:line | `potion/PotionAmaterasuFlame.java:22-93`; `procedure/ProcedureAmaterasuFlameOnPotionActiveTick.java:22-51` |

**1.20.1 status: DONE.** `effect/AmaterasuFlameEffect.java` + `ModEffects.AMATERASUFLAME`. Damage type
`ModDamageTypes.ninjutsuFire`; particle `FLAME_COLORED` color `0xA0000000` scale 20; Mangekyo protection.
Custom HUD/inventory icon (`textures/mob_effect/amaterasuflame.png`) should be verified.

---

## Jutsu 6 — Amaterasu: Kagutsuchi black-flame block — supporting

| Field | Value |
|---|---|
| Display name | Amaterasu Block (`BlockAmaterasuBlock`) |
| Registry | `narutomod:amaterasublock`; material `AMATERASU` |
| Behavior | placed by Amaterasu on blocks; sets flame effect on entities inside; removable by extinguish sweep |
| 1.12.2 file:line | `procedure/ProcedureAmaterasuPlaceBlock.java`; `block/BlockAmaterasuBlock.java` (referenced) |

**1.20.1 status: DONE (block) / MISSING (placement trigger).** `ModBlocks.AMATERASUBLOCK` exists and
applies `AMATERASUFLAME` on contact (`ModBlocks.java:162-174`). But nothing places it — the placement
came from `ProcedureAmaterasu` which is unported (see Jutsu 5).

---

## Jutsu 7 — Susanoo (須佐能乎, ribcage → skeleton → clothed → winged avatar)

| Field | Value |
|---|---|
| Display name | Susanoo (tooltip `entity.susanooclothed.name`) |
| Trigger | specialjutsu2 (F), any Mangekyo helmet (Sasuke/Obito/Eternal) — toggle summon/dismiss |
| Chakra | summon/upgrade `BASE_CHAKRA_USAGE = 500` each (`ProcedureSusanoo.java:38`); per-second drain on the entity scales by stage (skeleton 50, clothed 50-90, winged 70-120) (`EntitySusanooBase.java:43`, subclasses) |
| Level/XP/rank | Battle-XP gates: L1 skeleton 2000, L2 clothed-half 6000, L3 clothed-full 12000, L4 winged 24000 (`EntitySusanooBase.java:39-42`) |
| Cooldown | on dismiss: WEAKNESS III + NAUSEA II for `susanoo_ticks*0.25*cd_modifier` ticks (unless Eternal/creative/Rinnegan) + FeatherFalling 60t (`ProcedureSusanoo.java:89-103`) |
| Entities | `EntitySusanooSkeleton.EntityCustom` (id 32), `EntitySusanooClothed.EntityCustom` (id 36) + `EntityMagatama` (id 37), `EntitySusanooWinged.EntityCustom` (id 42) |
| Procedures | `ProcedureSusanoo` (summon/dismiss/upgrade + leave/dim hooks); `ProcedureSusanooSkeletonBodyTickEvent` (skeleton body tick from helmet) |
| Particles | per-entity flame aura particles (`Particles.Types.FLAME`, tinted by eye `FLAME_COLOR`) (`EntitySusanooBase.java:324-329`) |
| Sounds | `block.fire.ambient` every 30t while active |
| Visual | rideable colossus; skeleton ribcage (no legs), clothed humanoid (with/without legs) wielding Totsuka sword, winged 4-armed form wielding Kagutsuchi sword + Kamui shuriken; flame outline shader pass |
| 1.12.2 file:line | `procedure/ProcedureSusanoo.java:35-180`; `entity/EntitySusanooBase.java`; `entity/EntitySusanooSkeleton.java`; `entity/EntitySusanooClothed.java`; `entity/EntitySusanooWinged.java` |

Behavior detail:
- **Summon** (`ProcedureSusanoo.execute:53-105`): requires non-blinded eye, BXP ≥ L1, consume 500 chakra;
  spawn `EntitySusanooSkeleton`, set `susanoo_activated`, `susanoo_cd = world_tick + 2400`, player rides.
- **Dismiss** (same key): remove entity, clear flags, apply weakness/nausea cooldown unless Eternal.
- **Upgrade** (`upgrade:124-154`): when riding, re-press at higher BXP consumes 500 chakra and replaces with
  next stage (skeleton→clothed-half→clothed-full→winged).
- **Stage abilities**: skeleton's `collideWithEntity` sets Amaterasu flame on touched mobs if wearing
  Sasuke Mangekyo (`EntitySusanooSkeleton.java:74-80`); clothed wields `ItemTotsukaSword` + fires
  `EntityMagatama` (Yasaka Magatama) ranged bullet; winged gives the rider `ItemKagutsuchiSwordRanged`
  and/or `ItemKamuiShuriken` based on helmet.
- **Combat**: rider-steered; melee uses jutsu damage; flame immunity; Amaterasu-source immune.
- **Auto-cleanup** (`PlayerHook`): dismissed on dimension change / logout / disconnect.

**1.20.1 status: PARTIAL→mostly DONE (but key moved).**
- Entities ported: `entity/AbstractSusanooEntity.java`, `SusanooSkeletonEntity`, `SusanooClothedEntity`
  (+ `YasakaMagatamaEntity`), `SusanooWingedEntity` (+ `KagutsuchiFireballEntity`, `KamuiShurikenEntity`).
  Renderers/models all present (`client/renderer/Susanoo*Renderer`, `client/model/Susanoo*Model`).
- Summon/upgrade/dismiss DONE in `item/SusanooPowerIncreaseHandler.java`: same 500 chakra, identical BXP
  thresholds (L1-L4), `susanoo_activated`/`susanoo_ticks`/`summonedSusanooID` state, deactivate penalty
  (WEAKNESS III + CONFUSION II for `ticks*0.25`, FeatherFalling 60t, Eternal/Rinnegan/creative exempt),
  flame aura particles (`spawnFlames` → `FLAME_COLORED` tinted by `FLAME_COLOR`), `block.fire.ambient`
  not confirmed but smoke/flame visuals present, rider-steered combat (`attackLookTarget`).
- DONE: clothed Totsuka/Magatama (`createMagatama`/`launchMagatama` → `YasakaMagatamaEntity`), winged
  Kagutsuchi + Kamui shuriken items (`KamuiShurikenItem`, `KagutsuchiSwordItem`).
- **DEVIATION / RISK**: summon+upgrade moved from **key2 (F)** to the new **Up-arrow power-increase key**
  (`PowerIncreaseKeyHandler` → `SusanooPowerIncreaseHandler.handlePowerIncreaseKey`). In 1.12.2 it was F.
  `SpecialJutsuKeyMessage.handle` (F) has no Susanoo branch, so pressing F does nothing for Susanoo. If
  faithful keybinds matter, wire `SusanooPowerIncreaseHandler.activate` into the key2 path for Mangekyo
  heads, or document the rebinding.
- UNCONFIRMED: skeleton `collideWithEntity` Amaterasu-flame-on-touch for Sasuke Mangekyo wearers; the
  per-owner eye `color` tinting the flame (port hardcodes `0x20B83DBA` for Sasuke/Eternal, vs original
  reading the helmet's random `color`); the exact ranged-attack input trigger for Magatama.

---

## Jutsu 7b — Susanoo: Yasaka Magatama (八坂瓊勾玉, clothed ranged bullet)

| Field | Value |
|---|---|
| Display name | `yasaka_magatama` (entity id 37) |
| Behavior | spinning magatama projectile fired by clothed Susanoo |
| 1.12.2 file:line | `entity/EntitySusanooClothed.java:68-76` (EntityMagatama + RenderMagatama) |

**1.20.1 status: DONE.** `entity/YasakaMagatamaEntity.java`; spawned via
`SusanooClothedEntity.createMagatama/launchMagatama` (`:182-189`).

---

## Jutsu 7c — Susanoo: Totsuka Sword / Kagutsuchi Sword / Kamui Shuriken (held weapons)

| Field | Value |
|---|---|
| Totsuka | clothed Susanoo main-hand `ItemTotsukaSword` (sealing blade) |
| Kagutsuchi | winged Susanoo grants rider `ItemKagutsuchiSwordRanged` (fires black-flame fireballs) |
| Kamui Shuriken | winged Susanoo grants rider `ItemKamuiShuriken` (Obito/Eternal) |
| 1.12.2 file:line | `entity/EntitySusanooClothed.java:112`; `entity/EntitySusanooWinged.java:81-118` |

**1.20.1 status: DONE.** `item/KagutsuchiSwordItem.java` (applies `AMATERASUFLAME`, fires
`KagutsuchiFireballEntity`), `item/KamuiShurikenItem.java` (fires `KamuiShurikenEntity`, scales to
Susanoo). Totsuka sword ported (`item/*Totsuka*` referenced by clothed entity — verify item class exists).

---

## Jutsu 8 — Kamui (神威, Obito's space-time ninjutsu) — intangibility + teleport + grab

| Field | Value |
|---|---|
| Display name | Kamui (tooltip `tooltip.mangekyo.kamui.jutsu1`) |
| Trigger | Obito helmet: specialjutsu1 (R); Eternal helmet: specialjutsu3 (C) |
| Chakra | intangible `1/tick`, teleport `8/tick` (x2 non-owner) (`ItemMangekyoSharinganObito.java:35-36`) |
| Level/XP/rank | teleport transfer progress scales with `PlayerTracker.getNinjaLevel/300.1` (`ProcedureKamuiJikukanIdo.java:148`) |
| Cooldown | continuous chakra drain; no fixed CD |
| Entities | none (warps entities/blocks to `WorldKamuiDimension`) |
| Procedures | `ProcedureKamuiJikukanIdo` (`procedure/...:29`), `ProcedureKamuiTeleportEntity`, `ProcedureGrabEntity`/`ProcedurePullAndHold` (grab inside Kamui dim) |
| Particles | `Particles.Types.PORTAL_SPIRAL` at target (color `0x20000000`, scale 5, life 30) (`:133`) |
| Sounds | `narutomod:KamuiSFX` every 60 timer ticks while charging teleport (`:130-131`) |
| Visual | portal spiral; FOV narrowing overlay (`OverlayByakuganView.sendCustomData`, fov = 70 - ln(dist)*15); noclip intangible pass-through |
| 1.12.2 file:line | `procedure/ProcedureKamuiJikukanIdo.java:29-167` |

Behavior detail (three modes):
- **Intangible** (not sneaking, hold): purge harmful effects, set noClip, disable block edit, message
  `chattext.intangible true/false`, set `kamui_intangible`; Obito sets `InvulnerableTime=2`.
- **Teleport** (sneaking, hold to charge then release): raytrace 100 blocks, narrow FOV by distance,
  spawn portal spiral + KamuiSFX; on release, if charge sufficient warp target entity to/from
  `WorldKamuiDimension` (`ProcedureKamuiTeleportEntity.eEntity`); partial charge instead deals
  proportional max-health damage. Self-target warps the player.
- **Grab** (inside Kamui dim, not sneaking): `ProcedureGrabEntity` pulls/holds looked-at entity.

**1.20.1 status: DONE (faithful, well-ported).**
- `item/ObitoKamuiHandler.java` is a thorough port covering all three modes:
  - Intangible: `setIntangible` (`noPhysics`, `invulnerableTime`, syncs `kamui_intangible` +
    `NO_CLIP_FLAG`), purge harmful effects, `chattext.intangible` message, `onLivingAttack` cancel.
  - Teleport: `startTeleport`/`releaseTeleport`, 100-block raytrace, portal spiral particles
    (`NarutoParticleKind.PORTAL_SPIRAL` color `0x20000000` scale 30) + `ModSounds.SOUND_KAMUISFX` every
    60t, `KamuiDimension.toggleEntity` warp, partial-charge proportional damage
    (`transferProgress` uses ninja level / 300.1), block-drop transfer to/from Kamui dim.
  - Grab: `handleGrabKey`/`maintainGrab` (pulls entities, special-cases `ItemEntity`, `ExperienceOrb`,
    `EarthBlocksEntity`) — the 1.12.2 `ProcedureGrabEntity`/`ProcedurePullAndHold` analog.
  - FOV narrowing handled client-side (`client/KamuiFovEvents.java`).
  - Key routing: `key==1 && hasObitoHelmet` OR `key==3 && hasEternalHelmet` (`canHandleSpecialJutsuKey`).
- Minor verify: exact teleport chakra `warningDisplay` vs original silent fail; `chattext.intangible`
  localization; KamuiSFX sound id parity.

---

## Jutsu 9 — Uchiha armor set (chest / legs / boots) — set bonus carrier

| Field | Value |
|---|---|
| Display name | Uchiha body / legs / boots |
| Registry | `narutomod:uchihabody`, `uchihalegs`, `uchihaboots`; ModElement id 24 |
| Chakra | none |
| Level/XP/rank | n/a |
| Cooldown | n/a |
| Entities | none |
| Procedures | `ProcedureUchihaBodyTickEvent` (set bonus with Sharingan/Mangekyo head) |
| Particles/Sounds | none |
| Visual | custom `ModelArmorCustom` biped overlay; `narutomod:sasuke_*` textures (layer 1/2) |
| 1.12.2 file:line | `item/ItemUchiha.java:32-147`; `procedure/ProcedureUchihaBodyTickEvent.java:25-54` |

Set bonus: legs+boots equipped AND any Sharingan/Mangekyo-Sasuke/Mangekyo-Obito head →
STRENGTH II + HASTE III (server-side, refreshed each tick) (`ProcedureUchihaBodyTickEvent.java:46-51`).

**1.20.1 status: DONE.**
- `item/UchihaArmorItem.java` registers chest/legs/boots (`ModItems.UCHIHA*`), custom client model via
  `UchihaArmorClientExtensions`, textures `sasuke__layer_1/2.png`.
- Set bonus DONE: `inventoryTick` (chest piece) checks legs+boots+Sharingan/Mangekyo head
  (`hasLegacyUchihaSet`) → DAMAGE_BOOST II (STRENGTH II) + DIG_SPEED III (HASTE III). Faithful.

---

## Cross-cutting / supporting assets

| Asset | 1.12.2 | 1.20.1 status |
|---|---|---|
| `WorldKamuiDimension` (Kamui pocket dim) | `world/WorldKamuiDimension` | DONE → `world/KamuiDimension` |
| `OverlayByakuganView` FOV narrowing | `gui/overlay/OverlayByakuganView` | DONE → `client/KamuiFovEvents` + `ByakuganViewSyncMessage` |
| `PORTAL_SPIRAL` particle | `Particles.Types.PORTAL_SPIRAL` | DONE → `NarutoParticleKind.PORTAL_SPIRAL` |
| `FLAME` colored particle | `Particles.Types.FLAME` | DONE → `NarutoParticleKind.FLAME_COLORED` |
| `ProcedureUtils.AMATERASU` damage source | custom | DONE → `ModDamageTypes.ninjutsuFire` |
| `mangekyosharinganopened` advancement | grant on awakening | present (awakening roll grants it) |
| `narutomod:amaterasu2` ignite sound | played on Amaterasu ignite | UNCONFIRMED — verify `ModSounds` has it (only used by the missing activation jutsu) |
| `narutomod:KamuiSFX` sound | Kamui charge | DONE → `ModSounds.SOUND_KAMUISFX` |
| `ItachiEntity` (NPC user of these jutsu) | `entity/EntityItachi` | ported `entity/ItachiEntity` — verify it can still cast Amaterasu/Susanoo given the missing activation procedure |

---

## Summary of outstanding work (developer checklist)

1. **[HIGH] Port the Amaterasu activation jutsu** (`ProcedureAmaterasu`): key1 for Mangekyo
   Sasuke/Eternal. Add an `AmaterasuHandler.handleSpecialJutsuKey` wired into `SpecialJutsuKeyMessage`,
   plus a network message if needed. Implement raytrace-ignite (entity flame / block placement),
   `amaterasu_active`/`amaterasu_cd` NBT, ignite sound, held-cooldown extension, and the WEAKNESS/NAUSEA
   self-penalty on release. Without this, the headline Sasuke/Eternal jutsu is unusable by players.
2. **[HIGH] Port the Amaterasu sneak-extinguish sweep** (`ProcedureAmaterasuExtinguishEntities`):
   clear fire + Amaterasu blocks + flame potions in a 15-block cube on sneak-release.
3. **[MED] Restore Sharingan passive `PlayerHook`**: 50% melee dodge + SLOWNESS/MINING_FATIGUE on
   attacker; and the 20-block "reveal invisibles via GLOWING" sweep. Confirm whether these live anywhere
   in the port (not found).
4. **[MED] Keybinding parity for Susanoo**: original summons on key2 (F); port uses the new Up-arrow
   power-increase key. Either wire Susanoo into the key2 path for Mangekyo heads or accept the rebinding.
5. **[LOW] Mangekyo-Sasuke helmet passives**: there is no `MangekyoSharinganHelmetItem` class; confirm
   SPEED III pulse + active-jutsu durability drain are reproduced (Obito/Eternal have bespoke classes).
6. **[LOW] Eye-color tinting of Susanoo flame**: original reads the helmet's random per-owner `color`;
   port hardcodes `0x20B83DBA`. Restore per-owner tint if desired.
7. **[LOW] Verify**: `amaterasu2` sound asset, AmaterasuFlame HUD icon, skeleton-touch Amaterasu flame
   for Sasuke wearers, Totsuka sword item existence, snug-helmet highlight/forehead render overlay.
