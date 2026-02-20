# Локальное хранение данных пользователя

## Расположение в коде

| Файл | Назначение |
|------|------------|
| `data/database/UserEntity.kt` | Entity с флагами (isFriend, isFriendRequest, isBlacklisted) |
| `data/database/UserDao.kt` | DAO с Flow методами |
| `data/database/SWDatabase.kt` | База данных Room |
| `data/repository/SWRepositoryImpl.kt` | Кэширование, Flow методы |
| `viewmodel/ProfileViewModel.kt` | Реактивное обновление через `currentUser: StateFlow<User?>` |

## Стратегия кэширования

Online-first с fallback на кэш:
1. Сначала запрос к серверу
2. При ошибке сети — данные из Room
3. Flow для реактивного обновления UI

## Реализовано

- ✅ `UserEntity` с флагами категоризации
- ✅ `UserDao` с Flow методами (getCurrentUserFlow, getFriendsFlow, getFriendRequestsFlow, getBlacklistFlow)
- ✅ Кэширование в `SWRepositoryImpl`
- ✅ DI: userDao в AppContainer
- ✅ `ProfileViewModel` использует `currentUser: StateFlow<User?>`
- ✅ Реактивное обновление UI в `ProfileRootScreen`

---

## Невыполненные задачи

### Этап 5: Очистка данных при logout

- [ ] Добавить метод `clearAll()` в `UserDao`
- [ ] Добавить метод `clearUserData()` в `SWRepository`
- [ ] Реализовать очистку в `ProfileRootScreen`

**Что удаляется:** профиль, друзья, заявки, blacklist, сообщения, дневники, комментарии
**Что НЕ удаляется:** площадки (публичные данные)

### Этап 6: Тестирование

- [ ] Unit-тесты для `UserDao`
- [ ] Интеграционные тесты для `SWRepository`
