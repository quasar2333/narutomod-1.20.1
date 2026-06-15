#!/usr/bin/env python3
from __future__ import annotations

import csv
import json
import re
import shutil
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
LEGACY_ASSETS = ROOT / "1.12.2" / "src" / "main" / "resources" / "assets" / "narutomod"
TARGET_ASSETS = ROOT / "1.20.1" / "src" / "main" / "resources" / "assets" / "narutomod"
TARGET_JAVA = ROOT / "1.20.1" / "src" / "main" / "java" / "net" / "narutomod" / "registry" / "ModSounds.java"
AUDIT_ROOT = ROOT / "audit"
VALID_RESOURCE_PATH = re.compile(r"^[a-z0-9_./-]+$")


def normalize_resource_path(path: str) -> str:
    path = path.strip().lower().replace("\\", "/")
    path = re.sub(r"[^a-z0-9_./-]+", "_", path)
    path = re.sub(r"_+", "_", path)
    return "/".join(part.strip("._-") or "_" for part in path.split("/"))


def sound_name_to_path(name: str) -> str:
    return name.split(":", 1)[1] if ":" in name else name


def java_identifier(name: str) -> str:
    identifier = re.sub(r"[^A-Za-z0-9_]", "_", name).upper()
    identifier = re.sub(r"_+", "_", identifier).strip("_")
    return f"SOUND_{identifier}"


def copy_sound_file(old_path: str, new_path: str) -> None:
    source = LEGACY_ASSETS / "sounds" / f"{old_path}.ogg"
    target = TARGET_ASSETS / "sounds" / f"{new_path}.ogg"
    if not source.exists():
        raise FileNotFoundError(f"Missing legacy sound file for {old_path}: {source}")

    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, target)


def write_csv(path: Path, rows: list[dict[str, object]], columns: list[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=columns, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def write_mod_sounds(sound_keys: list[str]) -> None:
    lines = [
        "package net.narutomod.registry;",
        "",
        "import net.minecraft.sounds.SoundEvent;",
        "import net.minecraftforge.registries.RegistryObject;",
        "import net.narutomod.NarutomodMod;",
        "",
        "public final class ModSounds {",
    ]
    for key in sound_keys:
        lines.append(f"    public static final RegistryObject<SoundEvent> {java_identifier(key)} = ModRegistries.SOUND_EVENTS.register(\"{key}\",")
        lines.append(f"            () -> SoundEvent.createVariableRangeEvent(NarutomodMod.location(\"{key}\")));")
    lines.extend([
        "",
        "    private ModSounds() {",
        "    }",
        "",
        "    public static void touch() {",
        "    }",
        "}",
        "",
    ])
    TARGET_JAVA.write_text("\n".join(lines), encoding="utf-8")


def main() -> None:
    source_json = LEGACY_ASSETS / "sounds.json"
    sounds = json.loads(source_json.read_text(encoding="utf-8"))
    converted: dict[str, object] = {}
    issues: list[dict[str, object]] = []

    target_sounds_dir = (TARGET_ASSETS / "sounds").resolve()
    if target_sounds_dir.exists():
        if ROOT.resolve() not in target_sounds_dir.parents:
            raise RuntimeError(f"Refusing to clear sound directory outside workspace: {target_sounds_dir}")
        shutil.rmtree(target_sounds_dir)

    for original_key, definition in sounds.items():
        new_key = original_key.lower()
        if new_key in converted:
            raise ValueError(f"Sound key collision after lowercase conversion: {original_key} -> {new_key}")

        new_definition = dict(definition)
        new_sounds = []
        for entry in definition.get("sounds", []):
            if isinstance(entry, str):
                old_name = entry
                old_path = sound_name_to_path(old_name)
                new_path = normalize_resource_path(old_path)
                new_entry = f"narutomod:{new_path}" if ":" in old_name else new_path
            else:
                new_entry = dict(entry)
                old_name = str(entry["name"])
                old_path = sound_name_to_path(old_name)
                new_path = normalize_resource_path(old_path)
                new_entry["name"] = f"narutomod:{new_path}" if ":" in old_name else new_path

            if original_key != new_key or old_path != new_path or not VALID_RESOURCE_PATH.fullmatch(old_path):
                issues.append({
                    "original_key": original_key,
                    "new_key": new_key,
                    "original_path": old_path,
                    "new_path": new_path,
                    "reason": ";".join(reason for reason in [
                        "uppercase_key" if original_key != new_key else "",
                        "invalid_path_chars" if old_path != new_path or not VALID_RESOURCE_PATH.fullmatch(old_path) else "",
                    ] if reason),
                })

            copy_sound_file(old_path, new_path)
            new_sounds.append(new_entry)

        new_definition["sounds"] = new_sounds
        converted[new_key] = new_definition

    TARGET_ASSETS.mkdir(parents=True, exist_ok=True)
    (TARGET_ASSETS / "sounds.json").write_text(json.dumps(converted, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    write_mod_sounds(list(converted.keys()))
    write_csv(AUDIT_ROOT / "sound_conversion_issues.csv", issues, ["original_key", "new_key", "original_path", "new_path", "reason"])

    print(json.dumps({
        "sound_events": len(converted),
        "converted_entries": len(issues),
        "output_sounds_json": str((TARGET_ASSETS / "sounds.json").relative_to(ROOT)),
        "output_java": str(TARGET_JAVA.relative_to(ROOT)),
        "audit": "audit/sound_conversion_issues.csv",
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
