# Doton (Earth Release) — Jutsu Audit (1.12.2 → 1.20.1)

Faithful inventory of every Doton technique, comparing the original 1.12.2 Forge source to the
current 1.20.1 Forge port. Citations are `file:line` against the original unless noted.

## Framework / Notes

### 1.12.2 structure
- Tome item: `item/ItemDoton.java`. Registers 5 jutsu as `ItemJutsu.JutsuEnum` constants
  (`ItemDoton.java:62-66`), each carrying `(index, registryName, rank-char, chakraUsage, IJutsuCallback)`.
  The item is `RangedItem extends ItemJutsu.Base` of type `JutsuEnum.Type.DOTON` (`ItemDoton.java:87-89`).
- Charge/power: `RangedItem.getPower(...)` maps each jutsu to `(base, divisor)` pairs
  (`ItemDoton.java:97-113`); `getMaxPower(...)` caps EARTH_WALL→50, SANDWICH→8 (`ItemDoton.java:115-125`).
  HIDINGINROCK suppresses the held-tick channel via `onUsingTick` override (`ItemDoton.java:127-132`).
- Two entities are defined INLINE in the tome: `EntityHidingInRock` (`ItemDoton.java:139-226`) and
  `EntityEarthWall` (`ItemDoton.java:228-433`); registered at ids `ENTITY2ID=10134` and `ENTITYID=134`
  (`ItemDoton.java:59-60, 75-78`).
- Three jutsu reference external entity files: `EntityEarthSandwich` (id 177), `EntitySwampPit`
  (id 191), `EntityEarthSpears` (id 243).
- Earthen-material test = GROUND, ROCK, SAND, CLAY (`ItemDoton.java:61, 135-137`).
- Scroll/learn items (unlock the jutsu on the Doton tome via a learning GUI): `ItemScrollHidingInRock`
  (C-rank), `ItemScrollEarthWall` (B), `ItemScrollEarthSandwich` (B), `ItemScrollSwampPit` (A),
  `ItemScrollEarthSpears` (C). Each opens a `GuiScroll*Gui` on right-click. These five scroll classes
  contain NO mechanics — they only gate learning and set a tooltip.

### 1.20.1 structure
- Tome item: `item/DotonItem.java` (`JutsuItem`, type `DOTON`). 5 `JutsuDefinition` constants with
  matching index/rank/chakra and `.withPower(base, divisor)` (`DotonItem.java:24-40`).
- Charge/power port: `getDotonPower(...)` + `getMaxDotonPower(...)` replicate base/divisor and the
  EARTH_WALL→50 / SANDWICH→8 caps (`DotonItem.java:269-295`); creative gets full power.
- Entities: `HidingInRockEntity`, `EarthWallEntity`, `EarthSandwichEntity`, `SwampPitEntity`,
  `EarthSpearsEntity`, plus shared helpers `SpikeEntity` (projectile base) and `EarthBlocksEntity`
  (moving-block render container). All registered in `registry/ModEntityTypes.java`
  (lines 160-162, 198-199, 297, 304).
- Renderers in `client/renderer/`: `SpikeRenderer` (texture `spike.png`), `EarthSpearRenderer`
  (`spike_stone.png`, extends `LegacySpikeModelRenderer`), `EarthBlocksRenderer` (renders block states).
  Earth Wall / Earth Sandwich / Swamp Pit / Hiding-in-Rock use `NoopEntityRenderer` (invisible
  controller entities — correct, they only place/move blocks or particles). Registered in
  `client/ClientModEvents.java:263-384`.
- Scroll learning ported via the data-driven `item/JutsuScrollDefinition.java` enum: `HIDING_IN_ROCK`,
  `EARTH_SPEARS`, `EARTH_WALL`, `SWAMP_PIT`, `EARTH_SANDWICH`, each mapping the scroll translation key
  to `ModItems.DOTON` + the matching `DotonItem` definition (lines 227-266). Replaces the per-scroll
  `ItemScroll*` + `GuiScroll*Gui` plumbing.
- Sounds: `ModSounds.SOUND_JUTSU`, `SOUND_ROCKS`, `SOUND_HAND_PRESS`, `SOUND_SANDO_NO_JUTSU`,
  `SOUND_YOMINUMA` are all referenced and present.

### Overall verdict
All 5 Doton jutsu are ported with faithful mechanics, entities, renderers, registry, scrolls, sounds
and particles. No MISSING / STUB items. Remaining gaps are small behavioral nuances (see per-jutsu
"remaining" notes), not absent features.

---

## Hiding in Rock (Iwagakure no Jutsu — "entityhidinginrock")

