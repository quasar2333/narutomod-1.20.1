# Jutsu Backlog — Inton (Yin) + Shikotsumyaku (Bone) + Kekkei Mora + Black Receiver

Audit date: 2026-06-15. Compares 1.12.2 source-of-truth against the 1.20.1 Forge port.

- ORIGINAL base: `D:\mcmodding\naruto\1.12.2\src\main\java\net\narutomod`
- PORT base: `D:\mcmodding\naruto\1.20.1\src\main\java\net\narutomod`

## Framework / Notes

**1.12.2 architecture.** Each domain is a "tome" `Item*` class extending `ElementsNarutomodMod.ModElement`, whose item is `ItemJutsu.Base` (a `RangedItem`). Jutsu are declared as `ItemJutsu.JutsuEnum(index, translationKey, rank, [requiredXp], chakraUsage, IJutsuCallback)`. `IJutsuCallback.createJutsu(stack, entity, power)` runs the effect. Cooldown comes from `defaultCooldownMap[i]` (often forced to 0) plus per-call `ItemJutsu.setCurrentJutsuCooldown(...)`. Rank is a single char (`'S'`,`'B'`,`'C'`) that drives the XP-to-unlock curve. Damage uses `ItemJutsu.causeJutsuDamage` / vanilla damage sources.

**1.20.1 architecture (port).** Two host items cover this whole domain:
- `AdvancedNatureJutsuItem` (`item/AdvancedNatureJutsuItem.java`) hosts BOTH `SHIKOTSUMYAKU` and `KEKKEI_MORA` (plus many other natures) via the `AdvancedNatureKind` enum. Each kind holds a `JutsuDefinition[]`. The dispatcher `releaseUsing(...)` branches on `isShikotsumyaku*` / `isKekkeiMora*` predicates (translationKey + index). EightyGods uses `onUseTick` (charge-loop every 4 ticks), matching the 1.12.2 `onUsingTick`.
- `SpecialProjectileWeaponItem` (`item/SpecialProjectileWeaponItem.java`, `WeaponKind.BLACK_RECEIVER` / `ASH_BONES`) hosts Black Receiver and Ash Bones throwing.
- Inton lives in its own `IntonItem` (`item/IntonItem.java`).
- `BoneArmorItem` (`item/BoneArmorItem.java`) is the chest piece toggled by Larch/Willow.

**`JutsuDefinition` record** (`item/JutsuItem.java:343`): `requiredXpForRank` maps `'S'->400, 'A'->250, 'B'->200, 'C'->150, 'D'->100`. `JutsuDefinition.ranked(...)` derives requiredXp from rank; `definition(idx,key,rank,reqXp,chakra)` lets the port hard-set requiredXp (the SHIKOTSUMYAKU/MOKUTON/SHAKUTON/etc. entries pass explicit requiredXp like 150/400). `.withPower(basePower, powerUpDelay)` enables charge scaling (Bracken & FingerBone use it). `defaultEnabled=true` is passed for AdvancedNatureJutsuItem so jutsu start usable; `defaultCooldownMap` filled with `0L` (no cooldown) — matches 1.12.2 where these tomes set `defaultCooldownMap[i]=0`.

**Registry wiring (verified present).**
- `registry/ModItems.java`: `SHIKOTSUMYAKU` (l.310), `KEKKEI_MORA` (l.134), `BLACK_RECEIVER` (l.88), `ASHBONES` (l.72), `BONE_ARMORBODY` (l.93), `BONE_DRILL` (l.95), `BONE_SWORD` (l.97).
- `registry/ModEntityTypes.java`: `ENTITY80GODS`, `ENTITYBRACKENDANCE`, `FINGER_BONE`, `SPIKE`, `TRUTHSEEKERBALL`, `ENTITYBULLETASHBONES`, `ENTITYBULLETBLACK_RECEIVER`, plus Inton's `MIND_TRANSFER*`, `SHADOW_IMITATION`.
- Renderers in `client/ClientModEvents.java` + `client/renderer/*`: `EightyGodsRenderer` (+`EightyGodsModel`/`armfist.png`), `BrackenDanceRenderer` (`spike_bone.png` via `LegacySpikeModelRenderer`), `FingerBoneRenderer` (+`FingerBoneModel`), `TruthSeekerBallRenderer`, `SpikeRenderer`, `ShadowImitationRenderer`, `MindTransferSelfRenderer`, `BlackReceiverProjectileRenderer`, `AshBonesProjectileRenderer`.
- Particles: `NarutoParticleKind.MOB_APPEARANCE` exists (`particle/NarutoParticleKind.java:12`, `registry/ModParticleTypes.java`).

