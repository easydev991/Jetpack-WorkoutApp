# Jetpack Workout App - Технический контекст

## Архитектура приложения

Приложение построено по архитектуре MVVM (Model-View-ViewModel) с использованием современных технологий Android:

- **Язык программирования**: Kotlin
- **Фреймворк UI**: Jetpack Compose
- **Сетевое взаимодействие**: Retrofit с Kotlinx Serialization
- **Локальное хранилище**: Room Database
- **Управление состоянием**: Jetpack Compose State
- **Асинхронные операции**: Kotlin Coroutines
- **Навигация**: Compose Navigation

## Структура проекта

```
app/
├── src/main/
│   ├── java/com/workout/jetpack_workout/
│   │   ├── data/           # Слой данных (репозитории, источники данных)
│   │   ├── model/          # Модели данных
│   │   ├── network/        # Сетевой слой (API интерфейсы, DTO)
│   │   ├── ui/             # UI слой (экраны, компонуемые элементы, темы)
│   │   ├── utils/          # Утилиты и вспомогательные функции
│   │   ├── JetpackWorkoutApplication.kt  # Класс приложения
│   │   └── MainActivity.kt # Главная активность
│   ├── res/                # Ресурсы (строки, изображения и т.д.)
│   └── AndroidManifest.xml # Манифест приложения
```

## Зависимости

Основные зависимости проекта:
- `androidx.core:core-ktx:1.17.0`
- `androidx.lifecycle:lifecycle-runtime-ktx:2.10.0`
- `androidx.activity:activity-compose:1.12.2`
- `androidx.compose:*:compose-bom:2025.12.01` (Material 3)
- `com.squareup.retrofit2:retrofit:3.0.0`
- `org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0`
- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2`
- `io.coil-kt:coil-compose:2.7.0`
- `androidx.datastore:datastore-preferences:1.2.0`
- `androidx.room:room-runtime:2.8.4`

## Конфигурация сборки

- **compileSdk**: 36
- **minSdk**: 24
- **targetSdk**: 34
- **Версия приложения**: 3.1 (versionCode: 241)
- **Компиляция Kotlin**: JVM Target 1.8
- **Compose**: Включена поддержка
- **ProGuard/R8**: Включена минификация для релизных сборок

## Ключевые компоненты

1. **JetpackWorkoutApplication**: Класс приложения, инициализирующий контейнер зависимостей
2. **MainActivity**: Главная активность, устанавливающая Compose контент
3. **RootScreen**: Корневой экран приложения, управляющий основной навигацией
4. **AppContainer**: Контейнер для управления зависимостями приложения

## Слой данных

- Использует Repository паттерн для управления источниками данных
- Локальное хранилище реализовано через Room Database
- Предпочтения хранятся в DataStore
- Сетевые запросы обрабатываются через Retrofit

## UI слой

- Построен полностью на Jetpack Compose
- Использует Material Design 3 компоненты
- Тема приложения определена в `ui.theme` пакете
- Навигация между экранами реализована через Navigation Compose

## Разрешения

Приложение запрашивает следующие разрешения:
- `INTERNET` - для сетевого взаимодействия
- `CAMERA` - для съемки фотографий
- `ACCESS_NETWORK_STATE` - для проверки состояния сети
- `ACCESS_FINE_LOCATION` и `ACCESS_COARSE_LOCATION` - для определения местоположения
- `READ_MEDIA_IMAGES` - для доступа к медиафайлам

## Сборка и разработка

Для сборки проекта используется Gradle с Kotlin DSL. Основные команды:
- `./gradlew build` - полная сборка проекта
- `./gradlew assembleDebug` - сборка отладочной версии
- `./gradlew installDebug` - установка отладочной версии на устройство