| Field | Value |
|---|---|
| Index | 0 |
| Chakra | 10.0 (per-second drain while active) |
| Rank | C |
| Power (base/div) | 1.12.2: not charged (instant on release; `getPower` returns base 2). 1.20.1: `.withPower(2.0,50.0)` (unused — instant) |
| Cooldown | Default jutsu cooldown map (none special set) |
| Entity | `ItemDoton.EntityHidingInRock` (id 10134) — invisible tracker, follows user |
| Procedures | `ProcedureOnLivingUpdate.isNoClip/setNoClip`; `Chakra.pathway(...).consume` |
| Particles | none |
| Sounds | `narutomod:jutsu` on activation (`ItemDoton.java:218-219`) |
| Visual | No model. Sets user intangible/noclip (phase through earth). Status message `chattext.intangible<bool>` |
| 1.12.2 file:line | `ItemDoton.java:62, 139-226` |

Behavior: on use, if user is NOT already noclip, play `jutsu`, spawn `EntityHidingInRock` bound to user.
Each tick the entity snaps to user pos, sets noclip; every 20 ticks consumes 10 chakra; dies if user
leaves earthen material after `waitTime=60` ticks, or runs out of chakra/dies (`ItemDoton.java:184-203`).
On death restores tangibility (`ItemDoton.java:158-164`).

1.20.1 status: DONE. `HidingInRockEntity` reproduces owner-follow, 60-tick grace, 20-tick 10-chakra
drain, earthen-block test (DIRT/STONE/SAND/CLAY/GRAVEL/TERRACOTTA tags), noclip via
`NarutomodModVariables.NO_CLIP_FLAG` + `ProcedureSync`, and the `chattext.intangible` message
(`HidingInRockEntity.java:59-197`). `DotonItem.activateHidingInRock` plays `SOUND_JUTSU`, guards against
double-activation (`DotonItem.java:84-106`).
- Remaining (minor): port adds `maintainOwnerMotion` (gentle sink/hold) not in original — acceptable
  QoL, verify it doesn't fight other movement code. Activation path consumes 1 tick of chakra up front
  in the item, whereas original relied purely on the entity's 20-tick drain; functionally equivalent.

---

## Earth Wall (Doryūheki — "entityearthwall")

| Field | Value |
|---|---|
| Index | 1 |
| Chakra | 20.0 (× power) |
| Rank | B |
| Power (base/div) | base 2, divisor 15 (`ItemDoton.java:101`); min activation 5; max 50 |
| Cooldown | default |
| Entity | `ItemDoton.EntityEarthWall` (id 134) — invisible builder, raises a wall of copied earthen blocks |
| Procedures | `ProcedureUtils.raytraceBlocks` (30d); `ProcedureUtils.BB.getCenter`; `event.EventSetBlocks` (delayed cleanup) |
| Particles | `BLOCK_DUST` (id of source block) 5 per placed block, speed 0.15 (`ItemDoton.java:326-328`) |
| Sounds | `narutomod:rocks` every 30 ticks while building, vol 5, pitch 0.3-0.8 (`ItemDoton.java:315-316`) |
| Visual | No model — places real block states (neighbor earthen block, ores/bedrock→STONE) layer by layer up to wallHeight; pushes entities up; reverts after `removeTime` (1200 ticks) |
| 1.12.2 file:line | `ItemDoton.java:63, 228-433` |

Behavior: raytrace block at ≤30; spawn wall centered there, yaw = user facing, width = power, height =
0.6·width, thickness = 0.25·width, auto-remove. Collects base positions along a line, raises them
`blockChunk=128`/tick, copying the neighboring earthen block state, dusting each, moving up colliding
entities +1.5 (`ItemDoton.java:305-340`). On death schedules all placed blocks → AIR via `EventSetBlocks`.

1.20.1 status: DONE. `EarthWallEntity` reproduces line collection, 128 blocks/tick layered build,
neighbor-earthen copy with bedrock/ore→STONE normalization, BLOCK particle dust, `SOUND_ROCKS` every 30
ticks, entity push-up, and timed cleanup (1200 ticks) including NBT persistence of build/placed lists
(`EarthWallEntity.java:34-352`). Item activation enforces min power 5, raytrace + buildable checks,
chakra `20×power` (`DotonItem.java:131-156`).
- Remaining (minor): original used `EventSetBlocks` (a scheduled global revert that survives entity
  death); port reverts via the entity's own `cleanupPlacedBlocks` on remove/timeout and persists state
  to NBT instead — behaviorally close, but if the entity is force-removed without `shouldDestroy`,
  blocks could linger. Entity push-up offset is +2.5 (port) vs +1.5 (original) — cosmetic.

---

## Earth Sandwich (Doton: Dosekiryū / Earth Release Sandwich — "earth_sandwich")

