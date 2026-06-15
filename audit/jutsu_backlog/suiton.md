# Suiton (Water Release) — Jutsu Port Audit (1.12.2 → 1.20.1)

Faithful inventory of every Suiton-domain technique, comparing the read-only 1.12.2 source with the
work-in-progress 1.20.1 port. Goal: a developer can restore EVERY technique (mechanics AND visuals)
with no omissions.

Original (truth) base: `D:\mcmodding\naruto\1.12.2\src\main\java\net\narutomod`
Port (WIP) base:       `D:\mcmodding\naruto\1.20.1\src\main\java\net\narutomod`

---

## Framework / Notes

### 1.12.2 structure
- The tome item is `item/ItemSuiton.java`. It holds the jutsu list as `ItemJutsu.JutsuEnum` constants
  (`ItemSuiton.java:65-70`). Each `JutsuEnum(index, registryName, rank, chakraUsage, IJutsuCallback)`.
  The `IJutsuCallback` (a static `Jutsu` inner class on each entity) is the actual mechanic.
- `RangedItem extends ItemJutsu.Base` (`ItemSuiton.java:99-132`) drives charging via `getPower(...)`
  (per-jutsu base-power + power-up-delay) and `getMaxPower(...)` caps.
- Two jutsu entities live INSIDE `ItemSuiton.java` itself: `EntityMist` (Hiding in Mist) and
  `EntityStream` (Water Bullet/Stream). The other four live in `entity/` files:
  `EntityWaterDragon`, `EntityWaterPrison`, `EntitySuitonShark`, `EntityWaterShockwave`.
- The eight "scroll" items (`ItemScrollWaterDragon`, `ItemScrollWaterPrison`, `ItemScrollWaterShark`,
  `ItemScrollWaterShockwave`, `ItemScrollWaterStream`, `ItemScrollHidingInMist`, `ItemScrollPoisonMist`)
  are NOT mechanics. Each is a plain `Item` that on right-click opens a learning GUI
  (`entity.openGui(NarutomodMod.instance, GuiScroll...Gui.GUIID, ...)`) and adds a rank tooltip
  (e.g. "B-rank jutsu scroll"). They are the unlock UIs for the corresponding jutsu.
- IMPORTANT: `ItemScrollPoisonMist` (`ItemScrollPoisonMist.java`) is grouped with Suiton scrolls here,
  but Poison Mist is NOT one of the six `ItemSuiton` jutsu. In the port it belongs to the medical
  element (`item/IryoJutsuItem.java`). It is documented below for completeness but is out of the
  Suiton tome's scope in both versions.

### Shared base classes / entities used
- `EntityBeamBase.Base` / `EntityBeamBase.Renderer` / `EntityBeamBase.Model` — base for `EntityStream`.
- `EntityScalableProjectile.Base` — base for `EntityWaterDragon.EC` and `EntitySuitonShark.EC`.
- `Entity` (plain) — base for `EntityMist`, `EntityWaterPrison.EC`, `EntityWaterShockwave.EC`.
- Procedures: `ProcedureAirPunch` (Stream block/entity sweep), `ProcedureRenderView.setFogDensity`
  (Mist fog), `ProcedureSync.SetGlowing` / `ProcedureSync.EntityNBTTag` (Mist glow, Prison bow-pose),
  `ProcedureAoeCommand` (Dragon/Shark AoE damage), `ProcedureUtils` (geometry, air-block scan, block sorter).
- Blocks: `BlockWaterStill.block` (solid water used by Prison / Shockwave domes).
- Event: `net.narutomod.event.EventSetBlocks` (timed FLOWING_WATER placement on impact/release).

### 1.20.1 port structure
- Tome: `item/SuitonItem.java extends JutsuItem`. Jutsu = `JutsuDefinition` records
  (`SuitonItem.java:26-43`), created via `JutsuDefinition.ranked(index, translationKey, rank, chakraUsage)`
  optionally `.withPower(basePower, powerUpDelay)`.
