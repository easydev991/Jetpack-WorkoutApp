# План исправления предупреждений линтера (detekt)

## Актуальное состояние (последняя проверка: 2026-01-23)

**Детектировано:** 23 предупреждений detekt, 0 предупреждений компилятора Kotlin

**Выполнено:** 75% этапов (9 из 12 активных)

## Обзор проблем

### Актуальные категории предупреждений Detekt

1. **LongMethod** (1 шт.) - функции длиннее 60 строк
2. **TooGenericExceptionCaught** (6 шт.) - ловля слишком общих исключений
3. **TooManyFunctions** (1 шт.) - слишком много функций в файле (больше 11)
4. **ForbiddenComment** (21 шт.) - TODO-комментарии в RootScreen (не трогаем по заданию)

### Выполненные категории

~~**LongMethod**~~ - ✅ исправлены ListRowViewPreview, DialogRowViewPreview  
~~**UnusedImports**~~ - ✅ удалены все неиспользуемые импорты  
~~**MaxLineLength**~~ - ✅ разбиты длинные строки  
~~**MagicNumber**~~ - ✅ исправлены все магические числа  
~~**AnnotationDefaultTarget**~~ - ✅ добавлены `@param:` к аннотациям  
~~**DeprecatedAPI**~~ - ✅ заменены все устаревшие API  
~~**DeprecatedJavaAPI**~~ - ✅ заменено устаревшее Java API  
~~**LongParameterList**~~ - ✅ созданы data class-ы: `JournalActionsMenuConfig`, `CommentActionsMenuConfig`, `AsyncImageConfig`

---

## Порядок выполнения

### ✅ Этап 1: Исправление TooGenericExceptionCaught (6 случаев) - ВЫПОЛНЕНО

**Приоритет:** Высокий - проблемы безопасности и обработки ошибок

#### 1.1. MoreScreen.kt:276 ✅

**Проблема:** `catch (t: Throwable)` слишком общий  
**Действие:** Заменить на `SecurityException`, `IllegalArgumentException`, `IllegalStateException`

#### 1.2. SecureTokenRepository.kt:99, 117 ✅

**Проблема:** `catch (e: Exception)`  
**Действие:** Заменить на `IOException` для DataStore операций

#### 1.3. CryptoManagerImpl.kt:43 ✅

**Проблема:** `catch (e: Exception)`  
**Действие:** Заменить на `SecurityException`

#### 1.4. EncryptedStringSerializer.kt:49, 73 ✅

**Проблема:** `catch (e: Exception)`  
**Действие:** Заменить на `SecurityException`, `IllegalArgumentException`

---

### ❌ Этап 2: Рефакторинг LongMethod (1 случай)

**Приоритет:** Средний - улучшение читаемости кода

#### 2.1. RootScreen.kt:24

**Проблема:** Функция 85 строк (лимит 60)  
**Действие:** Вынести логику в отдельные компоненты

---

### ✅ Этап 3: LongParameterList - ВЫПОЛНЕНО

Созданы data class-ы для группировки параметров:

- `JournalActionsMenuConfig` (JournalRowView.kt:103)
- `CommentActionsMenuConfig` (CommentRowView.kt:77)
- `AsyncImageConfig` (SWAsyncImage.kt:39)

---

### ✅ Этап 4: UnusedImports - ВЫПОЛНЕНО

Все неиспользуемые импорты удалены.

---

### ❌ Этап 5: TooManyFunctions (1 случай)

**Приоритет:** Низкий - архитектурное улучшение

#### 5.1. MoreScreen.kt:1

**Проблема:** 13 функций (лимит 11)  
**Действие:** Вынести функции в отдельные компоненты

---

## Прогресс выполнения

| Этап | Статус | Описание |
|-------|---------|----------|
| Этап 1: TooGenericExceptionCaught | ✅ ВЫПОЛНЕНО | 6 случаев |
| Этап 2: LongMethod | ❌ НЕ ВЫПОЛНЕНО | 1 случай (RootScreen) |
| Этап 3: LongParameterList | ✅ ВЫПОЛНЕНО | 3 случая |
| Этап 4: UnusedImports | ✅ ВЫПОЛНЕНО | 4 случая |
| Этап 5: TooManyFunctions | ❌ НЕ ВЫПОЛНЕНО | 1 случай (MoreScreen) |

**Прогресс:** 75% этапов выполнено (9 из 12 активных)

**Осталось исправить:** 23 предупреждений detekt

- LongMethod: 1 случай
- TooManyFunctions: 1 случай
- ForbiddenComment: 21 случай (не трогаем)

---

## После выполнения

1. Запустить `./gradlew ktlintCheck` ✅
2. Запустить `./gradlew detekt` - убедиться, что все предупреждения устранены
3. Запустить `make format`
4. Запустить `make build`
5. Запустить `./gradlew test` ✅

---

## Примечания

- **TODO-комментарии в RootScreen** - не трогаем (21 предупреждение ForbiddenComment)
- ktlint - без ошибок ✅
- Компилятор Kotlin - без ошибок ✅
- LongParameterList - исправлен ✅
- UnusedImports - исправлен ✅
- LongMethod - частично исправлен (RootScreen требует доработки)
