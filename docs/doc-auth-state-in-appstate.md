# Состояние авторизации в AppState

Краткая сводка по реализации. Подробное описание механики находится в `docs/doc-appstate-auth-state.md`.

## Что реализовано

В `AppState` уже есть полноценное состояние авторизации:

- `currentUser: User?` на `mutableStateOf`;
- вычисляемое свойство `isAuthorized`;
- метод `updateCurrentUser(user: User?)`;
- `bottomNavVisualEpoch` для принудительного обновления временного визуального состояния нижней навигации после auth-flow.

## Откуда берётся `currentUser`

В `RootScreen` создаётся общий `ProfileViewModel`, после чего:

1. `currentUser` читается через `collectAsState()`;
2. `LaunchedEffect(currentUser)` синхронизирует значение в `appState.updateCurrentUser(currentUser)`.

Это и есть текущий источник правды для auth-state на уровне UI.

## Где это уже используется

Подтверждённые использования в коде:

- `ProfileTopAppBar` открывает поиск пользователей с учётом `appState.isAuthorized`;
- `ParksRootScreen` использует `appState.isAuthorized` для действий, доступных только после входа;
- `MessagesRootScreen` переключает гостевой и авторизованный сценарии;
- `BottomNavigationBar` учитывает `isAuthorized` и `bottomNavVisualEpoch`, чтобы корректно обновлять визуальное состояние после login/logout.

## Что было устаревшим в старой версии документа

Старый список "следующих шагов" больше не соответствует проекту:

- условный UI по авторизации уже применяется;
- проверки авторизации в сообщениях уже есть;
- документ с неправильной ссылкой `docs/appstate-auth-state.md` заменён на актуальный путь `docs/doc-appstate-auth-state.md`.

## Проверка актуальности

Документ сверён с:

- `app/src/main/java/com/swparks/navigation/AppState.kt`;
- `app/src/main/java/com/swparks/ui/screens/RootScreen.kt`;
- `app/src/main/java/com/swparks/ui/screens/profile/ProfileRootScreen.kt`;
- `app/src/main/java/com/swparks/ui/screens/parks/ParksRootScreen.kt`;
- `app/src/main/java/com/swparks/ui/screens/messages/MessagesRootScreen.kt`;
- `app/src/main/java/com/swparks/navigation/Navigation.kt`.
