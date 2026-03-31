# UserTrainingParks: Room-кэш списка площадок пользователя (TDD-план)

## Описание задачи

Реализовать локальный кэш списка площадок, где тренируется пользователь, без дублирования полных данных `Park` внутри `UserEntity`.

Цель:
- при успешном `GET /users/{userId}/areas` сохранять пришедшие площадки в общую таблицу `parks`
- отдельно сохранять локальную связь `userId -> [parkId]`
- использовать этот кэш для повторного открытия `UserTrainingParksScreen`
- подготовить базу для сценария выбора площадки при создании нового `event`, чтобы не дёргать сервер каждый раз

Ключевая идея:
- сами данные площадок продолжают жить в общей таблице `parks`
- список площадок пользователя хранится как отдельный локальный кэш связей
- `parksCount` в `UserEntity` остаётся только счётчиком, а не источником истины для списка

---

## Почему выбран такой вариант

### Плюсы

- не дублирует полный `List<Park>` внутри `UserEntity`
- переиспользует уже существующую таблицу `parks`
- подходит и для `currentUser`, и для любого другого `userId`
- хорошо сочетается с уже реализованным Room-кэшем для `ParkDetailScreen`
- упрощает повторное использование списка площадок пользователя в разных экранах

### Что не подходит

- хранить список тренировочных площадок в `addedParks`
  - это другая бизнес-семантика: "добавленные пользователем площадки", а не "площадки, где он тренируется"
- полагаться только на `parksCount`
  - сервер не присылает сам список площадок в `User`
  - `parksCount > 0` не означает, что локальный список уже загружен

---

## Целевое поведение

### Сценарий 1: успешная загрузка `/users/{userId}/areas`

1. `SWRepository.getParksForUser(userId)` получает `List<Park>` с сервера
2. Все полученные `Park` сохраняются через `upsert` в общую таблицу `parks`
3. Локальный кэш связей для `userId` полностью перезаписывается новым набором `parkId`
4. Метод возвращает список площадок как и сейчас

### Сценарий 2: повторное открытие списка площадок пользователя

1. `UserTrainingParksViewModel` сначала пытается прочитать локальный кэш по `userId`
2. Если локальный список найден, экран сразу показывает `Success`
3. После этого выполняется обычный refresh с сервера
4. При успехе серверный ответ обновляет и UI, и Room
5. При ошибке сеть не ломает уже показанный контент, а ошибка уходит в `UserNotifier`

### Сценарий 3: кэша нет

1. `UserTrainingParksViewModel` не находит локальный список по `userId`
2. Экран остаётся в `Loading`
3. Выполняется `GET /users/{userId}/areas`
4. При успехе список сохраняется в Room и показывается на экране
5. При ошибке экран показывает `Error`

### Сценарий 4: сервер вернул пустой список

1. Площадки пользователя по `userId` очищаются в локальном кэше связей
2. Старые связи не должны оставаться в Room
3. Экран показывает валидный пустой список, а не устаревшие данные

---

## Архитектурные решения

### 1. Хранить связь отдельно от `UserEntity`

Не добавлять `trainingParks: List<Park>` и не перегружать `UserEntity`.

Вместо этого создать отдельную локальную сущность связи, например:
- `UserTrainingParkEntity(userId, parkId)`

Эта сущность:
- не хранит полные данные `Park`
- ссылается на уже существующую таблицу `parks`
- легко очищается и пересобирается после каждого успешного `/users/{userId}/areas`

### 2. Кэш проектировать для любого `userId`

Схема данных должна работать для любого пользователя, а не только для `currentUser`.

Причина:
- ручка уже параметризована через `userId`
- это убирает специальную логику только для текущего пользователя
- в первой итерации можно использовать кэш только в основном сценарии `currentUser`, но модель данных лучше сделать общей сразу

### 3. Общая таблица `parks` остаётся единственным источником данных о площадках

