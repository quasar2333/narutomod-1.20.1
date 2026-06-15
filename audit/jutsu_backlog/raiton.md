# Raiton (Lightning Release) — Jutsu Audit (1.12.2 → 1.20.1)

Faithful inventory of every jutsu in the Raiton domain, comparing the read-only 1.12.2 source to the
work-in-progress 1.20.1 port. Goal: a developer can restore every technique (mechanics AND visuals)
without missing any. Line citations are to the **1.12.2** files unless stated otherwise.

## Framework / how Raiton is wired

### 1.12.2 (original)
- **Tome item:** `item/ItemRaiton.java`. Registry name `raiton`, `ItemJutsu.Base` of type `JutsuEnum.Type.RAITON`.
  Five `ItemJutsu.JutsuEnum` entries (indexes 0-4) declared at `ItemRaiton.java:46-50`:
  - `CHIDORI`  = index 0, rank `'A'`, chakra `EntityChidori.CHAKRA_USAGE` (=150d), callback `EntityChidori.EC.Jutsu`.
  - `CHAKRAMODE` = index 1, rank `'B'`, chakra `10d` (per-second burn), callback `EntityChakraMode.Jutsu` (inner class of `ItemRaiton`).
  - `CHASINGDOG` (Lightning Beast) = index 2, rank `'C'`, chakra `20d`, callback `EntityLightningBeast.EC.Jutsu`.
  - `GIAN` (False Darkness) = index 3, rank `'B'`, chakra `100d`, callback `EntityFalseDarkness.EC.Jutsu`.
  - `KIRIN` = index 4, rank `'S'`, chakra `1500d`, callback `EntityKirin.EC.Jutsu`.
- **Power/charge curves** in `ItemRaiton.RangedItem.getPower` (`ItemRaiton.java:78-90`): CHASINGDOG `getPower(...,5f,30f)`, GIAN `getPower(...,1f,150f)`, KIRIN `getPower(...,0f,400f)`. `getMaxPower` caps KIRIN at 1.0 (`:92-99`).
- **Charging hooks:** `onUsingTick` calls `EntityKirin.chargingEffects` server-side for KIRIN (`:101-107`). `onItemRightClick` calls `EntityKirin.startWeatherThunder` on KIRIN success (`:109-116`).
- **Shared bolt entity:** `EntityLightningArc.Base` — a non-rendering-model lightning bolt rendered via custom GL `Render<Base>` (recursive fractal line, `EntityLightningArc.java:263-349`). `onStruck` static applies damage + vanilla `EntityLightningBolt` visual + `PotionParalysis` (`:68-78`). Spawns are registered as the `lightning_arc` entity.
- **Scrolls (learning GUIs):** `ItemScrollChidori` ("A-rank"), `ItemScrollLightningBeast` ("C-rank"), `ItemScrollLightningChakraMode` ("B-rank"), `ItemScrollFalseDarkness` ("B-rank"). Each is a plain `Item` whose `onItemRightClick` opens a `GuiScroll...Gui` to teach the jutsu. **No Kirin scroll exists** (S-rank, taught by other means).

