#!/usr/bin/env python3
"""Generate reviewable LayerDefinition Java snippets from the legacy model manifest."""

from __future__ import annotations

import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
AUDIT = ROOT / "audit"
MANIFEST = AUDIT / "legacy_model_manifest.json"
OUT_DIR = AUDIT / "generated_layer_snippets"
GENERATED_SOURCE = (
    ROOT
    / "1.20.1"
    / "src"
    / "generated"
    / "java"
    / "net"
    / "narutomod"
    / "client"
    / "model"
    / "generated"
    / "LegacyModelLayerDefinitions.java"
)
MAX_COMPILED_BOXES = 600
NORMALIZE_DYNAMIC_POSE_FOR_COMPILED_SOURCE = {
    ("net/narutomod/entity/EntitySlug.java", "ModelSlug"),
    ("net/narutomod/entity/EntityToad.java", "ModelToad"),
    ("net/narutomod/entity/EntitySusanooClothed.java", "ModelSusanooClothed"),
    ("net/narutomod/entity/EntitySusanooWinged.java", "ModelSusanooWinged"),
    ("net/narutomod/entity/EntitySevenTails.java", "ModelSevenTails"),
    ("net/narutomod/entity/EntityPuppetSanshouo.java", "ModelSanShouo"),
    ("net/narutomod/item/ItemEightGates.java", "ModelNightguyDragon"),
}

JAVA_KEYWORDS = {
    "abstract",
    "assert",
    "boolean",
    "break",
    "byte",
    "case",
    "catch",
    "char",
    "class",
    "const",
    "continue",
    "default",
    "do",
    "double",
    "else",
    "enum",
    "extends",
    "final",
    "finally",
    "float",
    "for",
    "goto",
    "if",
    "implements",
    "import",
    "instanceof",
    "int",
    "interface",
    "long",
    "native",
    "new",
    "package",
    "private",
    "protected",
    "public",
    "return",
    "short",
    "static",
    "strictfp",
    "super",
    "switch",
    "synchronized",
    "this",
    "throw",
    "throws",
    "transient",
    "try",
    "void",
    "volatile",
    "while",
}

STANDARD_PART_NAMES = {
    "bipedHead": "head",
    "bipedHeadwear": "hat",
    "bipedBody": "body",
    "bipedRightArm": "right_arm",
    "bipedLeftArm": "left_arm",
    "bipedRightLeg": "right_leg",
    "bipedLeftLeg": "left_leg",
}


def java_float(value: str | None, default: str = "0.0F") -> str:
    if value is None or value == "":
        return default
    value = value.strip()
    if re.fullmatch(r"[-+]?\d+", value):
        return f"{value}.0F"
    if re.fullmatch(r"[-+]?(?:\d+\.\d*|\.\d+)[fFdD]?", value):
        return re.sub(r"[dD]$", "F", value if value[-1:].lower() == "f" else f"{value}F")
    return value


def java_int(value: str | None, default: str = "0") -> str:
    if value is None or value == "":
        return default
    return re.sub(r"[fFdD]$", "", value.strip())


def java_bool(value: str | None) -> bool:
    return value is not None and value.strip().lower() == "true"


def child_name(part_name: str) -> str:
    return STANDARD_PART_NAMES.get(part_name, sanitize_child_name(part_name))


def sanitize_child_name(part_name: str) -> str:
    cleaned = re.sub(r"[^A-Za-z0-9_]+", "_", part_name)
    cleaned = re.sub(r"_+", "_", cleaned).strip("_")
    return cleaned or "part"


def var_name(part_name: str) -> str:
    cleaned = sanitize_child_name(part_name)
    if cleaned[0].isdigit():
        cleaned = f"part_{cleaned}"
    if cleaned in JAVA_KEYWORDS:
        cleaned = f"{cleaned}_part"
    return cleaned


def method_name(model: dict) -> str:
    file_stem = Path(model["file"]).stem
    raw = f"{file_stem}_{model['class_name']}_{model['line_start']}"
    cleaned = sanitize_child_name(raw)
    if cleaned[0].isdigit():
        cleaned = f"model_{cleaned}"
    if cleaned in JAVA_KEYWORDS:
        cleaned = f"{cleaned}_model"
    return cleaned


def model_key(model: dict) -> tuple[str, str]:
    return model["file"], model["class_name"]