**Overall assessment.** This domain is far more complete than a stub. Every listed 1.12.2 jutsu has a real 1.20.1 runtime path. The remaining risks are faithfulness deltas (a few mechanics drift or were intentionally cut), NOT missing techniques.

---

# INTON (Yin) — `ItemInton.java` (1.12.2) → `IntonItem.java` (1.20.1)

Original tome: `1.12.2/.../item/ItemInton.java`. Item registry `narutomod:inton`. Three jutsu. Note: the port lives in standalone `IntonItem` (not AdvancedNatureJutsuItem) and is heavily implemented.

## Genjutsu

| Field | Value |
|---|---|
| Index | 0 |
| translationKey | `genjutsu` |
| Rank / reqXp | `'B'` → 200 XP |
| Chakra | 300 |
| Cooldown | 1200 ticks (60s), set on success |
| Range | 30 blocks (look-at target) |
| Entities | none |
| Procedures | `ProcedureUtils.objectEntityLookingAt`; `ProcedureSync.MobAppearanceParticle.send` (1.12.2) |
| Particles | MobAppearance particle sent to target player (mob-appearance illusion overlay) |
| Sounds | `narutomod:genjutsu` (played at target) |
| Effects | `PotionParalysis` dur=200 amp=1; `MobEffects.NAUSEA` dur=240 amp=0; `MobEffects.BLINDNESS` dur=200 amp=0 |
| Visual | No entity; target sees mob-appearance particle illusion + screen blindness/nausea |
| 1.12.2 file:line | `ItemInton.java:37,70-94` |

1.20.1 status: **DONE**. `IntonItem.activateGenjutsu` (`IntonItem.java:66-100`) replicates: range 30, paralysis(`ModEffects.PARALYSIS`) 200/amp1, `MobEffects.CONFUSION` 240/amp0, BLINDNESS 200/amp0, sound `ModSounds.SOUND_GENJUTSU`, cooldown 1200, and sends `NarutoParticleKind.MOB_APPEARANCE` to the target via `ServerLevel.sendParticles`. Adds XP. Faithful.

## Mind Transfer (Shintenshin no Jutsu / 心転身の術)

| Field | Value |
|---|---|
| Index | 1 |
| translationKey | `mind_transfer` |
| Rank / reqXp | `'C'` → 150 XP |
| Chakra | 300 (+ continuous burn `chakraUsage*0.005`/tick) |
| Cooldown | none default |
| Range | 30 blocks |
| Entities | `EntityMindTransfer.EC` (controller, `mind_transfer` id 288) + `EntityMindTransfer.EntityDuplicate` (`mind_transfer_self` id 289, an `EntityClone.Base`) |
| Procedures | `ProcedureUtils.objectEntityLookingAt`; `ProcedureOnLivingUpdate.setNoClip`; `PlayerInput.Hook` (input hijack); `PlayerRender.setSkinCloneTarget` |
| Sounds | `narutomod:mindtransfer` |
| Effects on end | user gets WEAKNESS 600/32, NAUSEA 600/1, SLOWNESS 600/4; HP swap with target/clone |
| Visual | leaves an `EntityDuplicate` clone at origin; user body flies to target over 60 ticks (invisible), then possesses target (spectator/skin-clone); re-cast cancels |
| 1.12.2 file:line | `ItemInton.java:38`; `EntityMindTransfer.java` (whole) |

1.20.1 status: **DONE (PARTIAL on input-hijack fidelity — verify)**. `IntonItem.activateMindTransfer` (`IntonItem.java:102-125`) toggles via `MindTransferEntity.hasActiveFor/stopFor/spawnFrom`, passes burn `chakraUsage*0.005`. Ported entities `MindTransferEntity`/`MindTransferSelfEntity` + `MindTransferSelfRenderer` exist. RISK: the 1.12.2 possession relies on `PlayerInput.Hook` input forwarding, spectator camera, skin-clone render, and dimension-change cancellation; confirm the port reproduces movement/mouse hijack and the clone HP-swap on release. Mechanically wired; deep behavior not line-verified here.

