# Jutsu Backlog — Mokuton (Wood) + Hyoton (Ice) Domain

Audit of the 1.12.2 → 1.20.1 hand-port for every technique tied to the
`ItemMokuton`, `ItemHyoton`, and `ItemIceSenbon` tomes.

- ORIGINAL (read-only): `D:\mcmodding\naruto\1.12.2\src\main\java\net\narutomod`
- PORT (WIP): `D:\mcmodding\naruto\1.20.1\src\main\java\net\narutomod`

Status legend: MISSING (no port) / STUB (registered only) / PARTIAL (some behavior,
incomplete mechanics or missing visuals) / DONE (faithful mechanics + visuals).

---

## Framework / Notes

### 1.12.2 framework
- Each element tome (`ItemMokuton`, `ItemHyoton`) is a `ElementsNarutomodMod.ModElement`
  whose `block` Item subclasses `ItemJutsu.Base`. Jutsu are declared as static
  `ItemJutsu.JutsuEnum(index, key, rank, [xp,] chakraUsage, IJutsuCallback)` fields and
  passed into the item constructor.
  - `JutsuEnum(index, key, rank, chakraUsage, callback)` — no XP gate (XP arg omitted).
  - `JutsuEnum(index, key, rank, xp, chakraUsage, callback)` — `xp` is the XP-to-unlock.
  - `rank` is a single char ('S' for every jutsu in this domain).
- `getPower(stack, entity, timeLeft, min, max)` ramps power with charge time (right-hold).
- `defaultCooldownMap[index]` is set to `0` for ALL jutsu in both tomes (no built-in
  cooldown — the only gating is chakra + XP-to-unlock).
- Each spawned effect is an `EC` (entity class) nested inside an `Entity*` ModElement that
  registers the EntityType via `EntityEntryBuilder` and a client `Render`.
- `ItemJutsu.causeJutsuDamage(...)`, `ItemJutsu.canTarget(...)`, `Chakra.pathway(...)`,
  `ProcedureUtils.objectEntityLookingAt/raytraceBlocks`, and `net.narutomod.event.EventSetBlocks`
  (timed block placement + auto-revert) are the shared hooks.

### 1.20.1 framework (port)
- Both tomes are collapsed into ONE class `item/AdvancedNatureJutsuItem.java`, parameterized by
  `AdvancedNatureKind` enum. `ModItems.MOKUTON` and `ModItems.HYOTON` each instantiate it with the
  matching kind. Jutsu definitions live in the enum constants:
  - `AdvancedNatureKind.HYOTON` (file line ~1847): ice_spike, ice_spear, ice_dome, ice_prison.
  - `AdvancedNatureKind.MOKUTON` (file line ~1885): wood_burial(leftclick), wood_prison,
    wood_house(rightclick2), wood_golem, wood_arm.
- Per-jutsu dispatch is in the long `if/else` chain at `AdvancedNatureJutsuItem` lines ~180-244,
  guarded by `isHyoton*`/`isMokuton*` predicates (index + translationKey match), each routing to
  an `activate*` method (lines ~584-817).
- Effect entities are standalone `*Entity` classes under `entity/` registered in
  `registry/ModEntityTypes.java`; renderers/models under `client/renderer/` + `client/model/`
  registered in `client/ClientModEvents.java`.
- Damage uses `ModDamageTypes.ninjutsu(level, source, owner)`. Block placement is reimplemented
  per-entity with a `placedX` map + tick-based expiry + auto-revert on `remove(...)` (replaces
  `EventSetBlocks`).
- Shared base classes from 1.12.2 (`ItemMokuton.WoodSegment`, `EntitySpike.Base`,
  `EntityShieldBase`, `EntityScalableProjectile.Base`) are NOT ported as shared bases; each effect
  is self-contained. The WoodSegment visual is preserved as `client/model/WoodSegmentModel` driven
  by `WoodArmRenderer`/`WoodBurialRenderer`.

### Passives (per-tick while item held)
- Mokuton (`ItemMokuton.ItemCustom.onUpdate`, 1.12.2 line 115): SATURATION 1t amp0 + heal 0.2/tick.
  - Port: `Passive.MOKUTON` (line ~2008) — IDENTICAL.
