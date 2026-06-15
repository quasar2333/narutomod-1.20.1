#!/usr/bin/env python3
from __future__ import annotations

import csv
import json
import re
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = ROOT / "1.12.2" / "src" / "main" / "java"
AUDIT_ROOT = ROOT / "audit"
VARIABLES_FILE = JAVA_ROOT / "net" / "narutomod" / "NarutomodModVariables.java"


ENTITY_DATA_CALL = re.compile(r"\bgetEntityData\s*\(\s*\)\s*\.\s*(?P<method>[A-Za-z0-9_]+)\s*\(")
STRING_CONSTANT = re.compile(
    r"(?:public|private|protected)?\s*(?:static\s+)?(?:final\s+)?String\s+([A-Za-z0-9_]+)\s*=\s*\"([^\"]+)\""
)
LOCAL_VARIABLE_ALIAS = re.compile(
    r"(?:private|public|protected)?\s*static\s+final\s+String\s+([A-Za-z0-9_]+)\s*=\s*NarutomodModVariables\.([A-Za-z0-9_]+)\s*;"
)
STRING_LITERAL = re.compile(r"^\s*\"([^\"]+)\"")
VARIABLE_REF = re.compile(r"^\s*(?:[A-Za-z_][A-Za-z0-9_]*\.)*([A-Za-z_][A-Za-z0-9_]*)")


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


def variable_constants() -> dict[str, str]:
    text = read_text(VARIABLES_FILE)
    return dict(STRING_CONSTANT.findall(text))


def local_aliases(text: str, constants: dict[str, str]) -> dict[str, str]:
    aliases: dict[str, str] = {}
    for alias, constant in LOCAL_VARIABLE_ALIAS.findall(text):
        if constant in constants:
            aliases[alias] = constants[constant]
    return aliases


def first_arg(args: str) -> str:
    depth = 0
    for index, char in enumerate(args):
        if char == "(":
            depth += 1
        elif char == ")" and depth:
            depth -= 1
        elif char == "," and depth == 0:
            return args[:index].strip()
    return args.strip()


def strip_wrapping_parentheses(value: str) -> str:
    value = value.strip()
    while value.startswith("(") and value.endswith(")"):
        depth = 0
        wraps = True
        for index, char in enumerate(value):
            if char == "(":
                depth += 1
            elif char == ")":
                depth -= 1
                if depth == 0 and index != len(value) - 1:
                    wraps = False
                    break
        if not wraps:
            break
        value = value[1:-1].strip()
    return value


def call_args(text: str, start: int) -> str:
    depth = 1
    index = start
    while index < len(text):
        char = text[index]
        if char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                return text[start:index]
        index += 1
    return text[start:]


def resolve_key(argument: str, constants: dict[str, str], aliases: dict[str, str]) -> tuple[str, str]:
    argument = strip_wrapping_parentheses(argument)
    literal = STRING_LITERAL.match(argument)
    if literal:
        return literal.group(1), "literal"

    ref = VARIABLE_REF.match(argument)
    if ref:
        name = ref.group(1)
        if name in constants:
            return constants[name], f"NarutomodModVariables.{name}"
        if name in aliases:
            return aliases[name], name
    return "", argument


def scope_for(path: Path) -> str:
    rel = path.relative_to(JAVA_ROOT).as_posix()
    parts = rel.split("/")
    if len(parts) >= 3 and parts[0] == "net" and parts[1] == "narutomod":
        return parts[2]
    return "root"


def main() -> None:
    constants = variable_constants()
    rows: list[dict[str, object]] = []
    unresolved: list[dict[str, object]] = []
    raw_get_entity_data_occurrences = 0

    for path in sorted(JAVA_ROOT.rglob("*.java")):
        raw = read_text(path)
        raw_get_entity_data_occurrences += len(re.findall(r"\bgetEntityData\s*\(", raw))
        text = strip_comments_preserve_lines(raw)
        aliases = local_aliases(text, constants)
        file_constants = {**constants, **dict(STRING_CONSTANT.findall(text))}
        rel = path.relative_to(JAVA_ROOT).as_posix()
        for match in ENTITY_DATA_CALL.finditer(text):
            args = call_args(text, match.end())
            argument = first_arg(args)
            key, source = resolve_key(argument, file_constants, aliases)
            row = {
                "key": key,
                "method": match.group("method"),
                "source": source,
                "scope": scope_for(path),
                "legacy_file": rel,
                "line": line_number(text, match.start()),
                "argument": " ".join(argument.split()),
            }
            rows.append(row)
            if not key:
                unresolved.append(row)

    sorted_rows = sorted(rows, key=lambda row: (str(row["key"]), str(row["legacy_file"]), int(row["line"])))
    write_csv(
        AUDIT_ROOT / "entity_persistent_data_keys.csv",
        sorted_rows,
        ["key", "method", "source", "scope", "legacy_file", "line", "argument"],
    )
    write_csv(
        AUDIT_ROOT / "entity_persistent_data_unresolved.csv",
        unresolved,
        ["key", "method", "source", "scope", "legacy_file", "line", "argument"],
    )

    resolved_keys = {str(row["key"]) for row in rows if row["key"]}
    methods_by_key: dict[str, Counter[str]] = {}
    for row in rows:
        key = str(row["key"])
        if not key:
            continue
        methods_by_key.setdefault(key, Counter()).update([str(row["method"])])

    summary = {
        "raw_get_entity_data_occurrences": raw_get_entity_data_occurrences,
        "entity_data_calls": len(rows),
        "resolved_calls": len(rows) - len(unresolved),
        "unresolved_calls": len(unresolved),
        "unique_resolved_keys": len(resolved_keys),
        "methods": dict(sorted(Counter(str(row["method"]) for row in rows).items())),
        "keys": {
            key: dict(sorted(methods_by_key[key].items()))
            for key in sorted(methods_by_key)
        },
        "outputs": [
            "audit/entity_persistent_data_keys.csv",
            "audit/entity_persistent_data_unresolved.csv",
        ],
    }
    (AUDIT_ROOT / "entity_persistent_data_key_summary.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
