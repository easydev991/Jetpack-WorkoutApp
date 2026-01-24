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

- [x] Создан `AppConstants.kt` с константами (APP_SHARE_URL, APP_RATE_URL, GITHUB_REPOSITORY_URL, DAYS_COUNTER_APP_STORE_URL)
- [x] Добавлены все необходимые строковые ресурсы (settings, app_theme_and_icon, share_the_app, github_page и др.)
- [x] Исправлены предупреждения detekt: созданы `FeedbackSender.kt` и `ExternalLinkRow`

---

## Этап 2: Domain Layer - Модели данных ✅

- [x] Создан `AppTheme.kt` enum (LIGHT, DARK, SYSTEM)
- [x] Создан `AppIcon.kt` enum с 11 иконками и методом `getComponentName()` (без учета темной темы)

---

## Этап 3: Data Layer - Хранение настроек ✅

- [x] Создан `AppSettingsDataStore.kt` с DataStore Preferences для персистентного хранения настроек
- [x] Реализованы Flow для theme, useDynamicColors, icon и методы setTheme(), setUseDynamicColors(), setIcon()
- [x] Добавлен factory метод createAppSettingsDataStore() для ручного DI

---

## Этап 4: Domain Layer - Use Case для смены иконки ✅

- [x] Создан `IconManager.kt` use case с методом `changeIcon(icon: AppIcon)`
- [x] Реализована смена иконки через PackageManager и activity aliases (без учета темной темы)
- [x] Добавлена обработка исключений (SecurityException, NameNotFoundException, IllegalArgumentException) с логированием на русском языке

---

## Этап 5: UI Layer - State management ✅

- [x] Создан `ThemeIconUiState.kt` data class (theme, useDynamicColors, icon, isLoading)
- [x] Создан `ThemeIconViewModel.kt` с StateFlow uiState, объединяющим настройки из DataStore
- [x] Реализованы методы updateTheme(), updateDynamicColors(), updateIcon() с логированием на русском языке
- [x] Добавлен factory метод для ручного DI и обработка исключений в updateIcon()

---

## Этап 6: UI Layer - Компоненты для ThemeIconScreen ✅

### 6.1 Создать компонент DynamicColors

- [x] Создать файл `app/src/main/java/com/swparks/ui/theme/DynamicColors.kt`
- [x] Реализовать метод `isDynamicColorAvailable()` для проверки Android 12+
- [x] Использовать `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S`

### 6.2 Создать компонент IconPreviewItem

- [x] Создать файл `app/src/main/java/com/swparks/ui/screen/components/themeicon/IconPreviewItem.kt`
- [x] Принимать параметры: `appIcon`, `isSelected`, `onClick`
- [x] Отображать превью иконки приложения
- [x] Показывать индикатор выбранной иконки

### 6.3 Создать компонент DaysRadioButton

- [x] Создать файл `app/src/main/java/com/swparks/ui/screen/components/common/DaysRadioButton.kt` (или переименовать)
- [x] Принимать параметры: `text`, `selected`, `onClick`, `onClickable`
- [x] Использовать RadioButton из Material3

---

## Этап 7: UI Layer - ThemeIconScreen ✅

### 7.1 Создать ThemeIconScreen

- [x] Создать файл `app/src/main/java/com/swparks/ui/screens/themeicon/ThemeIconScreen.kt`
- [x] Создать функции: `ThemeIconScreen(viewModel, onBackClick)`, `ThemeIconScreenContent(...)` для UI-тестов
- [x] Реализовать секции: `ThemeSection` (radio buttons с DaysRadioButton), `DynamicColorsSection` (switch, Android 12+), `IconSection` (с сеткой иконок)
- [x] Использовать FlowRow в `IconGrid` для адаптивной сетки иконок
- [x] Добавить кнопку назад в TopAppBar
- [x] Использовать компоненты: `DaysRadioButton`, `IconPreviewItem`, `DynamicColors`

---

## Этап 8: Доработка MoreScreen

### 8.1 Создать секцию "Настройки"

- [x] Создать `SettingsSection` с кнопкой "Тема и иконка приложения"
- [x] Добавить `ThemeAndIconRow` с навигацией на ThemeIconScreen
- [x] Вставить `SettingsSection` первой в `ScreenContent` перед `AboutAppSection`

### 8.2 Доработать секцию "О приложении"

- [x] Удалить кнопку "Правила использования" (TermsOfUseRow)
- [x] Добавить кнопку "Поделиться приложением" (ShareAppRow) после "Разработчик приложения", перед "Версия"

### 8.3 Создать секцию "Другие приложения"

- [x] Создать `OtherAppsSection` после `AboutAppSection`
- [x] Добавить кнопку "Счётчик дней" (DaysCounterRow) со ссылкой на App Store
- [x] Добавить HorizontalDivider после секции

### 8.4 Дополнить секцию "Поддержать проект"

- [x] Добавить кнопку "GitHub page" (GithubRow) после "Магазин WORKOUT"

### 8.5 Реализовать обработчики кликов

**Примечание:** В JetpackDays реализованы отдельные функции для шеринга и GitHub - использовать аналогичный подход:

**Функция `shareApp(context)` (аналог JetpackDays):**

- [x] Использовать `Intent.ACTION_SEND` с типом `"text/plain"`
- [x] Добавлять текст шеринга через строковый ресурс `share_text` с названием приложения
- [x] Использовать `AppConstants.APP_SHARE_URL` для ссылки
- [x] Отображать chooser с `share_chooser_title`
- [x] Обрабатывать `ActivityNotFoundException` с логированием ошибки