- `JutsuDefinition` and the XP/cooldown framework live in `item/JutsuItem.java` (record at line 319):
  rank→requiredXp map (`JutsuItem.java:340-349`): S=400, A=250, B=200, C=150, D=100, default=900.
  Non-affinity users pay 2× requiredXp (`JutsuItem.java:152-154`). Default `powerUpDelay` = 50f
  (`JutsuItem.java:329`); charging formula at `JutsuItem.java:239-241`. Cooldowns are tag-driven
  (`JutsuItem.java:168-197`) — note: SuitonItem does NOT call `setJutsuCooldown` on activation, so
  no cooldown is currently applied (mirrors 1.12.2, where `ItemSuiton` left cooldowns at default/commented).
- Entities ported to `entity/`: `SuitonStreamEntity`, `SuitonMistEntity`, `WaterDragonEntity`,
  `WaterPrisonEntity`, `WaterSharkEntity`, `WaterShockwaveEntity`, plus `PoisonMistEntity`.
- Client renderers (`client/renderer/`) + models (`client/model/`): `SuitonStreamRenderer`+`SuitonStreamModel`,
  `WaterDragonRenderer`+`WaterDragonModel`, `WaterSharkRenderer`+`WaterSharkModel`. Registered in
  `client/ClientModEvents.java:389-404`. Mist / Prison / Shockwave / PoisonMist use `NoopEntityRenderer`
  (no entity model — correct, they are fog / blockworld effects, same as 1.12.2 which had no Render for them).
- All seven scrolls registered in `registry/ModItems.java` (lines 262, 280, 293-301).
- Custom render types referenced: `NarutoRenderTypes.scrollingTranslucent`, `translucentEmissiveNoCull`.
- Textures needed: `narutomod:textures/dragon_blue.png`, `narutomod:textures/gas256.png`,
  `narutomod:textures/shark.png`, `minecraft:textures/block/water_flow.png`.

---

## 1. Hiding in Mist (Kirigakure no Jutsu) — "suitonmist"

| Field | Value |
|---|---|
| Index | 0 (`ItemSuiton.java:65`) |
| Registry/key | `suitonmist`; port translation key `entity.narutomod.suitonmist` |
| Rank | 'D' |
| Chakra | 100 flat (`HIDINGINMIST`, no power scaling) |
| Level/XP/rank | D-rank scroll `scroll_hiding_in_mist` (`ItemScrollHidingInMist.java`); port req XP=100 (×2 non-affinity) |
| Cooldown | none set (commented in 1.12.2 `ItemSuiton.java:105`) |
| Entity | `ItemSuiton.EntityMist` (plain `Entity`, `ItemSuiton.java:134-241`) |
| Procedures | `ProcedureRenderView.setFogDensity` (player fog), `ProcedureSync.SetGlowing` (mark enemies for user) |
| Particles | none (effect is fog + glow + AI follow-range debuff) |
| Sounds | `narutomod:kirigakurenojutsu` (PLAYERS, vol 5) at cast (`ItemSuiton.java:234-236`) |
| Visual | No model. Radius = min(1.5×playerLevel, 60) for players else 32. Fog density ramps in over `buildTime`=200t, idle (400t dry / 1200t near liquid), dissipates over 120t. Reduces mob `FOLLOW_RANGE` via attribute modifier `7c3e5536-...`; user sees enemies glowing. |

Mechanic detail (`ItemSuiton.java:134-240`): per-tick, for every `EntityLivingBase` within radius+100:
players get fog density `d0 = phase × density / max(distanceOutsideRadius,1)`; mobs get a negative
`FOLLOW_RANGE` modifier scaled by phase; non-user entities glow for the user (if user is `EntityPlayerMP`).
On `setDead()` resets fog and removes all modifiers/glow.

