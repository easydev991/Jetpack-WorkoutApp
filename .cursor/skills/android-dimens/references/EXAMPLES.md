# Примеры использования dimension resources

## Padding on all sides

### Before

```kotlin
Modifier.padding(16.dp)
```

### After

```kotlin
Modifier.padding(dimensionResource(id = R.dimen.spacing_regular))
```

## Padding on specific sides

### Before

```kotlin
Modifier.padding(horizontal = 16.dp)
Modifier.padding(vertical = 12.dp)
Modifier.padding(top = 8.dp, bottom = 16.dp, start = 4.dp, end = 8.dp)
```

### After

```kotlin
Modifier.padding(horizontal = dimensionResource(id = R.dimen.spacing_regular))
Modifier.padding(vertical = dimensionResource(id = R.dimen.spacing_small))
Modifier.padding(
    top = dimensionResource(id = R.dimen.spacing_xsmall),
    bottom = dimensionResource(id = R.dimen.spacing_regular),
    start = dimensionResource(id = R.dimen.spacing_xxsmall),
    end = dimensionResource(id = R.dimen.spacing_xsmall)
)
```

## Spacing in arrangements

### Before

```kotlin
Arrangement.spacedBy(8.dp)
```

### After

```kotlin
Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_xsmall))
```

## Size modifiers

### Before

```kotlin
Modifier.size(32.dp)
Modifier.size(width = 48.dp, height = 24.dp)
```

### After

```kotlin
Modifier.size(dimensionResource(id = R.dimen.size_small))
Modifier.size(
    width = dimensionResource(id = R.dimen.size_medium),
    height = dimensionResource(id = R.dimen.spacing_large)
)
```

## Corner shapes

### Before

```kotlin
RoundedCornerShape(8.dp)
RoundedCornerShape(topStart = 20.dp, topEnd = 0.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
```

### After

```kotlin
RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_small))
RoundedCornerShape(
    topStart = dimensionResource(id = R.dimen.corner_radius_bubble),
    topEnd = dimensionResource(id = R.dimen.corner_radius_none),
    bottomStart = dimensionResource(id = R.dimen.corner_radius_bubble),
    bottomEnd = dimensionResource(id = R.dimen.corner_radius_bubble)
)
```

## Complete example: Composable with dimens

### Before

```kotlin
@Composable
fun UserProfileCard(user: User) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(user.name, style = MaterialTheme.typography.titleLarge)
            Text(user.email, style = MaterialTheme.typography.bodyMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Text(user.phone)
            }
        }
    }
}
```

### After

```kotlin
@Composable
fun UserProfileCard(user: User) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.spacing_regular))
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(id = R.dimen.spacing_regular)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_xsmall))
        ) {
            Text(user.name, style = MaterialTheme.typography.titleLarge)
            Text(user.email, style = MaterialTheme.typography.bodyMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_regular))
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(id = R.dimen.icon_size_menu))
                )
                Text(user.phone)
            }
        }
    }
}
```

## Using icon sizes

### Different icon sizes available

```kotlin
// Small icon
Icon(
    imageVector = Icons.Default.Add,
    contentDescription = null,
    modifier = Modifier.size(dimensionResource(id = R.dimen.icon_size_small))
)

// Menu icon (24dp)
Icon(
    imageVector = Icons.Default.Menu,
    contentDescription = null,
    modifier = Modifier.size(dimensionResource(id = R.dimen.icon_size_menu))
)

// Medium icon
Icon(
    imageVector = Icons.Default.Home,
    contentDescription = null,
    modifier = Modifier.size(dimensionResource(id = R.dimen.icon_size_medium))
)

// Large icon
Icon(
    imageVector = Icons.Default.Star,
    contentDescription = null,
    modifier = Modifier.size(dimensionResource(id = R.dimen.icon_size_large))
)

// Avatar icon (34dp)
Icon(
    imageVector = Icons.Default.Person,
    contentDescription = null,
    modifier = Modifier.size(dimensionResource(id = R.dimen.icon_size_avatar))
)

// Indicator icon (15dp)
Icon(
    imageVector = Icons.Default.Notifications,
    contentDescription = null,
    modifier = Modifier.size(dimensionResource(id = R.dimen.icon_size_indicator))
)
```

## Spacing hierarchy examples

### Using different spacings

```kotlin
@Composable
fun SpacingExample() {
    Column(
        modifier = Modifier.padding(dimensionResource(id = R.dimen.spacing_regular)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_small))
    ) {
        // xxsmall (4dp) - minimal spacing
        Text("Content", modifier = Modifier.padding(dimensionResource(id = R.dimen.spacing_xxsmall)))

        // xsmall (8dp) - small spacing
        Text("Content", modifier = Modifier.padding(dimensionResource(id = R.dimen.spacing_xsmall)))

        // small (12dp) - regular small spacing
        Text("Content", modifier = Modifier.padding(dimensionResource(id = R.dimen.spacing_small)))

        // regular (16dp) - standard spacing (most common)
        Text("Content", modifier = Modifier.padding(dimensionResource(id = R.dimen.spacing_regular)))

        // medium (20dp) - medium spacing
        Text("Content", modifier = Modifier.padding(dimensionResource(id = R.dimen.spacing_medium)))

        // large (24dp) - large spacing
        Text("Content", modifier = Modifier.padding(dimensionResource(id = R.dimen.spacing_large)))

        // xlarge (32dp) - extra large spacing
        Text("Content", modifier = Modifier.padding(dimensionResource(id = R.dimen.spacing_xlarge)))
    }
}
```

## Border and elevation examples

### Border width

```kotlin
Box(
    modifier = Modifier
        .border(
            width = dimensionResource(id = R.dimen.border_width),
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_small))
        )
)
```

### Elevation

```kotlin
// No elevation
Card(elevation = CardDefaults.cardElevation(dimensionResource(id = R.dimen.elevation_none)))

// Small elevation
Card(elevation = CardDefaults.cardElevation(dimensionResource(id = R.dimen.elevation_small)))

// Medium elevation
Card(elevation = CardDefaults.cardElevation(dimensionResource(id = R.dimen.elevation_medium)))
```

## Component-specific dimensions

### Message bubble example

```kotlin
@Composable
fun MessageBubble(text: String, isOwn: Boolean) {
    Box(
        modifier = Modifier
            .padding(horizontal = dimensionResource(id = R.dimen.bubble_horizontal_margin))
    ) {
        Surface(
            color = if (isOwn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_bubble)),
            modifier = Modifier.padding(dimensionResource(id = R.dimen.bubble_padding_horizontal))
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(dimensionResource(id = R.dimen.spacing_regular))
            )
        }
    }
}
```

## LazyColumn spacing

```kotlin
@Composable
fun ItemsList(items: List<Item>) {
    LazyColumn(
        contentPadding = PaddingValues(dimensionResource(id = R.dimen.spacing_regular)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_small))
    ) {
        items(items, key = { it.id }) { item ->
            ItemRow(item)
        }
    }
}
```

## Button sizing

```kotlin
@Composable
fun CustomButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(id = R.dimen.size_medium)),
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_small))
    ) {
        Text("Button")
    }
}
```