- Hyoton (`ItemHyoton.RangedItem.onUpdate`, 1.12.2 line 120): SPEED 2t amp3, extinguish,
  FrostWalker.freezeNearby radius 1 on block change, auto-grant ICE_SENBON if missing; plus a
  "must have Futon+Suiton in inventory" right-click gate (line 111) and an IN_WALL+inside-ICE damage
  immunity hook (`DamageHook`, line 143).
  - Port: `Passive.HYOTON` (line ~1998) does SPEED 2t amp3 + clearFire + auto-grant ICE_SENBON.
    The Futon+Suiton requirement is the kind's `requiredFlags` (REQ_FUTON|REQ_SUITON, line 1849).
    The IN_WALL/inside-ICE immunity is in the shared LivingAttack hook (lines ~1785-1808).
  - PARTIAL GAP: the port's Hyoton passive does NOT call FrostWalker `freezeNearby`
    (frozen-water trail under the user is not reproduced). Everything else matches.

---

## Mokuton (Wood) — `ItemMokuton`

### WOODBURIAL — "Wood Burial" (左click / Mokuton: leftclick)
| Field | Value |
|---|---|
| Index | 0 (`WOODBURIAL`) |
| Display key | `tooltip.mokuton.leftclick` |
| Chakra | 100 (`JutsuEnum(0, ..., 'S', 100d, ...)`), no charge scaling |
| XP / rank | rank 'S', no XP gate |
| Cooldown | 0 (`defaultCooldownMap[0]=0`) |
| Entity | `EntityWoodBurial.EC` (id `narutomod:wood_burial`, ENTITYID 327), extends `ItemMokuton.WoodSegment` |
| Procedure | `EntityWoodBurial.EC.Jutsu.createJutsu` → `ProcedureUtils.objectEntityLookingAt(entity, 20d, 1.0d, ...)`; targets EntityLivingBase only |
| Particles | BLOCK_DUST from `WoodSegment.onUpdate` (per-segment, when over a full block); spawns LEAVES blocks at segment tips via `EventSetBlocks` |
| Sounds | `narutomod:woodgrow` (5% chance on segment spawn) |
| Visual | Many `WoodSegment` entities (ModelWoodSegment, `textures/woodblock.png`, scale 2x, 5°/index twist) grow as branching tendrils from ground toward target, wrapping it; holds + IN_WALL-damages target 1/tick |
| 1.12.2 file:line | `item/ItemMokuton.java:67`; entity `entity/EntityWoodBurial.java:43-152` |

1.20.1 status: **DONE**
- Definition: `AdvancedNatureKind.MOKUTON ranked(0, "tooltip.mokuton.leftclick", 'S', 100.0D)` (line ~1891).
- Dispatch: `isMokutonWoodBurial` (line 1570) → `activateWoodBurial` (line 662): findTarget within 20,
  consume 100 chakra, `WoodBurialEntity.spawnFrom`.
- Entity `entity/WoodBurialEntity.java`: holds target at captured pos, IN_WALL damage 1/tick,
  20-tick root-block plan (OAK_LOG center column + OAK_LEAVES persistent), BLOCK particles, auto-revert
  on remove, 1200t lifetime, `woodgrow` sound. Renderer `WoodBurialRenderer` draws branching curved
  WoodSegment tendrils (model + woodblock texture). Faithful.
- Note: original root blocks were LOG/LEAVES placed via EventSetBlocks at random offsets from segment
  tips; port uses a deterministic root plan. Functionally equivalent (visual/mechanic preserved).