1.20.1 status: **DONE** — `entity/SuitonMistEntity.java` faithfully reproduces radius formula
(`SuitonMistEntity.java:57`), build/idle/dissipate phases (BUILD_TIME=200, idle 400/1200, DISSIPATE=120,
`:36-44,165-173`), per-tick fog sync via `network/SuitonMistFogMessage` + `client/SuitonMistFogEvents`
(`:193-199`), mob FOLLOW_RANGE reduction with the same UUID (`:35,209-224`), enemy glow via
`network/EntityGlowMessage` (`:201-207`), cleanup on remove (`:226-234,262-277`), and cast sound
`SOUND_KIRIGAKURENOJUTSU` (`:69-70`). NoopEntityRenderer registered (`ClientModEvents.java:389`).
Particles: port ADDS `ParticleTypes.CLOUD` puff (`:236-246`) that 1.12.2 did not have — harmless
visual enhancement, not a regression. Remaining: verify `ProcedureRenderView` fog density 255 reset
edge case is matched by the fog message duration; otherwise faithful.

---

## 2. Water Bullet / Water Stream (Suiton: Teppodama) — "suitonstream" (a.k.a. WATERBULLET)

| Field | Value |
|---|---|
| Index | 1 (`ItemSuiton.java:66`) |
| Registry/key | `suitonstream`; port key `entity.narutomod.suitonstream` |
| Rank | 'C' |
| Chakra | 10 × power (`WATERBULLET`); power scales |
| Power scaling | base 5f, powerUpDelay 20 (`getPower`, `ItemSuiton.java:117`); max power 30 (`getMaxPower`, `:127`) |
| Level/XP/rank | C-rank scroll `scroll_water_stream` (`ItemScrollWaterStream.java`); port req XP=150 |
| Cooldown | none set (commented `ItemSuiton.java:106`) |
| Entity | `ItemSuiton.EntityStream` (`EntityBeamBase.Base`, `ItemSuiton.java:243-325`) |
| Procedures | inner `AirPunch extends ProcedureAirPunch` (`:286-314`) — block sweep + entity damage |
| Particles | `WATER_DROP` (pre), `WATER_SPLASH` (during) via AirPunch (`:290-291`) |
| Sounds | `narutomod:waterblast` at tick 1, pitch=power/30 (`:274`) |
| Visual | `RenderStream` (`:327-346`) + `ModelLongCube` (`:351-373`): long water cube beam, texture `minecraft:.../water_flow.png`, length = beamLength × life-fade (grows over first 10t, shrinks last 10t). |

Mechanic detail: beam re-anchors to shooter's look each tick (`shoot()`, `:258-267`), lives `maxLife`=100t,
every 5th tick (`ticksAlive%5==1`) runs `stream.execute2(shooter, power, 0.5)`. Damage =
`power × damageModifier(0.5)` (`:296-298`). Block break: hardness ≤5, dropChance 0.4, breakChance falls
off with distance (`:311-313`); broken blocks topped with timed FLOWING_WATER via `EventSetBlocks` (`:304-305`).

1.20.1 status: **DONE** — `entity/SuitonStreamEntity.java`: MAX_LIFE=100, STREAM_RADIUS=0.5,
DAMAGE_MODIFIER=0.5, EXECUTE_INTERVAL 5 (`:36-40`); re-anchors to look (`:189-198`); damages along ray
(`:218-229`); breaks blocks via `ProcedureUtils.breakBlockAndDropWithChance(... 5f, breakChance, 0.4f ...)`
and places temporary water above (`:238-249,278-284`); distance break-chance falloff (`:270-276`);
`SOUND_WATERBLAST` at tick 1 pitch power/30 (`:90-93`). Visual: `SuitonStreamRenderer`+`SuitonStreamModel`
reproduce the long water-flow cube with life-fade (`SuitonStreamRenderer.java:32-66`), registered
(`ClientModEvents.java:391`). Particles use `ParticleTypes.SPLASH` along the beam (`:315-341`) — a
reasonable analogue of WATER_DROP/WATER_SPLASH. Faithful.

