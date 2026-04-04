#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Обновляет пути к Android-скриншотам в таблице README.md.
Берет самые свежие файлы вида {name}_{timestamp}.png из ru-RU папки fastlane metadata.
"""

from __future__ import annotations

import glob
import os
import re
from pathlib import Path

SCREENSHOT_NAMES = [
    "1-parksMap",
    "2-parksList",
    "3-parkDetails",
    "4-pastEvents",
    "5-eventDetails",
    "6-profile",
]

LOCALE = "ru-RU"
README_PATH = Path("README.md")
SCREENSHOTS_DIR = Path(f"fastlane/metadata/android/{LOCALE}/images/phoneScreenshots")


def find_latest_file(name: str) -> str | None:
    pattern = str(SCREENSHOTS_DIR / f"{name}_*.png")
    files = glob.glob(pattern)
    if not files:
        return None
    files.sort(key=lambda value: value.split("_")[-1], reverse=True)
    return files[0].replace(os.sep, "/")


def replace_img_tag(content: str, name: str, new_path: str) -> tuple[str, bool]:
    pattern = re.compile(
        rf'<img\s+src="\./fastlane/metadata/android/[^/]+/images/phoneScreenshots/{re.escape(name)}(?:_\d+)?\.png"[^>]*>'
    )
    replacement = f'<img src="./{new_path}" alt="">'
    updated = pattern.sub(replacement, content, count=1)
    return updated, updated != content


def main() -> int:
    if not SCREENSHOTS_DIR.is_dir():
        print(f"❌ Папка со скриншотами не найдена: {SCREENSHOTS_DIR}")
        print("💡 Сначала выполните: make screenshots")
        return 1

    if not README_PATH.is_file():
        print(f"❌ Файл не найден: {README_PATH}")
        return 1

    content = README_PATH.read_text(encoding="utf-8")
    changed = False

    for name in SCREENSHOT_NAMES:
        latest = find_latest_file(name)
        if latest is None:
            print(f"⚠️  Не найден скриншот для шаблона: {name}_*.png")
            continue

        content, updated = replace_img_tag(content, name, latest)
        if updated:
            changed = True
            print(f"✅ Обновлен путь для {name}")
        else:
            print(f"⚠️  Тег <img> для {name} не найден в README.md")

    if not changed:
        print("⚠️  Изменений не обнаружено")
        return 1

    README_PATH.write_text(content, encoding="utf-8")
    print("🎉 README.md обновлен")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
