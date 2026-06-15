# Katon (Fire Release) — Jutsu Port Audit

Compares the original **1.12.2 (Forge)** source to the in-progress **1.20.1 (Forge)** port.
Goal: a developer can restore every Katon technique (mechanics **and** visuals) without missing any.

- ORIGINAL base: `D:\mcmodding\naruto\1.12.2\src\main\java\net\narutomod`
- PORT base: `D:\mcmodding\naruto\1.20.1\src\main\java\net\narutomod`

## Framework / shared notes

### The tome
All Katon jutsu live in one item class.
- 1.12.2: `item\ItemKaton.java` → registry name `narutomod:katon`, item class `ItemKaton.RangedItem extends ItemJutsu.Base`. Jutsu list passed to constructor: `GREATFIREBALL, GFANNIHILATION, HIDINGINASH, GREATFLAME` (`ItemKaton.java:64-67,75,96-104`).
- 1.20.1: `item\KatonItem.java` → `KatonItem extends JutsuItem`, type `JutsuType.KATON`, list `GREAT_FIREBALL, FIRE_ANNIHILATION, HIDING_IN_ASH, GREAT_FLAME` (`KatonItem.java:23-41`). Registered as `ModItems.KATON`.

### Base classes / framework hooks
| Concern | 1.12.2 | 1.20.1 |
|---|---|---|
| Item base | `ItemJutsu.Base` (`ItemJutsu.java:105`) | `JutsuItem` (`JutsuItem.java:18`) |
| Jutsu descriptor | `ItemJutsu.JutsuEnum` (`ItemJutsu.java:548`) | `JutsuItem.JutsuDefinition` record (`JutsuItem.java:319`) |
| Callback | `IJutsuCallback.createJutsu(stack, entity, power)` (`ItemJutsu.java:542`) | inlined `activate*` methods + `ModEntityTypes.*.create()` (`KatonItem.java:104-198`) |
| Charging/power | `getPower(stack, entity, timeLeft, basePower, powerupDelay)` (`ItemJutsu.java:151`) | `getChargingPower(...)` (`JutsuItem.java:239`) — identical formula `base + max(useDur - remaining,0)/(delay*chakraMod*xpMod)` |
| Max power | `getMaxPower` capped per-jutsu (`ItemKaton.java:120-130`) | `getMaxKatonPower` capped per-jutsu (`KatonItem.java:259-277`) |
| Chakra spend | `Chakra.pathway(e).consume(chakra*power)` inside `executeJutsu` (`ItemJutsu.java:134-147`) | `Chakra.pathway(p).consume(def.chakraUsage()*power)` per-activate (`KatonItem.java:112,134,184`) |
| Required XP by rank | C=150, B=200 (`ItemJutsu.java:571`) | same switch (`JutsuItem.java:340-349`); non-affinity doubles it (`JutsuItem.java:152-155`) |
| Charging FX | onUsingTick: SMOKE cloud + `narutomod:charging_chakra` every 10t (`ItemJutsu.java:170-185`) | onUseTick: FLAME_COLORED 0xCCFF6600 every 5t + `SOUND_CHARGING_CHAKRA` every 10t (`KatonItem.java:53-75`) — **divergent**: port shows orange flame instead of blue smoke; cosmetic only |

### Cooldowns
In 1.12.2 every Katon `setCurrentJutsuCooldown(...)` call is **commented out** (`ItemKaton.java:102-103,248,260,338-339`; `EntityHidingInAsh.java` Jutsu has none), so all four jutsu have **no cooldown** in the original. The port keeps cooldown plumbing (`getRemainingCooldownTicks`, `KatonItem.java:240-246`) but never *sets* a cooldown after firing, so effective cooldown is also 0. Faithful.

