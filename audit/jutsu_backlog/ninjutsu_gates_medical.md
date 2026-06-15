# Jutsu Backlog — Basic Ninjutsu + Eight Gates + Medical + Remaining Scrolls

Audit of the narutomod 1.12.2 → 1.20.1 hand-port for the "Basic Ninjutsu, Eight Gates, Medical (Iryo), and remaining jutsu-teaching scrolls" domain.

Status legend: **MISSING** (no port) · **STUB** (registered/placeholder only) · **PARTIAL** (some behavior, incomplete mechanics or missing visuals) · **DONE** (faithful mechanics + visuals).

---

## Framework / Notes

### 1.12.2 tome framework (`ItemJutsu`)
- Source: `1.12.2/.../item/ItemJutsu.java`.
- A "tome" item (`ItemNinjutsu`, `ItemIryoJutsu`, etc.) extends `ItemJutsu.Base` and holds an `ImmutableList<JutsuEnum>`. Each `JutsuEnum` (file lines 548-652) carries: `index`, `unlocalizedName` (display key), `rank` char (`S/A/B/C/D`), `requiredXP`, `chakraUsage`, and an `IJutsuCallback jutsu` (the behavior), plus `basePower`/`powerUpDelay` for charged jutsu.
- Rank → requiredXP map (line 571): `S=400, A=250, B=200, C=150, D=100, (none)=900`. Non-affinity users pay **2×** XP (`getRequiredXp`, line 259-262).
- Right-click charges (`onItemRightClick`/`onUsingTick`/`onPlayerStoppedUsing`). `getPower(...)` ramps power by `(maxUseDuration - timeLeft)/(powerupDelay * chakraModifier * xpModifier)`, capped at `getMaxPower = chakra/chakraUsage`. `executeJutsu` consumes `chakraUsage * power` then calls `jutsu.createJutsu(stack, entity, power)`.
- Per-stack NBT maps: cooldown map (`JutsuCDMapKey<i>`, `-1` = jutsu locked/not learned, `>=0` = unlocked), XP map (`JutsuExperienceMapKey`), owner UUID, affinity flag, current jutsu index. Gating (`canActivateJutsu`/`onItemRightClick`, lines 459-507): owner check + XP >= requiredXP + `PlayerTracker.isNinja` + cooldown.
- Charging visuals (base `onUsingTick`, lines 170-185): colored SMOKE particles `0x106AD1FF` + `narutomod:charging_chakra` sound every 10 ticks + status text of current power.

### 1.20.1 tome framework (`JutsuItem`)
- Source: `1.20.1/.../item/JutsuItem.java`. Faithful re-implementation: `JutsuDefinition` record (index, translationKey, rank, requiredXp, chakraUsage), same NBT tag names (`JutsuIndexKey`, `JutsuCDMapKey`, `JutsuExperienceMapKey`, `OwnerIdKey`, `IsNatureAffinityKey`), same rank→XP table and 2× non-affinity multiplier. `JutsuDefinition.ranked(idx, key, rank, chakra)` is the equivalent constructor. Helpers: `canUseJutsu`, `hasEnoughJutsuXp`, `getRemainingCooldownTicks`, `setJutsuCooldown`, `addJutsuXp`, `isOwnedByOrUnbound`, `setOwnerIfMissing`, `getChargingPower`, `onMaxedOut`.

### Scroll "tome" items
- In 1.12.2 the nine `ItemScroll*` files are **GUI-opening teach-scrolls**, not jutsu performers. Each is a trivial `Item` that on right-click opens a `Gui*` (`openGui(...)`) and shows a rank tooltip ("X-rank jutsu scroll"). They contain **no jutsu mechanics** — they let the player learn/level the jutsu that lives in a tome (`ItemNinjutsu`, `ItemIryoJutsu`, `ItemInton`, `ItemFuton`, …).
- 1.20.1 collapses all of these into **one** generic `JutsuScrollItem` + the `JutsuScrollDefinition` enum (`1.20.1/.../item/JutsuScrollDefinition.java`), backed by `JutsuScrollMenu`/`menu.JutsuScrollMenu`. `ScrollRasenganItem` is the only dedicated subclass and just passes `JutsuScrollDefinition.RASENGAN`. Each enum entry maps id → title key → jutsu-name key → rank tooltip → icon texture → target tome item → target `JutsuDefinition`.

