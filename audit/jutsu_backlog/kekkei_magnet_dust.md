# Jutsu Backlog — Jiton (Magnet) + Jinton (Dust) + Gourd domain

Audit of the 1.12.2 → 1.20.1 hand-port. Original (source of truth):
`D:\mcmodding\naruto\1.12.2\src\main\java\net\narutomod`. Port (WIP):
`D:\mcmodding\naruto\1.20.1\src\main\java\net\narutomod`.

> NOTE: The task brief said the 1.20.1 counterparts are "none yet — likely entirely
> unported." This is **incorrect**: the domain is **substantially ported**. The five
> entities (SandShield, SandBullet, SandBind, SandLevitation, JintonBeam, JintonCube)
> exist as native 1.20.1 entity classes, the Jiton/Jinton "tomes" are merged into a
> single generic `AdvancedNatureJutsuItem`, and three custom renderers/models were
> rebuilt. The main regressions are around **sand material color variants (IRON/SAND/GOLD)**
> and the **Gourd armor** (custom model, passive effects, auto-equip/decay all dropped).

---

## Framework / wiring notes

### 1.12.2 framework
- **`ItemJiton` / `ItemJinton`** are per-element "tome" `ModElement` classes. Each declares
  `ItemJutsu.JutsuEnum` constants (index, translation-key, rank char `'S'`, XP-to-learn,
  chakra cost, and an `IJutsuCallback` that spawns the entity). The item is a
  `ItemJutsu.Base` ("RangedItem") that scrolls through jutsu and fires the current one.
- **`ItemJutsu.JutsuEnum(index, key, rank, xpToLearn, chakraUsage, callback)`** — the 4th arg
  is the XP threshold to unlock; the 5th is the chakra cost. `defaultCooldownMap[index]=0`.
- Entities are registered via `EntityEntryBuilder` with explicit numeric IDs + tracker
  (range, updateFreq, velocity-updates).
- The shared particle engine is **`ItemJiton.SwarmTarget`** (an inner class of `ItemJiton`),
  reused by SandShield, SandBind and SandLevitation to animate hundreds of
  `EntityParticle.Base` toward a moving target AABB. **This is the heart of all Jiton visuals.**
- **`ItemGourd.getMouthPos(EntityLivingBase)`** computes the gourd-mouth world position
  (offset `(0.4, 1.75, -0.4)` rotated by `renderYawOffset`) — the origin point for sand.

### 1.20.1 framework
- Both tomes collapse into **`item/AdvancedNatureJutsuItem.java`** keyed by an
  `AdvancedNatureKind` enum. `JINTON` and `JITON` enum entries hold `JutsuDefinition`s with
  the **same indices/keys/chakra costs** as 1.12.2 (verified — see each jutsu below).
  - `JITON` (`AdvancedNatureJutsuItem.java:1865`): `requiresGourdBody = true`,
    `requiredFlags = REQ_FUTON | REQ_DOTON`.
  - `JINTON` (`:1857`): `requiresGourdBody = false`, `requiredFlags = REQ_KATON | REQ_FUTON | REQ_DOTON`.
- Items registered in **`registry/ModItems.java:120-123`**:
  `JINTON`/`JITON → new AdvancedNatureJutsuItem(...)`; `GOURDBODY → new ArmorItem(DIAMOND, CHESTPLATE)` (`:112`).
- Entities registered in **`registry/ModEntityTypes.java`**: `ENTITYJITONSHIELD` (:201),
  `JINTONBEAM` (:236), `JINTONCUBE` (:237), `SAND_BIND` (:281), `SAND_BULLET` (:282),
  `SAND_LEVITATION` (:283). Same registry-name strings as 1.12.2.
- Renderers registered in **`client/ClientModEvents.java`**:
  `SAND_BIND`, `SAND_BULLET`, `ENTITYJITONSHIELD` → `NoopEntityRenderer` (:361-364);
  `SAND_LEVITATION → SandLevitationRenderer` (:363); `JINTONBEAM/JINTONCUBE` → their renderers (:309-310).
- **The `SwarmTarget` particle engine was re-implemented per entity, server-side**, as a
  private `SandSwarm`/`SandTrail` simulation that emits vanilla server particles
  (`ModParticleTypes.options(NarutoParticleKind.SUSPENDED_COLORED, color, ...)`) rather than
  spawning `EntityParticle` entities. Cosmetically similar; mechanically lighter.