### 1.20.1 (port)
- **Tome item:** `item/RaitonItem.java` extends `JutsuItem` (type `JutsuType.RAITON`). Same five `JutsuDefinition`s, same indexes/ranks/chakra/power curves (`RaitonItem.java:32-43`). Charge/release handled in `use`/`onUseTick`/`releaseUsing` (`:49-106`). KIRIN weather + charging effects wired (`:55-57`, `:68-70`). Cooldown writer `setChakraModeCooldown` (`:108-111`).
- **Entities registered** in `registry/ModEntityTypes.java`: `CHIDORI` (:157), `CHIDORI_SPEAR` (:158), `FALSE_DARKNESS` (:209), `KIRIN` (:244), `LIGHTNING_ARC` (:250), `LIGHTNING_BEAST` (:251), `RAITONCHAKRAMODE` (:274).
- **Renderers registered** in `client/ClientModEvents.java`: CHIDORI + CHIDORI_SPEAR → `ChidoriRenderer` (:260-261); FALSE_DARKNESS → `NoopEntityRenderer` (:284); KIRIN → `KirinRenderer` (:325); LIGHTNING_BEAST → `LightningBeastRenderer` (:330); LIGHTNING_ARC → `LightningArcRenderer` (:331); RAITONCHAKRAMODE → `NoopEntityRenderer` (:354). Models: `client/model/KirinModel.java`, `client/model/LightningBeastModel.java`.
- **Scrolls/learning:** `item/JutsuScrollDefinition.java` defines CHIDORI, LIGHTNING_CHAKRA_MODE, LIGHTNING_BEAST, FALSE_DARKNESS scroll entries (`:115-146`), each mapping to the matching `RaitonItem` jutsu. **Kirin has no scroll** (matches original).
- **Shared bolt:** `entity/LightningArcEntity.java` + `client/renderer/LightningArcRenderer.java` faithfully reproduce the fractal-line bolt (same per-section alpha 0xF0/0x80/0x20, white core, recursive depth, branch chance 1/5). `configureBetween`/`configureRandom` mirror the legacy constructors. `onStruck` applies damage + visual `LightningBolt` + Paralysis (`LightningArcEntity.java:235-255`).

**Overall:** This domain is unusually complete. All 5 jutsu have ported mechanics, entities, renderers/models, and (for the 4 non-S jutsu) scroll learning. Remaining gaps are small fidelity items, noted per jutsu.

---

## Chidori (千鳥 "One Thousand Birds") — Lightning Cutter

| Field | Value |
|---|---|
| Display name | `chidori` (index 0) — `ItemRaiton.java:46` |
| Index / rank | 0 / `'A'` |
| Chakra | Activation `EntityChidori.CHAKRA_USAGE` = 150d (`EntityChidori.java:60`); burn 40d/sec (`CHAKRA_BURN`, `:61`), consumed every 20 ticks (`:163`) |
| Level/xp | Duration scales with ninja level: `ninjalevel * 5 / xpModifier` ticks (`EntityChidori.java:262`). Damage multiplier `ninjaLevel/25` (`:226-231`) |
| Cooldown | None set on Chidori itself (commented out `defaultCooldownMap` in `ItemRaiton.java:75`) |
| Entities | `EntityChidori.EC` (held chidori, id `chidori`=141), `EntityChidori.Spear` (thrust, id `chidori_spear`=142) — `EntityChidori.java:69-72` |
| Procedures | `ProcedureLightSourceSetBlock` (light, `:173`), `ProcedureSync.EntityNBTTag`/`forceBowPose` (arm pose, `:147,158`), `ProcedureRenderView.setFOV` (lunge FOV, `:205`), `ProcedureUtils.objectEntityLookingAt/setVelocity/isWeapon/getModifiedAttackDamage/getAttackSpeed` |
| Particles | `Particles.Types.SMOKE` white puffs at hand (`:370`); `EntityLightningArc.spawnAsParticle` blue arcs (`:373,384,394`) |
| Sounds | `narutomod:chidori` on cast (`:256-258`); `narutomod:electricity` per-tick crackle (`:169,298`) |
| Visual | `RenderChidori` (`Render<EC>`, `:334-416`): no entity model — it reads the owner's biped arm `ModelRenderer` angles and spawns a ball of white smoke + blue fractal `EntityLightningArc` particles at the hand in 3rd person. Empty hand vs weapon-in-hand branches; forces a bow/aim arm pose. Mining-fatigue applied to owner. On mature swing within 5 blocks: `EntityLightningArc.onStruck` (lightning-bolt strike + paralysis + 25× or weapon×1.3 damage). Auto-launch toward looked-at target every 6 ticks. **Spear** thrust: 6-block forward blue arc (`0x800000FF`) + occasional bright arc; if sneaking, `ryu` mode = radial bolts to all nearby entities + random vertical bolt (`:301-327`). |
| 1.12.2 file:line | `item/ItemRaiton.java:46`; `entity/EntityChidori.java` (whole file) |