---

## 3. Water Dragon (Suiryudan no Jutsu) — "water_dragon"

| Field | Value |
|---|---|
| Index | 2 (`ItemSuiton.java:67`) |
| Registry/key | `water_dragon`; port key `entity.narutomod.water_dragon` |
| Rank | 'B' |
| Chakra | 50 × power; +50×2 extra if NOT over water (`EntityWaterDragon.java:223-224`) |
| Power scaling | base 0.9f, powerUpDelay 150 (`getPower`, `ItemSuiton.java:113`); max power 5 (`:129`) |
| Activation gate | requires `power≥1.0 && entity.onGround` (`EntityWaterDragon.java:223`) |
| Level/XP/rank | B-rank scroll `scroll_water_dragon` (`ItemScrollWaterDragon.java`); port req XP=200 |
| Cooldown | none set |
| Entity | `EntityWaterDragon.EC` (`EntityScalableProjectile.Base`, `EntityWaterDragon.java:69-238`) |
| Procedures | `ProcedureAoeCommand` (3-block AoE damage on impact), `ProcedureUtils` (aim, air-block scan, Vec2f spine) |
| Particles | wait phase: `WATER_WAKE` at wake point; launched: `WATER_DROP` cloud (`:182-195`) |
| Sounds | `narutomod:suiton_suiryuudan` (NEUTRAL, vol 5) on cast (`:232-234`) |
| Visual | `RenderDragon` (`:240-306`) + `ModelDragonHead` (`:311-579`): blue serpentine dragon, head + up to 100 articulated spine segments following motion (`partRot`/`updateSegments`), whiskers/horns/jaw/eyes; double-pass render (gas256 translucent underlay tinted 0.04/0.325/0.733, then `dragon_blue.png` body at alpha 0.8); emissive eyes. |

Mechanic detail: `wait`=60t rise/aim phase (rises `motionY` first half), then launches toward look /
mob target at speed 0.95 (`:140-159`). On impact (`onImpact`, `:198-214`): explosion radius `5×scale`,
AoE `ProcedureAoeCommand.set(this,0,3).damageEntities(jutsuDamage, 20×scale)`, fills its bounding box's
bottom layer with FLOWING_WATER via `EventSetBlocks`. Lifespan ≤100t. Spine animation driven by
`partRot` list + segment-length stepping (`updateSegments`, `:161-180`).

1.20.1 status: **DONE** — `entity/WaterDragonEntity.java`: WAIT_TICKS=60, SPINE_SEGMENTS=100,
MAX_LIFE=100, PROJECTILE_SPEED=0.95 (`:45-49`); rise/aim then launch (`:135-145,327-347`); aim at
look/mob target via `ProcedureUtils.objectEntityLookingAt` (`:349-357`); explosion `5×scale` + 3-block
AoE `20×scale` damage (`:462-485`); impact water fill (`:487-498`); seeded `partRotations` matching the
8-entry seed list (`:268-277` vs `EntityWaterDragon.java:79-83`); per-segment update (`:279-309`).
Extra-chakra-when-not-over-water handled in `SuitonItem.java:159-165`; onGround + power≥1 gate
(`SuitonItem.java:151-158`). Sound `SOUND_SUITON_SUIRYUUDAN` (`:90-91`). Visual: `WaterDragonRenderer`
+ `WaterDragonModel` reproduce double-pass gas underlay + body, tint, scrolling gas texture, segment
articulation, face-detail toggle (`WaterDragonRenderer.java:36-81`); registered (`ClientModEvents.java:402`).
Faithful. Minor: port launched particle uses `FALLING_WATER` and bubbles in water vs legacy `WATER_DROP`
— acceptable analogue.

---

## 4. Water Prison (Suiro no Jutsu) — "water_prison"

