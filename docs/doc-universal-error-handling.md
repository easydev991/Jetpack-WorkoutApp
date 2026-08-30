# Универсальный механизм обработки ошибок

## Расположение в коде

| Файл                       | Назначение                                                 |
|----------------------------|------------------------------------------------------------|
| `util/AppError.kt`         | sealed класс ошибок (Network, Validation, Server, Generic, LocationFailed, LocationDisabled, GeocodingFailed, ResourceNotFound) |
| `util/AppErrorExt.kt`      | extension `toUiText()` для локализации (используется в `RootScreen` и `RegisterSheetHost`) |
| `util/UserNotifier.kt`     | Класс с `errorFlow: SharedFlow<AppError>` и `notificationFlow` |
| `util/AppNotification.kt`  | sealed класс уведомлений (Info)                            |
| `ui/screens/RootScreen.kt` | Сбор ошибок через `LaunchedEffect`, показ Snackbar         |

## Реализовано

### Основные компоненты

- ✅ `AppError` — модель ошибок (Network, Validation, Server, Generic, LocationFailed, LocationDisabled, GeocodingFailed, ResourceNotFound)
- ✅ `AppNotification` — модель уведомлений (Info)
- ✅ `UserNotifier` — SharedFlow для отправки ошибок и уведомлений
- ✅ DI в `AppContainer`, все ViewModels обновлены
- ✅ Snackbar в `RootScreen` через `LaunchedEffect`
- ✅ Локализация сообщений через `toUiText(context)`

### Локализация

- ✅ Английский язык (values/strings.xml)
- ✅ Русский язык (values-ru/strings.xml)
- ✅ Поддержка полей для Validation ошибок (email, password)
- ✅ Поддержка HTTP кодов для Server ошибок (401, 403, 404, 500, 503)
- ✅ Локализация LocationFailed/LocationDisabled/GeocodingFailed/ResourceNotFound

### Аналитика ошибок

- ✅ `AnalyticsEvent.AppError` — событие аналитики (операция + throwable)
- ✅ `analytics/FirebaseAnalyticsProvider` — отправка `app_error` в Firebase Analytics
- ✅ `util/crash/FirebaseCrashReporter` — логирование исключений в Crashlytics
- ✅ ViewModels и репозитории логируют ошибки через `AnalyticsService.log(AnalyticsEvent.AppError(...))`; `UserNotifier` сам в аналитику не отправляет

### Тестирование

- ✅ Unit-тесты `AppErrorTest`
- ✅ Unit-тесты `UserNotifierTest`
- ✅ Инструментальные тесты `RootScreenTest`
  - Network error с IOException
  - Validation error (password)
  - Server error (500)
  - Generic error

### Буферизация

- ✅ Буфер ошибок: 10 элементов
- ✅ Буфер уведомлений: 10 элементов
- ✅ Стратегия переполнения: DROP_OLDEST

---

## Статус

**✅ Завершено**

Все основные задачи реализованы и протестированы. Механизм готов к использованию в production.

---

## Будущие улучшения

### AlertDialog для критических ошибок

- Snackbar для Network/Validation
- AlertDialog для Server/Generic

### Кнопка "Повторить" в Snackbar

- Добавить `retryAction` в `AppError.Network`

### Кэш истории ошибок

- Хранить последние 100 ошибок для отладки