## Shadow Imitation (Kagemane no Jutsu / 影真似の術)

| Field | Value |
|---|---|
| Index | 2 |
| translationKey | `shadow_imitation` |
| Rank / reqXp | `'B'` → 200 XP |
| Chakra | 50 (+ continuous burn = chakraUsage + target punch-damage, per second) |
| Cooldown | none default |
| Range | 30 blocks; multi-target (int-array of EC ids) |
| Entities | `EntityShadowImitation.EC` (`shadow_imitation` id 293) |
| Procedures | `ProcedureUtils.objectEntityLookingAt`; `PlayerInput.Hook.haltTargetInput/copyInputFrom`; LOS via `rayTraceBlocks` |
| Sounds | `narutomod:shadow_sfx` |
| Visual | custom GL render draws a black shadow tendril (`textures/black.png`) crawling along ground/walls from user to each target; target mirrors user's movement; dropped on LOS break / chakra-out / re-press / `JutsuKey2Pressed` |
| 1.12.2 file:line | `ItemInton.java:39`; `EntityShadowImitation.java` (whole) |

1.20.1 status: **DONE (PARTIAL on shadow-trail render — verify)**. `IntonItem.activateShadowImitation` (`IntonItem.java:127-151`): shift-click releases via `ShadowImitationEntity.stopOwnedNear(128)`, else `spawnFrom(player, target, chakraUsage)`. `ShadowImitationEntity` + `ShadowImitationRenderer` exist. RISK: confirm the renderer reproduces the bespoke ground/wall-hugging black quad strip (1.12.2 `RenderCustom.doRender` builds a Tessellator strip between user/target along solid blocks) and that input-mirroring + chakra-burn-with-punch-damage are intact.

---

# SHIKOTSUMYAKU (Dead Bone Pulse / 屍骨脈) — `ItemShikotsumyaku.java` → `AdvancedNatureJutsuItem` (KIND `SHIKOTSUMYAKU`)

Original tome: `1.12.2/.../item/ItemShikotsumyaku.java`, registry `narutomod:shikotsumyaku`, type SHIKOTSUMYAKU. Six jutsu, all rank `'S'`, no cooldown by default. Port enum: `AdvancedNatureJutsuItem.java:1913-1924`. Dispatch predicates: `AdvancedNatureJutsuItem.java:1504-1538`.

## Dance of the Larch (Tsubaki no Mai / 柳髏の舞 — "dancelarch")

| Field | Value |
|---|---|
| Index | 0 |
| translationKey | `tooltip.shikotsumyaku.dancelarch` |
| Rank / reqXp | `'S'` / 150 (explicit) |
| Chakra | 100 |
| Cooldown | 0 |
| Entities | none |
| Item effect | equips/creates `ItemBoneArmor.body` (`narutomod:bone_armorbody`) chest, toggles `LarchActive` NBT |
| Passive (while worn) | RESISTANCE amp3, and (LarchActive) reflects 70% of incoming melee as THORNS damage; armor self-destructs if Shikotsumyaku not in inventory |
| Sounds | `narutomod:bonecrack` |
| Visual | `ModelBoneArmor` (giant bone exoskeleton biped model, `textures/bonearmor.png`) |
| 1.12.2 file:line | `ItemShikotsumyaku.java:43,101-126`; armor `ItemBoneArmor.java` |

1.20.1 status: **DONE**. `toggleLarchDance` (`AdvancedNatureJutsuItem.java:951-969`) toggles via `BoneArmorItem.toggleLarch/setLarchActive`, plays bonecrack, adds XP. `BoneArmorItem` (`BoneArmorItem.java`) ports the passive: RESISTANCE 3 if Larch active else 2, self-shrink if no Shikotsumyaku, and `ForgeEvents.onLivingAttack` reflects 70% THORNS (l.189-205). Bone-armor model via `BoneArmorClientExtensions`. Faithful.

## Dance of the Willow (Yanagi no Mai / 釣指の舞 — "dancewillow")

