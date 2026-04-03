# Документация: retry из ErrorContentView переводит экран в Loading

## Статус реализации

Функциональность реализована и закреплена тестами.

Изначальная цель была закрыть баг, при котором после нажатия `Повторить` экран оставался в `ErrorContentView` и не переходил в наблюдаемое loading-состояние. По итогам работ этот контракт приведён к одному виду на всех целевых экранах.

## Финальный контракт

- retry из full-screen error переводит экран в `InitialLoading` или `Loading` до завершения повторной операции;
- refresh поверх уже показанного контента не разрушает `Content`;
- ошибка refresh поверх контента не возвращает экран в full-screen error;
- `ErrorContentView` показывается только для сценария без валидного ранее загруженного контента;
- источник loading один: ViewModel/state, а не локальная UI-логика.

## Что было сделано

### Уже закрытые экраны из первой части плана

- [x] `EventsScreen` / `EventsViewModel`
- [x] `EventDetailScreen` / `EventDetailViewModel`
- [x] `ParkDetailScreen` / `ParkDetailViewModel`

### Доработанные экраны из второй части плана

- [x] `JournalsListScreen` / `JournalsViewModel`
- [x] `JournalEntriesScreen` / `JournalEntriesViewModel`
- [x] `ParksAddedByUserScreen` / `UserAddedParksViewModel`

## Реализованные изменения по оставшимся экранам

### Journals

- [x] `retry()` больше не является простым повторным сетевым вызовом
- [x] retry из `Error` переводит экран в `InitialLoading`
- [x] refresh поверх `Content` сохраняет список дневников на экране
- [x] ошибка refresh поверх `Content` больше не разрушает UI
- [x] поведение пустого состояния и full-screen error разведено через явный orchestration загрузки

### JournalEntries

- [x] `retry()` переведён на контракт `Error -> InitialLoading -> Content/Error`
- [x] refresh поверх уже загруженных записей сохраняет existing content
- [x] при refresh не теряются derived-поля `firstEntryId`, `canCreateEntry`, `journal`
- [x] ошибка поверх контента не переводит экран в full-screen error

### ParksAddedByUser

- [x] разделены сценарии `retry from error` и `refresh from success`
- [x] в `IUserAddedParksViewModel` добавлен отдельный `retry()`
- [x] `ParksAddedByUserScreen` вызывает `retry()` из ветки `ErrorContentView`
- [x] refresh поверх `Success` сохраняет список парков
- [x] сценарий `seedParks + refresh failure` сохраняет уже показанный `Success`

## Тестовое покрытие

### Unit

- [x] добавлены тесты на `Error -> Loading -> Content`
- [x] добавлены тесты на `Error -> Loading -> Error`
- [x] добавлены тесты на сохранение `Content` при refresh-failure
- [x] обновлены unit-тесты для `JournalsViewModel`
- [x] обновлены unit-тесты для `JournalEntriesViewModel`
- [x] обновлены unit-тесты для `UserAddedParksViewModel`

### UI / instrumentation

- [x] добавлены UI-тесты для `JournalsListScreen` на retry success/failure
- [x] добавлены UI-тесты для `JournalEntriesScreen` на retry success/failure
- [x] добавлен новый suite `ParksAddedByUserScreenTest`
- [x] instrumentation smoke-check целевых экранов: `60/60` зелёных на `Pixel_9 (AVD) - 16`

## Верификация

- [x] `make test`
- [x] `make lint`
- [x] `./gradlew :app:testDebugUnitTest --tests "com.swparks.ui.viewmodel.JournalsViewModelTest" --tests "com.swparks.ui.viewmodel.JournalEntriesViewModelTest" --tests "com.swparks.ui.viewmodel.UserAddedParksViewModelTest"`
- [x] `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.swparks.ui.screens.journals.JournalsListScreenTest,com.swparks.ui.screens.journals.JournalEntriesScreenTest,com.swparks.ui.screens.parks.ParksAddedByUserScreenTest`

## Итог

- [x] все экраны из этого направления больше не застревают в `ErrorContentView` во время retry
- [x] loading при retry стал наблюдаемым и закреплён тестами
- [x] refresh поверх контента больше не разрушает UI
- [x] первоначальный и оставшийся план сведены в один итоговый документ

## Примечание

Документ описывает фактически реализованный retry/loading-контракт и заменяет собой прежние плановые файлы по этой доработке.