### Shared registries used by ports in this domain
- `registry.ModEntityTypes` (RASENGAN etc.), `registry.ModSounds` (SOUND_CHARGING_CHAKRA, SOUND_RASENGAN_START, SOUND_BUGS, SOUND_OPENGATE, SOUND_EIGHTGATESRELEASE, SOUND_EXPLOSION, SOUND_WINDECHO), `registry.ModParticleTypes` + `particle.NarutoParticleKind.SMOKE_COLORED`, `registry.ModEffects` (FLIGHT, CHAKRA_ENHANCED_STRENGTH), `Chakra.pathway`, `PlayerTracker`, `procedure.ProcedureUtils`.
- All entities referenced below exist in 1.20.1 (`entity/*Entity.java`), and the Eight-Gates visual entities have renderers + models present:
  `client/renderer/HirudoraRenderer.java` + `client/model/HirudoraTigerModel.java`, `client/renderer/NightGuyDragonRenderer.java` + `client/model/NightGuyDragonModel.java`, `client/renderer/SekizoRenderer.java` + `client/model/LegacyLongCubeModel.java`, `client/renderer/AsakujakuFireballRenderer.java`, `client/renderer/CellularActivationRenderer.java`.

> Per-entity render fidelity (textures, animation, particle counts, beam scaling) was NOT exhaustively diffed for this domain audit — see the dedicated entity/render backlog for that. The renderer/model classes are confirmed to exist; statuses below mark visuals DONE where the spawning item-side logic + entity wiring are present.

---

# A. Basic Ninjutsu (`ItemNinjutsu` → `NinjutsuItem`)

Original tome: `1.12.2/.../item/ItemNinjutsu.java` (jutsu list lines 59-67).
Port tome: `1.20.1/.../item/NinjutsuItem.java` (jutsu list lines 42-50). All 9 entries present with matching index/rank/chakra.

## Replacement Clone (Kawarimi / Body Replacement)
| Field | Value |
|---|---|
| index | 0 |
| display key | `replacementclone` |
| rank / requiredXP | D / 100 (×2 non-affinity) |
| chakra | 30.0 |
| cooldown | 100 ticks (`COOLDOWN`, 1.12.2 line 198) |
| entities | `EntityReplacementClone` (extends `EntityClone.Base`); ported `entity.ReplacementCloneEntity` |
| procedures | `ProcedureUtils.getYawFromVec`; griefing log-block drop via `EntityFallingBlock` |
| particles | 300× `EXPLOSION_NORMAL` on `setDead` (1.12.2 lines 182-185) |
| sounds | `narutomod:poof` on death (line 163) |
| visual | Player teleports behind attacker; a clone is left and after 40 ticks poofs into smoke + drops a log block |
| 1.12.2 ref | `ItemNinjutsu.java:59` (enum), `:141-237` (entity + `Jutsu.Hook` LivingAttackEvent toggle) |

**1.20.1 status: DONE.** Toggle-on-use via `toggleReplacement` (NinjutsuItem.java:304-319); `LivingAttackEvent` interception in `ForgeEvents.onLivingAttack` (:710-747) with the 100-tick `REPLACEMENT_COOLDOWN_TICKS`, chakra consume, and `ReplacementCloneEntity.spawnFrom`. Faithful.

## Shadow Clone (Kage Bunshin)
| Field | Value |
|---|---|
| index | 1 |
| display key | `kage_bunshin` |
| rank / requiredXP | B / 200 |
| chakra | 0.0 in enum (cost handled inside `EntityKageBunshin.EC.Jutsu`) |
| entities | `EntityKageBunshin` (`EC.Jutsu` callback); ported `entity.KageBunshinEntity` |
| visual | Spawns shadow clone(s); standard clone model + poof |
| 1.12.2 ref | `ItemNinjutsu.java:60` |

**1.20.1 status: DONE.** `useKageBunshin` (NinjutsuItem.java:321-339): `KageBunshinEntity.spawnFrom`, shift-click `removeAllFor`. Faithful.

