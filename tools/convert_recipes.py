#!/usr/bin/env python3
from __future__ import annotations

import csv
import json
import re
import shutil
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
LEGACY_RECIPES = ROOT / "1.12.2" / "src" / "main" / "resources" / "assets" / "narutomod" / "recipes"
TARGET_RECIPES = ROOT / "1.20.1" / "src" / "main" / "resources" / "data" / "narutomod" / "recipes"
AUDIT_ROOT = ROOT / "audit"

WOOL = {
    0: "minecraft:white_wool",
    1: "minecraft:orange_wool",
    2: "minecraft:magenta_wool",
    3: "minecraft:light_blue_wool",
    4: "minecraft:yellow_wool",
    5: "minecraft:lime_wool",
    6: "minecraft:pink_wool",
    7: "minecraft:gray_wool",
    8: "minecraft:light_gray_wool",
    9: "minecraft:cyan_wool",
    10: "minecraft:purple_wool",
    11: "minecraft:blue_wool",
    12: "minecraft:brown_wool",
    13: "minecraft:green_wool",
    14: "minecraft:red_wool",
    15: "minecraft:black_wool",
}

DYE = {
    0: "minecraft:black_dye",
    1: "minecraft:red_dye",
    2: "minecraft:green_dye",
    3: "minecraft:brown_dye",
    4: "minecraft:blue_dye",
    5: "minecraft:purple_dye",
    6: "minecraft:cyan_dye",
    7: "minecraft:light_gray_dye",
    8: "minecraft:gray_dye",
    9: "minecraft:pink_dye",
    10: "minecraft:lime_dye",
    11: "minecraft:yellow_dye",
    12: "minecraft:light_blue_dye",
    13: "minecraft:magenta_dye",
    14: "minecraft:orange_dye",
    15: "minecraft:white_dye",
}

METADATA_MAP = {
    "minecraft:wool": WOOL,
    "minecraft:dye": DYE,
    "minecraft:red_flower": {0: "minecraft:poppy"},
}

VALID_RESOURCE_LOCATION = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")


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


def convert_stack(stack: object, recipe: str, location: str, issues: list[dict[str, object]]) -> object:
    if not isinstance(stack, dict):
        return stack
    out = dict(stack)
    item = out.get("item")
    if not isinstance(item, str):
        return out

    data = out.pop("data", None)
    if data is None:
        return out

    data_int = int(data)
    mapped = METADATA_MAP.get(item, {}).get(data_int)
    if mapped is not None:
        out["item"] = mapped
        issues.append({
            "recipe": recipe,
            "location": location,
            "old_item": item,
            "data": data_int,
            "new_item": mapped,
            "action": "metadata_flattened",
        })
    elif data_int == 32767:
        issues.append({
            "recipe": recipe,
            "location": location,
            "old_item": item,
            "data": data_int,
            "new_item": item,
            "action": "wildcard_metadata_dropped",
        })
    else:
        issues.append({
            "recipe": recipe,
            "location": location,
            "old_item": item,
            "data": data_int,
            "new_item": item,
            "action": "unmapped_metadata_dropped",
        })
    return out


def convert_recipe(recipe_name: str, data: dict[str, object], issues: list[dict[str, object]]) -> dict[str, object]:
    out = dict(data)
    key = out.get("key")
    if isinstance(key, dict):
        out["key"] = {
            symbol: convert_stack(value, recipe_name, f"key.{symbol}", issues)
            for symbol, value in key.items()
        }
    ingredients = out.get("ingredients")
    if isinstance(ingredients, list):
        out["ingredients"] = [
            convert_stack(value, recipe_name, f"ingredients.{index}", issues)
            for index, value in enumerate(ingredients)
        ]
    result = out.get("result")
    if isinstance(result, dict):
        out["result"] = convert_stack(result, recipe_name, "result", issues)
    return out


def validate_recipe_items(recipe_name: str, data: object, issues: list[dict[str, object]]) -> None:
    if isinstance(data, dict):
        item = data.get("item")
        if isinstance(item, str) and not VALID_RESOURCE_LOCATION.fullmatch(item):
            issues.append({
                "recipe": recipe_name,
                "location": "item",
                "old_item": item,
                "data": "",
                "new_item": item,
                "action": "invalid_resource_location",
            })
        for value in data.values():
            validate_recipe_items(recipe_name, value, issues)
    elif isinstance(data, list):
        for value in data:
            validate_recipe_items(recipe_name, value, issues)


def main() -> None:
    target = ensure_inside_workspace(TARGET_RECIPES)
    if target.exists():
        shutil.rmtree(target)
    target.mkdir(parents=True, exist_ok=True)

    issues: list[dict[str, object]] = []
    converted = 0
    for path in sorted(LEGACY_RECIPES.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        out = convert_recipe(path.name, data, issues)
        validate_recipe_items(path.name, out, issues)
        (target / path.name).write_text(json.dumps(out, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        converted += 1

    write_csv(
        AUDIT_ROOT / "recipe_conversion_issues.csv",
        issues,
        ["recipe", "location", "old_item", "data", "new_item", "action"],
    )
    summary = {
        "recipes_converted": converted,
        "issues": len(issues),
        "output_dir": str(target.relative_to(ROOT)),
        "audit": "audit/recipe_conversion_issues.csv",
    }
    (AUDIT_ROOT / "recipe_conversion_summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
