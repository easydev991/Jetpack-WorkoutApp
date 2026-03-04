# План: Автоматическое обновление версий в README.md

## Описание задачи

Создать Gradle-таск, который автоматически обновляет раздел с версиями (Kotlin, Android SDK, Gradle, AGP) в README.md на основе данных из:
- `gradle/libs.versions.toml`
- `app/build.gradle.kts`
- `gradle/wrapper/gradle-wrapper.properties`

## Этап 0: Исправление текущих рассинхронизаций (опционально)

Перед автоматизацией исправить текущие несоответствия версий в README.md:

| Параметр | README.md | Файл | Корректное значение |
|----------|-----------|------|---------------------|
| Kotlin | 2.3.0 | libs.versions.toml | 2.3.10 |
| compileSdk | 35 | app/build.gradle.kts | 36 |
| Gradle | 9.2.1 | gradle-wrapper.properties | 9.3.1 |

- [ ] Обновить README.md с корректными версиями вручную

## Этап 1: Подготовка README.md

- [ ] Добавить маркеры `<!-- BEGIN_VERSIONS -->` и `<!-- END_VERSIONS -->` вокруг блока с бейджами в README.md
- [ ] Проверить, что бейджи соответствуют формату для последующей замены

**Ожидаемый результат:**
```markdown
<!-- BEGIN_VERSIONS -->
[<img alt="Kotlin Version" src="https://img.shields.io/badge/Kotlin_Version-...">](https://kotlinlang.org/)
...
<!-- END_VERSIONS -->
```

## Этап 2: Создание Gradle таска

- [ ] Создать файл `build.gradle.kts` в корне проекта (если отсутствует) или добавить таск в существующий
- [ ] Реализовать таск `updateReadmeVersions`:

  ### Использование Gradle API
  - [ ] Считать версии Kotlin и AGP из `gradle/libs.versions.toml` через `libs.versions.toml` (или парсить toml напрямую)
  - [ ] Считать SDK версии (compileSdk, targetSdk, minSdk) через Gradle API:
    - Использовать `project.extensions.getByType<com.android.build.gradle.BaseExtension>()` или
    - `project.afterEvaluate { project.android.defaultConfig... }`
  - [ ] Считать версию Gradle из `gradle/wrapper/gradle-wrapper.properties`

  ### Генерация и обновление
  - [ ] Сгенерировать markdown-блок с бейджами
  - [ ] Обновить README.md, заменив содержимое между маркерами

  ### Обработка ошибок и логирование
  - [ ] Обернуть операции чтения/записи в try/catch
  - [ ] Добавить логирование через `logger.lifecycle()` или `println()`
  - [ ] Выводить понятные сообщения об ошибках с указанием причины
  - [ ] Обработать случай отсутствия маркеров в README.md

- [ ] Проверить работу таска локально: `./gradlew updateReadmeVersions`

**Ожидаемый результат:** Таск успешно обновляет README.md при запуске

## Этап 3: Настройка pre-commit hook

- [ ] Создать директорию `.git/hooks/`
- [ ] Создать скрипт pre-commit hook (shell script)
- [ ] Настроить скрипт на запуск `./gradlew updateReadmeVersions` перед коммитом
- [ ] Сделать скрипт исполняемым (`chmod +x`)
- [ ] Добавить инструкцию по установке hook в CONTRIBUTING.md или создать отдельный скрипт установки

**Пример скрипта:**
```bash
#!/bin/sh
./gradlew updateReadmeVersions
```

**Ожидаемый результат:** При каждом коммите автоматически обновляются версии в README.md

## Этап 4: Документация и инструкции

- [ ] Обновить CONTRIBUTING.md с информацией о pre-commit hook
- [ ] Добавить информацию о таске в README.md (секция "Разработка" или "Contributing")
- [ ] Проверить, что таск работает при чистом клонировании репозитория

## Зависимости между этапами

| Этап | Зависит от |
|------|------------|
| Этап 1 | Этап 0 |
| Этап 2 | Этап 1 |
| Этап 3 | Этап 2 |
| Этап 4 | Этап 3 |

## Критерии завершения

- [ ] Таск `./gradlew updateReadmeVersions` работает без ошибок
- [ ] README.md корректно обновляется при запуске таска
- [ ] Pre-commit hook установлен и срабатывает перед коммитом
- [ ] Документация обновлена

## Текущие рассинхронизации версий

При запуске таска после реализации, должны быть автоматически исправлены:

| Параметр | Текущее в README | Будет после таска |
|----------|------------------|-------------------|
| Kotlin Version | 2.3.0 | 2.3.10 |
| Android SDK | 35 | 36 |
| Min SDK | 26 | 26 (без изменений) |
| Gradle | 9.2.1 | 9.3.1 |
| AGP | 9.1.0 | 9.1.0 (без изменений) |
