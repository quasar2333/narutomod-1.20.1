# Jutsu Backlog — Ranton (Storm) + Bakuton (Explosion / Clay)

Audit of the two element "tome" item classes for this domain, comparing the read-only 1.12.2 source of truth against the in-progress 1.20.1 port. Goal: a developer can restore every technique (mechanics AND visuals) without missing any.

- ORIGINAL base: `D:\mcmodding\naruto\1.12.2\src\main\java\net\narutomod`
- PORT base: `D:\mcmodding\naruto\1.20.1\src\main\java\net\narutomod`
- 1.12.2 tomes audited:
  - `item\ItemRanton.java` (2 jutsu: CLOUD, LASERCIRCUS)
  - `item\ItemBakuton.java` (3 jutsu: JIRAIKEN, CLAY, CLONE)

## Framework & shared notes

**JutsuEnum constructor semantics** (`item\ItemJutsu.java:560-586`). Relevant overloads:
- `JutsuEnum(int idx, String name, char rank, double chakra, callback)` → `requiredXP` derived from rank: **S=400**, A=250, B=200, C=150, D=100 (line 568-572). Used by both Ranton jutsu (so CLOUD and LASERCIRCUS both require **400 XP**).
- `JutsuEnum(int idx, String name, char rank, int xp, double chakra, callback)` → explicit `requiredXP` (line 578). Used by all three Bakuton jutsu (xp = 150 / 200 / 200).
- `chakraUsage` is the per-activation cost (scaled by power for charged jutsu); `rank` drives requiredXP and tooltip rank letter.

**Charging / power model** (`item\ItemJutsu.java:151-158`, `getPower(stack, entity, timeLeft, basePower, powerupDelay)`): power ramps from `basePower` over `(maxUseDuration - timeLeft)/(powerupDelay * modifier)`, clamped to `getMaxPower`. XP-to-power scaling is via `getJutsuXp` / `getCurrentJutsuRequiredXp`.

**1.20.1 port architecture**: All five jutsu are folded into one generic item `item\AdvancedNatureJutsuItem.java` (the `AdvancedNatureKind` enum), keyed by `JutsuType.RANTON` / `JutsuType.BAKUTON`. Each technique is a `JutsuDefinition` (index, translationKey, rank, xp, chakra, optional `.withPower(base, delay)`). Dispatch happens in `releaseUsing(...)` (line 158-245) and `use(...)` (line 112-123). Items registered in `registry\ModItems.java` (`RANTON` line 205, `BAKUTON` line 80). Definitions at `AdvancedNatureJutsuItem.java:1830-1903`.
- Definition parity verified: BAKUTON = jiraiken(idx0,S,xp150,ch30,power0.2/150), c_1(idx1,S,xp200,ch75,power1/150), explosive_clone(idx2,S,xp200,ch150). RANTON = rantoncloud(idx0,S,ch1), laser_circus(idx1,S,ch100,power0.1/50). **All match 1.12.2 exactly.**

**Element requirement gates** (faithful):
- Ranton needs Raiton + Suiton items in inventory. 1.12.2 `ItemRanton.java:84-85`; 1.20.1 `REQ_RAITON | REQ_SUITON` (`AdvancedNatureJutsuItem.java:1898`). Tooltip `tooltip.ranton.musthave`.
- Bakuton needs Doton + Raiton items in inventory. 1.12.2 `ItemBakuton.java:134-135`; 1.20.1 `REQ_DOTON | REQ_RAITON` (`AdvancedNatureJutsuItem.java:1832`). Tooltip `tooltip.bakuton.musthave`.

