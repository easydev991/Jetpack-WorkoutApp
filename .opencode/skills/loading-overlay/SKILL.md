---
name: loading-overlay
description: Block UI screen content during data loading or asynchronous operations with loading indicator display. Use this skill when developing screens with network requests, CRUD operations, and other asynchronous actions.
---

# Content Blocking During Loading (Loading Overlay)

## When to Use

- Use this skill when developing a screen with loading data from server
- Use this skill when implementing CRUD operations (create, update, delete)
- Use this skill when you need to block buttons and interactive elements during processing
- Use this skill when user should see operation progress
- This skill is useful for preventing repeated user actions during waiting
- This skill provides consistent UX during data loading

## Core Approach

### Step 1: ViewModel - add loading state

In ViewModel, add `StateFlow<Boolean>` for tracking loading state:

```kotlin
private val _isProcessing = MutableStateFlow(false)
val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
```

In operation methods, set `_isProcessing.value = true` at the beginning and `_isProcessing.value = false` in the `finally` block.

### Step 2: UI - pass loading state

In Composable screen, collect state via `collectAsState()` and pass to the stateless component:

```kotlin
val isProcessing by viewModel.isProcessing.collectAsState()

MyFriendsScreenContent(
    // other parameters...
    isProcessing = isProcessing
)
```

### Step 3: Stateless component - blocking and indicator

In stateless component:
1. Pass `enabled = !isProcessing` to content components
2. Use `LoadingOverlayView` from design system over content during loading

```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    Content(enabled = !isProcessing)

    if (isProcessing) {
        LoadingOverlayView()
    }
}
```

**Note:** `LoadingOverlayView` automatically occupies the entire size of the parent Box and blocks all gestures. The component is located in `com.swparks.ui.ds`.

### Step 4: List components - enabled parameter

In list components, pass `enabled` to interactive elements:

```kotlin
Box(
    modifier = Modifier.clickable(enabled = enabled) { onClick() }
) {
    UserRowView(/* parameters */)
}
```

## Typical Usage Scenarios

### Scenario 1: Loading list from server

- User opens screen
- Show indicator in center (`UiState.Loading`)
- After successful loading show data (`UiState.Success`)

### Scenario 2: User action on list item

- User clicks button in list (accept, decline, delete)
- Block all list items (`enabled = false`)
- Show indicator over content (`isProcessing = true`)
- After successful execution update data and unblock (`isProcessing = false`)

### Scenario 3: Combining multiple loading states

- Initial data loading (`isLoading`)
- Executing asynchronous action (`isProcessing`)
- Combine both states via `combine` or in UI (`!isLoading && !isProcessing`)

## When to Use Different Indicator Variants

### 1. Initial Data Loading

Use regular `CircularProgressIndicator` or `LoadingIndicator` in the center of the screen in the `when` block for `UiState.Loading`.

### 2. Data Update or User Actions

Use `LoadingOverlayView` from design system over content in `Box` with `isProcessing`. This component automatically blocks all gestures and occupies the entire size of the parent container.

### 3. Indicator Inside Button

For buttons with CRUD operations, use built-in `CircularProgressIndicator` or `enabled` parameter.

## Key Implementation Points

### 1. Box with fillMaxSize for content

Always use `Box` with `Modifier.fillMaxSize()` for content so `LoadingOverlayView` can display over and correctly block all gestures.

### 2. LoadingOverlayView automatically occupies parent size

`LoadingOverlayView` from design system automatically occupies the parent `Box` size and blocks all gestures. No need to use `matchParentSize()` or `fillMaxSize()`.

### 3. try-finally for reliable blocking

Always use `try-finally` for guaranteed unlocking on successful execution or error.

### 4. Passing enabled to all interactive elements

Don't forget to pass `enabled` to all clickable elements: buttons, clickable Box/Row/Surface.

## Implementation Checklist

- [ ] Added `StateFlow<Boolean>` for loading state in ViewModel
- [ ] Wrapped asynchronous operations in `try-finally` for reliable blocking
- [ ] Collected loading state in Composable screen via `collectAsState()`
- [ ] Passed `enabled = !isProcessing` to stateless component
- [ ] Added `LoadingOverlayView()` from design system over content during loading
- [ ] Passed `enabled` to interactive list elements (buttons, clickable areas)
- [ ] Checked that overlay displays over content during loading
- [ ] Checked that buttons are blocked and not clickable during processing
- [ ] Tested error handling (user sees error, overlay hides)
