---
name: jetpack-compose-safe-areas
description: Correct handling of safe areas in Jetpack Compose Android applications. Use when working with TopAppBar, Scaffold, and screen safe areas in Jetpack-WorkoutApp.
---

# Safe Areas in Jetpack Compose (Jetpack-WorkoutApp)

## When to Use

- Use this skill when working with Scaffold and TopAppBar in Compose in Jetpack-WorkoutApp
- Use this skill when creating new screens with or without top bars
- Use this skill when content is overlapped by TopAppBar or BottomBar
- Use this skill when you need to ensure correct display on devices with screen notch
- Use this skill when content goes under status bar or system elements
- This skill is useful when refactoring existing screens for correct safe area handling
- This skill helps avoid common mistakes when working with windowInsets

## Core Principle

In Jetpack Compose, the `TopAppBar` component automatically handles safe areas (status bar insets) and requires no additional configuration. This means that with proper implementation, the screen header will not be overlapped by camera, speaker, or other system elements on devices with screen notch.

**Key rule:** NEVER use `windowInsets = WindowInsets(top = 0)` in TopAppBar. This disables automatic safe area handling and causes content overlap.

## Safe Areas Architecture

### MainActivity

In MainActivity, `enableEdgeToEdge()` is used, which forces the app to occupy the entire screen, including areas under system bars. This requires special inset configuration at the Scaffold level.

### Root Scaffold (RootScreen)

In the root screen of the application (RootScreen), a Scaffold with BottomNavigationBar is used. It is important to disable automatic content padding through `contentWindowInsets = WindowInsets(0, 0, 0, 0)` so content does not receive extra padding. The `paddingValues` parameter is only used to account for BottomNavigationBar.

**Rules:**
- In root Scaffold always disable automatic content padding through `contentWindowInsets = WindowInsets(0, 0, 0, 0)`
- The `paddingValues` parameter is only applied to NavHost to account for BottomNavigationBar
- On individual screens, safe area is handled separately through TopAppBar or manually

## Standard Screen Structure

### Screen with TopAppBar (Recommended Pattern)

Use this pattern for screens with scrolling content and optional bottom button:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExampleScreen(
    modifier: Modifier = Modifier,
    viewModel: IExampleViewModel,
    onAction: (ExampleNavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.title)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(ExampleNavigationAction.Back) }) {
                        Icon(
                            imageVector = AutoMirroredIcons.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Optional bottom button
            SaveButton(
                enabled = uiState.canSave && !uiState.isLoading,
                onClick = viewModel::onSaveClick
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(
                        horizontal = dimensionResource(R.dimen.spacing_regular),
                        vertical = dimensionResource(R.dimen.spacing_regular)
                    ),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
            ) {
                // Screen content
            }

            // Loading overlay
            if (uiState.isLoading) {
                LoadingOverlayView()
            }
        }
    }
}
```

### Key Implementation Rules

1. **TopAppBar without windowInsets parameter** - TopAppBar handles safe area automatically
2. **Use fillMaxSize() for Column** - Apply `paddingValues` through `.padding(paddingValues)` modifier
3. **Box wrapper for overlays** - Wrap Column in Box to enable loading overlays
4. **bottomBar for action buttons** - Place primary action buttons in `bottomBar` slot

## Required Imports

```kotlin
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.res.dimensionResource
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CenterAlignedTopAppBar
```

## Common Mistakes

### Mistake 1: Overriding windowInsets in TopAppBar

**Problem:** Using `windowInsets = WindowInsets(top = 0)` disables automatic safe area handling.

**Solution:** Do not specify the `windowInsets` parameter in TopAppBar.

### Mistake 2: Not applying paddingValues to content

**Problem:** Content goes under TopAppBar or BottomBar.

**Solution:** Always apply `paddingValues` to the content Column: `Modifier.fillMaxSize().padding(paddingValues)`

### Mistake 3: Forgetting Box wrapper for loading overlay

**Problem:** Loading overlay cannot be displayed on top of scrollable content.

**Solution:** Wrap the content Column in a Box and place the loading overlay inside the Box after the Column.

## Correctness Check

### Correct Implementation Criteria

- [ ] TopAppBar does NOT have parameter `windowInsets = WindowInsets(top = 0)`
- [ ] RootScreen has `contentWindowInsets = WindowInsets(0, 0, 0, 0)`
- [ ] Screen content uses `Modifier.fillMaxSize().padding(paddingValues)`
- [ ] Content is wrapped in Box if loading overlay is needed
- [ ] Primary action buttons are placed in `bottomBar` slot
- [ ] `verticalScroll` is applied to Column, not to Box

### Testing on device with screen notch

For full verification of correct safe area handling:

1. Open the app on a device with screen notch (notch, speaker, or camera)
2. Check that TopAppBar header does not go under camera or speaker
3. Check that content is not overlapped by TopAppBar (scroll list to top)
4. Check that content is not overlapped by BottomBar (scroll list to bottom)
5. Rotate screen 90 degrees and check that content does not go under camera/speaker
6. Rotate screen 270 degrees and check that content does not go under camera/speaker
7. Test in portrait and landscape orientation

## Reference Examples

See for correct implementation:
- `EditProfileScreen.kt` - Screen with TopAppBar, bottomBar, loading overlay
- `EventFormScreen.kt` - Screen with TopAppBar, bottomBar, loading overlay

## References

Jetpack Compose documentation: https://developer.android.com/jetpack/compose/layouts/insets