### Entities, renderers, particles, sounds (port wiring — all present)
- Entities registered in `registry\ModEntityTypes.java`: `KATONFIREBALL` (:241), `KATONFIRESTREAM` (:242), `HIDING_IN_ASH` (:227). Builders at :951-952, :1102-1103.
- Renderers bound in `client\ClientModEvents.java`: `KATONFIREBALL → KatonFireballRenderer` (:317); `KATONFIRESTREAM → NoopEntityRenderer` (:318, intentional — see Great Flame). `HIDING_IN_ASH` is **not** bound to a renderer (correct — it is invisible, particle-only).
- Renderer: `client\renderer\KatonFireballRenderer.java` (billboard quad, `textures/fireball.png`, FULL_BRIGHT, cutout-no-cull) — faithful port of `ItemKaton.RenderBigFireball` (`ItemKaton.java:352-399`).
- Particles: `NarutoParticleKind.FLAME_COLORED` and `BURNING_ASH` exist (`particle\NarutoParticleKind.java:11,13`).
- Sounds: `ModSounds.SOUND_FLAMETHROW`, `SOUND_KATON_GOKAMEKEKU`, `SOUND_HIDING_IN_ASH`, `SOUND_CHARGING_CHAKRA` all registered (`registry\ModSounds.java:154,152,92,144`). Original registry names: `narutomod:flamethrow`, `narutomod:katon_gokamekeku`, `narutomod:hiding_in_ash`, `narutomod:charging_chakra`.

### Scrolls (learning items)
The four 1.12.2 scroll classes (`ItemScrollGreatFireball`, `ItemScrollFireAnnihilation`, `ItemScrollHidingInAsh`, `ItemScrollFireStream`) were thin items that opened a per-scroll learning GUI. In the port they are unified: items registered in `registry\ModItems.java` (`SCROLL_GREAT_FIREBALL`:254, `SCROLL_FIRE_ANNIHILATION`:244, `SCROLL_FIRE_STREAM`:246, `SCROLL_HIDING_IN_ASH`:258) and wired through `item\JutsuScrollDefinition.java` entries `GREAT_FIREBALL`(:147), `FIRE_ANNIHILATION`(:155), `HIDING_IN_ASH`(:163), `GREAT_FLAME`(:171), each pointing at the matching `KatonItem.*` definition and the generic `JutsuScrollItem` learning flow. DONE.

---

## Great Fireball — Katon: Goukakyuu no Jutsu (火遁・豪火球の術)

| Field | Value |
|---|---|
| Display name key | `entity.katonfireball.name` (1.12.2 unloc `katonfireball`); port `entity.narutomod.katonfireball` |
| Index / id | 0 (`ItemKaton.java:64`; `KatonItem.java:24`) |
| Rank | C → requiredXP 150 (`ItemKaton.java:64`) |
| Chakra | 30.0 × power (`ItemKaton.java:64`; `KatonItem.java:24`) |
| Charge curve | basePower 0.1, powerUpDelay 30 (`ItemKaton.java:112`; `KatonItem.java:25` `withPower(0.1f,30f)`) |
| Max power cap | 10.0 (`ItemKaton.java:126-127`; `KatonItem.java:261,267-268`) |
| Cooldown | none (commented out, `ItemKaton.java:338-339`) |
| Entity spawned | `ItemKaton.EntityBigFireball` (id `narutomod:katonfireball`, ENTITYID 123, tracker 64/1/true) (`ItemKaton.java:76-77,266`) → port `KatonFireballEntity` (`KatonFireballEntity.java:38`) |
| Procedure | inline (`EntityBigFireball.Jutsu.createJutsu`, `ItemKaton.java:334-348`) |
| Particles | `Particles.Types.FLAME` red/orange `0xffff0000\|((0x40+rand(0x80))<<8)`, count `(int)fullScale*2`, life 30, around body each tick (`ItemKaton.java:307-311`) |
| Sounds | `narutomod:flamethrow` at 20% chance/tick, pitch 0.6-1.1 (`ItemKaton.java:326-330`) |
| Visual | Camera-facing textured billboard quad, `textures/fireball.png`, FULL_BRIGHT, scales from 1.0 up to `fullScale` over 20 ticks; explosion on impact (`ItemKaton.java:352-399`) |
| 1.12.2 file:line | `ItemKaton.java:64, 266-349, 352-399` |

**Mechanics (original):** Spawned via `shoot(look, 0.95f, 0)` (no inaccuracy); no-gravity accelerated projectile (`EntityScalableProjectile.Base`). Scale ramps `1 + (fullScale-1)*ticksAlive/20`. `damage = fullScale*10`. On impact: sets shooter `InvulnerableTime=40`, deals NINJUTSU fire damage + `setFire(10)` to a hit entity (skips shooter / other fireballs), then `world.newExplosion(size = max(fullScale-1,0))` with mob-griefing flag, no fire spread. Dies after `ticksInAir>100` or contact with water.

