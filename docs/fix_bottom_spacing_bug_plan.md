# План исправления бага с пустым пространством между LazyColumn и BottomNavigationBar

## Описание проблемы

На экранах `ParksRootScreen.kt` и `EventsScreen.kt` между LazyColumn и BottomNavigationBar появляется пустое пространство.

## Корневая причина

Двойная вложенность Scaffold:
- **RootScreen.kt** имеет внешний `Scaffold` с `bottomBar` (BottomNavigationBar) и `contentWindowInsets = WindowInsets(0, 0, 0, 0)`
- **ParksRootScreen.kt** и **EventsScreen.kt** имеют свой внутренний `Scaffold` с `topBar`
- Внутренний Scaffold получает `paddingValues` только от своего TopAppBar, но не учитывает BottomNavigationBar из родительского Scaffold

## Решение

Передача TopAppBar как параметра в родительский Scaffold:
- Обновить `ParksRootScreen` и `EventsScreen` для приема `topBar` как параметра вместо создания собственного Scaffold
- Удалить внутренний Scaffold из этих экранов
- Обновить `RootScreen.kt` для передачи соответствующего TopAppBar через параметр `topBar` внешнего Scaffold
- Убедиться, что LazyColumn использует `paddingValues` от родительского Scaffold

## Этапы реализации

### Этап 1: Изменение ParksRootScreen.kt
- [ ] Изменить сигнатуру для приема `@Composable () -> Unit topBar` или использовать Box вместо Scaffold
- [ ] Удалить внутренний Scaffold (строки 35-56)
- [ ] Добавить TopAppBar напрямую в UI без Scaffold или передать его как параметр

### Этап 2: Изменение EventsScreen.kt и PastEventsScreen.kt
- [ ] Аналогично изменить `EventsScreen.kt` и `PastEventsScreen.kt`
- [ ] Убрать внутренние Scaffold

### Этап 3: Обновление RootScreen.kt
- [ ] Обновить `RootScreen.kt` для передачи TopAppBar в каждый экран через параметры
- [ ] Настроить передачу разных TopAppBar для разных вкладок (Parks, Events)

### Этап 4: Проверка корректности
- [ ] Убедиться, что LazyColumn не перекрывает BottomNavigationBar
- [ ] Проверить, что контент прокручивается до самого низа без лишних отступов
- [ ] Убедиться, что TopAppBar отображается корректно
- [ ] Проверить на разных размерах экранов
- [ ] Протестировать на экранах без контента (пустой список)
- [ ] Проверить корректность прокрутки

### Этап 5: Актуализация документации
- [ ] Добавить раздел в архитектурную документацию о правильной работе с Scaffold и windowInsets
- [ ] Обновить правила разработки экранов для предотвращения повторения этой ошибки

## Критерии завершения

- ✅ Нет пустого пространства между LazyColumn и BottomNavigationBar
- ✅ LazyColumn прокручивается до конца контента
- ✅ TopAppBar отображается корректно на всех экранах
- ✅ Контент не перекрывается BottomNavigationBar
- ✅ Отступы корректны на всех размерах экранов
- ✅ Документация обновлена

## Примечание

`contentPadding.bottom` не влияет на верстку, потому что добавляет отступ внутри прокручиваемого контента, который "уходит вверх" при прокрутке вниз. Проблема специфична для вложенных Scaffold — каждый Scaffold имеет свои paddingValues.
