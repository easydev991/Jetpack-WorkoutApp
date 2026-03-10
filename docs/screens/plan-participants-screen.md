---

# План реализации экрана ParticipantsScreen

## Цель

Сделать переиспользуемый экран списка пользователей для двух сценариев:

* участники мероприятия (`EventDetail`)
* пользователи, тренирующиеся на площадке (`ParkDetail`)

Экран не загружает данные из сети: принимает готовый список пользователей и отображает его.  
При нажатии на пользователя выполняется навигация в `OtherUserProfileScreen`.

## Референсы

* iOS: `SwiftUI-WorkoutApp/Screens/Common/ParticipantsScreen.swift`
* Android маршруты: `Screen.EventParticipants`, `Screen.ParkTrainees`, `Screen.OtherUserProfile`
* Android UI-компонент: `UserRowView`

## Границы первой версии

* Без API-запросов, без repository/use case.
* Только отображение списка `User`.
* Заголовок зависит от места вызова:
  * для мероприятия: "Участники мероприятия"
  * для площадки: "Здесь тренируются"
* Переход по клику: `OtherUserProfileScreen`.
* Для текущего пользователя (если `user.id == currentUserId`) клик отключён.

## Этап 0: Контракт экрана

* [ ] Создать UI-модель режима экрана, например `ParticipantsMode`:
  * `EVENT`
  * `PARK`
* [ ] Определить входные параметры `ParticipantsScreen`:
  * `mode`
  * `users: List<User>`
  * `currentUserId: Long?`
  * `onBack`
  * `onUserClick: (Long) -> Unit`
* [ ] Зафиксировать правило заголовка через `stringResource` по `mode`.

## Этап 1: UI-реализация

* [ ] Создать файл экрана: `ui/screens/common/ParticipantsScreen.kt`.
* [ ] Реализовать `Scaffold` + `TopAppBar` + back.
* [ ] Реализовать список через `LazyColumn`.
* [ ] Каждого пользователя рендерить через `UserRowView`:
  * avatar
  * name
  * адрес (country/city, если уже доступен в модели)
* [ ] Для `currentUserId` отключать клик по строке.
* [ ] Для остальных вызывать `onUserClick(user.id)`.
* [ ] Добавить `EmptyStateView`, если список пуст.

## Этап 2: Навигация и переиспользование

* [ ] Подключить экран в `RootScreen` для `Screen.EventParticipants`.
* [ ] Подключить тот же экран в `RootScreen` для `Screen.ParkTrainees`.
* [ ] Из `EventDetailScreen` направить `onParticipantsCountClick` в реальную навигацию на `Screen.EventParticipants`.
* [ ] Для `ParkDetail` (когда будет реализован) использовать тот же экран с `mode = PARK`.
* [ ] На клик пользователя навигировать через:
  * `Screen.OtherUserProfile.createRoute(userId, source)`
  * `source` сохранять от экрана-источника.

## Этап 3: Локализация

* [ ] Добавить/переиспользовать строки для заголовков:
  * `event_participants_title` / "Участники мероприятия"
  * `park_trainees_title` / "Здесь тренируются"
* [ ] Проверить `values/strings.xml` и `values-ru/strings.xml`.

## Этап 4: Тесты и проверка

* [ ] Unit/UI тест на выбор заголовка по `mode`.
* [ ] UI тест на рендер списка пользователей.
* [ ] UI тест: клик по чужому пользователю вызывает `onUserClick`.
* [ ] UI тест: для `currentUserId` клик недоступен.
* [ ] UI тест: пустой список показывает `EmptyStateView`.

## Критерии готовности

* [ ] Один переиспользуемый экран работает для event и park сценариев.
* [ ] Заголовок корректно меняется по месту вызова.
* [ ] Навигация в `OtherUserProfileScreen` работает с корректным `source`.
* [ ] Экран не содержит сетевых запросов и бизнес-логики загрузки.
* [ ] Есть минимальный набор UI-тестов на ключевые сценарии.

## Порядок внедрения

1. Этап 0 (контракт)
2. Этап 1 (UI)
3. Этап 3 (локализация)
4. Этап 2 (навигация)
5. Этап 4 (тесты)
