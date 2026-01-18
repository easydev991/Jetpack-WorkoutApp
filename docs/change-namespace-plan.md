# План изменения namespace с `com.workout.jetpack_workout` на `com.swparks`

## Обзор задачи

Необходимо изменить пространство имен приложения с `com.workout.jetpack_workout` на `com.swparks` для упрощения и соответствия с `applicationId`.

**Важно**: `applicationId = "com.swparks"` остается без изменений - это уже корректное значение для Google Play Store.

## Быстрое начало (автоматизированный способ)

### Использование автоматизированного скрипта

Для автоматического выполнения всех этапов переноса используйте скрипт `scripts/change-namespace.sh`:

```bash
# Запуск скрипта из корня проекта
./scripts/change-namespace.sh
```

**Скрипт выполняет следующие действия:**
1. Создание новой структуры директорий
2. Перенос всех Kotlin файлов в новую структуру
3. Обновление package-объявлений во всех файлах
4. Обновление импортов во всех файлах
5. Обновление namespace в `build.gradle.kts`
6. Генерация отчета о выполнении

**После выполнения скрипта:**
1. Проверьте структуру новых файлов
2. Проверьте, что все импорты обновлены корректно
3. Выполните `./gradlew clean`
4. Выполните `./gradlew build`
5. Если все работает корректно, удалите старую директорию
6. Зафиксируйте изменения в git

## Затрагиваемые файлы

- `app/build.gradle.kts` - изменение namespace (строка 11)
- `app/src/main/AndroidManifest.xml` - проверка путей к компонентам (относительные пути не меняются)
- **58 файлов Kotlin** - перемещение в новую структуру пакетов и обновление импортов:
  - 2 файла в корне: `JetpackWorkoutApplication.kt`, `MainActivity.kt`
  - 14 файлов в `model/`
  - 1 файл в `network/`
  - 3 файла в `data/`
  - 2 файла в `utils/`
  - 2 файла в `ui/theme/`
  - 23 файла в `ui/ds/`
  - 11 файлов в `ui/screens/` (включая подпапки)

## Текущая структура проекта

```
app/src/main/java/com/workout/jetpack_workout/
├── data/
│   ├── AppContainer.kt
│   ├── SWRepository.kt
│   └── UserPreferencesRepository.kt
├── JetpackWorkoutApplication.kt
├── MainActivity.kt
├── model/
│   ├── BlacklistAction.kt
│   ├── City.kt
│   ├── Comment.kt
│   ├── Country.kt
│   ├── Event.kt
│   ├── EventKind.kt
│   ├── FriendAction.kt
│   ├── Gender.kt
│   ├── LoginSuccess.kt
│   ├── Park.kt
│   ├── ParkSize.kt
│   ├── ParkType.kt
│   ├── Photo.kt
│   ├── TabBarItem.kt
│   └── User.kt
├── network/
│   └── SWApi.kt
├── ui/
│   ├── ds/
│   │   ├── CheckmarkRowView.kt
│   │   ├── CircleBackgroundModifier.kt
│   │   ├── CircleBadgeView.kt
│   │   ├── ColorScheme+isLight.kt
│   │   ├── CommentRowView.kt
│   │   ├── DialogRowView.kt
│   │   ├── EventRowView.kt
│   │   ├── FormCardContainer.kt
│   │   ├── FormRowContainer.kt
│   │   ├── FormRowView.kt
│   │   ├── FriendRequestRowView.kt
│   │   ├── IncognitoProfileView.kt
│   │   ├── JournalRowView.kt
│   │   ├── ListRowView.kt
│   │   ├── LoadingOverlayView.kt
│   │   ├── MessageBubbleView.kt
│   │   ├── ParkRowView.kt
│   │   ├── ScaleOnTapModifier.kt
│   │   ├── SectionView.kt
│   │   ├── SWAsyncImage.kt
│   │   ├── SWButton.kt
│   │   ├── SWDateTimePicker.kt
│   │   ├── SWTextEditor.kt
│   │   ├── SWTextField.kt
│   │   ├── UserProfileCardView.kt
│   │   └── UserRowView.kt
│   ├── screens/
│   │   ├── events/
│   │   │   ├── EventsNavHost.kt
│   │   │   ├── EventsScreen.kt
│   │   │   └── EventsViewModel.kt
│   │   ├── messages/
│   │   │   └── MessagesNavHost.kt
│   │   ├── more/
│   │   │   └── MoreScreen.kt
│   │   ├── parks/
│   │   │   ├── ParksNavHost.kt
│   │   │   └── ParksRootScreen.kt
│   │   ├── profile/
│   │   │   └── ProfileNavHost.kt
│   │   └── RootScreen.kt
│   └── theme/
│       ├── Color.kt
│       └── Theme.kt
└── utils/
    ├── JsonUtils.kt
    └── ReadJSONFromAssets.kt
```