| Field | Value |
|---|---|
| Index | 2 |
| Chakra | 100.0 (× power) |
| Rank | B |
| Power (base/div) | base 2, divisor 75 (`ItemDoton.java:104`); min activation 2; max 8 |
| Cooldown | default |
| Entity | `EntityEarthSandwich.EC` (id 177); spawns TWO `EntityEarthWall` (non-auto) then crushes them inward |
| Procedures | `ProcedureUtils.objectEntityLookingAt` (30d); `EntityEarthBlocks.BlocksMoveHelper` (move/collide/break); `ProcedureUtils.breakBlockAndDropWithChance` |
| Particles | inherited from the two child Earth Walls (BLOCK_DUST) |
| Sounds | `narutomod:sando_no_jutsu` if power≥8 (vol 5), else `narutomod:jutsu` (vol 1) (`EntityEarthSandwich.java:149-154`) |
| Visual | Two earth walls form on either side of target, then slide together (moving block helper) and slam, breaking blocks and dealing FALLING_BLOCK damage = collisionForce×4 |
| 1.12.2 file:line | `ItemDoton.java:64`; `entity/EntityEarthSandwich.java:1-163` |

Behavior: raytrace a LIVING target ≤30. Snap yaw to nearest cardinal. Spawn two `EntityEarthWall`
(non-auto, width/height = power, thickness 0.6·width) offset ±90° at ±width (`EntityEarthSandwich.java:54-67`).
Once both walls report `isDone()`, convert their blocks to a `BlocksMoveHelper` and push them toward
each other at 0.15/tick for 100 ticks (`EntityEarthSandwich.java:92-125`); colliding blocks broken,
entities hit with `DamageSource.FALLING_BLOCK`, force×4. On death drops the helper blocks (`fall()`).

1.20.1 status: DONE. `EarthSandwichEntity` raytraces an entity target, snaps yaw, spawns two
non-auto `EarthWallEntity` (width=power, height=0.6·width), waits for both `isDone()`, then converts
placed blocks to `FallingBlockEntity` (no-gravity, driven), slides inward 0.15/tick for 100 ticks,
breaks obstacles, and deals `ModDamageTypes.ninjutsu` ≈ `motion·mass·4`. Plays `SANDO_NO_JUTSU`/`JUTSU`
by the power≥8 split, with the pre-spawn buildability check (`EarthSandwichEntity.java:50-374`,
`DotonItem.java:180-205`).
- Remaining (minor): original used a custom `BlocksMoveHelper` (rigid block cluster); port uses
  individual driven `FallingBlockEntity`s — visually equivalent moving stone, but collision/crush mass
  and knockback (`push`) are approximated rather than the original `collisionForce()` model. Damage
  source differs (FALLING_BLOCK → ninjutsu) — intentional 1.20.1 damage-type system.

---

## Swamp Pit (Yomi Numa / Swamp of the Underworld — "swamp_pit")

| Field | Value |
|---|---|
| Index | 3 |
| Chakra | 100.0 (× power) |
| Rank | A |
| Power (base/div) | base 1, divisor 30 (`ItemDoton.java:107`); used as integer radius |
| Cooldown | default |
| Entity | `EntitySwampPit.EC` (id 191) — invisible; carves a pit and fills with `BlockMud` |
| Procedures | `ProcedureUtils.raytraceBlocks` (50d); `Particles.spawnParticle` |
| Particles | `Particles.Types.SMOKE`, 100 per ring, color `0x801c120d`, scale 25 (`EntitySwampPit.java:80-83`) |
| Sounds | `narutomod:yominuma`, PLAYERS, vol 1 (`EntitySwampPit.java:112-113`) |
| Visual | Brown smoke billows; a square pit of radius `power` is excavated downward each tick and replaced with mud (`BlockMud`); entities sink |
| 1.12.2 file:line | `ItemDoton.java:65`; `entity/EntitySwampPit.java:1-122` |

Behavior: raytrace block ≤50 (non-MISS). Center = hit + radius up. Computes `offsetY` (how deep the
air column goes). Each tick: spawn radius×100 brown smoke particles above; for the current layer
`1 - ticksExisted`, set air above `offsetY` else `BlockMud` (`EntitySwampPit.java:78-97`). Dies when
`ticksExisted >= radius - offsetY`.

1.20.1 status: DONE. `SwampPitEntity` reproduces center+radius (clamped ≤32), `offsetY` air-column
computation, per-tick layer carve (air above offset, `ModBlocks.MUD` below), brown smoke via
`ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0x801C120D, 25,...)` count radius×100, and
`SOUND_YOMINUMA`, dying at `radius - offsetY` (`SwampPitEntity.java:27-158`, `DotonItem.java:158-178`).
- Remaining: none of note. Confirm `ModBlocks.MUD` is the faithful port of `BlockMud` (mud that drags
  entities down) — texture/behavior parity not re-verified in this audit.

