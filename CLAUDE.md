# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repository is

This is **not a single mod project** — it is the working tree for an in-progress **manual port** of the Naruto mod (`narutomod` 0.2.10-beta) from **Minecraft 1.12.2 / Forge** to **Minecraft 1.20.1 / Forge 47.x**. MCreator was abandoned; the generated Java is being hand-ported file by file.

Layout at the repo root (`D:\mcmodding\naruto`):

- `1.12.2/` — the **original** MCreator-generated mod. **Read-only reference.** It is the source of truth for behavior and for the exact magic constants (blend modes, alpha, damage multipliers, tick counts, motion factors). **Never edit it.**
- `1.20.1/` — the **actual port** you work in. A standard ForgeGradle 6 / Java 17 MDK project (`net.narutomod.*`).
- `PORTING_PLAN_1.20.1.md` — the master plan and acceptance criteria (Chinese). Read **§14「新会话接手卡」(handoff card)** first when picking up work.
- `MIGRATION_PROGRESS.md` — append-only progress log; its **tail is the real source of truth for what is done**. ~14k lines.
- `tools/` — one-off Python conversion scripts + the validation/gate scripts run on every change.
- `audit/` — CSV/JSON snapshots produced by the audit scripts (GL-coupling file list, registry names, persistent-data keys, resource validation, etc.). Referenced throughout the plan.
- `docs/SINGLE_JUTSU_PORTING_RECIPE_RASENGAN.md` — the reusable recipe for porting one jutsu as a vertical slice.

Most planning docs are in Chinese; match that when updating them.

## Working rhythm: the "slice" workflow

Work proceeds in small **slices** (one jutsu behavior, one entity, one explosion gate, etc.), not bulk file translation. The established loop for every slice:

1. **Read the 1.12.2 original first** and extract the exact constants — do not invent or "tune" values. The plan repeatedly stresses 直接照抄数值 (copy the numbers verbatim).
2. Implement in `1.20.1/`, keeping behavior identical and isolating any client-only code (see below).
3. Run the **full verification gate** (below).
4. **Append a detailed slice entry to `MIGRATION_PROGRESS.md`**: what the old code did (with old call signatures), what the new code does, and the verification commands run with their results. This log format is consistent throughout the file — follow it.

Do **not** restart from milestone M0. The project is mid-M6 (entity layer); render infra (M3) and the Rasengan vertical slice (M4) are done.

## Build & verification commands

The Gradle wrapper lives in `1.20.1/`; the Python/PowerShell tools run from the repo root. All Gradle invocations pass the cert-check-disable flag and `--no-daemon`.

From `1.20.1/`:
```bash
./gradlew.bat -Dnet.minecraftforge.gradle.check.certs=false --no-daemon compileJava   # fast inner loop
./gradlew.bat -Dnet.minecraftforge.gradle.check.certs=false --no-daemon build          # full jar (run when touching runtime/release)
./gradlew.bat -Dnet.minecraftforge.gradle.check.certs=false --no-daemon runData        # datagen -> src/generated/resources
```

From the repo root (`D:\mcmodding\naruto`):
```bash
python tools/validate_port_resources.py            # resource/atlas/blockstate/model/lang/sound validation -> audit/*
python tools/validate_dedicated_server_safety.py   # scans non-client Java for client refs (must report 0 issues)
powershell -ExecutionPolicy Bypass -File tools/run_dedicated_server_gate.ps1   # boots a dedicated server, asserts clean startup
```

A slice is "green" when `compileJava` + both validators pass; run full `build` and the dedicated-server gate when the change affects runtime or the release jar. There is no unit-test suite — verification is the validators, the server gate, and in-game visual comparison against 1.12.2 screenshots.

When the user reports "textures broken" or "crash", check first:
```bash
ls -t 1.20.1/run/crash-reports/ 2>/dev/null | head -3
grep -nE "Missing textures|Missing model|Missing blockstate|Missing Narutomod player variables|ERROR|FATAL|Exception" 1.20.1/run/logs/latest.log
```
Don't mass-rename resources to chase a purple/black texture — locate whether it's atlas/stitch, model reference path, or a blockstate/item-model variant first (see plan §14.2).

## Architecture of the 1.20.1 port (`net.narutomod.*`)

**Entry point** — `NarutomodMod` (`@Mod`): constructor wires `ModRegistries.register(bus)`, capability/attribute listeners, and `commonSetup` (which does `NetworkHandler.register()` + `ModSpawnPlacements.register()` inside `enqueueWork`). Use `NarutomodMod.location(path)` for all `ResourceLocation`s.