**1.20.1 status: DONE.** `KatonFireballEntity` reproduces: no-gravity accel flight via custom `travelAndImpact`/`updateLegacyNoGravityMotion` (`KatonFireballEntity.java:221-331`), `TIME_TO_FULL_SCALE=20`, `MAX_LIFE=100`, `damage=safeScale*10` (:67), explosion `size=max(fullScale-1,0)` with `ExplosionInteraction.NONE` (:314-316), `InvulnerableTime=40` on shooter (:306), fire damage + `setSecondsOnFire(10)` (:311-312), water/lifetime death (:119), red FLAME_COLORED particles (:333-349), `flamethrow` sound at 20% (:351-356). Renderer faithful (`KatonFireballRenderer.java`). Activation in `KatonItem.activateGreatFireball` (`KatonItem.java:104-124`).
Minor notes: port `MAX_LIFE=100` matches original `ticksInAir>100`. Charging visual is orange flame vs original blue smoke (shared framework difference, cosmetic).

---

## Fire Annihilation — Katon: Goka Mekkyaku (火遁・豪火滅却)

| Field | Value |
|---|---|
| Display name key | `tooltip.katon.annihilation` (`ItemKaton.java:65`; `KatonItem.java:26`) |
| Index / id | 1 (`ItemKaton.java:65`; `KatonItem.java:26`) |
| Rank | B → requiredXP 200 (`ItemKaton.java:65`) |
| Chakra | 50.0 × power (`ItemKaton.java:65`; `KatonItem.java:26`) |
| Charge curve | basePower 1.0, powerUpDelay 30 (`ItemKaton.java:113-114`; `KatonItem.java:27` `withPower(1.0f,30f)`) |
| Max power cap | 30.0 (`ItemKaton.java:124-125`; `KatonItem.java:270-271`) |
| Cooldown | none (commented out, `ItemKaton.java:248`) |
| Entity spawned | `ItemKaton.EntityFireStream` (id `narutomod:katonfirestream`, ENTITY2ID 10123, tracker 64/1/true) via `Jutsu1` (`ItemKaton.java:78-79,134,241-251`) → port `KatonFireStreamEntity` (`KatonFireStreamEntity.java:33`) |
| Procedure | `EntityFireStream.FireStream extends ProcedureAirPunch` (`ItemKaton.java:194-239`) |
| Particles | `Particles.Types.FLAME` yellow `0xffffcf00`, cone spray, count `range*farRadius*0.8`, scale ~ length×50 (`ItemKaton.java:210-221`) |
| Sounds | start `narutomod:katon_gokamekeku` (vol 5) on cast (`ItemKaton.java:244-246`); loop `narutomod:flamethrow` every 10 ticks (`ItemKaton.java:178-181`) |
| Visual | No entity model — purely the yellow flame cone particles emitted from the caster's eyes/look direction (no renderer registered for `EntityFireStream`) |
| 1.12.2 file:line | `ItemKaton.java:65, 134-264 (Jutsu1 at 241-251)` |

**Mechanics (original):** `Jutsu1.createJutsu`: play `katon_gokamekeku`, spawn `EntityFireStream(player, width=power*0.8, range=power*1.5)`. Entity sticks to the caster's look position (`setIdlePosition`), `wait=50`, `maxLife=110`; after the wait, each tick runs `FireStream.execute(shooter, range*d, width*d)` with falloff `d = 1 - (t/maxLife)^2 * 0.8`. Damage = `range * rand(0.5..1.0)`, NINJUTSU fire, **bypasses armor**, `setFire(10)`, no knockback. Ignites blocks at >4 distance with 10% chance (spreads fire to air neighbors); `getBreakChance` returns -1 (does not break blocks). Dies on `ticksExisted>maxLife` or water contact.

