# Исправление предупреждения о deprecated statusBarColor

## Анализ проблемы

В файле `app/src/main/java/com/swparks/ui/theme/Theme.kt:122` используется устаревшее свойство `statusBarColor`:

```kotlin
window.statusBarColor = Color.Transparent.toArgb()
```

Этот API deprecated в Android API 35. Современный подход — использование edge-to-edge дисплея.

## План исправления

### 1: Об Этап новление Theme.kt

- [ ] Удалить строку с устаревшим `window.statusBarColor = Color.Transparent.toArgb()`
- [ ] Добавить версионную проверку для edge-to-edge:
  - Для API 30+ использовать `WindowCompat.setDecorFitsSystemWindows(window, false)`
  - Для API 26-29 оставить `window.statusBarColor = Color.Transparent.toArgb()` с аннотацией `@Suppress("DEPRECATION")`
- [ ] Сохранить логику `WindowCompat.getInsetsController` для управления appearance статус бара (уже реализовано)

### Этап 2: Проверка MainActivity

- [ ] Проверить `MainActivity.kt` — убедиться, что используется корректная обработка WindowInsets
- [ ] При необходимости добавить версионную логику:
  - Для API 32+ использовать `enableEdgeToEdge()`
  - Для API 26-29 использовать традиционный подход

### Этап 3: Тестирование

- [ ] Запустить приложение и проверить отображение статус бара
- [ ] Убедиться, что контент не перекрывается системными UI
- [ ] Проверить на устройствах с Android 8.0+ (minSdk 26)

## Примечание

Для Android 8.0-10 (API 26-29):
- Сохраняем `statusBarColor` с `@Suppress("DEPRECATION")` — предупреждение будет подавлено, код работает корректно

Для Android 11-12 (API 30-32):
- Используем `WindowCompat.setDecorFitsSystemWindows(window, false)` — edge-to-edge без устаревших API

Для Android 13+ (API 33+):
- Рекомендуется использовать `enableEdgeToEdge()` в Activity