| Field | Value |
|---|---|
| Index | 3 (`ItemSuiton.java:68`) |
| Registry/key | `water_prison`; port key `entity.narutomod.water_prison` |
| Rank | 'C' |
| Chakra | 200 flat to cast; sustained burn = 200×0.1 = 20/sec (`EntityWaterPrison.java:71,149`) |
| Power scaling | none |
| Activation gate | a living target within 4 blocks (look-at for players, attackTarget for mobs) |
| Level/XP/rank | C-rank scroll `scroll_water_prison` (`ItemScrollWaterPrison.java`); port req XP=150 |
| Cooldown | none set |
| Entity | `EntityWaterPrison.EC` (plain `Entity`, `EntityWaterPrison.java:65-218`) |
| Procedures | `ProcedureUtils` (air-block scan, FOV, velocity), `ProcedureSync.EntityNBTTag` (forceBowPose) |
| Particles | none (effect is solid-water cube + holding) |
| Sounds | `narutomod:suironojutsu` (NEUTRAL) on cast (`:197-199`); splash on release (`getSplashSound`, `:109`) |
| Visual | No model. Fills a target-sized box (±0.5) with `BlockWaterStill` solid water; holds target locked at center; pushes others out. User forced into bow pose. |

Mechanic detail: default duration 3600t (`:192`). Each tick if user&target alive, ≤4 blocks apart,
target in user FOV, and ≥2/3 of prison water remains (`enoughWaterRemaining`, `:125-134`): target is
teleported to center, others in box shoved outward 0.2 (`:142-147`), 20/sec chakra burn (`:148-150`).
`trappedMap` static prevents double-trapping. On death: removes water, places timed FLOWING_WATER floor,
clears bow pose (`setDead`, `:106-123`).

1.20.1 status: **DONE** — `entity/WaterPrisonEntity.java`: DEFAULT_DURATION 3600, MAX_HOLD_DISTANCE 4,
CHAKRA_BURN_PER_SECOND 20 (`:42-45`); `findTarget` mirrors look-at/attackTarget within 4 blocks
(`:100-108`, used by `SuitonItem.java:177`); fills prison box with `ModBlocks.WATER_STILL` (`:329-344`);
holds target, pushes others (`:151-153,359-368`); `enoughWaterRemaining` 2/3 check (`:346-357`);
FOV + distance continue check (`:309-316`); forceBowPose via `ProcedureSync.EntityNBTTag` +
`NarutomodModVariables.FORCE_BOW_POSE` (`:463-471`); release places temporary floor water + splash
(`:370-416`); static `TRAPPED_BY_TRAPPER` map with same `isEntityTrapped/By/Trapping` API (`:46,110-120`);
chakra burn 20/20t (`:156-158`); sound `SOUND_SUIRONOJUTSU` (`:93-94`). NoopEntityRenderer (`:403`).
Faithful. Port ADDS splash particles (`:452-461`) absent in 1.12.2 — harmless.

---

## 5. Water Shark Bomb (Suikodan no Jutsu) — "suiton_shark" (WATERSHARK)

| Field | Value |
|---|---|
| Index | 4 (`ItemSuiton.java:69`) |
| Registry/key | `suiton_shark`; port key `entity.narutomod.suiton_shark` |
| Rank | 'B' |
| Chakra | 75 × power (`WATERSHARK`) |
| Power scaling | base 0.9f, powerUpDelay 150 (`getPower`, `ItemSuiton.java:113`, shared with Dragon); max power 5 (`:128`) |
| Activation gate | power ≥ 1.0 (`EntitySuitonShark.java:255`) |
| Level/XP/rank | B-rank scroll `scroll_water_shark` (`ItemScrollWaterShark.java`); port req XP=200 |
| Cooldown | none set |
| Entity | `EntitySuitonShark.EC` (`EntityScalableProjectile.Base`, `EntitySuitonShark.java:66-268`) |
| Procedures | `ProcedureAoeCommand` (AoE damage), `ProcedureUtils` (target look-up, air-block scan) |
| Particles | wait: `WATER_WAKE`; flight: `WATER_DROP` cloud (`:151-152,193-197`) |
| Sounds | `narutomod:suikodannojutsu` (PLAYERS) on cast if user not submerged (`:260-262`) |
| Visual | `RenderCustom` (`:270-306`) + `ModelShark` (`:311-395`): textured shark (`textures/shark.png`), swimming tail/fin limb-swing animation, opening jaw (mouthOpenAmount), translucent blend, no lighting. |