**1.20.1 status: DONE (minor fidelity gaps).**
- Ported: `entity/ChidoriEntity.java`, `entity/ChidoriSpearEntity.java`, `client/renderer/ChidoriRenderer.java`, `network/ChidoriHandPositionMessage.java`. Activation/spear-on-second-press in `RaitonItem.activateChidori` (`RaitonItem.java:123-154`). Same 150 chakra, 40/s burn (`ChidoriEntity.java:42-43`), duration formula (`RaitonItem.chidoriDuration:308-311`), damage multiplier, mining-fatigue, light source (`LightSourceBlock.setOrRefresh`), forceBowPose sync, auto-launch, swing damage via `LightningArcEntity.onStruck`. Spear normal + `ryu` modes ported (`ChidoriSpearEntity.java:187-221`).
- Remaining nuances to verify (not blocking):
  1. Empty-hand hand arcs/smoke: original spawned them **per-render-frame** in `RenderChidori.doRender` using live arm-model angles; port spawns smoke + arcs **server-side in `ChidoriEntity.spawnFeedback`** at a look-vector-derived point, with the client only sending an exact hand position for the weapon-spark case (`ChidoriRenderer.computeThirdPersonHandPosition`). Visually close but the empty-hand arc origin is approximate when no client packet has arrived.
  2. `ProcedureRenderView.setFOV` (FOV punch on lunge, original `:205`) is **not** reproduced in the port's `launchAtTarget` (`ChidoriEntity.java:295-298`). Minor visual.
  3. Texture: port uses `textures/white_square.png` for the renderer base (`ChidoriRenderer.java:33`); arcs drawn additively (matches original GL).

---

## Lightning Release Chakra Mode (雷遁チャクラモード) — "raitonchakramode"

| Field | Value |
|---|---|
| Display name | `raitonchakramode` (index 1) — `ItemRaiton.java:47` |
| Index / rank | 1 / `'B'` |
| Chakra | `CHAKRAMODE.chakraUsage` = 10d burned **per second** (every 20 ticks) — `ItemRaiton.java:120,174-178` |
| Level/xp | None to cast; cooldown = active-ticks × item modifier (`setNewCooldown`, `:204-215`) |
| Cooldown | Dynamic: `ticksExisted * item.getModifier` written on death (`:211-212`) |
| Entities | `ItemRaiton.EntityChakraMode` (registry `raitonchakramode`, id 129) — registered in `ItemRaiton.initElements` (`:59-60`). No-render placeholder entity stuck to summoner |
| Procedures | `Chakra.pathway().consume`, `ProcedureUtils.getItemStackIgnoreDurability` (`:207`) |
| Particles | `EntityLightningArc.spawnAsParticle` red arcs around body (`:189-190`); `Particles.Types.SMOKE` colored `0x2080D0FF` (`:193-197`) |
| Sounds | `narutomod:electricity` (`:185-186`) |
| Visual | No model/renderer — pure aura: arcs + blue smoke emitted around the user. Buffs while active (each 20t, `:178-181`): RESISTANCE amp3, SPEED amp32, STRENGTH amp `9(+strength)`, JUMP_BOOST amp6. Toggle on/off (second cast kills entity, `Jutsu.createJutsu:225-240`). |
| 1.12.2 file:line | `item/ItemRaiton.java:47, 119-241`; scroll `item/ItemScrollLightningChakraMode.java` |

**1.20.1 status: DONE.**
- Ported as `entity/RaitonChakraModeEntity.java`. Same 10/s burn (`:34,155`), same buffs DAMAGE_RESISTANCE 3 / MOVEMENT_SPEED 32 / DAMAGE_BOOST `9(+)` / JUMP 6 (`:158-161`), every-20t-offset-2 cadence (`:100`), red arcs + `0x2080D0FF` smoke feedback (`:185-199`), electricity sound (`:170-175`), toggle via persistent `EntityChakraModeIdKey` (`:33`, `RaitonItem.toggleChakraMode:156-176`), dynamic cooldown on remove (`writeCooldown:207-226`, `RaitonItem.setChakraModeCooldown:108-111`).
- Renderer: `NoopEntityRenderer` (correct — original had no renderer either). Scroll learning present (`JutsuScrollDefinition LIGHTNING_CHAKRA_MODE`).

---

## Lightning Beast Running Technique / Chasing Dog (雷獣走り) — "lightning_beast" / CHASINGDOG