**Функция `openGitHub(context)` (аналог JetpackDays):**

- [x] Использовать `Intent.ACTION_VIEW` с `Uri.parse(AppConstants.GITHUB_REPOSITORY_URL)`
- [x] Обрабатывать `ActivityNotFoundException` с логированием ошибки

**Сравнение с текущей реализацией:**

- Текущая реализация использует объект `Links` и прямой вызов `uriHandler.openUri()`
- Подход из JetpackDays с Intent более гибкий и позволяет обрабатывать исключения

**Задачи:**

- [x] Обновить кнопку "Оценить приложение" для использования `APP_RATE_URL` из AppConstants
- [x] Реализовать функцию `shareApp(context)` для шеринга приложения (аналог JetpackDays)
- [x] Реализовать функцию `openGitHub(context)` для открытия GitHub (аналог JetpackDays)
- [x] Создать компоненты: `ShareAppRow`, `DaysCounterRow`, `GithubRow`
- [x] Реализовать навигацию на ThemeIconScreen из ThemeAndIconRow (которая была создана в Этапе 8.1)
- [x] Обновить RootScreen или NavController для поддержки маршрута ThemeIcon

**Примечание по `sendFeedback`:**

- Текущая реализация `didTapSendFeedback` (73 строки) работает корректно
- JetpackDays использует отдельную функцию `sendFeedback` с динамическим получением названия приложения
- **Решение:** Оставить текущую реализацию как есть (работает корректно), но вынести её в отдельный класс `FeedbackSender.kt` в рамках решения предупреждения detekt (Этап 1.3)

### 8.6 Интеграция с навигацией

- [x] Добавить маршрут для ThemeIconScreen в `navigation/Screen.kt`
- [x] Реализовать навигацию на ThemeIconScreen из ThemeAndIconRow (которая была создана в Этапе 8.1)
- [x] Обновить RootScreen или NavController для поддержки маршрута ThemeIcon

**Примечание по реализациям shareApp и openGitHub:**

- В swparks кнопки GitHub, Оценить приложение, Магазин WORKOUT уже реализованы через прямые вызовы `uriHandler.openUri(Links.XXX)`
- Это допустимый подход, но JetpackDays использует Intent-подход с функциями `shareApp()` и `openGitHub()`
- **Аналогичные функции для шеринга и GitHub были реализованы при создании секций "Другие приложения" и "Поддержать проект" (см. Этап 8.5)**
- Это позволяет обрабатывать исключения (ActivityNotFoundException) более гибко, как в JetpackDays
- Отправка обратной связи через Intent (JetpackDays) уже реализована в swparks в функции `didTapSendFeedback()` - менять не требуется

### 8.7 Порядок кнопок в секциях

**Секция "Настройки":** Тема и иконка приложения

**Секция "О приложении":** Отправить обратную связь, Оценить приложение, Официальный сайт, Разработчик приложения, Поделиться приложением, Версия

**Секция "Другие приложения":** Счётчик дней

**Секция "Поддержать проект":** Магазин WORKOUT, GitHub page

---

## Этап 9: Настройка иконок приложения

### 9.1 Создать activity aliases для иконок

- [ ] В `AndroidManifest.xml` добавить activity-aliases для каждой иконки
- [ ] Настроить enabled/disabled logic для иконок

### 9.2 Добавить ресурсы иконок

- [ ] Добавить PNG иконки в `mipmap-*/ic_launcher_*` для каждого варианта
- [ ] Добавить adaptive icons для Android 8+
- [ ] Убедиться в наличии иконок для всех размеров (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi)

---

## Этап 10: Тестирование

### 10.1 Unit-тесты

**Тестирование enum моделей настроек:**

- [ ] Создать файл `app/src/test/java/com/swparks/domain/model/SettingsModelsTest.kt`
- [ ] Написать тесты для `AppTheme` enum (проверить все значения: LIGHT, DARK, SYSTEM)
- [ ] Написать тесты для `AppIcon` enum (проверить все значения иконок SWParks)
- [ ] Тестировать `valueOf`, `name`, выброс исключений

**Тестирование ThemeIconViewModel:**

- [ ] Создать файл `app/src/test/java/com/swparks/viewmodel/ThemeIconViewModelTest.kt`
- [ ] Использовать MockK для мокирования зависимостей (AppSettingsDataStore, IconManager, Logger)
- [ ] Тестировать `updateTheme`, `updateIcon`, `updateDynamicColors`
- [ ] Проверять, что методы ViewModel вызывают соответствующие методы DataStore и IconManager
- [ ] Использовать JUnit 5 аннотации и корутины

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

- ✅ Все кнопки отображаются на MoreScreen
- ✅ Кнопки "Поделиться приложением", "GitHub page", "Оценить приложение" работают корректно
- ✅ ThemeIconScreen открывается и работает
- ✅ Тема приложения переключается и сохраняется
- ✅ Иконка приложения меняется и сохраняется
- ✅ Динамические цвета работают на Android 12+
- ✅ Все настройки персистентны после перезапуска
- ✅ Все тесты проходят (unit и UI)
- ✅ Проект собирается без ошибок
- ✅ Код отформатирован (`make format`)
- ✅ Нет замечаний от linting tools