**Shared entities / registries used by this domain (1.20.1):**
- `registry\ModEntityTypes.java`: `RANTONCLOUD` (line 276), `LASER_CIRCUS` (247), `LASER_RING` (248), `LIGHTNING_ARC` (250), `C_1`/`C_2`/`C_3` (151-153), `EXPLOSIVE_CLONE` (208).
- `registry\ModSounds.java`: `electricity` (158), `lasercircus` (132), `c3` (118), `katsu` (120), `poof` (66), `kagebunshin` (76), `charging_chakra` (144).
- `registry\ModItems.java`: `RANTON`, `BAKUTON`.
- Renderers/models registered in `client\ClientModEvents.java`: C_1/C_2/C_3 → `ExplosiveClayRenderer` (254-256) using `ClayC1Model`/`ClayC2Model`/`ClayC3Model` layers (429-431); `EXPLOSIVE_CLONE` → `ExplosiveCloneRenderer` (323); `LASER_RING` → `LaserRingRenderer` (328); `LIGHTNING_ARC` → `LightningArcRenderer` (331); `LASER_CIRCUS`, `RANTONCLOUD` → `NoopEntityRenderer` (327, 358) — correct, these are invisible 0.01-size controller entities in 1.12.2 too.

**NOTE — out of scope:** `RANTON_KOGA` / `entity\RantonKogaEntity.java` (1.20.1, registered line 275) does **not** originate from the `ItemRanton` tome (which only defines CLOUD and LASERCIRCUS). It belongs to a different/new source and is not part of this audit. Flagged so it is not mistaken for a missing Ranton jutsu.

---

## Ranton: Storm Release Cloud (Rantongumo / "rantoncloud")

| Field | Value |
|---|---|
| Index / id | 0 (`CLOUD`), registry name `rantoncloud` |
| Display name key | `entity.rantoncloud.name` (unlocalizedName `rantoncloud`) |
| Chakra | 1.0/tick continuous drain while active (`CLOUD.chakraUsage = 1d`) |
| Level / XP / rank | Rank **S** → requiredXP **400** (derived) |
| Cooldown | 0 (`defaultCooldownMap[CLOUD.index] = 0`, `ItemRanton.java:69`) |
| Entities spawned | `EntityRaiunkuha` (controller, size 0.01, ENTITYID 278) which spawns `EntityLightningArc.Base` and `EntityLightningArc.spawnAsParticle` arcs |
| Procedures | `ProcedureUtils.objectEntityLookingAt`; `Chakra.pathway(...).consume`; `ItemJutsu.causeJutsuDamage` |
| Particles | `Particles.Types.SMOKE`, color `0xff303030`, ~100 count, around summoner (`ItemRanton.java:136-137`); plus lightning-arc particles |
| Sounds | `narutomod:electricity` (random ~1/20 ticks, pitch 0.3-0.9, vol 0.1) |
| Visual | Invisible controller follows summoner; emits a swirling storm cloud of dark smoke + crackling blue lightning arcs (`0xc00000ff`). Auto-zaps any LivingEntity within 4-block radius and whatever the summoner looks at within 10 blocks. |
| 1.12.2 file:line | `item\ItemRanton.java:43,69,103-190`; arc `entity\EntityLightningArc.java:80-94` |

Mechanics detail: Toggle jutsu. `Jutsu.createJutsu` (`ItemRanton.java:173-189`) stores `RaiunkuhaEntityId` on the stack; re-cast kills the existing cloud (toggle off). `onUpdate` (126-151): every tick consumes 1 chakra or dies; emits smoke + a random lightning particle arc (length 1.2); strikes all nearby living entities and the look-target via `setLightningOn` → blue arc with damage `rand*0.05*jutsuXp`.

**1.20.1 status: DONE.** Port `entity\RantonCloudEntity.java` (full) + dispatch `toggleRantonCloud` (`AdvancedNatureJutsuItem.java:853-870`).
- Toggle via `findActive`/`discard` (853-862). Chakra drain 1.0/tick `CHAKRA_BURN_PER_TICK` (line 29, 82). Smoke particle `SMOKE_COLORED 0xFF303030`, count 100 (137-146). `electricity` sound 1/20 ticks (120-125). Random feedback arc length 1.2 (127-136). Strikes living within 4 blocks (149-154) and 10-block look-target (156-162); arc color `0xC00000FF`, damage `rand*0.05*damageXp` (168-184). DamageXp seeded from `getJutsuXp` at cast (866). Uses `LightningArcEntity` + `LightningArcRenderer`. Renderer = Noop (invisible controller) — matches original. No gaps found.

---

## Ranton: Storm Release Laser Circus (Ranton: Reizā Sākasu / "laser_circus")

