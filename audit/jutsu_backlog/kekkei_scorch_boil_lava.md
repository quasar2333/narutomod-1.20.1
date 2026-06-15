# Jutsu Backlog — Shakuton (Scorch) + Futton (Boil) + Yooton/Yoton (Lava / Yang)

Audit of the four "Item* tomes" `ItemShakuton`, `ItemFutton`, `ItemYooton`, `ItemYoton`
(1.12.2 Forge) versus the in-progress 1.20.1 Forge port.

> **Headline finding:** This whole domain is **already ported and wired** — far better
> than the prompt's "(none yet — likely entirely unported)" assumption. All four legacy
> `Item*` classes were collapsed into a single data-driven item `AdvancedNatureJutsuItem`
> (one item per `AdvancedNatureKind`), the spawned entities were rewritten as modern
> `net.minecraft.world.entity.Entity` subclasses, and renderers/models/layers are
> registered. The remaining work is verification + a few faithfulness deltas noted per
> jutsu below, not from-scratch porting.

---

## Framework / how this domain is wired in 1.20.1

**Legacy (1.12.2) shape.** Each element was an `Item*` extending `ItemJutsu.Base`. Each jutsu
was an `ItemJutsu.JutsuEnum` constant `(index, nameKey, rankChar [, requiredXp], chakra, IJutsuCallback)`.
The callback (`IJutsuCallback.createJutsu`) either spawned an entity or ran a procedure. Spawned
entities were either `EntityScalableProjectile.Base` projectiles (custom GL billboard render) or
plain `Entity` aura helpers (size 0.01, particle-only). Rendering was registered in
`preInit` via `RenderingRegistry.registerEntityRenderingHandler`.

**Port (1.20.1) shape.**
- **Item / jutsu table:** `net.narutomod.item.AdvancedNatureJutsuItem` (extends
  `JutsuItem`). One instance per `AdvancedNatureJutsuItem.AdvancedNatureKind` enum value.
  Relevant kinds: `SHAKUTON`, `FUTTON`, `YOOTON`, `YOTON` (also FUTON-wind is a *different*
  item `FutonItem` — do not confuse `FUTON`/wind with `FUTTON`/boil).
- **Jutsu definitions:** held in the `AdvancedNatureKind` enum constructor as
  `JutsuDefinition[]` built with `definition(idx,key,rank,reqXp,chakra)` (fixed XP) or
  `ranked(idx,key,rank,chakra)` (rank-curve XP), each optionally `.withPower(base, powerUpDelay)`.
- **Registration:** items in `registry/ModItems.java` (`SHAKUTON`/`FUTTON`/`YOOTON`/`YOTON`),
  entities in `registry/ModEntityTypes.java`, renderers/layers/model-layers in
  `client/ClientModEvents.java`.
- **Dispatch:** `AdvancedNatureJutsuItem.releaseUsing(...)` is a giant `if (isXxx(def)) activateXxx(...)`
  chain. Each `isXxx(def)` matcher checks `kind == … && index == … && translationKey == …`.
  Each `activateXxx` re-checks `canActivateAdvancedNature` (learned? enough jutsu-xp? cooldown?
  chakra?), consumes chakra `= chakraUsage * power`, spawns the entity, and awards 1 jutsu-xp.
- **Charge / power:** `use()` starts a 72000-tick BOW-anim charge; `getAdvancedNaturePower`
  uses `getChargingPower(stack, player, remaining, basePower, powerUpDelay)` then caps to
  available chakra. While charging, `onUseTick` emits colored smoke + `SOUND_CHARGING_CHAKRA`.
- **Damage:** legacy `ItemJutsu.NINJUTSU_DAMAGE` → `ModDamageTypes.ninjutsu(...)` /
  `ModDamageTypes.ninjutsuFire(...)`.
- **Particles:** legacy `Particles.spawnParticle(world, Types.SMOKE, …, ARGB, …)` →
  `serverLevel.sendParticles(ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, argb,
  life, …, ownerId, …), …)`. Lava uses vanilla `ParticleTypes.LAVA`.
- **Owner tracking:** every aura/projectile stores owner by entity-id (synched) + UUID
  (NBT) with a `getOwner()` that re-resolves; legacy used `shootingEntity` / a `user` field.

**Cross-element requirements (gating, ported in `AdvancedNatureKind.hasRequirements`):**
- Shakuton requires Futon(wind) **+** Katon in inventory (`REQ_FUTON | REQ_KATON`). *(NOTE: legacy
  `ItemShakuton.onItemRightClick` checked `ItemFuton.block` (wind) + `ItemKaton.block` — matches.)*