| Field | Value |
|---|---|
| Display name | `lightning_beast` (index 2), internal const `CHASINGDOG` — `ItemRaiton.java:48` |
| Index / rank | 2 / `'C'` |
| Chakra | `20d` (`ItemRaiton.java:48`). Charge power `getPower(...,5f,30f)` → min 5, max 30 (`:81-83`) |
| Level/xp | Power = charge; lifetime = `power*20` ticks (`EntityLightningBeast.java:204`); attack damage = `power` |
| Cooldown | None explicit |
| Entities | `EntityLightningBeast.EC` extends `EntityTameable` (registry `lightning_beast`, id 189) — `:59-60` |
| Procedures | `ProcedureUtils.raytraceBlocks` (spawn pos, `:87`), `getModifiedSpeed`, `ItemJutsu.causeJutsuDamage`; AI: `EntityAISwimming` only (`:106`) |
| Particles | `EntityLightningArc.Base` from owner-eyes to beast every 4t (tether, `:192-195`); `EntityLightningArc.spawnAsParticle` body arcs (`:201-202`) |
| Sounds | `narutomod:electricity` (`:197`); wolf ambient/hurt/death (`:110-122`); soundVolume 2 |
| Visual | `RenderCustom extends RenderLiving` (`:240-264`) using inner `ModelLightningWolf` (`:315-394`, scale 2.0). Additive blend, fade-in alpha `ticksExisted/60`, full-bright. `LayerLightningCharge` (`:267-312`): second `electric_armor.png` pass with scrolling UV, flickers every 20t. Beast: ARMOR 100, HEALTH 1000, SPEED 1.6, follow 64; immune to damage (`attackEntityFrom`→false); melee applies `PotionParalysis` 200/2 + `power` damage on collide (`:146-151,210-215`); homes to owner's looked-at block (`findDestination/updateAITasks:153-187`). |
| 1.12.2 file:line | `item/ItemRaiton.java:48, 78-83`; `entity/EntityLightningBeast.java` (whole file); scroll `item/ItemScrollLightningBeast.java` |

**1.20.1 status: DONE (verify charge fade-in).**
- Ported `entity/LightningBeastEntity.java` (extends `PathfinderMob`), `client/renderer/LightningBeastRenderer.java`, `client/model/LightningBeastModel.java`. Same attributes (ARMOR 100/HEALTH 1000/SPEED 1.6/FOLLOW 64, `:57-63`), `hurt`→false invulnerable (`:142-144`), paralysis-on-touch + `power` damage (`:261-273`), destination homing (`findDestination/moveTowardDestination:196-226`), owner tether arc every 4t + local body arcs (`:228-252`), electricity sound (`:254-259`), lifetime `power*20` (`:122`). Activation in `RaitonItem.activateLightningBeast` (`:178-198`), chakra `20*power`. Scroll learning present.
- Render: additive `energyAdditive` body pass + `scrollingAdditive` charge pass with flicker (`LightningBeastRenderer.java:48-75`) — reproduces the `LayerLightningCharge` scrolling-UV electric overlay. Body fade-in alpha `age/60` ported (`:39`). Charge-flicker uses a deterministic per-id phase instead of `getRNG().nextInt(10)` (`:86-90`) — cosmetic, acceptable.

---

## False Darkness (偽暗 Gian) — "false_darkness" / GIAN