| Field | Value |
|---|---|
| Index | 1 |
| translationKey | `tooltip.shikotsumyaku.dancewillow` |
| Rank / reqXp | `'S'` / 150 |
| Chakra | 100 |
| Item effect | equips bone armor, toggles `WillowActive` |
| Passive | STRENGTH amplifier +5 (stacked onto existing) while worn |
| Sounds | `narutomod:bonecrack` |
| Visual | same `ModelBoneArmor` chest |
| 1.12.2 file:line | `ItemShikotsumyaku.java:44,128-153` |

1.20.1 status: **DONE**. `toggleWillowDance` (`AdvancedNatureJutsuItem.java:971-989`); `BoneArmorItem.inventoryTick` adds DAMAGE_BOOST +5 when WillowActive (l.106-112). Faithful.

## Dance of the Camellia (Tsubaki no Mai / 椿の舞 — "dancecamellia")

| Field | Value |
|---|---|
| Index | 2 |
| translationKey | `tooltip.shikotsumyaku.dancecamellia` |
| Rank / reqXp | `'S'` / 150 |
| Chakra | 100 |
| Item effect | equips `ItemBoneSword.block` (`narutomod:bone_sword`) into mainhand |
| Sounds | `narutomod:bonecrack` |
| Visual | bone sword item model (BoneSword: ItemSword, +4/8 atk material, acts as shield) |
| 1.12.2 file:line | `ItemShikotsumyaku.java:45,155-172`; `ItemBoneSword.java` |

1.20.1 status: **DONE**. `activateCamelliaDance` (`AdvancedNatureJutsuItem.java:1000-1016`) equips `ModItems.BONE_SWORD` (a `BasicMeleeWeaponItem` WeaponKind.BONE_SWORD), plays bonecrack. Verify the bone sword's shield/parry behavior is carried in `BasicMeleeWeaponItem`. Faithful at jutsu level.

## Finger Bullet (Teshi Sendan / 指穿弾 — "finger_bone")

| Field | Value |
|---|---|
| Index | 3 |
| translationKey | `finger_bone` |
| Rank / reqXp | `'S'` / 150 |
| Chakra | 10 |
| Entities | `EntityFingerBone.EC` (`finger_bone` id 319; `EntityScalableProjectile.Base`, scale 0.4, no gravity, damage 8, dies at 100 ticks) |
| Procedures | spawn helper in `EntityFingerBone.EC.Jutsu.createJutsu` |
| Sounds | `narutomod:bonecrack` (fire), `narutomod:bullet_impact` (hit), BLOCK_DUST(BONE_BLOCK) particles on block hit |
| Visual | spinning bone bullet model `ModelFingerBone` (`textures/fingerbone.png`), rotates 30°/tick |
| 1.12.2 file:line | `ItemShikotsumyaku.java:46`; `EntityFingerBone.java` (whole) |

1.20.1 status: **DONE**. `activateFingerBone` (`AdvancedNatureJutsuItem.java:1206-1217`) → `FingerBoneEntity.spawnFrom(player)`. Entity `FingerBoneEntity.java` (DAMAGE 8.0), renderer `FingerBoneRenderer` + `FingerBoneModel` + `fingerbone.png`. Definition `.withPower(1.0F,1.0F)` (`:1922`). Faithful.

## Dance of the Clematis: Flower (Karamatsu no Mai: Hana / 鉄線蓮の舞・花 — "danceclementisflower")

| Field | Value |
|---|---|
| Index | 4 |
| translationKey | `tooltip.shikotsumyaku.danceclementisflower` |
| Rank / reqXp | `'S'` / 400 (explicit) |
| Chakra | 500 |
| Cooldown | 1200 ticks (60s) on success |
| Item effect | grants/equips `ItemBoneDrill.block` (`narutomod:bone_drill`, +79 atk, big drill) |
| Sounds | `narutomod:bonecrack` |
| Visual | bone drill item; (commented-out 1.12.2 code had a drill-arm armor model — NOT active) |
| 1.12.2 file:line | `ItemShikotsumyaku.java:47,174-216`; `ItemBoneDrill.java` |

1.20.1 status: **DONE**. `activateClematisFlower` (`AdvancedNatureJutsuItem.java:1018-1035`): if no bone drill in inventory, consume chakra, `setCurrentJutsuCooldown 1200`, give `ModItems.BONE_DRILL` (a `BasicMeleeWeaponItem` WeaponKind.BONE_DRILL), bonecrack. Faithful (matches the active 1.12.2 branch; the commented armor-drill variant is not in either codebase).