- Futton(boil) requires Katon **+** Suiton (`REQ_KATON | REQ_SUITON`). Matches legacy.
- Yooton requires Doton **+** Katon (`REQ_DOTON | REQ_KATON`). Matches legacy.
- Yoton has **no** element requirement (flags `0`). Matches legacy (`ItemYoton` had no `onItemRightClick` gate).

**Passives (ported in `AdvancedNatureKind.applyPassive` / `inventoryTick`):**
- Yooton passive `clearFire()` each tick (legacy `RangedItem.onUpdate` `extinguish()`), **plus**
  a Forge `LivingAttackEvent` handler (`AdvancedNatureJutsuItem.onLivingAttack`) cancels fire/lava/hot-floor
  damage to players holding YOOTON — this is the legacy `ItemYooton.RangedItem.DamageHook`.
- Shakuton: holding-check clears stored Scorch-Orb ids when the Shakuton item leaves both hands
  (legacy `RangedItem.onUpdate` `clearBalls`).

**Shared/base classes the legacy jutsu needed (for reference):**
`EntityScalableProjectile.Base` (ScorchBall, MagmaBall, Melting droplet), plain `Entity`
(BoilingMist, UnrivaledStrength.EC, LavaChakraMode.EC), `EntityClone.Base` (BiggerMe),
`EntitySealing.EC` (Sealing), `ProcedureAirPunch` (BoilingMist cone), `ProcedureAoeCommand`
(ScorchBall blast), `EventSphericalExplosion`/`EventSetBlocks` (→ `SpecialEvent`),
`PotionCorrosion` (→ `ModEffects.CORROSION`), `ItemSteamArmor` (→ `ModItems.STEAM_ARMOR*`),
`EntityBijuManager.getTails` (→ `BijuManager.isJinchurikiOf` / `JINCHURIKI_TAILS` var).

---

# SHAKUTON — 灼遁 (Scorch Release) — item `narutomod:shakuton`
Legacy file: `1.12.2/.../item/ItemShakuton.java`. Three jutsu, all `rank 'S'`.
Port item: `ModItems.SHAKUTON` → `AdvancedNatureJutsuItem(SHAKUTON)`.
Shared spawned entity for all three: **`EntityScorchBall`** (port `ScorchOrbEntity`).

## Scorch Orb — "scorchorb" (灼遁・スコーチオーブ)
| Field | Value |
|---|---|
| index / nameKey | 0 / `scorchorb` |
| chakra | 100.0 |
| rank / XP-to-learn | 'S' / requiredXp **150** |
| cooldown | 0 (`defaultCooldownMap[ORB]=0`) |
| entity spawned | `EntityScorchBall` (`EntityScalableProjectile.Base`), tracker(64,1) id 269 |
| procedure / callback | `EntityScorchBall.Jutsu.createJutsu` — caps at **20** live orbs; saves orb id into stack NBT `SpawnedBallsId` |
| particles | idle: SMOKE color `0x40FF4E83`, ~`width*25` count, brightness `0xF0`; scorch-hit/extinguish: SMOKE `0x40FFFFFF` |
| sounds | `BLOCK_FIRE_EXTINGUISH` on scorch contact/impact |
| visual | small spinning fire **billboard** quad, `textures/fireball2.png`, scale starts 0.5; orbits the user's head (`Vec3.fromPitchYaw(0, ticks*9)+eyeHeight`); rotates `9°*ticksExisted` |
| behavior | orbits caster; touching a living entity → 1 dmg + scorch FX; dies if leaves water/owner unholsters; on ground → scorch FX, drops target |
| 1.12.2 file:line | `ItemShakuton.java:57` (enum), `:185-358` (entity+Jutsu), `:375-414` (render) |

**1.20.1 status: DONE.**
- Item def: `AdvancedNatureJutsuItem.java:1910` `definition(0,"scorchorb",'S',150,100.0).withPower(1,1)`.
- Spawn/dispatch: `activateScorchOrb` (`:890`), `saveScorchBall`/`countActiveScorchBalls`/`getActiveScorchBalls`
  (`:1686-1735`) replicate the legacy 20-orb cap + NBT `SpawnedBallsId` int-array bookkeeping.