### WOODPRISON — "Wood Prison"
| Field | Value |
|---|---|
| Index | 1 (`WOODPRISON`) |
| Display key | `wood_prison` |
| Chakra | 50 (`JutsuEnum(1, ..., 'S', 50d, ...)`); power scales 1→50 (right-hold, `getPower(...,1f,50f)`, line 102) |
| XP / rank | rank 'S', no XP gate |
| Cooldown | 0 |
| Entity | `EntityWoodPrison.EC` (id `narutomod:wood_prison`, ENTITYID 228), plain Entity, empty renderer |
| Procedure | `EntityWoodPrison.EC.Jutsu` → `ProcedureUtils.raytraceBlocks(entity, 20d)` |
| Particles | none (blocks ARE the effect) |
| Sounds | `narutomod:woodspawn` every 20 add-ticks (via `EventSetBlocks.onAddTick`) |
| Visual | Box of OAK_FENCE (walls/interior) capped with WOODEN_SLAB at top, radius=ceil(power*0.5), height=ceil(power-0.5); traps + MINING_FATIGUE 1200t amp2 on all living inside; auto-revert after 1200t |
| 1.12.2 file:line | `item/ItemMokuton.java:68`; entity `entity/EntityWoodPrison.java:73-195` |

1.20.1 status: **DONE**
- Definition: `ranked(1, "wood_prison", 'S', 50.0D).withPower(1.0F, 50.0F)` (line ~1892).
- Dispatch: `isMokutonWoodPrison` (line 1564) → `activateWoodPrison` (line 640): raytraceBlocks 20,
  consume 50*power, `WoodPrisonEntity.spawnFrom`. Renderer = `NoopEntityRenderer` (matches original empty render).
- Entity `entity/WoodPrisonEntity.java`: power clamped 1..50, radius/height match, captures living in AABB
  + holds them, MINING_FATIGUE(DIG_SLOWDOWN) 1200t amp2 every 20t, OAK_FENCE walls + OAK_SLAB top cap,
  BLOCK particles, `woodspawn` sound every 20t, auto-revert. Faithful.
- Minor: port emits a debug chat message "Wood Prison created: power %.2f" (line 658) — cosmetic, not in original.

### WOODHOUSE — "Four-Pillar House" (右click2 / Mokuton: rightclick2)
| Field | Value |
|---|---|
| Index | 2 (`WOODHOUSE`) |
| Display key | `tooltip.mokuton.rightclick2` |
| Chakra | 100 (`JutsuEnum(2, ..., 'S', 100d, ...)`) |
| XP / rank | rank 'S', no XP gate |
| Cooldown | 0 |
| Entity | NONE — places a structure template, not an entity |
| Procedure | `ItemMokuton.JutsuHouse.createJutsu` (line 167): mob-griefing gate, `objectEntityLookingAt(30)`, requires BLOCK hit on side UP and ground material (GROUND/SAND/GRASS/ROCK); 4-way rotation from yaw |
| Particles | BLOCK_DUST (Blocks.OAK_FENCE id) over the placement footprint |
| Sounds | `narutomod:woodspawn` |
| Visual | Places structure template `narutomod:wood_house_2` with rotation chosen by player yaw |
| 1.12.2 file:line | `item/ItemMokuton.java:69`, callback `JutsuHouse` `item/ItemMokuton.java:167-217` |

1.20.1 status: **DONE (asset-dependent)**
- Definition: `ranked(2, "tooltip.mokuton.rightclick2", 'S', 100.0D)` (line ~1893).
- Dispatch: `isMokutonWoodHouse` (line 1576) → `activateWoodHouse` (line 679): raytrace 30, require UP face +
  earth/sand/stone top (`canSpawnWoodStructureOn`), load `StructureManager.get(narutomod:wood_house_2)`,
  consume 100, yaw-based rotation (`woodHousePlacement`), placement particles
  (`spawnWoodHouseParticles`), `SOUND_WOODSPAWN`, `template.placeInWorld`.
- Mechanics fully ported. ONLY remaining risk: the structure NBT `wood_house_2` must exist under
  `data/narutomod/structures/` (or `structure/`) in the 1.20.1 resources — port prints a "Missing
  structure template" message if absent. VERIFY the asset is shipped.