---

## Earth Spears (Doryūsō / Earth Release: Earth-Style Rampart — "earth_spears")

| Field | Value |
|---|---|
| Index | 4 |
| Chakra | 50.0 (× power) |
| Rank | C |
| Power (base/div) | base 0.5, divisor 150 (`ItemDoton.java:110`) |
| Cooldown | default |
| Entity | `EntityEarthSpears.EC extends EntitySpike.Base` (id 243); many spikes, grow then deal damage |
| Procedures | `world.rayTraceBlocks` (30d, must hit UP face); `ItemJutsu.causeJutsuDamage` |
| Particles | `BLOCK_DUST` of STONE, 6 per spike during grow (`EntityEarthSpears.java:76-77`) |
| Sounds | `narutomod:hand_press`, BLOCKS, vol 5, pitch 0.8-1.2 (`EntityEarthSpears.java:98-100`) |
| Visual | Stone spike model (`EntitySpike.ModelSpike`), texture `spike_stone.png`, grows to scale 2.0 over 8 ticks; field of `power²·5` spikes scattered in `±power·3` radius on solid ground |
| 1.12.2 file:line | `ItemDoton.java:66`; `entity/EntityEarthSpears.java:1-116`; base `entity/EntitySpike.java` |

Behavior: raytrace ≤30 hitting an UP block face. Play `hand_press`. Spawn `(int)(power²·5)` spikes;
each placed at a random offset within `±power·3`, snapped to the ground surface, random yaw and slight
pitch (`EntityEarthSpears.java:101-108`). Each spike grows scale 0→2.0 over `growTime=8`, dusting STONE
particles, damaging all living within `grow(1,0,1)` (except shooter) with `causeJutsuDamage` = `power`,
resetting hurtResistantTime (`EntityEarthSpears.java:71-88`).

1.20.1 status: DONE. `EarthSpearsEntity` raytraces an UP face ≤30, plays `SOUND_HAND_PRESS`, spawns
`max(power²·5,1)` spikes scattered `±power·3`, snapped to surface with random yaw/pitch; each grows
0→2.0 over 8 ticks, dusts STONE BLOCK particles, and damages nearby living (`invulnerableTime=0`,
`ModDamageTypes.ninjutsu`, amount=power) excluding owner/vehicle. Renders with `EarthSpearRenderer`
(`spike_stone.png`) via the `LegacySpikeModelRenderer` port of `ModelSpike`
(`EarthSpearsEntity.java:35-237`, `DotonItem.java:108-129`). Synced SCALE drives dimensions/render.
- Remaining (minor): `SpikeEntity` (the shared base) carries full projectile travel logic (used by
  other spike jutsu); `EarthSpearsEntity` is a standalone Entity that only grows in place — matches
  the original `EC` which overrode update to grow rather than fly, so this is faithful. Verify
  `spike_stone.png` texture asset is present (the legacy renderer references it).

---

## Cross-reference: shared assets the Doton domain needs

- Entities (1.20.1): `HidingInRockEntity`, `EarthWallEntity`, `EarthSandwichEntity`, `SwampPitEntity`,
  `EarthSpearsEntity`, `SpikeEntity`, `EarthBlocksEntity` — all registered (`ModEntityTypes` 160-162,
  198-199, 297, 304).
- Renderers: `SpikeRenderer`, `EarthSpearRenderer`, `EarthBlocksRenderer`; `NoopEntityRenderer` for the
  invisible controllers (`ClientModEvents` 263-384).
- Models/textures: `LegacySpikeModelRenderer` (port of `ModelSpike`); textures `spike.png`,
  `spike_stone.png` present under `assets/narutomod/textures/`.
- Sounds: `SOUND_JUTSU`, `SOUND_ROCKS`, `SOUND_HAND_PRESS`, `SOUND_SANDO_NO_JUTSU`, `SOUND_YOMINUMA`.
- Particles: `BlockParticleOption(ParticleTypes.BLOCK, state)` (wall/spears dust);
  `NarutoParticleKind.SMOKE_COLORED` (swamp).
- Block: `ModBlocks.MUD` (port of `BlockMud`) for Swamp Pit.
- Learning: `JutsuScrollDefinition` entries HIDING_IN_ROCK / EARTH_SPEARS / EARTH_WALL / SWAMP_PIT /
  EARTH_SANDWICH map scrolls → `ModItems.DOTON` (replaces 5 `ItemScroll*` + `GuiScroll*Gui`).

No unresolved references encountered.