- Entity: `entity/ScorchOrbEntity.java` — full faithful rewrite (orbit via `getIdlePosition`,
  grow-and-shoot, target chase 60-tick, idle/scorch particle ARGB + `FIRE_EXTINGUISH`,
  spherical-explosion on impact). Owner-holding check `isOwnerHoldingShakuton`.
- Render: `client/renderer/ScorchOrbRenderer.java` — camera-facing billboard quad,
  `fireball2.png`, `Z` spin `9*(tick+pt)`, `entityCutoutNoCull`, full-bright. Faithful.
- Wiring: `ModEntityTypes.SCORCHORB` (id "scorchorb"), `ClientModEvents:365`.
- *Minor delta:* legacy ORB JutsuEnum already had `requiredXp=150`; port matches. Verify the
  `EventSphericalExplosion` damage radius (`maxScale*60`) matches under the new `SpecialEvent` impl.

## Scorch Kill — "tooltip.shakuton.scorchkill" (Steam Imperial Funeral / target lock-and-burst)
| Field | Value |
|---|---|
| index / nameKey | 1 / `tooltip.shakuton.scorchkill` |
| chakra | 50.0 |
| rank / XP-to-learn | 'S' / legacy: **none** (2-arg-style enum). Port: requiredXp **200** |
| cooldown | 0 |
| entity spawned | none new — retargets an existing `EntityScorchBall` |
| procedure / callback | `SetOrbTarget.createJutsu` — `get1stBallAndPutLast`, raytrace `objectEntityLookingAt(30)`, `orb.setTarget(hit)` |
| particles / sounds | none of its own (orb handles its FX) |
| visual | makes the oldest orbiting orb home onto the looked-at entity for 60 ticks |
| 1.12.2 file:line | `ItemShakuton.java:58` (enum), `:170-183` (SetOrbTarget) |

**1.20.1 status: DONE (with an XP-curve delta — verify intent).**
- Item def: `AdvancedNatureJutsuItem.java:1911` `definition(1,"tooltip.shakuton.scorchkill",'S',200,50.0)`.
- Dispatch: `isShakutonScorchKill` (`:1468`), `activateScorchKill` (`:909`): `getFirstScorchBallAndRotate`,
  requires `EntityHitResult` from `objectEntityLookingAt(30)`, `orb.setTarget(...)`. Faithful.
- **DELTA:** Legacy `ItemShakuton.SHOOT`/`BLAST` used the *4-param* `JutsuEnum(index,name,rank,chakra,cb)`
  constructor (**no `requiredXp` → learn-free / rank-only**). The port assigns `requiredXp=200`
  (kill) / `250` (blast). If exact legacy unlock economy matters, these two should arguably be
  `ranked(...)` not `definition(...,reqXp,...)`. Functionally minor; flag for design sign-off.

## Scorch Blast — "tooltip.shakuton.scorchblast" (Super Steam Kill / detonate all orbs)
| Field | Value |
|---|---|
| index / nameKey | 2 / `tooltip.shakuton.scorchblast` |
| chakra | 50.0 |
| rank / XP-to-learn | 'S' / legacy: none (4-param enum). Port: requiredXp **250** |
| cooldown | 0 |
| entity spawned | none new — grows + detonates existing orbs |
| procedure / callback | `SuperSteamBlast.createJutsu` — first orb `setMaxScale(0.5*count)` (grows huge then shoots+explodes), the rest `setMaxScale(0)` (fizzle); `clearBalls` |
| particles / sounds | from the orb: `EventSphericalExplosion`, `BLOCK_FIRE_EXTINGUISH`, big SMOKE |
| visual | one orb swells to `0.5*N`, flies forward on the look vector, then spherical explosion + AoE damage (`maxScale*60`) |
| 1.12.2 file:line | `ItemShakuton.java:59` (enum), `:360-373` (SuperSteamBlast), `:307-322` (onImpact) |

**1.20.1 status: DONE (same XP-curve delta as Scorch Kill).**
- Item def: `AdvancedNatureJutsuItem.java:1912` `definition(2,...,'S',250,50.0)`.
- Dispatch: `isShakutonScorchBlast` (`:1474`), `activateScorchBlast` (`:931`): grows first orb to
  `0.5*size`, zeros the rest, `clearScorchBalls`. Matches legacy exactly.
- Explosion path lives in `ScorchOrbEntity.impact` (`moveGrowAndShoot`→`shoot`→`travelAndImpact`→`impact`
  → `SpecialEvent.setSphericalExplosionEvent` + AoE `scale*60`). Faithful.