Mechanic detail: `wait`=30t grow-in (scale ramps 0.2→full, `:148-153`), then homes onto target /
look-at, speed 0.85–0.9 (faster in water). Mouth opens over `mouthOpenTime`=20t. `health = power×20`;
takes ninjutsu damage to GROW if fullScale≥4 (`:236-241`), else loses health (`:242-248`).
fullScale≥2 within 10t in-air = rideable (`processInitialInteract`, `:227-232`). On impact (`:201-220`):
AoE damage `scale×(24 in water / 16 dry)`, explosion `scale×2`, fills bottom with FLOWING_WATER.
Lifespan: dies if `ticksInAir>120`.

1.20.1 status: **DONE** — `entity/WaterSharkEntity.java`: WAIT_TICKS=30, MOUTH_OPEN_TICKS=20,
MIN_SCALE=0.2 (`:46-49`); grow-in (`:158-165,402-410`); homing target/look-up via
`ProcedureUtils.objectEntityLookingAt` (`:480-487`); speeds 0.8/0.85/0.9 in/out of water (`:501-506`);
mouth-open ramp (`:542-548`); legacy limb-swing synced data for animation (`:54-57,550-563`);
health = power×20, grow-on-ninjutsu when fullScale≥4 else lose health (`:189-207`); rideable when
fullScale≥2 & flightTicks≤10 (`:319-352,444-450`); impact AoE `scale×(24/16)` + explosion `scale×2` +
water fill (`:632-669`); dies if flightTicks>120 (`:153`); sound `SOUND_SUIKODANNOJUTSU` when not
submerged (`:94-97`). Visual: `WaterSharkRenderer`+`WaterSharkModel` reproduce shark model with
limb-swing + mouth-open animation and translucent emissive render (`WaterSharkRenderer.java:29-63`);
registered (`ClientModEvents.java:390`). Note: port's `SuitonItem` WATER_SHARK uses `.withPower(0.9F,150)`
(`SuitonItem.java:32-33`) and max-power cap 5 (`:303,318`), matching 1.12.2. Faithful.

---

## 6. Exploding Water Shockwave / Great Water Dome (Daibakusui Shoha) — "water_shockwave" (WATERSHOCK)

| Field | Value |
|---|---|
| Index | 5 (`ItemSuiton.java:70`) |
| Registry/key | `water_shockwave`; port key `entity.narutomod.water_shockwave` |
| Rank | 'B' |
| Chakra | 30 × power (`WATERSHOCK`) |
| Power scaling | base 5f, powerUpDelay 50 (`getPower`, `ItemSuiton.java:115`); max power 25 (`:126`) |
| Toggle | re-cast while active = dismiss (`EntityWaterShockwave.java:230-233`; port `SuitonItem.java:212-216`) |
| Level/XP/rank | B-rank scroll `scroll_water_shockwave` (`ItemScrollWaterShockwave.java`); port req XP=200 |
| Cooldown | none set |
| Entity | `EntityWaterShockwave.EC` (plain `Entity`, `EntityWaterShockwave.java:53-250`) |
| Procedures | `ProcedureUtils` (air-block scan, BlockposSorter, velocity), attribute SWIM_SPEED modifier |
| Particles | none (effect is the water dome) |
| Sounds | `narutomod:daibakusuishoha` (PLAYERS, vol 2) on cast (`:242-244`) |
| Visual | No model. Builds a solid `BlockWaterStill` dome of radius=power around the user, moves with the user, grants the user water-breathing + night-vision + swim-speed ×1.2; slows entities inside; collapses (scatter/clear) over death phase. |

