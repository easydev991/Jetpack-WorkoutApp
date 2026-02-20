# Рефакторинг: Перенос генерации токена в Data Layer

## Цель

Перенести генерацию токена авторизации из `LoginCredentials` (Domain Layer) в отдельный класс в Data Layer для устранения зависимости от Android SDK в Domain Layer и улучшения чистоты архитектуры.

**Текущая проблема:**
- `LoginCredentials.token` использует `android.util.Base64` (Android SDK класс)
- Domain layer не должен зависеть от платформо-специфичных классов
- Unit-тесты требуют настройку `isReturnDefaultValues = true` для работы

**Решение:**
- Создать `TokenEncoder` в data layer
- Перенести логику генерации токена из `LoginCredentials` в `TokenEncoder`
- Обновить `LoginUseCase` для использования `TokenEncoder`
- Удалить свойство `token` из `LoginCredentials`

---

## ✅ Завершенная работа

### Этап 1: Создание TokenEncoder ✅

- Создан класс `TokenEncoder` в data layer для генерации Base64 токена
- Реализованы модульные тесты (7 тестов)
- Используется `java.util.Base64` для кроссплатформенности

### Этап 2: Обновление Domain Layer ✅

- Удалено свойство `token` из `LoginCredentials`
- Все использования `credentials.token` найдены и заменены

### Этап 3: Обновление Data Layer ✅

- `LoginUseCase` использует `TokenEncoder` через DI
- `AppContainer` обновлен с созданием `TokenEncoder`
- Модульные тесты обновлены

### Этап 4: Обновление UI Layer ✅

- UI компоненты не используют напрямую `credentials.token`
- Тесты `LoginViewModel` обновлены

### Этап 5: Реализация Logger ✅

- Создан интерфейс `Logger` для абстракции логирования
- Реализованы `AndroidLogger` и `NoOpLogger`
- Все 18 тестов `LoginViewModelTest` проходят

### Этап 6: Финальная проверка ✅

- Все 489 тестов проекта проходят успешно
- Lint (ktlint) и detekt без новых ошибок
- Приложение собирается успешно
- Настройка `isReturnDefaultValues = true` отсутствует

---

## Критерии успеха

✅ Свойство `token` удалено из `LoginCredentials`
✅ `TokenEncoder` создан и протестирован в data layer
✅ `LoginUseCase` использует `TokenEncoder`
✅ Реализован интерфейс `Logger` для решения проблемы с `android.util.Log`
✅ Все модульные тесты проходят (489)
✅ Lint (ktlint) и detekt проходят без новых ошибок
✅ Приложение собирается успешно

---

## Потенциальные проблемы - все решены ✅

1. ✅ Credentials.token используется в других частях проекта - найдены и заменены
2. ✅ Тесты падают после удаления isReturnDefaultValues - решено через Logger
3. ✅ LoginViewModel напрямую использует android.util.Log - решено через Logger интерфейс
4. ✅ DI контейнер не найден - найден AppContainer.kt и обновлен

---

## Объем работы

- **Время:** ~3 часа
- **Изменено файлов:** ~11
- **Сложность:** Средняя

---

## Следующие улучшения (опционально)

1. Создать интерфейс `ITokenEncoder` для улучшения тестируемости
2. Добавить логирование в `TokenEncoder` для отладки
3. Добавить unit тесты для edge cases в `TokenEncoder` (null credentials, unicode символы)
4. Создать документацию по использованию `TokenEncoder` и `Logger` в `.cursor/rules/`
5. Рассмотреть использование Logger в других ViewModels для консистентности
6. Добавить тесты для других ViewModels, если они используют `android.util.Log` или `android.os.Bundle`
