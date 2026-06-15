#!/usr/bin/env python3
from __future__ import annotations

import csv
import json
import re
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
LEGACY_ROOT = ROOT / "1.12.2"
JAVA_ROOT = LEGACY_ROOT / "src" / "main" / "java"
RESOURCE_ROOT = LEGACY_ROOT / "src" / "main" / "resources"
ASSET_ROOT = RESOURCE_ROOT / "assets" / "narutomod"
AUDIT_ROOT = ROOT / "audit"
REGISTRY_NAME_LITERAL = re.compile(r"(?<![A-Za-z0-9_])(?:[A-Za-z0-9_]+\.)?setRegistryName\s*\(\s*\"([^\"]+)\"\s*\)")


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace").replace("\r\n", "\n").replace("\r", "\n")


def write_csv(path: Path, rows: list[dict[str, object]], columns: list[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=columns, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def line_number(text: str, index: int) -> int:
    return text.count("\n", 0, index) + 1


def strip_comments_preserve_lines(text: str) -> str:
    def keep_newlines(match: re.Match[str]) -> str:
        return "\n" * match.group(0).count("\n")

    text = re.sub(r"/\*.*?\*/", keep_newlines, text, flags=re.S)
    return re.sub(r"//.*", "", text)


def java_files() -> list[Path]:
    return sorted(JAVA_ROOT.rglob("*.java"))


def relative_java(path: Path) -> str:
    return path.relative_to(JAVA_ROOT).as_posix()


def registry_scope(rel: str) -> str:
    parts = rel.split("/")
    if len(parts) >= 3 and parts[0] == "net" and parts[1] == "narutomod":
        return parts[2]
    return "root"


def extract_registry_rows(files: list[Path]) -> list[dict[str, object]]:
    rows: list[dict[str, object]] = []

    for path in files:
        raw = read_text(path)
        text = strip_comments_preserve_lines(raw)
        rel = relative_java(path)
        scope = registry_scope(rel)

        if scope in {"block", "item", "potion"}:
            registry = {"block": "block_or_block_item", "item": "item", "potion": "mob_effect"}[scope]
            for match in REGISTRY_NAME_LITERAL.finditer(text):
                rows.append({
                    "registry": registry,
                    "name": match.group(1),
                    "legacy_file": rel,
                    "line": line_number(text, match.start()),
                    "details": "setRegistryName literal",
                })

        for match in re.finditer(r"EntityEntryBuilder\.create\(\)(.*?\.build\s*\(\s*\))", text, flags=re.S):
            block = match.group(1)
            id_match = re.search(
                r"\.id\s*\(\s*new\s+ResourceLocation\s*\(\s*\"narutomod\"\s*,\s*\"([^\"]+)\"\s*\)\s*,\s*([^)]+?)\s*\)",
                block,
                flags=re.S,
            )
            name_match = re.search(r"\.name\s*\(\s*\"([^\"]+)\"\s*\)", block)
            tracker_match = re.search(r"\.tracker\s*\(([^)]*)\)", block)
            egg_match = re.search(r"\.egg\s*\(([^)]*)\)", block)
            if id_match:
                rows.append({
                    "registry": "entity_type",
                    "name": id_match.group(1),
                    "legacy_file": rel,
                    "line": line_number(text, match.start()),
                    "details": json.dumps({
                        "legacy_numeric_id": " ".join(id_match.group(2).split()),
                        "legacy_name": name_match.group(1) if name_match else None,
                        "tracker": " ".join(tracker_match.group(1).split()) if tracker_match else None,
                        "egg": " ".join(egg_match.group(1).split()) if egg_match else None,
                    }, ensure_ascii=False, separators=(",", ":")),
                })

    sounds_path = ASSET_ROOT / "sounds.json"
    if sounds_path.exists():
        sounds = json.loads(read_text(sounds_path))
        for key in sorted(sounds):
            rows.append({
                "registry": "sound_event",
                "name": key,
                "legacy_file": sounds_path.relative_to(LEGACY_ROOT).as_posix(),
                "line": "",
                "details": "sounds.json key",
            })

    unique: dict[tuple[str, str, str, object], dict[str, object]] = {}
    for row in rows:
        unique[(str(row["registry"]), str(row["name"]), str(row["legacy_file"]), row["line"])] = row
    return sorted(unique.values(), key=lambda row: (str(row["registry"]), str(row["name"]), str(row["legacy_file"]), str(row["line"])))


def extract_sound_case_rows(files: list[Path]) -> list[dict[str, object]]:
    sounds_path = ASSET_ROOT / "sounds.json"
    if not sounds_path.exists():
        return []

    sounds = json.loads(read_text(sounds_path))
    all_java = "\n".join(read_text(path) for path in files)
    rows: list[dict[str, object]] = []
    for key in sorted(sounds):
        lowered = key.lower()
        if key == lowered:
            continue
        rows.append({
            "original_key": key,
            "lowercase_key": lowered,
            "java_literal_occurrences": all_java.count(f'"{key}"') + all_java.count(f"'{key}'"),
            "sounds_json": sounds_path.relative_to(LEGACY_ROOT).as_posix(),
        })
    return rows


def extract_model_item_mismatch_rows(registry_rows: list[dict[str, object]]) -> list[dict[str, object]]:
    item_names = {str(row["name"]) for row in registry_rows if row["registry"] in {"item", "block_or_block_item"}}
    model_root = ASSET_ROOT / "models" / "item"
    model_names = {path.stem for path in model_root.glob("*.json")} if model_root.exists() else set()

    rows: list[dict[str, object]] = []
    for name in sorted(item_names - model_names):
        rows.append({"issue": "registry_without_item_model", "name": name, "details": ""})
    for name in sorted(model_names - item_names):
        rows.append({"issue": "item_model_without_registry_literal", "name": name, "details": ""})
    return rows


def extract_network_rows(files: list[Path]) -> list[dict[str, object]]:
    rows: list[dict[str, object]] = []
    pattern = re.compile(
        r"elements\.addNetworkMessage\s*\(\s*([^,]+?)\s*,\s*([^,]+?)\s*,\s*Side\.(CLIENT|SERVER)\s*\)",
        flags=re.S,
    )
    for path in files:
        raw = read_text(path)
        text = strip_comments_preserve_lines(raw)
        rel = relative_java(path)
        for match in pattern.finditer(text):
            rows.append({
                "legacy_order": len(rows),
                "side": match.group(3),
                "handler_class": " ".join(match.group(1).split()),
                "message_class": " ".join(match.group(2).split()),
                "legacy_file": rel,
                "line": line_number(text, match.start()),
            })
    return rows


def extract_damage_source_rows(files: list[Path]) -> list[dict[str, object]]:
    rows: list[dict[str, object]] = []
    source_patterns: list[tuple[str, re.Pattern[str]]] = [
        ("new_damage_source", re.compile(r"new\s+DamageSource\s*\(([^)]*)\)")),
        ("new_indirect_damage_source", re.compile(r"new\s+EntityDamageSourceIndirect\s*\(([^)]*)\)")),
        ("vanilla_factory", re.compile(r"DamageSource\.cause[A-Za-z0-9_]+\s*\(([^)]*)\)")),
        ("jutsu_factory", re.compile(r"ItemJutsu\.cause(?:Jutsu|Senjutsu)Damage\s*\(([^)]*)\)")),
        ("vanilla_constant", re.compile(r"DamageSource\.([A-Z][A-Z0-9_]+)")),
    ]
    modifier_pattern = re.compile(r"\.(setDamageBypassesArmor|setDamageIsAbsolute|setFireDamage|setMagicDamage|setProjectile|setExplosion)\s*\(")

    for path in files:
        text = strip_comments_preserve_lines(read_text(path))
        rel = relative_java(path)
        for kind, regex in source_patterns:
            for match in regex.finditer(text):
                line_end = text.find("\n", match.start())
                if line_end == -1:
                    line_end = min(len(text), match.end() + 240)
                scan_end = min(len(text), line_end + 320)
                expression = " ".join(text[match.start():scan_end].split())
                modifiers = sorted(set(modifier_pattern.findall(text[match.end():scan_end])))
                source_name = ""
                if kind == "vanilla_constant":
                    source_name = match.group(1)
                elif kind in {"new_damage_source", "new_indirect_damage_source"}:
                    source_name = " ".join(match.group(1).split())
                elif kind == "vanilla_factory":
                    source_name = text[match.start(): text.find("(", match.start())]
                elif kind == "jutsu_factory":
                    source_name = text[match.start(): text.find("(", match.start())]

                rows.append({
                    "kind": kind,
                    "source_name": source_name,
                    "modifiers": "|".join(modifiers),
                    "legacy_file": rel,
                    "line": line_number(text, match.start()),
                    "expression": expression,
                })

    return sorted(rows, key=lambda row: (str(row["legacy_file"]), int(row["line"]), str(row["kind"])))


def extract_reference_rows(files: list[Path], pattern: str, label: str, strip_comments: bool = False) -> list[dict[str, object]]:
    rows: list[dict[str, object]] = []
    regex = re.compile(pattern)
    for path in files:
        text = read_text(path)
        if strip_comments:
            text = strip_comments_preserve_lines(text)
        rel = relative_java(path)
        matches = list(regex.finditer(text))
        if not matches:
            continue
        rows.append({
            "kind": label,
            "legacy_file": rel,
            "occurrences": len(matches),
            "first_line": line_number(text, matches[0].start()),
        })
    return rows


def main() -> None:
    files = java_files()
    registry_rows = extract_registry_rows(files)
    sound_case_rows = extract_sound_case_rows(files)
    model_mismatch_rows = extract_model_item_mismatch_rows(registry_rows)
    network_rows = extract_network_rows(files)
    damage_rows = extract_damage_source_rows(files)
    client_rows = extract_reference_rows(files, r"\bMinecraft\.getMinecraft\s*\(", "minecraft_get_minecraft")
    entity_data_rows = extract_reference_rows(files, r"\bgetEntityData\s*\(", "get_entity_data")

    write_csv(
        AUDIT_ROOT / "legacy_registry_names.csv",
        registry_rows,
        ["registry", "name", "legacy_file", "line", "details"],
    )
    write_csv(
        AUDIT_ROOT / "sound_key_case_issues.csv",
        sound_case_rows,
        ["original_key", "lowercase_key", "java_literal_occurrences", "sounds_json"],
    )
    write_csv(
        AUDIT_ROOT / "model_item_name_mismatches.csv",
        model_mismatch_rows,
        ["issue", "name", "details"],
    )
    write_csv(
        AUDIT_ROOT / "network_messages.csv",
        network_rows,
        ["legacy_order", "side", "handler_class", "message_class", "legacy_file", "line"],
    )
    write_csv(
        AUDIT_ROOT / "damage_source_usage.csv",
        damage_rows,
        ["kind", "source_name", "modifiers", "legacy_file", "line", "expression"],
    )
    write_csv(
        AUDIT_ROOT / "client_class_references.csv",
        client_rows,
        ["kind", "legacy_file", "occurrences", "first_line"],
    )
    write_csv(
        AUDIT_ROOT / "entity_persistent_data_refs.csv",
        entity_data_rows,
        ["kind", "legacy_file", "occurrences", "first_line"],
    )

    summary = {
        "java_files_scanned": len(files),
        "registry_rows": len(registry_rows),
        "registry_counts": dict(sorted(Counter(str(row["registry"]) for row in registry_rows).items())),
        "uppercase_sound_keys": len(sound_case_rows),
        "model_item_mismatches": len(model_mismatch_rows),
        "network_messages": len(network_rows),
        "damage_source_rows": len(damage_rows),
        "minecraft_get_minecraft_files": len(client_rows),
        "minecraft_get_minecraft_occurrences": sum(int(row["occurrences"]) for row in client_rows),
        "get_entity_data_files": len(entity_data_rows),
        "get_entity_data_occurrences": sum(int(row["occurrences"]) for row in entity_data_rows),
        "outputs": [
            "audit/legacy_registry_names.csv",
            "audit/sound_key_case_issues.csv",
            "audit/model_item_name_mismatches.csv",
            "audit/network_messages.csv",
            "audit/damage_source_usage.csv",
            "audit/client_class_references.csv",
            "audit/entity_persistent_data_refs.csv",
        ],
    }
    (AUDIT_ROOT / "m0_audit_summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
