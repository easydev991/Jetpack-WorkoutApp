# Универсальный механизм обработки ошибок

## Расположение в коде

| Файл                       | Назначение                                                 |
|----------------------------|------------------------------------------------------------|
| `util/AppError.kt`         | sealed класс ошибок (Network, Validation, Server, Generic) |
| `util/AppErrorExt.kt`      | extension `toUiText()` для локализации                     |
| `util/UserNotifier.kt`     | Интерфейс с `errorFlow: SharedFlow<AppError>`              |
| `util/UserNotifierImpl.kt` | Реализация (buffer=10, DROP_OLDEST)                        |
| `util/AppNotification.kt`  | sealed класс уведомлений (Info)                            |
| `ui/screens/RootScreen.kt` | Сбор ошибок через `LaunchedEffect`, показ Snackbar         |

## Реализовано

### Основные компоненты

- ✅ `AppError` — модель ошибок (Network, Validation, Server, Generic)
- ✅ `AppNotification` — модель уведомлений (Info)
- ✅ `UserNotifier` + `UserNotifierImpl` — SharedFlow для отправки ошибок и уведомлений
- ✅ DI в `AppContainer`, все ViewModels обновлены
- ✅ Snackbar в `RootScreen` через `LaunchedEffect`
- ✅ Локализация сообщений через `toUiText(context)`

### Локализация

- ✅ Английский язык (values/strings.xml)
- ✅ Русский язык (values-ru/strings.xml)
- ✅ Поддержка полей для Validation ошибок (email, password)
- ✅ Поддержка HTTP кодов для Server ошибок (401, 403, 404, 500, 503)

### Тестирование

- ✅ Unit-тесты `AppErrorTest` (12 тестов)
- ✅ Unit-тесты `UserNotifierImplTest` (10 тестов)
- ✅ Инструментальные тесты `RootScreenTest` (4 теста)
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

### Аналитика ошибок

- Интеграция с Firebase Analytics/Crashlytics
