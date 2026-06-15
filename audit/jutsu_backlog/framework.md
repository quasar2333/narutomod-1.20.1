# Jutsu Framework / Chakra / Ninja Level / Learning / Cooldown / Commands — Port Audit

Domain audit comparing the 1.12.2 (Forge) source of truth against the 1.20.1 (Forge) work-in-progress port.
Scope: the generic jutsu framework (`ItemJutsu`), the **Ninjutsu tome** (`ItemNinjutsu`, 9 techniques), chakra system (`Chakra`), ninja-level / battle-XP tracking (`PlayerTracker`, `NarutomodModVariables`), and the three relevant commands (`addninjaxp`, `addxp2jutsu`, `locateEntity`).

This file is exhaustive per-technique. Element-specific tomes (Katon, Suiton, Doton, Futon, Raiton, Inton, Iryo, Yoton, etc.) are OUT OF SCOPE here — they have their own backlog files. Only their *learning hooks* appear here because `JutsuScrollDefinition` (the port's unified scroll registry) references them.

---

## Framework / shared notes

### 1.12.2 framework anatomy (`item/ItemJutsu.java`)
- **`ItemJutsu.Base extends Item`** (line 105) — abstract base for every jutsu tome. Holds:
  - NBT keys: `JutsuIndexKey` (current selected jutsu), `JutsuCDMapKey<i>` (per-jutsu cooldown / enable flag; `-1` = locked/not-learned, `>=0` = learned, `>worldtime` = on cooldown), `JutsuExperienceMapKey` (int[] per-jutsu XP), `OwnerIdKey` (bound owner UUID), `IsNatureAffinityKey` (affinity → halves required XP).
  - `executeJutsu` (134): checks `power>0` and chakra `>= chakraUsage*power`, calls `JutsuEnum.jutsu.createJutsu`, then `pw.consume(chakraUsage*power)`.
  - Charging model: held like a BOW (`getItemUseAction`=BOW, max use 72000 ticks). `onUsingTick` (170) spawns blue smoke particles (`0x106AD1FF`) and plays `narutomod:charging_chakra` every 10 ticks; shows live power as status message. `getPower(...,basePower,powerupDelay)` (151) = `basePower + (maxUse-timeLeft)/(powerupDelay*modifier)`, capped at `getMaxPower`. `onPlayerStoppedUsing` (188) runs `executeJutsu`, +1 XP on success.
  - `getModifier` (160) = `Chakra.getChakraModifier(entity) * getCurrentJutsuXpModifier`. `getMaxPower(entity,usage)` (101) = `Chakra.amount / usage * 0.9999`.
  - Learn / enable: `enableJutsu(stack,jutsu,true)` flips CD flag from `-1` to `0`. `canActivateJutsu` (459) gates on owner + enabled + `jutsuXp >= requiredXp` + `PlayerTracker.isNinja` + cooldown.
  - Owner binding: `isOwner` (429) auto-binds on first tick and resets maps; `onUpdate` (438) refuses to operate if not owner. `onDroppedByPlayer` returns false (can't drop).
  - `switchNextJutsu` / `setNextJutsu` (361) cycles to the next usable jutsu and sends its name as status message.
  - Tooltip (392) lists usable jutsu with `>`-marker on current, plus XP/required.
- **`JutsuEnum`** (548) — a technique descriptor: `index`, `unlocalizedName`, `rank` (S/A/B/C/D, blank=900xp), `requiredXP`, `chakraUsage`, `IJutsuCallback jutsu`, `Type`, `basePower`, `powerUpDelay=50`. Rank→XP table (571): S=400, A=250, B=200, C=150, D=100, default=900. Required XP is **doubled if not affinity** (`getRequiredXp` 259).
- **`IJutsuCallback`** (542): `createJutsu(stack,entity,power)→bool`, `isActivated`, `getPower`. This is where each technique's actual behavior lives (often delegated to the spawned Entity's `Jutsu` inner class).
- **Damage types**: `ninjutsu_damage` (`NINJUTSU_DAMAGE`), `senjutsu_damage` (`SENJUTSU_DAMAGE`); helper `causeJutsuDamage`, `causeSenjutsuDamage`, `isDamageSourceNinjutsu`, `canTarget` (excludes `kamui_intangible`).
- **XP plumbing**: `logBattleXP` (74) +1 to current jutsu when held during combat; `addBattleXP` (87) adds N. Called from `PlayerTracker.logBattleExp` and `ProcedureAddXP2JutsuCommandExecuted`.
- **`Type` enum** (623): 27 elemental/dojutsu categories.

### 1.20.1 framework anatomy (`item/JutsuItem.java`)
- **`JutsuItem extends Item`** — faithful re-implementation. Same NBT tag names (`JutsuIndexKey`, `JutsuCDMapKey`, `JutsuExperienceMapKey`, `OwnerIdKey`, `IsNatureAffinityKey`). Record **`JutsuDefinition`** (319) replaces `JutsuEnum` (fields: index, translationKey, rank, requiredXp, chakraUsage, basePower, powerUpDelay, type) with the same rank→XP table (S400/A250/B200/C150/D100/900) and same affinity-doubling logic.
- Owner binding via `inventoryTick` (47) auto-binds + `resetJutsuMaps`. `enableJutsu`, `isJutsuEnabled`, cooldown map (`-1` locked), `canUseJutsu`, `hasEnoughJutsuXp`, `switchToNextUsableJutsu`, affinity, `getChargingPower` — all present and matching semantics. Owner helpers delegate to `ProcedureUtils.getOwnerId` / `setOriginalOwner` (a port-specific persistence layer).
- `JutsuType` enum (352) — all 27 categories preserved.
- **MISSING from `JutsuItem` base vs 1.12.2**: the generic BOW-charging particles/sound (`onUsingTick`) and `onPlayerStoppedUsing` live **per-item** now (e.g. `NinjutsuItem.onUseTick`/`releaseUsing`) rather than in the base — acceptable refactor. The base no longer owns `ItemJutsu.causeJutsuDamage`/`SENJUTSU_DAMAGE` damage-source constants; damage typing moved to vanilla `DamageTypes` / per-entity (see entity backlog). `canTarget`/`kamui_intangible` guard not in base (handled per-entity).

### Learning system (NEW in port; replaces NBT-only enable in 1.12.2)
- **`item/JutsuLearning.java`** — `learn(player, targetItem, definition, displayName)`: requires ninja XP (or creative), finds owned/unbound stack (or creates one, binds owner), calls `item.enableJutsu(...)`, special-cases Rasengan via `NinjutsuItem.setRasenganLearned`. Grants advancement `narutomod:learned_1st_jutsu`. This is the port's "learn a jutsu from a scroll" flow.
- **`item/JutsuScrollDefinition.java`** — enum registry of **35 learnable scrolls** mapping scroll id → title key → jutsu name key → rank tooltip → icon texture → target `JutsuItem` → `JutsuDefinition`. Covers all 6 Ninjutsu scrolls in scope (RASENGAN, BODY_REPLACEMENT, KAGE_BUNSHIN, HIDING_IN_CAMOUFLAGE, TRANSFORMATION, BUG_SWARM) plus 29 element scrolls (out of scope here). **Note: LIMBO_CLONE, SEALING_CHAIN, PUPPET have NO scroll entry** — they are not learnable through the scroll UI in the port (in 1.12.2 they were also enabled only via NBT/command, never had a dedicated scroll), so this is consistent, but flag it.

### Chakra system
- **1.12.2 `Chakra.java`**: `Pathway`/`PathwayPlayer` per entity. Max = `PlayerTracker.getBattleXp * 0.5`. `getLevel` = `sqrt(max(amount,max))`. `getChakraModifier` = `ProcedureUtils.getCDModifier(level)`. Regen when motionless >100 ticks (`consume(-0.006f)` every 80t) and full regen when all players asleep (`consume(-0.6f)`). Overflow drains 10/20t. Low-chakra (<10, max>150) applies Weakness/Slowness/Nausea III. Network `Message` syncs amount+max to client; `PlayerHook` handles death (reset to 10), respawn clone, dimension change, login.
- **1.20.1 `Chakra.java`**: faithful pathway. `PlayerPathway` reads/writes `ChakraPathwaySystem` via `NarutomodModVariables` capability; max = `battleXp*0.5`. Overflow drain, low-chakra effects (Weakness/Slowness/Confusion III), motionless regen, all-asleep regen, death-reset-to-10, clone cleanup all present. Warning via `ChakraWarningMessage` network packet.
  - **CONFIRMED DISCREPANCY (PARTIAL): `Chakra.getChakraModifier` curve changed.** 1.12.2 (`Chakra.java:63-65`) returns `ProcedureUtils.getCDModifier(getLevel)` = `1/(0.5 + 0.02*level)`. The port (`Chakra.java:33-36`) instead hardcodes `level<=0 ? 1 : max(0.05, 20/(level+20))`. These are different functions (e.g. at level=25: 1.12.2 → `1/(0.5+0.5)=1.0`; port → `20/45≈0.444`). This modifier feeds `getModifier`/charging power, so jutsu charge-rate scaling with ninja level diverges from the original. NOTE: the port's own `ProcedureUtils.getCDModifier` (`1/(0.5+0.02*level)`) IS a faithful copy — the bug is that `Chakra.getChakraModifier` no longer calls it. **Fix: have `Chakra.getChakraModifier` return `ProcedureUtils.getCDModifier(getLevel(entity))`.**

### Ninja level / battle XP (`PlayerTracker` + `NarutomodModVariables`)
- **1.12.2**: `isNinja` = `battleXp>0`. `getNinjaLevel`=`sqrt(battleXp)`. `addBattleXp` caps at 100000. `logBattleExp` requires advancement `narutomod:ninjaachievement`, then also logs to `ItemEightGates` + `ItemJutsu`. `PlayerHook` grants bonus max-health (`battleXp*0.005`, `NINJA_HEALTH` modifier), awards XP on damage dealt/taken with a damage/armor/resistance formula (capped 50), keeps XP across dimension/clone, gates reset on `keepNinjaXp` gamerule.
- **1.20.1**: `PlayerTracker` faithful — `isNinja`, `getBattleXp`, `getNinjaLevel`, `addBattleXp` (cap 100000), `updateNinjaHealth` (`NINJA_HEALTH` modifier, `battleXp*0.005`), damage-XP award. **PARTIAL: the damage-dealt XP formula is simplified** — 1.12.2 used `sqrt(maxHealth * modifiedAttackDamage * sqrt(armor+1)) * resistance`; the port uses `sqrt(max(maxHealth,1)) * min(amount/maxHp,1) * 0.5` (drops attack-damage, armor, resistance terms). Also the port `logBattleExp` does NOT require the `ninjaachievement` advancement and does NOT forward to `ItemEightGates`/`ItemJutsu` per-jutsu XP logging (the held-jutsu +1-per-hit passive). `Deaths` sub-tracker ported faithfully (with added time-based pruning). The `keepNinjaXp` gamerule reset is handled in `NarutomodModVariables` (line ~107/208 reset to 0).

### Commands
- **1.12.2** has 3 standalone command classes, each delegating to a MCreator-style procedure:
  - `CommandAddNinjaXp` → `/addninjaxp <target> <int>` (perm 4) → `ProcedureAddNinjaXpCommandExecuted`: validates target has `ninjaachievement`, `PlayerTracker.addBattleXp`.
  - `CommandAddXP2Jutsu` → `/addxp2jutsu <int>` (perm: open) → `ProcedureAddXP2JutsuCommandExecuted`: adds XP to held biju cloak (`EntityBijuManager.addCloakXp`) / EightGates / current `ItemJutsu` (`ItemJutsu.addBattleXP`).
  - `CommandLocateEntity` → `/locateEntity biju | jinchuriki [list|revoke|assign <player>] [tails]` (open) → `ProcedureLocateEntityCommandExecuted`.
- **1.20.1**: all three are **consolidated into a single Brigadier debug command** `/narutoport` (`command/PortingDebugCommands.java`, perm level 2). Equivalents:
  - `addninjaxp` → `/narutoport vars set_battle_xp <value>` and `/narutoport vars add_battle_xp <value>` (self only — no `<target>` arg).
  - `addxp2jutsu` → no direct port; jutsu XP is granted through debug helpers (`equip_*` paths call `JutsuItem.setJutsuXp(stack, def, requiredXp)`) and the `JutsuLearning` flow. There is **no `/addxp2jutsu`-equivalent that adds N XP to the currently-held jutsu/cloak/gates**.
  - `locateEntity biju/jinchuriki` → `/narutoport vars jinchuriki list|assign|revoke ...` plus `BijuManager` queries. Biju **location/coords reporting** is not obviously reproduced (the original located nearest biju entity).

---

## Jutsu 1 — Body Replacement (Replacement Clone / Kawarimi)

| field | value |
|---|---|
| display name / key | `replacementclone` (`entity.replacementclone.name`); scroll title `Body Replacement` |
| index / id | 0 (entity registry id 133) |
| rank / required XP | D → 100 (×2=200 if no affinity) |
| chakra cost | 30 (constructor `JutsuEnum(0,"replacementclone",'D',30d,...)`) |
| cooldown | internal 100 ticks between auto-substitutions (`COOLDOWN`); toggle is instant |
| entities spawned | `EntityReplacementClone` (`EntityClone.Base`) |
| procedures / callbacks | `EntityReplacementClone.Jutsu` (toggle activation via `JUTSULASTUSEKEY`); `Jutsu.Hook.onAttacked` (`LivingAttackEvent`) auto-substitutes |
| particles | 300× `EXPLOSION_NORMAL` on clone death (setDead) |
| sounds | `narutomod:poof` on clone death |
| visual | toggle jutsu (no projectile). On hit: player teleports away (5 blocks, repositioned behind attacker), a clone is left, which after 40 ticks dies into a smoke/explosion poof and (mob-griefing on) drops/falls a LOG block where it stood |
| 1.12.2 file:line | `item/ItemNinjutsu.java:59,141-237` |

**1.20.1 status: DONE.** Ported as `REPLACEMENT` (`NinjutsuItem.java:42`). Toggle via `toggleReplacement` (304) using `REPLACEMENT_LAST_USE_TAG`. Auto-substitution in `ForgeEvents.onLivingAttack` (704-741): cooldown `REPLACEMENT_COOLDOWN_TICKS=100`, consumes 30 chakra, spawns `ReplacementCloneEntity.spawnFrom`, cancels damage, +1 XP. Scroll `BODY_REPLACEMENT`. Verify `ReplacementCloneEntity` reproduces the poof/log-drop visuals (see entity backlog).

---

## Jutsu 2 — Shadow Clone (Kage Bunshin)

| field | value |
|---|---|
| display name / key | `kage_bunshin` (`entity.kage_bunshin.name`) |
| index / id | 1 |
| rank / required XP | B → 200 (×2=400) |
| chakra cost | 0 set in JutsuEnum, but **runtime cost = current chakra / (clones+1)** — splits chakra+health among clones; requires `>=200` chakra to add |
| cooldown | none (default -1 until learned) |
| entities spawned | `EntityKageBunshin.EC` (`EntityClone.Base`) |
| procedures / callbacks | `EntityKageBunshin.EC.Jutsu` (`createJutsu`): sneak+use removes all clones (or kills self if a clone); otherwise `updateClones(add)` splits maxhealth via `MAXHEALTH` modifier `1/(n+1)-1` and chakra |
| particles | `EXPLOSION_NORMAL` burst on clone death (~line 139) |
| sounds | `narutomod:kagebunshin` on create; clones replay owner's held activated jutsus on first tick |
| visual | spawns a full copy of the player (NinjaMob-style clone render), shares HP/chakra; clones can themselves hold/activate the player's other active jutsus (e.g. Rasengan) |
| 1.12.2 file:line | `item/ItemNinjutsu.java:60`; `entity/EntityKageBunshin.java:63,204-294` |

**1.20.1 status: DONE.** `KAGE_BUNSHIN` (`NinjutsuItem.java:43`). `useKageBunshin` (321): sneak → `KageBunshinEntity.removeAllFor`; else `KageBunshinEntity.spawnFrom`, +1 XP. Clone jutsu replay handled via `activateCloneHeldJutsus` (210) — re-spawns clone's Rasengan etc. Verify chakra/health splitting and `narutomod:kagebunshin` sound in `KageBunshinEntity` (entity backlog).

---

## Jutsu 3 — Rasengan

| field | value |
|---|---|
| display name / key | `rasengan` (`entity.rasengan.name`) |
| index / id | 2 |
| rank / required XP | A → 250 (×2=500) |
| chakra cost | 150 × power (charge-scaled); max power capped at **3.0** for Ninjutsu rasengan |
| cooldown | 40 ticks (`cooldownTicks=40` in port profile) |
| entities spawned | `EntityRasengan.EC` (`EntityScalableProjectile.Base`) |
| procedures / callbacks | `EntityRasengan.EC.Jutsu.createJutsu` (240): requires `ItemNinjutsu` & power≥0.5 (or Senjutsu & power≥3.0); stores `RasenganSize`; spawns `EC(entity,power,stack)` |
| particles | `Particles.Types.WHIRLPOOL` on impact (227); generic blue charging smoke while charging |
| sounds | `narutomod:rasengan_start` on spawn; `narutomod:rasengan_during` looping while active; `ENTITY_GENERIC_EXPLODE` on impact |
| visual | spinning blue sphere held in right hand (custom `RenderRasengan`, model `ModelRasengan`, texture `longcube_white.png`); rendered 10× rotated layers with additive blend; 1st/3rd-person arm-attached transforms; grows with power |
| 1.12.2 file:line | `item/ItemNinjutsu.java:61`; `entity/EntityRasengan.java:83,240-264,295-369` |

**1.20.1 status: DONE (mechanics) / verify visuals.** `RASENGAN` (`NinjutsuItem.java:44`). Full charge model via `onUseTick`/`releaseUsing` (143-207): BOW charging with smoke (`SMOKE_COLORED 0x106AD1FF`) + `SOUND_CHARGING_CHAKRA`; `RasenganProfile("Rasengan",150,base0,min0.5,max3.0,powerup200,cd40,senjutsu=false)`. Spawns `RasenganEntity.configureAttached`, `SOUND_RASENGAN_START`, cooldown 40, logs battle XP. Clone variant via `spawnCloneRasengan`. Learnable via scroll `RASENGAN` + `RASENGAN_LEARNED_TAG`. Confirm `RasenganEntity` reproduces spinning-sphere model/whirlpool/rasengan_during loop (entity backlog).

---

## Jutsu 4 — Limbo Clone (Rinbo: Hengoku)

| field | value |
|---|---|
| display name / key | `limbo_clone` (`entity.limbo_clone.name`) |
| index / id | 3 |
| rank / required XP | S → 400 (×2=800) |
| chakra cost | `EntityLimboClone.CHAKRA_USAGE` = **500** |
| cooldown | 1800 ticks (90s) — set via `ItemJutsu.setCurrentJutsuCooldown(...,1800)` |
| entities spawned | **2×** `EntityLimboClone.EC` (`EntityClone._Base`) |
| procedures / callbacks | `EntityLimboClone.EC.Jutsu.createJutsu` (172): aborts if clones already exist; spawns 2 clones, stores ids in `LimboCloneEntityIds`, sets 1800 cd |
| particles | none on spawn (clones are invisible to most) |
| sounds | `narutomod:rinbo_hengoku` on cast |
| visual | invisible limbo clones — only visible to Rinnegan/Tenseigan viewers (`canBeDetectedBy`), rendered with transparent-model blend profile; act independently |
| 1.12.2 file:line | `item/ItemNinjutsu.java:62`; `entity/EntityLimboClone.java:48,73,172-207,209-231` |

**1.20.1 status: DONE.** `LIMBO_CLONE` (`NinjutsuItem.java:45`). `useLimboClone` (341): aborts if active, consumes 500 chakra, `LimboCloneEntity.spawnPairFrom`, sets cooldown `LimboCloneEntity.COOLDOWN_TICKS`, +1 XP. Refunds chakra on spawn failure. Attack interception via `LimboCloneEntity.interceptAttack` (event 715). No scroll entry (consistent with original; command/creative only). Verify `narutomod:rinbo_hengoku` sound + Rinnegan-only visibility in `LimboCloneEntity` (entity backlog).

---

## Jutsu 5 — Sealing Chains (Adamantine Sealing Chains)

| field | value |
|---|---|
| display name / key | `sealing_chains` (`entity.sealing_chains.name`) |
| index / id | 4 |
| rank / required XP | A → 250 (×2=500) |
| chakra cost | 50 |
| cooldown | none default |
| entities spawned | `EntitySealingChains.EC` (`EntityBeamBase.Base`) |
| procedures / callbacks | `EntitySealingChains.EC.Jutsu.createJutsu` (191): sneak → retract nearby chains; else ray-trace 50 blocks, spawn chain to hit `EntityLivingBase` |
| particles | none |
| sounds | a sound is played in EC (`playSound` ~line 143 — verify event name) |
| visual | golden beam/chain that physically links caster (mid-body) to target; custom `EntityBeamBase.Renderer` with `ModelChainLink`, texture `chainlink_gold.png`, additive blend, oriented caster→target |
| 1.12.2 file:line | `item/ItemNinjutsu.java:63`; `entity/EntitySealingChains.java:58,191-207,210-250` |

**1.20.1 status: DONE.** `SEALING_CHAIN` (`NinjutsuItem.java:46`). `useSealingChains` (367): sneak → `SealingChainsEntity.retractOwnedNear(player,4)`; else `findTarget`, consume 50 chakra, `spawnFrom`, +1 XP, refund on failure. Verify gold chain-link beam model/texture and the EC sound (entity backlog).

---

## Jutsu 6 — Puppet Technique (Kugutsu)

| field | value |
|---|---|
| display name / key | `tooltip.ninjutsu.puppetjutsu` |
| index / id | 5 |
| rank / required XP | C → 150 (×2=300) |
| chakra cost | 0.25 (per-tick control upkeep) |
| cooldown | none |
| entities spawned | none directly — binds an existing `EntityPuppet.Base` the player is looking at (≤4 blocks) |
| procedures / callbacks | `ItemNinjutsu.PuppetJutsu.createJutsu` (239): returns true if `objectEntityLookingAt(4d)` is a `EntityPuppet.Base` |
| particles | none |
| sounds | none |
| visual | no jutsu visual of its own — enables remote control of a puppet entity (puppet has its own model/render) |
| 1.12.2 file:line | `item/ItemNinjutsu.java:64,239-247` |

**1.20.1 status: DONE (control hook) / depends on puppet entity.** `PUPPET` (`NinjutsuItem.java:47`). `usePuppet` (400): sneak → `AbstractPuppetEntity.releaseOwnedNear(player,32)`; else `findLookedAtPuppet` (≤4 blocks), consume 0.25 chakra, `puppet.bindTo(player)`, +1 XP. No scroll entry (consistent — never had one). Full puppet behavior/model is in the puppet entity backlog.

---

## Jutsu 7 — Bug Swarm / Kikaichu Sphere (Bugball)

| field | value |
|---|---|
| display name / key | `bugball` (`entity.bugball.name`); scroll title `Bug Swarm`, scroll item key `scroll_kikaichu_sphere` |
| index / id | 6 |
| rank / required XP | C → 150 (×2=300) |
| chakra cost | 100 × power (charge-scaled, powerupDelay 100) |
| cooldown | none |
| entities spawned | `EntityKikaichu.EC` (the bug-ball) which manages many `EntityKikaichu.EntityCustom` insect particles |
| procedures / callbacks | `EntityKikaichu.EC.Jutsu.createJutsu` (177): ray-trace 30 blocks (with `true` flag); if hit is an existing `EC` → refresh its life; if a living target → spawn `EC(entity,target,power)` |
| particles | bug-swarm rendered via `ECRender` (custom `Render<EC>`); `EXPLOSION_NORMAL` on EntityCustom death (306) |
| sounds | a swarm sound played in EntityCustom (~line 104 — verify event name; port uses `SOUND_BUGS`) |
| visual | charged-up sphere/cloud of insects that homes on a target; custom render of the ball + individual bug entities |
| 1.12.2 file:line | `item/ItemNinjutsu.java:65`; `entity/EntityKikaichu.java:65,78,177-194,196-236` |

**1.20.1 status: DONE.** `BUG_SWARM` (`NinjutsuItem.java:48`). Charged via `beginBugSwarmCharge`/`releaseBugSwarm` (428-478): sneak → `BugSwarmEntity.returnOwnedNear`; charge power (delay 100, min 0.1, max = chakra/100); look-at existing swarm → `triggerReturn`; else `findTarget`, consume `100*power`, `BugSwarmEntity.spawnFrom(player,target,power)`, plays `SOUND_BUGS`, +1 XP + battle XP. Scroll `BUG_SWARM`. Verify swarm render + bug entities (entity backlog).

---

## Jutsu 8 — Hiding in Camouflage / Hiding With Camouflage (Invisibility)

| field | value |
|---|---|
| display name / key | `tooltip.ninjutsu.hidingincamouflage` |
| index / id | 7 |
| rank / required XP | A → 250 (×2=500) |
| chakra cost | 20 per second (per 20-tick upkeep) |
| cooldown | none |
| entities spawned | none |
| procedures / callbacks | `ItemNinjutsu.HidingWithCamouflage` (249): toggle `HidingWithCamouflageActive`; `RangedItem.onUpdate` (128) consumes 20 chakra/20t and applies `INVISIBILITY` potion (21t, hidden), else `deactivate`. `HidingWithCamouflage.Hook.onSetAttackTarget` clears mob targets that are invisible unless mob wears Sharingan/Byakugan |
| particles | none |
| sounds | none |
| visual | full invisibility potion effect (vanilla invisible render); mobs can't target the invisible player without dojutsu |
| 1.12.2 file:line | `item/ItemNinjutsu.java:66,127-138,249-280` |

**1.20.1 status: DONE.** `INVISIBILITY` (`NinjutsuItem.java:49`). `toggleHidingInCamouflage` (513). Upkeep in `inventoryTick` (128-140): applies `INVISIBILITY` (21t, 20 chakra/20t), deactivates when chakra runs out. Mob de-target in `ForgeEvents.onLivingTick` (688-696), exemption for Sharingan/Byakugan head (`SusanooPowerIncreaseHandler.isSharinganHead`/`ByakuganHandler.isByakuganHead`). Scroll `HIDING_IN_CAMOUFLAGE`. Faithful.

---

## Jutsu 9 — Transformation (Henge no Jutsu)

| field | value |
|---|---|
| display name / key | `transformation_jutsu` (`entity.transformation_jutsu.name`) |
| index / id | 8 |
| rank / required XP | D → 100 (×2=200) |
| chakra cost | 50 (upkeep entity carries `chakraUsage*0.1`) |
| cooldown | none |
| entities spawned | `EntityTransformationJutsu.EC` (an Entity implementing `PlayerInput.Hook.IHandler`) |
| procedures / callbacks | `EntityTransformationJutsu.EC.Jutsu.createJutsu` (162): if active EC exists → kill it (cancel transform); else ray-trace 30 blocks, transform into the looked-at `EntityLivingBase` |
| particles | smoke poof on transform (see EC) |
| sounds | poof on transform (see EC) |
| visual | player's render is swapped to look like the target entity (`PlayerInput`-driven); toggle to revert |
| 1.12.2 file:line | `item/ItemNinjutsu.java:67`; `entity/EntityTransformationJutsu.java:52,162-181` |

**1.20.1 status: DONE.** `TRANSFORM` (`NinjutsuItem.java:50`). `useTransformation` (480): sneak or already-active → `TransformationJutsuEntity.stopFor`; else `findTarget` (30 blocks), consume 50 chakra, `spawnFrom(player,target)`, +1 XP, refund on failure. Attack interception via `TransformationJutsuEntity.interceptAttack` (event 711). Scroll `TRANSFORMATION`. Verify transform render swap + poof (entity backlog).

---

## Framework / system items status summary

| item | 1.20.1 status | notes |
|---|---|---|
| `ItemJutsu.Base` framework | DONE → `JutsuItem` | NBT keys, cooldown/enable/XP maps, affinity, owner binding, charging math all preserved. Generic charging particles/sound moved per-item. Damage-source constants (`NINJUTSU_DAMAGE`/`SENJUTSU_DAMAGE`) not in base. |
| `JutsuEnum` | DONE → `JutsuDefinition` record | identical rank→XP table & affinity rule |
| Learning (`enableJutsu` NBT) | DONE + enhanced → `JutsuLearning` + `JutsuScrollDefinition` (35 scrolls) | new scroll-based learn flow; Limbo/Sealing/Puppet have no scroll (consistent w/ original) |
| `Chakra` | PARTIAL | faithful pathway/regen/effects; **`getChakraModifier` curve hardcoded differently from 1.12.2 `ProcedureUtils.getCDModifier` — verify equivalence** |
| `PlayerTracker` / battle XP | PARTIAL | `isNinja`, level, ninja-health, deaths ported; **damage-dealt XP formula simplified** (drops attack-damage/armor/resistance terms); `logBattleExp` no longer requires `ninjaachievement` advancement and no longer forwards held-jutsu/EightGates passive +1-per-hit XP |
| `NarutomodModVariables` | DONE (for this domain) | `BATTLEXP`, `CHAKRA_PATHWAY_SYSTEM` via capability; `isNinja`/`getNinjaLevel`/`set/getBattleExperience` present; keepNinjaXp reset present |
| `CommandAddNinjaXp` (`/addninjaxp`) | PARTIAL | → `/narutoport vars set_battle_xp` / `add_battle_xp`; **self-only, no `<target>` arg, perm 2 not 4** |
| `CommandAddXP2Jutsu` (`/addxp2jutsu`) | MISSING | no equivalent that adds N XP to held jutsu/cloak/gates; only debug `setJutsuXp(=requiredXp)` helpers exist via `/narutoport equip_*` |
| `CommandLocateEntity` (`/locateEntity`) | PARTIAL | jinchuriki list/assign/revoke → `/narutoport vars jinchuriki ...`; **biju entity location/coords reporting not reproduced** |

## Top risks / things to restore
1. `/addxp2jutsu` has no functional port — players/admins cannot grant XP to a held jutsu, biju cloak, or Eight Gates incrementally. (MISSING)
2. `PlayerTracker.logBattleExp` dropped the per-hit passive XP forwarding to the held `ItemJutsu`/`ItemEightGates` (1.12.2 `ItemJutsu.logBattleXP`/`addBattleXP` path) and the `ninjaachievement` advancement gate; the damage-dealt XP formula is simplified.
3. CONFIRMED: `Chakra.getChakraModifier` uses a different curve (`max(0.05, 20/(level+20))`) than 1.12.2 (`1/(0.5+0.02*level)`); the faithful `ProcedureUtils.getCDModifier` exists but is no longer called by `getChakraModifier`. Jutsu charge-speed scaling diverges with ninja level. Fix is a one-liner.
4. `/addninjaxp` lost its `<target>` argument (admins can only modify themselves) and permission level dropped from 4 to 2.
5. `/locateEntity biju` (locate nearest tailed-beast entity / coordinates) appears unported; only jinchuriki assignment management survives.