## Rasengan
| Field | Value |
|---|---|
| index | 2 |
| display key | `rasengan` |
| rank / requiredXP | A / 250 |
| chakra | 150.0 |
| max power | clamped to 3.0 (`getMaxPower`, 1.12.2 lines 110-114) |
| charge | powerUp over 200f (`getPower(...,0f,200f)`, line 120) |
| entities | `EntityRasengan` (`EC.Jutsu`); ported `entity.RasenganEntity` |
| visual | Spinning blue sphere held/thrown; scales with power |
| sounds | rasengan start sound on release |
| 1.12.2 ref | `ItemNinjutsu.java:61`, `:110-125` |

**1.20.1 status: DONE.** Full charge/release in `releaseUsing` (:168-207) via `RasenganProfile` (basePower 0, minPower 0.5, maxPower 3.0, powerupDelay 200, cooldown 40). `RASENGAN_SIZE_TAG`/`RASENGAN_LEARNED_TAG`, clone Rasengan (`spawnCloneRasengan`), `ModSounds.SOUND_RASENGAN_START`, battle-XP logging. Faithful and arguably richer.

## Limbo Clone (Limbo: Border Jail)
| Field | Value |
|---|---|
| index | 3 |
| display key | `limbo_clone` |
| rank / requiredXP | S / 400 |
| chakra | `EntityLimboClone.CHAKRA_USAGE` |
| entities | `EntityLimboClone` (`EC.Jutsu`); ported `entity.LimboCloneEntity` |
| visual | Invisible Limbo clone(s) that intercept attacks |
| 1.12.2 ref | `ItemNinjutsu.java:62` |

**1.20.1 status: DONE.** `useLimboClone` (:341-365): single-active guard, chakra consume w/ refund-on-fail, `LimboCloneEntity.spawnPairFrom`, `COOLDOWN_TICKS`; attack interception in `onLivingAttack` (:721). Faithful.

## Sealing Chains (Adamantine Sealing Chains)
| Field | Value |
|---|---|
| index | 4 |
| display key | `sealing_chains` |
| rank / requiredXP | A / 250 |
| chakra | 50.0 |
| entities | `EntitySealingChains` (`EC.Jutsu`); ported `entity.SealingChainsEntity` |
| visual | Golden chains binding a target |
| 1.12.2 ref | `ItemNinjutsu.java:63` |

**1.20.1 status: DONE.** `useSealingChains` (:367-398): `findTarget`, chakra consume + refund-on-fail, `spawnFrom(player,target)`, shift-click `retractOwnedNear`. Faithful.

## Puppet Technique (Kugutsu no Jutsu)
| Field | Value |
|---|---|
| index | 5 |
| display key | `tooltip.ninjutsu.puppetjutsu` |
| rank / requiredXP | C / 150 |
| chakra | 0.25 |
| entities | targets `EntityPuppet.Base`; ported `entity.AbstractPuppetEntity` |
| behavior | `PuppetJutsu.createJutsu`: if looking at a puppet within 4 blocks, bind/control it (1.12.2 lines 239-247) |
| 1.12.2 ref | `ItemNinjutsu.java:64`, `:239-247` |

**1.20.1 status: DONE.** `usePuppet` (:400-426): `findLookedAtPuppet` (4-block raytrace), `puppet.bindTo(player)`, shift-click `releaseOwnedNear`. Faithful (and adds explicit chakra cost guard).

## Bug Swarm / Bug Ball (Kikaichu)
| Field | Value |
|---|---|
| index | 6 |
| display key | `bugball` |
| rank / requiredXP | C / 150 |
| chakra | 100.0 |
| charge | powerUp over 100f (`getPower(...,0f,100f)`, 1.12.2 line 122) |
| entities | `EntityKikaichu` (`EC.Jutsu`); ported `entity.BugSwarmEntity` |
| visual | Charged ball/swarm of insects launched at target |
| 1.12.2 ref | `ItemNinjutsu.java:65`, `:121-123` |

**1.20.1 status: DONE.** Charged via `beginBugSwarmCharge`/`releaseBugSwarm` (:428-478): `getBugSwarmPower` (powerupDelay 100, minPower 0.1), `findTarget`, chakra consume + refund, `spawnFrom(player,target,power)`, `ModSounds.SOUND_BUGS`, shift-click `returnOwnedNear`, look-at recall of existing swarm. Faithful.