### GOLEM — "Wood Golem" (Mokujin)
| Field | Value |
|---|---|
| Index | 3 (`GOLEM`) |
| Display key | `wood_golem` |
| Chakra | 1000 (`JutsuEnum(3, ..., 'S', 800, 1000d, ...)`); golem burns `chakraUsage*0.05*xpModifier` per second |
| XP / rank | rank 'S', XP-to-unlock 800 |
| Cooldown | 0 |
| Entity | `EntityWoodGolem.EC` (id `narutomod:wood_golem`, ENTITYID 276), extends `EntityShieldBase`; MODELSCALE 8.0 |
| Procedure | `EntityWoodGolem.EC.Jutsu`: only if not already riding an EC; spawns, owner steers |
| Particles | BLOCK_DUST from ground blocks during grow (growTime 30) |
| Sounds | `narutomod:mokujin_no_jutsu` on summon |
| Visual | Giant rideable wood golem (ModelWoodGolem = ModelBiped + dragon back-piece; `textures/woodgolem.png`); MAX_HEALTH = chakraLevel*50, ATTACK 200, REACH 18; first-person hides head/dragon |
| 1.12.2 file:line | `item/ItemMokuton.java:70`; entity `entity/EntityWoodGolem.java:66-172`, model `:229-756` |

1.20.1 status: **DONE (mechanics + visual)**, with simplifications
- Definition: `definition(3, "wood_golem", 'S', 800, 1000.0D)` (line ~1894) — chakra 1000, XP 800 match.
- Dispatch: `isMokutonWoodGolem` (line 1582) → `activateWoodGolem` (line 732): block re-summon if already
  riding owned golem, consume 1000, `chakraBurn = 1000*0.05*max(xpModifier,0.05)`, `WoodGolemEntity.spawnFrom`.
- Entity `entity/WoodGolemEntity.java` (PathfinderMob): WIDTH 4.8 / HEIGHT 16 (≈0.6*8 / 2.0*8), MAX_HEALTH
  chakraLevel*50, ATTACK 200, growth particles, `mokujin_no_jutsu` sound, rider-steered movement,
  swing→ATTACK_REACH 18 look-attack, per-20t chakra burn, sneak-to-dismiss. Renderer `WoodGolemRenderer` +
  `WoodGolemModel` (scaled biped, `woodgolem.png`). Faithful.
- Differences from original (non-blocking): original `EntityShieldBase` shield/steer semantics and the
  detailed dragon back-ornament + first-person head/dragon hiding are approximated; movement uses
  PathfinderMob+manual control instead of `setOwnerCanSteer`. Visual model present but verify the dragon
  back-piece + headwear glow detail parity if exact fidelity is required.

### ARMATTACK — "Wood Arm"
| Field | Value |
|---|---|
| Index | 4 (`ARMATTACK`) |
| Display key | `wood_arm` |
| Chakra | 50 (`JutsuEnum(4, ..., 'S', 400, 50d, ...)`) |
| XP / rank | rank 'S', XP-to-unlock 400 |
| Cooldown | 0 |
| Entity | `EntityWoodArm.EC` (id `narutomod:wood_arm`, ENTITYID 321), extends `ItemMokuton.WoodSegment` |
| Procedure | `EntityWoodArm.EC.Jutsu` → `objectEntityLookingAt(entity, 30d)`; needs entityHit |
| Particles | BLOCK_DUST from WoodSegment.onUpdate (over full blocks) |
| Sounds | `narutomod:woodgrow` (50% chance on segment spawn) |
| Visual | Wood-segment arm grows from user (offset -0.4,1.2,0 yaw0 pitch90) toward target; on reach: knockback-disabled jutsu damage 4 + grabs/holds target for ~4/5 of 200t lifespan |
| 1.12.2 file:line | `item/ItemMokuton.java:71`; entity `entity/EntityWoodArm.java:35-134` |

1.20.1 status: **DONE**
- Definition: `definition(4, "wood_arm", 'S', 400, 50.0D)` (line ~1895) — chakra 50, XP 400 match.
- Dispatch: `isMokutonWoodArm` (line 1588) → `activateWoodArm` (line 715): findTarget 30, consume 50,
  `WoodArmEntity.spawnFrom`.
- Entity `entity/WoodArmEntity.java`: LIFETIME 200, GROW 20, HOLD = 4/5*200, arm base from owner look,
  lerp tip to target, on reach: ninjutsu damage 4 + grab/hold via teleport + PARALYSIS effect, wood-trail
  BLOCK(OAK_LOG) particles, `woodgrow` sound. Renderer `WoodArmRenderer` draws the WoodSegment chain
  (model + woodblock texture, 5°/segment twist). Faithful.
