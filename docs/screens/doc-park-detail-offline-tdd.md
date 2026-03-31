# ParkDetailScreen: Offline-поддержка (упрощённый план)

## Описание задачи

Реализовать простую cache-first схему для `ParkDetailScreen` на базе уже существующей таблицы `parks`.

Принцип работы:
- при успешной загрузке `getPark(id)` сохранять полные данные площадки в Room
- при следующем открытии `ParkDetailScreen` сначала читать данные из Room
- после показа кэша пытаться обновить площадку с сервера
- если сеть недоступна, сохранять текущее поведение ошибок через `UserNotifier`, но не уводить экран в `Error`, если в Room уже есть данные
- кнопки карты должны быть недоступны, если координаты площадки отсутствуют или невалидны

Это решение намеренно проще, чем отдельный потоковый `UseCase` с несколькими эмиссиями. Для текущего проекта достаточно:
- расширить существующую `parks`-таблицу
- добавить простые cache-операции в `SWRepository`
- обновить `ParkDetailViewModel`, чтобы она сначала подставляла кэш, а затем делала обычный network refresh

---

## Целевое поведение

### Сценарий 1: в Room уже есть данные площадки

1. `ParkDetailScreen` открывается
2. `ParkDetailViewModel` читает `Park` из Room по `parkId`
3. Если запись найдена, экран сразу показывает `Content`
4. Параллельно выполняется обычный `swRepository.getPark(parkId)`
5. Если сеть успешна:
   - новые данные сохраняются в Room
   - UI обновляется свежими данными
6. Если сеть падает:
   - ошибка показывается через существующий `UserNotifier`
   - экран остаётся на уже показанных данных из Room

### Сценарий 2: в Room данных нет

1. `ParkDetailScreen` открывается
2. `ParkDetailViewModel` не находит запись в Room
3. Экран показывает `InitialLoading`
4. Выполняется `swRepository.getPark(parkId)`
5. Если сеть успешна:
   - данные сохраняются в Room
   - экран показывает `Content`
6. Если сеть падает:
   - экран показывает `Error`, как и сейчас

### Сценарий 3: pull-to-refresh

- `refresh()` всегда идёт в сеть
- при успехе обновляет Room и UI
- при ошибке:
  - если на экране уже есть контент, он остаётся
  - ошибка показывается через `UserNotifier`

---

## Почему этот вариант выбран

### Плюсы

- использует уже существующую таблицу `parks`
- не требует новой таблицы `ParkDetailEntity`
- не требует отдельного `Flow`-контракта для `GetParkDetailUseCase`
- легко покрывается unit-тестами
- хорошо ложится на текущее устройство `ParkDetailViewModel`

### Ограничения

- сложные поля (`photos`, `comments`, `trainingUsers`, `author`, `equipmentIDS`) придётся хранить через `TypeConverters`
- после мутаций, которые возвращают `Result<Unit>`, нужно явно выбрать стратегию обновления кэша

---

## Текущее состояние

- `SWRepository.getPark(id)` уже сохраняет успешно загруженную площадку в Room
- `SWRepository` уже умеет `getParkFromCache(parkId)` и `cachePark(park)`
- `SWRepository` уже обновляет detail-кэш после `savePark()`, `deletePark()`, `changeTrainHereStatus()`, `deleteParkPhoto()`, `deleteComment()`
- `ParkDetailViewModel.loadPark()` уже работает по схеме cache-first: сначала Room, затем фоновый refresh с сервера
- `refresh()` в `ParkDetailViewModel` уже использует текущий `Content` как fallback при сетевой ошибке
- `ParkEntity` уже расширен detail-полями (`author`, `photos`, `comments`, `trainingUsers`, `trainHere`, `mine`, `canEdit`, `equipmentIDS`, `createDate`, `modifyDate`)
- `commentsCount` и `trainingUsersCount` уже переведены в nullable-семантику
- общий объект `AppConverters` уже добавлен и подключён на уровне `SWDatabase`
- `ParkDao` уже поддерживает `getParkById`, `upsertPark`, `deleteById`
- тесты для `Converters`, `ParkEntity`, `ParkDao`, `SWRepository` и `ParkDetailViewModel` уже реализованы и проходят на текущем покрытии
- полный прогон `make test` и `make lint` уже проходит

