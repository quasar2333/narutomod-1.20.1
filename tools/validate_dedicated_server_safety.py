#!/usr/bin/env python3
from __future__ import annotations

import csv
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = ROOT / "1.20.1" / "src" / "main" / "java"
AUDIT_ROOT = ROOT / "audit"
CLIENT_SEGMENT = f"net{Path('/')}narutomod{Path('/')}client"
CLIENT_PATTERNS = [
    re.compile(r"\bnet\.minecraft\.client\b"),
    re.compile(r"\bMinecraft\.getInstance\s*\("),
]


def write_csv(path: Path, rows: list[dict[str, object]], columns: list[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=columns, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def line_number(text: str, index: int) -> int:
    return text.count("\n", 0, index) + 1


def main() -> None:
    rows: list[dict[str, object]] = []
    files_scanned = 0
    for path in sorted(JAVA_ROOT.rglob("*.java")):
        rel = path.relative_to(JAVA_ROOT)
        if str(rel).startswith(CLIENT_SEGMENT):
            continue
        files_scanned += 1
        text = path.read_text(encoding="utf-8", errors="replace")
        for pattern in CLIENT_PATTERNS:
            for match in pattern.finditer(text):
                rows.append({
                    "file": rel.as_posix(),
                    "line": line_number(text, match.start()),
                    "pattern": pattern.pattern,
                    "match": match.group(0),
                })

    write_csv(
        AUDIT_ROOT / "dedicated_server_safety_issues.csv",
        rows,
        ["file", "line", "pattern", "match"],
    )
    summary = {
        "files_scanned": files_scanned,
        "client_reference_issues": len(rows),
        "outputs": [
            "audit/dedicated_server_safety_issues.csv",
        ],
    }
    (AUDIT_ROOT / "dedicated_server_safety_summary.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