`/users/{userId}/areas` при успехе:
- добавляет новые площадки в `parks`, если их там ещё нет
- обновляет существующие записи, если площадка уже есть
- затем обновляет связи `userId -> parkId`

### 4. Разделять состояния "не загружено" и "пусто"

Важно не смешивать:
- локальный список для `userId` ещё ни разу не загружался
- локальный список был загружен и оказался пустым

Иначе можно получить неверный UX:
- `parksCount > 0`, а UI показывает пустой список только потому, что кэш ещё не был заполнен

### 5. Миграции Room

Так как задача меняет схему Room, нужно добавить новую entity в `SWDatabase`.

Для текущего проекта фиксируем упрощённый подход:
- локальная база в разработке считается disposable
- допустим bump версии БД с destructive recreation локальной базы
- полноценные миграции не входят в scope этой задачи

---

## Текущее состояние

- `currentUser` уже хранится в Room через `UserEntity`
- в `UserEntity` уже есть `parksCount`, но это только счётчик
- `SWRepository.getParksForUser(userId)` сейчас работает как network-only и не использует Room
- `UserTrainingParksViewModel` сейчас тоже network-only: `Loading -> server -> Success/Error`
- `Park` уже умеет храниться в Room через общую таблицу `parks`
- `ParkDetailScreen` уже использует Room-кэш через общую таблицу `parks`

---

## Изменения в модели данных

### Новая entity связи

Добавить новую Room-сущность связи между пользователем и площадкой.

Примерная ответственность:
- хранить `userId`
- хранить `parkId`
- по желанию хранить порядок сортировки, если серверный порядок нужно сохранить

Если порядок важен для UI, стоит сразу добавить поле вроде:
- `position: Int`

Если порядок не важен, можно ограничиться составным ключом `userId + parkId`.

### Новый DAO

Добавить DAO для локального кэша связей.

Минимально нужны методы:
- получить список `parkId` по `userId`
- получить список `Park` для `userId` через join или двухшаговую выборку
- заменить все связи для `userId`
- очистить связи для `userId`
- проверить, есть ли локальный кэш для `userId`

---

## TDD-план

### RED phase

#### 1. Room entity и DAO для связей

**Файлы:**
- `app/src/test/java/com/swparks/data/database/entity/UserTrainingParkEntityTest.kt`
- `app/src/test/java/com/swparks/data/database/dao/UserTrainingParkDaoTest.kt`

```kotlin
    @Test
    fun insertForUser_whenNewRelations_thenStoresUserParkIds()

    @Test
    fun replaceForUser_whenExistingRelations_thenOldRelationsRemoved()

    @Test
    fun getParkIdsForUser_whenNoRelations_thenReturnsEmptyList()

    @Test
    fun hasCachedParks_whenRelationsExist_thenReturnsTrue()

    @Test
    fun clearForUser_whenCalled_thenRemovesOnlySpecifiedUserRelations()
```

Если DAO сразу умеет возвращать `List<Park>` через join:

```kotlin
    @Test
    fun getParksForUserFromCache_whenRelationsAndParksExist_thenReturnsJoinedParks()

    @Test
    fun getParksForUserFromCache_whenRelationsMissing_thenReturnsEmptyList()
```

#### 2. Repository cache logic

**Файл:**
- `app/src/test/java/com/swparks/data/repository/SWRepositoryUserTrainingParksTest.kt`

```kotlin
    @Test
    fun getParksForUser_whenNetworkSucceeds_thenUpsertsParksIntoCommonParkTable()

    @Test
    fun getParksForUser_whenNetworkSucceeds_thenReplacesUserParkRelations()

    @Test
    fun getParksForUser_whenNetworkReturnsEmptyList_thenClearsUserRelations()

    @Test
    fun getCachedParksForUser_whenRelationsExist_thenReturnsParksFromRoom()

    @Test
    fun getCachedParksForUser_whenNoCache_thenReturnsNoCacheState()
```

Отдельно нужен тест на защиту от смешения состояний:

