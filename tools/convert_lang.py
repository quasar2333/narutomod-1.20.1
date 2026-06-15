#!/usr/bin/env python3
from __future__ import annotations

import csv
import json
import re
from collections import OrderedDict
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
LEGACY_LANG = ROOT / "1.12.2" / "src" / "main" / "resources" / "assets" / "narutomod" / "lang"
TARGET_LANG = ROOT / "1.20.1" / "src" / "main" / "resources" / "assets" / "narutomod" / "lang"
AUDIT_ROOT = ROOT / "audit"
MODID = "narutomod"


def normalize_path(value: str) -> str:
    value = value.strip().lower()
    value = re.sub(r"[^a-z0-9_./-]+", "_", value)
    value = re.sub(r"_+", "_", value)
    return value.strip("_")


def modern_key(key: str) -> str | None:
    item_match = re.fullmatch(r"item\.([^.]+)\.name", key)
    if item_match:
        return f"item.{MODID}.{normalize_path(item_match.group(1))}"

    block_match = re.fullmatch(r"tile\.([^.]+)\.name", key)
    if block_match:
        return f"block.{MODID}.{normalize_path(block_match.group(1))}"

    entity_match = re.fullmatch(r"entity\.([^.]+)\.name", key)
    if entity_match:
        return f"entity.{MODID}.{normalize_path(entity_match.group(1))}"

    effect_match = re.fullmatch(r"(?:potion\.)?effect\.([^.]+)", key)
    if effect_match:
        return f"effect.{MODID}.{normalize_path(effect_match.group(1))}"

    fluid_match = re.fullmatch(r"fluid\.([^.]+)", key)
    if fluid_match:
        return f"fluid_type.{MODID}.{normalize_path(fluid_match.group(1))}"

    item_group_match = re.fullmatch(r"itemGroup\.(?:narutomodmodtab|narutomod)", key)
    if item_group_match:
        return "itemGroup.narutomod"

    return None


def parse_lang(path: Path) -> OrderedDict[str, str]:
    entries: OrderedDict[str, str] = OrderedDict()
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        entries[key.strip()] = value
    return entries


def read_registry_rows() -> list[dict[str, str]]:
    path = AUDIT_ROOT / "legacy_registry_names.csv"
    if not path.exists():
        return []
    with path.open("r", encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle))


def humanize_registry_name(name: str) -> str:
    name = re.sub(r"^entitybullet", "", name)
    name = re.sub(r"entity$", "", name)
    name = re.sub(r"_+", " ", name).strip()
    if not name:
        name = "entity"
    return " ".join(part.capitalize() for part in name.split())


def write_csv(path: Path, rows: list[dict[str, object]], columns: list[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=columns, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def convert_file(lang: str) -> tuple[OrderedDict[str, str], list[dict[str, object]]]:
    source = LEGACY_LANG / f"{lang}.lang"
    entries = parse_lang(source)
    output: OrderedDict[str, str] = OrderedDict()
    rows: list[dict[str, object]] = []

    for key, value in entries.items():
        new_key = modern_key(key)
        if new_key is None:
            output[key] = value
        else:
            output[new_key] = value
            rows.append({
                "language": lang,
                "original_key": key,
                "new_key": new_key,
                "value_empty": value == "",
            })

    output.setdefault("itemGroup.narutomod", "Naruto Mod" if lang == "en_us" else "火影忍者模组")
    output.setdefault("item.narutomod.porting_marker", "Porting Marker" if lang == "en_us" else "移植标记")
    return output, rows


def add_registry_fallbacks(lang: str, output: OrderedDict[str, str]) -> list[dict[str, object]]:
    rows: list[dict[str, object]] = []
    for row in read_registry_rows():
        registry = row["registry"]
        name = row["name"]
        if registry == "item":
            key = f"item.{MODID}.{name}"
        elif registry == "block_or_block_item":
            key = f"block.{MODID}.{name}"
        elif registry == "entity_type":
            key = f"entity.{MODID}.{name}"
        else:
            continue

        if key in output:
            continue

        fallback = humanize_registry_name(name)
        if lang == "zh_cn" and name == "mud":
            fallback = "泥浆"
        output[key] = fallback
        rows.append({
            "language": lang,
            "translation_key": key,
            "fallback_value": fallback,
            "registry": registry,
            "registry_name": name,
        })

    return rows


def main() -> None:
    TARGET_LANG.mkdir(parents=True, exist_ok=True)
    audit_rows: list[dict[str, object]] = []
    fallback_rows: list[dict[str, object]] = []
    summary = {}

    for lang in ("en_us", "zh_cn"):
        converted, rows = convert_file(lang)
        fallbacks = add_registry_fallbacks(lang, converted)
        converted.setdefault("entity.narutomod.porting_dummy", "Porting Dummy" if lang == "en_us" else "移植测试实体")
        (TARGET_LANG / f"{lang}.json").write_text(
            json.dumps(converted, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        audit_rows.extend(rows)
        fallback_rows.extend(fallbacks)
        summary[lang] = {
            "legacy_entries_converted": len(rows),
            "fallback_entries_added": len(fallbacks),
            "output_entries": len(converted),
        }

    write_csv(
        AUDIT_ROOT / "lang_key_migrations.csv",
        audit_rows,
        ["language", "original_key", "new_key", "value_empty"],
    )
    write_csv(
        AUDIT_ROOT / "lang_fallback_translations.csv",
        fallback_rows,
        ["language", "translation_key", "fallback_value", "registry", "registry_name"],
    )

    print(json.dumps({
        "languages": summary,
        "audit": "audit/lang_key_migrations.csv",
        "fallback_audit": "audit/lang_fallback_translations.csv",
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
