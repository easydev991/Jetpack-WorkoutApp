# Скрипты для изменения namespace

В этой папке находятся скрипты для автоматизации изменения namespace с `com.workout.jetpack_workout` на `com.swparks`.

## Скрипты

### change-namespace.sh

Автоматизированный скрипт для изменения namespace. Выполняет все необходимые шаги:

- Создание новой структуры директорий
- Перенос всех Kotlin файлов в новую структуру пакетов
- Обновление package-объявлений во всех файлах
- Обновление импортов во всех файлах
- Обновление namespace в `build.gradle.kts`
- Генерация отчета о выполнении

#### Использование

```bash
# Из корня проекта
./scripts/change-namespace.sh
```

#### После выполнения скрипта

1. Проверьте структуру новых файлов: `app/src/main/java/com/swparks`
2. Проверьте, что все импорты обновлены корректно
3. Выполните `./gradlew clean`
4. Выполните `./gradlew build`
5. Если все работает корректно, удалите старую директорию: `app/src/main/java/com/workout/jetpack_workout`
6. Зафиксируйте изменения в git

#### Вывод скрипта

Скрипт выводит цветные сообщения:
- 🟢 `[INFO]` - информационные сообщения
- 🟡 `[WARN]` - предупреждения
- 🔴 `[ERROR]` - сообщения об ошибках

В конце выполнения скрипт показывает:
- Количество перемещенных файлов
- Оставшиеся ссылки на старый namespace (если есть)
- Следующие шаги

### revert-namespace.sh

Скрипт для отката изменений, если что-то пошло не так:

- Удаление новой структуры директорий
- Восстановление namespace в `build.gradle.kts`
- Проверка результата

#### Использование

```bash
# Из корня проекта
./scripts/revert-namespace.sh
```

#### Когда использовать

- Если после выполнения `change-namespace.sh` проект не собирается
- Если обнаружены ошибки в структуре пакетов
- Если нужно отменить изменения и попробовать другой подход

## Подготовка перед выполнением

### 1. Создайте коммит с текущим состоянием

```bash
git add .
git commit -m "feat: backup before namespace change"
```

### 2. Рекомендуется создать feature-branch

```bash
git checkout -b feature/change-namespace-com-swparks
```

## Полный рабочий процесс

### Выполнение изменения namespace

```bash
# 1. Создаем backup
git add .
git commit -m "feat: backup before namespace change"
git checkout -b feature/change-namespace-com-swparks

# 2. Запускаем скрипт изменения namespace
./scripts/change-namespace.sh

# 3. Проверяем результаты
find app/src/main/java/com/swparks -name "*.kt" | wc -l  # Должно быть 58
grep -r "com.workout.jetpack_workout" app/src/main/java/com/swparks --include="*.kt"  # Не должно быть результатов

# 4. Собираем проект
./gradlew clean
./gradlew build

# 5. Если сборка успешна, удаляем старую директорию
rm -rf app/src/main/java/com/workout/jetpack_workout

# 6. Финальная сборка
./gradlew build

# 7. Фиксируем изменения
git add .
git commit -m "refactor: change namespace from com.workout.jetpack_workout to com.swparks"

# 8. Сливаем в main
git checkout main
git merge feature/change-namespace-com-swparks
git push origin main
```

### Откат изменений (если нужно)

```bash
# Запускаем скрипт отката
./scripts/revert-namespace.sh

# Если старая директория была удалена
git checkout app/src/main/java/com/workout/jetpack_workout

# Собираем проект
./gradlew clean
./gradlew build
```

## Возможные проблемы

### Ошибка: Permission denied

**Решение:**

```bash
chmod +x scripts/change-namespace.sh
chmod +x scripts/revert-namespace.sh
```

### Ошибка: Command not found

**Решение:** Убедитесь, что вы используете bash:

```bash
#!/bin/bash
```

В начале скриптов уже указано использование bash, но если у вас есть проблемы, проверьте shell:

```bash
echo $SHELL
```

Должно быть `/bin/bash` или совместимый shell.

### Ошибки при сборке после выполнения скрипта

**Решение:**

1. Выполните `./gradlew clean`
2. Удалите папку `app/build`
3. Выполните `./gradlew build`
4. Если ошибки продолжаются, используйте `./scripts/revert-namespace.sh` и попробуйте ручной подход

### Разное количество файлов

Если скрипт сообщает, что количество новых и старых файлов не совпадает:

1. Проверьте лог выполнения скрипта
2. Сравните списки файлов:
   ```bash
   ls -R app/src/main/java/com/workout/jetpack_workout
   ls -R app/src/main/java/com/swparks
   ```
3. Скопируйте недостающие файлы вручную

## Требования

- Bash (на macOS и Linux уже установлен по умолчанию)
- Gradle
- Доступ к файловой системе проекта
- Git (для создания backup)

## Примечания

- Скрипты работают на macOS и Linux
- Скрипты используют `set -e` для остановки при ошибках
- Скрипты запрашивают подтверждение перед критическими действиями (revert-namespace.sh)
- Всегда создавайте backup перед выполнением скриптов

## Дополнительная информация

Более подробная информация о процессе изменения namespace находится в:
- `../docs/change-namespace-plan.md`
