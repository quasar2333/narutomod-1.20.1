#!/usr/bin/env python3
"""Extract legacy 1.12 ModelBase/ModelBiped geometry into audit manifests."""

from __future__ import annotations

import csv
import json
import re
from dataclasses import dataclass, field
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OLD_JAVA = ROOT / "1.12.2" / "src" / "main" / "java"
AUDIT = ROOT / "audit"

MODEL_CLASS_RE = re.compile(r"\bclass\s+([A-Za-z_]\w*)\s+extends\s+(ModelBase|ModelBiped|ModelQuadruped)\b")
PART_REF = r"(?:this\.)?([A-Za-z_]\w*(?:\s*\[[^\]]+\])*)"
NUM_LIKE = r"[-+]?(?:\d+(?:\.\d*)?|\.\d+)(?:[fFdD])?"


@dataclass
class Box:
    part: str
    source: str
    args: list[str]
    line: int


@dataclass
class Part:
    name: str
    line: int
    tex_u: str | None = None
    tex_v: str | None = None
    rotation_point: list[str] | None = None
    rotation: list[str] | None = None
    boxes: list[Box] = field(default_factory=list)
    children: list[str] = field(default_factory=list)


def strip_comments(text: str) -> str:
    out: list[str] = []
    i = 0
    in_string: str | None = None
    while i < len(text):
        ch = text[i]
        nxt = text[i + 1] if i + 1 < len(text) else ""
        if in_string:
            out.append(ch)
            if ch == "\\" and i + 1 < len(text):
                out.append(text[i + 1])
                i += 2
                continue
            if ch == in_string:
                in_string = None
            i += 1
            continue
        if ch in ("'", '"'):
            in_string = ch
            out.append(ch)
            i += 1
            continue
        if ch == "/" and nxt == "/":
            out.extend("  ")
            i += 2
            while i < len(text) and text[i] != "\n":
                out.append(" ")
                i += 1
            continue
        if ch == "/" and nxt == "*":
            out.extend("  ")
            i += 2
            while i < len(text) - 1:
                if text[i] == "*" and text[i + 1] == "/":
                    out.extend("  ")
                    i += 2
                    break
                out.append("\n" if text[i] == "\n" else " ")
                i += 1
            continue
        out.append(ch)
        i += 1
    return "".join(out)


def line_no(text: str, offset: int) -> int:
    return text.count("\n", 0, offset) + 1