- **Sand color is hard-coded** to `SandBulletEntity.DEFAULT_COLOR = 0xFF303030` (= IRON)
  in every `spawnFrom(...)` call. The 1.12.2 `ItemJiton.Type {IRON, SAND, GOLD}` system,
  per-stack `MaterialType` NBT, and the per-type color/texture are **not ported**.

### Cross-cutting GAPS (apply to multiple jutsu)
1. **Sand material `Type` (IRON 0xff303030 / SAND 0xffd8d09d / GOLD 0xfffffa58)** — dropped.
   In 1.12.2 the gourd is "loaded" with a sand type; sand color and SandLevitation texture
   switch on it. Port always uses IRON. (`ItemJiton.java:175-216`, `ItemGourd.getMaterial`.)
2. **`ItemGourd` armor behavior** — see the Gourd section; the port is a bare diamond chestplate.
3. **`SwarmTarget` as real entities** — port uses server particle bursts; no per-particle
   physics entity, so dome/cloud silhouettes are approximations not exact particle swarms.

---

## Jiton (Magnet Release) — `ItemJiton.java`

`ItemJiton` defines 4 jutsu (`ItemJiton.java:65-68`). Gating
(`ItemJiton.RangedItem.onItemRightClick`, `:130-137`): needs Futon **and** Doton tomes in
inventory **and** `ItemGourd.body` worn in CHEST slot (or creative).
Port gate: `requiresGourdBody` + `REQ_FUTON|REQ_DOTON` (`AdvancedNatureJutsuItem.java:1867-1868,1984`). ✔

### Jiton: Sand Shield (Suna no Tate)
| Field | Value |
|---|---|
| Display key | `entityjitonshield` |
| Index | 0 (`ItemJiton.java:65`) |
| Chakra | 20.0 to summon; **0.5 / tick** upkeep (`chakraUsage=0.5`, `ItemJiton.java:219,334`) |
| XP to learn / rank | 150 XP, rank `'S'` (`:65`) |
| Cooldown | 0 default; on death sets **2400 ticks** scaled by chakra modifier (`:118,273-275`) |
| Entity | `ItemJiton.EntitySandShield extends EntityShieldBase` (`:218`) |
| Procedure(s) | `EntitySandShield.Jutsu.createJutsu` (`:350`); `EntityEarthBlocks.BlocksMoveHelper.collideWithEntity`; `ProcedureUtils.pushEntity` |
| Particles | `SwarmTarget` swarms of `EntityParticle.Base` colored by sand Type; spawned on hit/collision and on return |
| Sounds | `SoundEvents.BLOCK_SAND_PLACE` (per `SwarmTarget.playFlyingSound`) |
| Visual | 3×3 invisible dome (`RenderCustom.doRender` is empty — **no model**); all visuals are flying sand particles converging from gourd mouth to impact points and back |
| Mechanics | MaxHealth = `5 × ninjaLevel` (player) / 100 (mob) (`:231-232`); intercepts incoming attacks, pushes attacker (5d,1.5f); on death collects sand back to gourd (`onDeathUpdate`/`SwarmTarget` to mouth) |
| 1.12.2 file:line | `ItemJiton.java:65, 218-364` |

**1.20.1 status: PARTIAL (mechanics mostly DONE; visuals approximated; material color dropped).**
- Port: `entity/SandShieldEntity.java` (native `Entity`, owner-ridden, synced HEALTH/MAX_HEALTH/COLOR/RETURNING).
- ✔ MaxHealth `= ninjaLevel × 5` (`:134`); ✔ 0.5 chakra/tick upkeep (`:49,182`); ✔ summon chakra 20 via
  `definition(0,...,20.0D)` (`:1871`); ✔ 2400-tick cooldown applied on return (`:51,375`); ✔ attack
  interception via `LivingAttackEvent` (`:114-129,530-545`) pushing attacker 5d/1.5f; ✔ fast-collider push
  (threshold 0.22) (`:53,325-339`); ✔ sand returns to gourd-mouth (`tickReturning`, `SandTrail`).