## Hiding in Camouflage / Invisibility
| Field | Value |
|---|---|
| index | 7 |
| display key | `tooltip.ninjutsu.hidingincamouflage` |
| rank / requiredXP | A / 250 |
| chakra | 20.0 per 20-tick upkeep |
| behavior | Toggle; while active, every 20 ticks consume chakra and re-apply `INVISIBILITY` potion (21t). `Hook.onSetAttackTarget` cancels mob targeting of invisible players unless they wear Sharingan/Byakugan (1.12.2 lines 249-280, upkeep 128-138) |
| 1.12.2 ref | `ItemNinjutsu.java:66`, `:249-280` |

**1.20.1 status: DONE.** `toggleHidingInCamouflage` (:513-530); upkeep in `inventoryTick` (:127-140) applies INVISIBILITY + consumes 20 chakra/20t, deactivates on empty chakra. Mob-target cancel in `ForgeEvents.onLivingTick` (:693-707) with `SusanooPowerIncreaseHandler.isSharinganHead`/`ByakuganHandler.isByakuganHead` exemption. Faithful.

## Transformation Jutsu (Henge)
| Field | Value |
|---|---|
| index | 8 |
| display key | `transformation_jutsu` |
| rank / requiredXP | D / 100 |
| chakra | 50.0 |
| entities | `EntityTransformationJutsu` (`EC.Jutsu`); ported `entity.TransformationJutsuEntity` |
| visual | Smoke poof, player disguised as target entity |
| 1.12.2 ref | `ItemNinjutsu.java:67` |

**1.20.1 status: DONE.** `useTransformation` (:480-511): single-active guard, `findTarget`, chakra consume + refund, `spawnFrom(player,target)`, shift/active-toggle `stopFor`; attack interception in `onLivingAttack` (:717). Faithful.

> NinjutsuItem.java unported-jutsu guard at :114-119 only triggers if a definition has no branch — all 9 are branched, so no gaps remain.

---

# B. Eight Gates (`ItemEightGates` → `EightGatesItem`)

Original: `1.12.2/.../item/ItemEightGates.java` (1326 lines). Port: `1.20.1/.../item/EightGatesItem.java` (405 lines).
This is a **single item with 8 gate levels** (not a JutsuEnum tome). Right-click + sneak ramps `gateOpened` by 0.05/tick; release at the right gate fires a signature technique. Battle-XP (`battleExperience`) gates how high you may open. Gate stats table (1.12.2 lines 224-232) is copied verbatim into 1.20.1 `GATES[]` (lines 47-57): xpRequired, particles, particleColor, strength, speed, resistance, health (max-HP modifier), damage-per-10-ticks, canFly.

Per-gate buffs while held (`activate`, 1.12.2 :152-174 → port `activateGate` :286-320): SATURATION, JUMP_BOOST 8, HASTE 3, STRENGTH=gate.strength, RESISTANCE=gate.resistance, SPEED=gate.speed; self-damage gate.damage/10t; flight at gates 7-8; MAX_HEALTH attribute modifier (UUID `f6944d0f-…`). Deactivate (`deActivate` :176-195 → `deactivateGate` :322-334): WEAKNESS + SLOWNESS debuff scaled by gate; gate 8 sets death animations; cooldown = gate×200.

## Gate 1 — Kaimon (Gate of Opening)
| Field | Value |
|---|---|
| gate index | 1 |
| xpRequired | 220 |
| stats | str 3, spd 2, res 0, +HP 10, dmg −1, particles 0 |
| name key | `chattext.eightgates.gate1` |
| 1.12.2 ref | `ItemEightGates.java:225` |

**1.20.1 status: DONE** — buff-only gate, `GATES[1]` (EightGatesItem.java:49); `activateGate` applies all effects. Faithful.

## Gate 2 — Kyumon (Gate of Healing)
| Field | Value |
|---|---|
| gate index | 2 |
| xpRequired | 240 |
| stats | str 4, spd 16, res 0, +HP 40, dmg −5, particles 0 |
| melee | gate ≥ 2 left-click pushes target (`pushEntity(...,gate*0.2+2)`) — 1.12.2 :362-365 |
| 1.12.2 ref | `ItemEightGates.java:226`, `:362-365` |

