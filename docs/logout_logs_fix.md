# Исправление дублирующихся логов при логауте

## 1. Анализ проблемы

При выходе из аккаунта в логах дважды появляется сообщение `Текущий пользователь отсутствует`.

**Причина:**
`UserPreferencesRepository.currentUserId` основан на `dataStore.data`. DataStore эмитит событие при *любом* изменении в файле настроек.

При логауте происходит два изменения:
1. `clearUserData()` удаляет `current_user_id` -> DataStore эмитит данные (userId = null).
2. `forceLogout()` меняет `is_authorized` -> DataStore эмитит данные (userId всё еще null).

Так как в потоке `currentUserId` нет фильтрации повторов, `SWRepository` получает сигнал `null` дважды и дважды выполняет логику "пользователь отсутствует".

## 2. Решение

Добавить оператор `distinctUntilChanged()` в `UserPreferencesRepository`.

### Файл: `app/src/main/java/com/swparks/data/UserPreferencesRepository.kt`

```kotlin
import kotlinx.coroutines.flow.distinctUntilChanged // Не забудьте импорт!

// ...

class UserPreferencesRepository(...) {

    // ...

    /**
     * ID текущего авторизованного пользователя.
     * Эмитит изменения при сохранении/очистке.
     */
    val currentUserId: Flow<Long?> = dataStore.data
        .catch {
            // ... (обработка ошибок) ...
        }
        .map { it[current_user_id] }
        .distinctUntilChanged() // <--- ДОБАВИТЬ ЭТУ СТРОКУ
```

### Почему это поможет

`distinctUntilChanged()` пропускает значение дальше только если оно отличается от предыдущего.
1. При удалении ID: было `123`, стало `null` -> пропускаем (Лог выводится).
2. При смене флага авторизации: было `null`, стало `null` -> **игнорируем** (Лог не выводится).

Это сделает поток данных чище и избавит от лишних срабатываний логики в репозитории и ViewModel.
