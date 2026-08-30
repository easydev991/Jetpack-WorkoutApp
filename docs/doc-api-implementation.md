# API в Jetpack-WorkoutApp

## Расположение в коде

### Основные файлы

| Файл                                                            | Назначение                          |
|-----------------------------------------------------------------|-------------------------------------|
| `app/src/main/java/com/swparks/network/SWApi.kt`                | Retrofit интерфейс с 57 эндпоинтами |
| `app/src/main/java/com/swparks/data/model/*.kt`                 | 19 моделей данных для API и кэша    |
| `app/src/main/java/com/swparks/data/repository/*.kt`            | Репозитории для работы с API (`AuthRepository`, `FriendsRepository`, `UserProfileRepository`, `ParksEventsRepository`, `CommentsRepository`, `JournalsRepositoryImpl`, `JournalEntriesRepositoryImpl`, `MessagesRepositoryImpl`, `CountriesRepositoryImpl`, базовый `BaseRepository`) |
| `app/src/main/java/com/swparks/data/AppContainer.kt`            | Создание `OkHttpClient`, `Retrofit` и общего `SWApi` |
| `app/src/main/java/com/swparks/JetpackWorkoutApplication.kt`    | Инициализация `DefaultAppContainer` и preload auth token |

### Interceptors

| Файл                                   | Назначение                    |
|----------------------------------------|-------------------------------|
| `data/interceptor/LoggingInterceptor.kt` | Debug-логирование запросов |
| `data/interceptor/TokenInterceptor.kt` | Добавление токена авторизации |
| `data/interceptor/AuthInterceptor.kt`  | Обработка ошибок 401          |
| `data/interceptor/RetryInterceptor.kt` | Повтор запросов при ошибках   |

## Структура SWApi

Интерфейс `SWApi` организован по функциональным группам:

| Группа                 | Эндпоинты | Описание                                                                      |
|------------------------|-----------|-------------------------------------------------------------------------------|
| Авторизация и профиль  | 7         | login, register, resetPassword, changePassword, getUser, editUser, deleteUser |
| Друзья и черный список | 10        | CRUD друзей, заявки, blacklist, search                                        |
| Страны и города        | 1         | getCountries                                                                  |
| Площадки               | 13        | CRUD, train, комментарии, фото                                                |
| Мероприятия            | 12        | CRUD, go/not go, комментарии, фото                                            |
| Сообщения              | 5         | dialogs, messages, markAsRead                                                 |
| Дневники               | 9         | CRUD, записи, настройки                                                       |

## Модели данных

### Data models (`data/model/`)

- `User`, `LoginSuccess` — пользователь и авторизация
- `Park`, `ParkType`, `ParkSize`, `ParkFilter` — площадки и их фильтрация
- `Event` — мероприятия
- `Country`, `City` — география
- `DialogResponse`, `MessageResponse` — сообщения
- `JournalResponse`, `JournalEntryResponse` — дневники
- `Comment`, `Photo`, `SocialUpdates` — вспомогательные
- `NewParkDraft` — черновик новой площадки
- `ApiFriendAction`, `ApiBlacklistOption` — параметры запросов друзей и blacklist

## Статус реализации

✅ **Завершено**: 57 из 57 эндпоинтов (100%)

## Примечания по текущей реализации

- `DefaultAppContainer` создаёт один общий экземпляр `SWApi` и раздаёт его через `provideAuthApi()`, `provideProfileApi()` и остальные factory-методы.
- `JetpackWorkoutApplication` не создаёт `Retrofit` напрямую: оно создаёт `DefaultAppContainer` и при старте подгружает токен в память через `secureTokenRepository.loadTokenToCache()`.
