# Проверка площадок перед созданием мероприятия

## Описание

В Android-приложении реализовано поведение, аналогичное iOS:

- авторизованный пользователь нажимает FAB создания мероприятия во вкладке `Events`;
- если у пользователя есть площадки, где он тренируется, открывается экран создания мероприятия;
- если у пользователя нет таких площадок, вместо открытия формы показывается алерт;
- из алерта пользователь может перейти в первую вкладку bottom navigation: `Parks`.

Ориентир по iOS:

- `SwiftUI-WorkoutApp/SwiftUI-WorkoutApp/Screens/Events/EventsListScreen.swift`
- локализационный ключ сообщения: `Alert.EventCreationRule`

## Пользовательский сценарий

### Сценарий 1: у пользователя есть площадки

1. Пользователь открывает вкладку `Events`.
2. Нажимает FAB создания мероприятия.
3. Приложение открывает `CreateEvent`.

### Сценарий 2: у пользователя нет площадок

1. Пользователь открывает вкладку `Events`.
2. Нажимает FAB создания мероприятия.
3. Приложение показывает алерт с правилом создания мероприятия.
4. Пользователь может:
   - перейти во вкладку `Parks`;
   - закрыть алерт кнопкой “Понятно”.

### Сценарий 3: пользователь отметил площадку как тренировочную

1. Пользователь переходит в `ParkDetailScreen`.
2. Включает toggle “Тренируюсь здесь”.
3. После успешного запроса `POST /areas/{parkId}/train` локальный кэш обновляет:
   - `park.trainHere`;
   - `park.trainingUsers` / `park.trainingUsersCount`;
   - `currentUser.parksCount`.
4. После возврата на `Events` FAB уже ведёт на создание мероприятия без повторного алерта.

## Тексты алерта

### Заголовок

- `ru`: `Необходимо выбрать площадку`
- `en`: `You need to select a park`

### Сообщение

- `ru`: `Чтобы создать мероприятие, нужно указать хотя бы одну площадку, где ты тренируешься`
- `en`: `To create an event, you need to specify at least one park where you train`

### Кнопки

- confirm:
  - `ru`: `Перейти на карту`
  - `en`: `Open parks`
- dismiss:
  - `ru`: `Понятно`
  - `en`: `Understood`

## Архитектурное решение

### Источник правила

Правило основано на `User.hasUsedParks` из модели пользователя:

- файл: `app/src/main/java/com/swparks/data/model/User.kt`
- вычисление основано на поле `parksCount` (`area_count` с сервера)

Таким образом, экран мероприятий не делает отдельный сетевой запрос для проверки допуска к созданию мероприятия.

### ViewModel

`EventsViewModel`:

- подписывается на `SWRepository.getCurrentUserFlow()`;
- хранит `currentUser` как `StateFlow<User?>`;
- на `onFabClick()` выбирает одно из двух событий:
  - `EventsEvent.NavigateToCreateEvent`
  - `EventsEvent.ShowEventCreationRule`

Ключевые файлы:

- `app/src/main/java/com/swparks/ui/viewmodel/EventsViewModel.kt`
- `app/src/main/java/com/swparks/ui/viewmodel/IEventsViewModel.kt`
- `app/src/main/java/com/swparks/data/AppContainer.kt`

### UI

`EventsScreen`:

- показывает FAB только если пользователь авторизован и `currentUser != null`;
- слушает `events` из ViewModel через `LaunchedEffect`;
- по `ShowEventCreationRule` открывает `AlertDialog`;
- по confirm-действию вызывает отдельный callback `onNavigateToParks`.

Ключевой файл:

- `app/src/main/java/com/swparks/ui/screens/events/EventsScreen.kt`

### Навигация

В `RootScreen` экран `EventsScreen` получает два отдельных callback:

- `onNavigateToCreateEvent`
- `onNavigateToParks`

Переход из алерта использует стандартный переход в top-level вкладку `Parks`, без дополнительного переключения внутренних tab `LIST/MAP`.

Ключевой файл:

- `app/src/main/java/com/swparks/ui/screens/RootScreen.kt`

## Важная деталь по кэшу пользователя

Во время реализации был найден важный баг:

- после `trainHere` обновлялся кэш площадки;
- но не обновлялся `currentUser.parksCount`;
- из-за этого `EventsViewModel` продолжал видеть `hasUsedParks == false`, даже если пользователь только что отметил площадку как тренировочную.

Исправление:

- в `SWRepository.updateTrainHereCache()` после успешного `trainHere` / `untrainHere` обновляется и `parksCount` текущего пользователя в `UserDao`.

Ключевой файл:

- `app/src/main/java/com/swparks/data/repository/SWRepository.kt`

## Покрытие тестами

### Unit tests

Покрыты:

- логика `EventsViewModel` для `hasUsedParks == true / false`;
- реакция `EventsViewModel` на обновление `currentUserFlow`;
- обновление `currentUser.parksCount` в `SWRepository` после `changeTrainHereStatus(true/false)`.

Ключевые файлы:

- `app/src/test/java/com/swparks/ui/screens/events/EventsViewModelTest.kt`
- `app/src/test/java/com/swparks/data/repository/SWRepositoryParksTest.kt`

### UI tests

Покрыты:

- показ алерта при отсутствии площадок;
- отсутствие алерта при наличии площадок;
- заголовок, сообщение и кнопки алерта;
- переход в `Parks` по confirm;
- закрытие алерта по dismiss;
- позитивный сценарий перехода в `CreateEvent`.

Ключевой файл:

- `app/src/androidTest/java/com/swparks/ui/screens/events/EventsScreenTest.kt`

## Проверки

По итогам реализации выполнены:

- `make format`
- `make lint`
- релевантные unit-тесты
- UI-тесты для `EventsScreen`
- ручная проверка в эмуляторе:
  - без площадок → показывается алерт;
  - из алерта происходит переход в `Parks`;
  - при наличии площадок открывается форма создания;
  - после `trainHere` сценарий обновляется без повторного ложного алерта.

## Файлы реализации

Основные изменения относятся к следующим файлам:

- `app/src/main/java/com/swparks/ui/viewmodel/EventsViewModel.kt`
- `app/src/main/java/com/swparks/ui/viewmodel/IEventsViewModel.kt`
- `app/src/main/java/com/swparks/ui/screens/events/EventsScreen.kt`
- `app/src/main/java/com/swparks/ui/screens/RootScreen.kt`
- `app/src/main/java/com/swparks/data/AppContainer.kt`
- `app/src/main/java/com/swparks/data/repository/SWRepository.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-ru/strings.xml`
- `app/src/test/java/com/swparks/ui/screens/events/EventsViewModelTest.kt`
- `app/src/test/java/com/swparks/data/repository/SWRepositoryParksTest.kt`
- `app/src/androidTest/java/com/swparks/ui/screens/events/EventsScreenTest.kt`

## Что важно помнить дальше

- Правило сейчас опирается именно на `currentUser.parksCount`, а не на отдельную загрузку списка площадок пользователя.
- Если в будущем появятся другие entry point создания мероприятия, это же правило стоит переиспользовать, а не дублировать локально в новом экране.
- Если продукт позже захочет вести пользователя не просто в `Parks`, а сразу в конкретный внутренний режим вкладки, это уже будет отдельная навигационная доработка.