```kotlin
    @Test
    fun getCachedParksForUser_whenNeverLoaded_thenReturnsCacheMissInsteadOfEmptyList()
```

#### 3. ViewModel cache-first поведение

**Файл:**
- `app/src/test/java/com/swparks/ui/viewmodel/UserTrainingParksViewModelTest.kt`

```kotlin
    @Test
    fun init_whenCacheExists_thenShowsCachedParksBeforeNetworkResult()

    @Test
    fun init_whenCacheExistsAndRefreshFails_thenKeepsCurrentContent()

    @Test
    fun init_whenCacheExistsAndRefreshSucceeds_thenUpdatesContent()

    @Test
    fun init_whenNoCache_thenShowsLoadingUntilNetworkResult()

    @Test
    fun init_whenNoCacheAndNetworkFails_thenShowsError()

    @Test
    fun refresh_whenContentAlreadyShownAndNetworkFails_thenKeepsContent()

    @Test
    fun refresh_whenNetworkFails_thenNotifiesUserAboutError()
```

#### 4. Интеграционный сценарий Room

**Файл:**
- `app/src/test/java/com/swparks/data/integration/UserTrainingParksCacheIntegrationTest.kt`

```kotlin
    @Test
    fun getParksForUser_whenSuccessful_thenParksBecomeReadableFromRoom()

    @Test
    fun repeatedLoad_whenServerListChanged_thenRelationsReplacedNotMerged()

    @Test
    fun cacheForDifferentUsers_whenStored_thenDoesNotMixTheirParkLists()
```

---

## GREEN phase: минимальная реализация

### 1. Phase 1: инфраструктура Room

- [ ] добавить новую entity связи `userId -> parkId`
- [ ] добавить новый DAO для локального кэша тренировочных площадок пользователя
- [ ] подключить новую entity/DAO в `SWDatabase`
- [ ] если требуется порядок списка, зафиксировать его в схеме и тестах
- [ ] написать и запустить Room-тесты для DAO

### 2. Phase 2: cache API в Repository

- [ ] расширить `SWRepository` методами чтения локального кэша площадок пользователя
- [ ] при успешном `getParksForUser(userId)` делать `upsert` всех `Park` в общую таблицу `parks`
- [ ] после этого полностью заменять связи `userId -> parkId`
- [ ] различать cache miss и cached empty result
- [ ] добавить тесты на repository-уровне

### 3. Phase 3: UserTrainingParksViewModel cache-first

- [ ] сначала читать локальный кэш по `userId`
- [ ] если кэш найден, сразу показывать `Success`
- [ ] после показа кэша запускать refresh с сервера
- [ ] если refresh падает, не уводить экран в `Error`, если контент уже показан
- [ ] если кэша нет, сохранять текущий `Loading -> Success/Error`
- [ ] добавить/обновить unit-тесты `UserTrainingParksViewModel`

### 4. Phase 4: интеграция со сценарием создания event

- [ ] проверить, где именно экран выбора площадки для нового события использует `UserTrainingParksScreen`
- [ ] убедиться, что при повторном открытии выбор площадки может использовать кэш
- [ ] не менять навигационный контракт без необходимости
- [ ] если потребуется, отдельно зафиксировать в плане следующий шаг: переиспользовать этот же кэш в других пользовательских списках площадок

### 5. Phase 5: polish и верификация

- [ ] прогнать целевые unit-тесты
- [ ] прогнать `make test`
- [ ] прогнать `make lint`
- [ ] вручную проверить сценарии:
  - первый вход без кэша
  - повторный вход с кэшем
  - пустой список
  - ошибка сети при наличии кэша
  - два разных `userId` с разными наборами площадок

---

## Архитектурные вопросы, которые нужно зафиксировать до реализации

### 1. Как представлять cache miss

Нельзя возвращать просто `emptyList()` и для "кэша нет", и для "кэш загружен, но пуст".

