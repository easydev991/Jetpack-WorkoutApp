# План реализации навигации в Jetpack-WorkoutApp

## Введение

Этот план описывает реализацию навигации в приложении Jetpack-WorkoutApp на основе официального примера Now in Android (android/nowinandroid).

## Основные принципы из Now in Android

### Реализованные принципы

- **Единый NavHost** в приложении вместо множественных
- **AppState** для управления состоянием навигации
- **Centralized Destinations** через sealed class Screen
- **Navigation Options** для вкладок (popUpTo, saveState, launchSingleTop, restoreState)

## Реализованные этапы

### Этап 1: Создание структуры навигации

- ✅ `TopLevelDestination.kt` - data class для верхнеуровневых назначений
- ✅ `Destinations.kt` - sealed class Screen со всеми маршрутами
- ✅ `AppState.kt` - класс для управления состоянием навигации
- ✅ `Navigation.kt` - BottomNavigationBar
- ✅ Строковые ресурсы для заголовков экранов

### Этап 2: Рефакторинг RootScreen

- ✅ Обновлен RootScreen.kt с использованием AppState и единого NavHost
- ✅ Созданы заглушки: MessagesRootScreen.kt, ProfileRootScreen.kt

### Этап 3: Обновление иконок

- ✅ Иконки обновлены в TopLevelDestinations (Icons.Filled.*и Icons.Outlined.*)

### Этап 4: Тестирование

- ✅ Переключение между вкладками работает корректно
- ✅ Сохранение состояния при переключении вкладок работает
- ✅ Навигация на детальные экраны реализована

### Этап 5: Проверка качества кода

- ✅ Форматирование (ktlintFormat)
- ✅ Проверка линтеров (ktlintCheck, detekt)

## Актуальные файлы навигации

- `TopLevelDestination.kt` - data class для верхнеуровневых назначений (вкладок)
- `Destinations.kt` - sealed class Screen со всеми маршрутами навигации
- `AppState.kt` - класс для управления состоянием навигации
- `Navigation.kt` - BottomNavigationBar для отображения вкладок
- `RootScreen.kt` - корневой экран с единым NavHost

## План дальнейших задач

1. Реализация детальных экранов (ParkDetailScreen, EventDetailScreen и т.д.)
2. Добавление маршрутов с параметрами
3. Интеграция с Firebase Analytics (при необходимости)
4. Реализация авторизации и сценариев для неавторизованных пользователей
5. Добавление глубокой навигации (deep linking)
6. Оптимизация производительности навигации

## Анализ архитектурного подхода

Jetpack WorkoutApp частично следует архитектурному подходу Now in Android, особенно в части использования Jetpack Compose, Kotlin Flows и разделения на слои.
