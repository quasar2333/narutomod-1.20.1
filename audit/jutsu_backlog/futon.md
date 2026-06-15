# Futon (Wind Release) — Jutsu Audit & Port Backlog

Comparison of the 1.12.2 Forge source (read-only source of truth) against the
1.20.1 Forge port. Goal: a developer can restore EVERY Futon technique (mechanics
AND visuals) without missing any.

- 1.12.2 base: `D:\mcmodding\naruto\1.12.2\src\main\java\net\narutomod`
- 1.20.1 base: `D:\mcmodding\naruto\1.20.1\src\main\java\net\narutomod`

## Domain scope

The Futon "tome" item is `ItemFuton` (1.12.2) / `FutonItem` (1.20.1). It registers
**4 jutsu** (indices 0–3). Three D/B/C/S scroll items teach them, and there is a
standalone **Folding Fan** weapon that reuses the Great Breakthrough mechanic.

Total distinct techniques audited: **5**
- 4 are real Futon jutsu in the tome (Chakra Flow, Rasenshuriken, Vacuum Sphere, Great Breakthrough)
- 1 is the Folding Fan item attack (a projectile + Great Breakthrough wind)

Plus 3 supporting scroll items (learning UI) and 1 weapon item.

## Framework / shared infrastructure

### 1.12.2 framework
- **`ItemJutsu.Base`** (`item/ItemJutsu.java`) — base for the tome. Holds per-jutsu
  XP map, cooldown map (`defaultCooldownMap`, default `-1` = unset), `getPower(...)`
  charging curve (`getPower(stack, entity, timeLeft, basePower, powerupDelay)` at
  `ItemJutsu.java:151`), `getMaxPower`, `getModifier` (chakra modifier × XP modifier).
- **`ItemJutsu.JutsuEnum`** (`ItemJutsu.java:548`) — `(index, unlocalizedName, rank,
  chakraUsage, IJutsuCallback)`. Rank→XP curve (`ItemJutsu.java:571`):
  S=400, A=250, B=200, C=150, D=100, none=900 required XP.
- **`ItemJutsu.IJutsuCallback.createJutsu(stack, entity, power)`** — every jutsu's
  spawn entry point.
- Charging feedback (shared, `ItemJutsu.java:173-185`): blue smoke particle
  `0x106AD1FF` around player + `narutomod:charging_chakra` sound every 10 ticks +
  on-screen power readout.

### 1.20.1 framework (port)
- **`JutsuItem`** + **`JutsuDefinition`** replace `ItemJutsu.Base`/`JutsuEnum`.
  `JutsuDefinition.ranked(index, translationKey, rank, chakraUsage)` then
  `.withPower(basePower, powerUpDelay)`. See `FutonItem.java:24-30`.
- **`JutsuScrollDefinition`** enum (`item/JutsuScrollDefinition.java`) is a unified,
  data-driven replacement for the per-jutsu `ItemScroll*` classes. All four Futon
  scrolls are entries here (lines 267–298). There are **no** standalone
  `ScrollFuton*Item.java` classes — this is by design, not a gap.
- Entity types registered in `registry/ModEntityTypes.java`; renderers registered in
  `client/ClientModEvents.java`.
- Power charging: `JutsuItem.getChargingPower(stack, player, remainingUseDuration,
  basePower, powerUpDelay)`; `FutonItem.getFutonPower` clamps via `getMaxFutonPower`.

### Notable port-wide notes
- The two "invisible aura" entities (Great Breakthrough, Vacuum) and the Folding Fan
  projectile use **`NoopEntityRenderer`** in the port. This is FAITHFUL: in 1.12.2
  the Great Breakthrough/Vacuum `EC` entities had **no renderer** (pure server-side
  particle emitters), and the Folding Fan arrow was rendered as an AIR snowball
  (effectively invisible). Their visuals are entirely particle-based and are ported.

---

## Chakra Flow (Futon Chakra Flow)