---

# FUTTON — 沸遁 (Boil Release) — item `narutomod:futton`
Legacy file: `1.12.2/.../item/ItemFutton.java`. Two jutsu, both `rank 'S'`.
Port item: `ModItems.FUTTON` → `AdvancedNatureJutsuItem(FUTTON)`.
Power model: charge-scaled, `getPower(... 0.1f, 20f)`.

## Boiling Mist — "futton_mist" (沸遁・霧)
| Field | Value |
|---|---|
| index / nameKey | 0 / `futton_mist` |
| chakra | 50.0 (× power) |
| rank / XP-to-learn | 'S' / `ranked` (rank-curve XP) |
| power | base 0.1, powerUpDelay 20 |
| cooldown | 0 |
| entity spawned | `EntityBoilingMist` (plain `Entity`, size 0.01), tracker(64,1) id 281 |
| procedure / callback | `EntityBoilingMist.Jutsu` → `AirPunch extends ProcedureAirPunch` (cone, `blockHardnessLimit=1`, `getBreakChance=0`) |
| effects | living in cone: `BLOCK_FIRE_EXTINGUISH` + **Corrosion** potion (200t, amp 15); blocks: ICE/PACKED_ICE→WATER (via `EventSetBlocks`, 100t), FIRE (not Amaterasu)→air |
| particles | 50× SMOKE `0x20FFFFFF`, life 80-100, spread by `getFarRadius*0.15` along look |
| sounds | `BLOCK_FIRE_EXTINGUISH` per affected entity/block |
| visual | invisible emitter; a forward steam **cone** of corrosive mist |
| life | `ticksExisted > power*power*0.5` → dead |
| 1.12.2 file:line | `ItemFutton.java:58` (enum), `:114-213` (entity + AirPunch + Jutsu) |

**1.20.1 status: DONE.**
- Item def: `AdvancedNatureJutsuItem.java:1845` `ranked(0,"futton_mist",'S',50.0).withPower(0.1,20)`.
- Dispatch: `isFuttonMist` (`:1402`), `activateFuttonMist` (`:550`) → `FuttonMistEntity.spawnFrom`.
- Entity: `entity/FuttonMistEntity.java` — re-implements the `ProcedureAirPunch` cone inline
  (`collectBlocksInCone`/`blockInsideCone`/`corrodeEntities`), applies `ModEffects.CORROSION` (200t/amp15),
  ICE/PACKED_ICE→WATER via `SpecialEvent.setBlocksEvent`, fire→air (skips `ModBlocks.AMATERASUBLOCK`),
  `FIRE_EXTINGUISH` sounds, 50 SMOKE `0x20FFFFFF`, `maxLife = power*power*0.5`. Faithful.
- Render: `NoopEntityRenderer` (`ClientModEvents:291`) — correct; legacy entity was invisible
  (size 0.01, particle-only). No model needed.
- Wiring: `ModEntityTypes.FUTTON_MIST` (id "futton_mist").

## Unrivaled Strength — "unrivaled_strength" (Steam-armor / Tsunade-style buff burst)
| Field | Value |
|---|---|
| index / nameKey | 1 / `unrivaled_strength` |
| chakra | 50.0 (× power) |
| rank / XP-to-learn | 'S' / `ranked` |
| power | base 0.1, powerUpDelay 20 |
| cooldown | 0 |
| entity spawned | `EntityUnrivaledStrength.EC` (plain `Entity`, size 0.01, fire-immune), tracker(64,3) id 286 |
| procedure / callback | `EС.Jutsu` → spawn EC(power) |
| mechanics | if wearing **full Steam Armor** → power×2; duration `power*20`; grants STRENGTH `(int)power`, SPEED `power*2`, JUMP `power*0.5` (all stacking over current amp); on swing → `pushEntity(look target ≤5, 20d, 1.5)`; AoE `HOT_FLOOR` 1 dmg in r=7 (first 10t) then r=4 |
| particles | non-armor: 50 then 20 SMOKE `0x20FFFFFF` rising; armor: 10 SMOKE behind back at y+1.4 |
| sounds | spawn: `narutomod:kairikimuso`; loop: `BLOCK_FIRE_EXTINGUISH` every 5t |
| visual | invisible emitter; rising steam aura + sound; visible only as particles/buffs |
| 1.12.2 file:line | `entity/EntityUnrivaledStrength.java:44-142` (entity+Jutsu). Enum: `ItemFutton.java:59` |

