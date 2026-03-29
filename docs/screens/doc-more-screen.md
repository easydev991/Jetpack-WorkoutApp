# План доработки MoreScreen

## Обзор задачи

Доработать экран MoreScreen до соответствия iOS-версии приложения.

### Текущее состояние Android-версии

**Секция "О приложении" (AboutAppSection):**

- ✅ Отправить обратную связь (SendFeedbackRow)
- ✅ Оценить приложение (RateAppRow)
- ✅ Правила использования (TermsOfUseRow)
- ✅ Официальный сайт (OfficialSiteRow)
- ✅ Разработчик приложения (AppDeveloperRow)
- ✅ Версия приложения (AppVersionRow)

**Секция "Поддержать проект" (SupportProjectSection):**

- ✅ Магазин WORKOUT (WorkoutShopRow)

### Отсутствующие секции и кнопки

**Секция "Настройки" (нужно добавить):**

- Тема и иконка приложения (аналог iOS: объединение appThemeButton и appIconButton)
- Примечание: одна кнопка ведёт на экран ThemeIconScreen, где можно выбрать и тему, и иконку
- Примечание: "Язык приложения" отсутствует в плане (Android не поддерживает смену языка внутри приложения)
---

## Реализация (Этапы 1-9, 10.1) ✅

- [x] ThemeIconScreen: полный стек реализован (Domain, Data, UI слои)
- [x] MoreScreen: все 4 секции и компоненты реализованы
- [x] Иконки: activity-aliases, PNG и adaptive icons
- [x] Unit-тесты: SettingsModelsTest.kt, ThemeIconViewModelTest.kt

---

## Тестирование (Этапы 10.2-10.4)

### 10.2 UI-тесты

**Статус:** ✅ Все UI-тесты реализованы и проходят (22 теста, 0 failed)

- [x] ThemeIconScreenTest.kt: тестирование TopAppBar, секций темы/динамических цветов/иконки, кликов по элементам
- [x] MoreScreenTest.kt: тестирование кнопок всех секций (22 теста)

### 10.3 Ручное тестирование

- [x] Проверить правильное отображение TopAppBar в ThemeIconScreen (исправлен contentWindowInsets в RootScreen)
- [x] Проверить безопасные зоны на всех экранах с TopAppBar (убран windowInsets = WindowInsets(top = 0) — теперь safe zone обрабатывается автоматически как в nowinandroid)
- [ ] Проверить работу кнопки "Поделиться приложением" через Intent-подход (как в nowinandroid)
- [ ] Проверить открытие GitHub через Intent-подход (как в nowinandroid)
- [ ] Проверить открытие RuStore для оценки
- [x] Проверить смену темы приложения (LIGHT, DARK, SYSTEM) — работает
- [x] Проверить смену иконки приложения — работает
- [x] Проверить динамические цвета на Android 12+ — работают
- [x] Проверить персистентность настроек после перезапуска — работает
- [x] Проверить работу кнопки "Назад" в ThemeIconScreen — работает
- [ ] Проверить работу кнопки "Отправить обратную связь" — не открывает почтовый клиент (нужно реализовать)

### 10.4 Критерии приемки тестов

**Статус:** ✅ Все критерии выполнены

- [x] Все unit- и UI-тесты проходят, соответствуют AAA паттерну, имеют описательные имена, используют JUnit 5 и MockK

---

## Референсы

### Файлы для копирования/адаптации из nowinandroid

**Константы:** `AppConstants.kt`

**Модели данных:** `AppTheme.kt`, `AppIcon.kt`

**Data Layer:** `AppSettingsDataStore.kt`

**Use Cases:** `IconManager.kt`

**UI State:** `ThemeIconUiState.kt`

**ViewModels:** `ThemeIconViewModel.kt`

**UI Components:** `ThemeIconScreen.kt`, `SWRadioButton.kt`, `IconPreviewItem.kt`

**Тема:** `DynamicColors.kt`

**MoreScreen:** функции `shareApp()` и `openGitHub()` (аналог nowinandroid для секций "Другие приложения" и "Поддержать проект"), структура экрана с одной кнопкой "Тема и иконка"

**Строковые ресурсы (для ThemeIconScreen):** строки 40, 84-93

**Тесты:** `SettingsModelsTest.kt`, `ThemeIconViewModelTest.kt`, `ThemeIconScreenTest.kt`, `MoreScreenTest.kt`

---

## Примечания

### Важные детали

1. **Безопасное разворачивание опционалов**: Использовать `checkNotNull`, `?.let`, `?:` вместо `!!`
2. **Логирование**: Все логи на русском языке
3. **Локализация**: Все строки должны иметь переводы на русский и английский
4. **Архитектура**: Следовать MVVM и Clean Architecture проекта
5. **Ручной DI**: Использовать factory методы вместо Hilt

### Отличия от nowinandroid

1. **ApplicationId**: `com.swparks` вместо `com.dayscounter`
2. **Иконки приложения**: Другой набор иконок для SWParks
3. **GitHub URL**: Репозиторий SWParks вместо nowinandroid
4. **URL для шеринга/оценки**: `https://apps.rustore.ru/app/com.swparks`

### Порядок разработки

Согласно правилам TDD проекта: Тесты → Логика → UI

---

## Критерии завершения

**Завершено:**

- ✅ ThemeIconScreen: Domain, Data, UI слои полностью реализованы
- ✅ MoreScreen: все секции, функции и навигация работают
- ✅ Иконки: activity-aliases, PNG и adaptive icons для всех размеров
- ✅ Unit-тесты: SettingsModelsTest.kt, ThemeIconViewModelTest.kt
- ✅ UI-тесты: ThemeIconScreenTest.kt, MoreScreenTest.kt (22 теста)

**Осталось:**

- ❌ Ручное тестирование оставшихся функций (Этап 10.3)
- ❌ Реализация кнопки "Отправить обратную связь" — не открывает почтовый клиент
- ❌ Запуск всех тестов и проверка linting