**1.20.1 status: DONE.** `KatonItem.activateFireAnnihilation` (`KatonItem.java:148-155`) calls shared `activateFireStream` with width=`power*0.8`, range=`power*1.5`, wait=`ANNIHILATION_WAIT_TICKS(50)`, life=`DEFAULT_MAX_LIFE(110)`, and plays `katon_gokamekeku`. `KatonFireStreamEntity` reproduces idle-follow (:177-186), falloff `1-progress^2*0.8` (:188-192), cone flame particles `0xFFFFCF00` (:229-247), damage `range*rand(0.5..1)` fire + `setSecondsOnFire(10)` (:205-220), block ignition at >4 distance @10% (:256-298), `flamethrow` loop every 10t (:87-90), water/life death (:76). No renderer (Noop) — matches original (particle-only).
**CONFIRMED REGRESSION:** original `FireStream.attackEntityFrom` dealt damage with `.setDamageBypassesArmor()` (`ItemKaton.java:204-205`). The port's damage type JSON `data\narutomod\damage_type\katon_firestream_damage.json` has **no `tags` array**, so it is missing `minecraft:bypasses_armor`. Result: in 1.20.1 the fire-stream direct hit is now reduced by armor, unlike the original. Fix = add a damage-type tag file (`data\narutomod\tags\damage_type\bypasses_armor.json` listing `narutomod:katon_firestream_damage`). The `setSecondsOnFire(10)` burn still applies separately, so only the direct hit's armor handling regressed.

---

## Hiding In Ash — Katon: Kakuremino / Haijin (灰隠れ)