| Field | Value |
|---|---|
| Index / id | 1 (`LASERCIRCUS`), registry names `laser_circus` (ENTITYID 279) + `laser_ring` (ENTITYID 280) |
| Display name key | `entity.laser_circus.name` |
| Chakra | 100 × power (`LASERCIRCUS.chakraUsage = 100d`) — charged |
| Level / XP / rank | Rank **S** → requiredXP **400** (derived) |
| Cooldown | 0 (`ItemRanton.java:70`) |
| Power / charge | `getPower(..., 0.1f, 50f)` base 0.1, delay 50 (`ItemRanton.java:76`). Duration = `power*20` ticks (`EntityLaserCircus.java:74`) |
| Entities spawned | `EntityLaserCircus.EC` (invisible emitter) + `EntityLaserCircus.EntityRing` (spinning visible ring) + per-tick `EntityLightningArc.Base` beams |
| Procedures | `ProcedureUtils.objectEntityLookingAt(summoner, 25d)`; `ItemJutsu.causeJutsuDamage` |
| Particles | (none directly; lightning arcs carry the visual) |
| Sounds | `narutomod:lasercircus` on ring spawn (`EntityLaserCircus.java:162`); `narutomod:electricity` every 10 ticks from emitter (97-98) |
| Visual | A glowing rotating/expanding lightning ring (`textures/ring_lightning.png`) anchored in front of the caster, firing rapid blue lightning beams (color `0xc00000ff`, 10 segments, 0.1 amp) at the look-target up to 25 blocks. Ring renderer: additive quad, spins 9°/tick, pulses scale `(1 - age%5/5)*3`, fades in over 30 ticks. |
| 1.12.2 file:line | `item\ItemRanton.java:44,70,73-94`; `entity\EntityLaserCircus.java` (full, esp. EC 60-142, EntityRing 148-204, RenderRing 206-253) |

Mechanics detail: On right-click (`ItemRanton.java:82-94`) the ring is spawned immediately (`EntityRing`, stored as `LaserCircusRingEntityId` on stack) while the item is held/charged; on release `EC.Jutsu.createJutsu` (135-141) spawns the emitter with `duration = power*20`. `EC.onUpdate` (91-113): re-anchors in front of caster, plays electricity every 10 ticks, ray-traces look-target to 25 blocks and fires a 10-segment beam (`setLightningAt`, 115-120); on expiry kills the linked ring. `EntityRing` simply follows and is the only visible part. `ringSpawned` guard prevents duplicates (144-146).

**1.20.1 status: DONE.** Ports: emitter `entity\LaserCircusEntity.java`, ring `entity\LaserRingEntity.java`, renderer `client\renderer\LaserRingRenderer.java`; dispatch `activateLaserCircus` (`AdvancedNatureJutsuItem.java:872-888`) + ring pre-spawn in `use(...)` (119-121) via `LaserRingEntity.spawnOrGet`.
- Duration `power*20` (`LaserCircusEntity.java:42`). Beam: `objectEntityLookingAt(owner, 25)`, arc color `0xC00000FF`, 10 segments, 0.1 amp (132-143). `electricity` every 10 ticks (86-89). Damage `rand*0.05*damageXp` (138-141). Emitter kills linked ring on expiry (`discardWithRing` 155-163). Ring: `lasercircus` sound on spawn (57-58), follows caster, persists while LaserCircus active or item still channeling laser_circus index (122-133). Renderer faithfully reproduces the 1.12.2 GL ring (translucent emissive no-cull quad, 9°/tick Z-spin, `(1-age%5/5)*3` scale pulse, alpha fade over 30 ticks, `ring_lightning.png`) — `LaserRingRenderer.java:28-66`. No gaps found.

---

## Bakuton: Jiraiken (Landmine Fist / "tooltip.bakuton.jiraiken")

