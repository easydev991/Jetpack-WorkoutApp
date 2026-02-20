# Исправление предупреждений линтера (detekt)

## Текущее состояние

**Detekt:** 23 предупреждения (LongMethod: 1, TooManyFunctions: 1, ForbiddenComment: 21)
**ktlint:** 0 ошибок ✅
**Компилятор Kotlin:** 0 ошибок ✅

---

## Завершённые этапы

| Этап | Статус |
|------|--------|
| TooGenericExceptionCaught (6 случаев) | ✅ |
| LongParameterList (3 случая) | ✅ Созданы data class-ы |
| UnusedImports | ✅ |
| MaxLineLength | ✅ |
| MagicNumber | ✅ |
| AnnotationDefaultTarget | ✅ |
| DeprecatedAPI | ✅ |
| DeprecatedJavaAPI | ✅ |

---

## Невыполненные задачи

### Этап 2: LongMethod (1 случай)

- `RootScreen.kt:24` — функция 85 строк (лимит 60)
- Вынести логику в отдельные компоненты

### Этап 5: TooManyFunctions (1 случай)

- `MoreScreen.kt` — 13 функций (лимит 11)
- Вынести функции в отдельные компоненты

### ForbiddenComment (21 случай)

- TODO-комментарии в RootScreen — не трогаем по заданию

---

## После исправления

1. `./gradlew ktlintCheck`
2. `./gradlew detekt`
3. `make format`
4. `make build`
