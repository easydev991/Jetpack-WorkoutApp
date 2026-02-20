# API в Jetpack-WorkoutApp

## Расположение в коде

### Основные файлы

| Файл | Назначение |
|------|------------|
| `app/src/main/java/com/swparks/network/SWApi.kt` | Retrofit интерфейс с 57 эндпоинтами |
| `app/src/main/java/com/swparks/data/model/*.kt` | 17 DTO моделей для API |
| `app/src/main/java/com/swparks/data/repository/SWRepository.kt` | Репозиторий для работы с API |
| `app/src/main/java/com/swparks/JetpackWorkoutApplication.kt` | Создание Retrofit клиента |

### Interceptors

| Файл | Назначение |
|------|------------|
| `data/interceptor/TokenInterceptor.kt` | Добавление токена авторизации |
| `data/interceptor/AuthInterceptor.kt` | Обработка ошибок 401 |
| `data/interceptor/RetryInterceptor.kt` | Повтор запросов при ошибках |

## Структура SWApi

Интерфейс `SWApi` организован по функциональным группам:

| Группа | Эндпоинты | Описание |
|--------|-----------|----------|
| Авторизация и профиль | 7 | login, register, resetPassword, changePassword, getUser, editUser, deleteUser |
| Друзья и черный список | 10 | CRUD друзей, заявки, blacklist, search |
| Страны и города | 1 | getCountries |
| Площадки | 15 | CRUD, train, комментарии, фото |
| Мероприятия | 12 | CRUD, go/not go, комментарии, фото |
| Сообщения | 5 | dialogs, messages, markAsRead |
| Дневники | 7 | CRUD, записи, настройки |

## Модели данных

### Data models (`data/model/`)

- `User`, `LoginSuccess` — пользователь и авторизация
- `Park`, `ParkType`, `ParkSize` — площадки
- `Event` — мероприятия
- `Country`, `City` — география
- `DialogResponse`, `MessageResponse` — сообщения
- `JournalResponse`, `JournalEntryResponse` — дневники
- `Comment`, `Photo`, `SocialUpdates` — вспомогательные

## Статус реализации

✅ **Завершено**: 57 из 57 эндпоинтов (100%)

## Запланированные улучшения

- **Этап 8**: Рефакторинг kotlinx.serialization (kotlinx-datetime, JsonNamingStrategy.SnakeCase)
