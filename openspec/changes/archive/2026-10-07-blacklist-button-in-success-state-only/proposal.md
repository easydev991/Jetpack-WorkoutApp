# Proposal

## Why

На экране профиля другого пользователя кнопка блокировки в `TopAppBar` отображается во
всех состояниях, включая ошибку загрузки профиля. Без загруженного `viewedUser` действие
блокировки невозможно (`performBlacklistAction()` завершается no-op), поэтому в состояниях
`Error`, `Loading`, `UserNotFound`, `BlockedByUser` кнопка — мёртвый UI, вводящий
пользователя в заблуждение.

## What Changes

- Кнопка блокировки/разблокировки в `TopAppBar` экрана чужого профиля отображается
  только в состоянии `OtherUserProfileUiState.Success` — единственном, где профиль
  загружен и действие осмысленно.
- Кнопка «Назад» и заголовок остаются видимыми во всех состояниях.
- Добавляются UI-тесты (instrumented, Compose): в состоянии Error кнопки нет, в Success
  — есть (защита от регрессии).

## Capabilities

### New Capabilities

- `other-user-profile`: поведение экрана профиля другого пользователя — доступность
  действий в `TopAppBar` (блокировка) в зависимости от состояния загрузки профиля.

### Modified Capabilities

<!-- Спеков в проекте пока нет, изменяемых capability нет. -->

## Impact

- Код: `app/src/main/java/com/swparks/ui/screens/profile/OtherUserProfileScreen.kt`
  (`OtherUserProfileScaffold`, `OtherUserProfileTopAppBar`).
- Тесты: `app/src/androidTest/java/com/swparks/ui/screens/profile/OtherUserProfileScreenTest.kt`
  (новый фейк `FakeIOtherUserProfileViewModel` + 2 теста).
- Логика ViewModel, репозиториев и API не меняется; визуально меняется только слот
  `actions` топбара.