- Note: original applied `TempData_disableKnockback`+IN_WALL-style hold; port uses `ModEffects.PARALYSIS`
  to immobilize — functionally equivalent.

---

## Hyoton (Ice) — `ItemHyoton`

Right-click gate (1.12.2 line 111): requires Futon + Suiton items in inventory (or creative).
Port: enforced via `AdvancedNatureKind.HYOTON requiredFlags = REQ_FUTON | REQ_SUITON` (line 1849)
plus `tooltip.hyoton.musthave` tooltip.

### KILLSPIKES — "Ice Spike" (ice_spike)
| Field | Value |
|---|---|
| Index | 0 (`KILLSPIKES`) |
| Display key | `ice_spike` |
| Chakra | 100 (`JutsuEnum(0, ..., 'S', 150, 100d, ...)`); power scales 1→80 (right-hold, `getPower(...,1f,80f)`, line 106) |
| XP / rank | rank 'S', XP-to-unlock 150 |
| Cooldown | 0 |
| Entity | `ItemHyoton.EntityIceSpike` (id `narutomod:ice_spike`, ENTITYID 219), extends `EntitySpike.Base`; growTime 10, maxScale 3.0 |
| Procedure | `EntityIceSpike.Jutsu`: rayTrace 30 (must hit BLOCK side UP), spawns `(int)(power*power*5)` spikes at random ground offsets |
| Particles | none from spike directly (model is the spike); growth-area logic |
| Sounds | `narutomod:spiked` (vol 5) |
| Visual | Field of growing translucent ice spikes (ModelSpike, `textures/spike_ice.png`, color 0xC0FFFFFF); each grows to scale 3 over 10t and hits all living within +1 AABB for 30 dmg |
| 1.12.2 file:line | `item/ItemHyoton.java:59`; entity `item/ItemHyoton.java:156-211` (inner) + base `entity/EntitySpike.java` |

1.20.1 status: **DONE**
- Definition: `definition(0, "ice_spike", 'S', 150, 100.0D).withPower(1.0F, 80.0F)` (line ~1853) — match.
- Dispatch: `isHyotonIceSpike` (line 1414) → `activateIceSpike` (line 797): require upward-ground target,
  consume 100*power, `IceSpikeEntity.spawnFrom`.
- Entity `entity/IceSpikeEntity.java`: GROW_TIME 10, MAX_SCALE 3, DAMAGE 30, count `power*power*5`, surface
  search, ICE BLOCK dust particles, `spiked` sound vol 5, +1 inflate damage AABB. Renderer `IceSpikeRenderer`
  + `SpikeModel` (translucent ice). Faithful.

### ICESPEARS — "Ice Spears" (ice_spear)
| Field | Value |
|---|---|
| Index | 1 (`ICESPEARS`) |
| Display key | `ice_spear` |
| Chakra | 20 (`JutsuEnum(1, ..., 'S', 150, 20d, ...)`); power scales 1→40 (right-hold, line 106) |
| XP / rank | rank 'S', XP-to-unlock 150 |
| Cooldown | 0 |
| Entity | `EntityIceSpear.EC` (id `narutomod:ice_spear`, ENTITYID 222), extends `EntitySpike.Base` |
| Procedure | `EntityIceSpear.EC.Jutsu.createJutsu`: fires `(int)(power*3)` no-gravity scale-0.5 spears from eyes+look*1.5, inaccuracy 0.05, speed 0.95 |
| Particles | spike model render; (shatter shards spawned by IceDome break) |
| Sounds | `narutomod:ice_shoot_small` per spear |
| Visual | Volley of small translucent ice spears (ModelSpike scale 0.5, color 0xC0FFFFFF); on hit: SLOWNESS 200t amp1 + 10 jutsu projectile dmg; random in-air wobble (RAND_YAW/PITCH) |
| 1.12.2 file:line | `item/ItemHyoton.java:60`; entity `entity/EntityIceSpear.java:68-173` |

