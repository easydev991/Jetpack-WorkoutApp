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
- Примечание: Использовать структуру JetpackDays - одна кнопка ведёт на экран ThemeIconScreen, где можно выбрать и тему, и иконку
- Примечание: "Язык приложения" отсутствует в плане (Android не поддерживает смену языка внутри приложения)

**Секция "О приложении" (добавить/изменить):**

- ❌ Поделиться приложением (shareAppButton) - нужно добавить после "Разработчик приложения", перед "Версия"
- ❌ Удалить кнопку "Правила использования" (в iOS её нет)
- ❌ Добавить кнопку "GitHub page" в секцию "Поддержать проект"

**Секция "Другие приложения" (нужно добавить):**

- Счётчик дней (аналог iOS: daysCounterButton) - ссылка на App Store

**Секция "Поддержать проект" (дополнить):**

- ❌ GitHub page (githubButton)

### Цель

Привести Android-версию к структуре iOS-версии с адаптацией под подход JetpackDays:

1. **Секция "Настройки"** (новая): Тема и иконка приложения (одна кнопка ведёт на ThemeIconScreen)
2. **Секция "О приложении"** (существующая, изменить): Отправить обратную связь, Оценить приложение, Официальный сайт, Разработчик приложения, Поделиться приложением, Версия
3. **Секция "Другие приложения"** (новая): Счётчик дней
4. **Секция "Поддержать проект"** (существующая, дополнить): Магазин WORKOUT, GitHub page

**Важно:** Экран ThemeIconScreen полностью аналогичен JetpackDays - одна кнопка на MoreScreen ведёт на экран с тремя секциями: тема, динамические цвета, иконка

---

## Этап 1: Создание вспомогательных констант и утилит ✅

- [x] Созданы `AppConstants.kt` с константами URL, строковые ресурсы, `FeedbackSender.kt`, `ExternalLinkRow`

---

## Этап 2: Domain Layer - Модели данных ✅

- [x] Созданы `AppTheme.kt` enum (LIGHT, DARK, SYSTEM) и `AppIcon.kt` enum с 11 иконками

---

## Этап 3: Data Layer - Хранение настроек ✅

- [x] Создан `AppSettingsDataStore.kt` с DataStore Preferences и Flow для theme, useDynamicColors, icon

---

## Этап 4: Domain Layer - Use Case для смены иконки ✅

- [x] Создан `IconManager.kt` use case для смены иконки через PackageManager с обработкой исключений

---

## Этап 5: UI Layer - State management ✅

- [x] Созданы `ThemeIconUiState.kt` и `ThemeIconViewModel.kt` с StateFlow для управления состоянием

---

## Этап 6: UI Layer - Компоненты для ThemeIconScreen ✅

- [x] Созданы компоненты: `DynamicColors.kt`, `IconPreviewItem.kt`, `DaysRadioButton.kt`

---

## Этап 7: UI Layer - ThemeIconScreen ✅

- [x] Создан `ThemeIconScreen.kt` с секциями: тема, динамические цвета, иконка (аналог JetpackDays)

---

## Этап 8: Доработка MoreScreen ✅

- [x] Добавлена секция "Настройки" с навигацией на ThemeIconScreen
- [x] Удалена кнопка "Правила использования", добавлены кнопки "Поделиться приложением", "Счётчик дней", "GitHub page"
- [x] Реализованы функции `shareApp()` и `openGitHub()` через Intent с обработкой исключений
- [x] Добавлены компоненты: `ShareAppRow`, `DaysCounterRow`, `GithubRow`
- [x] Интегрирован маршрут ThemeIconScreen в навигацию
- [x] Исправлена интеграция MainActivity с MainActivityViewModel (DataStore в onCreate, factory без IconManager)

**Порядок кнопок:**

- Настройки: Тема и иконка
- О приложении: Отправить обратную связь, Оценить приложение, Официальный сайт, Разработчик, Поделиться приложением, Версия
- Другие приложения: Счётчик дней
- Поддержать проект: Магазин WORKOUT, GitHub page

---

## Этап 9: Настройка иконок приложения

### 9.1 Создать activity aliases для иконок ✅

- [x] В `AndroidManifest.xml` добавить activity-aliases для каждой иконки
- [x] Настроить enabled/disabled logic для иконок

### 9.2 Добавить ресурсы иконок ✅

- [x] Добавить PNG иконки в `mipmap-*/ic_launcher_*` для каждого варианта
- [x] Добавить adaptive icons для Android 8+
- [x] Убедиться в наличии иконок для всех размеров (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi)

---

## Этап 10: Тестирование

### 10.1 Unit-тесты ✅

**Тестирование enum моделей настроек:**

- [x] Создать файл `app/src/test/java/com/swparks/domain/model/SettingsModelsTest.kt`
- [x] Написать тесты для `AppTheme` enum (проверить все значения: LIGHT, DARK, SYSTEM)
- [x] Написать тесты для `AppIcon` enum (проверить все значения иконок SWParks)
- [x] Тестировать `valueOf`, `name`, выброс исключений

**Тестирование ThemeIconViewModel:**

