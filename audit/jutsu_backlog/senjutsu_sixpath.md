# Jutsu Backlog — Senjutsu (Sage) + Six Paths Senjutsu + Asura Path

Audit comparing the original **1.12.2 (Forge)** source against the in‑progress **1.20.1 (Forge)** port.
Goal: enable a developer to restore EVERY technique (mechanics AND visuals) without missing any.

- ORIGINAL base: `D:\mcmodding\naruto\1.12.2\src\main\java\net\narutomod`
- PORT base:     `D:\mcmodding\naruto\1.20.1\src\main\java\net\narutomod`

## Framework / shared notes

**Jutsu tome model (1.12.2).** Each "Item*" tome extends `ItemJutsu.Base` and registers an array of
`ItemJutsu.JutsuEnum` entries. `JutsuEnum(index, translationKey, rankChar, chakraUsage, IJutsuCallback)`.
The rank char maps to a fixed XP‑to‑level curve in `ItemJutsu.java:571`:
`S=400, A=250, B=200, C=150, D=100, (else)=900` required XP per jutsu level. All Senjutsu / Six‑Path jutsu
below are rank **'S' → 400 XP per level**. (`ItemSenjutsu.java:66-70`, `ItemSixPathSenjutsu.java:41-45`.)
Each `IJutsuCallback.createJutsu(stack, entity, power)` spawns the payload. Power is computed by
`getPower(stack, entity, timeLeft, basePower, powerUpDelay)` and capped by `getMaxPower`.

**Cooldowns.** Both tomes zero‑out `defaultCooldownMap` for their entries (`ItemSenjutsu.java:119`,
`ItemSixPathSenjutsu.java:83-85`) → essentially no per‑item cooldown in 1.12.2 (gating is via chakra + Sage Mode + XP).
The 1.20.1 port adds small 20‑tick (1s) cooldowns on WOOD_BUDDHA / SNAKE_8_HEADS (`SenjutsuItem.java:478,554`).

**Damage source.** Senjutsu lightning/laser use `ItemJutsu.causeSenjutsuDamage(source, shooter)`
(`ItemJutsu.java:56`), ported as `LightningArcEntity.setSenjutsuDamage(...)`.

**Port status legend:** MISSING / STUB / PARTIAL / DONE.

**Port-level deltas worth knowing:**
- 1.12.2 SAGE_MODE chakra cost on the JutsuEnum is `10d` (`ItemSenjutsu.java:66`); the gate is "power ≥ 100"
  and the depletion‑threshold logic, not a flat cost. The 1.20.1 port adds an explicit
  `SAGE_MODE_BASE_CHAKRA_COST = 1000.0D` activation cost (`SenjutsuItem.java:61`). This is a deliberate
  port change, not present in 1.12.2.
- 1.20.1 SixPathSenjutsu adds a **6th jutsu, OUTER_PATH (index 5, "chattext.rinnegan.path5", 2000 chakra)**
  that does NOT exist in 1.12.2 `ItemSixPathSenjutsu` (`SixPathSenjutsuItem.java:45`). It summons the Gedo
  Statue. Flagged below as an ADDED jutsu (no 1.12.2 source of truth).

---

# DOMAIN A — Senjutsu / Sage tome (`ItemSenjutsu`)

1.12.2: `item\ItemSenjutsu.java`  →  1.20.1: `item\SenjutsuItem.java` (extends `NinjutsuItem`).
Sage Mode is the master toggle; Rasengan / Rasenshuriken / Wood Buddha are *inherited* jutsu that are only
enabled when the player also owns the corresponding base tome and meets its requirements
(`ItemSenjutsu.java:196-206` → `SenjutsuItem.updateInheritedJutsuGates`, `SenjutsuItem.java:430-454`).

Helper item: **`ItemSageModeArmor`** (sage_mode_armorhelmet) — invisible head item that hosts the
`buffMap` attribute set and the snug‑helmet visual model. The same `buffMap` is reused by `ItemSenjutsu`
when Sage Mode is active (`ItemSenjutsu.java:171-186`). In 1.20.1 the buffs are inlined into
`SenjutsuItem.SAGE_MODE_MODIFIERS` (`SenjutsuItem.java:66-91`); the helmet item itself ports as the snug
helmet/textures.

## Sage Mode (item.sage_mode_armorhelmet.name)