**1.20.1 status: DONE.**
- Item def: `AdvancedNatureJutsuItem.java:1846` `ranked(1,"unrivaled_strength",'S',50.0).withPower(0.1,20)`.
- Dispatch: `isFuttonUnrivaledStrength` (`:1408`), `activateUnrivaledStrength` (`:567`) → `UnrivaledStrengthEntity.spawnFrom`.
- Entity: `entity/UnrivaledStrengthEntity.java` — Steam-armor ×2 (`isWearingSteamArmor` via
  `ModItems.STEAM_ARMOR{HELMET,BODY,LEGS}`), stacking DAMAGE_BOOST/MOVEMENT_SPEED/JUMP via
  `addStackingEffect`, `pushLookTarget` (range 5, 20d/1.5), `hurtNearby` hotFloor r7→r4,
  aura + steam-armor smoke variants, `SOUND_KAIRIKIMUSO` spawn + `FIRE_EXTINGUISH` loop. Faithful.
- Render: `NoopEntityRenderer` (`ClientModEvents:401`) — correct (invisible legacy entity).
- Wiring: `ModEntityTypes.UNRIVALED_STRENGTH`.

> NOTE: `EntityUnrivaledStrength` was referenced *both* as a Futton jutsu and (in legacy)
> as an entity tome. No JutsuScroll exists for it.

---

# YOOTON — 熔遁 (Lava Release) — item `narutomod:yooton`
Legacy file: `1.12.2/.../item/ItemYooton.java`. Three jutsu, all `rank 'S'`.
Port item: `ModItems.YOOTON` → `AdvancedNatureJutsuItem(YOOTON)`.
Passive: `clearFire()` + fire/lava damage immunity while item in inventory.

## Magma Ball — "magmaball" (熔遁・溶岩弾 / Rocks)
| Field | Value |
|---|---|
| index / nameKey | 0 / `magmaball` |
| chakra | 30.0 (× power) |
| rank / XP-to-learn | 'S' / requiredXp **200** |
| power | base 1, powerUpDelay 15 |
| cooldown | 0 (`defaultCooldownMap[ROCKS]=0`) |
| entity spawned | `EntityMagmaBall` (`EntityScalableProjectile.Base`), tracker(64,1) id 270 |
| procedure / callback | `EntityMagmaBall.Jutsu` → spawn at look vec, `shoot(look,0.95,0)` |
| mechanics | `explosionSize = max(scale-1,0)`; `damage = scale*0.1*(playerLevel or 5)`; on hit: fire-ninjutsu dmg + `setFire(10)`, vanilla explosion (mobGriefing-gated); sets owner `InvulnerableTime=40`; dies after 100t in air or in water |
| particles | lava-ish SMOKE `0x80F50000|rand`, `scale*10` count, brightness `0xF0` |
| sounds | `BLOCK_FIRE_EXTINGUISH` 20% per tick |
| visual | spinning **billboard** `textures/magmaball.png`, scale-sized, `Z` spin `9*ticks` |
| 1.12.2 file:line | `ItemYooton.java:59` (enum), `:145-218` (entity+Jutsu), `:220-259` (render) |

**1.20.1 status: DONE.**
- Item def: `AdvancedNatureJutsuItem.java:1931` `definition(0,"magmaball",'S',200,30.0).withPower(1,15)`.
- Dispatch: `isMagmaBall` (`:1366`), `activateMagmaBall` (`:416`) → `ModEntityTypes.MAGMABALL.create`,
  `magmaBall.configure(player,power)`, `SOUND_FLAMETHROW` (legacy had no spawn sound — *added* flavor).
- Entity: `entity/MagmaBallEntity.java` — `explosionSize=max(scale-1,0)`, `damage=scale*0.1*expLevel|5`,
  `ninjutsuFire` + 10s fire, `serverLevel.explode`, owner `INVULNERABLE_TIME=40`, lava SMOKE
  `0x80F50000|rand<<8`, `FIRE_EXTINGUISH` 20%, 100t/water death. Faithful.
- Render: `client/renderer/MagmaBallRenderer.java` — billboard quad, `magmaball.png`, `Z` spin. Faithful.
- Wiring: `ModEntityTypes.MAGMABALL`, `ClientModEvents:333`.

