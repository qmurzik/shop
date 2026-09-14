#!/usr/bin/env python3
from pathlib import Path
import shutil, re, sys

if len(sys.argv) != 2:
    print('Usage: python apply_overlay.py /path/to/J2ME-Loader')
    raise SystemExit(2)
root = Path(sys.argv[1]).resolve()
here = Path(__file__).resolve().parent
if not (root / 'app' / 'build.gradle').exists():
    raise SystemExit('Not a J2ME-Loader source tree: ' + str(root))

src = here / 'overlay'
for p in src.rglob('*'):
    if p.is_file():
        dst = root / p.relative_to(src)
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(p, dst)

# Give the standalone build its own package so it can coexist with J2ME Loader.
bg = root / 'app' / 'build.gradle'
s = bg.read_text(encoding='utf-8')
needle = "open {\n            buildConfigField 'boolean', 'FULL_EMULATOR', 'true'"
replacement = "open {\n            applicationId \"com.qmods.solidweapon2\"\n            buildConfigField 'boolean', 'FULL_EMULATOR', 'true'"
if 'applicationId "com.qmods.solidweapon2"' not in s:
    if needle not in s:
        raise SystemExit('Could not locate open flavor block in app/build.gradle')
    s = s.replace(needle, replacement, 1)
bg.write_text(s, encoding='utf-8')
print('Overlay applied. Build with: ./gradlew assembleOpenDebug')