Mechanic detail: build-up phase fills `radius²`/tick air blocks within sphere radius (`:105-116`); then
follows user, refilling and trimming out-of-range blocks (cap 512/tick), bailing to death if too much
air re-appears (`:118-142`). Applies WATER_BREATHING(2t), NIGHT_VISION(210t), SWIM_SPEED +1.2 modifier
to user, slows non-shark entities in water by ×0.8 (`:143-156`). Death: scatters dome (1/3 water, rest
air), then clears, ~30t (`onDeathUpdate`, `:79-96`). The static `Jutsu` stores entity id in user NBT
(`WaterShockwaveEntityIdKey`) so re-cast dismisses (`:226-239`).

1.20.1 status: **DONE** — `entity/WaterShockwaveEntity.java`: radius clamp 1..32, BUILDING/DYING states,
STABLE_BLOCKS_PER_TICK=512 (`:48-53,65-71`); build phase fills radius² air blocks (`:146-153,247-275`);
stable phase follows user, refills, trims out-of-range, bails if >half air (`:154-167,277-298`); slows
non-shark entities in water ×0.8 (`:300-307`); user gets WATER_BREATHING(2t)+NIGHT_VISION(210t)+
SWIM_SPEED ×1.2 via `ForgeMod.SWIM_SPEED` modifier (`:309-316,43-47`); death scatter (1/3 water) then
clear over >30t (`:318-345`); toggle dismiss via `ACTIVE_ID_TAG="WaterShockwaveEntityIdKey"` persistent
data (`:41,73-104`, used by `SuitonItem.java:211-235`); sound `SOUND_DAIBAKUSUISHOHA` (`:83-84`);
uses `ModBlocks.WATER_STILL` solid water (`:264-275`). NoopEntityRenderer (`:404`). max-power cap 25
(`SuitonItem.java:306,321`), base 5/delay 50 (`SuitonItem.java:34-35`). Faithful. Port ADDS splash
particles (`:381-391`) absent in 1.12.2 — harmless.

---

## 7. Poison Mist (Dokugiri) — "poison_mist"  [related scroll; NOT a Suiton-tome jutsu]

| Field | Value |
|---|---|
| 1.12.2 source | Only `item/ItemScrollPoisonMist.java` (the learning scroll: opens `GuiScrollPoisonMistGui`, "B-rank jutsu scroll"). No `ItemSuiton` entry; no Suiton-domain mechanic class. |
| Element/owner | NOT in `ItemSuiton`. The mechanic is a medical/Iryo technique. Confirm the 1.12.2 mechanic class outside Suiton scope (likely an `EntityPoisonMist`/Iryo procedure) before porting. |
| Port jutsu | `item/IryoJutsuItem.java:38` `POISON_MIST = ranked(1, "entity.narutomod.poison_mist", 'B', 20.0)` (Iryo/medical tome). |
| Port entity | `entity/PoisonMistEntity.java` (plain `Entity`, owner-attached cone). |
| Port behavior | Per-tick cone from owner look: poisons living entities in a widening radius with WITHER(300t, amp2) (`PoisonMistEntity.java:189-201`), emits `SMOKE_COLORED` purple particles (`:162-187`), plays `SOUND_WINDECHO` (`:77-80`); lifespan ~ power×2 ticks. |
| Port visual | NoopEntityRenderer (`ClientModEvents.java:351`) — particles only; no entity model. |

