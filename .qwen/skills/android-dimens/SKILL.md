---
name: android-dimens
description: Guide using dimension resources for UI sizes, spacings, and paddings in Android Compose. Use when creating or modifying UI components, replacing hardcoded dp values, or ensuring consistency across Compose screens.
---

# Android Compose Dimens Usage

## When to Use

- Используй этот навык, когда создаешь или модифицируешь UI компоненты в Compose
- Используй этот навык, когда заменяешь хардкодные значения `*.dp` на dimension resources
- Используй этот навык, когда добавляешь новые размеры, отступы или spacing
- Используй этот навык, когда обеспечиваешь консистентность размеров между экранами
- Используй этот навык, когда рефакторишь существующий код для соответствия стандартам
- Этот навык полезен при верстке новых экранов и компонентов дизайн-системы

## Instructions

### Required Import

```kotlin
import androidx.compose.ui.res.dimensionResource
import com.swparks.R
```

### Core Principles

#### 1. Never Hardcode Sizes

All paddings, sizes, spacings, and dimensions must come from `app/src/main/res/values/dimens.xml`.

**❌ Bad:**

```kotlin
Modifier.padding(16.dp)
Modifier.size(32.dp)
Arrangement.spacedBy(8.dp)
```

**✅ Good:**

```kotlin
Modifier.padding(horizontal = dimensionResource(id = R.dimen.spacing_regular))
Modifier.size(dimensionResource(id = R.dimen.icon_size_small))
Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_xsmall))
```

#### 2. Standard Spacing Hierarchy

Используй существующие spacing токены из `dimens.xml`:

```kotlin
// Примеры существующих spacings:
dimensionResource(id = R.dimen.spacing_xxsmall)  // 4dp
dimensionResource(id = R.dimen.spacing_regular)  // 16dp - standard spacing (most common)
dimensionResource(id = R.dimen.spacing_large)    // 24dp - large spacing
```

#### 3. Icon Sizes

Используй существующие icon size токены из `dimens.xml`:

```kotlin
// Примеры существующих icon sizes:
dimensionResource(id = R.dimen.icon_size_small)  // 32dp
dimensionResource(id = R.dimen.icon_size_medium) // 42dp
dimensionResource(id = R.dimen.icon_size_large)  // 48dp
```

#### 4. Common Sizes

Используй существующие common size токены из `dimens.xml`:

```kotlin
// Примеры существующих common sizes:
dimensionResource(id = R.dimen.size_small)  // 32dp
dimensionResource(id = R.dimen.size_medium) // 48dp
dimensionResource(id = R.dimen.size_large)  // 80dp
```

#### 5. Border and Elevation

Используй существующие border и elevation токены из `dimens.xml`:

```kotlin
// Примеры существующих значений:
dimensionResource(id = R.dimen.border_width_small) // 2dp
dimensionResource(id = R.dimen.elevation_none)      // 0dp
dimensionResource(id = R.dimen.elevation_small)     // 2dp
dimensionResource(id = R.dimen.elevation_medium)    // 4dp
```

#### 6. Corner Radii

Используй существующие corner radius токены из `dimens.xml`:

```kotlin
// Примеры существующих corner radii:
dimensionResource(id = R.dimen.corner_radius_none)   // 0dp
dimensionResource(id = R.dimen.corner_radius_small)  // 8dp
dimensionResource(id = R.dimen.corner_radius_bubble) // 20dp
```

#### 7. Component-Specific Dimensions

Используй существующие component-specific токены из `dimens.xml`:

```kotlin
// Примеры существующих component-specific значений:
dimensionResource(id = R.dimen.bubble_horizontal_margin)  // 40dp
dimensionResource(id = R.dimen.bubble_padding_horizontal) // 18dp
```

### When to Add New Dimensions

Only add new dimensions when:

1. No existing dimension fits use case
2. Value is repeated multiple times (3+ occurrences)
3. Value has semantic meaning in design (e.g., specific corner radius, bubble margin)

**Naming and sorting rules:**

- **Опирайтесь на существующие значения**: при выборе имени ориентируйтесь на уже существующие размеры в этой категории
- **Логичная сортировка в dimens.xml**: новые значения добавляйте и сортируйте по числовому значению, а не по алфавиту

**Пример**: если в `dimens.xml` уже есть `spacing_regular = 16dp` и `spacing_large = 24dp`, а нужно добавить значение 20dp:

- ✅ Хорошо: использовать существующее `spacing_medium = 20dp` (если есть)
- ✅ Хорошо: если нет, создать `spacing_medium = 20dp` и разместить по числовому значению между `regular` и `large`
- ❌ Плохо: назвать `spacing_regularplus` — нелогично, так как 20 находится ровно посередине между 16 и 24

**Примеры логического именования**:

```xml
<!-- Проверяем существующие значения в dimens.xml -->
<dimen name="spacing_regular">16dp</dimen>

<!-- Если нужно 20dp - проверяем, есть ли spacing_medium -->
<dimen name="spacing_medium">20dp</dimen>  ← используем или создаем по числовому значению

<dimen name="spacing_large">24dp</dimen>
```

**Naming conventions:**

- Spacings: `spacing_<semantic>` (например: `spacing_regular`, `spacing_medium`, `spacing_micro`)
- Icons: `icon_size_<semantic>` или `icon_<semantic>` (например: `icon_size_small`, `icon_size_avatar`)
- Corners: `corner_radius_<semantic>` (например: `corner_radius_small`, `corner_radius_bubble`)
- Component-specific: `<component>_<property>` (например: `bubble_horizontal_margin`)

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

Для примеров использования смотрите файл [references/EXAMPLES.md](references/EXAMPLES.md).

### Padding on all sides

См. пример в [references/EXAMPLES.md](references/EXAMPLES.md#padding-on-all-sides).

### Padding on specific sides

См. пример в [references/EXAMPLES.md](references/EXAMPLES.md#padding-on-specific-sides).

### Spacing in arrangements

См. пример в [references/EXAMPLES.md](references/EXAMPLES.md#spacing-in-arrangements).

### Size modifiers

См. пример в [references/EXAMPLES.md](references/EXAMPLES.md#size-modifiers).

### Corner shapes

См. пример в [references/EXAMPLES.md](references/EXAMPLES.md#corner-shapes).

## Additional Examples

Для полных примеров использования смотрите файл [references/EXAMPLES.md](references/EXAMPLES.md):

- Complete example: Composable with dimens
- Using different icon sizes
- Spacing hierarchy examples
- Border and elevation examples
- Component-specific dimensions (message bubble)
- LazyColumn spacing
- Button sizing
