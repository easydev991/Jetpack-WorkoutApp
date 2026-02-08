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

### Screens with TopAppBar

When a screen has TopAppBar, the safe area is handled automatically. TopAppBar itself determines the status bar height and adds necessary padding.

**Rules:**
- Do not specify the `windowInsets` parameter in TopAppBar
- For LazyColumn apply the entire `paddingValues` through the component modifier, not through `contentPadding`
- For Column use `fillMaxWidth()` instead of `fillMaxSize()` for correct safe area operation
- Always apply `paddingValues` to content so content is not overlapped by TopAppBar or BottomBar

### Screens without TopAppBar

If a screen does not have TopAppBar, you need to manually add a safe zone at the top so content does not go under the status bar. Use `WindowInsets.systemBars.top` for this.

**Rules:**
- Add `WindowInsets.systemBars.top` as separate padding
- Use `paddingValues` only for BottomNavigationBar (if present)
- Apply safe zone before applying `paddingValues`

**Note:** In the current project implementation, all screens use TopAppBar, so this scenario is not yet applied.

## Required Imports

For working with safe areas, the following imports are needed:

```kotlin
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.res.dimensionResource
```

## Common Mistakes

### Mistake 1: Using fillMaxSize() instead of fillMaxWidth() for Column

**Problem:** When using `fillMaxSize()` instead of `fillMaxWidth()` for Column, when rotating the screen, content overlaps with camera or speaker.

**Solution:** Use `fillMaxWidth()` instead of `fillMaxSize()` for Column with vertical scrolling.

### Mistake 2: Using contentPadding for LazyColumn with calculateTopPadding()

**Problem:** When using `calculateTopPadding()` in `contentPadding` LazyColumn, content goes under camera when rotating the screen.

**Solution:** Apply the entire `paddingValues` through the LazyColumn modifier, and use `contentPadding` only for horizontal padding and small visual padding at the top.

## Correctness Check

### Correct Implementation Criteria

When implementing safe areas, check the following criteria:

- TopAppBar does NOT have parameter `windowInsets = WindowInsets(top = 0)`
- RootScreen has `contentWindowInsets = WindowInsets(0, 0, 0, 0)`
- Screens with TopAppBar + LazyColumn use `Modifier.padding(paddingValues)` for LazyColumn
- Screens with TopAppBar + Column use `fillMaxWidth()` and `Modifier.padding(paddingValues)`
- Screens without TopAppBar manually add `WindowInsets.systemBars.top`

### Testing on device with screen notch

For full verification of correct safe area handling:

1. Open the app on a device with screen notch (notch, speaker, or camera)
2. Check that TopAppBar header does not go under camera or speaker
3. Check that content is not overlapped by TopAppBar (scroll list to top)
4. Check that content is not overlapped by BottomBar (scroll list to bottom)
5. Rotate screen 90 degrees and check that content does not go under camera/speaker
6. Rotate screen 270 degrees and check that content does not go under camera/speaker
7. Check that on screens without TopAppBar, content does not go under status bar
8. Test in portrait and landscape orientation

## References

Jetpack Compose documentation: https://developer.android.com/jetpack/compose/layouts/insets