## Dance of the Seedling Fern (Sawarabi no Mai / 蕨手の舞 — "entitybrackendance")

| Field | Value |
|---|---|
| Index | 5 |
| translationKey | `entitybrackendance` |
| Rank / reqXp | `'S'` / 400 (explicit) |
| Chakra | 100 (× power; power charge 0.5..150) |
| Cooldown | 0 |
| Entities | `EntityBrackenDance` (`entitybrackendance` id 318; `EntitySpike.Base`, grow 8 ticks to scale 2.0, color 0xFFFFFFFF, damage=power) — spawns `power*power*5` spikes |
| Procedures | raycast 30 blocks, only if hitting BLOCK with sideHit UP; scatters spikes on solid ground |
| Sounds | `narutomod:bonecrack` (per spike, volume 5) |
| Visual | forest of white bone spikes erupting from ground, texture `narutomod:textures/spike_bone.png` rendered by `EntitySpike.Renderer` |
| 1.12.2 file:line | `ItemShikotsumyaku.java:48,57-79,218-272` |

1.20.1 status: **DONE**. `activateBrackenDance` (`AdvancedNatureJutsuItem.java:1219-1238`): requires `BrackenDanceEntity.hasUpwardGroundTarget`, charge power, `BrackenDanceEntity.spawnFrom(player, power)`. Entity `BrackenDanceEntity` + `BrackenDanceRenderer extends LegacySpikeModelRenderer` with `textures/spike_bone.png`. Definition `.withPower(0.5F,150.0F)` (`:1924`) matches 1.12.2 `getPower(...,0.5f,150f)`. Faithful.

---

# KEKKEI MORA (Kekkei Mōra / 血継網羅) — `ItemKekkeiMora.java` → `AdvancedNatureJutsuItem` (KIND `KEKKEI_MORA`)

Original tome: `1.12.2/.../item/ItemKekkeiMora.java`, registry `narutomod:kekkei_mora`, type KEKKEIMORA. Four jutsu, all rank `'S'`, chakra 10, no cooldown by default. Port enum `AdvancedNatureJutsuItem.java:1875-1884`. Dispatch predicates `:1540-1562`.

## Eighty Gods Vacuum Attack (Hachimon Tonkō... actually Banbutsu Sōzō-class punch — "entity80gods")

| Field | Value |
|---|---|
| Index | 0 |
| translationKey | `entity80gods` |
| Rank / reqXp | `'S'` / 400 |
| Chakra | 10 (per shot; held: `onUsingTick` fires every 4 ticks while ticksExisted%4==1) |
| Cooldown | 0 |
| Entities | `Entity80Gods` (`entity80gods` id 368; `EntityScalableProjectile.Base`, OGSize 0.5×0.25, scale grows +1/tick, dies >10 ticks, explosionStrength 6.0, immune to explosions) |
| Damage | 500 to first living hit; then `newExplosion` strength 6 (mob-griefing per config) |
| Procedures | `Entity80Gods.Jutsu.createJutsu` — shoot at look vec, speed 1.25, inaccuracy 0.1, random yaw/pitch spread |
| Sounds | `narutomod:throwpunch` |
| Visual | `Render80Gods` + `ModelArmFist` (4 nested arm/fist boxes, `textures/armfist.png`), additive blend, full-bright; renders a giant ghostly fist hurtling forward |
| 1.12.2 file:line | `ItemKekkeiMora.java:48,95-104,145-261,263-289` |

1.20.1 status: **DONE**. Charge loop in `onUseTick` every 4 ticks (`AdvancedNatureJutsuItem.java:133-136`), `activateEightyGods` (`:1055-1070`) → `EightyGodsEntity.spawnFrom` (sound `SOUND_THROWPUNCH`). `EightyGodsEntity`: INITIAL_SCALE 2, DAMAGE 500, EXPLOSION_STRENGTH 6, SPEED_FACTOR 1.25, scale+1/tick, MAX_LIFE gate, spawn offset with ±90°/±60° spread (`:80-97`). Renderer `EightyGodsRenderer` + `EightyGodsModel` + `armfist.png`. Faithful.

## Yomotsu Hirasaka (黄泉比良坂 — portal pair — "jutsu2")