| Field | Value |
|---|---|
| Display name | "futonchakraflow" (translationKey `entity.narutomod.futonchakraflow`) |
| Index / id | 0 |
| Rank | 'D' |
| Chakra cost | 20.0 to activate; then **drain 2.0 (20×0.1) every 10 ticks** while weapon held (`ItemFuton.java:177`) |
| XP to unlock | D-rank ⇒ 100 required XP (`ItemJutsu.java:571`) |
| Cooldown | default (`-1`, none set) |
| Entity spawned | `ItemFuton.ChakraFlow` (extends `EntityChakraFlow.Base`), entityid 132 |
| Procedure(s) | callback `ItemFuton.ChakraFlow.Jutsu`; toggles on/off via player NBT `FutonChakraFlowEntityIdKey` |
| Particles | client renderer `RenderChakraFlow`: 30× SMOKE colored `0x106AD1FF`, spawned along the right-arm/weapon blade (`ItemFuton.java:208-212`) |
| Sounds | none specific (activation handled by tome) |
| Visual | Invisible aura entity that follows the user. Renderer projects blue chakra smoke along the held weapon's blade using the player's right-arm model bone (3rd-person transform in `EntityChakraFlow.RenderCustom.transform3rdPerson`). Applies **STRENGTH** (amplifier = strengthModifier + original strength) + **Reach** potion while held. |
| Mechanics detail | `strengthModifier` scales with XP modifier × ninjaLevel/20 (`ItemFuton.java:150-157`). Effects re-applied each tick (duration 2) only while `isWeapon(mainhand)`. Toggle: re-casting kills the active entity. |
| 1.12.2 file:line | `item/ItemFuton.java:47, 140-213`; `entity/EntityChakraFlow.java` (base+renderer) |

**1.20.1 status: DONE.**
- Item entry `FutonItem.CHAKRA_FLOW` (`FutonItem.java:24`), activation
  `activateChakraFlow` (`FutonItem.java:141-157`) with toggle via
  `FutonChakraFlowEntity.stopActive`.
- Entity `entity/FutonChakraFlowEntity.java`: STRENGTH (`DAMAGE_BOOST`) + `ModEffects.REACH`
  applied each tick while `ProcedureUtils.isWeapon(mainhand)`; chakra drain
  `CHAKRA_FLOW.chakraUsage()*0.1` every 10 ticks (`:96`); strengthModifier scaling
  reproduced in `strengthModifierFor` (`:206-218`).
- Renderer `client/renderer/FutonChakraFlowRenderer.java` reproduces the right-arm
  blade smoke (`0x106AD1FF`, 30 particles) faithfully. Registered at
  `ClientModEvents.java:288`.
- Scroll: `JutsuScrollDefinition.CHAKRA_FLOW` (`JutsuScrollDefinition.java:291`).
- Minor difference (cosmetic): uses `textures/white_square.png` placeholder as the
  base entity texture (entity body itself is invisible); particle is the real visual.

---

## Rasenshuriken (Wind Release: Rasenshuriken)

| Field | Value |
|---|---|
| Display name | "rasenshuriken" (`entity.narutomod.rasenshuriken`) |
| Index / id | 1 |
| Rank | 'S' |
| Chakra cost | 1000.0 × power |
| Power curve | `getPower(..., basePower=0, powerupDelay=300)` (`ItemFuton.java:112`); maxPower clamped to **2.0** (non-creative) (`ItemFuton.java:98`) |
| XP to unlock | S-rank ⇒ 400 required XP |
| Cooldown | default (`-1`); gated by Rasengan being enabled (see below) |
| Entity spawned | `EntityRasenshuriken.EC` (extends `EntityScalableProjectile.Base`), entityid 344/345 |
| Procedure(s) | `EntityRasenshuriken.EC.Jutsu` (futon, power≥0.1) and `EntityRasenshuriken.EC.TSBVariant` (Six-Path, fixed 4.0 scale, black ball, 8× dmg); `ProcedureAoeCommand`, `ProcedureLightSourceSetBlock`, `ProcedureSync`, `EventSphericalExplosion` |
| Particles | growth: SMOKE `0x10FFFFFF` (`fullScale*12` count) each tick while growing (`EntityRasenshuriken.java:176-180`) |
| Sounds | on spawn `narutomod:rasenshuriken` (vol 5); looping `narutomod:wind` every 80 ticks; impact `narutomod:rasenshuriken_explode` every 4 impact ticks |
| Visual | Custom 3D model `ModelRasenshuriken` (central spinning **ball** of 24 stacked rotated cubes + 4 cross **flaps** of pinwheel blades). Ball color data-driven (`BALL_COLOR`, default `0x20A9DEFF`, TSB `0xE0101010`). Additive/translucent blend; full-bright; cull disabled. Grows from scale 0.1 → fullScale over 20 ticks above user, then homes to look target on launch. |
| Impact detail | On hit: `setImpactTicks`, halt motion, then expanding AoE — scale ×1.15/tick (first 20) then ×1.001, `doImpactDamage` (jutsu damage bypasses armor, `fullScale×impactDamageMultiplier`), spherical explosion event, motion zeroed; emits light source block if fullScale≥4; dies at impactTicks≥200 or ticksInAir>200. Sets `forceBowPose` NBT on user while alive. |
| 1.12.2 file:line | `item/ItemFuton.java:48, 97-99, 108-112`; `entity/EntityRasenshuriken.java` (full: EC, model, renderer) |