def pose(part: dict, normalize_dynamic: bool = False) -> str:
    point = part.get("rotation_point") or ["0.0F", "0.0F", "0.0F"]
    rotation = part.get("rotation") or ["0.0F", "0.0F", "0.0F"]

    def pose_float(value: str | None) -> str:
        if normalize_dynamic and not is_static_float_expression(value):
            return "0.0F"
        return java_float(value)

    point = [pose_float(value) for value in point[:3]]
    rotation = [pose_float(value) for value in rotation[:3]]
    if all(value in ("0.0F", "0F") for value in rotation):
        return f"PartPose.offset({point[0]}, {point[1]}, {point[2]})"
    return (
        "PartPose.offsetAndRotation("
        f"{point[0]}, {point[1]}, {point[2]}, {rotation[0]}, {rotation[1]}, {rotation[2]}"
        ")"
    )


def cube_builder(part: dict) -> list[str]:
    lines = ["CubeListBuilder.create()"]
    for box in part.get("boxes", []):
        source = box["source"]
        args = box["args"]
        if source == "ModelBox" and len(args) >= 11:
            tex_u, tex_v = java_int(args[1]), java_int(args[2])
            x, y, z = java_float(args[3]), java_float(args[4]), java_float(args[5])
            dx, dy, dz = java_float(args[6]), java_float(args[7]), java_float(args[8])
            grow = java_float(args[9])
            mirror = java_bool(args[10])
            lines.append(f"    .texOffs({tex_u}, {tex_v})")
            if mirror:
                lines.append("    .mirror()")
            lines.append(f"    .addBox({x}, {y}, {z}, {dx}, {dy}, {dz}, new CubeDeformation({grow}))")
            if mirror:
                lines.append("    .mirror(false)")
        elif source == "addBox" and len(args) >= 6:
            tex_u = java_int(part.get("tex_u"), "0")
            tex_v = java_int(part.get("tex_v"), "0")
            x, y, z = java_float(args[0]), java_float(args[1]), java_float(args[2])
            dx, dy, dz = java_float(args[3]), java_float(args[4]), java_float(args[5])
            grow = java_float(args[6]) if len(args) >= 7 else "0.0F"
            lines.append(f"    .texOffs({tex_u}, {tex_v})")
            lines.append(f"    .addBox({x}, {y}, {z}, {dx}, {dy}, {dz}, new CubeDeformation({grow}))")
        else:
            lines.append(f"    /* unsupported box at legacy line {box['line']}: {args} */")
    return lines


def part_lookup(model: dict) -> dict[str, dict]:
    return {part["name"]: part for part in model["parts"]}


def parent_lookup(model: dict) -> dict[str, str]:
    parents: dict[str, str] = {}
    for part in model["parts"]:
        for child in part.get("children", []):
            parents[child] = part["name"]
    return parents


def ordered_parts(model: dict) -> list[dict]:
    lookup = part_lookup(model)
    parents = parent_lookup(model)
    roots = [part["name"] for part in model["parts"] if part["name"] not in parents]
    ordered: list[dict] = []
    seen: set[str] = set()

    def visit(name: str) -> None:
        if name in seen or name not in lookup:
            return
        seen.add(name)
        part = lookup[name]
        ordered.append(part)
        for child in part.get("children", []):
            visit(child)

    for root in roots:
        visit(root)
    for part in model["parts"]:
        visit(part["name"])
    return ordered


def method_for(model: dict, method: str = "createBodyLayer", public: bool = True, normalize_dynamic_poses: bool = False) -> str:
    parents = parent_lookup(model)
    var_names = {part["name"]: var_name(part["name"]) for part in model["parts"]}
    visibility = "public " if public else ""
    lines = [
        f"// Source: {model['file']}:{model['line_start']} {model['class_name']}",
        f"{visibility}static LayerDefinition {method}() {{",
        "    MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);"
        if model["base_class"] == "ModelBiped"
        else "    MeshDefinition mesh = new MeshDefinition();",
        "    PartDefinition root = mesh.getRoot();",
        "",
    ]
    for part in ordered_parts(model):
        parent = parents.get(part["name"])
        parent_var = var_names[parent] if parent else "root"
        this_var = var_names[part["name"]]
        builder = cube_builder(part)
        lines.append(f"    PartDefinition {this_var} = {parent_var}.addOrReplaceChild(")
        lines.append(f"        \"{child_name(part['name'])}\",")
        for i, builder_line in enumerate(builder):
            suffix = "," if i == len(builder) - 1 else ""
            lines.append(f"        {builder_line}{suffix}")
        lines.append(f"        {pose(part, normalize_dynamic_poses)}")
        lines.append("    );")
        lines.append("")
    width = model.get("texture_width") or 64
    height = model.get("texture_height") or 64
    lines.append(f"    return LayerDefinition.create(mesh, {width}, {height});")
    lines.append("}")
    lines.append("")
    return "\n".join(lines)