| Field | Value |
|---|---|
| Index / id | 0 |
| Chakra | JutsuEnum cost `10d` (`ItemSenjutsu.java:66`); activation requires charge `power ≥ 100`, then `cp.consume(-0.6f/xpMod, true)` regen burst and stores `SageChakraDepletionAmount = current chakra` (`SageMode.createJutsu`, `ItemSenjutsu.java:320-336`). Upkeep: `consume(50d)` every 20 ticks while active; `SATURATION` effect; auto‑deactivate if chakra < depletion threshold (`ItemSenjutsu.java:187-195`). |
| Level/Xp/Rank | 'S' → 400 XP/level |
| Cooldown | 0 (`ItemSenjutsu.java:119`) |
| Power | `getPower(...,0f,20f)` ramp; `getMaxPower=100` (`ItemSenjutsu.java:142,152-154`) |
| Entities | `EntitySitPlatform` (ENTITYID 355) — invisible 1.0×0.01 ride platform the player sits on while charging (`ItemSenjutsu.java:80-81, 344-382`); spawned on right‑click SUCCESS (`ItemSenjutsu.java:243-245`) |
| Procedures | `ProcedureOnLeftClickEmpty.addQualifiedItem` (`ItemSenjutsu.java:108`); `OverlayChakraDisplay.ShowFlamesMessage.send` (flame overlay HUD on/off, `ItemSenjutsu.java:316,331`) |
| Buffs (`ItemSageModeArmor.buffMap`) | REACH +1.5, ATTACK_DAMAGE +60, ATTACK_SPEED ×2 (op1), MOVEMENT_SPEED ×1.5 (op1), MAX_HEALTH +80 (`ItemSageModeArmor.java:41-47`) |
| Particles | none in tome (visual is the helmet overlay + HUD flames) |
| Sounds | none direct |
| Visual | Snug sage helmet `ModelHelmetSnug` (head + headwear + fullbright "highlight" overlay quad) rendered via `getArmorModel` while active; texture per Sage `Type`: TOAD `sagetoadhelmet.png`, SNAKE `sagesnakehelmet.png`, SLUG `sageslughelmet.png` (`ItemSenjutsu.java:270-293, 421-450`). `showSkinLayer()=true`. Sage `Type` is random 1‑3 (`ItemSenjutsu.java:384-419`). HUD "flames" overlay around chakra display. |
| 1.12.2 file:line | `ItemSenjutsu.java:66,119,160-208,210-247,260-298,306-342,344-382,384-419,421-450`; `ItemSageModeArmor.java:41-142` |

**1.20.1 status: PARTIAL (mechanics largely DONE; verify visuals).**
Ported in `SenjutsuItem.java`: shift‑right‑click charge while riding `SenjutsuSitPlatformEntity`
(`SenjutsuItem.java:98-180`), power≥100 gate, depletion‑threshold deactivation, 50/20‑tick drain,
SATURATION, full attribute set (`SAGE_MODE_MODIFIERS`), Sage `Type` (TOAD/SNAKE/SLUG) random + tooltip,
NBT sync via `ProcedureSync`, and a `ForgeEvents` player‑tick safeguard that strips modifiers if no active
stack. **Deltas / remaining:** (1) added flat `1000` activation cost not in 1.12.2; (2) charge feedback uses
chat messages + `SMOKE_COLORED` particles + `SOUND_CHARGING_CHAKRA` (port‑added flavor, fine); (3) confirm
the snug‑helmet overlay model + per‑type textures + fullbright highlight render in‑game (the
`getArmorModel` equivalent) — verify `ItemSageModeArmor`/helmet visual and `showSkinLayer`; (4) confirm the
`OverlayChakraDisplay` "flames" HUD is wired to the new `SAGE_MODE_ACTIVATED` variable.

## Sage Rasengan (tooltip.senjutsu.rasengan)

| Field | Value |
|---|---|
| Index / id | 1 |
| Chakra | inherits `ItemNinjutsu.RASENGAN.chakraUsage` (`ItemSenjutsu.java:67`) |
| Level/Xp/Rank | 'S' → 400 XP/level |
| Power | `getPower(...,2.9f,200f)`, `getMaxPower=7.0` (`ItemSenjutsu.java:138,148-149`) |
| Entities | `EntityRasengan.EC.Jutsu` (delegated) |
| Gate | enabled only if player holds `ItemNinjutsu` and can use its RASENGAN (`ItemSenjutsu.java:196-199`) |
| Visual | Standard Rasengan orb (sage variant); see EntityRasengan |
| 1.12.2 file:line | `ItemSenjutsu.java:67,137-138,148-149,196-199` |

