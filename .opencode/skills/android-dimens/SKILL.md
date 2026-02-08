---
name: android-dimens
description: Guide using dimension resources for UI sizes, spacings, and paddings in Android Compose. Use when creating or modifying UI components, replacing hardcoded dp values, or ensuring consistency across Compose screens.
---

# Android Compose Dimens Usage

## When to Use

- Use this skill when creating or modifying UI components in Compose
- Use this skill when replacing hardcoded `*.dp` values with dimension resources
- Use this skill when adding new sizes, paddings, or spacing
- Use this skill when ensuring size consistency between screens
- Use this skill when refactoring existing code to meet standards
- This skill is useful when laying out new screens and design system components

## Instructions

### Required Import

```kotlin
import androidx.compose.ui.res.dimensionResource
import com.swparks.R
```

### Core Principles

#### 1. Never Hardcode Sizes

All paddings, sizes, spacings, and dimensions must come from `app/src/main/res/values/dimens.xml`.

**Bad:**

```kotlin
Modifier.padding(16.dp)
Modifier.size(32.dp)
Arrangement.spacedBy(8.dp)
```

**Good:**

```kotlin
Modifier.padding(horizontal = dimensionResource(id = R.dimen.spacing_regular))
Modifier.size(dimensionResource(id = R.dimen.icon_size_small))
Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_xsmall))
```

#### 2. Standard Spacing Hierarchy

Use existing spacing tokens from `dimens.xml`:

```kotlin
// Examples of existing spacings:
dimensionResource(id = R.dimen.spacing_xxsmall)  // 4dp
dimensionResource(id = R.dimen.spacing_regular)  // 16dp - standard spacing (most common)
dimensionResource(id = R.dimen.spacing_large)    // 24dp - large spacing
```

#### 3. Icon Sizes

Use existing icon size tokens from `dimens.xml`:

```kotlin
// Examples of existing icon sizes:
dimensionResource(id = R.dimen.icon_size_small)  // 32dp
dimensionResource(id = R.dimen.icon_size_medium) // 42dp
dimensionResource(id = R.dimen.icon_size_large)  // 48dp
```

#### 4. Common Sizes

Use existing common size tokens from `dimens.xml`:

```kotlin
// Examples of existing common sizes:
dimensionResource(id = R.dimen.size_small)  // 32dp
dimensionResource(id = R.dimen.size_medium) // 48dp
dimensionResource(id = R.dimen.size_large)  // 80dp
```

#### 5. Border and Elevation

Use existing border and elevation tokens from `dimens.xml`:

```kotlin
// Examples of existing values:
dimensionResource(id = R.dimen.border_width_small) // 2dp
dimensionResource(id = R.dimen.elevation_none)      // 0dp
dimensionResource(id = R.dimen.elevation_small)     // 2dp
dimensionResource(id = R.dimen.elevation_medium)    // 4dp
```

#### 6. Corner Radii

Use existing corner radius tokens from `dimens.xml`:

```kotlin
// Examples of existing corner radii:
dimensionResource(id = R.dimen.corner_radius_none)   // 0dp
dimensionResource(id = R.dimen.corner_radius_small)  // 8dp
dimensionResource(id = R.dimen.corner_radius_bubble) // 20dp
```

#### 7. Component-Specific Dimensions

Use existing component-specific tokens from `dimens.xml`:

```kotlin
// Examples of existing component-specific values:
dimensionResource(id = R.dimen.bubble_horizontal_margin)  // 40dp
dimensionResource(id = R.dimen.bubble_padding_horizontal) // 18dp
```

### When to Add New Dimensions

Only add new dimensions when:

1. No existing dimension fits use case
2. Value is repeated multiple times (3+ occurrences)
3. Value has semantic meaning in design (e.g., specific corner radius, bubble margin)

**Naming and sorting rules:**

- **Refer to existing values**: when choosing a name, orient yourself to already existing sizes in this category
- **Logical sorting in dimens.xml**: add and sort new values by numeric value, not alphabetically

**Example**: if `dimens.xml` already has `spacing_regular = 16dp` and `spacing_large = 24dp`, and you need to add 20dp:

- Good: use existing `spacing_medium = 20dp` (if available)
- Good: if not available, create `spacing_medium = 20dp` and place it by numeric value between `regular` and `large`
- Bad: name it `spacing_regularplus` — illogical, as 20 is exactly halfway between 16 and 24

**Examples of logical naming**:

```xml
<!-- Check existing values in dimens.xml -->
<dimen name="spacing_regular">16dp</dimen>

<!-- If you need 20dp - check if spacing_medium exists -->
<dimen name="spacing_medium">20dp</dimen>  ← use or create by numeric value

<dimen name="spacing_large">24dp</dimen>
```

**Naming conventions:**

- Spacings: `spacing_<semantic>` (e.g., `spacing_regular`, `spacing_medium`, `spacing_micro`)
- Icons: `icon_size_<semantic>` or `icon_<semantic>` (e.g., `icon_size_small`, `icon_size_avatar`)
- Corners: `corner_radius_<semantic>` (e.g., `corner_radius_small`, `corner_radius_bubble`)
- Component-specific: `<component>_<property>` (e.g., `bubble_horizontal_margin`)

### What to Refactor

#### Must Refactor

- All `Modifier.padding(...)` with hardcoded values
- All `Modifier.size(...)` with hardcoded values
- All `Arrangement.spacedBy(...)` with hardcoded values
- All `RoundedCornerShape(...)` with hardcoded values
- Any `*.dp` value in Compose UI code (except Preview functions)

#### Do Not Refactor

- Hardcoded values in **parameter definitions** (if passed from outside)
- Hardcoded values in **data class configs** (if set by caller)
- Hardcoded values in **Preview functions** (if Preview-specific)
- Configuration parameters that should be **customizable by caller**

### Implementation Checklist

When replacing hardcoded values:

- [ ] Add import `androidx.compose.ui.res.dimensionResource`
- [ ] Add import `com.swparks.R` (if not already present)
- [ ] Check if existing dimension fits (reuse when possible)
- [ ] If no existing dimension, add to `dimens.xml` first
- [ ] Replace hardcoded value with `dimensionResource(id = R.dimen.<name>)`
- [ ] Run `make format` after changes
- [ ] Verify project builds: `./gradlew build`

## Common Patterns

### Padding on all sides

```kotlin
Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(dimensionResource(id = R.dimen.spacing_regular))
) {
    // Content with padding on all sides
}
```

### Padding on specific sides

```kotlin
Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(
            horizontal = dimensionResource(id = R.dimen.spacing_regular),
            vertical = dimensionResource(id = R.dimen.spacing_small)
        )
) {
    // Content with horizontal and vertical padding
}
```

### Spacing in arrangements

```kotlin
Column(
    verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_small))
) {
    // Items with spacing between them
}
```

### Size modifiers

```kotlin
Icon(
    imageVector = Icons.Default.Person,
    contentDescription = null,
    modifier = Modifier.size(dimensionResource(id = R.dimen.icon_size_medium))
)
```

### Corner shapes

```kotlin
Card(
    shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_small))
) {
    // Card content
}
```

## Additional Examples

- Complete example: Composable with dimens
- Using different icon sizes
- Spacing hierarchy examples
- Border and elevation examples
- Component-specific dimensions (message bubble)
- LazyColumn spacing
- Button sizing
