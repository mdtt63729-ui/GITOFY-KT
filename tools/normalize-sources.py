#!/usr/bin/env python3
"""Repair source files that arrived base64-encoded (a transfer accident).

Some upload paths (or API scripts) encode text files such as *.kt / *.kts as
base64. This walks the project and, for any file whose entire content is a single
base64 blob that decodes to recognisable source, writes the decoded text back.
It is a no-op on a normal checkout.
"""
import base64, os, re, sys

TARGETS = [
    "build.gradle.kts", "settings.gradle.kts", "gradle.properties",
    "gradle/libs.versions.toml", "app/build.gradle.kts",
    "app/src/main/AndroidManifest.xml",
]
for root, _, files in os.walk("app/src/main"):
    for fn in files:
        if fn.endswith((".kt", ".xml")):
            TARGETS.append(os.path.join(root, fn))

MARKERS = ("package ", "plugins {", "android {", "[versions]", "<manifest", "<?xml", "rootProject.name", "<resources")
B64 = re.compile(r"[A-Za-z0-9+/=\s]+")

fixed = 0
for p in TARGETS:
    if not os.path.exists(p):
        continue
    s = open(p, "rb").read().decode("utf-8", "ignore").strip()
    if len(s) < 40 or not B64.fullmatch(s):
        continue
    try:
        dec = base64.b64decode(re.sub(r"\s+", "", s), validate=True).decode("utf-8")
    except Exception:
        continue
    if any(m in dec for m in MARKERS):
        open(p, "w").write(dec)
        fixed += 1
        print("repaired:", p)

print("base64-encoded files repaired:", fixed)
sys.exit(0)
