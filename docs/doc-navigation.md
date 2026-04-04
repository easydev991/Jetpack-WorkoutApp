# Навигация в Jetpack-WorkoutApp

## Обзор

В приложении используется один `NavHost` в `RootScreen`, а маршруты централизованно описаны в `navigation/Destinations.kt` через sealed class `Screen`.

`AppState` отвечает за:

- текущий `NavHostController`;
- "липкое" состояние активной верхнеуровневой вкладки;
- состояние авторизации (`currentUser`, `isAuthorized`);
- синхронизацию визуального состояния нижней навигации после auth-flow.

## Основные файлы

| Файл | Ответственность |
| --- | --- |
| `app/src/main/java/com/swparks/navigation/Destinations.kt` | Все route и логика `findParentTab()` |
| `app/src/main/java/com/swparks/navigation/AppState.kt` | Активная вкладка, auth-state, обработка `onDestinationChanged()` |
| `app/src/main/java/com/swparks/navigation/Navigation.kt` | `BottomNavigationBar` |
| `app/src/main/java/com/swparks/ui/screens/RootScreen.kt` | Единственный `NavHost`, подключение экранов и top-level переходов |

## Как определяется активная вкладка

`AppState.onDestinationChanged()` работает в два шага:

1. Если текущий route совпадает с top-level экраном, активная вкладка обновляется напрямую.
2. Если открыт дочерний экран, вкладка определяется через `Screen.findParentTab(route, arguments)`.

Для экранов с `source` активная вкладка вычисляется динамически. Это используется, например, для:

- `UserSearch`;
- `OtherUserProfile`;
- `UserFriends`;
- `JournalEntries`;
- экранов деталей park/event и связанных переходов.

## Подключённые маршруты в `RootScreen`

### Верхнеуровневые вкладки

- `Screen.Parks`
- `Screen.Events`
- `Screen.Messages`
- `Screen.Profile`
- `Screen.More`

### Дочерние маршруты площадок

- `Screen.ParkDetail`
- `Screen.CreatePark`
- `Screen.EditPark`
- `Screen.ParkTrainees`
- `Screen.CreateEventForPark`
- `Screen.SelectCityForFilter`

### Дочерние маршруты мероприятий

- `Screen.EventDetail`
- `Screen.CreateEvent`
- `Screen.EditEvent`
- `Screen.SelectParkForEvent`
- `Screen.EventParticipants`

### Дочерние маршруты сообщений

- `Screen.Chat`
- `Screen.UserSearch`
- `Screen.FriendsForDialog`

### Дочерние маршруты профиля

- `Screen.EditProfile`
- `Screen.UserParks`
- `Screen.MyFriends`
- `Screen.UserFriends`
- `Screen.OtherUserProfile`
- `Screen.Blacklist`
- `Screen.UserTrainingParks`
- `Screen.JournalsList`
- `Screen.JournalEntries`
- `Screen.ChangePassword`
- `Screen.SelectCountry`
- `Screen.SelectCity`

### Дочерние маршруты раздела "Ещё"

- `Screen.ThemeIcon`

## Маршруты, объявленные в `Destinations.kt`, но не подключённые как отдельные destination

- `Screen.ParkFilter`
  Сейчас фильтр площадок открывается не через route, а через `ParksFilterDialog` поверх `RootScreen`.
- `Screen.ParkRoute`
- `Screen.AddParkComment`
- `Screen.ParkGallery`
- `Screen.EventGallery`
- `Screen.AddEventComment`
- `Screen.Friends`

Эти маршруты присутствуют в модели навигации, но отдельного `composable(...)` для них в текущем `RootScreen` нет.

## Поведение нижней навигации

Нижняя навигация показывается не на всех экранах. `RootScreen` скрывает её для flow, где нужен полноэкранный или модальный сценарий, включая:

- создание и редактирование park/event;
- чат;
- выбор площадки для event;
- выбор города для фильтра;
- экран выбора друга для создания диалога.

Список базовых route для скрытия хранится в `BOTTOM_BAR_HIDDEN_BASE_ROUTES`.

## Что важно для дальнейших изменений

- Для новых дочерних экранов нужно либо задать статический `parentTab`, либо корректно поддержать `source`.
- Если экран должен сохранять контекст вкладки при переходах вглубь, route должен прокидывать `source`.
- Если экран открывается как локальный dialog/sheet поверх уже существующего контекста, отдельный navigation route не обязателен.

## Проверка актуальности

Документ сверён с:

- `Destinations.kt`;
- `AppState.kt`;
- `Navigation.kt`;
- `RootScreen.kt`.