**Registration** — `registry/` package. Each domain has a `ModXxx` class (`ModBlocks`, `ModItems`, `ModEntityTypes`, `ModEffects`, `ModSounds`, `ModParticleTypes`, `ModMenuTypes`, `ModBlockEntities`, `ModDamageTypes`, `ModSpawnPlacements`). `ModRegistries` owns every `DeferredRegister` and follows a **touch()-then-register pattern**: `register()` calls `ModXxx.touch()` on each (forces static init so `RegisterObject` fields populate) before registering the `DeferredRegister`s to the mod bus. When you make a placeholder real, flip its `ModXxx` entry from a dummy/`PortingMarkerItem`/`PortingDummyEntity` factory to the real constructor — registry objects are the shared anchor for datagen, GUI, commands.

**Networking** — `network/` package. `NetworkHandler` registers ~20 `SimpleChannel` messages in a fixed sequence. **Registration order IS the wire protocol** — only append at the end; never insert or reorder. Each message is a class with static `encode/decode/handle`. Add a typed `sendToServer/sendToPlayer/...` helper alongside.

**Client isolation (dedicated-server safety)** — this is a hard invariant. Anything referencing `net.minecraft.client` or `Minecraft.getInstance()` must live under `net/narutomod/client/**`. Common (server-safe) classes only send packets / read data. `validate_dedicated_server_safety.py` enforces this and must report **0** issues. Client init is split across Forge events (`ClientModEvents`, `EntityRenderersEvent.*`, `RegisterParticleProvidersEvent`, `RegisterKeyMappingsEvent`, etc.) — there is no `@SidedProxy`.

**Rendering** — the heaviest subsystem (per audit, ~71/100 files had GL state coupled into model geometry in 1.12).
- `client/render/NarutoRenderTypes` — the shared `RenderType` library (the 4 state families: additive-glow, translucent-emissive double-sided, scrolling-UV, plus vanilla `lightning()` for untextured geometry). **Never scatter `RenderSystem.blendFunc` in render code** — route every state combo through a `RenderType` here.
- `client/render/SphereMesh` — replaces the deleted GLU sphere / display lists.
- `client/model/` (~80) — `LayerDefinition`-based models converted from the old `ModelBase`/`ModelBiped` classes.
- `client/renderer/` (~137) — `EntityRenderer`s and `RenderLayer`s (player layers for body-mounted items, biju cloak, susanoo flame, etc.).
- `client/particle/` — custom particle pipeline: `NarutoParticleOptions`/`NarutoParticleType` (carry an `int[]` mirroring the old packet args), `NarutoParticleProvider`, and custom `ParticleRenderType`s for the 4 self-managed atlases.

**Procedures** — `procedure/` holds the big util libraries `ProcedureUtils` (general utils, reflection replaced by Access Transformers) and `ProcedureSync` (sync primitives; its dynamic `tagName/message.tag` is intentional transport design — don't hardcode keys to zero out an old audit CSV). Per-jutsu logic mostly moved into the item/entity classes.

**Player variables** — `NarutomodModVariables` (capability-backed persistent player data) + `NarutomodSavedData` (`SavedData`) + `PlayerTracker`/`EntityTracker`. Death/respawn copy happens in `PlayerEvent.Clone`. `ForgeEvents` is registered explicitly on the Forge bus.

**Other domains**: `item/` (jutsu framework, weapons, armor — armor with custom glow/scroll RenderTypes uses `IClientItemExtensions` + AddLayers rather than the vanilla armor pipeline), `entity/`, `effect/` (`MobEffect`), `menu/` + `client/gui/` (scroll GUIs via `MenuType`), `block/` (incl. fluid `WaterStillBlock`, `PortalBlock`+`PortalBlockEntity`), `world/` (`KamuiChunkGenerator`/`KamuiDimension`, `VillagePoiHelper` for the dead Village API), `event/SpecialEvent` (persistable special events), `command/PortingDebugCommands`.

## Conventions & gotchas

- **Constants come from the 1.12.2 audit, not intuition.** When porting, cite the old behavior in the progress log.
- Resource keys / registry names must be **lowercase** (the old `sounds.json` had `Amaterasu`-style keys that were lowercased).
- `ModEntityTypes.dummyTypes()` should be empty; `PortingDummyEntity` is a scaffold, not a live entity list.
- `1.20.1/src/main/java/com/example/examplemod/` is empty leftover MDK scaffolding — ignore it.
- Mappings are **Parchment `2023.09.03-1.20.1`** layered on official Mojmap. Old MCP names (`world.isRemote`, `posX`, `motionX`) map mechanically to Mojmap (`level.isClientSide`, `getX()`, `getDeltaMovement()`); the plan §5.2 and §12 hold the translation tables.
- The repo is **not** a git repository — there is no commit/branch workflow.
