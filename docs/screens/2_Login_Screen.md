# LoginScreen (экран авторизации)

## Обзор

Экран авторизации позволяет войти в workout.su или восстановить пароль.

Источник истины для UI (iOS): `SwiftUI-WorkoutApp/SwiftUI-WorkoutApp/Screens/Common/LoginScreen.swift`.

## Готово

### Авторизация (проверено)

- Токен сохраняется (LoginUseCase, есть unit-тесты).
- Флаг авторизации сохраняется при успешном логине и сбрасывается при выходе (LogoutUseCase / SWRepository / UserPreferencesRepository).

### Реализованные компоненты

- Data: `SWRepository.resetPassword(login)`, `SecureTokenRepository`, `UserPreferencesRepository`, `AuthInterceptor` (401), `TokenInterceptor`.
- Domain: `LoginUseCase`, `LogoutUseCase`, `AuthViewModel` (база).
- UI (Design System): `SWTextField`, `SWButton`, `LoadingOverlayView`.
- Navigation/интеграция: маршрут LoginScreen и интеграция в root/profile, `loginAndLoadUserData()` (логин → сохранение токена → загрузка данных + retry).

### Этапы и тесты

- Этапы 1–4 завершены (Domain, локализация, UI, навигация/интеграция).
- Полноэкранное модальное отображение LoginScreen (ModalBottomSheet): закрытие только крестиком/после успеха, dismiss-жесты и back press запрещены.
- Unit-тесты: 29 (12 + 3 + 14).
- UI-тесты: `LoginScreenTest.kt` — 13 тестов.

## Осталось (по порядку)

1. Блокировка системного “назад” при загрузке (опционально).
2. Получение названий стран/городов из репозитория или кэша.
3. Проверить работу на устройстве/эмуляторе (в т.ч. поведение модального листа).

## Замечания (кратко)

- Null-safety: не использовать `!!`, применять `?`, `?:`, `checkNotNull`, корректно логировать ошибки.
- Логи на русском, уровни Error/Info/Debug.
- Сеть: проверять интернет перед запросом, показывать понятные ошибки, в use case’ах использовать `Result<T>`.
- После правок запускать `make format` и придерживаться стиля проекта; UI/логика должны соответствовать iOS-референсу.