**1.20.1 status: DONE.**
- Item entry `FutonItem.RASENSHURIKEN` w/ `.withPower(0,300)` (`FutonItem.java:25`),
  activation `activateRasenshuriken` (`:122-139`), power clamp to 2.0
  (`getMaxFutonPower`, `:256`). Gate: only usable if Rasengan available — see Risk note.
- Entity `entity/RasenshurikenEntity.java`: grow-over-20-ticks, homing
  (`guideTowardOwnerLook`), legacy block/entity sweep impact (`findImpact`),
  expanding impact (×1.15/×1.001, `growImpactScale`), AoE armor-bypass damage
  (`damageImpactTargets`), spherical explosion (`SpecialEvent.setSphericalExplosionEvent`),
  light source (`refreshLegacyLightSource`), forceBowPose sync, wind loop, charging smoke.
  Ball color + impactDamageMultiplier data-driven. TSB variant
  `spawnTruthSeekingVariantFrom` (`:114`) ported.
- Model `client/model/RasenshurikenModel.java` + renderer
  `client/renderer/RasenshurikenRenderer.java` (ball + flaps, color/alpha-driven
  render types `translucentEmissiveNoCull`/`energyAdditive`, FULL_BRIGHT). Registered
  `ClientModEvents.java:357`. Texture `textures/rasenshuriken.png`.
- Sounds: `ModSounds.SOUND_RASENSHURIKEN`, `SOUND_RASENSHURIKEN_EXPLODE`, `SOUND_WIND`.
- Scroll `JutsuScrollDefinition.RASENSHURIKEN` (`:275`).

---

## Vacuum Sphere (Futon: Vacuum Sphere / "futon_vacuum")

| Field | Value |
|---|---|
| Display name | "futon_vacuum" (`entity.narutomod.futon_vacuum`) |
| Index / id | 2 |
| Rank | 'B' |
| Chakra cost | 20.0 × power |
| Power curve | `getPower(..., basePower=0, powerupDelay=20)` (`ItemFuton.java:114`); maxPower clamped to **50.0** (`ItemFuton.java:99-100`); requires power≥1.0 to fire (`EntityFutonVacuum.java:94`) |
| XP to unlock | B-rank ⇒ 200 required XP |
| Cooldown | default (`-1`) |
| Entity spawned | `EntityFutonVacuum.EC` (extends `Entity`, invisible), entityid 185/186 |
| Procedure(s) | callback `EntityFutonVacuum.EC.Jutsu`; inner `AirStream extends ProcedureAirPunch` (`execute2`) |
| Particles | `AirStream.preExecuteParticles`: 400× SMOKE `0x20FFFFFF`, streamed forward from eye+1.6 (`EntityFutonVacuum.java:121-129`) |
| Sounds | every 5 ticks `BLOCK_FIRE_EXTINGUISH` (vanilla); on hit `narutomod:bullet_impact` |
| Visual | No entity renderer (invisible). Visual = forward-blasting white smoke stream. Lives `power*4` ticks, executing the air-stream every 5 ticks. |
| Mechanics detail | `AirStream` deals `power × 0.5` jutsu damage to entities in the line; `getBreakChance` returns 0.2 (chance to break blocks ≤ hardness limit) and plays impact sound. |
| 1.12.2 file:line | `item/ItemFuton.java:49, 99-100, 113-115`; `entity/EntityFutonVacuum.java` |