- GAP: renderer is `NoopEntityRenderer` — **no dome silhouette**; visuals are server particle bursts
  (`spawnShieldParticles`/`spawnImpactParticles`/`spawnPathParticles`), not the `EntityParticle` swarm.
  Faithful-ish but not the original per-particle simulation.
- GAP: color fixed to IRON `0xFF303030`; SAND/GOLD variants unavailable.
- DIVERGENCE: re-summoning while a shield exists makes the player **ride** it (`:78-80,207-213`);
  1.12.2 kept one shield per `ID_KEY` and did not mount the player. Verify intended.

### Jiton: Sand Bullet (Suna Shuriken-style projectile)
| Field | Value |
|---|---|
| Display key | `sand_bullet` |
| Index | 1 (`ItemJiton.java:66`) |
| Chakra | 20.0 (`:66`) |
| XP / rank | 100 XP, `'S'` (`:66`) |
| Cooldown | 0 (`:119`) |
| Entity | `EntitySandBullet.EC extends EntityScalableProjectile.Base` (`EntitySandBullet.java:71`) |
| Procedure(s) | `EntitySandBullet.EC.Jutsu.createJutsu` (`:119`); `ItemJutsu.causeJutsuDamage(...).setProjectile()`; `ProcedureUtils.pushEntity` |
| Particles | `Particles.spawnParticle(Particles.Types.SUSPENDED, ... color ...)` trail, 100/tick (`:113-116`) |
| Sounds | spawn `BLOCK_SAND_PLACE`; on hit `narutomod:bullet_impact` (`:98-99,122`) |
| Visual | invisible entity (`CustomRender.doRender` empty); a colored SUSPENDED particle trail |
| Mechanics | shot at speed 1.2; lifetime 80 ticks; on entity hit **10 dmg** + push (10d, 3.0f); no block stop (`checkOnGround()` empty) |
| 1.12.2 file:line | `ItemJiton.java:66`; `entity/EntitySandBullet.java` (whole) |

**1.20.1 status: DONE (mechanics) / PARTIAL (material color).**
- Port: `entity/SandBulletEntity.java`. ✔ speed 1.2 (`:41`); ✔ life 80 (`:40`); ✔ 10 damage + push 10d/3.0f
  (`:243-244`); ✔ no-gravity custom legacy motion (`updateLegacyNoGravityMotion`, water slowdown 0.8);
  ✔ block+entity raytrace impact (`findImpact`); ✔ spawn `SAND_PLACE`, hit `ModSounds.SOUND_BULLET_IMPACT`
  (`:75-76,240-241`); ✔ colored SUSPENDED trail particles (`spawnTrailParticles`, `:251-265`).
- GAP: color fixed to IRON (`DEFAULT_COLOR`), no Type variants.

### Jiton: Sand Bind / Sand Burial (Sabaku Kyū → Sabaku Sōsō)
| Field | Value |
|---|---|
| Display key | `sand_bind` |
| Index | 2 (`ItemJiton.java:67`) |
| Chakra | 100.0 to bind (`:67`); **50.0** to trigger the funeral/crush (`EntitySandBind.java:239`, `sandFuneral`) |
| XP / rank | 200 XP, `'S'` (`:67`) |
| Cooldown | 0 (`:120`) |
| Entity | `EntitySandBind.EC extends Entity` (`EntitySandBind.java:70`) |
| Procedure(s) | `EntitySandBind.EC.Jutsu.createJutsu` (`:217`); `EntitySandBind.sandFuneral` (`:237`, triggered by left-click self in `ItemJiton.RangedItem.onLeftClickEntity` `:161-166`); `ProcedureSync.ResetBoundingBox`; `ProcedureUtils.objectEntityLookingAt` |
| Particles | `SwarmTarget` of 1000 particles, scale 2, sand-colored (`:92-93`) |
| Sounds | bind spawn `SAND_PLACE` (via swarm); funeral `narutomod:sabakusoso` (`:240-242`) |
| Visual | invisible entity; a 1000-particle sand mass that envelops the target; bounding box tracks the swarm (`setEntityBoundingBox` from `SwarmTarget.getBorders`) |
| Mechanics | range 30 (`:220`); holds target (Paralysis potion + teleport to captured pos, `:162-163`); funeral = 20 ticks × **4 dmg/tick**, bypasses armor (`:128-129,146-155,176-177`); MAXTIME 600; re-cast on already-bound entity ends it (`:222-224`); damages gourd 20 on end (`:138-141`) |
| 1.12.2 file:line | `ItemJiton.java:67`; `entity/EntitySandBind.java` (whole) |

