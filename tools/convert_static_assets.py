#!/usr/bin/env python3
from __future__ import annotations

import csv
import json
import shutil
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
LEGACY_ASSETS = ROOT / "1.12.2" / "src" / "main" / "resources" / "assets" / "narutomod"
TARGET_ASSETS = ROOT / "1.20.1" / "src" / "main" / "resources" / "assets" / "narutomod"
AUDIT_ROOT = ROOT / "audit"


def ensure_inside_workspace(path: Path) -> Path:
    resolved = path.resolve()
    if ROOT.resolve() not in resolved.parents:
        raise RuntimeError(f"Refusing to touch path outside workspace: {resolved}")
    return resolved


def copy_tree_contents(source: Path, target: Path) -> int:
    count = 0
    ensure_inside_workspace(target)
    target.mkdir(parents=True, exist_ok=True)
    for path in source.rglob("*"):
        if not path.is_file():
            continue
        relative = path.relative_to(source)
        out = target / relative
        out.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, out)
        count += 1
    return count


def write_csv(path: Path, rows: list[dict[str, object]], columns: list[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=columns, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def normalize_blockstate(data: object) -> object:
    def normalize_model_refs(value: object) -> object:
        if isinstance(value, dict):
            out = {}
            for key, child in value.items():
                if key == "model" and isinstance(child, str):
                    out[key] = normalize_model_location(child)
                else:
                    out[key] = normalize_model_refs(child)
            return out
        if isinstance(value, list):
            return [normalize_model_refs(child) for child in value]
        return value

    if not isinstance(data, dict):
        return data
    variants = data.get("variants")
    if isinstance(variants, dict) and "normal" in variants:
        variants = dict(variants)
        variants[""] = variants.pop("normal")
        data = dict(data)
        data["variants"] = variants
    return normalize_model_refs(data)


def split_resource_location(value: str, default_namespace: str = "narutomod") -> tuple[str, str]:
    if ":" in value:
        namespace, path = value.split(":", 1)
    else:
        namespace, path = default_namespace, value
    return namespace, path


def normalize_model_location(value: str) -> str:
    namespace, path = split_resource_location(value)
    if namespace != "narutomod" or path.startswith(("block/", "custom/", "item/")):
        return value
    if (TARGET_ASSETS / "models" / "block" / f"{path}.json").exists():
        return f"narutomod:block/{path}"
    return value


def normalize_model_file(path: Path) -> list[dict[str, object]]:
    data = json.loads(path.read_text(encoding="utf-8"))
    rows: list[dict[str, object]] = []

    def walk(value: object, location: str) -> object:
        if isinstance(value, dict):
            out = {}
            for key, child in value.items():
                child_location = f"{location}.{key}" if location else key
                if key == "parent" and isinstance(child, str):
                    namespace, model_path = split_resource_location(child)
                    if namespace == "narutomod" and model_path.startswith("custom/"):
                        block_path = "block/" + model_path.removeprefix("custom/")
                        if (TARGET_ASSETS / "models" / f"{block_path}.json").exists() and not (TARGET_ASSETS / "models" / f"{model_path}.json").exists():
                            rows.append({
                                "model": path.relative_to(TARGET_ASSETS).as_posix(),
                                "location": child_location,
                                "old_value": child,
                                "new_value": f"narutomod:{block_path}",
                                "action": "parent_custom_to_block",
                            })
                            out[key] = f"narutomod:{block_path}"
                            continue
                if isinstance(child, str) and key != "parent":
                    namespace, texture_path = split_resource_location(child)
                    if namespace == "narutomod" and texture_path.startswith("items/"):
                        block_texture = "blocks/" + texture_path.removeprefix("items/")
                        if not (TARGET_ASSETS / "textures" / f"{texture_path}.png").exists() and (TARGET_ASSETS / "textures" / f"{block_texture}.png").exists():
                            rows.append({
                                "model": path.relative_to(TARGET_ASSETS).as_posix(),
                                "location": child_location,
                                "old_value": child,
                                "new_value": f"narutomod:{block_texture}",
                                "action": "texture_items_to_blocks",
                            })
                            out[key] = f"narutomod:{block_texture}"
                            continue
                out[key] = walk(child, child_location)
            return out
        if isinstance(value, list):
            return [walk(child, f"{location}[{index}]") for index, child in enumerate(value)]
        return value

    normalized = walk(data, "")
    if rows:
        path.write_text(json.dumps(normalized, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return rows


def convert_blockstates() -> tuple[int, list[dict[str, object]]]:
    source = LEGACY_ASSETS / "blockstates"
    target = TARGET_ASSETS / "blockstates"
    ensure_inside_workspace(target)
    target.mkdir(parents=True, exist_ok=True)
    converted = 0
    issues: list[dict[str, object]] = []

    for path in sorted(source.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        if "forge_marker" in data:
            issues.append({
                "blockstate": path.name,
                "action": "skipped",
                "reason": "forge_marker fluid blockstate requires 1.20 fluid rewrite",
            })
            continue

        out_data = normalize_blockstate(data)
        (target / path.name).write_text(json.dumps(out_data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        converted += 1
        if isinstance(data, dict) and isinstance(data.get("variants"), dict) and "normal" in data["variants"]:
            issues.append({
                "blockstate": path.name,
                "action": "converted",
                "reason": "normal variant renamed to empty default variant",
            })

    return converted, issues


def normalize_models() -> list[dict[str, object]]:
    rows: list[dict[str, object]] = []
    for path in (TARGET_ASSETS / "models").rglob("*.json"):
        rows.extend(normalize_model_file(path))
    return rows


def main() -> None:
    textures = copy_tree_contents(LEGACY_ASSETS / "textures", TARGET_ASSETS / "textures")
    model_counts = {}
    for subdir in ("block", "custom", "item"):
        model_counts[subdir] = copy_tree_contents(LEGACY_ASSETS / "models" / subdir, TARGET_ASSETS / "models" / subdir)

    marker_model = TARGET_ASSETS / "models" / "item" / "porting_marker.json"
    if not marker_model.exists():
        marker_model.write_text(json.dumps({
            "parent": "minecraft:item/generated",
            "textures": {"layer0": "minecraft:item/paper"},
        }, indent=2) + "\n", encoding="utf-8")

    model_issues = normalize_models()
    blockstates, blockstate_issues = convert_blockstates()
    write_csv(
        AUDIT_ROOT / "blockstate_conversion_issues.csv",
        blockstate_issues,
        ["blockstate", "action", "reason"],
    )
    write_csv(
        AUDIT_ROOT / "model_conversion_issues.csv",
        model_issues,
        ["model", "location", "old_value", "new_value", "action"],
    )

    summary = {
        "textures_copied": textures,
        "models_copied": model_counts,
        "blockstates_converted": blockstates,
        "blockstate_issues": len(blockstate_issues),
        "model_issues": len(model_issues),
        "outputs": [
            "audit/blockstate_conversion_issues.csv",
            "audit/model_conversion_issues.csv",
        ],
    }
    (AUDIT_ROOT / "static_asset_migration_summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