**1.20.1 status: DONE (mechanics).** `SenjutsuItem.SAGE_RASENGAN` (index1, 150 chakra) uses
`SENJUTSU_RASENGAN = RasenganProfile("Sage Rasengan",150,2.9,3.0,7.0,200,40,true)` (`SenjutsuItem.java:65,93-94`),
gated by `hasAnyLearnedRasengan` + active Sage Mode (`SenjutsuItem.java:198-215,431`). Visual reuses
`RasenganEntity`. Note chakra was rebased to 150 (1.12.2 inherited Ninjutsu value) — acceptable.

## Sage Rasenshuriken (tooltip.senjutsu.rasenshuriken)

| Field | Value |
|---|---|
| Index / id | 2 |
| Chakra | inherits `ItemFuton.RASENSHURIKEN.chakraUsage` (`ItemSenjutsu.java:68`) |
| Level/Xp/Rank | 'S' → 400 XP/level |
| Power | `getPower(...,1.9f,300f)`, `getMaxPower=6.0` (`ItemSenjutsu.java:139-140,150-151`) |
| Entities | `EntityRasenshuriken.EC.Jutsu` (delegated) |
| Gate | enabled only if player holds `ItemFuton` and can use FUTON RASENSHURIKEN (`ItemSenjutsu.java:200-202`) |
| Visual | Futon Rasenshuriken (sage); see EntityRasenshuriken |
| 1.12.2 file:line | `ItemSenjutsu.java:68,139-140,150-151,200-202` |

**1.20.1 status: DONE (mechanics).** `SAGE_RASENSHURIKEN` (index2, 1000 chakra). Charged via
`beginSageRasenshurikenCharge` / `releaseSageRasenshuriken` (`SenjutsuItem.java:334-359,421-428`); requires
active Sage Mode + usable Futon Rasenshuriken; spawns `RasenshurikenEntity.spawnFrom(player, power)`.

## Wood Buddha / Senju Mokuton 1000 (buddha_1000)

| Field | Value |
|---|---|
| Index / id | 3 |
| Chakra | `5000d` (`ItemSenjutsu.java:69`) |
| Level/Xp/Rank | 'S' → 400 XP/level |
| Cooldown | 0 (1.12.2); 20t in port |
| Entities | `EntityBuddha1000.EC.Jutsu` → `EntityBuddha1000.EC` (mountable giant statue, ENTITYID) + `EntityBuddha1000.EntityArm` (wood fists, ENTITYID_RANGED) |
| Gate | enabled only with `ItemMokuton` GOLEM usable (`ItemSenjutsu.java:203-205`) |
| Particles | `BLOCK_DUST` on growth (`EntityBuddha1000.java:271`); `Particles.Types.SMOKE` ×300 on poof (`EntityBuddha1000.java:341`) |
| Sounds | `narutomod:woodspawn`, `narutomod:poof`, `narutomod:shinsusenju` (`EntityBuddha1000.java:281,340,368,374`) |
| Visual | `RenderCustom extends RenderLivingBase<EC>` with `budha1000.png`; arms `RenderArm` with `woodfist.png` (`EntityBuddha1000.java:535-578, 494-529`) |
| 1.12.2 file:line | `ItemSenjutsu.java:69,203-205`; `EntityBuddha1000.java` |

**1.20.1 status: DONE (mechanics + visuals present).** `WOOD_BUDDHA` (index3, 5000) → `Buddha1000Entity`
(`SenjutsuItem.java:456-541`), toggles sitting / spawns, `SOUND_WOODSPAWN`. Renderer
`Buddha1000Renderer` + `Buddha1000Model` (+ `BuddhaArmEntity`) registered in `ClientModEvents.java:253,427`.
Verify shinsusenju sound + arm textures parity.

## Eight‑Branches Snake / Yamata (snake_8_heads)