---

## Архитектурные решения

### 1. Использовать существующую таблицу `parks`

Не создавать отдельную таблицу `ParkDetailEntity`.

Храним и short-данные списка, и full-данные detail-экрана в одной таблице:
- short-модель из списка продолжает записываться как раньше
- detail-модель поверх неё дозаполняет расширенные поля

### 2. Не добавлять отдельный `GetParkDetailUseCase`

Для этой задачи cache-first orchestration остаётся в `ParkDetailViewModel`.

Причина:
- логика простая
- экран один
- отдельный `Flow`-use case добавит больше контрактов и тестовой сложности, чем пользы

Если позже аналогичная логика появится ещё в нескольких местах, тогда можно будет вынести её в use case отдельной задачей.

### 3. Хранить сложные поля через typed-поля + `@TypeConverters`

Использовать один подход:
- `ParkEntity` получает typed-поля
- Room сериализует их через `@TypeConverters`
- не использовать ручные `String`-поля вида `photosJson`, `authorJson`

### 4. Обновление detail-кэша после мутаций

Фиксируем простую стратегию:

- `savePark()`:
  - после успешного ответа сохранить пришедший `Park` в Room

- `deletePark()`:
  - после успешного ответа удалить запись из Room

- `changeTrainHereStatus(trainHere, parkId)`:
  - после успешного `Result.success(Unit)` локально обновить кэшированную запись:
    - поменять `trainHere`
    - скорректировать `trainingUsers` и `trainingUsersCount`, если на экране есть достаточно данных
  - если локально корректно обновить нельзя, допускается fallback: повторно запросить `getPark(parkId)` и записать свежий `Park`

- `deleteParkPhoto(parkId, photoId)`:
  - после успешного `Result.success(Unit)` локально удалить фото из `photos`
  - обновить `preview`, если удалено текущее превью и в списке ещё есть фото
  - если локальная корректировка становится слишком сложной, допустим fallback через повторный `getPark(parkId)`

- `deleteComment(option, commentId)`:
  - обновлять detail-кэш только если `option` относится к парку
  - для park-case извлекать `parkId` из `TextEntryOption`
  - после успешного удаления локально удалять комментарий из `comments` и уменьшать `commentsCount`
  - для не-park сценариев кэш площадки не трогать

### 5. Поведение UI для partial cache

- partial cache допустим для показа на `ParkDetailScreen`
- секция участников не должна показывать `0`, если `trainingUsersCount` отсутствует или равен нулю
- секции с `photos`, `author`, `comments` могут не отображаться до прихода полных данных
- кнопки карты и маршрута должны быть disabled, если `latitude` / `longitude` отсутствуют или не парсятся в `Double`

### 6. Стратегия миграции Room

База сейчас создаётся через `fallbackToDestructiveMigration(...)`.

Для этой задачи фиксируем простой вариант:
- локальная база в разработке считается disposable
- после изменения схемы допускается удалить приложение / переустановить его в эмуляторе
- полноценный upgrade path с `addMigrations(...)` не входит в scope
- отдельная миграционная стратегия до первого релиза не требуется

Это соответствует текущему состоянию проекта и сильно упрощает задачу.

### 7. Конвертеры

Текущий `UserConverters` по смыслу перестанет соответствовать содержимому, если туда добавить `Photo`, `Comment`, `Int`.

Поэтому в рамках задачи нужно:
- либо переименовать `UserConverters` в более общий объект, например `AppConverters`
- либо создать отдельный объект для park-related конвертеров

Предпочтительный вариант: общий объект `AppConverters`.

### 8. Где размещать `@TypeConverters`

Зафиксировать `@TypeConverters` на уровне `SWDatabase`, а не только на одной entity.

