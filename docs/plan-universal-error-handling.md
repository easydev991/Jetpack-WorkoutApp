# Универсальный механизм обработки ошибок

## Расположение в коде

| Файл | Назначение |
|------|------------|
| `util/AppError.kt` | sealed класс ошибок (Network, Validation, Server, Generic) |
| `util/AppErrorExt.kt` | extension `toUiText()` для локализации |
| `util/UserNotifier.kt` | Интерфейс с `errorFlow: SharedFlow<AppError>` |
| `util/UserNotifierImpl.kt` | Реализация (buffer=10, DROP_OLDEST) |
| `ui/screens/RootScreen.kt` | Сбор ошибок через `LaunchedEffect`, показ Snackbar |

## Реализовано

- ✅ `AppError` — модель ошибок (Network, Validation, Server, Generic)
- ✅ `UserNotifier` + `UserNotifierImpl` — SharedFlow для отправки ошибок
- ✅ DI в `AppContainer`, все ViewModels обновлены
- ✅ Snackbar в `RootScreen` через `LaunchedEffect`
- ✅ Локализация сообщений через `toUiText(context)`
- ✅ Unit-тесты (59), инструментальные тесты (4)

---

## Невыполненные задачи

### Этап 9: Тестирование на устройстве

- [ ] Тест сценариев с ошибками сети (без интернета)
- [ ] Проверка буфера ошибок (DROP_OLDEST)
- [ ] Проверка локализации (английский/русский)

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