**1.20.1 status: DONE (mechanics) / PARTIAL (material color, swarm count).**
- Port: `entity/SandBindEntity.java`. ✔ bind chakra 100 (`:1873`); ✔ funeral chakra 50
  (`FUNERAL_CHAKRA_COST=50`, `:39`); ✔ MAX_LIFE 600 (`:40`); ✔ funeral 20 ticks × 4 dmg, invuln reset,
  ninjutsu damage (`:41,45,227-231`); ✔ hold = Paralysis (`ModEffects.PARALYSIS`) + teleport + zero motion
  (`:233-243`); ✔ target capture test (`isTargetCaptured`); ✔ `sabakusoso` sound (`:114`);
  ✔ return-to-gourd on end (`startReturnOrDiscard`); ✔ swarm bounding box sync (`updateSwarm`→`setBoundingBox`).
- ✔ trigger flow: right-click looking at own bind triggers funeral (`AdvancedNatureJutsuItem.java:1270-1280`).
  In 1.12.2 the funeral is triggered by **left-clicking yourself** (`onLeftClickEntity`); the port moved this
  to the right-click jutsu activation when looking at an owned bind. Verify UX intent.
- GAP: swarm uses 220 particles (`SWARM_TOTAL=220`, `:42`) vs 1000 in 1.12.2 — lighter/thinner sand mass.
- GAP: color fixed to IRON; no Type variants. Renderer is `NoopEntityRenderer` (particle-only, as in original).

### Jiton: Sand Levitation / flying sand cloud (Suna no Heya-style ride)
| Field | Value |
|---|---|
| Display key | `sand_levitation` |
| Index | 3 (`ItemJiton.java:68`) |
| Chakra | 200.0 to summon; **0.25 / tick** while flying (`SANDFLY.chakraUsage`, `:68`, used `EntitySandLevitation.java:213`) |
| XP / rank | 200 XP, `'S'` (`:68`) |
| Cooldown | 0 (`:121`) |
| Entity | `EntitySandLevitation.EC extends Entity` (`EntitySandLevitation.java:64`) |
| Procedure(s) | `EntitySandLevitation.EC.Jutsu.createJutsu` (`:242`) |
| Particles | `SwarmTarget` cloud of 100 (`:83-84`); on dismount, sand flies back to gourd |
| Sounds | none explicit on the cloud (swarm `SAND_PLACE`) |
| Visual | **`ModelSandCloud`** — a large ~150-box randomized sand-disc model (`CustomRender`, `:260-...`); texture IRON=`narutomod:textures/gray_dark.png`, SAND=`minecraft:.../sand.png`; CLOUD_SCALE 2; scales up over waitTime |
| Mechanics | rideable by up to 3 players (`canFitPassenger`, `:142`); summoner controls (yaw + moveRelative flight, up = -pitch/45); dismount when out of chakra; auto-mounts summoner after waitTime 40 (`:186-194`) |
| 1.12.2 file:line | `ItemJiton.java:68`; `entity/EntitySandLevitation.java` (whole; model `:291-...`) |

**1.20.1 status: PARTIAL (mechanics DONE; model simplified; material color partly kept).**
- Port: `entity/SandLevitationEntity.java` + `client/renderer/SandLevitationRenderer.java`
  + `client/model/SandLevitationModel.java`.
- ✔ summon chakra 200 (`:1874`); ✔ 0.25 chakra/tick (`CHAKRA_COST_PER_TICK=0.25`, `:33,257`);
  ✔ rides up to 3 (`canAddPassenger`, `:188`); ✔ 3-seat offsets (`positionRider`, `:202-218`);
  ✔ flight control (yaw, walk 0.04 / sprint 0.1 accel, up=-pitch/45) (`:256-294`); ✔ auto-mount after
  WAIT_TIME 40 (`:34,152-156`); ✔ dismount on chakra-out + return-to-gourd (`tickReturning`);
  ✔ CLOUD_SCALE 2, scale-up animation, texture switch IRON gray_dark vs sand
  (`SandLevitationRenderer.java:21-23,69`).
