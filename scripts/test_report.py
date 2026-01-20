#!/usr/bin/env python3
"""Скрипт для вывода статистики тестов"""

import re
from pathlib import Path
import xml.etree.ElementTree as ET

# Цвета для терминала
YELLOW = '\033[1;33m'
GREEN = '\033[1;32m'
RED = '\033[1;31m'
RESET = '\033[0m'

# Каталог с результатами тестов
TEST_RESULTS_DIR = Path('app/build/test-results')

# Проверка существования директории
if not TEST_RESULTS_DIR.exists():
    print(f"Директория с результатами тестов не найдена: {TEST_RESULTS_DIR}")
    exit(1)

# Поиск всех XML файлов с результатами тестов
test_xml_files = list(TEST_RESULTS_DIR.rglob('*.xml'))

if not test_xml_files:
    print("XML файлы с результатами тестов не найдены")
    exit(1)

# Подсчет статистики
total = 0
failed = 0
failed_tests = []

# Обработка каждого XML файла
for xml_file in test_xml_files:
    try:
        tree = ET.parse(xml_file)
        root = tree.getroot()

        # Подсчет всех testcase элементов
        testcases = root.findall('.//testcase')
        total += len(testcases)

        # Поиск упавших тестов
        for testcase in testcases:
            failure = testcase.find('failure')
            error = testcase.find('error')
            
            if failure is not None or error is not None:
                failed += 1
                
                # Извлечение имени теста из атрибута name
                test_name = testcase.get('name')
                if test_name:
                    failed_tests.append(test_name)
    except Exception as e:
        # Если не удалось распарсить как XML, используем regex как запасной вариант
        with open(xml_file, 'r') as f:
            content = f.read()
        
        # Поиск всех блоков testcase (включая дочерние элементы)
        testcases = re.findall(r'<testcase[^>]*>.*?</testcase>', content, re.DOTALL)
        total += len(testcases)
        
        # Поиск упавших тестов
        for testcase in testcases:
            if '<failure' in testcase or '<error' in testcase:
                failed += 1
                
                # Извлечение имени теста
                match = re.search(r'name="([^"]+)"', testcase)
                if match:
                    test_name = match.group(1)
                    failed_tests.append(test_name)

passed = total - failed

# Вывод результатов
print("Статистика тестов:")
print(f"Всего тестов: {total}")
print(f"{GREEN}Успешные: {passed}{RESET}")

if failed > 0:
    print(f"{RED}Упавшие: {failed}{RESET}")
    print(f"{RED}Список упавших тестов:{RESET}")
    for test_name in failed_tests:
        print(f"  - {test_name}")
