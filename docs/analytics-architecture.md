# Firebase Analytics Architecture

Документ фиксирует фактически реализованную архитектуру и правила аналитики в `Jetpack-WorkoutApp`.

## 1. Назначение

- Аналитика используется как технические breadcrumbs для диагностики сценариев и ошибок.
- Цель: понимать, какие экраны открывались, какие ключевые действия выполнялись и какие ошибки (`app_error`) происходили до проблемного состояния.
- Продуктовая персонализация и сбор персональных данных не являются целью.

## 2. Архитектура

- Единая модель событий: `AnalyticsEvent` (`sealed interface`)
  - `ScreenView(screen: AppScreen, source: AppScreen? = null)`
  - `UserAction(action: UserActionType, params: Map<String, String> = emptyMap())`
  - `AppError(operation: AppErrorOperation, throwable: Throwable)`
- Провайдерная схема:
  - `AnalyticsProvider` — контракт провайдера
  - `FirebaseAnalyticsProvider` — отправка в Firebase Analytics и прокидывание ошибок в `CrashReporter`
  - `NoopAnalyticsProvider` — заглушка для debug/test
- `AnalyticsService` выполняет fan-out по всем провайдерам.
- Ошибка одного провайдера не ломает отправку в остальные: исключения локально перехватываются внутри `AnalyticsService`.

## 3. DI и окружения

- `AnalyticsService` создается в `DefaultAppContainer` как singleton.
- `CrashReporter` создается в `DefaultAppContainer` как singleton (`FirebaseCrashReporter`) и передается в `FirebaseAnalyticsProvider`.
- Выбор провайдера зависит от сборки:
  - `BuildConfig.DEBUG = true` -> `NoopAnalyticsProvider`
  - иначе -> `FirebaseAnalyticsProvider`
- Сервис передается:
  - в UI через `rememberAppState(analyticsService = ...)`, после чего хранится в `AppState` (`appState.analyticsService`)
  - в ViewModel через конструкторы/фабрики `AppContainer`.
- Прямые вызовы Firebase SDK из экранов и ViewModel не используются.

## 4. Модель данных событий

- `AppScreen`: 30 экранов.
- `UserActionType`: 45 действий.
- `AppErrorOperation`: 26 операций.

Параметры `AppError` в Firebase:
- `operation`
- `error_domain` (имя класса исключения)
- `error_code` (hashCode исключения)

## 5. Реализованные правила трекинга

- Навигация в Android реализована в click-first подходе:
  - `ScreenView` отправляется при пользовательском действии (клик/переход), а не через `onAppear`-подход.
  - Для tab-переходов `ScreenView` отправляется в `AppState.navigateToTopLevelDestination`.
- `UserAction` отправляется в точке пользовательского действия (до выполнения долгих/потенциально падающих операций).
- `AppError` отправляется в error-ветках (`catch`/`onFailure`) ViewModel и use case.
- Для `AnalyticsEvent.AppError` в `FirebaseAnalyticsProvider` дополнительно вызывается `crashReporter.logException(event.throwable, event.operation.value)`.

## 6. Текущее покрытие

- Этапы 1-8 плана аналитики реализованы.
- Покрыты:
  - `ScreenView` для основных переходов и tab-навигации.
  - `UserAction` для основных пользовательских действий по модулям auth/profile/social/messages/parks/events/journals/settings/more.
  - `AppError` для 26 операций ошибок по ключевым веткам ViewModel/use case.
- Добавлены unit-тесты для ViewModel-сценариев с `UserAction` и `AppError`.

## 7. Приватность

- В аналитику не передаются PII:
  - email/телефон/пароль/токены
  - текст сообщений и комментариев
  - любые прямые пользовательские идентификаторы и персональные данные
- Разрешены технические и сценарные параметры:
  - `screen`, `action`, `operation`
  - `source`, `type`, `size`, `icon_name`, `theme`
  - `country_id`, `city_id`, `park_id` и аналогичные не-PII идентификаторы предметной области.

## 8. Проверка перед релизом

- `make format`
- `make lint`
- `make test`
- Smoke-проверка ключевых сценариев (авторизация, навигация, базовые CRUD-действия).

## 9. Ключевые файлы

- `app/src/main/java/com/swparks/analytics/AnalyticsEvent.kt`
- `app/src/main/java/com/swparks/analytics/AnalyticsProvider.kt`
- `app/src/main/java/com/swparks/analytics/AnalyticsService.kt`
- `app/src/main/java/com/swparks/analytics/FirebaseAnalyticsProvider.kt`
- `app/src/main/java/com/swparks/analytics/NoopAnalyticsProvider.kt`
- `app/src/main/java/com/swparks/analytics/AppScreen.kt`
- `app/src/main/java/com/swparks/analytics/UserActionType.kt`
- `app/src/main/java/com/swparks/analytics/AppErrorOperation.kt`
- `app/src/main/java/com/swparks/data/AppContainer.kt`
- `app/src/main/java/com/swparks/navigation/AppState.kt`
- `app/src/main/java/com/swparks/util/CrashReporter.kt`
- `app/src/main/java/com/swparks/util/crash/FirebaseCrashReporter.kt`