**1.20.1 status: DONE** — `GATES[2]`; knockback in `onLeftClickEntity` (:149-151). Faithful.

## Gate 3 — Seimon (Gate of Life)
| Field | Value |
|---|---|
| gate index | 3 |
| xpRequired | 280 |
| stats | str 6, spd 32, res 1, +HP 60, dmg −3, particles 20, color `0x10FFFFFF` |
| visual | gate ≥ 3: player color multiplier turns red (`PlayerRender.setColorMultiplier 0xB0B00000`, 1.12.2 :469-471) |
| 1.12.2 ref | `ItemEightGates.java:227` |

**1.20.1 status: PARTIAL.** Stats/effects faithful (`GATES[3]`). **Missing visual:** the red-skin `PlayerRender.setColorMultiplier` tint at gate ≥ 3 (and the deactivate reset) is not reproduced in `EightGatesItem` (no `PlayerRender` call). Knockback + buffs present. Restore the red overlay tint on the player while gate ≥ 3.

## Gate 4 — Shomon (Gate of Pain)
| Field | Value |
|---|---|
| gate index | 4 |
| xpRequired | 360 |
| stats | str 9, spd 64, res 2, +HP 60, dmg +1.2, particles 25, color `0x18FFFFFF` |
| visual | gate ≥ 4 charge: emits white smoke + `opengate`/`explosion` sounds while opening (1.12.2 :446-467) |
| 1.12.2 ref | `ItemEightGates.java:228` |

**1.20.1 status: DONE.** `GATES[4]`; opening particles/sounds in `spawnOpeningParticles`/`playOpeningSounds` (:343-369). Faithful.

## Gate 5 — Tomon (Gate of Limit)
| Field | Value |
|---|---|
| gate index | 5 |
| xpRequired | 520 |
| stats | str 21, spd 68, res 2, +HP 60, dmg +1.4, particles 30, color `0x20FFFFFF` |
| 1.12.2 ref | `ItemEightGates.java:229` |

**1.20.1 status: DONE** — `GATES[5]`; same opening-FX path. Faithful (see Gate 3 note re: red tint for gates ≥3, applies here too).

## Gate 6 — Keimon (Gate of View) — **Asakujaku (Morning Peacock)**
| Field | Value |
|---|---|
| gate index | 6 |
| xpRequired | 840 |
| stats | str 44, spd 72, res 3, +HP 60, dmg +1.6, particles 30, color `0x3000FF00` |
| technique | Left-click fires **Asakujaku**: 10 fast `EntitySmallFireball`s (motionFactor 1.1), each deals 0.5× modified attack damage + sets fire 10t + size-2 explosion, self-deletes after 12 ticks (1.12.2 `attackAsakujaku` :300-341) |
| entities | anonymous `EntitySmallFireball` subclass; ported `entity.AsakujakuFireballEntity` (+ `AsakujakuFireballRenderer`) |
| name key | `entity.entityasakujaku.name` |
| 1.12.2 ref | `ItemEightGates.java:230`, `:300-341`, `:357-360` |

**1.20.1 status: DONE.** `onLeftClickEntity` gate==6 → `AsakujakuFireballEntity.spawnBurst(player)` (:145-148); dedicated entity + renderer exist. Faithful (verify burst count = 10 and per-fireball damage/fire/explosion match in the entity backlog).

## Gate 7 — Kyomon (Gate of Wonder) — **Hirudora (Daytime Tiger)**
| Field | Value |
|---|---|
| gate index | 7 |
| xpRequired | 1480 |
| stats | str 84, spd 76, res 4, +HP 60, dmg +1.8, particles 30, color `0x300000FF`, **canFly** |
| technique | Release fires **Hirudora**: `EntityHirudora extends EntityScalableProjectile.Base`, OG size 1.0×0.5, charges to fullScale 6 over 20t (`NGD_SUSPEND_TIME`) at a "wait position", then launches; impact = AoE 0.5×scale armor-bypass damage ×3.0 + `newExplosion(...,70.0F)` + sets owner InvulnerableTime 40. Smoke particle cloud (`renderParticles`, 1.12.2 :682-688) |
| entities | `EntityHirudora`; ported `entity.HirudoraEntity` (+ `HirudoraRenderer` using `HirudoraTigerModel` / `HirudoraTiger.png`) |
| sounds | `narutomod:hirudora` on spawn (1.12.2 :280) |
| cooldown | 400 ticks (1.12.2 :269) |
| name key | `entity.entityhirudora.name` |
| visual | A white tiger-head fist of compressed air; huge explosion |
| 1.12.2 ref | `ItemEightGates.java:231`, `:276-283` (spawn), `:631-708` (entity), `:828-853` (render), `:1269-1323` (model) |