- GAP: `SandLevitationModel` is a rebuilt cloud model — confirm it matches the original ~150-box
  `ModelSandCloud` silhouette (original randomizes UVs per box; port likely uses a fixed BB shape).
- GAP: texture switch depends on `getColorForRender()` but **color is always IRON** at spawn, so the
  `sand.png` texture branch is effectively dead until Type variants are restored.

---

## Jinton (Dust Release) — `ItemJinton.java`

`ItemJinton` defines 2 jutsu (`ItemJinton.java:65-66`). `MIN_PLAYER_XP = 70`. Gating
(`onItemRightClick`, `:135-142`): needs Katon **and** Futon **and** Doton tomes (or creative).
Both are **charged** (held-use) jutsu: `getUsePercent = (maxUseDur - timeLeft)/400`; max usable power
scales with ninja level: `(ninjaLevel - 70 + 5)/5`, clamped to 10 (beam) / 50 (cube) (`:108-120`).
Cooldown on release: `usePercent × 12000 × cooldownModifier` (`:128-129`).

### Jinton: Particle Disintegration — Beam (Genkai Hakuri no Jutsu, beam form)
| Field | Value |
|---|---|
| Display key | `jintonbeam` |
| Index | 0 (`ItemJinton.java:65`) |
| Chakra | 500.0 × power (`:65`; consumed scaled by charge) |
| XP / rank | `MIN_PLAYER_XP*10` = **700** XP, `'S'` (`:65`) |
| Cooldown | up to ~12000 ticks × usePercent × modifier (`:128-129`) |
| Entity | `EntityBeam extends EntityBeamBase.Base` (`ItemJinton.java:151`) |
| Procedure(s) | `EntityBeam.Jutsu.createJutsu` (`:231`); inner `AirPunch extends ProcedureAirPunch` (`:209`) — beam sweep damage + block break; `attackEntityWithJutsu` (bypass-armor, absolute) |
| Particles | `EnumParticleTypes.SMOKE_LARGE` during (`:215`); blockDropChance −1, hardnessLimit 100, breakChance 1.0 |
| Sounds | `narutomod:genkaihakurinojutsu` (`:236-237`) |
| Visual | **`ModelLongCube`** scrolling beam (white core + translucent shell, layered boxes), texture `narutomod:textures/longcube_white.png`; grows over 10 ticks; wait 60, range 30, lifetime wait+60 (`:382-485`) |
| Mechanics | power = min(power/2+0.5, 10); damage proportional to `farRadius / target box edge × 0.25 × maxHealth`; non-living → onKillCommand/discard; immune to fire |
| 1.12.2 file:line | `ItemJinton.java:65, 151-247, 382-408, 450-485` |

**1.20.1 status: DONE.**
- Port: `entity/JintonBeamEntity.java` + `client/renderer/JintonBeamRenderer.java`
  + `client/model/LegacyLongCubeModel.java`.
- ✔ chakra 500 (`:1863`); ✔ 700 XP to learn (`definition(0,"jintonbeam",'S',700,500.0D)`, `:1863`);
  ✔ wait 60, active 60, range 30 (`:31-33`); ✔ scale `min(power/2+0.5,10)` (`:50`);
  ✔ damage = `maxHealth × (farRadius/avgEdge × 0.25)`, invuln reset, non-living discard (`:211-231`);
  ✔ block break with hardness limit 100, mob-griefing gated, SMOKE particles (`:233-264`);
  ✔ `genkaihakurinojutsu` sound (`:60`); ✔ scrolling translucent long-cube beam renderer
  (`renderJintonBeam`, grows over 10 ticks, scale derived like original `RenderBeam`, `:51-64`).
- ✔ charged power + ~12000-tick scaled cooldown path via `getAdvancedNaturePower` + `setCurrentJutsuCooldown`
  (`AdvancedNatureJutsuItem.java:819-834`). Confirm the cube/beam cooldown numerically equals
  `usePercent×12000×mod` from 1.12.2 (the generic charge/cooldown helper should be spot-checked).