def snippet_for(model: dict) -> str:
    return "\n".join(
        [
            f"// Source: {model['file']}:{model['line_start']} {model['class_name']}",
            "// Imports needed: CubeDeformation, CubeListBuilder, LayerDefinition, MeshDefinition, PartDefinition, PartPose",
            method_for(model),
        ]
    )


def is_snippet_candidate(model: dict) -> bool:
    return model["dynamic_box_arg_count"] == 0


def is_compiled_source_candidate(model: dict) -> bool:
    return (
        is_snippet_candidate(model)
        and model["box_count"] <= MAX_COMPILED_BOXES
        and (has_static_part_poses(model) or model_key(model) in NORMALIZE_DYNAMIC_POSE_FOR_COMPILED_SOURCE)
        and has_unique_child_graph(model)
    )


def has_static_part_poses(model: dict) -> bool:
    for part in model["parts"]:
        for value in part.get("rotation_point") or []:
            if not is_static_float_expression(value):
                return False
        for value in part.get("rotation") or []:
            if not is_static_float_expression(value):
                return False
    return True


def is_static_float_expression(value: str | None) -> bool:
    if value is None:
        return True
    value = value.strip()
    number = r"[-+]?(?:\d+(?:\.\d*)?|\.\d+)(?:[fFdD])?"
    operators = re.sub(number, "", value)
    return re.fullmatch(r"[-+*/().\s]*", operators) is not None


def has_unique_child_graph(model: dict) -> bool:
    seen_children: set[str] = set()
    for part in model["parts"]:
        children = part.get("children") or []
        if len(children) != len(set(children)):
            return False
        for child in children:
            if child in seen_children:
                return False
            seen_children.add(child)
    return True


def generated_source_for(models: list[dict]) -> str:
    lines = [
        "package net.narutomod.client.model.generated;",
        "",
        "import net.minecraft.client.model.HumanoidModel;",
        "import net.minecraft.client.model.geom.PartPose;",
        "import net.minecraft.client.model.geom.builders.CubeDeformation;",
        "import net.minecraft.client.model.geom.builders.CubeListBuilder;",
        "import net.minecraft.client.model.geom.builders.LayerDefinition;",
        "import net.minecraft.client.model.geom.builders.MeshDefinition;",
        "import net.minecraft.client.model.geom.builders.PartDefinition;",
        "",
        "public final class LegacyModelLayerDefinitions {",
        "    private LegacyModelLayerDefinitions() {",
        "    }",
        "",
    ]
    for model in models:
        normalize_dynamic_poses = model_key(model) in NORMALIZE_DYNAMIC_POSE_FOR_COMPILED_SOURCE
        for method_line in method_for(model, method_name(model), public=True, normalize_dynamic_poses=normalize_dynamic_poses).splitlines():
            lines.append(f"    {method_line}" if method_line else "")
    lines.append("}")
    lines.append("")
    return "\n".join(lines)


def main() -> None:
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))["models"]
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for old in OUT_DIR.glob("*.javafrag"):
        old.unlink()

    generated = []
    compiled_source_models = []
    skipped = []
    for model in manifest:
        if not is_snippet_candidate(model):
            skipped.append(model)
            continue
        stem = f"{Path(model['file']).stem}_{model['class_name']}_{model['line_start']}"
        out_path = OUT_DIR / f"{sanitize_child_name(stem)}.javafrag"
        out_path.write_text(snippet_for(model), encoding="utf-8")
        generated.append(model)
        if is_compiled_source_candidate(model):
            compiled_source_models.append(model)

    GENERATED_SOURCE.parent.mkdir(parents=True, exist_ok=True)
    GENERATED_SOURCE.write_text(generated_source_for(compiled_source_models), encoding="utf-8")

    summary = {
        "snippet_candidates": len(generated),
        "compiled_source_methods": len(compiled_source_models),
        "compiled_source_box_limit": MAX_COMPILED_BOXES,
        "compiled_source_requires_static_part_poses": True,
        "skipped": len(skipped),
        "array_part_models_included": sum(1 for model in generated if model["array_part_count"]),
        "skipped_dynamic_box_args": sum(1 for model in skipped if model["dynamic_box_arg_count"]),
        "output_dir": "audit/generated_layer_snippets",
        "generated_source": "1.20.1/src/generated/java/net/narutomod/client/model/generated/LegacyModelLayerDefinitions.java",
    }
    (AUDIT / "generated_layer_snippets_summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