| Field | Value |
|---|---|
| Index / id | 4 |
| Chakra | `3000d` (`ItemSenjutsu.java:70`); upkeep `chakraUsage*0.02` per sec drained while alive, also needs active Sage Mode (`EntitySnake8Heads.java:186-188,231`) |
| Level/Xp/Rank | 'S' → 400 XP/level |
| Entities | `EntitySnake8Heads.EC.Jutsu` → `EC` (8‑headed mount, "snake_8_heads") + `EntitySnakeHead` ("snake_8_head1", spawn egg) |
| Particles | `Particles.Types.SMOKE` ×300 on poof (`EntitySnake8Heads.java:212`) |
| Sounds | `narutomod:woodspawn`, `narutomod:snake_hiss`, `narutomod:woodgrow`, `narutomod:poof` (`EntitySnake8Heads.java:174-178,211,287`) |
| Visual | `RenderLivingBase<EC>` `ModelSnake8h` tex `snake_8h.png`; heads `EntitySnake.RenderSnake` tex `snake_8h1.png` (`EntitySnake8Heads.java:78-99`) |
| 1.12.2 file:line | `ItemSenjutsu.java:70`; `EntitySnake8Heads.java` |

**1.20.1 status: DONE (mechanics + visuals present).** `SNAKE_8_HEADS` (index4, 3000) →
`Snake8HeadsEntity.spawnFrom` (`SenjutsuItem.java:543-610`), with `hasActiveSageMode` upkeep check + chakra
drain (`Snake8HeadsEntity.java:386-396`). Renderers `Snake8HeadsRenderer`/`Snake8HeadRenderer` + models
registered (`ClientModEvents.java:373-374,477-478`). Verify hiss/woodgrow/poof sound parity.

---

# DOMAIN B — Six Paths Senjutsu tome (`ItemSixPathSenjutsu`)

1.12.2: `item\ItemSixPathSenjutsu.java`  →  1.20.1: `item\SixPathSenjutsuItem.java` (extends `JutsuItem`).
Whole tome requires the wielder to wear an active **Rinne Sharingan** (Rinnegan/Tenseigan helmet);
otherwise the stack is consumed each tick (`ItemSixPathSenjutsu.java:116-118`; port:
`SixPathSenjutsuItem.java:136-141, hasRinneSharingan 428-433`). The item maintains up to **9 Truth‑Seeking
Balls (Gudodama)** orbiting the wielder, auto‑respawning them, plus a "sentry" auto‑intercept of incoming
fast projectiles (`ItemSixPathSenjutsu.java:108-218`). Balls = `EntityTruthSeekerBall.EntityCustom`
(`truthseekerball`, ENTITYID).

## Truth‑Seeking Ball — Shoot (tooltip.6psenjutsu.shoot)

| Field | Value |
|---|---|
| Index / id | 0 |
| Chakra | `50d` (`ItemSixPathSenjutsu.java:41`) |
| Level/Xp/Rank | 'S' → 400 XP/level |
| Cooldown | 0 |
| Entities | next available `EntityTruthSeekerBall.EntityCustom` → `.shoot(look, 0.95, 0)` (`ShootTruthSeekerBall`, `ItemSixPathSenjutsu.java:221-234`) |
| Particles | `SMOKE_NORMAL` trail; `Particles.Types.EXPANDING_SPHERE` on impact (`EntityTruthSeekerBall.java:125,346`) |
| Sounds | launch sound on shoot (`EntityTruthSeekerBall.java:229`) |
| Visual | black orb model `ModelRenderer`, `RenderCustom`; spawns `EventSphericalExplosion` on impact (`EntityTruthSeekerBall.java:346-350`) |
| 1.12.2 file:line | `ItemSixPathSenjutsu.java:41,221-234`; `EntityTruthSeekerBall.java` |

**1.20.1 status: DONE.** `SHOOT` index0/50 → `TruthSeekerBallEntity.nextAvailableSixPathBall` + `shootFromOwner`
(`SixPathSenjutsuItem.java:151-169`). Ball maintenance/sentry ported
(`TruthSeekerBallEntity.maintainSixPathBalls/runSixPathSentry`, `SixPathSenjutsuItem.java:143-144`).
Renderer/model `TruthSeekerBallRenderer`/`TruthSeekerBallModel` registered + `EXPANDING_SPHERE` particle
(`ClientModEvents.java:399,491`; `TruthSeekerBallEntity.java:618`).

## Truth‑Seeking Ball — Shield (tooltip.6psenjutsu.shield)

| Field | Value |
|---|---|
| Index / id | 1 |
| Chakra | `50d` (`ItemSixPathSenjutsu.java:42`) |
| Level/Xp/Rank | 'S' → 400 XP/level |
| Entities | next TSB `.toggleShield()` → forms protective shield above wielder, grants `InvulnerableTime` while on (`TruthSeekerShield`, `ItemSixPathSenjutsu.java:236-248`; `EntityTruthSeekerBall.java:272-273,298-308`) |
| Visual | ball flattens into shield dome above player (`shieldProgress`, `EntityTruthSeekerBall.java:180-184`) |
| 1.12.2 file:line | `ItemSixPathSenjutsu.java:42,236-248`; `EntityTruthSeekerBall.java:272-308` |

