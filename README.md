# Naruto Mod Forge 1.20.1 Port

Work-in-progress Forge 1.20.1 port of NarutoMod `0.2.10-beta`.

Original mod metadata:

- Mod ID: `narutomod`
- Original Minecraft version: `1.12.2`
- Port target: Minecraft Forge `1.20.1`
- Original author: `ahznb`
- Original URL: https://www.youtube.com/c/AHZNB

## Repository Layout

- `1.12.2/` - original 1.12.2 source reference used for migration, tracked as a submodule from `AHZNB/naruto_mod`.
- `1.20.1/` - Forge 1.20.1 port workspace.
- `audit/` - migration audit reports and jutsu backlog notes.
- `docs/` - porting recipes and supporting documentation.
- `tools/` - migration validation and dedicated-server safety tools.
- `PORTING_PLAN_1.20.1.md` - migration plan.
- `MIGRATION_PROGRESS.md` - ongoing migration log.

## Build

Use the Forge 1.20.1 workspace:

```powershell
cd 1.20.1
.\gradlew.bat "-Dnet.minecraftforge.gradle.check.certs=false" --no-daemon build
```

The built mod jar is generated under `1.20.1/build/libs/`.

If you need the original 1.12.2 reference source after cloning, initialize the submodule:

```powershell
git submodule update --init --recursive
```

## Validation

The migration workflow validates resources and dedicated-server safety from the repository root:

```powershell
python tools\validate_port_resources.py
python tools\validate_dedicated_server_safety.py
```

When runtime or packaged behavior is affected, also run the dedicated server gate:

```powershell
powershell -ExecutionPolicy Bypass -File tools\run_dedicated_server_gate.ps1
```

## License And Distribution

This source is published under the original mod's stated distribution terms:

> My mod is open source because I want to help people understand what I'm doing, and maybe get support. But I own all the rights to my source code and the releases built from my source code. Forks or modifications are completely allowed, but monetization of any part of my mod/fork or any releases built on it without my permission is NOT allowed.

See `LICENSE` for the repository-level license notice.
