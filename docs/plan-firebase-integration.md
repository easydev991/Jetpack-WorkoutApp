# Интеграция Firebase в SW Parks

## Обзор

Интеграция Firebase на основе паттернов из проекта JetpackDays (`com.dayscounter`).
Реализуется **Crashlytics** (краш-репорты) для отслеживания крашей после релиза.

---

## Этап 1: Настройка Firebase (build system)

- [x] Firebase-проект создан, `google-services.json` добавлен в `.gitignore`
- [x] Настроены зависимости в `gradle/libs.versions.toml` (firebase-bom, google-services, firebase-crashlytics-gradle)
- [x] Плагины `com.google.gms.google-services` и `com.google.firebase.crashlytics` применены в `build.gradle.kts`
- [x] Build types: debug — Crashlytics отключён, release — включён с minification
- [x] Добавлен `<meta-data>` в `AndroidManifest.xml` и ProGuard-правила в `proguard-rules.pro`

---

## Этап 2: CrashlyticsHelper (TDD)

- [x] Реализованы `CrashReporter` интерфейс, `FirebaseCrashReporter` (object singleton, fail-safe) и `NoOpCrashReporter`
- [x] Тесты для `NoOpCrashReporter` покрывают `logException`, `setUserId`, `setCustomKey`
- [x] `CrashReporter` зарегистрирован в `AppContainer`

---

## Этап 3: Интеграция Crashlytics в репозитории

- [x] `CrashReporter` добавлен в `DefaultAppContainer` и репозитории
- [x] `userId` устанавливается при авторизации, сбрасывается при логауте

---

## Этап 4: Финализация и валидация

- [x] `make format` / `make lint` / `make test` — все проходят
- [x] Debug-сборка: Crashlytics отключён, `google-services.json` в `.gitignore`
- [ ] Ручное тестирование — вызвать тестовый crash, проверить в Firebase Console
- [ ] Собрать release APK и проверить mapping file upload
- [ ] Оценить прирост размера APK (< 200 KB)

---

## Правила проекта

- Логи на русском, `try/catch` обёртка для Firebase (fail-safe), без PII
- `Result<T>` сохраняется, ручной DI через `AppContainer`, TDD

## Важные замечания

1. **Google Play Services** — Crashlytics требует GMS (Huawei-устройства могут не получать краш-логи)
2. **CI/CD** — `google-services.json` хранить в secrets

## Структура файлов

```
app/src/main/java/com/swparks/util/crash/
  CrashReporter.kt           # interface
  FirebaseCrashReporter.kt   # object singleton
  NoOpCrashReporter.kt       # для тестов
app/src/test/java/com/swparks/util/crash/
  CrashReporterTest.kt
```

## Ссылки

- Референс: JetpackDays (`/Users/Oleg991/Documents/GitHub/JetpackDays`)
- Firebase BOM: `34.11.0`