**1.20.1 status: DONE.** `SHIELD` index1/50 → `ball.toggleShield(player)` (`SixPathSenjutsuItem.java:171-189`,
`TruthSeekerBallEntity.toggleShield` 262). Verify shield‑dome visual/invuln parity in‑game.

## Inton Raiha — Lightning Burst (inton_raiha)

| Field | Value |
|---|---|
| Index / id | 2 |
| Chakra | `100d` (`ItemSixPathSenjutsu.java:43`) |
| Level/Xp/Rank | 'S' → 400 XP/level |
| Power | `getPower(...,1f,80f)`, `getMaxPower=6.0` (`ItemSixPathSenjutsu.java:92-93,100-101`) |
| Entities | `EntityIntonRaiha.EC.Jutsu` → `EC`; spawns many `EntityLightningArc.Base` arcs (color `0x80FF00FF`) each tick after wait (`EntityIntonRaiha.java:84-96,120-126`) |
| Sounds | `narutomod:intonraiha` (charge, power≥4), `narutomod:electricity` (per arc) (`EntityIntonRaiha.java:78,85`) |
| Visual | force bow pose; radial purple lightning arcs around/forward from wielder; duration scales with power (`EntityIntonRaiha.java:77-99`) |
| 1.12.2 file:line | `ItemSixPathSenjutsu.java:43,92-93,100-101`; `EntityIntonRaiha.java` |

**1.20.1 status: DONE.** `THUNDER` index2/100 `.withPower(1,80)`; charge‑release via
`activateThunder` → `IntonRaihaEntity.spawnFrom(player,power)` (`SixPathSenjutsuItem.java:191-204`).
`IntonRaihaEntity` faithfully reproduces wait gate (power≥4 → 50t), `SOUND_INTONRAIHA`,
`SOUND_ELECTRICITY`, randomized purple `LightningArcEntity` arcs, force‑bow‑pose sync
(`IntonRaihaEntity.java:50-164`). Renderer is `NoopEntityRenderer` (correct — the EC is invisible; arcs are
the visible `LightningArcRenderer`).

## Ranton Koga — Storm Release Laser (ranton_koga)

| Field | Value |
|---|---|
| Index / id | 3 |
| Chakra | `100d` (`ItemSixPathSenjutsu.java:44`) |
| Level/Xp/Rank | 'S' → 400 XP/level |
| Power | `getPower(...,1f,50f)`, `getMaxPower=10.0` (`ItemSixPathSenjutsu.java:90-91,102-103`) |
| Entities | `EntityRantonKoga.EC.Jutsu` → `EC`; spawns straight `EntityLightningArc.Base` beam (`0x80FF00FF`, segments=1, jitter 0) each tick, dmg `20*power` (`EntityRantonKoga.java:55-69`) |
| Sounds | `narutomod:laser_short` once (`EntityRantonKoga.java:58`) |
| Visual | concentrated straight purple laser from eyes forward, length `power*4`; lives 20 ticks (`EntityRantonKoga.java:61-69`) |
| 1.12.2 file:line | `ItemSixPathSenjutsu.java:44,90-91,102-103`; `EntityRantonKoga.java` |

**1.20.1 status: DONE.** `LASER` index3/100 `.withPower(1,50)`; `activateLaser` →
`RantonKogaEntity.spawnFrom` (`SixPathSenjutsuItem.java:206-219`). `RantonKogaEntity` plays
`SOUND_LASER_SHORT`, spawns straight `LightningArcEntity` dmg `20*power` (`RantonKogaEntity.java:71-72,126-129`).
Renderer `NoopEntityRenderer` (beam is `LightningArcRenderer`). `getMaxPower=10` handled by
`getSixPathPower` laser branch (`SixPathSenjutsuItem.java:361-369`).

## Truth‑Seeking Rasenshuriken (tooltip.6psenjutsu.rasenshuriken)

