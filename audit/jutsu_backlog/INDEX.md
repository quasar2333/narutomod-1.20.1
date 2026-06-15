# Jutsu Porting Backlog — Master Index (narutomod 1.12.2 → 1.20.1)

Master index over the 16 per-domain backlog files in this directory. Each domain audited the
1.12.2 Forge source of truth against the in-progress 1.20.1 Forge port. Counts below are taken
from the per-domain audits; click through to each `backlogFile` for per-technique detail.

- ORIGINAL (truth): `D:\mcmodding\naruto\1.12.2\src\main\java\net\narutomod`
- PORT (WIP): `D:\mcmodding\naruto\1.20.1\src\main\java\net\narutomod`

---

## Domain status table

| # | Domain | Element(s) | Total | Missing | Stub | Partial | Done | One-line status |
|---|---|---|---:|---:|---:|---:|---:|---|
| 1 | [framework](framework.md) | NINJUTSU | 9 | 0 | 0 | 0 | 9 | All 9 core Ninjutsu ported; gaps are in support systems (chakra-modifier curve bug, simplified battle-XP, missing `/addxp2jutsu`). |
| 2 | [katon](katon.md) | KATON | 4 | 0 | 0 | 0 | 4 | All 4 Fire jutsu faithful; one confirmed regression — firestream damage no longer bypasses armor. |
| 3 | [suiton](suiton.md) | Suiton | 6 | 0 | 0 | 0 | 6 | All 6 Water jutsu faithful (mechanics + visuals); Poison Mist (cross-element) UNVERIFIED. |
| 4 | [doton](doton.md) | Doton | 5 | 0 | 0 | 0 | 5 | All 5 Earth jutsu faithful; only block-revert / crush-mass nuances to verify. |
| 5 | [futon](futon.md) | Futon | 5 | 0 | 0 | 0 | 5 | All 5 Wind jutsu faithful; Rasenshuriken's Rasengan-prerequisite gate not ported. |
| 6 | [raiton](raiton.md) | Lightning | 5 | 0 | 0 | 0 | 5 | DONE; Kirin flight reimplemented, Chidori spark origin server-approximated (verify feel). |
| 7 | [kekkei_wood_ice](kekkei_wood_ice.md) | Mokuton + Hyoton | 9 | 0 | 0 | 0 | 9 | DONE; verify `wood_house_2.nbt` structure asset + WoodGolem/Hyoton cosmetic parity. |
| 8 | [kekkei_storm_explosion](kekkei_storm_explosion.md) | Ranton + Bakuton | 5 | 0 | 0 | 0 | 5 | All 5 ported; only cosmetic particle substitutions + C3 AoE done inline. |
| 9 | [kekkei_scorch_boil_lava](kekkei_scorch_boil_lava.md) | Shakuton/Futton/Yooton | 10 | 0 | 0 | 0 | 10 | DONE; XP-curve + cast-SFX deltas to confirm; Yoton sealing cross-checked elsewhere. |
| 10 | [kekkei_magnet_dust](kekkei_magnet_dust.md) | Jiton/Jinton/Gourd | 7 | 0 | 1 | 2 | 4 | Mostly ported; **Gourd armor is a STUB** (bare diamond chestplate); sand material-type system dropped; Sand Shield/Levitation PARTIAL. |
| 11 | [yin_bone_mora](yin_bone_mora.md) | Inton/Bone/KekkeiMora/BlackReceiver | 13 | 0 | 0 | 3 | 10 | All 13 have runtimes; 3 PARTIAL are deep-behavior verification items (possession/shadow-trail). |
| 12 | [dojutsu_sharingan](dojutsu_sharingan.md) | dojutsu | 13 | 2 | 0 | 4 | 7 | Susanoo/Kamui/Amaterasu-DOT/Uchiha done; **Amaterasu ACTIVATION jutsu MISSING** (no key handler). |
| 13 | [dojutsu_rinnegan_byakugan_tenseigan](dojutsu_rinnegan_byakugan_tenseigan.md) | dojutsu | 24 | 3 | 0 | 2 | 19 | Most paths done; **Shinra Tensei, Ban Sho Ten'in, Hakke Kusho MISSING** (handlers never dispatch their keys). |
| 14 | [senjutsu_sixpath](senjutsu_sixpath.md) | Senjutsu | 13 | 0 | 0 | 1 | 12 | DONE; Sage Mode visuals + port-added OUTER_PATH/cost to confirm. |
| 15 | [ninjutsu_gates_medical](ninjutsu_gates_medical.md) | Ninjutsu/EightGates/Iryo | 31 | 0 | 0 | 1 | 30 | Essentially fully ported; only gap is Eight Gates Gate-3+ red player-skin tint. |
| | **GRAND TOTAL** | | **159** | **5** | **1** | **20** | **133** | |

