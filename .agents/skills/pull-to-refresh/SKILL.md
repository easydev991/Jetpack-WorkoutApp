---
name: pull-to-refresh
description: Implement pull-to-refresh in Jetpack Compose using Material 3 PullToRefreshBox. Use this skill when adding data refresh capability on screens with lists or cards.
---

# Pull-to-Refresh in Jetpack Compose

## When to Use

- Use this skill when adding data refresh capability on screens
- Use this skill when screen contains lists or cards with data
- Use this skill when user should be able to refresh content without navigating to another screen
- Use this skill when data can become stale and requires periodic updates
- This skill is useful when implementing profiles, park lists, events, and other data screens

## Architecture

### ViewModel Layer

**Required components:**

1. **Refresh state** (`_isRefreshing`) - separate StateFlow for managing loading state
2. **Refresh method** (e.g., `refreshProfile()`) - calls data loading from server
3. **Logging** - recording refresh operations for debugging
4. **Error handling** - errors during refresh should not change main UI State

**Important rules:**

- Use separate `isRefreshing` StateFlow instead of main `uiState`
- Data refresh should NOT change main `uiState` (use `updateUiState = false` parameter)
- Always reset `_isRefreshing` in `finally` block
- Log start and end of refresh operation

### UI Layer

**Required components:**

1. **PullToRefreshBox** - Material 3 container for pull-to-refresh
2. **rememberPullToRefreshState** - pull-to-refresh gesture state
3. **PullToRefreshDefaults.Indicator** - refresh indicator
4. **Vertical scrolling** - content must support vertical scrolling

**Important rules:**

- Wrap all scrollable content in `PullToRefreshBox`
- Use `rememberScrollState` for `verticalScroll`
- Configure indicator positioning with padding from safe areas
- Subscribe to `isRefreshing` from ViewModel

## Instructions

### Step 1: Add refresh state to ViewModel

Create separate StateFlow for refresh state:

```kotlin
// Data refresh state (pull-to-refresh)
private val _isRefreshing = MutableStateFlow(false)
val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
```

### Step 2: Create data refresh method

Add a method that updates data from server:

```kotlin
/**
 * Refreshes data from server (for pull-to-refresh).
 */
fun refreshData() {
    val data = currentData.value ?: run {
        logger.w(TAG, "Skipping refresh: data is missing")
        return
    }

    viewModelScope.launch {
        try {
            _isRefreshing.update { true }
            logger.i(TAG, "Starting data refresh: ${data.id}")

            // Load data from server
            loadDataFromServer(data.id, updateUiState = false)
        } catch (e: Exception) {
            val errorMessage = "Error refreshing data: ${e.message}"
            logger.e(TAG, errorMessage)
        } finally {
            _isRefreshing.update { false }
        }
    }
}
```

### Step 3: Update UI for pull-to-refresh

Use `PullToRefreshBox` to wrap content:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScreen(
    modifier: Modifier = Modifier,
    viewModel: MyViewModel,
) {
    val scope = rememberCoroutineScope()

    // Get refresh state (isRefreshing)
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    // State for pull-to-refresh
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
            // Your content here
        }
    }
}
```

## Required Imports

For using pull-to-refresh, the following imports are needed:

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

### Mistake 1: Using main uiState for isRefreshing

**Problem:** Data refresh changes main UI State, causing entire screen redraw.

**Solution:** Use separate `isRefreshing` StateFlow for managing loading state.

### Mistake 2: Missing finally block

**Problem:** If error occurs during refresh, `isRefreshing` state remains `true` forever.

**Solution:** Always reset `_isRefreshing` in `finally` block.

### Mistake 3: Missing vertical scrolling

**Problem:** Pull-to-refresh doesn't work because content doesn't support scrolling.

**Solution:** Wrap content in `Column` with `verticalScroll(rememberScrollState())`.

### Mistake 4: Incorrect indicator positioning

**Problem:** Indicator is overlapped by TopAppBar or other elements.

**Solution:** Use `top = dimensionResource(R.dimen.spacing_regular)` for indicator padding.

## Testing

### ViewModel Unit Tests

Test the data refresh method:

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