def find_matching_brace(text: str, open_index: int) -> int:
    depth = 0
    for i in range(open_index, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return i
    raise ValueError(f"unmatched brace at offset {open_index}")


def split_args(args: str) -> list[str]:
    result: list[str] = []
    current: list[str] = []
    depth = 0
    for ch in args:
        if ch in "([{":
            depth += 1
        elif ch in ")]}":
            depth -= 1
        if ch == "," and depth == 0:
            result.append("".join(current).strip())
            current = []
        else:
            current.append(ch)
    if current:
        result.append("".join(current).strip())
    return result


def normalize_part(raw: str) -> str:
    return re.sub(r"\s+", "", raw.replace("this.", ""))


def is_literal_number(value: str) -> bool:
    return re.fullmatch(NUM_LIKE, value.strip()) is not None


def extract_model(java_file: Path, text: str, stripped: str, match: re.Match[str]) -> dict:
    class_name = match.group(1)
    base_class = match.group(2)
    open_index = stripped.find("{", match.end())
    close_index = find_matching_brace(stripped, open_index)
    body = stripped[open_index + 1 : close_index]
    body_offset = open_index + 1

    parts: dict[str, Part] = {}

    texture_width = first_match(body, r"(?:this\.)?textureWidth\s*=\s*(\d+)")
    texture_height = first_match(body, r"(?:this\.)?textureHeight\s*=\s*(\d+)")

    new_renderer_re = re.compile(
        rf"{PART_REF}\s*=\s*new\s+ModelRenderer\s*\(\s*this\s*(?:,\s*({NUM_LIKE})\s*,\s*({NUM_LIKE})\s*)?\s*\)"
    )
    for part_match in new_renderer_re.finditer(body):
        part_name = normalize_part(part_match.group(1))
        parts.setdefault(
            part_name,
            Part(
                name=part_name,
                line=line_no(stripped, body_offset + part_match.start()),
                tex_u=part_match.group(2),
                tex_v=part_match.group(3),
            ),
        )

    rotation_point_re = re.compile(rf"{PART_REF}\s*\.\s*setRotationPoint\s*\(([^;]+?)\)\s*;")
    for point_match in rotation_point_re.finditer(body):
        part_name = normalize_part(point_match.group(1))
        part = parts.setdefault(part_name, Part(name=part_name, line=line_no(stripped, body_offset + point_match.start())))
        part.rotation_point = split_args(point_match.group(2))

    child_re = re.compile(rf"{PART_REF}\s*\.\s*addChild\s*\(\s*{PART_REF}\s*\)\s*;")
    for child_match in child_re.finditer(body):
        parent = normalize_part(child_match.group(1))
        child = normalize_part(child_match.group(2))
        parts.setdefault(parent, Part(name=parent, line=line_no(stripped, body_offset + child_match.start()))).children.append(child)
        parts.setdefault(child, Part(name=child, line=line_no(stripped, body_offset + child_match.start())))

    set_rotate_re = re.compile(rf"\bsetRotat(?:e|ion)Angle\s*\(\s*{PART_REF}\s*,\s*([^)]+?)\)\s*;")
    for rotate_match in set_rotate_re.finditer(body):
        part_name = normalize_part(rotate_match.group(1))
        if part_name not in parts:
            continue
        part = parts[part_name]
        part.rotation = split_args(rotate_match.group(2))

    direct_rotate_re = re.compile(rf"{PART_REF}\s*\.\s*rotateAngle([XYZ])\s*=\s*([^;]+?)\s*;")
    for rotate_match in direct_rotate_re.finditer(body):
        part_name = normalize_part(rotate_match.group(1))
        if part_name not in parts:
            continue
        axis = rotate_match.group(2)
        value = rotate_match.group(3).strip()
        part = parts[part_name]
        rotation = part.rotation or ["0.0F", "0.0F", "0.0F"]
        rotation["XYZ".index(axis)] = value
        part.rotation = rotation

    add_box_re = re.compile(rf"{PART_REF}\s*\.\s*addBox\s*\(([^;]+?)\)\s*;")
    for box_match in add_box_re.finditer(body):
        part_name = normalize_part(box_match.group(1))
        line = line_no(stripped, body_offset + box_match.start())
        box = Box(part=part_name, source="addBox", args=split_args(box_match.group(2)), line=line)
        parts.setdefault(part_name, Part(name=part_name, line=line)).boxes.append(box)

    model_box_re = re.compile(rf"{PART_REF}\s*\.\s*cubeList\s*\.\s*add\s*\(\s*new\s+ModelBox\s*\(([^;]+?)\)\s*\)\s*;")
    for box_match in model_box_re.finditer(body):
        part_name = normalize_part(box_match.group(1))
        line = line_no(stripped, body_offset + box_match.start())
        args = split_args(box_match.group(2))
        box = Box(part=part_name, source="ModelBox", args=args, line=line)
        parts.setdefault(part_name, Part(name=part_name, line=line)).boxes.append(box)

    children = {child for part in parts.values() for child in part.children}
    root_parts = sorted(name for name in parts if name not in children)
    array_parts = [name for name in parts if "[" in name]
    boxes = [box for part in parts.values() for box in part.boxes]
    dynamic_box_args = [
        {"part": box.part, "line": box.line, "arg": arg}
        for box in boxes
        for arg in normalized_box_numeric_args(box)
        if not is_literal_number(arg)
    ]

    return {
        "file": str(java_file.relative_to(OLD_JAVA)).replace("\\", "/"),
        "class_name": class_name,
        "base_class": base_class,
        "line_start": line_no(stripped, match.start()),
        "line_end": line_no(stripped, close_index),
        "texture_width": int(texture_width) if texture_width else None,
        "texture_height": int(texture_height) if texture_height else None,
        "part_count": len(parts),
        "box_count": len(boxes),
        "child_link_count": sum(len(part.children) for part in parts.values()),
        "root_parts": root_parts,
        "array_part_count": len(array_parts),
        "dynamic_box_arg_count": len(dynamic_box_args),
        "parts": [part_to_json(part) for part in sorted(parts.values(), key=lambda p: (p.line, p.name))],
        "dynamic_box_args": dynamic_box_args[:200],
    }


def first_match(text: str, pattern: str) -> str | None:
    match = re.search(pattern, text)
    return match.group(1) if match else None


def normalized_box_numeric_args(box: Box) -> list[str]:
    args = box.args
    if box.source == "ModelBox":
        return args[1:10]
    return args


def part_to_json(part: Part) -> dict:
    return {
        "name": part.name,
        "line": part.line,
        "tex_u": part.tex_u,
        "tex_v": part.tex_v,
        "rotation_point": part.rotation_point,
        "rotation": part.rotation,
        "children": part.children,
        "boxes": [
            {
                "source": box.source,
                "line": box.line,
                "args": box.args,
            }
            for box in part.boxes
        ],
    }


def main() -> None:
    AUDIT.mkdir(parents=True, exist_ok=True)
    models: list[dict] = []
    for java_file in sorted(OLD_JAVA.rglob("*.java")):
        text = java_file.read_text(encoding="utf-8", errors="replace")
        stripped = strip_comments(text)
        for match in MODEL_CLASS_RE.finditer(stripped):
            models.append(extract_model(java_file, text, stripped, match))

    summary_path = AUDIT / "legacy_model_manifest.csv"
    json_path = AUDIT / "legacy_model_manifest.json"
    sample_path = AUDIT / "model_sample_modelhelmetsnug.json"
    dynamic_args_path = AUDIT / "legacy_model_dynamic_box_args.csv"

    with summary_path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(
            handle,
            fieldnames=[
                "file",
                "class_name",
                "base_class",
                "line_start",
                "line_end",
                "texture_width",
                "texture_height",
                "part_count",
                "box_count",
                "child_link_count",
                "root_parts",
                "array_part_count",
                "dynamic_box_arg_count",
            ],
        )
        writer.writeheader()
        for model in models:
            writer.writerow(
                {
                    "file": model["file"],
                    "class_name": model["class_name"],
                    "base_class": model["base_class"],
                    "line_start": model["line_start"],
                    "line_end": model["line_end"],
                    "texture_width": model["texture_width"],
                    "texture_height": model["texture_height"],
                    "part_count": model["part_count"],
                    "box_count": model["box_count"],
                    "child_link_count": model["child_link_count"],
                    "root_parts": "|".join(model["root_parts"]),
                    "array_part_count": model["array_part_count"],
                    "dynamic_box_arg_count": model["dynamic_box_arg_count"],
                }
            )

    json_path.write_text(json.dumps({"models": models}, indent=2), encoding="utf-8")
    samples = [model for model in models if model["class_name"] == "ModelHelmetSnug"]
    sample_path.write_text(json.dumps({"models": samples}, indent=2), encoding="utf-8")

    with dynamic_args_path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=["file", "class_name", "part", "line", "arg"])
        writer.writeheader()
        for model in models:
            for row in model["dynamic_box_args"]:
                writer.writerow(
                    {
                        "file": model["file"],
                        "class_name": model["class_name"],
                        "part": row["part"],
                        "line": row["line"],
                        "arg": row["arg"],
                    }
                )

    by_source_dir: dict[str, int] = {}
    for model in models:
        source_dir = model["file"].split("/")[2] if "/" in model["file"] else ""
        by_source_dir[source_dir] = by_source_dir.get(source_dir, 0) + 1

    summary = {
        "model_classes": len(models),
        "model_classes_by_source_dir": by_source_dir,
        "model_helmet_snug_samples": len(samples),
        "models_with_array_parts": sum(1 for model in models if model["array_part_count"]),
        "models_with_dynamic_box_args": sum(1 for model in models if model["dynamic_box_arg_count"]),
        "dynamic_box_arg_rows": sum(model["dynamic_box_arg_count"] for model in models),
        "outputs": [
            "audit/legacy_model_manifest.csv",
            "audit/legacy_model_manifest.json",
            "audit/model_sample_modelhelmetsnug.json",
            "audit/legacy_model_dynamic_box_args.csv",
            "audit/legacy_model_manifest_summary.json",
        ],
    }
    (AUDIT / "legacy_model_manifest_summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
