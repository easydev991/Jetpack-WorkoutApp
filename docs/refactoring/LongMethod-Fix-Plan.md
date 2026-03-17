# План исправления предупреждений LongMethod

## Общая информация

- **Всего предупреждений**: 0 (изначально 29, все исправлены)
- **Максимальная длина метода (по правилам)**: 60 строк
- **Стратегия**: Извлечение логических блоков в отдельные composable-функции или приватные функции

---

## Этап 1-3: Критические, высокие и средние методы ✅

- [x] Этап 1: 10 экранов (>100 строк) — извлечены composables, state-классы, params-классы
- [x] Этап 2: 7 экранов (80-100 строк) — EventsScreen, ProfileContent, MessagesRootScreen, SearchUserScreenContent, SuccessContent, MyBlacklistScreenContent, ChangePasswordScreen
- [x] Этап 3: 4 файла (70-80 строк) — SWDateTimePicker, PhotoDetailSheetHost, TextEntryViewModel.onSend, RegisterSheetHost

---

## Этап 4: Низкие методы (60-70 строк) ✅

- [x] ChatContent, LoginScreen, PhotoDetailScreen, TextEntryScreen — соответствуют лимиту
- [x] TextEntrySheetHost — извлечён `SheetContent` + `SheetContentParams`
- [x] EventHeaderMapCalendarSection — извлечён `EventHeaderContent`
- [x] ParticipantsScreen — извлечён `ParticipantsContent`
- [x] JournalsList — извлечён `JournalItem`
- [x] JournalEntriesScreen — извлечены `ScaffoldContent`, `rememberScaffoldState`, `ScaffoldStateParams`

---

## Этап 5: Финальная проверка

- [x] `make lint` - все LongMethod предупреждения устранены (0 warnings)
- [x] `make test` - тесты проходят
- [ ] Запустить приложение и проверить UI визуально на ключевых экранах

---

## Итоговое состояние

**Остальные detekt предупреждения** (32 total):
- LongParameterList: 11
- TooManyFunctions: 9
- InstanceOfCheckForException: 5
- MaxLineLength: 2
- прочие: 5

---

## Правила рефакторинга

1. **Один экран = одна главная composable-функция**, которая собирает части
2. **Извлечённая функция должна иметь понятное имя**, отражающее её назначение
3. **Передавать только необходимые параметры**, избегая передачи всего состояния
4. **Сохранять `Modifier` как первый параметр** для composables
5. При большом количестве параметров создавать data class для группировки
