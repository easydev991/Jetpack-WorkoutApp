#!/bin/bash

# Скрипт для автоматического изменения namespace с com.workout.jetpack_workout на com.swparks
# Выполняет:
# 1. Создание новой структуры папок
# 2. Перенос всех Kotlin файлов
# 3. Обновление package-объявлений
# 4. Обновление импортов во всех файлах

set -e  # Останавливаем выполнение при любой ошибке

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Константы
OLD_NAMESPACE="com.workout.jetpack_workout"
NEW_NAMESPACE="com.swparks"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAVA_DIR="$PROJECT_ROOT/app/src/main/java"
OLD_DIR="$JAVA_DIR/com/workout/jetpack_workout"
NEW_DIR="$JAVA_DIR/com/swparks"

# Функция для вывода сообщений
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Функция для проверки существования директории
check_dir() {
    if [ ! -d "$1" ]; then
        log_error "Директория не существует: $1"
        exit 1
    fi
}

# Функция для создания директории, если она не существует
create_dir() {
    if [ ! -d "$1" ]; then
        mkdir -p "$1"
        log_info "Создана директория: $1"
    fi
}

# Функция для обновления package-объявления в файле
update_package() {
    local file=$1
    local old_package=$2
    local new_package=$3

    # Используем sed для замены package-объявления
    sed -i '' "s|^package $old_package|package $new_package|" "$file"
}

# Функция для обновления импортов в файле
update_imports() {
    local file=$1
    local old_namespace=$2
    local new_namespace=$3

    # Заменяем все импорты со старым namespace на новый
    sed -i '' "s|import $old_namespace|import $new_namespace|g" "$file"
}

# Функция для перемещения файла
move_file() {
    local src=$1
    local dst=$2

    if [ -f "$src" ]; then
        cp "$src" "$dst"
        log_info "Перемещен файл: $(basename $src)"
    fi
}

# Этап 1: Подготовка и проверка
log_info "=== Этап 1: Подготовка и проверка ==="

check_dir "$PROJECT_ROOT"
check_dir "$JAVA_DIR"
check_dir "$OLD_DIR"

log_info "Проект: $PROJECT_ROOT"
log_info "Старый namespace: $OLD_NAMESPACE"
log_info "Новый namespace: $NEW_NAMESPACE"

# Этап 2: Создание новой структуры директорий
log_info ""
log_info "=== Этап 2: Создание новой структуры директорий ==="

create_dir "$NEW_DIR"
create_dir "$NEW_DIR/model"
create_dir "$NEW_DIR/network"
create_dir "$NEW_DIR/data"
create_dir "$NEW_DIR/utils"
create_dir "$NEW_DIR/ui/theme"
create_dir "$NEW_DIR/ui/ds"
create_dir "$NEW_DIR/ui/screens"
create_dir "$NEW_DIR/ui/screens/events"
create_dir "$NEW_DIR/ui/screens/messages"
create_dir "$NEW_DIR/ui/screens/more"
create_dir "$NEW_DIR/ui/screens/parks"
create_dir "$NEW_DIR/ui/screens/profile"

# Этап 3: Перемещение корневых файлов
log_info ""
log_info "=== Этап 3: Перемещение корневых файлов ==="

move_file "$OLD_DIR/JetpackWorkoutApplication.kt" "$NEW_DIR/JetpackWorkoutApplication.kt"
move_file "$OLD_DIR/MainActivity.kt" "$NEW_DIR/MainActivity.kt"

# Этап 4: Перемещение моделей
log_info ""
log_info "=== Этап 4: Перемещение моделей ==="