Нужно выбрать один вариант:
- отдельный sealed-результат для cache lookup
- пара методов вида `hasCachedParksForUser(userId)` + `getCachedParksForUser(userId)`
- или хранить служебный marker о том, что список для `userId` уже был синхронизирован

Предпочтительный вариант для простоты:
- `hasCachedParksForUser(userId)`
- `getCachedParksForUser(userId)`

### 2. Сохранять ли порядок площадок

Если порядок ответа `/users/{userId}/areas` важен для UI, его лучше сохранить явно.

Иначе при чтении через Room порядок может стать нестабильным.

### 3. Scope первой итерации

Рекомендуемый scope:
- модель данных и кэш делать для любого `userId`
- UI-использование в первую очередь проверить на сценарии `currentUser` и выборе площадки для создания `event`

---

## Изменяемые файлы

### Создаваемые

| Файл | Описание |
|------|----------|
| `app/src/main/java/com/swparks/data/database/entity/UserTrainingParkEntity.kt` | Entity связи `userId -> parkId` |
| `app/src/main/java/com/swparks/data/database/dao/UserTrainingParkDao.kt` | DAO для локального кэша списка площадок пользователя |
| `app/src/test/java/com/swparks/data/database/dao/UserTrainingParkDaoTest.kt` | Room-тесты для связи и чтения кэша |
| `app/src/test/java/com/swparks/data/repository/SWRepositoryUserTrainingParksTest.kt` | Тесты repository cache logic |
| `app/src/test/java/com/swparks/data/integration/UserTrainingParksCacheIntegrationTest.kt` | Интеграционные тесты кэша |

### Модифицируемые

| Файл | Изменения |
|------|-----------|
| `app/src/main/java/com/swparks/data/database/SWDatabase.kt` | подключение новой entity и DAO |
| `app/src/main/java/com/swparks/data/repository/SWRepository.kt` | cache-first логика для `getParksForUser(userId)` |
| `app/src/main/java/com/swparks/ui/viewmodel/UserTrainingParksViewModel.kt` | переход на cache-first поведение |
| `app/src/test/java/com/swparks/ui/viewmodel/UserTrainingParksViewModelTest.kt` | тесты нового сценария |

---

## Критерии завершения

1. [ ] После успешного `GET /users/{userId}/areas` все площадки сохраняются в общую таблицу `parks`
2. [ ] Для каждого `userId` локально сохраняется отдельный набор связей `userId -> parkId`
3. [ ] Повторное открытие `UserTrainingParksScreen` может показать данные из Room до ответа сервера
4. [ ] При ошибке сети и наличии кэша экран остаётся в `Success`
5. [ ] При отсутствии кэша и сетевой ошибке экран показывает `Error`
6. [ ] Пустой серверный ответ очищает старые связи пользователя, а не оставляет устаревший список
7. [ ] Кэши разных пользователей не смешиваются между собой
8. [ ] Unit/integration-тесты проходят
9. [ ] `make test` и `make lint` проходят

---

## Что не входит в scope

- хранение полного списка тренировочных площадок прямо в `UserEntity`
- замена `addedParks` на новую семантику
- полноценные Room migrations без потери локальных данных
- TTL / `cachedAt` / expiry policy для списка площадок пользователя
- автоматическое обновление кэша тренировочных площадок при любом изменении профиля
- рефактор всех остальных экранов, где потенциально можно переиспользовать этот кэш

---

## Порядок реализации

1. [ ] Зафиксировать модель кэша: отдельная связь `userId -> parkId`
2. [ ] Написать падающие DAO-тесты
3. [ ] Реализовать entity/DAO и подключить их в Room
4. [ ] Написать падающие repository-тесты
5. [ ] Реализовать кэширование `getParksForUser(userId)` через `parks` + связи
6. [ ] Написать падающие ViewModel-тесты
7. [ ] Перевести `UserTrainingParksViewModel` на cache-first
8. [ ] Проверить сценарий выбора площадки для создания события
9. [ ] Выполнить полный прогон тестов и линта
