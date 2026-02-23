# Исправление навигации SearchUserScreen - сохранение активной вкладки

## Проблема

Экран `SearchUserScreen` открывается из двух мест: `ProfileRootScreen` и `MessagesRootScreen`. При открытии из Profile вкладка bottom navigation некорректно переключается на Messages, хотя должна оставаться Profile.

**Корневая причина:** В `Destinations.kt` экран `UserSearch` имеет фиксированный `parentTab = Messages`, поэтому система всегда считает, что мы находимся во вкладке Messages.

## Этап 1: Модификация модели навигации (Destinations.kt)

- [ ] Добавить параметр `source` в маршрут `UserSearch` для определения исходной вкладки
- [ ] Обновить `createRoute()` метод для приема параметра source (profile/messages)
- [ ] Убрать фиксированный `parentTab = Messages` или сделать его динамическим
- [ ] Обновить `findParentTab()` для учета параметра source из маршрута

**Детали реализации:**
- Маршрут должен стать: `user_search?source={source}`
- Значения source: `"profile"` | `"messages"`
- Метод `createRoute(source: String)` должен формировать URL с query параметром

## Этап 2: Обновление AppState для динамического определения вкладки

- [ ] Модифицировать `onDestinationChanged()` для парсинга query параметра `source` из маршрута
- [ ] При маршруте `user_search` определять parentTab на основе значения source
- [ ] Сохранить обратную совместимость для случаев без source параметра

**Детали реализации:**
- Использовать `Uri.parse(route)` для извлечения query параметров
- Если source=profile → parentTab = Profile
- Если source=messages или отсутствует → parentTab = Messages

## Этап 3: Обновление точек навигации

- [ ] В `ProfileRootScreen` изменить вызов навигации на `UserSearch.createRoute("profile")`
- [ ] В `MessagesRootScreen` изменить вызов навигации на `UserSearch.createRoute("messages")`

**Детали реализации:**
- Найти места вызова `navController.navigate(Screen.UserSearch.route)`
- Заменить на использование `createRoute()` с соответствующим параметром

## Этап 4: Обновление RootScreen (NavHost)

- [ ] Обновить определение composable для `UserSearch` для поддержки query параметров
- [ ] Убедиться, что маршрут с query параметрами правильно регистрируется в NavHost

**Детали реализации:**
- Маршрут в composable должен учитывать query параметры: `user_search?source={source}`
- Значение по умолчанию для source: `"messages"` для обратной совместимости

## Этап 5: Тестирование

- [ ] Открыть SearchUserScreen из Profile и проверить, что активная вкладка остается Profile
- [ ] Открыть SearchUserScreen из Messages и проверить, что активная вкладка Messages
- [ ] Проверить нажатие на BottomNavigation во время открытого SearchUserScreen
- [ ] Проверить возврат назад (back navigation) корректно работает
- [ ] Проверить deep links (если используются)

## Критерии завершения

- [ ] При открытии SearchUserScreen из Profile вкладка bottom navigation остается Profile
- [ ] При открытии SearchUserScreen из Messages вкладка bottom navigation остается Messages
- [ ] BottomNavigationBar корректно отображает активную вкладку в обоих случаях
- [ ] Переходы между вкладками работают корректно
- [ ] Код соответствует style guide проекта (ktlint, detekt)

## Файлы для изменения

1. `app/src/main/java/com/swparks/navigation/Destinations.kt` - добавление параметра source
2. `app/src/main/java/com/swparks/navigation/AppState.kt` - динамическое определение parentTab
3. `app/src/main/java/com/swparks/ui/screens/RootScreen.kt` - обновление маршрута в NavHost
4. `app/src/main/java/com/swparks/ui/screens/profile/ProfileRootScreen.kt` - обновление вызова навигации
5. `app/src/main/java/com/swparks/ui/screens/messages/MessagesRootScreen.kt` - обновление вызова навигации (опционально, для явности)
