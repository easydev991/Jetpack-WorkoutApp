# План: Загрузка списка диалогов на экране Messages

## Статус: ✅ ВЫПОЛНЕНО

**Дата завершения:** 13 февраля 2026

---

## Реализация

**Архитектура:** MVVM + Fallback cache (сервер → Room → UI, офлайн-режим)

**Data Layer:** `DialogEntity`, `DialogDao`, `MessagesRepository`, миграция БД v2

**UI Layer:** `DialogsViewModel` с `DialogsUiState`, `MessagesRootScreen`, `DialogRowView`

**Тесты:** 11 unit-тестов ViewModel, 5 unit-тестов репозитория

---

## Исправленные баги (8)

| # | Проблема | Решение |
|---|----------|---------|
| 1 | Диалоги не удаляются при logout | `dialogDao.deleteAll()` в `clearUserData()` |
| 2 | Дата не форматируется | `DateFormatter.formatDate()` |
| 3 | EmptyStateView не по центру | `Box` + `contentAlignment = Center` |
| 4 | Error state не по центру | `Box` + `contentAlignment = Center` |
| 5 | Pull to refresh не работает при пустом списке | `verticalScroll(rememberScrollState())` |
| 6 | Диалоги не загружаются при повторной авторизации | `LaunchedEffect(appState.isAuthorized)` |
| 7 | Не показывается LoadingOverlayView после авторизации | `isLoadingProfile`/`isLoadingDialogs` флаги + `loadDialogsAfterAuth()` |
| 8 | При 0 диалогах от сервера показывается Error вместо EmptyStateViewForDialogs | Явная установка `Success(emptyList())` при успехе в `loadDialogsInternal()` |

---

## Ключевые файлы

```
data/database/entity/DialogEntity.kt
data/database/dao/DialogDao.kt
domain/repository/MessagesRepository.kt
data/repository/MessagesRepositoryImpl.kt
data/model/DialogResponse.kt
ui/viewmodel/DialogsViewModel.kt
ui/screens/messages/MessagesRootScreen.kt
ui/ds/DialogRowView.kt
```

---

## Примечания

- **Pull-to-refresh:** Material3 `PullToRefreshBox`
- **Безопасные зоны:** `parentPaddingValues` от `RootScreen`