| Field | Value |
|---|---|
| Index | 1 |
| translationKey | `tooltip.byakurinnesharingan.jutsu2` |
| Rank / reqXp | `'S'` / 400 |
| Chakra | 10 |
| Cooldown | 0 |
| Entities | none — places `BlockPortalBlock` pairs |
| Procedures | `ProcedureYomotsuHirasaka.executeProcedure` — entry portal 2 blocks ahead facing user; exit at look-target (150 blocks) or target entity location; 2-tall each, `TileEntityCustom.setPair` links them |
| Sounds | `block.portal.travel` |
| Visual | two linked portal blocks (BlockPortalBlock) |
| 1.12.2 file:line | `ItemKekkeiMora.java:49,107-113`; `ProcedureYomotsuHirasaka.java` (whole) |

1.20.1 status: **DONE**. `activateYomotsuHirasaka` (`AdvancedNatureJutsuItem.java:1090-1106`) + `placeYomotsuHirasakaPortals` (`:1108-1190`): resolves entry (2 ahead, opposite facing) and exit (150-block raytrace target or entity), places 2-tall `ModBlocks.PORTALBLOCK` columns, binds `PortalBlockEntity.setPair` both ways, plays `SoundEvents.PORTAL_TRAVEL`, refunds chakra on failure. Faithful re-implementation of the procedure (block/TE now `PortalBlock`/`PortalBlockEntity`).

## Expansive Truth-Seeking Ball (Gudōdama — "expansivetsb")

| Field | Value |
|---|---|
| Index | 2 |
| translationKey | `tooltip.kekkeimora.expansivetsb` |
| Rank / reqXp | `'S'` / 400 |
| Chakra | 10 |
| Cooldown | 1200 ticks (60s) |
| Entities | `EntityTruthSeekerBall.EntityCustom(entity, 9, stack)` with `setMaxScale(25f)` — a giant expanding black sphere |
| Visual | TSB sphere render (`EntityTruthSeekerBall.RenderCustom`), expands to scale 25 |
| 1.12.2 file:line | `ItemKekkeiMora.java:50,115-124`; `EntityTruthSeekerBall.java` |

1.20.1 status: **DONE**. `activateExpansiveTruthSeekingBall` (`AdvancedNatureJutsuItem.java:1072-1088`): guard `TruthSeekerBallEntity.hasActiveExpansive`, then `spawnExpansiveFrom(player)`, XP, cooldown 1200. `TruthSeekerBallEntity.configureExpansive` (`:220`) + `TruthSeekerBallRenderer`. Verify the expansive max-scale equals 25 and damage matches; entity & renderer present. Faithful at jutsu level.

## Ash Bones (灰骨 — "ashbones")

| Field | Value |
|---|---|
| Index | 3 |
| translationKey | `item.ashbones.name` |
| Rank / reqXp | `'S'` / 400 |
| Chakra | 10 |
| Cooldown | 0 |
| Item effect | equips `ItemAshBones.block` (`narutomod:ashbones`) into mainhand (a thrown/ranged bone item that, on hit/throw, applies a death/kill effect) |
| Sounds | `narutomod:bonecrack` (vol 0.5) |
| Visual | ash-bones item model + its own arrow entity `entitybulletashbones` |
| 1.12.2 file:line | `ItemKekkeiMora.java:51,126-143`; `ItemAshBones.java` |

1.20.1 status: **DONE**. `activateKekkeiMoraAshBones` (`AdvancedNatureJutsuItem.java:1037-1053`) equips `ModItems.ASHBONES`, bonecrack vol 0.5. The ash-bones item itself is `SpecialProjectileWeaponItem(ASH_BONES)`: throws `ENTITYBULLETASHBONES`, `applyAshBonesEffect` runs `ProcedureUtils.setDeathAnimations(target,1,200)` (death-animation kill), 80-tick cooldown, self-destructs unless wearing Byakurinnesharingan/Rinnegan/Tenseigan helmet. Renderer `AshBonesProjectileRenderer`. Faithful.

---

# BLACK RECEIVER (Kokushōkaku / 黒い受信機) — `ItemBlackReceiver.java` → `SpecialProjectileWeaponItem` (WeaponKind.BLACK_RECEIVER)

