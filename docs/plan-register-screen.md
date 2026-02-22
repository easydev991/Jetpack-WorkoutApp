# План реализации экрана регистрации (RegisterUserScreen)

## Описание задачи

Реализовать экран регистрации `RegisterUserScreen` в Android-приложении по аналогии с iOS (коммит `990c88c0`, экран `AccountInfoView(mode: .create)`).

---

## Выполненные этапы ✅

### Базовая реализация (Этапы 1-8)

- Строковые ресурсы EN/RU, ViewModel, UI экран с формой и навигацией в sheet
- DI factory, интеграция в RootScreen, тесты RegisterViewModelTest

### UI и валидация (Этапы 9-13)

- Блокировка свайпа sheet, загрузка стран, выбор города с авто-выбором страны
- Валидация пароля (6+ символов), email и логина на лету

### API и обработка ошибок (Этапы 14-16)

- `@FormUrlEncoded` с `@Field` вместо JSON
- UserNotifier для ошибок в Snackbar

### Оптимизация (Этапы 17-19)

- `SWApi.register()` возвращает `User`, сохраняется в базу при регистрации
- SnackbarHost внутри ModalBottomSheet, профиль без лишнего API запроса

---

## Реализованные файлы

- **UI Models & State**: `RegisterForm.kt`, `RegisterUiState.kt`, `RegisterEvent.kt`
- **ViewModel**: `IRegisterViewModel.kt`, `RegisterViewModel.kt`
- **UI Screens**: `RegisterUserScreen.kt`, `RegisterSheetHost.kt`, `RegisterSelectCountryScreen.kt`, `RegisterSelectCityScreen.kt`
- **Tests**: `RegisterViewModelTest.kt`

---

## Критерии приемки

- [x] Экран открывается, поля валидируются, регистрация работает
- [x] Токен сохраняется, профиль загружается без лишнего запроса
- [x] Навигация страна/город, sheet нельзя закрыть свайпом
- [x] Ошибки отображаются в Snackbar

---

## Задача завершена ✅

**Дата завершения:** Февраль 2026
