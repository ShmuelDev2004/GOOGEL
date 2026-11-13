"""Static checks used while no Gradle/ktlint is available: import order, unused imports, line length, rules."""
import pathlib
import re
import sys

root = pathlib.Path(__file__).resolve().parent.parent
bad = []
for p in sorted(root.rglob("*")):
    if p.suffix not in (".kt", ".kts") or "build" in p.parts:
        continue
    lines = p.read_text(encoding="utf-8").split("\n")
    idx = [i for i, l in enumerate(lines) if l.startswith("import ")]
    if idx:
        a, b = idx[0], idx[-1]
        block = lines[a:b + 1]
        if all(l.startswith("import ") for l in block):
            lines[a:b + 1] = sorted(set(block))
            p.write_text("\n".join(lines), encoding="utf-8")
        else:
            bad.append(f"{p.name}: imports not contiguous")
    body = "\n".join(l for l in lines if not l.startswith("import ") and not l.startswith("package "))
    for n, l in enumerate(lines, 1):
        if len(l) > 120:
            bad.append(f"{p.name}:{n} line too long ({len(l)})")
        if "!!" in l:
            bad.append(f"{p.name}:{n} uses !!")
        if re.match(r"import .*\*$", l):
            bad.append(f"{p.name}:{n} wildcard import")
        if l != l.rstrip():
            bad.append(f"{p.name}:{n} trailing whitespace")
        if "\t" in l:
            bad.append(f"{p.name}:{n} tab")
        if p.suffix == ".kt" and re.search("[\u0590-\u05FF]", l):
            bad.append(f"{p.name}:{n} Hebrew literal")
        if l.startswith("import "):
            name = l.split(".")[-1].strip()
            if name in ("getValue", "setValue", "provideDelegate"):
                if " by " not in body:
                    bad.append(f"{p.name}:{n} unused import {name}")
            elif not re.search(r"\b" + re.escape(name) + r"\b", body):
                bad.append(f"{p.name}:{n} unused import {name}")
    if lines and lines[-1] != "":
        bad.append(f"{p.name}: no final newline")
    if len(lines) > 1 and lines[-2] == "":
        bad.append(f"{p.name}: blank line at end")
print("\n".join(bad) if bad else "static checks OK")
sys.exit(1 if bad else 0)
