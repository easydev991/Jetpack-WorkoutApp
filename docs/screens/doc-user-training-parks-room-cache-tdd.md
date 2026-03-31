# UserTrainingParks: Room-кэш списка площадок пользователя

## Описание

В проекте реализован локальный Room-кэш для списка площадок, где тренируется пользователь.

Решение построено так, чтобы:
- не дублировать полный `List<Park>` внутри `UserEntity`
- переиспользовать уже существующую общую таблицу `parks`
- поддерживать кэш для любого `userId`, а не только для `currentUser`
- позволять `UserTrainingParksScreen` быстро показывать локальные данные до ответа сервера
- уменьшать количество повторных запросов к `/users/{userId}/areas`, в том числе в сценарии выбора площадки при создании события

---

## Что реализовано

### 1. Общая модель хранения

Сами площадки продолжают храниться в общей таблице `parks`.

Для списка площадок пользователя добавлены отдельные сущности:
- `UserTrainingParkEntity` — связь `userId -> parkId`
- `UserTrainingParkCacheStateEntity` — marker того, что кэш для `userId` уже был инициализирован

Это позволяет разделять два разных состояния:
- `cache miss` — список ещё ни разу не загружался
- `cached empty` — список уже был успешно загружен, но оказался пустым

`parksCount` в `UserEntity` при этом остаётся только счётчиком из профиля и не используется как источник истины для списка площадок.

---

## Структура данных

### Таблица площадок

Используется уже существующая таблица:
- `parks`

Она хранит сами данные `Park`, которые могут приходить:
- из общего списка площадок
- из `ParkDetail`
- из `/users/{userId}/areas`

### Таблица связей пользователя с площадками

Используется:
- `user_training_parks`

Хранит:
- `userId`
- `parkId`
- `position`

`position` нужен для сохранения порядка площадок, пришедшего с сервера.

### Таблица состояния кэша

Используется:
- `user_training_parks_cache_state`

Она нужна только для одного:
- понимать, что кэш для конкретного `userId` уже был синхронизирован хотя бы один раз

Без этого пустой список и отсутствие кэша выглядели бы одинаково.

---

## Как работает кэширование

### Успешный запрос `/users/{userId}/areas`

При успешном `SWRepository.getParksForUser(userId)`:
- сервер возвращает `List<Park>`
- все полученные площадки сохраняются в общую таблицу `parks`
- для `userId` полностью пересобираются связи `userId -> parkId`
- marker кэша для `userId` помечается как инициализированный

Если сервер вернул пустой список:
- старые связи пользователя очищаются
- marker кэша всё равно сохраняется

Это значит, что пустой список считается валидным успешным результатом, а не отсутствием кэша.

### Чтение локального кэша

В `SWRepository` реализованы методы:
- `hasCachedParksForUser(userId)`
- `getCachedParksForUser(userId)`

Логика такая:
- если marker кэша отсутствует, возвращается `cache miss`
- если marker есть, читается список площадок пользователя из Room
- если связей нет, возвращается корректный пустой список

---

## Поведение UserTrainingParksViewModel

`UserTrainingParksViewModel` работает по схеме cache-first.

### При открытии экрана

1. Сначала проверяется, есть ли локальный кэш для `userId`
2. Если кэш есть:
   - экран сразу переходит в `Success`
   - показываются локальные площадки из Room
   - затем запускается фоновый refresh с сервера
3. Если кэша нет:
   - экран остаётся в `Loading`
   - выполняется загрузка с сервера

### При успешном фоне

- UI обновляется свежими данными
- Room уже синхронизирован на уровне `SWRepository`

### При ошибке фона

- если контент уже показан, экран остаётся в `Success`
- ошибка уходит в `UserNotifier`

### При pull-to-refresh

- всегда выполняется запрос в сеть
- при успехе экран обновляется
- при ошибке:
  - если контент уже есть, он сохраняется
  - если контента нет, экран переходит в `Error`

### Обработка ошибок чтения кэша

Если исключение происходит во время cache lookup:
- `ViewModel` переводит экран в `Error`
- ошибка отправляется в `UserNotifier`

---

## Интеграция со сценарием создания события

Сценарий выбора площадки для нового события уже использует этот кэш без отдельной дополнительной логики.

Маршрут:
- `SelectParkForEvent`

Использует:
- `UserTrainingParksScreen`
- `UserTrainingParksViewModel`

Это значит:
- если пользователь уже загружал свои площадки раньше, они могут быть показаны сразу из Room
- выбор площадки для создания события получает benefit от cache-first автоматически
- навигационный контракт для этого не менялся

