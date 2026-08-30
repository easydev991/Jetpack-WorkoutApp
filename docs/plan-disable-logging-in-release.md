# План: Отключение логирования в release-сборках

## Выполнено

- ✅ **AppContainer.kt:135** — `logger` условный (`BuildConfig.DEBUG ? AndroidLogger : NoOpLogger`)
- ✅ **AppContainer.kt:313-314** — `LoggingInterceptor` добавляется только в DEBUG
- ✅ **JetpackWorkoutApplication** — `logger` удалён, используется DI из `container.logger`
- ✅ `private val loggingInterceptor` — удалён

> **FirebaseCrashReporter** оставлен без изменений (нужен для Crashlytics).

---

## Этап 3: Проверка

- [ ] **3.1** Запустить `make lint` — убедиться что нет предупреждений
- [ ] **3.2** Собрать debug: `./gradlew assembleDebug` — убедиться что все `logger.d/i/w/e` работают
- [ ] **3.3** Собрать release: `./gradlew assembleRelease` — убедиться что логов нет в logcat
- [ ] **3.4** Проверить что Crashlytics по-прежнему работает (crash в release передаёт информацию)

---

## Итоговая структура изменений

| Файл | Изменение |
|------|-----------|
| `app/src/main/java/com/swparks/data/AppContainer.kt` | Условный `logger` + условный `LoggingInterceptor` |
| `app/src/main/java/com/swparks/JetpackWorkoutApplication.kt` | `logger` удалён (DI) |

---

## Что не нужно менять

- `logger.d/i/w/e()` в ViewModels — работают через DI (release: `NoOpLogger`)
- `Logger`, `AndroidLogger`, `NoOpLogger`, `FirebaseCrashReporter`

---

## Дополнительная оптимизация: тяжёлые лог-вызовы

Текущая реализация (`NoOpLogger` в release) уже отключает реальные вызовы `logger.d/i/w/e` на уровне DI. Однако даже с `NoOpLogger` строка может собираться заранее, поэтому точечная обёртка `if (BuildConfig.DEBUG)` в самых тяжёлых местах будет чуть эффективнее.

### Места с потенциально тяжёлыми объектами

> **Актуализация:** `SWRepository` разделён на специализированные репозитории; общая обработка ошибок HTTP теперь в `BaseRepository.kt`.

**1. `BaseRepository.kt:48-58` (`handleHttpException`)** — чтение и десериализация сырого тела HTTP-ошибки:

```kotlin
val responseBody = e.response()?.errorBody()?.string()
val errorResponse = json.decodeFromString<ErrorResponse>(responseBody)
```

Аналогично: `BaseRepository.kt:75-83` (`handleResponseError`) и `BaseRepository.kt:97-98`.

**2. `BaseRepository.kt:58, 83`** — здесь заранее собирается строка с сообщением об ошибке десериализации, что тоже создаёт лишнюю работу в release:

```kotlin
logger.e(tag, "Не удалось десериализовать ответ об ошибке: ${se.message}")
```

**3. `LoggingInterceptor.kt:25-63`** — `buildString` для всего request/response, включая тела

```kotlin
val requestLog = buildString { ... append("\n📦 Body: ${buffer.readString(charset)}") }
```

Уже отключён в release через условное добавление в `AppContainer`.

### Часто вызываемые места с `logger.d`

**`ParksRootViewModel.kt`** — множество `logger.d` на UI-события:
- `onLocalFilterChange`, `onFilterToggleSize`, `onFilterToggleType`, `onCitySearchQueryChange`, `onTabSelected`, `onMapEvent` и др.
- Вызываются часто (каждое взаимодействие пользователя с фильтром/картой)

**`ParkDetailViewModel.kt`** — логирование загрузки, действий пользователя

### Рекомендация

Не рефакторить массово. При желании — точечно обернуть в `if (BuildConfig.DEBUG)` только самые тяжёлые места, где собираются большие строки и response body:

```kotlin
if (BuildConfig.DEBUG) {
    logger.e(TAG, "Тело ответа сервера: $responseBody")
}
```

Текущая реализация через `NoOpLogger` уже достаточна — в release сами методы `logger.*` ничего не делают, но часть работы по созданию аргументов сообщений может происходить до вызова.
