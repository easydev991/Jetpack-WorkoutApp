---
name: pull-to-refresh
description: Реализуй pull-to-refresh в Jetpack Compose с использованием Material 3 PullToRefreshBox. Используй этот навык при добавлении возможности обновления данных на экранах со списками или карточками.
---

# Pull-to-Refresh в Jetpack Compose

## When to Use

- Используй этот навык, когда добавляешь возможность обновления данных на экранах
- Используй этот навык, когда экран содержит списки или карточки с данными
- Используй этот навык, когда пользователь должен иметь возможность обновить контент без перехода на другой экран
- Используй этот навык, когда данные могут устаревать и требуют периодического обновления
- Этот навык полезен при реализации профилей, списков площадок, мероприятий и других экранов с данными

## Architecture

### ViewModel Layer

**Обязательные компоненты:**

1. **Состояние обновления** (`_isRefreshing`) - отдельное StateFlow для управления состоянием загрузки
2. **Метод обновления** (например, `refreshProfile()`) - вызывает загрузку данных с сервера
3. **Логирование** - запись операций обновления для отладки
4. **Обработка ошибок** - ошибки при обновлении не должны менять основной UI State

**Важные правила:**

- Используй отдельный `isRefreshing` StateFlow вместо основного `uiState`
- Обновление данных НЕ должно менять основной `uiState` (используй параметр `updateUiState = false`)
- Всегда сбрасывай `_isRefreshing` в `finally` блоке
- Логируй начало и конец операции обновления

### UI Layer

**Обязательные компоненты:**

1. **PullToRefreshBox** - контейнер Material 3 для pull-to-refresh
2. **rememberPullToRefreshState** - состояние жеста pull-to-refresh
3. **PullToRefreshDefaults.Indicator** - индикатор обновления
4. **Вертикальная прокрутка** - контент должен поддерживать вертикальную прокрутку

**Важные правила:**

- Оберни весь прокручиваемый контент в `PullToRefreshBox`
- Используй `rememberScrollState` для `verticalScroll`
- Настрой позиционирование индикатора с отступами от безопасных зон
- Подписывайся на `isRefreshing` из ViewModel

## Instructions

### Шаг 1: Добавь состояние обновления в ViewModel

Создай отдельное StateFlow для состояния обновления:

```kotlin
// Состояние обновления данных (pull-to-refresh)
private val _isRefreshing = MutableStateFlow(false)
val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
```

### Шаг 2: Создай метод обновления данных

Добавь метод, который обновляет данные с сервера:

```kotlin
/**
 * Обновляет данные с сервера (для pull-to-refresh).
 */
fun refreshData() {
    val data = currentData.value ?: run {
        logger.w(TAG, "Пропускаем обновление: данные отсутствуют")
        return
    }

    viewModelScope.launch {
        try {
            _isRefreshing.update { true }
            logger.i(TAG, "Начало обновления данных: ${data.id}")

            // Загрузка данных с сервера
            loadDataFromServer(data.id, updateUiState = false)
        } catch (e: Exception) {
            val errorMessage = "Ошибка обновления данных: ${e.message}"
            logger.e(TAG, errorMessage)
        } finally {
            _isRefreshing.update { false }
        }
    }
}
```

### Шаг 3: Обнови UI для pull-to-refresh

Используй `PullToRefreshBox` для обертки контента:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScreen(
    modifier: Modifier = Modifier,
    viewModel: MyViewModel,
) {
    val scope = rememberCoroutineScope()

    // Получаем состояние обновления (isRefreshing)
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    // Состояние для pull-to-refresh
    val pullRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshData() },
        state = pullRefreshState,
        modifier = modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = dimensionResource(R.dimen.spacing_regular))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(R.dimen.spacing_regular)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
        ) {
            // Твой контент здесь
        }
    }
}
```

## Required Imports

Для использования pull-to-refresh необходимы следующие импорты:

### ViewModel

```kotlin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
```

### UI (Compose)

```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
```

## Common Mistakes

### Ошибка 1: Использование основного uiState для isRefreshing

**Проблема:** Обновление данных меняет основной UI State, что вызывает перерисовку всего экрана.

**Решение:** Используй отдельное `isRefreshing` StateFlow для управления состоянием загрузки.

### Ошибка 2: Отсутствие finally блока

**Проблема:** Если при обновлении происходит ошибка, состояние `isRefreshing` остается `true` навсегда.

**Решение:** Всегда сбрасывай `_isRefreshing` в `finally` блоке.

### Ошибка 3: Отсутствие вертикальной прокрутки

**Проблема:** Pull-to-refresh не работает, потому что контент не поддерживает прокрутку.

**Решение:** Оберни контент в `Column` с `verticalScroll(rememberScrollState())`.

### Ошибка 4: Неверное позиционирование индикатора

**Проблема:** Индикатор перекрывается TopAppBar или другими элементами.

**Решение:** Используй `top = dimensionResource(R.dimen.spacing_regular)` для отступа индикатора.

## Testing

### Unit-тесты ViewModel

Тестируй метод обновления данных:

```kotlin
@Test
fun refreshData_whenCalled_thenUpdatesIsRefreshing() = runTest {
    // Given
    mainDispatcherRule.advanceUntilIdle()

    // When
    viewModel.refreshData()
    advanceUntilIdle()

    // Then
    assertEquals(false, viewModel.isRefreshing.value)
}
```

## Ссылки на примеры

Более подробные примеры кода см. в файле [references/EXAMPLES.md](references/EXAMPLES.md).

### Доступные примеры

- **Полная реализация ProfileViewModel** - пример с состоянием isRefreshing и методом refreshProfile()
- **Полная реализация ProfileRootScreen** - пример с PullToRefreshBox и индикатором обновления
- **Пример для списков (LazyColumn)** - адаптация pull-to-refresh для списков