for file in "$OLD_DIR/model"/*.kt; do
    if [ -f "$file" ]; then
        move_file "$file" "$NEW_DIR/model/$(basename $file)"
    fi
done

# Этап 5: Перемещение сетевого слоя
log_info ""
log_info "=== Этап 5: Перемещение сетевого слоя ==="

move_file "$OLD_DIR/network/SWApi.kt" "$NEW_DIR/network/SWApi.kt"

# Этап 6: Перемещение данных и репозиториев
log_info ""
log_info "=== Этап 6: Перемещение данных и репозиториев ==="

for file in "$OLD_DIR/data"/*.kt; do
    if [ -f "$file" ]; then
        move_file "$file" "$NEW_DIR/data/$(basename $file)"
    fi
done

# Этап 7: Перемещение утилит
log_info ""
log_info "=== Этап 7: Перемещение утилит ==="

for file in "$OLD_DIR/utils"/*.kt; do
    if [ -f "$file" ]; then
        move_file "$file" "$NEW_DIR/utils/$(basename $file)"
    fi
done

# Этап 8: Перемещение темы
log_info ""
log_info "=== Этап 8: Перемещение темы ==="

for file in "$OLD_DIR/ui/theme"/*.kt; do
    if [ -f "$file" ]; then
        move_file "$file" "$NEW_DIR/ui/theme/$(basename $file)"
    fi
done

# Этап 9: Перемещение дизайн-системы
log_info ""
log_info "=== Этап 9: Перемещение дизайн-системы ==="

for file in "$OLD_DIR/ui/ds"/*.kt; do
    if [ -f "$file" ]; then
        move_file "$file" "$NEW_DIR/ui/ds/$(basename $file)"
    fi
done

# Этап 10: Перемещение экранов
log_info ""
log_info "=== Этап 10: Перемещение экранов ==="

for file in "$OLD_DIR/ui/screens"/*.kt; do
    if [ -f "$file" ]; then
        move_file "$file" "$NEW_DIR/ui/screens/$(basename $file)"
    fi
done

for subdir in "$OLD_DIR/ui/screens"/*; do
    if [ -d "$subdir" ]; then
        subdir_name=$(basename "$subdir")
        for file in "$subdir"/*.kt; do
            if [ -f "$file" ]; then
                move_file "$file" "$NEW_DIR/ui/screens/$subdir_name/$(basename $file)"
            fi
        done
    fi
done

# Этап 11: Обновление package-объявлений и импортов
log_info ""
log_info "=== Этап 11: Обновление package-объявлений и импортов ==="

# Функция для обработки всех Kotlin файлов в директории
process_directory() {
    local dir=$1
    local old_package=$2
    local new_package=$3

    for file in "$dir"/*.kt; do
        if [ -f "$file" ]; then
            update_package "$file" "$old_package" "$new_package"
            update_imports "$file" "$OLD_NAMESPACE" "$NEW_NAMESPACE"
        fi
    done

    # Рекурсивно обрабатываем поддиректории
    for subdir in "$dir"/*; do
        if [ -d "$subdir" ]; then
            local subdir_name=$(basename "$subdir")
            local new_sub_package="$new_package.$subdir_name"
            local old_sub_package="$old_package.$subdir_name"
            process_directory "$subdir" "$old_sub_package" "$new_sub_package"
        fi
    done
}

# Обрабатываем все файлы в новой директории
process_directory "$NEW_DIR" "$NEW_NAMESPACE" "$NEW_NAMESPACE"

# Этап 12: Обновление namespace в build.gradle.kts
log_info ""
log_info "=== Этап 12: Обновление namespace в build.gradle.kts ==="

BUILD_GRADLE="$PROJECT_ROOT/app/build.gradle.kts"
sed -i '' "s|namespace = \"$OLD_NAMESPACE\"|namespace = \"$NEW_NAMESPACE\"|" "$BUILD_GRADLE"
log_info "Обновлен namespace в build.gradle.kts"

# Этап 13: Проверка и отчет
log_info ""
log_info "=== Этап 13: Проверка и отчет ==="

# Подсчет файлов
NEW_FILES_COUNT=$(find "$NEW_DIR" -name "*.kt" | wc -l | tr -d ' ')
OLD_FILES_COUNT=$(find "$OLD_DIR" -name "*.kt" 2>/dev/null | wc -l | tr -d ' ')

log_info "Новых файлов: $NEW_FILES_COUNT"
log_info "Старых файлов: $OLD_FILES_COUNT"

if [ "$NEW_FILES_COUNT" -ne "$OLD_FILES_COUNT" ]; then
    log_warn "Внимание: количество новых и старых файлов не совпадает!"
fi

# Поиск оставшихся ссылок на старый namespace
REMAINING_IMPORTS=$(grep -r "$OLD_NAMESPACE" "$NEW_DIR" --include="*.kt" 2>/dev/null | wc -l | tr -d ' ')

if [ "$REMAINING_IMPORTS" -gt 0 ]; then
    log_warn "Найдено $REMAINING_IMPORTS ссылок на старый namespace в новых файлах"
    grep -r "$OLD_NAMESPACE" "$NEW_DIR" --include="*.kt" 2>/dev/null | head -10
else
    log_info "Все импорты успешно обновлены"
fi

# Финальное сообщение
log_info ""
log_info "=== Перенос файлов завершен ==="
log_info ""
log_info "Следующие шаги:"
log_info "1. Проверьте структуру новых файлов: $NEW_DIR"
log_info "2. Проверьте, что все импорты обновлены корректно"
log_info "3. Выполните ./gradlew clean"
log_info "4. Выполните ./gradlew build"
log_info "5. Если все работает корректно, удалите старую директорию: $OLD_DIR"
log_info "6. Зафиксируйте изменения в git"