| Field | Value |
|---|---|
| Display name | `false_darkness` (index 3), internal const `GIAN` — `ItemRaiton.java:49` |
| Index / rank | 3 / `'B'` |
| Chakra | `100d` (`ItemRaiton.java:49`). Charge power `getPower(...,1f,150f)` → min 1, max 150 (`:84-85`) |
| Level/xp | Damage = `BASE_DAMAGE(30) * power` (`EntityFalseDarkness.java:30,88`); build time = `power*20` ticks (`:69`) |
| Cooldown | None explicit |
| Entities | `EntityFalseDarkness.EC` (registry `false_darkness`, id 241) — `:38-39`. Requires a living target via look raytrace (`Jutsu.createJutsu:112-122`) |
| Procedures | `ProcedureUtils.objectEntityLookingAt(20d)` for target lock (`:115`); `ItemJutsu.causeJutsuDamage` |
| Particles | Black arcs `EntityLightningArc.spawnAsParticle(...,0x000000ff)` during charge (`:77-79`); final black beam `EntityLightningArc.Base(..., 0x000000FF, 40, 0f)` (`:86-87`) |
| Sounds | `narutomod:electricity` charge (`:73-74`) and fire (`:82-83`); vanilla `ENTITY_LIGHTNING_IMPACT` on target (`:84-85`) |
| Visual | No custom renderer — charge-up cluster of **black** lightning arcs at the caster's eye, then a single thick black lightning beam (`EntityLightningArc.Base`, dur 40) from caster to target eyes dealing `30*power`. `isInRangeToRenderDist` extended to 68.5 (`:97-102`). |
| 1.12.2 file:line | `item/ItemRaiton.java:49, 84-85`; `entity/EntityFalseDarkness.java` (whole file); scroll `item/ItemScrollFalseDarkness.java` |

**1.20.1 status: DONE.**
- Ported `entity/FalseDarknessEntity.java`. Same `BASE_DAMAGE=30` (`:24`), black color `0x000000FF` (`:25`), build time `power*20` (`:67`), charge arcs scaling with progress (`charge/spawnChargeArc:172-198`), final beam `configureBetween(... ,40, ...)` + `setDamage(30*power)` (`:200-215`), electricity + `LIGHTNING_BOLT_IMPACT` sounds (`:204-207`). Target acquisition via look raytrace in `RaitonItem.activateFalseDarkness` (`:200-223`), chakra `100*power`. Renderer = `NoopEntityRenderer` (correct). Scroll learning present.
- Note: port shows a client message "False Darkness needs a living target." when no target (`RaitonItem.java:206`) — original silently failed. Cosmetic improvement, not a regression.

---

## Kirin (麒麟) — "kirin"

| Field | Value |
|---|---|
| Display name | `kirin` (index 4) — `ItemRaiton.java:50` |
| Index / rank | 4 / `'S'` |
| Chakra | `1500d` (`ItemRaiton.java:50`). Charge power `getPower(...,0f,400f)`, **capped at 1.0** by `getMaxPower` (`:86-88,92-99`). Fires only when `power >= 1.0` (`EntityKirin.java:204`) |
| Level/xp | None; gated by full charge to power 1.0 |
| Cooldown | On cast (non-creative): `3600 * item.getModifier` ticks + NAUSEA 300 (`EntityKirin.java:206-210`) |
| Entities | `EntityKirin.EC` extends `EntityScalableProjectile.Base` (registry `kirin`, id 373, tracker 128) — `:53-54`. Scale 10, drops from y+100 and strikes |
| Procedures | `ProcedureAoeCommand` (AoE fire+damage on impact, `:185-186`), `ProcedureUtils.objectEntityLookingAt/getYawPitchFromVec/Vec2f`; `ForgeEventFactory.getMobGriefingEvent`; `world.newExplosion` |
| Particles | Many `EntityLightningArc.Base` along the body each tick (`renderParticles:160-170`); charging arcs high in sky (`chargingEffects:218-231`); 150-tall impact column (`:179-181`) |
| Sounds | `narutomod:dragon_roar` spawn (`:88-89`); `narutomod:lightning_shoot` launch (`:108-109`); `narutomod:kirin_dialog` at 80% charge (`chargingEffects:220-222`); vanilla `ENTITY_LIGHTNING_IMPACT` impact (`:177`) and `ENTITY_LIGHTNING_THUNDER` weather (`startWeatherThunder:239-240`) |
| Visual | `RenderDragon` (`Render<EC>`, `:243-320`) with elaborate inner `ModelDragonHead` (`:325-588`): head, jaw, teeth, horns, whiskers, eyes, and a 100-segment articulated **spine** that trails the flight path (`updateSegments/partRot`). Three additive passes: body (`dragon_lightning.png`), scrolling `electric_armor.png`, blue aura. Charge: thunderstorm starts (`startWeatherThunder`) + sky arcs + dialog SFX; dragon-shaped lightning falls from y+100, waits 60t, then shoots toward look target at speed 1.2; on impact: explosion size=scale(10), AoE 100×scale fire damage in r=10, giant 150-block vertical bolt. |
| 1.12.2 file:line | `item/ItemRaiton.java:50, 86-116`; `entity/EntityKirin.java` (whole file). **No scroll item.** |

