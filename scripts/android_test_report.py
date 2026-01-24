#!/usr/bin/env python3
"""Скрипт для вывода статистики интеграционных (Android) тестов"""

import re
import sys
from pathlib import Path
import xml.etree.ElementTree as ET

# Цвета для терминала
GREEN = '\033[1;32m'
RED = '\033[1;31m'
RESET = '\033[0m'

# ANSI escape коды для очистки из строк при подсчете длины
ANSI_ESCAPE = re.compile(r'\033\[[0-9;]*m')

def strip_ansi(text: str) -> str:
    """Удалить ANSI escape коды из строки"""
    return ANSI_ESCAPE.sub('', text)

# Каталог с результатами тестов - несколько возможных путей
POSSIBLE_DIRS = [
    Path('app/build/outputs/androidTest-results/connected/debug'),
    Path('app/build/reports/androidTests/connected/debug/results'),
    Path('app/build/reports/androidTests/connectedTest-results/debug'),
    Path('app/build/test-results/connectedDebugAndroidTest'),
]

# Поиск первой существующей директории
TEST_RESULTS_DIR = None
for dir_path in POSSIBLE_DIRS:
    if dir_path.exists():
        TEST_RESULTS_DIR = dir_path
        break

# Проверка существования директории
if TEST_RESULTS_DIR is None or not TEST_RESULTS_DIR.exists():
    print(f"Директория с результатами интеграционных тестов не найдена")
    print(f"Проверенные пути:")
    for dir_path in POSSIBLE_DIRS:
        print(f"  - {dir_path}")
    print(f"\nСначала выполните: make android-test")
    sys.exit(1)

# Поиск всех XML файлов с результатами тестов
test_xml_files = list(TEST_RESULTS_DIR.rglob('*.xml'))

if not test_xml_files:
    print("XML файлы с результатами тестов не найдены")
    sys.exit(1)

# Подсчет статистики
total = 0
failed = 0
failed_tests = []
test_class_stats = {}

# Обработка каждого XML файла
for xml_file in test_xml_files:
    try:
        tree = ET.parse(xml_file)
        root = tree.getroot()

        # Получение всех testcase элементов
        testcases = root.findall('.//testcase')

        # Подсчет всех тестов и группировка по классу
        for testcase in testcases:
            # Получение имени класса из атрибута classname каждого testcase
            class_name = testcase.get('classname', 'Unknown')
            test_name = testcase.get('name', 'Unknown')

            # Подсчет общего количества тестов
            total += 1

            # Инициализация статистики для класса, если её нет
            if class_name not in test_class_stats:
                test_class_stats[class_name] = {
                    'total': 0,
                    'failed': 0,
                    'passed': 0
                }

            # Обновление статистики класса
            test_class_stats[class_name]['total'] += 1

            # Поиск упавших тестов
            failure = testcase.find('failure')
            error = testcase.find('error')

            if failure is not None or error is not None:
                failed += 1
                test_class_stats[class_name]['failed'] += 1
                failed_tests.append(f"{class_name}::{test_name}")
            else:
                test_class_stats[class_name]['passed'] += 1

    except Exception as e:
        # Если не удалось распарсить как XML, используем regex как запасной вариант
        with open(xml_file, 'r') as f:
            content = f.read()

        # Поиск всех блоков testcase (включая дочерние элементы)
        testcases = re.findall(r'<testcase[^>]*>.*?</testcase>', content, re.DOTALL)

        # Группировка тестов по классу
        for testcase in testcases:
            # Извлечение classname и name из атрибутов testcase
            class_match = re.search(r'classname="([^"]+)"', testcase)
            name_match = re.search(r'name="([^"]+)"', testcase)

            class_name = class_match.group(1) if class_match else xml_file.stem
            test_name = name_match.group(1) if name_match else 'Unknown'

            # Подсчет общего количества тестов
            total += 1

            # Инициализация статистики для класса, если её нет
            if class_name not in test_class_stats:
                test_class_stats[class_name] = {
                    'total': 0,
                    'failed': 0,
                    'passed': 0
                }

            # Обновление статистики класса
            test_class_stats[class_name]['total'] += 1

            # Поиск упавших тестов
            if '<failure' in testcase or '<error' in testcase:
                failed += 1
                test_class_stats[class_name]['failed'] += 1
                failed_tests.append(f"{class_name}::{test_name}")
            else:
                test_class_stats[class_name]['passed'] += 1

passed = total - failed

# Вывод результатов
print("=" * 80)
if failed > 0:
    print(f"{RED}❌ ТЕСТЫ ПРОВАЛИЛИСЬ{RESET}")
else:
    print(f"{GREEN}✅ СБОРКА УСПЕШНА{RESET}")
print("=" * 80)
print()

print(f"Статистика по интеграционным тестам (Android-эмулятор):")
print(f"Всего тестов: {total}")
print(f"{GREEN}Успешные: {passed}{RESET}")
if failed > 0:
    print(f"{RED}Упавшие: {failed}{RESET}")
    print(f"{RED}Список упавших тестов:{RESET}")
    for test_name in failed_tests:
        print(f"  - {test_name}")
print()

# Вывод статистики по классам
if test_class_stats:
    print("=" * 80)
    print(f"Статистика по тестовым классам ({len(test_class_stats)} классов):")
    print("=" * 80)

    # Вычисление максимальных ширин столбцов для автоматического выравнивания
    max_class_name_length = max(len("Класс"), *map(lambda x: len(x), test_class_stats.keys()))
    max_class_name_length = min(max_class_name_length, 60)  # Ограничение до 60 символов

    total_width = max(len("Всего"), 8)
    passed_width = max(len("Успешно"), 10)
    failed_width = max(len("Упало"), 8)

    # Функция для форматирования значения по центру с учетом ANSI кодов
    def center_text(text: str, width: int) -> str:
        """Отформатировать текст по центру с учетом ANSI кодов"""
        clean_text = strip_ansi(text)
        clean_len = len(clean_text)

        if clean_len >= width:
            return text

        total_padding = width - clean_len
        left_padding = total_padding // 2
        right_padding = total_padding - left_padding

        return ' ' * left_padding + text + ' ' * right_padding

    # Формирование строки заголовка (все числовые столбцы по центру)
    header = f"{'Класс':<{max_class_name_length}} {center_text('Упало', failed_width)} {center_text('Успешно', passed_width)} {center_text('Всего', total_width)}"
    print(header)
    print("-" * len(header))

    # Сортировка классов по количеству упавших тестов (по убыванию)
    sorted_classes = sorted(
        test_class_stats.items(),
        key=lambda x: (x[1]['failed'], x[1]['total']),
        reverse=True
    )

    for class_name, stats in sorted_classes:
        class_display = class_name[:max_class_name_length]  # Обрезка длинных имен
        total_display = str(stats['total'])
        passed_display = f"{GREEN}{stats['passed']}{RESET}" if stats['passed'] > 0 else "0"
        failed_display = f"{RED}{stats['failed']}{RESET}" if stats['failed'] > 0 else "0"

        # Используем выравнивание по центру для всех числовых столбцов
        print(f"{class_display:<{max_class_name_length}} {center_text(failed_display, failed_width)} {center_text(passed_display, passed_width)} {center_text(total_display, total_width)}")

    print("=" * 80)

# Итоговый статус
if failed > 0:
    sys.exit(1)
else:
    sys.exit(0)