## Melting Apparition (Stream) — "melting_jutsu" (熔遁・溶解の術)
| Field | Value |
|---|---|
| index / nameKey | 1 / `melting_jutsu` |
| chakra | 30.0 (× power) |
| rank / XP-to-learn | 'S' / requiredXp **200** |
| power | base 1, powerUpDelay 30 |
| cooldown | 0 (`defaultCooldownMap[STREAM]=0`) |
| entity spawned | `EntityMeltingJutsu.EC` (`EntityScalableProjectile.Base`, OG 0.25), tracker(64,3) id 272 |
| procedure / callback | `EntityMeltingJutsu.EC.Jutsu` → spawn emitter EC(power) |
| mechanics | emitter `duration=power*20`; each emit-tick spawns **10** droplet EC's `shoot(look,0.85,0.1)`; droplet grows to `0.5+3.5*airTicks/20`; on block/entity impact → place LAVA at hit cell, then ~120-200t "death" phase that **drags entities down** (`multiplyVelocity 0.4`, motionY-=0.04); after death → solidify the lava column to OBSIDIAN downward |
| particles | none direct (visual is the lava model) |
| sounds | `narutomod:movement` at a random air-tick |
| visual | scrolling lava **cube model** (`ModelBlock`, 4×4×4), `textures/lava.png`, V-scroll via texmatrix; shadow 0.3 |
| 1.12.2 file:line | `entity/EntityMeltingJutsu.java:57-192` (entity+model+render). Enum: `ItemYooton.java:60` |

**1.20.1 status: DONE.**
- Item def: `AdvancedNatureJutsuItem.java:1932` `definition(1,"melting_jutsu",'S',200,30.0).withPower(1,30)`.
- Dispatch: `isMeltingJutsu` (`:1372`), `activateMeltingJutsu` (`:439`) → `MELTING_JUTSU.create`,
  `configureEmitter(player,power)`, `SOUND_FLAMETHROW` (added flavor).
- Entity: `entity/MeltingJutsuEntity.java` — emitter/droplet split (`EMISSION_TICKS`), 10 droplets/tick
  `shootWithInaccuracy(look,0.85,0.1)`, grow `0.5+3.5*air/20`, impact→LAVA cell, `startDeath`
  (120-200t) drag-down (`multiplyVelocity 0.4`, +(-0.04) Y), `solidifyLava`→OBSIDIAN column,
  `SOUND_MOVEMENT`. Faithful (gravity/drag constants reproduced).
- Render: `client/renderer/MeltingJutsuRenderer.java` + `client/model/MeltingJutsuModel.java`
  (legacy 4³ box via `LegacyModelLayerDefinitions.EntityMeltingJutsu_ModelBlock_239()`), V-scroll
  `scrollingTranslucent(lava.png, 0,0.01)`, shadow 0.3. Faithful.
- Wiring: `ModEntityTypes.MELTING_JUTSU`, `ClientModEvents:336` (renderer) + `:456` (model layer).

## Lava Chakra Mode — "lava_chakra_mode" (Four-Tails cloak buff)
| Field | Value |
|---|---|
| index / nameKey | 2 / `lava_chakra_mode` |
| chakra | 10.0/sec drain (×power for the activation only) |
| rank / XP-to-learn | 'S' / requiredXp **250** |
| power | base 1, powerUpDelay 30 |
| cooldown | none in legacy `defaultCooldownMap` (only ROCKS/STREAM set) |
| entity spawned | `EntityLavaChakraMode.EC` (plain `Entity`, size 0.01), tracker(64,3) id 274 |
| gate | requires `EntityBijuManager.getTails == 4` (Four-Tails jinchuriki); toggle off if already active |
| mechanics | every 20t: consume 10 chakra (else die), grant STRENGTH `amp 9 (+existing)`, SPEED `amp 16`; AoE `DamageSource.LAVA` 4 dmg + `setFire(15)` in r=5 |
| particles | vanilla LAVA at user, 1/10 ticks |
| sounds | `BLOCK_LAVA_AMBIENT` 1/20 ticks |
| visual | **lava cloak overlay** re-rendering the user's own model with `textures/lavacloak1.png`, scrolling texmatrix, alpha 0.6, full-bright; hidden in 1st-person |
| 1.12.2 file:line | `entity/EntityLavaChakraMode.java:66-170` (entity+Jutsu), `:172-258` (cloak render). Enum: `ItemYooton.java:61` |