**1.20.1 status: DONE.** `releaseUsing` gate==7 → `HirudoraEntity.spawnFrom(player)` + 400t cooldown (EightGatesItem.java:109-117). Entity + renderer + tiger model present. Faithful (verify scale ramp, ×3 damage, explosion radius 70, sound in entity backlog).

## Gate 8 — Shimon (Gate of Death) — **Sekizo + Yagai (Night Guy Dragon)**
| Field | Value |
|---|---|
| gate index | 8 |
| xpRequired | 2760 |
| stats | str 349, spd 80, res 5, +HP 60, dmg +2.0, particles 30, color `0x30FF0000`, **canFly** |
| death cost | opening gate 8 triggers `setDeathAnimations(2,200)` (slow death) on activate/deactivate |
| technique A — **Sekizo** | Left-click (gate 8) fires `EntitySekizo extends EntityBeamBase.Base`: a 1-of-4 combo punch (`sekizoPunchCount`), beam range 30, damage = modifiedAttackDamage × 2^punch via `AirPunch` (ProcedureAirPunch, blockDrop 0.2). Sound `narutomod:sekizo`. Beam rendered as `ModelLongCube` (longcube_white.png), grows over 30t (1.12.2 `attackSekizo` :285-298, entity :586-629, render :856-873, model :907-938) |
| technique B — **Night Guy Dragon (Yagai)** | Release (gate 8) spawns `EntityNGDragon extends EntityScalableProjectile.Base`, OG 1.0×1.0, fullScale 6 over 20t, drags the shooter with it; impact = single-target ×64 armor-bypass dmg + knockback + AoE ×16 + `newExplosion(...,10.0F)`. Flame particles charging / dark-red smoke launched. Sound `narutomod:yagai`. Cooldown 200t + death animation. Rendered with custom `ModelNightguyDragon` (dragon_red.png) w/ animated spine/whiskers (1.12.2 `onPlayerStoppedUsing` case 8 :252-264, entity :710-825, render :876-904, model :941-1265) |
| name keys | `entity.entitysekizo.name` (formatted w/ punch#), `entity.entityngdragon.name` |
| 1.12.2 ref | `ItemEightGates.java:232`, `:252-264`, `:285-298`, `:586-629`, `:710-825` |

**1.20.1 status: DONE.** Sekizo: `onLeftClickEntity` gate==8 → `SekizoEntity.spawnFrom(player, punch)` with `getSekizoPunchNum` 0-3 combo (:130-144) + `SekizoRenderer`/`LegacyLongCubeModel`. Night Guy Dragon: `releaseUsing` gate==8 → `NightGuyDragonEntity.spawnFrom(player)` + death animation + 200t cooldown (:118-127) + `NightGuyDragonRenderer`/`NightGuyDragonModel`. Faithful (verify damage multipliers ×64/×16, scale ramp, sounds `sekizo`/`yagai`, animated model in entity backlog).

**Eight Gates summary status: DONE** except **Gate 3+ red player-tint visual (PARTIAL — see Gate 3).** Flight is reimplemented via `ModEffects.FLIGHT` effect instead of the 1.12.2 `capabilities.allowFlying` toggle — behavioral equivalent, acceptable.

---

# C. Medical / Iryo Jutsu (`ItemIryoJutsu` → `IryoJutsuItem`)

Original: `1.12.2/.../item/ItemIryoJutsu.java`. Port: `1.20.1/.../item/IryoJutsuItem.java`. All 4 entries present (indices/ranks/chakra match).

## Healing Jutsu (Shōsen Jutsu)
| Field | Value |
|---|---|
| index | 0 |
| display key | `healingjutsu` |
| rank / requiredXP | A / 250 |
| chakra | 0.25 per tick |
| behavior | Hold-use channel: raytrace 3 blocks for a `EntityLivingBase` (or self); each tick heal `power*0.01`, apply SLOWNESS 6 (80t) to target, power = `chakraLevel * xpRatio / 15`. Cancels right-click-entity interaction via `PlayerHook` (1.12.2 :109-153) |
| particles | cyan smoke `0x0000fff6` over target |
| sounds | `narutomod:windecho` every 3 ticks, pitch via sin wave |
| 1.12.2 ref | `ItemIryoJutsu.java:46`, `:109-153` |

**1.20.1 status: DONE.** `onUseTick` HEALING (:90-104, `healTarget` :296-320): same 3-block target/self select, `power*0.01` heal, SLOWNESS 6/80t, cyan `0x..00FFF6` particle, `ModSounds.SOUND_WINDECHO` sin-pitch every 3t. Charge gating via `canActivateIryo`. Faithful.

## Poison Mist (Doku Giri)
| Field | Value |
|---|---|
| index | 1 |
| display key | `poison_mist` |
| rank / requiredXP | B / 200 |
| chakra | 20.0 × power |
| charge | `getPower(...,5f,15f)` → base 5, powerUp 15 |
| entities | `EntityPoisonMist` (`EC.Jutsu`); ported `entity.PoisonMistEntity` |
| visual | Purple/green poison gas cloud expelled forward |
| 1.12.2 ref | `ItemIryoJutsu.java:47`, `:76-79` |

**1.20.1 status: DONE.** Charged `tickPoisonMistCharge`/`activatePoisonMist` (:172-208): `getPoisonMistPower` (base 5, powerupDelay 15, chakra-capped), purple smoke `0x40630065` charge particles + `SOUND_CHARGING_CHAKRA`, `PoisonMistEntity.spawnFrom(player,power)`. Faithful.

## Cellular Activation / Medical Mode (Sōzō Saisei buildup)
| Field | Value |
|---|---|
| index | 2 |
| display key | `cellular_activation` |
| rank / requiredXP | A / 250 |
| chakra | 0.0 enum (handled by entity) |
| entities | `EntityCellularActivation` (`EC.Jutsu`); ported `entity.CellularActivationEntity` (+ `CellularActivationRenderer`) |
| visual | Glowing regeneration aura/marks on the user |
| 1.12.2 ref | `ItemIryoJutsu.java:48` |

**1.20.1 status: DONE.** Toggle via `toggleCellularActivation` → `CellularActivationEntity.toggleFor(player)` (:210-221); renderer present. Faithful.

## Chakra Enhanced Strength (Power Mode)
| Field | Value |
|---|---|
| index | 3 |
| display key | `enhanced_strength` |
| rank / requiredXP | A / 250 |
| chakra | 30.0 to toggle; upkeep adds `PotionChakraEnhancedStrength` each tick at amp `xpModifier*chakraLevel/2` |
| behavior | Toggle; while active apply the custom potion. On hit, consume chakra to add bonus damage + knockback; faster block breaking (1.12.2 :49, :100-106, :155-171) |
| potion | `potion.PotionChakraEnhancedStrength` → ported `registry.ModEffects.CHAKRA_ENHANCED_STRENGTH` |
| 1.12.2 ref | `ItemIryoJutsu.java:49`, `:155-171` |

**1.20.1 status: DONE.** `toggleEnhancedStrength` (:154-170) + upkeep in `inventoryTick` (:123-134) re-applying `CHAKRA_ENHANCED_STRENGTH` at amp `xpRatio*chakraLevel/2`. `ForgeEvents.onLivingHurt` (:331-348) adds strength damage + knockback + explode sound; `onBreakSpeed` (:350-361) speeds mining. Faithful (1.12.2 had no explicit on-hit handler in this file — port adds the effect's gameplay, equivalent to the original potion behavior).

---

# D. Remaining Teaching Scrolls (GUI tome-scrolls)

All nine 1.12.2 `ItemScroll*` items are pure GUI-openers with no jutsu mechanics. In 1.20.1 they are unified into `JutsuScrollItem` + `JutsuScrollDefinition` (+ `ScrollRasenganItem` subclass) backed by `JutsuScrollMenu`. Each maps to a target tome's `JutsuDefinition` (the actual jutsu mechanics live in those tomes and are covered by their own audits). Statuses below reflect the **scroll wiring**, not the underlying jutsu.

| Scroll (1.12.2) | rank tooltip | GUI (1.12.2) | 1.12.2 ref | 1.20.1 mapping | status |
|---|---|---|---|---|---|
| `ItemScrollKageBunshin` | B-rank | `GuiScrollKageBunshinGui` | `:82` | `JutsuScrollDefinition.KAGE_BUNSHIN` → `NinjutsuItem.KAGE_BUNSHIN` | **DONE** |
| `ItemScrollTransformation` | D-rank | `GuiScrollTransformationGui` | `:82` | `TRANSFORMATION` → `NinjutsuItem.TRANSFORM` | **DONE** |
| `ItemScrollBodyReplacement` | D-rank | `GuiScrollBodyReplacementGui` | `:82` | `BODY_REPLACEMENT` → `NinjutsuItem.REPLACEMENT` | **DONE** |
| `ItemScrollShadowImitation` | "Hidden jutsu scroll" | `GuiScrollShadowImitationGui` | `:82` | `SHADOW_IMITATION` → `IntonItem.SHADOW_IMITATION` | **DONE** |
| `ItemScrollMindTransfer` | "Hidden C-rank jutsu scroll" | `GuiScrollMindTransferGui` | `:82` | `MIND_TRANSFER` → `IntonItem.MIND_TRANSFER` | **DONE** |
| `ItemScrollGenjutsu` | B-rank | `GuiScrollGenjutsuGui` | `:82` | `GENJUTSU` → `IntonItem.GENJUTSU` | **DONE** |
| `ItemScrollEnhancedStrength` | A-rank | `GuiScrollEnhancedStrengthGui` | `:82` | `ENHANCED_STRENGTH` → `IryoJutsuItem.ENHANCED_STRENGTH` | **DONE** |
| `ItemScrollRasengan` | A-rank | `GuiScrollRasenganGui` | `:82` | `RASENGAN` → `NinjutsuItem.RASENGAN` (dedicated `ScrollRasenganItem`) | **DONE** |
| `ItemScrollRasenshuriken` | S-rank | `GuiScrollRasenshurikenGui` | `:82` | `RASENSHURIKEN` → `FutonItem.RASENSHURIKEN` | **DONE** |

Notes:
- 1.20.1 `JutsuScrollDefinition` additionally defines `HIDING_IN_CAMOUFLAGE`, `BUG_SWARM`, `HEALING`, `POISON_MIST`, `CELLULAR_ACTIVATION` scrolls (no separate 1.12.2 `ItemScroll*` files in this list — they were taught by other means in 1.12.2). These are **extra/new wiring**, not regressions.
- `MedicalScrollItem` (`ItemMedicalScroll` → `MedicalScrollItem`) is a **crafting/combination** scroll, not a teach-scroll: opens `MedicalScrollMenu`/`GuiMedicalScrollGUI` to combine dojutsu helmets (Eternal Mangekyo from two Mangekyo of different owners; Byakugan→Tenseigan progression). 1.12.2 just opened the GUI (`:73`); 1.20.1 implements the full recipe logic server-side (`MedicalScrollItem.activate` :45-148). **status: DONE** (this is a medical-scroll item but is dojutsu-crafting; cross-reference the dojutsu audit for the helmet recipes).

---

## Outstanding work for this domain
1. **Eight Gates Gate-3+ red player tint** — restore `PlayerRender.setColorMultiplier` red overlay while gate ≥ 3 and the reset on deactivate (only visual gap found). (`ItemEightGates.java:469-471`, `:193`)
2. **Entity-level render/damage fidelity** for Asakujaku, Hirudora, Sekizo, Night Guy Dragon, Poison Mist, Cellular Activation should be confirmed in the dedicated entity/render backlog (renderer + model classes exist; spawning logic is faithful, but per-entity scale ramps, damage multipliers, explosion radii, particle counts, and animation were not byte-diffed here).
3. **Unresolved references:** none. All entities, renderers, models, effects, sounds, particles, and menus referenced by the ported items were located in 1.20.1.