1.20.1 status: **DONE**
- Definition: `definition(1, "ice_spear", 'S', 150, 20.0D).withPower(1.0F, 40.0F)` (line ~1854) — match.
- Dispatch: `isHyotonIceSpear` (line 1420) → `activateIceSpear` (line 584): consume 20*power,
  `IceSpearEntity.spawnFrom`.
- Entity `entity/IceSpearEntity.java`: JUTSU_SCALE 0.5, DAMAGE 10, count `power*3`, no-gravity custom
  travel/accel, tip-collision raycast, on hit: MOVEMENT_SLOWDOWN 200t amp1 + 10 dmg, RAND_YAW/PITCH wobble,
  ice-trail SMOKE_COLORED particles, water bubbles + 0.8 slowdown, `ice_shoot_small` sound,
  `spawnShatteredShard` for dome break. Renderer `IceSpearRenderer` (spike_ice). Faithful (also adds
  `spawnAtTarget` helper used by the dome).

### ICEDOME — "Ice Dome" / Demonic Mirroring Ice Crystals (ice_dome)
| Field | Value |
|---|---|
| Index | 2 (`ICEDOME`) |
| Display key | `ice_dome` |
| Chakra | 5 (`JutsuEnum(2, ..., 'S', 200, 5d, ...)`); upkeep 5/20t (`ItemHyoton.ICEDOME.chakraUsage`); power scales 1→40 |
| XP / rank | rank 'S', XP-to-unlock 200 |
| Cooldown | 0 |
| Entity | `EntityIceDome.EC` (id `narutomod:ice_dome`, ENTITYID 224), extends `EntityShieldBase`; ENTITY_SCALE 8.0 |
| Procedure | `EntityIceDome.EC.Jutsu`: not riding → spawn dome at feet; if riding own dome → `shootSpears()` (100t) |
| Particles | (shatter shards on setDead via EntityIceSpear shards) |
| Sounds | `narutomod:makyohyosho` (cast), `narutomod:ice_formation` (talkTime 26), `narutomod:ice_shoot_small` (break) |
| Visual | Translucent dome shell (ModelDome, `textures/dome_ice.png`, scale 8, alpha-fade-in over growTime+talkTime); ARMOR 100, MAX_HEALTH 400; custom 6-face barrier collision; captures entities inside; rains ice spears at non-allied insiders; LivingHook redirects in-dome attacks to dome and hides riders |
| 1.12.2 file:line | `item/ItemHyoton.java:61`; entity `entity/EntityIceDome.java:75-368`, model `:417-548` |

1.20.1 status: **DONE**
- Definition: `definition(2, "ice_dome", 'S', 200, 5.0D).withPower(1.0F, 40.0F)` (line ~1855) — match.
- Dispatch: `isHyotonIceDome` (line 1426) → `activateIceDome` (line 602): consume 5*power,
  `IceDomeEntity.spawnOrTrigger` (spawn, or shootSpears if already inside own dome).
- Entity `entity/IceDomeEntity.java`: WIDTH 9.6 / HEIGHT 6.4 (1.2*8 / 0.8*8), TALK_TIME 26, GROW+TALK 56,
  SHOOT 100t, UPKEEP 5/20t, MAX_HEALTH 400, damage absorbed at 0.2x, inside-capture, 6-face barrier
  collision (keepInside/pushOutside), rains `IceSpearEntity` at inside members, shard-burst on break,
  ignored damage types + IceSpear immunity, `ForgeEvents.onLivingAttack` redirects cross-boundary/owner
  attacks to dome. Sounds makyohyosho/ice_formation/ice_shoot_small. Renderer `IceDomeRenderer` +
  `IceDomeModel` (alpha-fade dome). Faithful.