### Jinton: Particle Disintegration — Cube (Genkai Hakuri no Jutsu, cube form)
| Field | Value |
|---|---|
| Display key | `jintoncube` |
| Index | 1 (`ItemJinton.java:66`) |
| Chakra | 600.0 × power (`:66`) |
| XP / rank | 700 XP, `'S'` (`:66`) |
| Cooldown | charge-scaled (same path as beam) |
| Entity | `EntityCube extends EntityScalableProjectile.Base` (`ItemJinton.java:258`) |
| Procedure(s) | `EntityCube.Jutsu.createJutsu` (`:370`); `ProcedureAoeCommand.set(...).effect(SLOWNESS,5,5)` (`:290`); `destroyBlocksAndEntitiesInAABB`; `attackEntityWithJutsu` |
| Particles | `Particles.Types.FALLING_DUST` 0xC0A0A0A0 during idle (`:343-344`) |
| Sounds | `narutomod:genkaihakurinojutsu` (`:374-375`) |
| Visual | **`ModelCube`** white core + translucent shell box cube (texture longcube_white), rendered with alpha 0.3 shell; timeline wait 60 → grow 30 → idle 40 → shrink 10 (`:258-380, 410-448, 487-515`) |
| Mechanics | power = power×2+2 (cap maxUsablePower 50); positions at look target ≤50; AoE Slowness; destroys all blocks + damages entities in cube AABB by `volumeOverlap/volume × 0.5 × maxHealth`; immune to fire |
| 1.12.2 file:line | `ItemJinton.java:66, 258-380, 410-448, 487-515` |

**1.20.1 status: DONE.**
- Port: `entity/JintonCubeEntity.java` + `client/renderer/JintonCubeRenderer.java`
  + `client/model/JintonCubeModel.java`.
- ✔ chakra 600, 700 XP (`definition(1,"jintoncube",'S',700,600.0D)`, `:1864`);
  ✔ timeline wait 60 / grow 30 / idle 40 / shrink 10 (`:38-41`); ✔ fullScale = `power×2+2` (`:61`);
  ✔ base side 0.5 (`:44`); ✔ anchor at look target ≤50, Slowness amp 5 dur 5 (`:43,194-214`);
  ✔ hold entities (zero motion) during life (`holdEntities`, `:216-222`);
  ✔ block destruction (mob-griefing gated) + entity damage `maxHealth × overlap/volume × 0.5`
  (`destroyBlocks`/`damageEntities`, `:231-280`); ✔ FALLING_DUST 0xC0A0A0A0 (`:42,224-229`);
  ✔ `genkaihakurinojutsu` sound (`:72`); ✔ core+translucent-shell cube renderer (`renderCore`/`renderShell`
  alpha 0.3, `:39-42`).

---

## Gourd — `ItemGourd.java` (`gourdbody`)

The Gourd is the **chest-slot enabler** for all Jiton jutsu and provides passive survivability.

| Field | Value |
|---|---|
| Registry name | `gourdbody` (`ItemGourd.java:39,109`) |
| Type | `ItemArmor` with custom `ArmorMaterial "GOURD"` — durability 1024, reduction `{2,5,1024,2}`, toughness 5 (`:48-49`) |
| Armor model | **`ModelGourd extends ModelBiped`** — large multi-bone sand-gourd on the back (`:139-331`) |
| Textures | `narutomod:textures/gourd_iron.png` (IRON/default) or `narutomod:textures/gourd_sand.png` (SAND) — switched by `getMaterial(stack)` (`:62-70`) |
| `onArmorTick` | While wearing AND holding Jiton tome: extinguish fire; apply **Resistance II** (2 ticks); every 20 ticks **repair 1 durability** (`:73-84`) |
| `onUpdate` | If player no longer has the Jiton tome in inventory → **shrink the gourd stack by 1** (it crumbles) (`:87-93`) |
| Material NBT | `setMaterial/getMaterial` store `MaterialType` int = `ItemJiton.Type` ID (`:112-121`) |
| `getMouthPos` | static helper used by all sand entities (`:123-129`) |
| Auto-grant | `ItemJiton.RangedItem.onUpdate` re-gives a gourd of the loaded sand Type if missing, after a 2400-tick cooldown (`ItemJiton.java:140-154`) |
| 1.12.2 file:line | `ItemGourd.java` (whole) |