| Field | Value |
|---|---|
| Index / id | 0 (`JIRAIKEN`) — no spawned entity |
| Display name key | `tooltip.bakuton.jiraiken` |
| Chakra | 30 × power (`JIRAIKEN`, chakra 30d) — charged |
| Level / XP / rank | Rank **S**, requiredXP **150** (explicit) |
| Cooldown | 0 (`ItemBakuton.java:99`) |
| Power / charge | `getPower(..., 0.2f, 150f)` base 0.2, delay 150 (`ItemBakuton.java:107`) |
| Entities spawned | none |
| Procedures | `PotionChakraEnhancedStrength` potion application; `ProcedureUtils.objectEntityLookingAt` (left-click retarget) |
| Particles | none |
| Sounds | none directly |
| Visual | No model/entity. A self-buff "explosive fist" toggle: while active, applies `PotionChakraEnhancedStrength` at amplifier `power*19` each tick — punches detonate. (Visual is the potion/strength behaviour + vanilla melee.) |
| 1.12.2 file:line | `item\ItemBakuton.java:63,99,141-159,169-194`; potion `potion\PotionChakraEnhancedStrength.java` |

Mechanics detail: Toggle buff. `Jiraiken.createJutsu` (169-183) flips `isJiraikenActivated` + stores `JiraikenPower` on the stack (returns true only if not already strength-buffed). `onUpdate` (141-148) re-applies `PotionChakraEnhancedStrength` (duration 2, amp `power*19`) while activated. `onLeftClickEntity` (150-159) sets revenge target (or look-target within 50 blocks if self-clicked).

**1.20.1 status: DONE.** Dispatch `toggleJiraiken` (`AdvancedNatureJutsuItem.java:489-511`) + `inventoryTick` re-apply (273-280) + `onLeftClickEntity` retarget (248-263).
- Toggle on/off with on-end effect removal (490-494). Power via `getAdvancedNaturePower(0.2,150)`. Chakra `30*power` (504). Stores `isJiraikenActivated`/`JiraikenPower` tags (1737-1750). Re-applies `ModEffects.CHAKRA_ENHANCED_STRENGTH` amplifier `power*19` each inventory tick (278-279). `canBeginAdvancedNature` lets you re-cast to toggle off even when buffed (326-328). Left-click retargets to look-target within 50 blocks when self-clicked and sets `setLastHurtMob` (251-262). No gaps found.

---

## Bakuton: Explosive Clay — C1 / C2 / C3 ("c_1")

This single jutsu index spawns one of three creature tiers selected by charge power (`ItemBakuton.ExplosiveClay.Jutsu.createJutsu`, `ItemBakuton.java:292-312`): power<2 → C1, power<3 → C2, power<4 → C3, else fail. Max power clamped to 3.1 (`getMaxPower`, `ItemBakuton.java:114-120`); on-using HUD shows "C-N" (122-130).

| Field | Value |
|---|---|
| Index / id | 1 (`CLAY`); entities `c_1` (231), `c_2` (233), `c_3` (235) |
| Display name key | `entity.c_1.name` (jutsu key `c_1`) |
| Chakra | 75 × power (`CLAY`, chakra 75d) — charged |
| Level / XP / rank | Rank **S**, requiredXP **200** (explicit) |
| Cooldown | 0 (`ItemBakuton.java:100`) |
| Power / charge | `getPower(..., 1f, 150f)` floored; max 3.1 (`ItemBakuton.java:108-119`) |
| Entities spawned | `EntityC1.EC`, `EntityC2.EC`, `EntityC3.EC` (all extend `ItemBakuton.ExplosiveClay` except C3 which is a standalone `EntityLiving`) |
| Procedures | `ProcedureAoeCommand` (C3 AoE damage); `EventSphericalExplosion` (C3); Forge `newExplosion`/mob-griefing |
| Particles | C3: `EntitySpecialEffect ROTATING_LINES_COLOR_END` (0xFFFF00); detonations: vanilla explosion |
| Sounds | C3: `narutomod:c3` (charge) + `narutomod:katsu` (arm/fuse) |
| Visual | C1 = small "vex"-like clay bird, biped model w/ flapping wings, `textures/vex1.png`, scale 0.4. C2 = large rideable phantom-shaped clay bird, `textures/phantom1.png`, scale 3, animated wings/tail. C3 = giant clay owl/figure that grows 8× over 30 ticks then drops, `textures/c3.png`, with yellow rotating-line aura before a 30-radius spherical explosion. |
| 1.12.2 file:line | `item\ItemBakuton.java:64,100,104-130,196-372`; `entity\EntityC1.java` (full), `EntityC2.java` (full), `EntityC3.java` (full) |