### ICEPRISON — "Ice Prison" (ice_prison)
| Field | Value |
|---|---|
| Index | 3 (`ICEPRISON`) |
| Display key | `ice_prison` |
| Chakra | 50 (`JutsuEnum(3, ..., 'S', 150, 50d, ...)`); power scales 1→40 |
| XP / rank | rank 'S', XP-to-unlock 150 |
| Cooldown | 0 |
| Entity | `EntityIcePrison.EC` (id `narutomod:ice_prison`, ENTITYID 226), plain Entity, empty renderer |
| Procedure | `EntityIcePrison.EC.Jutsu` → `objectEntityLookingAt(entity, 10d, true)`, needs EntityLivingBase |
| Particles | none (ICE blocks are the effect) |
| Sounds | `narutomod:ice_shoot` (cast) |
| Visual | Encases target in a growing Blocks.ICE shell (octant `tpos` sweep, radius=width*0.5+1, height+1); holds target + MINING_FATIGUE 600t amp1 every 4t; auto-revert after 1200t |
| 1.12.2 file:line | `item/ItemHyoton.java:62`; entity `entity/EntityIcePrison.java:67-164` |

1.20.1 status: **DONE**
- Definition: `definition(3, "ice_prison", 'S', 150, 50.0D).withPower(1.0F, 40.0F)` (line ~1856) — match.
- Dispatch: `isHyotonIcePrison` (line 1432) → `activateIcePrison` (line 619): require living target in sight,
  consume 50*power, `IcePrisonEntity.spawnFrom`.
- Entity `entity/IcePrisonEntity.java`: same octant sweep algorithm (POSITION_MULTIPLIERS), radius/height
  match, holds target, DIG_SLOWDOWN 600t amp1 every 4t, ICE blocks 3/tick, ICE BLOCK particles, `ice_shoot`
  sound, 1200t expiry + auto-revert on remove. Renderer = `NoopEntityRenderer` (matches empty original).
  Faithful (port reimplements the full build plan eagerly into a list).

---

## Ice Senbon (auxiliary tool) — `ItemIceSenbon`

NOT a jutsu but tightly coupled: auto-granted to Hyoton users (see Hyoton passive).

| Field | Value |
|---|---|
| Registry | `narutomod:ice_senbon`, creativeTab null (hidden) |
| Stats | maxDamage 100, maxStackSize 1, full3D, enchantability 0, destroySpeed 4, canHarvest any |
| Attributes | MAINHAND ATTACK_DAMAGE +6, ATTACK_SPEED -3 |
| On hit | `ProcedureIceSenbonLivingEntityIsHitWithTool` → SLOWNESS 200t amp1 (server-side); damages stack 1 |
| 1.12.2 file:line | `item/ItemIceSenbon.java:30-110`; procedure `procedure/ProcedureIceSenbonLivingEntityIsHitWithTool.java` |

1.20.1 status: **DONE**
- Port `item/IceSenbonItem.java`: durability 100, MAINHAND ATTACK_DAMAGE +6 / ATTACK_SPEED -3, destroySpeed
  4.0, `hurtEnemy` applies MOVEMENT_SLOWDOWN 200t amp1 (server) + damages stack 1, `mineBlock` damages 1.
  Registered `ModItems.ICE_SENBON`. Faithful (the separate procedure is inlined). Verify item model/texture
  `ice_senbon` JSON exists.

---

## Cross-cutting gaps / risks (to verify in the port)

1. Hyoton passive FrostWalker freeze-trail under the user is NOT reproduced in `Passive.HYOTON`
   (cosmetic; everything else of the passive matches).
2. WOODHOUSE depends on the structure template `narutomod:wood_house_2` being shipped under the
   1.20.1 datapack (`data/narutomod/structures/wood_house_2.nbt` or `structure/`). Port emits a
   "Missing structure template" message if absent — confirm the asset exists.
3. WoodGolem: the elaborate `ModelWoodGolem` dragon back-ornament + first-person head/dragon hiding +
   `EntityShieldBase` steer/shield semantics are approximated in the PathfinderMob port; confirm the
   `WoodGolemModel` reproduces the dragon piece and the lightmap-glow headwear if exact parity is wanted.
4. All listed jutsu lost their original built-in cooldown of 0 — port relies on chakra/XP only, same as
   original (no regression).
5. Verify the assorted custom sounds resolve in the 1.20.1 `ModSounds` registry:
   `woodgrow`, `woodspawn`, `mokujin_no_jutsu`, `spiked`, `ice_shoot`, `ice_shoot_small`,
   `ice_formation`, `makyohyosho` (all referenced by the ports above).