Причина:
- единая точка подключения
- меньше риска забыть аннотацию на новых entity
- проще поддерживать общий набор конвертеров

---

## Изменения в модели данных

### ParkEntity

Расширить `ParkEntity`, чтобы она могла хранить данные detail-экрана:

- `author: User?`
- `photos: List<Photo>?`
- `comments: List<Comment>?`
- `trainingUsers: List<User>?`
- `trainHere: Boolean?`
- `mine: Boolean?`
- `canEdit: Boolean?`
- `equipmentIDS: List<Int>?`
- `createDate: String?`
- `modifyDate: String?`

Существующие поля `commentsCount` и `trainingUsersCount` стоит пересмотреть:
- предпочтительно сделать nullable в entity тоже
- не превращать `null` автоматически в `0`, если это можно избежать

Причина:
- `null` означает "сервер не прислал значение"
- `0` означает "значение известно и равно нулю"

---

## TDD-план

### RED phase

#### 1. ParkEntity и мапперы

**Файл:** `app/src/test/java/com/swparks/data/database/entity/ParkEntityTest.kt`

```kotlin
    @Test
    fun toEntity_whenFullPark_thenPreservesAllDetailFields()

    @Test
    fun toPark_whenFullParkEntity_thenRestoresAllDetailFields()

    @Test
    fun toEntity_whenNullComplexFields_thenPreservesNullValues()

    @Test
    fun toPark_whenEntityWithNullComplexFields_thenPreservesNullValues()

    @Test
    fun roundTrip_whenToEntityToPark_thenPreservesNullableCountersSemantics()
```

#### 2. TypeConverters

**Файл:** `app/src/test/java/com/swparks/data/database/ConvertersTest.kt`

```kotlin
    @Test
    fun photoListConverter_whenSerializedAndDeserialized_thenReturnsCorrectResult()

    @Test
    fun commentListConverter_whenSerializedAndDeserialized_thenReturnsCorrectResult()

    @Test
    fun userListConverter_whenSerializedAndDeserialized_thenReturnsCorrectResult()

    @Test
    fun intListConverter_whenSerializedAndDeserialized_thenReturnsCorrectResult()

    @Test
    fun userConverter_whenSerializedAndDeserialized_thenReturnsCorrectResult()

    @Test
    fun converters_whenNullInput_thenReturnNull()
```

#### 3. ParkDao

**Файл:** `app/src/test/java/com/swparks/data/database/dao/ParkDaoTest.kt`

```kotlin
    @Test
    fun getParkById_whenEntityExists_thenReturnsEntity()

    @Test
    fun getParkById_whenEntityMissing_thenReturnsNull()

    @Test
    fun upsertPark_whenNewEntity_thenInsertsSuccessfully()

    @Test
    fun upsertPark_whenExistingEntity_thenReplacesEntity()

    @Test
    fun storedEntity_whenDetailFieldsSet_thenPreservesDetailFieldsViaRoom()
```

#### 4. SWRepository cache methods

**Файл:** `app/src/test/java/com/swparks/data/repository/SWRepositoryParkCacheTest.kt`

```kotlin
    @Test
    fun getParkFromCache_whenEntityExists_thenReturnsCachedPark()

    @Test
    fun getParkFromCache_whenEntityMissing_thenReturnsNull()

    @Test
    fun getPark_whenNetworkSucceeds_thenSavesParkToCache()

    @Test
    fun savePark_whenNetworkSucceeds_thenUpdatesCache()

    @Test
    fun deletePark_whenNetworkSucceeds_thenRemovesEntityFromCache()

    @Test
    fun changeTrainHereStatus_whenSuccess_thenUpdatesCacheOrRefreshesFromNetwork()

    @Test
    fun deleteParkPhoto_whenSuccess_thenUpdatesCacheOrRefreshesFromNetwork()

    @Test
    fun deleteComment_whenParkOptionAndSuccess_thenUpdatesCachedParkDetail()

    @Test
    fun deleteComment_whenNonParkOption_thenDoesNotTouchParkCache()
```

#### 5. ParkDetailViewModel