### Grand totals
- **159** techniques audited across **16** domains.
- **133 DONE** (≈84%), **20 PARTIAL** (≈13%), **1 STUB** (≈0.6%), **5 MISSING** (≈3%).
- Every MISSING item lives in the two **dojutsu** domains (5 total); the only STUB is the **Gourd armor**.
- 9 of 16 domains are 100% DONE. The elemental-affinity tomes (Katon, Suiton, Doton, Futon, Raiton)
  and the Eight Gates / Medical / Basic Ninjutsu domain are all complete except for cosmetic/verify deltas.

---

## The 6 items that are not DONE (MISSING + STUB)

These are the only items that block player-facing functionality (everything else is faithful or a verify-level PARTIAL):

1. **Amaterasu activation jutsu** (MISSING — domain 12). No key-1 handler for Mangekyo-Sasuke/Eternal; players cannot ignite black flame, place flame blocks, or sneak-extinguish. The flame DOT effect + block exist but nothing triggers them. (+ its sneak-extinguish sweep counts as the 2nd MISSING here.)
2. **Amaterasu sneak-extinguish sweep** (MISSING — domain 12). No port of `ProcedureAmaterasuExtinguishEntities` (clear fire + flame blocks + potions in 15-block cube).
3. **Shinra Tensei** (MISSING — domain 13). Rinnegan/Tenseigan Key 1 never dispatched; `RinneganSpecialJutsuHandler` only handles `key==2`. Sound registered but unused.
4. **Ban Sho Ten'in** (MISSING — domain 13). Rinnegan/Tenseigan Key 3 never dispatched; no `ProcedurePullAndHold` port. Sound registered but unused.
5. **Hakke Kusho** (MISSING — domain 13). Byakugan Key 1 + sneak; no `ProcedureAirPunch` base class ported, no sneak branch.
6. **Gourd armor** (STUB — domain 10). `GOURDBODY` is a bare vanilla diamond chestplate: no `ModelGourd`, no textures, no `onArmorTick` passives (extinguish / Resistance II / self-repair), no crumble/regrow loop. Required as the CHEST gate for all Jiton sand jutsu.

---

## Cross-cutting infrastructure gaps (block multiple jutsu / shared base classes)

These shared pieces, if restored, unblock or de-risk several techniques at once. Listed roughly by leverage.

### A. Shared activation base classes / procedures not ported
- **`ProcedureAirPunch` (base)** — air-blast cone (range/radius/damage/block-launch curves). Blocks **Hakke Kusho** (Byakugan) directly, and is the legacy base for several sweep effects (Suiton Stream's AirPunch was inlined, but the reusable base is gone). Port once, reuse.
- **`ProcedurePullAndHold` / `ProcedureGravityPower.dislodgeBlocks`** — grab-and-hold-entity loop + terrain-chunk dislodge (`EarthBlocksEntity` already exists). Blocks **Ban Sho Ten'in**. (Kamui's grab in `ObitoKamuiHandler` is a partial analog that could be generalized.)
- **`SpecialEvent.setSphericalExplosionEvent`** — spherical terrain explosion. Needed by **Shinra Tensei** (power>20) and used by ScorchOrb (Shakuton); port a faithful helper rather than per-jutsu manual loops.
- **`ItemDojutsu.Base`** (shared dojutsu-helmet base) — never centralized in the port; the **foreign-wearer Blindness 1200t penalty appears MISSING** across Rinnegan/Byakugan/Tenseigan. Ownership/`MostRecentWornDojutsuTime` were re-implemented per-handler. Restoring a shared base fixes the penalty for all dojutsu helmets at once.