**1.20.1 status: DONE (verify flight/spine fidelity).**
- Ported `entity/KirinEntity.java`, `client/renderer/KirinRenderer.java`, `client/model/KirinModel.java`. Same WAIT 60 / MAX_LIFE 100 / SCALE 10 / 100 spine segments (`:42-46`), seed part-rotations identical to original `partRot` list (`seedPartRotations:304-313`), waiting drop + launch at look/target (`updateWaiting/launchAtTarget:249-276`), impact = `LIGHTNING_BOLT_IMPACT` SFX + 150-block bolt column + explosion size 10 + r=10 fire/100×scale damage (`impact:413-442`), `chargingEffects` (sky arcs + 80% dialog SFX) and `startWeatherThunder` (`:185-215`), per-tick body arcs (`spawnFlightArcs:444-460`). Cooldown `3600*modifier` + CONFUSION/nausea 300 + KIRIN weather on use (`RaitonItem.activateKirin:225-248`, `use:55-57`). Three-pass additive dragon render with scrolling charge texture + blue aura (`KirinRenderer.java:51-94`), articulated spine via `partRotations` (`KirinModel` + `updateVisualSegments:315-344`).
- Nuances to verify at runtime (not blocking): the original extended `EntityScalableProjectile.Base` (engine projectile motion); the port reimplements no-gravity flight + impact raycast manually (`updateLaunched/findImpact/updateLegacyNoGravityMotion:278-499`). Trajectory feel and exact spine trailing should be visually checked against original. `tracker(128,...)` view range — confirm the port's `KIRIN` EntityType uses a large enough `clientTrackingRange` (it is a 100-block-tall effect).

---

## Cross-cutting assets the Raiton domain needs (status)

| Asset | 1.12.2 | 1.20.1 | Status |
|---|---|---|---|
| `lightning_arc` entity + fractal renderer | `EntityLightningArc.java` | `entity/LightningArcEntity.java` + `client/renderer/LightningArcRenderer.java` | DONE |
| Paralysis potion (beast/onStruck) | `potion/PotionParalysis` | `effect/ParalysisEffect` / `ModEffects.PARALYSIS` | DONE |
| `narutomod:electricity` sound | yes | `ModSounds.SOUND_ELECTRICITY` | DONE |
| `narutomod:chidori` sound | yes | `ModSounds.SOUND_CHIDORI` (`sounds/chidori.ogg` present) | DONE |
| `narutomod:dragon_roar` / `lightning_shoot` / `kirin_dialog` sounds | yes | `ModSounds.SOUND_DRAGON_ROAR` / `SOUND_LIGHTNING_SHOOT` / `SOUND_KIRIN_DIALOG` (`sounds/kirin_dialog.ogg` present) | DONE |
| Textures `wolf_lightning.png`, `electric_armor.png`, `dragon_lightning.png` | yes | all three present under `assets/narutomod/textures/` (plus `white_square.png` for arc/chidori base) — confirmed on disk | DONE |
| `forceBowPose` arm-pose sync | `NarutomodModVariables.forceBowPose` | `NarutomodModVariables.FORCE_BOW_POSE` + `ProcedureSync.EntityNBTTag` | DONE |
| `LightSource` block (chidori glow) | `ProcedureLightSourceSetBlock` | `block/LightSourceBlock.setOrRefresh` | DONE |

### Risks / open items
1. Kirin flight is a hand-rolled no-gravity reimplementation (original used `EntityScalableProjectile.Base`); trajectory and 100-segment spine trailing should be verified in-game, plus the EntityType client tracking range for the tall effect.
2. Chidori empty-hand spark origin is server-approximated (no per-frame live arm-angle sampling like the original `RenderChidori`); the lunge FOV punch (`ProcedureRenderView.setFOV`) is not reproduced.
3. Lightning Beast charge-overlay flicker uses a deterministic per-id phase instead of the original per-tick RNG — cosmetic only.

(All Raiton textures and sound files referenced by the ported renderers were confirmed present on disk.)
