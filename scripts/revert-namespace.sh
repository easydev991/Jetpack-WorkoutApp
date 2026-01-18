#!/bin/bash

# Скрипт для отката изменений namespace
# Удаляет новую структуру пакетов и восстанавливает исходную конфигурацию

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
BUILD_GRADLE="$PROJECT_ROOT/app/build.gradle.kts"

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

# Функция для подтверждения действия
confirm_action() {
    local message=$1
    read -p "$message (y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_info "Отмена операции"
        exit 0
    fi
}

# Проверка существования новой директории
if [ ! -d "$NEW_DIR" ]; then
    log_error "Новая директория не существует: $NEW_DIR"
    log_info "Нечего откатывать"
    exit 1
fi

# Проверка существования старой директории
if [ ! -d "$OLD_DIR" ]; then
    log_warn "Старая директория не существует: $OLD_DIR"
    confirm_action "Старая директория отсутствует, продолжить с откатом конфигурации?"
fi

log_info "=== Откат изменений namespace ==="
log_info ""
log_info "Новая директория: $NEW_DIR"
log_info "Старая директория: $OLD_DIR"
log_info ""

# Подтверждение удаления
confirm_action "Вы уверены, что хотите удалить новую директорию и откатить изменения в build.gradle.kts?"

# Этап 1: Удаление новой структуры директорий
log_info "=== Этап 1: Удаление новой структуры директорий ==="

if [ -d "$NEW_DIR" ]; then
    rm -rf "$NEW_DIR"
    log_info "Удалена новая директория: $NEW_DIR"
else
    log_warn "Новая директория не найдена, пропускаем удаление"
fi

# Этап 2: Восстановление namespace в build.gradle.kts
log_info ""
log_info "=== Этап 2: Восстановление namespace в build.gradle.kts ==="

sed -i '' "s|namespace = \"$NEW_NAMESPACE\"|namespace = \"$OLD_NAMESPACE\"|" "$BUILD_GRADLE"
log_info "Восстановлен namespace в build.gradle.kts"

# Этап 3: Проверка результата
log_info ""
log_info "=== Этап 3: Проверка результата ==="

# Проверка, что новая директория удалена
if [ -d "$NEW_DIR" ]; then
    log_error "Новая директория все еще существует: $NEW_DIR"
    exit 1
else
    log_info "✅ Новая директория успешно удалена"
fi

# Проверка, что старая директория существует (если она существовала до изменений)
if [ -d "$OLD_DIR" ]; then
    log_info "✅ Старая директория существует: $OLD_DIR"
else
    log_warn "⚠️ Старая директория не найдена: $OLD_DIR"
    log_warn "Возможно, она была удалена до применения скрипта change-namespace.sh"
fi

# Проверка namespace в build.gradle.kts
if grep -q "namespace = \"$OLD_NAMESPACE\"" "$BUILD_GRADLE"; then
    log_info "✅ Namespace в build.gradle.kts восстановлен"
else
    log_error "❌ Namespace в build.gradle.kts не восстановлен"
    exit 1
fi

# Подсчет файлов в старой директории
if [ -d "$OLD_DIR" ]; then
    OLD_FILES_COUNT=$(find "$OLD_DIR" -name "*.kt" | wc -l | tr -d ' ')
    log_info "Файлов в старой директории: $OLD_FILES_COUNT"
fi

# Финальное сообщение
log_info ""
log_info "=== Откат завершен ==="
log_info ""
log_info "Следующие шаги:"
if [ -d "$OLD_DIR" ]; then
    log_info "1. Выполните ./gradlew clean"
    log_info "2. Выполните ./gradlew build"
    log_info "3. Убедитесь, что проект собирается корректно"
else
    log_warn "⚠️ Старая директория не найдена!"
    log_warn "Если вы удалили её до применения скрипта, восстановите проект из git:"
    log_warn "  git checkout app/src/main/java/com/workout/jetpack_workout"
    log_info ""
    log_info "После восстановления:"
    log_info "1. Выполните ./gradlew clean"
    log_info "2. Выполните ./gradlew build"
fi