| Field | Value |
|---|---|
| Index / id | 4 |
| Chakra | `1000d` (`ItemSixPathSenjutsu.java:45`) |
| Level/Xp/Rank | 'S' → 400 XP/level |
| Entities | `EntityRasenshuriken.EC.TSBVariant` — Truth‑Seeking‑Ball variant rasenshuriken (`ItemSixPathSenjutsu.java:45`) |
| Gate | enabled only with usable Futon Rasenshuriken (`ItemSixPathSenjutsu.java:144-147`) |
| Visual | black/gudodama‑tinted Rasenshuriken; see EntityRasenshuriken TSBVariant |
| 1.12.2 file:line | `ItemSixPathSenjutsu.java:45,144-147` |

**1.20.1 status: DONE.** `RASENSHURIKEN` index4/1000 → `RasenshurikenEntity.spawnTruthSeekingVariantFrom(player)`
(`SixPathSenjutsuItem.java:221-236`), gated by `hasUsableFutonRasenshuriken` + Rinne Sharingan
(`SixPathSenjutsuItem.java:264-268,395-406`).

## (ADDED IN PORT) Outer Path — Gedo Statue (chattext.rinnegan.path5)

| Field | Value |
|---|---|
| Index / id | 5 |
| Chakra | `2000d` |
| Notes | NOT present in 1.12.2 `ItemSixPathSenjutsu`. Added in port; requires ninja level ≥ 90 and no rival Ten‑Tails jinchuriki; summons/dismisses `GedoStatueEntity`, sets invulnerable time, plays `SOUND_KUCHIYOSENOJUTSU` + `EXPLOSION_EMITTER`. |
| 1.20.1 file:line | `SixPathSenjutsuItem.java:45-46,99-103,284-359` |

**1.20.1 status: ADDED (no 1.12.2 source of truth).** Flag for reviewer: confirm intent — this is a new
sixth entry, so the 1.12.2 tome had only 5 jutsu (indices 0‑4).

---

# DOMAIN C — Asura Path (mechanical arm path)

The Asura Path is not a JutsuEnum tome; it is a **gear set** auto‑equipped when a Rinnegan/Tenseigan helmet
is set to the Asura path. Three items: the chest armor (mechanical body + tails), the offhand cannon, and
the ash‑bone finger projectile.

## Asura Path Armor (asurapatharmorbody)

| Field | Value |
|---|---|
| Item | chest armor, custom material `ASURAPATHARMOR` (durability 1024×, chest defense 1024) (`ItemAsuraPathArmor.java:43`) |
| Buffs | MAX_HEALTH +40 attribute (`ItemAsuraPathArmor.java:36,73-80`); every 40 ticks while worn → STRENGTH II, SPEED II, HASTE (amp5), JUMP_BOOST (amp5), SATURATION (`ProcedureAsuraPathArmorBodyTickEvent.java:55-66`) |
| Requirement | requires Rinnegan OR Tenseigan helmet in head slot, else `itemstack.shrink(1)` (`ProcedureAsuraPathArmorBodyTickEvent.java:42-47`) |
| Visual | `ModelArmorCustom` extends ModelBiped: thick body+arms + an 11‑segment hinged mechanical **tail** chain; texture `narutomod:textures/asura_path.png` (`ItemAsuraPathArmor.java:48-59,94-182`) |
| 1.12.2 file:line | `ItemAsuraPathArmor.java`; `ProcedureAsuraPathArmorBodyTickEvent.java` |

**1.20.1 status: DONE (mechanics + visuals).** `AsuraPathArmorItem` (chest, MAX_HEALTH +40, texture
`asura_path.png`, no‑drop) with client model via `AsuraPathArmorClientExtensions`
(`AsuraPathArmorItem.java`). The tick buffs (STRENGTH/SPEED/HASTE/JUMP/SATURATION every 40t) + the
"requires Rinnegan/Tenseigan helmet else shrink" rule are ported into
`RinneganSpecialJutsuHandler.tickAsuraBody` + `maintainAsuraPath` (effects exactly match the procedure;
`RinneganSpecialJutsuHandler.java:118-140,376-396`). Verify the 11‑segment tail mesh exists in
`AsuraPathArmorClientExtensions`' model (the most visually load‑bearing piece).

## Asura Cannon (asuracanon)