**1.20.1 status: DONE.**
- Item def: `AdvancedNatureJutsuItem.java:1933` `definition(2,"lava_chakra_mode",'S',250,10.0).withPower(1,30)`.
- Dispatch: `isLavaChakraMode` (`:1378`), `toggleLavaChakraMode` (`:462`): toggle off existing,
  else `hasFourTails` gate (`BijuManager.isJinchurikiOf(...,4)` or `JINCHURIKI_TAILS==4`), spawn,
  store id in `LavaChakraModeEntity.ENTITY_ID_KEY`, `LAVA_AMBIENT` sound. `findActiveLavaChakraMode`
  (`:1761`) re-resolves by stored id or 64-block scan.
- Entity: `entity/LavaChakraModeEntity.java` — 10 chakra/20t (`Chakra.pathway.consume`), DAMAGE_BOOST
  amp `9(+existing)`, MOVEMENT_SPEED amp 16, LAVA AoE 4 dmg + 15s fire r5, LAVA particle 1/10,
  `LAVA_AMBIENT` 1/20, syncs `ACTIVE_TAG` client var. Faithful.
- Render: `client/renderer/LavaChakraModeRenderer.java` (third-person, re-renders owner model with
  `lavacloak1.png`, `scrollingTranslucent(0.01,0.01)`, alpha 0.6, full-bright, 1st-person hidden)
  **plus** `client/renderer/LavaChakraModeLayer.java` (player-model layer keyed off `ACTIVE_TAG`).
  This is a more robust split than the single legacy `RenderCustom`, and faithful to the visual.
- Wiring: `ModEntityTypes.LAVA_CHAKRA_MODE`, `ClientModEvents:329` (renderer) + `:507` (player layer).

---

# YOTON — 陽遁 (Yang Release) — item `narutomod:yoton`
Legacy file: `1.12.2/.../item/ItemYoton.java`. Two jutsu.
Port item: `ModItems.YOTON` → `AdvancedNatureJutsuItem(YOTON)`. No element requirement.

## Multi-Size Technique (Bigger Me) — "biggerme" (超肥大化の術)
| Field | Value |
|---|---|
| index / nameKey | 0 / `biggerme` |
| chakra | 50.0 (× power) |
| rank / XP-to-learn | **'B'** / `ranked` |
| power | base 2, powerUpDelay 50; max-power capped to **10** (legacy `getMaxPower` `min(super,10)`) |
| cooldown | none in legacy `defaultCooldownMap` (Yoton sets none) |
| entity spawned | `EntityBiggerMe` (`EntityClone.Base`), tracker(64,1) id 149 |
| procedure / callback | `EntityBiggerMe.Jutsu` → spawn EC(power); user `startRiding` |
| mechanics | grows over 40t to `scale`; reach modifier `sqrt(4·s²+h²)`; ATTACK_DAMAGE `+s²`; permanent JUMP_BOOST amp s; steerable mount; redirects damage to rider; when un-ridden → 500 EXPLOSION_NORMAL particles + die |
| sounds | (legacy: none on spawn) |
| visual | a giant clone of the player (clone renderer), rider hidden via `RenderPlayerEvent.Pre` cancel |
| 1.12.2 file:line | `ItemYoton.java:50` (enum), `:107-227` (entity+Jutsu), `:229-263` (render+hook) |
| **JutsuScroll** | `JutsuScrollDefinition.MULTI_SIZE` → icon `yoton.png`, target `ModItems.YOTON`, def `YOTON.definitionByIndex(0)` |

**1.20.1 status: DONE.**
- Item def: `AdvancedNatureJutsuItem.java:1940` `ranked(0,"biggerme",'B',50.0).withPower(2,50)`.
  Power clamp to [2,10] in `activateBiggerMe` (`:783` `Mth.clamp(rawPower,2,10)`). Matches legacy cap.
- Dispatch: `isYotonBiggerMe` (`:1600`), `activateBiggerMe` (`:770`): block if already riding own BiggerMe,
  custom chakra-cost `chakraUsage*power`, `BiggerMeEntity.spawnFrom`.
- Entity: `entity/BiggerMeEntity.java` (`PathfinderMob`) — 40t growth lerp, reach `sqrt(4s²+h²)`,
  ATTACK_DAMAGE `+s²`, JUMP amp s, steerable (`tickControlled`/`controlAcceleration`), damage redirect
  to rider, copies owner equipment/attributes/name, un-ridden → POOF + `SOUND_POOF`. Faithful
  (legacy used 500 EXPLOSION_NORMAL on cleanup; port uses 160 POOF — *cosmetic delta, acceptable*).
  Rider hidden via client pose handling (`PlayerPoseEvents` referenced in grep) rather than the legacy
  `RenderPlayerEvent.Pre` cancel — verify the local player is hidden while riding.