## Ручное выполнение (если не используется автоматизированный скрипт)

### Этап 1. Подготовка и обновление конфигурации

#### 1.1. Резервное копирование

- ✅ Создать коммит с текущим состоянием проекта перед изменениями

#### 1.2. Обновление `app/build.gradle.kts`

- Изменить `namespace` с `com.workout.jetpack_workout` на `com.swparks` (строка 11)
- Проверить, что `applicationId` остается `com.swparks` (без изменений)

### Этап 2. Проверка AndroidManifest.xml

#### 2.1. Проверка путей к компонентам

- Проверить, что все пути к компонентам используют относительные пути (с точкой в начале)
- Относительные пути (например, `.JetpackWorkoutApplication`, `.MainActivity`) **не нужно менять** - Android автоматически будет использовать новый namespace `com.swparks` как базовый пакет
- Убедиться, что нет абсолютных путей с `com.workout.jetpack_workout` (если есть, заменить на относительные)

**Текущее состояние AndroidManifest.xml:**
- `android:name=".JetpackWorkoutApplication"` - относительный путь ✅
- `android:name=".MainActivity"` - относительный путь ✅

### Этап 3. Перемещение файлов и обновление пакетов

#### 3.1. Перемещение Application и MainActivity

- Переместить `JetpackWorkoutApplication.kt` в `app/src/main/java/com/swparks/`
- Переместить `MainActivity.kt` в `app/src/main/java/com/swparks/`
- Обновить package-объявления в файлах:
  - `package com.workout.jetpack_workout` → `package com.swparks`

#### 3.2. Перемещение моделей данных

Переместить все файлы из `com/workout/jetpack_workout/model/` в `com/swparks/model/`:

```
BlacklistAction.kt, City.kt, Comment.kt, Country.kt, Event.kt,
EventKind.kt, FriendAction.kt, Gender.kt, LoginSuccess.kt,
Park.kt, ParkSize.kt, ParkType.kt, Photo.kt, TabBarItem.kt, User.kt
```

Обновить package-объявления: `package com.swparks.model`

#### 3.3. Перемещение сетевого слоя

Переместить `SWApi.kt` из `com/workout/jetpack_workout/network/` в `com/swparks/network/`

Обновить package-объявление: `package com.swparks.network`
Обновить все импорты моделей: `import com.workout.jetpack_workout.model.*` → `import com.swparks.model.*`

#### 3.4. Перемещение данных и репозиториев

Переместить файлы из `com/workout/jetpack_workout/data/` в `com/swparks/data/`:

```
AppContainer.kt, SWRepository.kt, UserPreferencesRepository.kt
```

Обновить package-объявление: `package com.swparks.data`
Обновить импорты моделей и сетевого слоя

#### 3.5. Перемещение утилит

Переместить файлы из `com/workout/jetpack_workout/utils/` в `com/swparks/utils/`:

```
JsonUtils.kt, ReadJSONFromAssets.kt
```

Обновить package-объявление: `package com.swparks.utils`

### Этап 4. Обновление UI компонентов

#### 4.1. Перемещение темы