| Field | Value |
|---|---|
| Item | offhand cannon, durability 100, bow‑use anim, charge to power up (`ItemAsuraCanon.java:62-102`) |
| Mechanic | on release spawns `EntityCanonball`; `explosivePower = (maxUse - timeLeft)/15 + 1`; shot speed 2.0 (`ItemAsuraCanon.java:73-85`) |
| Entities | `EntityCanonball extends EntityThrowable` (ENTITYID 31, "entitybulletasuracanon"); on impact `world.newExplosion(power, fire=false, mobGriefing)` (`ItemAsuraCanon.java:105-129`) |
| Sounds | `entity.blaze.shoot` on fire (`ItemAsuraCanon.java:81-83`) |
| Visual | `RenderSnowball` rendering a `Items.FIREWORK_CHARGE` item (`ItemAsuraCanon.java:58-59`) |
| BUG in 1.12.2 | `explosivePower` is a `static` field on `EntityCanonball` (`ItemAsuraCanon.java:106`) → shared across all balls; port fixed to per‑instance synched data. |
| 1.12.2 file:line | `ItemAsuraCanon.java` |

**1.20.1 status: DONE.** `AsuraCannonItem` (durability 100, bow anim, charge) → `AsuraCannonballEntity`
with per‑instance `explosivePower` synched data, `serverLevel.explode(... MOB)` on impact, `BLAZE_SHOOT`
sound (`AsuraCannonItem.java`; `AsuraCannonballEntity.java:30-97`). Registered renderer `ThrownItemRenderer`
(`ClientModEvents.java:271`). NOTE: registry name kept as `ENTITYBULLETASURACANON` ("asuracanon" spelling
preserved). Verify the firework‑charge item visual is the chosen render model.

## Ash Bones — finger bone projectile (ashbones)

| Field | Value |
|---|---|
| Item | full3D hand weapon, max stack 1, bow‑use anim; consumes itself each tick unless creative/Rinnegan/Byakugan‑rinnesharingan (`ItemAshBones.java:68-138`) |
| Mechanic | on release fires `EntityArrowCustom` (speed 2, dmg 2, no crit, no knockback, silent), 80‑tick player cooldown; melee hit also triggers death‑anim (`ItemAshBones.java:79-108`) |
| Entities | `EntityArrowCustom extends EntityArrow` (ENTITYID 63, "entitybulletashbones"); smoke trail; AoE `hitLivingEntity` within 0.75 grow box; sets `deathAnimationType=1` + slow‑dust death anim (`ItemAshBones.java:141-188`) |
| Particles | `SMOKE_NORMAL` ×5 per tick trail (`ItemAshBones.java:169-171`) |
| Sounds | `narutomod:hand_shoot` on fire (`ItemAshBones.java:93-96`) |
| Visual | `RenderCustom` renders the ashbones item as a spinning projectile (yaw‑90/pitch transform, GROUND transform) (`ItemAshBones.java:190-232`) |
| 1.12.2 file:line | `ItemAshBones.java` |

**1.20.1 status: DONE.** Ported as `SpecialProjectileWeaponItem` with `WeaponKind.ASH_BONES`
(`item\SpecialProjectileWeaponItem.java:48,103-104,124,163-177`): 80‑tick cooldown, `applyAshBonesEffect`
(slow‑dust death anim), self‑consume unless creative/rinnesharingan; projectile is
`ThrownSpecialWeaponEntity` with renderer `AshBonesProjectileRenderer`. Registered item `ASHBONES`
(`registry\ModItems.java:72`) and entity `ENTITYBULLETASHBONES`. Verify `hand_shoot` sound + smoke trail +
AoE‑on‑pass parity.

---

# Unresolved references / cautions

- The spawned‑payload tomes (`EntityBuddha1000.java` 4555 ln, `EntitySnake8Heads.java` 2268 ln,
  `EntityTruthSeekerBall.java` 461 ln) were inspected for registry names, textures, sounds, particles, and
  renderer setup only — their full internal AI/movement logic was NOT line‑audited here. If exact damage,
  mount controls, or animation timing must match 1.12.2, diff those entities directly against their 1.20.1
  counterparts (`Buddha1000Entity`, `BuddhaArmEntity`, `Snake8HeadsEntity`, `Snake8HeadEntity`,
  `TruthSeekerBallEntity`).
- `EntityRasengan` / `EntityRasenshuriken` are shared with the Ninjutsu/Futon domains; the Sage / TSB
  variants reuse those classes — covered under their own domains' audits.
- The 1.20.1 `SAGE_MODE` `1000` activation cost and `SixPathSenjutsu` `OUTER_PATH` (index 5) are
  intentional port additions with no 1.12.2 equivalent; called out so they are not mistaken for parity.
