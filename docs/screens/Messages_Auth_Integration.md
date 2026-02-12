# Интеграция авторизации на экране "Messages"

## Статус: ✅ ЗАВЕРШЕНО

## Описание

Интеграция LoginScreen при нажатии на "Authorize" на экране "Messages" с переиспользованием существующего LoginSheetHost. Для авторизованных пользователей показывается заглушка.

## Выполненные этапы

### Этап 1: MessagesRootScreen ✅

- Добавлены параметры `appState: AppState` и `onShowLoginSheet: () -> Unit`
- Реализована условная логика: `isAuthorized` → заглушка, иначе → `IncognitoProfileView`

### Этап 2: Интеграция в RootScreen ✅

- Переданы `appState` и `onShowLoginSheet` в `MessagesRootScreen`
- Используется общий `LoginSheetHost` (без навигации после авторизации)

### Этап 3: Локализация ✅

- `messages_placeholder`: "Your messages will be here" / "Тут будут ваши сообщения"

### Этап 4: Тестирование ✅

- Все сценарии проверены (авторизация, logout, кросс-экранная синхронизация)

## Изменённые файлы

```
app/src/main/java/com/swparks/
├── ui/screens/
│   ├── messages/MessagesRootScreen.kt  # appState, onShowLoginSheet
│   └── RootScreen.kt                   # передача параметров
└── res/values*/strings.xml             # messages_placeholder
```

## Acceptance Criteria: ✅ Все выполнены
