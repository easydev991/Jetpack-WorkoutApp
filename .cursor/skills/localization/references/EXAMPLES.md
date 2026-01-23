# Примеры локализации

## Ресурсы строк

### Базовые строки (strings.xml)

```xml
<!-- res/values/strings.xml -->
<string name="days_count_format">%d дней</string>
<string name="app_name">SW Parks</string>

<!-- res/values-en/strings.xml -->
<string name="days_count_format">%d days</string>
<string name="app_name">SW Parks</string>
```

### Использование в Compose

```kotlin
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun DaysCountText(count: Int) {
    Text(stringResource(R.string.days_count_format, count))
}

@Composable
fun AppTitle() {
    Text(stringResource(R.string.app_name))
}
```

### Использование в обычном Kotlin коде

```kotlin
import android.content.Context
import com.swparks.R

class SomeClass(private val context: Context) {
    fun getDaysCountText(count: Int): String {
        return context.getString(R.string.days_count_format, count)
    }

    fun getAppName(): String {
        return context.getString(R.string.app_name)
    }
}
```

## Множественное число

### Ресурсы (strings.xml)

```xml
<!-- res/values/strings.xml -->
<plurals name="days_count">
    <item quantity="one">%d день</item>
    <item quantity="few">%d дня</item>
    <item quantity="many">%d дней</item>
    <item quantity="other">%d дней</item>
</plurals>
```

### Использование в Compose

```kotlin
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource

@Composable
fun DaysCountPluralText(count: Int) {
    Text(pluralStringResource(R.plurals.days_count, count))
}
```

### Использование в обычном Kotlin коде

```kotlin
import android.content.Context
import com.swparks.R

class SomeClass(private val context: Context) {
    fun getDaysCountPluralText(count: Int): String {
        return context.resources.getQuantityString(
            R.plurals.days_count,
            count,
            count
        )
    }
}
```

## Форматирование

### String.format()

```kotlin
// В обычном коде
val count = 5
val message = String.format(context.getString(R.string.days_count_format), count)

// В Compose
Text(stringResource(R.string.days_count_format, 5))
```

### DateFormat

```kotlin
import java.text.DateFormat
import java.util.Date
import java.util.Locale

// Получение форматированной даты
fun formatDate(date: Date): String {
    val dateFormat = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault())
    return dateFormat.format(date)
}

// Пример использования
val currentDate = Date()
val formattedDate = formatDate(currentDate)
```

### Параметризованные строки в Compose

```kotlin
// Ресурс
// <string name="greeting">Привет, %s!</string>

// Использование в Compose
@Composable
fun Greeting(name: String) {
    Text(stringResource(R.string.greeting, name))
}
```

### Множественные параметры в строке

```kotlin
// Ресурс
// <string name="user_info">Пользователь %s имеет %d друзей</string>

// Использование в Compose
@Composable
fun UserInfo(name: String, friendsCount: Int) {
    Text(stringResource(R.string.user_info, name, friendsCount))
}
```
