#!/usr/bin/env python3
from __future__ import annotations

import csv
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
TARGET_RESOURCES = ROOT / "1.20.1" / "src" / "main" / "resources"
ASSETS = TARGET_RESOURCES / "assets" / "narutomod"
AUDIT_ROOT = ROOT / "audit"
VALID_RESOURCE_SEGMENT = re.compile(r"^[a-z0-9_.-]+$")
VALID_RESOURCE_PATH = re.compile(r"^[a-z0-9_./-]+$")
VALID_RESOURCE_LOCATION = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")
VANILLA_BLOCK_ATLAS_PREFIXES = {"block/", "item/"}
_BLOCK_ATLAS_SOURCES: tuple[set[str], set[str]] | None = None


def write_csv(path: Path, rows: list[dict[str, object]], columns: list[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=columns, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def read_csv(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle))


def generated_registry_names() -> dict[str, set[str]]:
    result: dict[str, set[str]] = {}
    path = AUDIT_ROOT / "generated_registry_names.csv"
    if path.exists():
        for row in read_csv(path):
            result.setdefault(row["registry"], set()).add(row["name"])

    sound_java = ROOT / "1.20.1" / "src" / "main" / "java" / "net" / "narutomod" / "registry" / "ModSounds.java"
    if sound_java.exists():
        text = sound_java.read_text(encoding="utf-8")
        result.setdefault("sound_event", set()).update(re.findall(r'\.register\("([^"]+)"', text))
    return result


def load_lang(lang: str) -> dict[str, str]:
    path = ASSETS / "lang" / f"{lang}.json"
    return json.loads(path.read_text(encoding="utf-8"))


def validate_sound_resources() -> list[dict[str, object]]:
    rows: list[dict[str, object]] = []
    path = ASSETS / "sounds.json"
    sounds = json.loads(path.read_text(encoding="utf-8"))

    for key, definition in sounds.items():
        if not VALID_RESOURCE_PATH.fullmatch(key):
            rows.append({"kind": "invalid_sound_key", "key": key, "path": "", "details": ""})
        for entry in definition.get("sounds", []):
            name = entry["name"] if isinstance(entry, dict) else entry
            sound_path = name.split(":", 1)[1] if ":" in name else name
            if not VALID_RESOURCE_PATH.fullmatch(sound_path):
                rows.append({"kind": "invalid_sound_path", "key": key, "path": sound_path, "details": ""})
            ogg = ASSETS / "sounds" / f"{sound_path}.ogg"
            if not ogg.exists():
                rows.append({"kind": "missing_sound_file", "key": key, "path": sound_path, "details": str(ogg.relative_to(ROOT))})

    return rows


def validate_resource_paths() -> list[dict[str, object]]:
    rows: list[dict[str, object]] = []
    roots = [TARGET_RESOURCES / "assets", TARGET_RESOURCES / "data"]
    for root in roots:
        if not root.exists():
            continue
        for path in root.rglob("*"):
            if not path.is_file():
                continue
            relative = path.relative_to(TARGET_RESOURCES).as_posix()
            for segment in relative.split("/"):
                if not VALID_RESOURCE_SEGMENT.fullmatch(segment):
                    rows.append({
                        "kind": "invalid_resource_path_segment",
                        "key": "",
                        "path": relative,
                        "details": segment,
                    })
    return rows


def validate_lang_against_registry() -> list[dict[str, object]]:
    registry_rows = read_csv(AUDIT_ROOT / "legacy_registry_names.csv")
    en_us = load_lang("en_us")
    zh_cn = load_lang("zh_cn")
    rows: list[dict[str, object]] = []

    expectations: list[tuple[str, str, str]] = []
    for row in registry_rows:
        registry = row["registry"]
        name = row["name"]
        if registry == "item":
            expectations.append(("item", name, f"item.narutomod.{name}"))
        elif registry == "block_or_block_item":
            expectations.append(("block", name, f"block.narutomod.{name}"))
        elif registry == "entity_type":
            expectations.append(("entity", name, f"entity.narutomod.{name}"))

    seen = set()
    for kind, name, key in expectations:
        if (kind, name, key) in seen:
            continue
        seen.add((kind, name, key))
        if key not in en_us:
            rows.append({"kind": f"missing_en_us_{kind}", "registry_name": name, "translation_key": key})
        if key not in zh_cn:
            rows.append({"kind": f"missing_zh_cn_{kind}", "registry_name": name, "translation_key": key})

    return rows


def split_resource_location(value: str, default_namespace: str = "narutomod") -> tuple[str, str]:
    if ":" in value:
        namespace, path = value.split(":", 1)
    else:
        namespace, path = default_namespace, value
    return namespace, path


def block_atlas_sources() -> tuple[set[str], set[str]]:
    global _BLOCK_ATLAS_SOURCES
    if _BLOCK_ATLAS_SOURCES is not None:
        return _BLOCK_ATLAS_SOURCES

    prefixes = set(VANILLA_BLOCK_ATLAS_PREFIXES)
    singles: set[str] = set()
    atlas = TARGET_RESOURCES / "assets" / "minecraft" / "atlases" / "blocks.json"
    if atlas.exists():
        data = json.loads(atlas.read_text(encoding="utf-8"))
        for source in data.get("sources", []):
            if not isinstance(source, dict):
                continue
            source_type = source.get("type", "")
            if source_type in {"directory", "minecraft:directory"}:
                prefix = source.get("prefix")
                if isinstance(prefix, str):
                    prefixes.add(prefix)
            elif source_type in {"single", "minecraft:single"}:
                resource = source.get("resource")
                if isinstance(resource, str):
                    singles.add(resource)

    _BLOCK_ATLAS_SOURCES = prefixes, singles
    return _BLOCK_ATLAS_SOURCES


def is_block_atlas_texture(path: str) -> bool:
    prefixes, singles = block_atlas_sources()
    return path in singles or any(path.startswith(prefix) for prefix in prefixes)


def check_model_resource(value: str, owner: str, kind: str, rows: list[dict[str, object]], default_namespace: str = "narutomod") -> None:
    if value.startswith("#"):
        return
    namespace, path = split_resource_location(value, default_namespace)
    if namespace == "minecraft":
        return
    if namespace != "narutomod":
        rows.append({"kind": f"external_{kind}", "owner": owner, "reference": value, "details": "not checked"})
        return
    target = ASSETS / "textures" / f"{path}.png" if kind == "texture" else ASSETS / "models" / f"{path}.json"
    if not target.exists():
        rows.append({"kind": f"missing_{kind}", "owner": owner, "reference": value, "details": str(target.relative_to(ROOT))})
    elif kind == "texture" and not is_block_atlas_texture(path):
        rows.append({"kind": "texture_not_in_block_atlas", "owner": owner, "reference": value, "details": str(target.relative_to(ROOT))})


def walk_model_json(value: object, owner: str, rows: list[dict[str, object]]) -> None:
    if isinstance(value, dict):
        parent = value.get("parent")
        if isinstance(parent, str) and "/" in parent:
            check_model_resource(parent, owner, "model", rows, default_namespace="minecraft")
        textures = value.get("textures")
        if isinstance(textures, dict):
            for texture in textures.values():
                if isinstance(texture, str):
                    check_model_resource(texture, owner, "texture", rows)
        for child in value.values():
            walk_model_json(child, owner, rows)
    elif isinstance(value, list):
        for child in value:
            walk_model_json(child, owner, rows)


def collect_blockstate_models(value: object) -> list[str]:
    models: list[str] = []
    if isinstance(value, dict):
        model = value.get("model")
        if isinstance(model, str):
            models.append(model)
        for child in value.values():
            models.extend(collect_blockstate_models(child))
    elif isinstance(value, list):
        for child in value:
            models.extend(collect_blockstate_models(child))
    return models


def validate_model_references() -> list[dict[str, object]]:
    rows: list[dict[str, object]] = []
    for path in (ASSETS / "models").rglob("*.json"):
        owner = path.relative_to(ASSETS).as_posix()
        data = json.loads(path.read_text(encoding="utf-8"))
        walk_model_json(data, owner, rows)

    blockstates = ASSETS / "blockstates"
    if blockstates.exists():
        for path in blockstates.glob("*.json"):
            owner = path.relative_to(ASSETS).as_posix()
            data = json.loads(path.read_text(encoding="utf-8"))
            for model in collect_blockstate_models(data):
                check_model_resource(model, owner, "model", rows)
    return rows


def validate_recipes() -> list[dict[str, object]]:
    rows: list[dict[str, object]] = []
    recipes = TARGET_RESOURCES / "data" / "narutomod" / "recipes"
    if not recipes.exists():
        return rows

    def walk(owner: str, value: object, path: str) -> None:
        if isinstance(value, dict):
            if "data" in value:
                rows.append({"kind": "legacy_data_metadata", "recipe": owner, "path": path, "details": str(value["data"])})
            item = value.get("item")
            if isinstance(item, str) and not VALID_RESOURCE_LOCATION.fullmatch(item):
                rows.append({"kind": "invalid_item_id", "recipe": owner, "path": path, "details": item})
            for key, child in value.items():
                walk(owner, child, f"{path}.{key}" if path else key)
        elif isinstance(value, list):
            for index, child in enumerate(value):
                walk(owner, child, f"{path}[{index}]")

    for recipe in recipes.glob("*.json"):
        data = json.loads(recipe.read_text(encoding="utf-8"))
        walk(recipe.name, data, "")
    return rows


def validate_loot_tables() -> list[dict[str, object]]:
    rows: list[dict[str, object]] = []
    loot_root = TARGET_RESOURCES / "data" / "narutomod" / "loot_tables"
    if not loot_root.exists():
        return rows

    def walk(owner: str, value: object, path: str) -> None:
        if isinstance(value, dict):
            for key in ("type", "condition", "function", "name"):
                entry = value.get(key)
                if isinstance(entry, str) and key != "name" and not VALID_RESOURCE_LOCATION.fullmatch(entry):
                    rows.append({"kind": f"invalid_{key}", "file": owner, "path": path, "details": entry})
                if isinstance(entry, str) and key == "name" and not VALID_RESOURCE_LOCATION.fullmatch(entry):
                    rows.append({"kind": "invalid_name", "file": owner, "path": path, "details": entry})
            for key, child in value.items():
                walk(owner, child, f"{path}.{key}" if path else key)
        elif isinstance(value, list):
            for index, child in enumerate(value):
                walk(owner, child, f"{path}[{index}]")

    for path in loot_root.rglob("*.json"):
        data = json.loads(path.read_text(encoding="utf-8"))
        walk(path.relative_to(loot_root).as_posix(), data, "")
    return rows


def validate_advancements() -> list[dict[str, object]]:
    rows: list[dict[str, object]] = []
    advancements = TARGET_RESOURCES / "data" / "narutomod" / "advancements"
    recipes = TARGET_RESOURCES / "data" / "narutomod" / "recipes"
    recipe_ids = {f"narutomod:{path.stem}" for path in recipes.glob("*.json")} if recipes.exists() else set()
    advancement_ids = {f"narutomod:{path.stem}" for path in advancements.glob("*.json")} if advancements.exists() else set()

    if not advancements.exists():
        return rows

    def walk_conditions(owner: str, value: object, path: str) -> None:
        if isinstance(value, dict):
            if "item" in value:
                rows.append({"kind": "legacy_item_predicate", "file": owner, "path": path, "details": str(value["item"])})
            for key, child in value.items():
                walk_conditions(owner, child, f"{path}.{key}" if path else key)
        elif isinstance(value, list):
            for index, child in enumerate(value):
                walk_conditions(owner, child, f"{path}[{index}]")

    for path in advancements.glob("*.json"):
        owner = path.name
        data = json.loads(path.read_text(encoding="utf-8"))
        parent = data.get("parent")
        if isinstance(parent, str) and parent.startswith("narutomod:") and parent not in advancement_ids:
            rows.append({"kind": "missing_parent", "file": owner, "path": "parent", "details": parent})
        rewards = data.get("rewards")
        if isinstance(rewards, dict):
            for recipe in rewards.get("recipes", []):
                if isinstance(recipe, str) and recipe.startswith("narutomod:") and recipe not in recipe_ids:
                    rows.append({"kind": "missing_reward_recipe", "file": owner, "path": "rewards.recipes", "details": recipe})
        for criterion_name, criterion in data.get("criteria", {}).items():
            if isinstance(criterion, dict) and isinstance(criterion.get("conditions"), dict):
                walk_conditions(owner, criterion["conditions"], f"criteria.{criterion_name}.conditions")
    return rows


def validate_particles() -> list[dict[str, object]]:
    rows: list[dict[str, object]] = []
    particles = ASSETS / "particles"
    if not particles.exists():
        return rows
    for path in particles.glob("*.json"):
        data = json.loads(path.read_text(encoding="utf-8"))
        textures = data.get("textures", [])
        if not isinstance(textures, list):
            rows.append({"kind": "invalid_textures", "file": path.name, "path": "textures", "details": ""})
            continue
        for texture in textures:
            if not isinstance(texture, str):
                rows.append({"kind": "invalid_texture_value", "file": path.name, "path": "textures", "details": str(texture)})
                continue
            namespace, texture_path = split_resource_location(texture)
            if namespace == "narutomod":
                target = ASSETS / "textures" / "particle" / f"{texture_path}.png"
                if not target.exists():
                    rows.append({"kind": "missing_particle_texture", "file": path.name, "path": "textures", "details": texture})
    return rows


def validate_registry_references() -> list[dict[str, object]]:
    rows: list[dict[str, object]] = []
    generated = generated_registry_names()
    items = generated.get("item", set())
    blocks = generated.get("block", set())
    particles = generated.get("particle_type", set())

    def check_item(owner: str, path: str, item_id: str) -> None:
        if not item_id.startswith("narutomod:"):
            return
        name = item_id.split(":", 1)[1]
        if name not in items:
            rows.append({"kind": "missing_item_registry", "owner": owner, "path": path, "reference": item_id})

    def walk_items(owner: str, value: object, path: str) -> None:
        if isinstance(value, dict):
            item = value.get("item")
            if isinstance(item, str):
                check_item(owner, f"{path}.item" if path else "item", item)
            items_value = value.get("items")
            if isinstance(items_value, list):
                for index, item_id in enumerate(items_value):
                    if isinstance(item_id, str):
                        check_item(owner, f"{path}.items[{index}]" if path else f"items[{index}]", item_id)
            name = value.get("name")
            if isinstance(name, str):
                check_item(owner, f"{path}.name" if path else "name", name)
            for key, child in value.items():
                walk_items(owner, child, f"{path}.{key}" if path else key)
        elif isinstance(value, list):
            for index, child in enumerate(value):
                walk_items(owner, child, f"{path}[{index}]")

    for root in [
        TARGET_RESOURCES / "data" / "narutomod" / "recipes",
        TARGET_RESOURCES / "data" / "narutomod" / "loot_tables",
        TARGET_RESOURCES / "data" / "narutomod" / "advancements",
    ]:
        if root.exists():
            for path in root.rglob("*.json"):
                data = json.loads(path.read_text(encoding="utf-8"))
                walk_items(path.relative_to(TARGET_RESOURCES).as_posix(), data, "")

    block_loot = TARGET_RESOURCES / "data" / "narutomod" / "loot_tables" / "blocks"
    if block_loot.exists():
        for path in block_loot.glob("*.json"):
            if path.stem not in blocks:
                rows.append({"kind": "missing_block_registry", "owner": path.name, "path": "filename", "reference": path.stem})

    particle_root = ASSETS / "particles"
    if particle_root.exists():
        for path in particle_root.glob("*.json"):
            if path.stem not in particles:
                rows.append({"kind": "missing_particle_type_registry", "owner": path.name, "path": "filename", "reference": path.stem})
    return rows


def main() -> None:
    sound_rows = validate_sound_resources()
    path_rows = validate_resource_paths()
    lang_rows = validate_lang_against_registry()
    model_rows = validate_model_references()
    recipe_rows = validate_recipes()
    loot_rows = validate_loot_tables()
    advancement_rows = validate_advancements()
    particle_rows = validate_particles()
    registry_rows = validate_registry_references()

    write_csv(AUDIT_ROOT / "resource_path_issues.csv", sound_rows + path_rows, ["kind", "key", "path", "details"])
    write_csv(AUDIT_ROOT / "lang_registry_gaps.csv", lang_rows, ["kind", "registry_name", "translation_key"])
    write_csv(AUDIT_ROOT / "model_reference_issues.csv", model_rows, ["kind", "owner", "reference", "details"])
    write_csv(AUDIT_ROOT / "recipe_validation_issues.csv", recipe_rows, ["kind", "recipe", "path", "details"])
    write_csv(AUDIT_ROOT / "loot_table_validation_issues.csv", loot_rows, ["kind", "file", "path", "details"])
    write_csv(AUDIT_ROOT / "advancement_validation_issues.csv", advancement_rows, ["kind", "file", "path", "details"])
    write_csv(AUDIT_ROOT / "particle_validation_issues.csv", particle_rows, ["kind", "file", "path", "details"])
    write_csv(AUDIT_ROOT / "registry_reference_issues.csv", registry_rows, ["kind", "owner", "path", "reference"])

    summary = {
        "sound_or_path_issues": len(sound_rows) + len(path_rows),
        "lang_registry_gaps": len(lang_rows),
        "model_reference_issues": len(model_rows),
        "recipe_validation_issues": len(recipe_rows),
        "loot_table_validation_issues": len(loot_rows),
        "advancement_validation_issues": len(advancement_rows),
        "particle_validation_issues": len(particle_rows),
        "registry_reference_issues": len(registry_rows),
        "outputs": [
            "audit/resource_path_issues.csv",
            "audit/lang_registry_gaps.csv",
            "audit/model_reference_issues.csv",
            "audit/recipe_validation_issues.csv",
            "audit/loot_table_validation_issues.csv",
            "audit/advancement_validation_issues.csv",
            "audit/particle_validation_issues.csv",
            "audit/registry_reference_issues.csv",
        ],
    }
    (AUDIT_ROOT / "resource_validation_summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