### B. Key-dispatch wiring (root cause of all 5 dojutsu MISSING items)
- The port's `SpecialJutsuKeyMessage.handle` routes only to `ObitoKamuiHandler → ByakuganHandler → RinneganSpecialJutsuHandler → BijuManager`, and `RinneganSpecialJutsuHandler` **only handles key==2**. There is **no Amaterasu branch and no Susanoo branch** in the key1/2/3 path (Susanoo was moved to the new Up-arrow power-increase key).
- Net effect: Keys 1 and 3 for Rinnegan/Tenseigan and the sneak-branch of Byakugan Key 1 are dead. Fixing the dispatch table (add Amaterasu handler on key1, Rinnegan key1/key3 branches, Byakugan key1+sneak branch) is the single highest-leverage infra fix — it unblocks 4 of the 5 MISSING jutsu. Keybind parity for Susanoo (F vs Up-arrow) is a related decision.

### C. Visual / particle infrastructure (cosmetic, but recurring)
- **Sand material-`Type` system (IRON/SAND/GOLD)** — dropped; every sand spawn forced to IRON color. Affects all Jiton visuals + makes SandLevitation's sand-texture branch dead code. One enum + NBT restores color variety across the whole Jiton set.
- **`SwarmTarget` per-particle entity engine** — replaced by server particle bursts. Affects Sand Shield / Bind / Levitation silhouettes (now NoopEntityRenderer). Cosmetic but domain-wide.
- **`NarutoParticleKind.SMOKE_COLORED` render mode** (translucent vs additive) — should be spot-checked against legacy across Scorch/Boil/Lava/Tenseigan; one render-type decision affects many jutsu.
- **Charging FX divergence** — the shared framework's charging puff is an orange `FLAME_COLORED` (port) vs the original blue `SMOKE` cloud; affects every charged jutsu via `JutsuItem`. One-line cosmetic fix in the base.
- **Snug-helmet overlay model** (`ModelHelmetSnug` highlight + forehead planes) — no confirmed analog; affects all dojutsu helmet renders.

### D. Confirmed mechanical regressions (one-line / small fixes, broad impact)
- **`Chakra.getChakraModifier` curve bug** (framework): uses `20/(level+20)` instead of `1/(0.5+0.02*level)`. The faithful `ProcedureUtils.getCDModifier` exists but is no longer called — so charge-speed scaling diverges for EVERY charged jutsu. One-line fix (have `getChakraModifier` call `getCDModifier(getLevel(...))`).
- **`katon_firestream_damage` armor-bypass** (katon): damage type lost its `bypasses_armor` tag → Fire Annihilation & Great Flame no longer pierce armor. Add the data tag.
- **`PlayerTracker.logBattleExp`** (framework): dropped per-hit passive XP forwarding to held jutsu / Eight Gates and the `ninjaachievement` gate; damage-dealt XP formula simplified. Affects XP economy for all held jutsu.
- **`/addxp2jutsu` command** (framework): no port — can't grant incremental XP to held jutsu / biju cloak / Eight Gates.

---

## RECOMMENDED PORTING ORDER

Ordering honors the user's stated priority sequence (Suiton, Katon, Doton, Futon, Raiton, Jinton/Dust,
Inton/Yin, Yang, Senjutsu, Rinnegan, Sharingan, Tenseigan, Six Paths, all kekkei genkai) while pulling
forward (a) shared infrastructure that unblocks multiple jutsu and (b) confirmed one-line bug fixes,
and preferring domains whose entities/renderers already exist in the port (which is nearly all of them —
the port is far more complete than the brief assumed, so "porting order" is mostly a **fix/verify order**).