Mechanics detail (per tier):
- **Base `ExplosiveClay`** (`ItemBakuton.java:196-372`): flying `EntityCreature`, immune to fire/explosion/fall, no-gravity, lifeSpan 600 ticks, `AIChargeAttack` (charges target and `attackEntityAsMob`), `AICopyOwnerTarget` (mirrors owner's target/revenge), `EntityAIHurtByTarget`, flying navigator. No drops, silent.
- **C1** (`EntityC1.java`): HP 14, flying speed 0.4, size 0.4×0.8; on contact `newExplosion` radius 4 then dies. Biped+wings model, scale 0.4.
- **C2** (`EntityC2.java`): HP 20, speed 0.4, size 2.0×1.2; **rideable** (up to 2 passengers, `processInteract` mounts + extends life to 10000), custom `travel` flight controls, contact `newExplosion` radius 10; unridden life capped to 400. Phantom model scale 3, translate (0,1.3125,0.1875).
- **C3** (`EntityC3.java`): standalone `EntityLiving`, HP 20, growTime 30, fuseTime 100; grows (render scale to 8×), at growTime plays `c3` + spawns `ROTATING_LINES_COLOR_END` 0xFFFF00 aura, at fuseTime drops (gravity on) + plays `katsu`, on ground triggers `EventSphericalExplosion(radius 30)` + `ProcedureAoeCommand` 30 damage in 30 radius, then dies. Custom owl-ish model with animated wing-spread tied to fuse progress.

**1.20.1 status: DONE.** All three tiers folded into `entity\ExplosiveClayEntity.java` (full, `ClayTier` enum C1/C2/C3) + dispatch `activateExplosiveClay` (`AdvancedNatureJutsuItem.java:531-548`) + clay-target left-click (251-262). Renderer `client\renderer\ExplosiveClayRenderer.java` + models `ClayC1Model`/`ClayC2Model`/`ClayC3Model`.
- Tier select by floored power, clamp 3.1 (`activateExplosiveClay` 536-537; `ClayTier.fromPower` 514-517). Chakra `75*power` (541). HUD "Explosive clay C-N" (546). Base: PathfinderMob, fire/explosion/fall immune (315-326), no-gravity flight, lifeSpan 600 (43), owner-target copy (`findOwnerTarget` 202-218), charge-and-detonate (`tickFlyingClay` 153-175).
- C1: HP 14, speed 0.4, explosion 4 (`ClayTier.C1` 496); C2: HP 20, explosion 10, **rideable** (`mobInteract` mounts + life 10000, `positionRider` 2-seat offsets, `tickControlled` flight, unridden life→400) (335-374, 155-157); C3: HP 20, grow scale 8× over 30 ticks (`getRenderScale` 387-392), `c3` sound + END_ROD aura at growTime (182-190), `katsu` + gravity at fuseTime (191-196), on-ground 30-radius explosion + 30 dmg AoE + EXPLOSION_EMITTER (`detonateC3` 288-303). Textures vex1/phantom1/c3 with per-tier scale/translate matching 1.12.2 (`ExplosiveClayRenderer.java:60-93`).
- Minor fidelity notes (cosmetic, not blocking): C3 aura uses vanilla `END_ROD` particles instead of `EntitySpecialEffect ROTATING_LINES_COLOR_END 0xFFFF00`; C3 AoE uses `serverLevel.explode` + manual hurt loop instead of `EventSphericalExplosion`/`ProcedureAoeCommand`. Behaviourally equivalent; only the exact particle flavour differs.

---

## Bakuton: Explosive Clone (Bunshin Daibakuha / "explosive_clone")

| Field | Value |
|---|---|
| Index / id | 2 (`CLONE`); entity `explosive_clone` (ENTITYID 239) |
| Display name key | `entity.explosive_clone.name` (key `explosive_clone`) |
| Chakra | 150 (`CLONE`, chakra 150d) — flat |
| Level / XP / rank | Rank **S**, requiredXP **200** (explicit) |
| Cooldown | 0 (`ItemBakuton.java:101`) |
| Entities spawned | `EntityExplosiveClone.EC` (extends `EntityClone.Base`) |
| Procedures | `ProcedureUtils.getModifiedSpeed`; Forge `createExplosion`/mob-griefing |
| Particles | `EXPLOSION_NORMAL` ×200 on poof (`EntityExplosiveClone.java:177-181`) |
| Sounds | `narutomod:kagebunshin` on spawn; `ENTITY_CREEPER_PRIMED` + `narutomod:katsu` on ignite; `narutomod:poof` on death |
| Visual | A shadow-clone copy of the caster (uses `EntityClone` clone model/renderer). On ignition it swells/pulses (scale animation in `CustomRender.preRenderCallback`) and flashes white before exploding (radius 8). Poofs into a burst of explosion particles. |
| 1.12.2 file:line | `item\ItemBakuton.java:65,101`; `entity\EntityExplosiveClone.java` (full) |

Mechanics detail: `EC.Jutsu.createJutsu` (`EntityExplosiveClone.java:183-198`): non-sneak → play `kagebunshin`, spawn one clone; **sneak → detonate/remove all existing explosive clones**. Clone has movement speed `getModifiedSpeed*3.5`, follows summoner (`AIFollowSummoner`), is invulnerable once ignited. `attackEntityAsMob` → `ignite()`. After ignite: at +1 tick plays creeper-primed + `katsu`; after `fuse=30` ticks `explode()` (radius 8) and dies, `poof()` emits 200 explosion particles + `poof` sound.

**1.20.1 status: DONE.** Port `entity\ExplosiveCloneEntity.java` (full) + dispatch `activateExplosiveClone` (`AdvancedNatureJutsuItem.java:513-529`).
- Sneak removes all (`removeAllFor`, 92-99; dispatch 514-518); non-sneak spawns + plays `kagebunshin` (74-90). Speed `getModifiedSpeed*3.5` clamped ≥0.6 (135). Copies owner equipment/attributes/name/handedness (113-140) — richer than 1.12.2 but consistent with the clone framework. Fire/explosion/fall + ignited immunity (233-244). `doHurtTarget` ignites (227-230). Fuse 30 (45): on ignite+1 plays `CREEPER_PRIMED` + `katsu` (289-293); after fuse `explode()` radius 8 (295-308). `poof()` = `poof` sound + 160 POOF particles (334-341). Owner self-hit dismisses the clone (239-242). Persisted clone-id tracking via `ExplosiveCloneEntity.ID_KEY`. Renderer `ExplosiveCloneRenderer` with ignition swell/white-flash via `getIgnitionProgress` (274-279) matching 1.12.2 `CustomRender`. 
- Minor fidelity note (cosmetic): poof uses vanilla `ParticleTypes.POOF` ×160 vs 1.12.2 `EXPLOSION_NORMAL` ×200. Not blocking.

---

## Summary

| Jutsu | 1.12.2 source | 1.20.1 status |
|---|---|---|
| Ranton Cloud (rantoncloud) | ItemRanton.java:43 | DONE |
| Ranton Laser Circus (laser_circus) | ItemRanton.java:44 | DONE |
| Bakuton Jiraiken | ItemBakuton.java:63 | DONE |
| Bakuton Explosive Clay C1/C2/C3 | ItemBakuton.java:64 | DONE |
| Bakuton Explosive Clone | ItemBakuton.java:65 | DONE |

All 5 jutsu (2 Ranton + 3 Bakuton) are fully ported with faithful mechanics, definitions (index/chakra/xp/rank/power/cooldown), requirement gates, entities, renderers, models, sounds and particles. No MISSING/STUB/PARTIAL items in this domain. The only deltas are two cosmetic particle substitutions (C3 aura: END_ROD instead of ROTATING_LINES_COLOR_END 0xFFFF00; Explosive Clone poof: POOF instead of EXPLOSION_NORMAL) and C3's AoE being implemented directly rather than via `EventSphericalExplosion`/`ProcedureAoeCommand` — all behaviourally equivalent and non-blocking.

`RANTON_KOGA`/`RantonKogaEntity` exists in the port but is NOT from this tome and is out of scope.