1.20.1 status: **PARTIAL/OUT-OF-SCOPE for Suiton** — Poison Mist is fully implemented in the port but
under the Iryo element (`IryoJutsuItem`), not the Suiton tome. It is listed here only because its scroll
sits with the Suiton scroll files in this audit batch. To validate faithfulness, the 1.12.2 mechanic
source (outside the eight files given) must be located — it could not be resolved from the Suiton files
alone, so the WITHER/particle/timing values above are the PORT's design and are UNVERIFIED against 1.12.2.
ACTION: locate the original Poison Mist mechanic (grep `1.12.2 .../entity` and `.../procedure` for
"poison"/"dokugiri") to confirm effect type/duration/particles before sign-off.

---

## Scroll items (learning UIs) — all 1.12.2 → 1.20.1

All 1.12.2 scroll items are plain unlock-GUI items (no mechanics). In the port they are registered as
`RegistryObject<Item>` in `registry/ModItems.java` and back the `JutsuScrollDefinition` learning system
(`item/JutsuScrollDefinition.java`).

| 1.12.2 file | rank tooltip | 1.20.1 ModItems | status |
|---|---|---|---|
| `ItemScrollHidingInMist.java` | D | `SCROLL_HIDING_IN_MIST` (`ModItems.java:262,476`) | DONE |
| `ItemScrollWaterStream.java` | C | `SCROLL_WATER_STREAM` (`ModItems.java:301,496`) | DONE |
| `ItemScrollWaterDragon.java` | B | `SCROLL_WATER_DRAGON` (`ModItems.java:293,492`) | DONE |
| `ItemScrollWaterPrison.java` | C | `SCROLL_WATER_PRISON` (`ModItems.java:295,493`) | DONE |
| `ItemScrollWaterShark.java` | B | `SCROLL_WATER_SHARK` (`ModItems.java:297,494`) | DONE |
| `ItemScrollWaterShockwave.java` | B | `SCROLL_WATER_SHOCKWAVE` (`ModItems.java:299,495`) | DONE |
| `ItemScrollPoisonMist.java` | B | `SCROLL_POISON_MIST` (`ModItems.java:280,485`) — wired to Iryo | DONE (binds Iryo, not Suiton) |

---

## Summary

The six Suiton-tome jutsu are all ported with mechanics AND visuals: Hiding in Mist, Water Bullet/Stream,
Water Dragon, Water Prison, Water Shark, Water Shockwave. Renderers/models exist for the three that need
entity models (Stream, Dragon, Shark); the three blockworld/fog effects (Mist, Prison, Shockwave) use
NoopEntityRenderer, matching 1.12.2 which had no renderer for them. All seven learning scrolls are
registered. Poison Mist is implemented but lives in the Iryo element and its faithfulness vs 1.12.2 is
UNVERIFIED (original mechanic source outside the provided files).

Counts (Suiton-tome jutsu only, Poison Mist counted separately): jutsu=6, missing=0, stub=0, partial=0,
done=6. Poison Mist (7th, cross-element) = partial/unverified.

### Top risks
1. Poison Mist's port (`PoisonMistEntity`, `IryoJutsuItem.POISON_MIST`) is UNVERIFIED — the 1.12.2
   mechanic was not in the provided Suiton files; locate the original before sign-off (effect type,
   duration, particle color, sound).
2. No cooldowns are applied in `SuitonItem` (it never calls `setJutsuCooldown`). This matches 1.12.2
   (cooldown lines commented in `ItemSuiton.java:105-106`), but confirm no Suiton jutsu was meant to
   have a non-default cooldown.
3. Particle parity drift: the port adds CLOUD/SPLASH particles to Mist/Prison/Shockwave that 1.12.2
   lacked, and substitutes SPLASH/FALLING_WATER/BUBBLE for legacy WATER_DROP/WATER_WAKE/WATER_SPLASH on
   Stream/Dragon/Shark. Visually close but not byte-identical — confirm acceptable, and verify the
   custom render types (`scrollingTranslucent`, `translucentEmissiveNoCull`) plus required textures
   (`dragon_blue.png`, `gas256.png`, `shark.png`, `water_flow.png`) all resolve at runtime.
```
