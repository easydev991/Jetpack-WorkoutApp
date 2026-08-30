# Локальное хранение данных пользователя

## Расположение в коде

| Файл                                       | Назначение                                                  |
|--------------------------------------------|-------------------------------------------------------------|
| `data/database/UserEntity.kt`              | Entity с флагами (isFriend, isFriendRequest, isBlacklisted) |
| `data/database/UserDao.kt`                 | DAO с Flow методами                                         |
| `data/database/SWDatabase.kt`              | База данных Room                                            |
| `data/repository/UserProfileRepository.kt` | Кэширование профиля, Flow текущего пользователя             |
| `data/repository/FriendsRepository.kt`     | Кэширование друзей/заявок/blacklist через UserDao Flow      |
| `data/repository/AuthRepository.kt`        | Очистка данных при logout (`clearUserData()`)               |
| `viewmodel/ProfileViewModel.kt`            | Реактивное обновление через `currentUser: StateFlow<User?>` |

## Стратегия кэширования

Online-first с fallback на кэш:
1. Сначала запрос к серверу
2. При ошибке сети — данные из Room
3. Flow для реактивного обновления UI

## Реализовано

- ✅ `UserEntity` с флагами категоризации
- ✅ `UserDao` с Flow методами (getCurrentUserFlow, getFriendsFlow, getFriendRequestsFlow, getBlacklistFlow)
- ✅ Кэширование в репозиториях (`UserProfileRepository`, `FriendsRepository`)
- ✅ DI: userDao в AppContainer
- ✅ `ProfileViewModel` использует `currentUser: StateFlow<User?>`
- ✅ Реактивное обновление UI в `ProfileRootScreen` (`viewModel.currentUser.collectAsState()`)
- ✅ Очистка при logout: `AuthRepository.clearUserData()` вызывается из `LogoutUseCase` и `DeleteUserUseCase`

---

## Этап 5: Очистка данных при logout — ✅ реализовано

- [x] Метод `clearAll()` в `UserDao` (`DELETE FROM users`)
- [x] Метод `clearUserData()` в `AuthRepository` (не в `SWRepository` — репозиторий был разделён): `userDao.clearAll()` + `dialogDao.deleteAll()` + очистка `currentUserId` в DataStore
- [x] Очистка реализована не в `ProfileRootScreen`, а через `LogoutUseCase` и `DeleteUserUseCase` (используются в `AuthViewModel`)

**Что удаляется фактически:** пользователи (профиль, друзья, заявки, blacklist — все строки таблицы `users`) и диалоги
**Что НЕ удаляется:** площадки (публичные данные); дневники и комментарии в Room не очищаются (расхождение с исходной постановкой, оставлено без правок кода)

## Этап 6: Тестирование

- [ ] Unit-тесты для `UserDao` (прямое покрытие DAO отсутствует)
- [x] Тесты репозиториев (аналог запланированных «интеграционных тестов для `SWRepository`»): `AuthRepositoryTest`, `FriendsRepositoryTest`, `UserProfileRepositoryTest` и др.