### Batch 1 — Framework fixes + top-priority elements (Suiton, Katon, Doton)
The user's first three priorities are already 100% DONE on entities/renderers, so this batch is small, high-confidence, and mostly bug-fix + verify:
1. **Chakra modifier curve** one-line fix (framework, domain 1) — unblocks correct charge scaling for ALL jutsu.
2. **Katon firestream armor-bypass** data tag (domain 2) — restores the only confirmed Katon mechanical regression.
3. **Suiton verify** (domain 3): resolve the Poison Mist UNVERIFIED faithfulness (locate 1.12.2 source), confirm custom render types + textures resolve.
4. **Doton verify** (domain 4): Earth Wall block-revert robustness + spike_stone/MUD asset parity.
   Optionally restore `PlayerTracker` per-hit XP forwarding + `/addxp2jutsu` here since they touch the framework.

### Batch 2 — Futon, Raiton, then Jinton/Dust (priorities 4-6) + the Gourd STUB
Entities/renderers all exist; this batch closes the only STUB and the next priority bugs:
1. **Futon** (domain 5): port the Rasenshuriken→Rasengan prerequisite gate; verify rasenshuriken/wind assets.
2. **Raiton** (domain 6): verify Kirin flight trajectory + Chidori spark origin in-game (no code gaps, feel-check).
3. **Jinton/Dust + Gourd** (domain 10): **build the Gourd armor** (model/textures/`onArmorTick` passives/crumble-regrow) — closes the only STUB and is the CHEST gate for all Jiton sand jutsu; then restore the sand material-`Type` (IRON/SAND/GOLD) system; finish Sand Shield + Sand Levitation PARTIALs.

### Batch 3 — Dojutsu key-dispatch infra + the 5 MISSING jutsu (Inton/Yin, Yang already done; Rinnegan/Sharingan/Tenseigan next priorities)
This is the heaviest batch and the source of every MISSING item. Do the shared infra first, then the individual jutsu fall out cheaply:
1. **Shared bases**: port `ProcedureAirPunch`, `ProcedurePullAndHold`/`dislodgeBlocks`, `SpecialEvent.setSphericalExplosionEvent`, and centralize `ItemDojutsu.Base` (restore foreign-wearer Blindness penalty).
2. **Fix the key-dispatch table** so Rinnegan key1/key3 and Byakugan key1+sneak actually fire.
3. **Implement the 5 MISSING jutsu** on top: **Amaterasu activation + sneak-extinguish** (Sharingan/Mangekyo, domain 12), **Shinra Tensei** + **Ban Sho Ten'in** (Rinnegan/Tenseigan, domain 13), **Hakke Kusho** (Byakugan, domain 13). Decide Susanoo keybind parity (F vs Up-arrow).
   Note: **Inton/Yin and Yang (Yooton) domains are already DONE/verify-only** (domains 11, 9), so their priority slots collapse into verification passes you can run alongside this batch.

### Batch 4 — Verify-only sweeps (Senjutsu, Six Paths, remaining kekkei genkai, Eight Gates/Medical)
All DONE or single-PARTIAL; no missing functionality, only fidelity confirmation:
- Eight Gates Gate-3+ red player tint (domain 15), Sage Mode visuals (domain 14), Mokuton structure asset (domain 7), Scorch/Boil/Lava SFX + AoE deltas (domain 9), Storm/Explosion cosmetic particles (domain 8), yin/bone/mora deep-behavior verification (domain 11), and the recurring particle/render-mode spot-checks listed under cross-cutting C.

---

## Notes
- The port is substantially more complete than the original brief assumed: 133/159 techniques are DONE, and the entire elemental-affinity stack (priorities 1-5) plus Eight Gates/Medical are done bar cosmetics. Treat this backlog primarily as a **fix-and-verify** plan, not a from-scratch port.
- All 5 MISSING items share a single root cause (the dojutsu key-dispatch table + a few un-ported shared procedures), so Batch 3's infra work has outsized leverage.