Переместить файлы из `com/workout/jetpack_workout/ui/theme/` в `com/swparks/ui/theme/`:

```
Theme.kt, Color.kt
```

Обновить package-объявление: `package com.swparks.ui.theme`

#### 4.2. Перемещение дизайн-системы

Переместить все файлы из `com/workout/jetpack_workout/ui/ds/` в `com/swparks/ui/ds/` (23 файла):

```
CheckmarkRowView.kt, CircleBackgroundModifier.kt, CircleBadgeView.kt,
ColorScheme+isLight.kt, CommentRowView.kt, DialogRowView.kt,
EventRowView.kt, FormCardContainer.kt, FormRowContainer.kt,
FormRowView.kt, FriendRequestRowView.kt, IncognitoProfileView.kt,
JournalRowView.kt, ListRowView.kt, LoadingOverlayView.kt,
MessageBubbleView.kt, ParkRowView.kt, ScaleOnTapModifier.kt,
SectionView.kt, SWAsyncImage.kt, SWButton.kt, SWDateTimePicker.kt,
SWTextEditor.kt, SWTextField.kt, UserProfileCardView.kt, UserRowView.kt
```

Обновить package-объявление: `package com.swparks.ui.ds`
Обновить все импорты

#### 4.3. Перемещение экранов

Переместить файлы из `com/workout/jetpack_workout/ui/screens/` в `com/swparks/ui/screens/`:

**Корневые файлы:**
```
RootScreen.kt
```

**Поддиректории:**

`events/`:
```
EventsNavHost.kt, EventsScreen.kt, EventsViewModel.kt
```

`messages/`:
```
MessagesNavHost.kt
```

`more/`:
```
MoreScreen.kt
```

`parks/`:
```
ParksNavHost.kt, ParksRootScreen.kt
```

`profile/`:
```
ProfileNavHost.kt
```

Обновить package-объявления и все импорты

### Этап 5. Проверка и тестирование

#### 5.1. Очистка проекта

```bash
./gradlew clean
```

#### 5.2. Пересборка проекта

```bash
./gradlew build
```

#### 5.3. Проверка компиляции

- Убедиться, что проект собирается без ошибок
- Проверить отсутствие предупреждений, связанных с пакетами

#### 5.4. Запуск тестов

```bash
./gradlew test
```

#### 5.5. Запуск приложения на устройстве/эмуляторе

- Установить приложение
- Проверить, что приложение запускается корректно
- Проверить основные функции

### Этап 6. Удаление старых файлов

#### 6.1. Удаление старой структуры пакетов

- Удалить директорию `com/workout/jetpack_workout/`
- Проверить, что не осталось ссылок на старый пакет

#### 6.2. Финальная проверка

```bash
./gradlew build
```

### Этап 7. Фиксация изменений

- Создать коммит с изменениями namespace
- Добавить описание изменений в commit message

## Проверки на каждом этапе

### После выполнения скрипта или ручного переноса

1. **Проверка структуры файлов**:
   ```bash
   find app/src/main/java/com/swparks -name "*.kt" | wc -l
   # Должно показать: 58 файлов
   ```

2. **Проверка package-объявлений**:
   ```bash
   grep -r "^package com.workout.jetpack_workout" app/src/main/java/com/swparks --include="*.kt"
   # Не должно показать никаких результатов
   ```

3. **Проверка импортов**:
   ```bash
   grep -r "import com.workout.jetpack_workout" app/src/main/java/com/swparks --include="*.kt"
   # Не должно показать никаких результатов
   ```

4. **Проверка namespace в build.gradle.kts**:
   ```bash
   grep "namespace" app/build.gradle.kts
   # Должно показать: namespace = "com.swparks"
   ```

### После завершения всех этапов

- ✅ Все файлы используют новый пакет `com.swparks`
- ✅ Нет ссылок на старый пакет `com.workout.jetpack_workout`
- ✅ Проект собирается без ошибок (`./gradlew build`)
- ✅ Тесты проходят успешно (`./gradlew test`)
- ✅ Приложение работает корректно на устройстве/эмуляторе