**1.20.1 status: DONE (no model — faithful, was invisible in 1.12.2).**
- Item entry `FutonItem.VACUUM` `.withPower(0,20)` (`FutonItem.java:27`); activation
  `activateVacuum` (`:103-120`) with power≥1.0 gate and 50.0 clamp.
- Entity `entity/FutonVacuumEntity.java`: lives `power*4` ticks; every 5 ticks plays
  `FIRE_EXTINGUISH` + executes air stream (400 smoke particles `0x20FFFFFF`,
  `damage = power*0.5`, block break chance 0.2 via
  `ProcedureUtils.breakBlockAndDropWithChance`, `bullet_impact` sound). Damage source
  `ModDamageTypes.ninjutsu`.
- Renderer: `NoopEntityRenderer` (`ClientModEvents.java:290`) — correct, entity is invisible.
- Scroll `JutsuScrollDefinition.FUTON_VACUUM` (`:267`).

---

## Great Breakthrough (Futon: Daitoppa / "futon_great_breakthrough")

| Field | Value |
|---|---|
| Display name | "futon_great_breakthrough" (`entity.narutomod.futon_great_breakthrough`) |
| Index / id | 3 |
| Rank | 'C' |
| Chakra cost | 20.0 × power |
| Power curve | `getPower(..., basePower=5, powerupDelay=20)` (`ItemFuton.java:117`); creative max 50 in port |
| XP to unlock | C-rank ⇒ 150 required XP |
| Cooldown | default (`-1`) |
| Entity spawned | `EntityFutonGreatBreakthrough.EC` (extends `Entity`, invisible), entityid 187/188 |
| Procedure(s) | callback `EntityFutonGreatBreakthrough.EC.Jutsu`; inner `AirPunch extends ProcedureAirPunch` (blockHardnessLimit 1.0) |
| Particles | `AirPunch.preExecuteParticles`: 50× SMOKE `0x80FFFFFF`, wide cone from look-vec, lifetime 80–100 (`EntityFutonGreatBreakthrough.java:101-112`) |
| Sounds | tick 1: `narutomod:wind` (pitch = power×0.2) |
| Visual | No entity renderer (invisible). Visual = broad wind smoke cone. Lives `power` ticks; runs the air-punch each tick. |
| Mechanics detail | Pushes entities: `ProcedureUtils.pushEntity(player, target, range×1.6, 3.0)`. Breaks blocks with distance-falloff chance `(1 - dist/clamp(range,0,30))×0.2`, hardness ≤ 1.0. |
| 1.12.2 file:line | `item/ItemFuton.java:50, 116-118`; `entity/EntityFutonGreatBreakthrough.java` |

**1.20.1 status: DONE (no model — faithful, was invisible in 1.12.2).**
- Item entry `FutonItem.BIG_BLOW` `.withPower(5,20)` (`FutonItem.java:29`); activation
  `activateBigBlow` (`:159-175`).
- Entity `entity/FutonGreatBreakthroughEntity.java`: lives `power` ticks; tick-1
  `SOUND_WIND` at pitch `power*0.2`; per-tick cone of 50 smoke particles `0x80FFFFFF`;
  push entities `pushEntity(owner, target, range*1.6, 3.0)`; block break with the same
  distance-falloff chance and hardness limit 1.0 (`getBreakChance`, `:249-256`).
- Renderer: `NoopEntityRenderer` (`ClientModEvents.java:289`) — correct, invisible.
- Scroll `JutsuScrollDefinition.BIG_BLOW` (`:283`, name "big_blow").

---

## Folding Fan (Gunbai) — weapon item

This is a standalone weapon (`ItemFoldingFan` / `FoldingFanItem`), not a tome entry.
On release it fires a projectile AND triggers a Great Breakthrough wind blast.

