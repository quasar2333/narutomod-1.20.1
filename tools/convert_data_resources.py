#!/usr/bin/env python3
from __future__ import annotations

import csv
import json
import re
import shutil
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
LEGACY_ASSETS = ROOT / "1.12.2" / "src" / "main" / "resources" / "assets" / "narutomod"
LEGACY_JAVA = ROOT / "1.12.2" / "src" / "main" / "java" / "net" / "narutomod"
TARGET_RESOURCES = ROOT / "1.20.1" / "src" / "main" / "resources"
TARGET_ASSETS = TARGET_RESOURCES / "assets" / "narutomod"
TARGET_DATA = TARGET_RESOURCES / "data" / "narutomod"
AUDIT_ROOT = ROOT / "audit"


NO_DROP_BLOCKS = {
    "amaterasublock",
    "kamuiblock",
    "light_source",
    "meteorite",
    "mud",
    "portalblock",
    "water_still",
}


def ensure_inside_workspace(path: Path) -> Path:
    resolved = path.resolve()
    if ROOT.resolve() not in resolved.parents:
        raise RuntimeError(f"Refusing to touch path outside workspace: {resolved}")
    return resolved


def write_csv(path: Path, rows: list[dict[str, object]], columns: list[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=columns, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def namespaced(value: str) -> str:
    return value if ":" in value else f"minecraft:{value}"


def convert_loot_value(value: object) -> object:
    if isinstance(value, dict):
        out = {}
        for key, child in value.items():
            if key in {"type", "condition", "function"} and isinstance(child, str):
                out[key] = namespaced(child)
            elif key == "name" and isinstance(child, str):
                out[key] = namespaced(child)
            elif key == "name" and not isinstance(child, str):
                out[key] = child
            else:
                out[key] = convert_loot_value(child)
        return out
    if isinstance(value, list):
        return [convert_loot_value(child) for child in value]
    return value


def convert_jutsu_loot_table() -> dict[str, object]:
    source = LEGACY_ASSETS / "loot_tables" / "jutsu_loot_table.json"
    data = json.loads(source.read_text(encoding="utf-8"))
    data = convert_loot_value(data)
    data["type"] = "minecraft:chest"
    for pool in data.get("pools", []):
        if isinstance(pool, dict):
            pool.pop("name", None)
    return data


def empty_block_loot_table(block_name: str) -> dict[str, object]:
    return {
        "type": "minecraft:block",
        "pools": [],
    }


def self_drop_block_loot_table(block_name: str) -> dict[str, object]:
    return {
        "type": "minecraft:block",
        "pools": [
            {
                "rolls": 1,
                "entries": [
                    {
                        "type": "minecraft:item",
                        "name": f"narutomod:{block_name}",
                    }
                ],
                "conditions": [
                    {
                        "condition": "minecraft:survives_explosion",
                    }
                ],
            }
        ],
    }


def convert_loot_tables() -> tuple[int, list[dict[str, object]]]:
    loot_root = ensure_inside_workspace(TARGET_DATA / "loot_tables")
    if loot_root.exists():
        shutil.rmtree(loot_root)
    (loot_root / "blocks").mkdir(parents=True, exist_ok=True)

    (loot_root / "jutsu_loot_table.json").write_text(
        json.dumps(convert_jutsu_loot_table(), ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    rows: list[dict[str, object]] = []
    block_names = []
    with (AUDIT_ROOT / "legacy_registry_names.csv").open("r", encoding="utf-8", newline="") as handle:
        for row in csv.DictReader(handle):
            if row["registry"] == "block_or_block_item" and row["name"] not in block_names:
                block_names.append(row["name"])

    for block_name in block_names:
        table = empty_block_loot_table(block_name) if block_name in NO_DROP_BLOCKS else self_drop_block_loot_table(block_name)
        (loot_root / "blocks" / f"{block_name}.json").write_text(
            json.dumps(table, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        rows.append({
            "block": block_name,
            "drop_policy": "empty" if block_name in NO_DROP_BLOCKS else "self",
            "reason": "legacy block dropped nothing or is fluid/special" if block_name in NO_DROP_BLOCKS else "basic self drop",
        })

    return 1 + len(block_names), rows


def convert_advancement_value(value: object) -> object:
    if isinstance(value, dict):
        out = {}
        for key, child in value.items():
            if key == "item" and isinstance(child, str):
                out["items"] = [child]
            else:
                out[key] = convert_advancement_value(child)
        return out
    if isinstance(value, list):
        return [convert_advancement_value(child) for child in value]
    return value


def convert_advancements() -> int:
    target = ensure_inside_workspace(TARGET_DATA / "advancements")
    if target.exists():
        shutil.rmtree(target)
    target.mkdir(parents=True, exist_ok=True)

    count = 0
    for path in sorted((LEGACY_ASSETS / "advancements").glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        criteria = data.get("criteria", {})
        for criterion in criteria.values():
            if isinstance(criterion, dict):
                conditions = criterion.get("conditions")
                if isinstance(conditions, dict):
                    criterion["conditions"] = convert_advancement_value(conditions)
        (target / path.name).write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        count += 1
    return count


def particle_names() -> list[str]:
    text = (LEGACY_JAVA / "Particles.java").read_text(encoding="utf-8", errors="replace")
    match = re.search(r"public\s+enum\s+Types\s*\{(?P<body>.*?);", text, flags=re.S)
    if not match:
        raise RuntimeError("Could not find Particles.Types enum")
    return re.findall(r'\b[A-Z0-9_]+\s*\(\s*"([^"]+)"', match.group("body"))


def convert_particles() -> int:
    particle_root = ensure_inside_workspace(TARGET_ASSETS / "particles")
    texture_root = ensure_inside_workspace(TARGET_ASSETS / "textures" / "particle")
    particle_root.mkdir(parents=True, exist_ok=True)
    texture_root.mkdir(parents=True, exist_ok=True)

    white_square = TARGET_ASSETS / "textures" / "white_square.png"
    if white_square.exists():
        shutil.copy2(white_square, texture_root / "white_square.png")

    count = 0
    for name in particle_names():
        (particle_root / f"{name}.json").write_text(
            json.dumps({"textures": ["narutomod:white_square"]}, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        count += 1
    return count


def main() -> None:
    loot_count, loot_rows = convert_loot_tables()
    advancement_count = convert_advancements()
    particle_count = convert_particles()
    write_csv(AUDIT_ROOT / "block_loot_table_policies.csv", loot_rows, ["block", "drop_policy", "reason"])

    summary = {
        "loot_tables_written": loot_count,
        "advancements_written": advancement_count,
        "particle_definitions_written": particle_count,
        "outputs": [
            "audit/block_loot_table_policies.csv",
        ],
    }
    (AUDIT_ROOT / "data_resource_conversion_summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