| Field | Value |
|---|---|
| Display name key | `hiding_in_ash` → `entity.hiding_in_ash.name` (1.12.2); port `entity.narutomod.hiding_in_ash` |
| Index / id | 2 (`ItemKaton.java:66`; `KatonItem.java:28`) |
| Rank | B → requiredXP 200 (`ItemKaton.java:66`) |
| Chakra | 50.0 × power (`ItemKaton.java:66`; `KatonItem.java:28`) |
| Charge curve | basePower 1.0, powerUpDelay 15 (`ItemKaton.java:109-111`; `KatonItem.java:29` `withPower(1.0f,15f)`) |
| Max power cap | 15.0 (`ItemKaton.java`'s `getMaxPower` falls through to super for HIDINGINASH; port caps 15.0 at `KatonItem.java:273-274`) |
| Cooldown | none (`EntityHidingInAsh.EC.Jutsu`, no cooldown set) |
| Entity spawned | `EntityHidingInAsh.EC` (id `narutomod:hiding_in_ash`, ENTITYID 175, tracker 64/3/true) (`EntityHidingInAsh.java:36-37,40`) → port `HidingInAshEntity` (`HidingInAshEntity.java:23`) |
| Procedure | inline `EntityHidingInAsh.EC.Jutsu.createJutsu` (`EntityHidingInAsh.java:115-124`) |
| Particles | `Particles.Types.BURNING_ASH`, count `range*20`, spread `range*0.1`, tied to user id, **client-side** in `onUpdate` (`EntityHidingInAsh.java:96-100`) |
| Sounds | `narutomod:hiding_in_ash` (vol 5) on cast (`EntityHidingInAsh.java:118-120`) |
| Visual | Caster gets INVISIBILITY (110 ticks), surrounded by a cloud of falling/burning ash particles centered on caster's look position; no entity model |
| 1.12.2 file:line | `ItemKaton.java:66`; `entity\EntityHidingInAsh.java:40-125` |

**Mechanics (original):** `EC.Jutsu.createJutsu`: play `hiding_in_ash`, spawn `EC(entity, range=power)`. Constructor applies `INVISIBILITY` potion (maxLife=110, amp 0, hidden particles/icon). Entity follows the caster's look position; client spawns `BURNING_ASH` particles each tick (`range*20` per tick). Dies at `ticksExisted>maxLife(110)`. Defensive/concealment jutsu — no damage.

**1.20.1 status: DONE.** `KatonItem.activateHidingInAsh` (`KatonItem.java:126-146`) consumes chakra, spawns `HidingInAshEntity.configure(player, power)`, plays `SOUND_HIDING_IN_ASH` vol 5. `HidingInAshEntity` applies `INVISIBILITY` 110t hidden (`HidingInAshEntity.java:40`), follows look position (:110-122), client-side `BURNING_ASH` particle cloud count `range*20` (:124-136), `MAX_LIFE=110` death (:61). Particle carries user id so the client renderer can attach ash to the invisible caster. Renderer intentionally absent (invisible entity). Faithful.

---

## Great Flame / Fire Stream — Katon: Endan (火遁・炎弾) (a.k.a. flamethrower)

| Field | Value |
|---|---|
| Display name key | `katonfirestream` → `entity.katonfirestream.name` (1.12.2); port `entity.narutomod.katonfirestream` |
| Index / id | 3 (`ItemKaton.java:67`; `KatonItem.java:30`) |
| Rank | C → requiredXP 150 (`ItemKaton.java:67`) |
| Chakra | 20.0 × power (`ItemKaton.java:67`; `KatonItem.java:30`) |
| Charge curve | basePower 1.0, powerUpDelay 30 (`ItemKaton.java:113-114`; `KatonItem.java:31` `withPower(1.0f,30f)`) |
| Max power cap | 30.0 (`ItemKaton.java:124-125`; `KatonItem.java:270-271`) |
| Cooldown | none (commented out, `ItemKaton.java:260`) |
| Entity spawned | `ItemKaton.EntityFireStream` (same `narutomod:katonfirestream`/ENTITY2ID 10123) via `Jutsu2` (`ItemKaton.java:253-263`) → port `KatonFireStreamEntity` |
| Procedure | `EntityFireStream.FireStream extends ProcedureAirPunch` (`ItemKaton.java:194-239`) |
| Particles | identical yellow flame cone `0xffffcf00` as Fire Annihilation (`ItemKaton.java:210-221`) |
| Sounds | loop `narutomod:flamethrow` every 10 ticks (`ItemKaton.java:178-181`); **no** start sound (unlike Annihilation) |
| Visual | Yellow flame cone particles from caster's look; no entity model / renderer |
| 1.12.2 file:line | `ItemKaton.java:67, 253-263` |

**Mechanics (original):** `Jutsu2.createJutsu`: spawn `EntityFireStream(player, width=power*0.1f, range=power)`, then override `wait=0` (fires immediately) and `maxLife=(int)(power*10)`. Same per-tick `FireStream.execute` damage/particle/ignition logic as Fire Annihilation but a narrower, longer-lived, immediately-active stream and **no** `katon_gokamekeku` start sound. This is the smaller, sustained flamethrower vs Annihilation's broad burst.

**1.20.1 status: DONE.** `KatonItem.activateGreatFlame` (`KatonItem.java:157-164`) calls shared `activateFireStream` with width=`power*0.1`, range=`power`, wait=`0`, life=`max(power*10,1)`, `playAnnihilationStartSound=false`. Reuses the same faithful `KatonFireStreamEntity` (see Fire Annihilation). Faithful.
Minor note: shares the confirmed armor-bypass regression documented under Fire Annihilation (`katon_firestream_damage.json` lacks the `bypasses_armor` tag).

---

## Summary of gaps / risks

1. **Armor bypass on Fire Stream damage (CONFIRMED bug)** — original `FireStream.attackEntityFrom` used `.setDamageBypassesArmor().setFireDamage()` (`ItemKaton.java:204-205`). Port damage type `data\narutomod\damage_type\katon_firestream_damage.json` has no `tags`, so it is NOT in `minecraft:bypasses_armor`. Fire Annihilation & Great Flame direct hits are now armor-reducible. Fix: add `narutomod:katon_firestream_damage` to a `bypasses_armor` damage-type tag.
2. **Charging visual divergence** — shared `JutsuItem`/`KatonItem.onUseTick` shows an orange `FLAME_COLORED` particle puff (`KatonItem.java:60-70`) instead of the original blue `SMOKE` cloud (`ItemJutsu.java:175-178`). Cosmetic, but differs from 1.12.2 for all four jutsu.
3. No other gaps found — all 4 jutsu, their entities, renderer, particles, sounds, and learning scrolls are present and faithfully ported.

**Unresolved references:** none. Every entity, particle, sound, renderer, and scroll referenced by the 1.12.2 Katon source resolves to a present 1.20.1 counterpart.