**Файл:** `app/src/test/java/com/swparks/ui/viewmodel/ParkDetailViewModelCacheTest.kt`

```kotlin
    @Test
    fun loadPark_whenCacheExists_thenShowsCachedContentBeforeNetworkResult()

    @Test
    fun loadPark_whenCacheExistsAndNetworkFails_thenKeepsScreenOutOfErrorState()

    @Test
    fun loadPark_whenCacheExistsAndNetworkSucceeds_thenUpdatesContentWithFreshData()

    @Test
    fun loadPark_whenCachedParkIsPartial_thenShowsCacheThenRequestsNetwork()

    @Test
    fun loadPark_whenCachedParkHasInvalidCoordinates_thenKeepsMapActionsDisabled()

    @Test
    fun loadPark_whenNoCache_thenShowsInitialLoadingUntilNetworkResult()

    @Test
    fun loadPark_whenNoCacheAndNetworkSucceeds_thenShowsContent()

    @Test
    fun loadPark_whenNoCacheAndNetworkFails_thenShowsError()

    @Test
    fun refresh_whenContentShownAndNetworkFails_thenKeepsCurrentContent()

    @Test
    fun refresh_whenNetworkFails_thenNotifiesUserAboutError()

    @Test
    fun refresh_whenNetworkSucceeds_thenUpdatesContentAndCache()
```

#### 6. Интеграционный тест

**Файл:** `app/src/test/java/com/swparks/data/integration/ParkCacheIntegrationTest.kt`

```kotlin
    @Test
    fun parkDetailLoad_whenSuccessful_thenIsReadableFromRoomOnNextOpen()

    @Test
    fun cachedShortPark_whenGetParkSucceeds_thenReplacedByFullerDetailVersion()

    @Test
    fun cachedPark_whenNetworkFailureLater_thenRemainsVisible()
```

---

## GREEN phase: минимальная реализация

### 1. Phase 1: инфраструктура Room [x]

- [x] введён общий объект `AppConverters`
- [x] `@TypeConverters(AppConverters::class)` подключён на уровне `SWDatabase`
- [x] добавлены конвертеры для `Photo`, `Comment`, `User`, `List<Int>`
- [x] `ParkEntity` расширен detail-полями
- [x] nullable-семантика для `commentsCount` и `trainingUsersCount` сохранена
- [x] `ParkDao` дополнен методами `upsertPark` и `deleteById`
- [x] добавлены и проходят тесты:
  - `ConvertersTest`
  - `ParkEntityTest`
  - `ParkDaoTest`

### 2. Phase 2: cache API в Repository [x]

- [x] добавлены `getParkFromCache(parkId)` и `cachePark(park)`
- [x] `getPark(id)` после успеха сохраняет `Park` в Room
- [x] `savePark()` после успеха сохраняет `Park` в Room
- [x] `deletePark()` после успеха удаляет запись из Room
- [x] реализовано обновление detail-кэша для:
  - `changeTrainHereStatus()`
  - `deleteParkPhoto()`
  - `deleteComment()`
- [x] добавлены и проходят repository-тесты для cache API и cache invalidation

### 3. Phase 3: ParkDetailViewModel cache-first [x]

- [x] `loadPark()` сначала пытается прочитать `Park` из Room
- [x] если кэш есть, `ViewModel` сразу показывает `Content`
- [x] если `park.isFull == false`, всё равно выполняется фоновый refresh
- [x] при успешном refresh UI обновляется свежими данными
- [x] при неуспешном refresh экран остаётся в `Content`, а ошибка уходит в `UserNotifier`
- [x] `refresh()` всегда идёт в сеть и использует текущий контент как fallback
- [x] добавлены cache-first тесты для `ParkDetailViewModel`

### 4. Phase 4: UI polish для partial cache [x]

Изменение `ParkDetailUIState` не обязательно.

Минимальный scope:
- оставить `InitialLoading`
- оставить `Content`
- оставить `Error`

Флаг `isFromCache` можно не добавлять в этой задаче, чтобы не раздувать контракт UI.