| Field | Value |
|---|---|
| Display name | "folding_fan" item |
| Index / id | item; projectile entityid 352 (`entitybulletfolding_fan`) |
| Rank | n/a (weapon, durability 500) |
| Chakra cost | none (item durability: -1 on stop-using in 1.12.2; -2 in port) |
| Attributes | +4 ATTACK_DAMAGE, -2.4 ATTACK_SPEED on mainhand; `setFull3D` |
| Entity spawned | `ItemFoldingFan.EntityArrowCustom` (extends `EntityTippedArrow`) + `EntityFutonGreatBreakthrough.EC` |
| Procedure(s) | `ProcedureFoldingFanRangedItemUsed` — damages item, extinguishes fire, spawns Great Breakthrough with power `getItemInUseMaxCount()*0.5` |
| Particles | none from item; Great Breakthrough provides the wind cone |
| Sounds | original plays an empty-ResourceLocation sound (effectively none) |
| Visual | Arrow rendered via `RenderSnowball` with an **AIR** itemstack ⇒ **invisible** projectile. UseAnim BOW, 72000 max use. Arrow: damage 5, knockback 5, no crit, silent, no pickup; removed when `inGround`. |
| 1.12.2 file:line | `item/ItemFoldingFan.java` (full); `procedure/ProcedureFoldingFanRangedItemUsed.java` |

**1.20.1 status: DONE (faithful; projectile intentionally invisible).**
- `item/FoldingFanItem.java`: +4 dmg / -2.4 speed modifiers; UseAnim BOW; on release
  spawns `FoldingFanProjectileEntity` (speed 2.0) AND
  `FutonGreatBreakthroughEntity.spawnFrom(player, windPower)` where
  `windPower = clamp(usedTicks*0.5, 1, 50)` — matches the original
  `getItemInUseMaxCount()*0.5`. Clears fire; durability -2.
- Projectile `entity/FoldingFanProjectileEntity.java` (ThrowableItemProjectile):
  damage 5, knockback 2.5 + 0.35 vertical, `MAX_LIFE` 200, logs battle exp, ignores
  owner. Renderer `NoopEntityRenderer` (`ClientModEvents.java:274`) — matches the
  original AIR-snowball (invisible) projectile.
- Difference (minor, intentional): original knockbackStrength was 5; port uses
  KNOCKBACK 2.5 + vertical lift. Original durability cost on stop-use was 1; port uses 2.

---

## Scroll items (jutsu-learning UI)

In 1.12.2 each scroll was its own class opening a GUI:
- `ItemScrollFutonChakraFlow.java` (D-rank) → `GuiScrollFutonChakraFlowGui`
- `ItemScrollFutonVacuum.java` (B-rank) → `GuiScrollFutonVacuumGui`
- `ItemScrollBigBlow.java` (C-rank) → `GuiScrollBigBlowGui`
- (no dedicated Rasenshuriken scroll class in the listed set; Rasenshuriken is
  gated behind Rasengan rather than a scroll in 1.12.2)

**1.20.1 status: DONE (unified system).**
All four are entries in the data-driven `item/JutsuScrollDefinition.java`
(`FUTON_VACUUM`, `RASENSHURIKEN`, `BIG_BLOW`, `CHAKRA_FLOW`, lines 267–298), each
pointing at `ModItems.FUTON` and the matching `FutonItem.<JUTSU>` definition with the
correct rank tooltip. No separate per-scroll classes/GUIs are needed; this is a
deliberate architecture change, not a missing port.

---

## Risks / verification notes

1. **Rasenshuriken Rasengan gate.** In 1.12.2 (`ItemFuton.RangedItem.onUpdate`,
   `ItemFuton.java:123-137`) Rasenshuriken is only enabled when the player can use
   Rasengan (from `ItemNinjutsu`). Confirm the port enforces this same prerequisite;
   `FutonItem.use`/`canActivateFuton` was not seen to replicate the per-tick
   enable/disable sync. **Needs verification** that Rasenshuriken availability still
   tracks Rasengan.
2. **Folding Fan knockback/durability deltas** are minor gameplay differences (5 vs
   2.5 knockback; 1 vs 2 durability cost). Decide if exact parity is required.
3. **Resource assets** (`textures/rasenshuriken.png`, `textures/blocks/futon.png`,
   sounds `rasenshuriken`, `rasenshuriken_explode`, `wind`, `bullet_impact`,
   `charging_chakra`) must exist in the 1.20.1 resource pack; code references them but
   asset presence was not file-checked in this audit.
4. **`white_square.png`** is used as the ChakraFlow entity texture placeholder; the
   entity is invisible so this is harmless, but confirm the asset exists to avoid a
   missing-texture log spam.