## Команды для проверки

### Проверка количества файлов

```bash
# Новые файлы
find app/src/main/java/com/swparks -name "*.kt" | wc -l

# Старые файлы (перед удалением)
find app/src/main/java/com/workout/jetpack_workout -name "*.kt" | wc -l
```

### Поиск оставшихся ссылок на старый namespace

```bash
# В новых файлах
grep -r "com.workout.jetpack_workout" app/src/main/java/com/swparks --include="*.kt"

# Во всем проекте
grep -r "com.workout.jetpack_workout" app/src --include="*.kt" --include="*.xml"
```

### Проверка package-объявлений

```bash
# Должны начинаться с package com.swparks
grep "^package" app/src/main/java/com/swparks -r --include="*.kt"
```

## Возможные проблемы и решения

### Проблема: Ошибки при компиляции

**Симптомы**: Kotlin компилятор сообщает о неразрешенных символах

**Решение**:
1. Выполнить `./gradlew clean`
2. Проверить все импорты в перемещенных файлах
3. Проверить package-объявления
4. Пересобрать проект `./gradlew build`

### Проблема: Ошибки в R-классе

**Симптомы**: Ошибки типа `Unresolved reference: R`

**Решение**:
1. Выполнить `./gradlew clean`
2. Удалить папку `app/build`
3. Пересобрать проект `./gradlew build`

### Проблема: Проблемы с путями в AndroidManifest.xml

**Симптомы**: Android Studio или компилятор не могут найти классы Application или Activity

**Решение**:
- Убедиться, что используются относительные пути (например, `.MainActivity`)
- Относительные пути автоматически используют namespace как базовый пакет
- **НЕ** используйте абсолютные пути вроде `com.swparks.MainActivity`

### Проблема: Остались ссылки на старый пакет

**Симптомы**: При поиске находятся ссылки на `com.workout.jetpack_workout`

**Решение**:
1. Используйте глобальный поиск по проекту (Cmd+Shift+F в Android Studio)
2. Замените все вхождения `com.workout.jetpack_workout` на `com.swparks`
3. Будьте внимательны при замене в комментариях и документации

### Проблема: Разное количество файлов

**Симптомы**: Скрипт сообщает, что количество новых и старых файлов не совпадает

**Решение**:
1. Проверьте, все ли файлы были скопированы
2. Проверьте наличие файлов в подпапках
3. Сравните списки файлов в старой и новой директориях

## Результат

После выполнения плана или использования скрипта:

- ✅ Namespace изменен на `com.swparks`
- ✅ Все файлы перемещены в новую структуру пакетов
- ✅ Все package-объявления обновлены
- ✅ Все импорты обновлены
- ✅ Проект собирается без ошибок
- ✅ Приложение работает корректно
- ✅ Код стал более читаемым и консистентным
- ✅ Namespace соответствует applicationId

## Примечания

- `applicationId` остается `com.swparks` - это не изменяется
- Изменение namespace не влияет на идентификацию приложения в Google Play Store
- **Рекомендуется создать backup проекта** перед началом изменений
- **Рекомендуется работать в feature-branch** для безопасной фиксации изменений
- Автоматизированный скрипт значительно упрощает процесс переноса
- После выполнения скрипта обязательно проверьте результаты перед удалением старой директории

## Сравнение: автоматизированный vs ручной подход

| Аспект | Автоматизированный скрипт | Ручной подход |
|--------|--------------------------|---------------|
| Время выполнения | ~1 минута | ~30-60 минут |
| Риск ошибок | Минимальный | Высокий |
| Количество шагов | 1 команда | ~50+ шагов |
| Откат изменений | Простое удаление новых файлов | Сложно |
| Рекомендация | ✅ Использовать | ❌ Не рекомендуется |

## Дополнительные ресурсы

- [Android namespace documentation](https://developer.android.com/build/configure-app-module#set-namespace)
- [ApplicationId vs Namespace](https://developer.android.com/studio/build/application-id-vs-namespace)