- [x] Создать файл `app/src/test/java/com/swparks/viewmodel/ThemeIconViewModelTest.kt`
- [x] Использовать MockK для мокирования зависимостей (AppSettingsDataStore, IconManager)
- [x] Тестировать `updateTheme`, `updateIcon`, `updateDynamicColors`
- [x] Проверять, что методы ViewModel вызывают соответствующие методы DataStore и IconManager
- [x] Использовать JUnit 5 аннотации и корутины

**Примечание:** ❌ Не писать unit-тесты для IconManager и AppSettingsDataStore (как в JetpackDays)

### 10.2 UI-тесты

**UI-тесты для ThemeIconScreen:**

- [ ] Создать файл `app/src/androidTest/java/com/swparks/ui/screen/ThemeIconScreenTest.kt`
- [ ] Использовать Compose Testing (`createComposeRule`, `onNodeWithText`, `assertIsDisplayed`)
- [ ] Тестировать: TopAppBar с кнопкой "Назад", секция "Тема приложения", секция "Динамические цвета", секция "Иконка приложения", клики по radio buttons и иконкам
- [ ] Использовать `ThemeIconScreenContent` (изолированный UI без ViewModel)

**UI-тесты для MoreScreen:**

- [ ] Создать файл `app/src/androidTest/java/com/swparks/ui/screen/MoreScreenTest.kt`
- [ ] Тестировать отображение кнопок: "Поделиться приложением", "GitHub page", "Оценить приложение", версия приложения
- [ ] Тестировать кнопки секций "Другие приложения": "Счётчик дней" с открытием RuStore
- [ ] Тестировать кнопки секций "Поддержать проект": "GitHub page" с открытием GitHub
- [ ] Использовать `moreScreen` (изолированный UI без ViewModel)

**Примечание:** ❌ Не писать тесты навигации между экранами

### 10.3 Ручное тестирование

- [ ] Проверить работу кнопки "Поделиться приложением" через Intent-подход (как в JetpackDays)
- [ ] Проверить открытие GitHub через Intent-подход (как в JetpackDays)
- [ ] Проверить открытие RuStore для оценки
- [ ] Проверить смену темы приложения (LIGHT, DARK, SYSTEM)
- [ ] Проверить смену иконки приложения
- [ ] Проверить динамические цвета на Android 12+
- [ ] Проверить персистентность настроек после перезапуска
- [ ] Проверить работу кнопки "Назад" в ThemeIconScreen

### 10.4 Критерии приемки тестов

- ✅ Все unit-тесты проходят (ThemeIconViewModelTest, SettingsModelsTest)
- ✅ Все UI-тесты проходят (ThemeIconScreenTest, MoreScreenTest)
- ✅ Тесты следуют AAA паттерну
- ✅ Описательные имена тестов на русском языке с `@DisplayName`
- ✅ Тесты независимы и быстрые
- ✅ Мокирование через MockK для зависимостей
- ✅ Использование JUnit 5 утверждений

---

## Референсы

### Файлы для копирования/адаптации из JetpackDays

**Константы:** `AppConstants.kt`

**Модели данных:** `AppTheme.kt`, `AppIcon.kt`

**Data Layer:** `AppSettingsDataStore.kt`

**Use Cases:** `IconManager.kt`

**UI State:** `ThemeIconUiState.kt`

**ViewModels:** `ThemeIconViewModel.kt`

**UI Components:** `ThemeIconScreen.kt`, `DaysRadioButton.kt`, `IconPreviewItem.kt`

**Тема:** `DynamicColors.kt`

**MoreScreen:** функции `shareApp()` и `openGitHub()` (аналог JetpackDays для секций "Другие приложения" и "Поддержать проект"), структура экрана с одной кнопкой "Тема и иконка"

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

### Отличия от JetpackDays

1. **ApplicationId**: `com.swparks` вместо `com.dayscounter`
2. **Иконки приложения**: Другой набор иконок для SWParks
3. **GitHub URL**: Репозиторий SWParks вместо Jetpack-Days
4. **URL для шеринга/оценки**: `https://apps.rustore.ru/app/com.swparks`

### Порядок разработки

Согласно правилам TDD проекта: Тесты → Логика → UI

---

## Критерии завершения

**Завершено (Этапы 1-8.7):**

- ✅ Все вспомогательные компоненты, модели, Data Layer, Domain Layer реализованы
- ✅ UI Layer: ThemeIconScreen, MoreScreen доработаны полностью
- ✅ Интеграция с навигацией и MainActivity выполнена
- ✅ Все кнопки работают (шеринг, GitHub, оценка, тема, динамические цвета)
- ✅ Проект собирается без ошибок и отформатирован

**Осталось (Этапы 9-10):**

- ❌ Настройка иконок в AndroidManifest.xml и ресурсы (Этап 9)
- ❌ Unit-тесты для моделей и ViewModel (Этап 10.1)
- ❌ UI-тесты для ThemeIconScreen и MoreScreen (Этап 10.2)
- ❌ Ручное тестирование (Этап 10.3)
- ❌ Смена иконки работает и сохраняется
- ❌ Тесты проходят успешно
- ❌ Нет замечаний от linting tools