**1.20.1 status: STUB / PARTIAL — major regressions.**
- Port: `registry/ModItems.java:112-113` → **`new ArmorItem(ArmorMaterials.DIAMOND, ArmorItem.Type.CHESTPLATE, new Item.Properties())`**.
  There is **no `GourdItem` class**; it is a plain vanilla diamond chestplate. `gourdbody.json` is only an item model.
- ✔ It satisfies the Jiton gate (`player.getItemBySlot(CHEST).is(GOURDBODY)`, `AdvancedNatureJutsuItem.java:1984`).
- ✔ Each sand entity reimplements `gourdMouthPosition(owner)` inline (e.g. `SandShieldEntity.java:449-462`,
  `SandBindEntity.java:369-381`, `SandLevitationEntity.java:373-386`) — functionally replaces `getMouthPos`.
  Note: the port uses `look/right` vectors, not the original `renderYawOffset (0.4,1.75,-0.4)` formula — close
  but **not identical** placement.
- **MISSING — custom `ModelGourd` armor model.** The diamond chestplate renders as vanilla diamond armor;
  the gourd-on-back silhouette is gone. Needs a `HumanoidModel`-based layer/`GeoModel` rebuild of the
  multi-bone gourd plus armor-model registration.
- **MISSING — gourd textures** `gourd_iron.png` / `gourd_sand.png` and the IRON/SAND texture switch.
- **MISSING — `onArmorTick` passives**: fire-extinguish, **Resistance II** while Jiton held, and the
  20-tick self-repair. (No `ArmorItem` subclass to host them; would need a Forge `LivingTickEvent`/custom item.)
- **MISSING — decay**: gourd does not crumble (`onUpdate` shrink) when the Jiton tome is dropped.
- **MISSING — gourd material `Type`** (`setMaterial/getMaterial`) and the **auto-grant/regrow** of a
  gourd via Jiton's `onUpdate` 2400-tick path. Search of the port found no equivalent
  (no `onArmorTick`/`inventoryTick` gourd logic anywhere; grep clean).

---

## Summary of remaining work (by risk)

1. **Gourd armor is a bare diamond chestplate** — restore custom `ModelGourd` + iron/sand textures,
   `onArmorTick` (extinguish + Resistance II + self-repair), crumble-on-Jiton-loss, and the
   auto-grant/regrow loop. (STUB → needs near-full reimplementation.)
2. **Sand material `Type` (IRON/SAND/GOLD)** dropped everywhere — color always IRON `0xFF303030`.
   Restore the per-stack `MaterialType` and propagate color/texture to bullet, shield, bind,
   levitation, and gourd. (Affects every Jiton jutsu's visual.)
3. **Sand swarm fidelity** — port uses server particle bursts (and a thinner 220-particle bind vs 1000),
   not the `EntityParticle`/`SwarmTarget` per-particle simulation; shield/bind have **no model**
   (NoopEntityRenderer). Decide whether the particle-burst approximation is acceptable.
4. **SandLevitation cloud model** — verify the rebuilt `SandLevitationModel` matches the original
   ~150-box `ModelSandCloud` silhouette and randomized UVs.
5. **Behavior divergences to verify**: SandShield re-summon mounts the player (new); Sand Bind funeral
   moved from left-click-self to right-click-on-owned-bind; confirm Jinton charge→cooldown math equals
   `usePercent × 12000 × modifier`.

### Status roll-up (8 techniques across Jiton + Jinton + Gourd)
- **DONE (4):** Jinton Beam, Jinton Cube, Sand Bullet, Sand Bind *(mechanics faithful; color/material caveat)*.
- **PARTIAL (3):** Sand Shield, Sand Levitation, *(and Sand Bullet/Bind carry the material-color gap)*.
- **STUB (1):** Gourd armor (vanilla diamond chestplate; model + passives + decay + material missing).
- **MISSING (0):** every technique has at least a working entity/activation port.

(Counted below as: jutsuCount=8 [4 Jiton + 2 Jinton + Gourd item + gourd auto-grant/decay behavior];
missing=0, stub=1 [Gourd], partial=3 [SandShield, SandBullet/material, SandLevitation], done=4.)
