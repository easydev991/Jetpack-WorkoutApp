# Автоматическое обновление версий в README.md

## Описание

Gradle-таск `updateReadmeVersions` автоматически синхронизирует версии в README.md с актуальными данными из файлов проекта.

## Как это работает

### Источники данных

Таск считывает версии из:

| Версия             | Источник                                   |
|--------------------|--------------------------------------------|
| Kotlin, AGP        | `gradle/libs.versions.toml`                |
| compileSdk, minSdk | `app/build.gradle.kts`                     |
| Gradle             | `gradle/wrapper/gradle-wrapper.properties` |

### Обновление README.md

Таск находит в README.md блок между маркерами:

```markdown
<!-- BEGIN_VERSIONS -->
<!-- END_VERSIONS -->
```

И заменяет его содержимое на актуальные бейджи с версиями.

## Использование

### Ручной запуск

```bash
./gradlew updateReadmeVersions
```

### Через Make

```bash
make update_readme_versions
```

Версии также обновляются автоматически при `make update_readme` (обновление таблицы скриншотов).

### Автоматически при коммите

Pre-commit hook автоматически обновляет версии перед каждым коммитом:

```bash
.githooks/pre-commit
```

Установка хуков (отдельная цель, `make setup` её не вызывает):

```bash
make _setup_git_hooks
```

или вручную:

```bash
git config core.hooksPath .githooks
```

## Формат бейджей

```markdown
<!-- BEGIN_VERSIONS -->
[<img alt="Kotlin Version" src="https://img.shields.io/badge/Kotlin_Version-2.4.20-purple">](https://kotlinlang.org/)
[<img alt="Android SDK" src="https://img.shields.io/badge/Android_SDK-37-green">](https://developer.android.com/)
[<img alt="Min SDK" src="https://img.shields.io/badge/Min_SDK-26-informational">](https://developer.android.com/)
[<img alt="Gradle" src="https://img.shields.io/badge/Gradle-9.8.0-blue">](https://gradle.org/)
[<img alt="AGP" src="https://img.shields.io/badge/AGP-9.4.1-green">](https://developer.android.com/tools/releases/gradle-plugin)
<!-- END_VERSIONS -->
```

## Реализация

**Расположение:** `build.gradle.kts` (корневой файл проекта)

**Функционал:**
- Чтение версий из файлов проекта
- Генерация markdown-блока с бейджами
- Обновление README.md с обработкой ошибок
- Логирование операций

**Статус:** ✅ Реализовано