- Render: `client/renderer/BiggerMeRenderer.java`, `ClientModEvents:249`.
- Wiring: `ModEntityTypes.BIGGERME`, `JutsuScrollDefinition.MULTI_SIZE`.

## Sealing — "sealing" (Yang-release sealing technique)
| Field | Value |
|---|---|
| index / nameKey | 1 / `sealing` |
| chakra | 100.0 |
| rank / XP-to-learn | 'S' / `ranked` |
| cooldown | none in legacy `defaultCooldownMap` |
| entity spawned | `EntitySealing.EC` (separate tome entity) |
| procedure / callback | `EntitySealing.EC.Jutsu` |
| 1.12.2 file:line | `ItemYoton.java:51` (enum). Entity in `entity/EntitySealing.java` (separate, not in the 4 listed tomes) |

**1.20.1 status: DONE (port present; verify against legacy `EntitySealing`).**
- Item def: `AdvancedNatureJutsuItem.java:1941` `ranked(1,"sealing",'S',100.0)`.
- Dispatch: `isYotonSealing` (`:1594`), `activateSealing` (`:753`): requires `SealingEntity.hasValidPlacementTarget`
  (clear 13×13 top-face circle w/ eight legacy torch anchors), `SealingEntity.spawnFrom`.
- Entity: `entity/SealingEntity.java` (501 lines) + `entity/SealingChainsEntity.java`; renderers
  `SealingRenderer`/`SealingChainsRenderer` (`ClientModEvents:367-368`); types
  `ModEntityTypes.SEALING`/`SEALING_CHAINS`.
- *Out of the 4 listed tome files' direct scope* — `EntitySealing` lives in its own legacy entity
  class; this audit did not line-diff it. Cross-check the Sealing/SealingChains backlog (if a
  dedicated entry exists) for full fidelity of the seal placement + chain visuals.

---

## Cross-domain risks / verification checklist
1. **XP-curve delta (Shakuton Kill & Blast):** legacy ScorchKill/ScorchBlast were learn-free
   (4-param `JutsuEnum`, rank-only). Port assigns `requiredXp` 200/250. Confirm the desired unlock economy.
2. **Added spawn sounds:** Magma Ball / Melting Jutsu got `SOUND_FLAMETHROW` on cast (legacy had
   none at spawn). Harmless flavor; flag if "faithful = no added SFX".
3. **BiggerMe cleanup FX:** legacy 500 `EXPLOSION_NORMAL` vs port 160 `POOF`. Cosmetic.
4. **BiggerMe first-person rider hiding:** legacy cancels `RenderPlayerEvent.Pre`; port relies on
   `PlayerPoseEvents`/render path — verify the riding player isn't double-drawn.
5. **ScorchOrb explosion magnitude:** confirm `SpecialEvent.setSphericalExplosionEvent`
   reproduces `EventSphericalExplosion(..., (int)maxScale, 0, 0.3333f)` and AoE `maxScale*60`.
6. **Sealing (Yoton #1):** not line-diffed here (separate legacy `EntitySealing`); verify in its own backlog.
7. **Particle color/brightness parity:** all ARGB constants were carried over verbatim into
   `ModParticleTypes.options(...)`; spot-check that `NarutoParticleKind.SMOKE_COLORED` renders the
   same as legacy `Particles.Types.SMOKE` (alpha, additive vs translucent).

## Required assets per jutsu (all present in port)
- `textures/fireball2.png` (Scorch Orb), `textures/magmaball.png` (Magma Ball),
  `textures/lava.png` (Melting Jutsu), `textures/lavacloak1.png` (Lava Chakra Mode cloak/layer),
  `textures/blocks/yoton.png` (Multi-Size scroll icon).
- Sounds: `SOUND_KAIRIKIMUSO` (`narutomod:kairikimuso`), `SOUND_MOVEMENT` (`narutomod:movement`),
  `SOUND_FLAMETHROW`, `SOUND_CHARGING_CHAKRA`, `SOUND_POOF`, `SOUND_JUTSU`; vanilla `FIRE_EXTINGUISH`,
  `LAVA_AMBIENT`.
- Effects: `ModEffects.CORROSION` (Boiling Mist) — legacy `PotionCorrosion`.
- Model layer: `melting_jutsu_legacy/main` (`MeltingJutsuModel`).