- [x] кнопки карты и маршрута переведены в disabled-состояние при невалидных координатах
- [x] для проверки координат используется `isValidCoordinates(latitude, longitude)`
- [x] полный прогон `make test` и `make lint` завершился успешно

---

## Изменяемые файлы

### Создаваемые

| Файл | Описание |
|------|----------|
| `app/src/test/java/com/swparks/data/database/entity/ParkEntityTest.kt` | Тесты мапперов `ParkEntity` |
| `app/src/test/java/com/swparks/data/database/ConvertersTest.kt` | Тесты конвертеров |
| `app/src/test/java/com/swparks/data/database/dao/ParkDaoTest.kt` | Тесты DAO |
| `app/src/test/java/com/swparks/data/repository/SWRepositoryParkCacheTest.kt` | Тесты cache API в repository |
| `app/src/test/java/com/swparks/ui/viewmodel/ParkDetailViewModelCacheTest.kt` | Тесты cache-first поведения экрана |
| `app/src/test/java/com/swparks/data/integration/ParkCacheIntegrationTest.kt` | Интеграционная проверка цикла Room → Repository → ViewModel |

### Модифицируемые

| Файл | Изменения |
|------|-----------|
| `app/src/main/java/com/swparks/data/database/Converters.kt` | Общие TypeConverters |
| `app/src/main/java/com/swparks/data/database/SWDatabase.kt` | Подключение конвертеров |
| `app/src/main/java/com/swparks/data/database/entity/ParkEntity.kt` | Detail-поля и мапперы |
| `app/src/main/java/com/swparks/data/database/dao/ParkDao.kt` | `upsertPark`, возможно `deleteById` |
| `app/src/main/java/com/swparks/data/repository/SWRepository.kt` | Простые cache-операции и обновление Room после успешных мутаций |
| `app/src/main/java/com/swparks/ui/viewmodel/ParkDetailViewModel.kt` | Сначала Room, затем refresh с сервера |

---

## Критерии завершения

1. [x] Инфраструктура Room для Park Detail готова:
   - `ParkEntity` расширен
   - `AppConverters` подключены
   - `ParkDao` умеет `upsertPark` и `deleteById`
   - тесты на мапперы, конвертеры и DAO проходят
2. [x] При успешной загрузке `getPark(id)` данные сохраняются в существующую таблицу `parks`
3. [x] При повторном открытии той же площадки экран может показать данные из Room до ответа сервера
4. [x] Если сервер недоступен, но кэш есть, экран остаётся в `Content`
5. [x] Если сервера нет и кэша нет, экран показывает `Error`
6. [x] Ошибки refresh по-прежнему проходят через существующий `UserNotifier`
7. [x] Тесты на repository и viewmodel проходят
8. [x] Кнопки карты и маршрута disabled при невалидных координатах
9. [x] `make test` и `make lint` проходят полным прогоном

---

## Что не входит в scope

- отдельный `GetParkDetailUseCase`
- отдельный `Flow`-контракт с несколькими эмиссиями cached/fresh/error
- отдельная таблица `ParkDetailEntity`
- полноценные Room migrations без потери локальных данных
- `cachedAt` / expiry policy
- UI-индикатор "данные из кэша"

## Технический долг

- полезно добавить отдельный UI/ViewModel тест именно на сценарий невалидных координат в cached park
- аналогичная проблема существует в `EventDetailSections.kt`, но выходит за рамки данной задачи

---

## Порядок реализации

1. [x] Расширить `ParkEntity` и конвертеры
2. [x] Обновить `SWDatabase` и `ParkDao`
3. [x] Добавить cache API в `SWRepository`
4. [x] Сохранять успешный `getPark(id)` в Room
5. [x] Перевести `ParkDetailViewModel` на схему "сначала Room, потом сеть"
6. [x] Добавить обновление detail-кэша после мутаций
7. [x] Сделать map actions disabled при невалидных координатах
8. [x] Закрыть оставшийся UI polish и полным прогоном тестов/линта