---

## Почему выбрана именно такая архитектура

### Что даёт текущее решение

- единый источник правды по площадкам через таблицу `parks`
- отсутствие дублирования полного `List<Park>` внутри `UserEntity`
- возможность переиспользовать одни и те же `Park` в разных сценариях
- поддержка кэша для любого `userId`
- корректное различение `cache miss` и пустого, но уже синхронизированного списка

### Что сознательно не использовалось

- хранение `trainingParks: List<Park>` прямо в `UserEntity`
- переиспользование `addedParks` для другой бизнес-семантики
- TTL / `cachedAt` / expiry policy
- полноценные Room migrations без потери локальных данных

Для текущего проекта используется упрощённый подход:
- база в разработке считается disposable
- при изменении схемы допускается переустановка приложения

---

## Основные файлы

### Data / Room

- [SWDatabase.kt](/Users/Oleg991/Documents/GitHub/Jetpack-WorkoutApp/app/src/main/java/com/swparks/data/database/SWDatabase.kt)
- [UserTrainingParkEntity.kt](/Users/Oleg991/Documents/GitHub/Jetpack-WorkoutApp/app/src/main/java/com/swparks/data/database/entity/UserTrainingParkEntity.kt)
- [UserTrainingParkCacheStateEntity.kt](/Users/Oleg991/Documents/GitHub/Jetpack-WorkoutApp/app/src/main/java/com/swparks/data/database/entity/UserTrainingParkCacheStateEntity.kt)
- [UserTrainingParkDao.kt](/Users/Oleg991/Documents/GitHub/Jetpack-WorkoutApp/app/src/main/java/com/swparks/data/database/dao/UserTrainingParkDao.kt)

### Repository

- [SWRepository.kt](/Users/Oleg991/Documents/GitHub/Jetpack-WorkoutApp/app/src/main/java/com/swparks/data/repository/SWRepository.kt)

### UI / ViewModel

- [UserTrainingParksViewModel.kt](/Users/Oleg991/Documents/GitHub/Jetpack-WorkoutApp/app/src/main/java/com/swparks/ui/viewmodel/UserTrainingParksViewModel.kt)
- [UserTrainingParksScreen.kt](/Users/Oleg991/Documents/GitHub/Jetpack-WorkoutApp/app/src/main/java/com/swparks/ui/screens/profile/UserTrainingParksScreen.kt)
- [RootScreen.kt](/Users/Oleg991/Documents/GitHub/Jetpack-WorkoutApp/app/src/main/java/com/swparks/ui/screens/RootScreen.kt)

### Тесты

- [UserTrainingParkDaoTest.kt](/Users/Oleg991/Documents/GitHub/Jetpack-WorkoutApp/app/src/test/java/com/swparks/data/database/dao/UserTrainingParkDaoTest.kt)
- [UserTrainingParkEntityTest.kt](/Users/Oleg991/Documents/GitHub/Jetpack-WorkoutApp/app/src/test/java/com/swparks/data/database/entity/UserTrainingParkEntityTest.kt)
- [SWRepositoryUserTrainingParksTest.kt](/Users/Oleg991/Documents/GitHub/Jetpack-WorkoutApp/app/src/test/java/com/swparks/data/repository/SWRepositoryUserTrainingParksTest.kt)
- [UserTrainingParksViewModelTest.kt](/Users/Oleg991/Documents/GitHub/Jetpack-WorkoutApp/app/src/test/java/com/swparks/ui/viewmodel/UserTrainingParksViewModelTest.kt)

---

## Что важно помнить при развитии

### 1. Не ломать семантику cache miss

Если менять DAO или repository-логику, важно сохранить различие между:
- отсутствием кэша
- пустым, но уже синхронизированным списком

### 2. Не дублировать `Park` в `UserEntity`

Текущая архитектура специально избегает хранения полного списка тренировочных площадок внутри пользователя.

Если понадобится новый сценарий со списками площадок пользователя, лучше переиспользовать:
- общую таблицу `parks`
- связи `userId -> parkId`

### 3. Сохранять порядок ответа сервера

Если UI зависит от порядка площадок, нужно поддерживать поле `position` и не терять его при обновлениях.

### 4. Переиспользование в других экранах

Текущий кэш уже подходит для дальнейшего использования в других пользовательских списках площадок, если такие сценарии появятся.

---

## Статус

Функциональность реализована и подтверждена тестами.

Проверено:
- unit-тесты DAO
- unit-тесты repository
- unit-тесты `UserTrainingParksViewModel`
- полный `make test`
- полный `make lint`