Original: `1.12.2/.../item/ItemBlackReceiver.java`, registry `narutomod:black_receiver`. This is NOT a JutsuEnum tome — it is a single bow-like thrown weapon item with passive effects. Port: `SpecialProjectileWeaponItem.java` + entity `ThrownSpecialWeaponEntity` (`entitybulletblack_receiver`).

## Black Receiver (thrown spike weapon)

| Field | Value |
|---|---|
| Index | n/a (standalone item) |
| Entity | `EntityArrowCustom` (`entitybulletblack_receiver` id 137; an `EntityArrow`) → port `ThrownSpecialWeaponEntity` (BLACK_RECEIVER variant) |
| Use | held like a bow (`getMaxItemUseDuration` 72000, `EnumAction.BOW`); on release shoots arrow power×2, crit, damage 10, knockback 0, item damaged 1, 40-tick cooldown |
| Melee | mainhand: +10 ATTACK_DAMAGE, -2.4 ATTACK_SPEED; `hitEntity` applies `onHitEntity` |
| onHitEntity | `PotionHeaviness` 600 ticks, amplifier = 1 + existing heaviness amp |
| Arrow onUpdate | every tick, nearby non-shooter living entities (AABB grow 0.75) get NAUSEA 200/amp1 |
| Passive in inventory | if NOT holding Rinnegan/Tenseigan helmet: wearer gets NAUSEA 100/amp1; non-player living that carries it gets `setNoAI(true)` |
| Cannot be dropped | `onDroppedByPlayer` returns false |
| Sounds | `narutomod:hand_shoot` (shoot) |
| Visual | `RenderCustom` renders the item itself as the projectile (`ItemCameraTransforms.TransformType.GROUND`), oriented to flight |
| 1.12.2 file:line | `ItemBlackReceiver.java` (whole) |

1.20.1 status: **DONE**. `SpecialProjectileWeaponItem` (BLACK_RECEIVER): bow-use, release shoots `ENTITYBULLETBLACK_RECEIVER` speed 2.0, sound `SOUND_HAND_SHOOT`, item `hurtAndBreak(1)`, 40-tick cooldown (`SpecialProjectileWeaponItem.java:65-99`). Melee modifiers +10/-2.4 (`:50-61`). `hurtEnemy` → HEAVINESS 600 amp = 1+existing (`:101-114`). `inventoryTick`/`tickBlackReceiver` (`:122,152-161`): if no Rinnegan/Tenseigan helmet → CONFUSION(=NAUSEA) 100/amp1; Mob → `setNoAi(true)`. `onDroppedByPlayer` false (`:130`). Entity `ThrownSpecialWeaponEntity`: direct hit damage 10 + HEAVINESS, near-miss AoE applies CONFUSION 200/amp1 (`ThrownSpecialWeaponEntity.java:105-152`), item-as-projectile renderer `BlackReceiverProjectileRenderer`. Faithful.

RISK (minor): 1.12.2 shot was `setIsCritical(true)`, `setSilent(true)`, knockback 0, pickupStatus DISALLOWED — confirm the port's `ThrownSpecialWeaponEntity.configure` sets crit/no-knockback/no-pickup; near-miss AoE radius 0.75 matches.

---

## Cross-cutting / faithfulness risks to re-verify

1. **Mind Transfer & Shadow Imitation deep behavior** — input-hijack (`PlayerInput.Hook`), spectator camera, skin-clone render, HP swap, dimension-change cancellation, and Shadow Imitation's bespoke ground/wall-hugging black quad strip. Entities + renderers are present; line-level behavior not exhaustively verified in this audit.
2. **Bone tools' secondary behavior** — `BoneSword` shield/parry and `BoneDrill` mining/attack are now `BasicMeleeWeaponItem` kinds; verify those carry the 1.12.2 special behavior (sword `isShield`, drill +79 atk).
3. **Expansive TSB scale/damage** — confirm `configureExpansive` uses maxScale 25 and the 9-segment TSB config; renderer present.
4. **Black Receiver projectile flags** — crit/silent/no-knockback/no-pickup on the thrown entity.
5. **No MISSING techniques** were found in this domain; all 13 distinct techniques (3 Inton + 6 Shikotsumyaku + 4 Kekkei Mora) plus the standalone Black Receiver item have working 1.20.1 runtimes.
