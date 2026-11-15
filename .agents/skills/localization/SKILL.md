---
name: localization
description: Properly work with localization in Android projects. Use this skill when adding new string resources, working with plurals, formatting dates and texts.
---

# Localization

## When to Use

- Use this skill when adding new string resources to the project
- Use this skill when working with plurals
- Use this skill when formatting dates, numbers, or texts
- Use this skill when localizing error messages
- Use this skill when updating existing strings in the project
- This skill is useful when you need to ensure localization consistency across all languages

## Instructions

### Project Languages

- **Russian (ru)** - primary language, all strings must be in Russian
- **English (en)** - additional language, translation must match Russian original

### String Resources

**Basic rules:**

1. All string resources are placed in `res/values/strings.xml` (Russian) and `res/values-en/strings.xml` (English)
2. Each string has a unique `name` in lowercase with underscores
3. Add new strings in parallel to both files (ru and en)

**Example of adding a string:**

```xml
<!-- res/values/strings.xml -->
<string name="welcome_message">Добро пожаловать!</string>

<!-- res/values-en/strings.xml -->
<string name="welcome_message">Welcome!</string>
```

**Usage in code:**

```kotlin
context.getString(R.string.welcome_message)
// or in XML
android:text="@string/welcome_message"
```

### Plurals

**When to use:**

- For words changing form based on quantity (day/days)
- For counters, lists, quantity indicators

**Russian language rule:**

- `one` - 1, 21, 31, 41...
- `few` - 2-4, 22-24, 32-34...
- `many` - 0, 5-20, 25-30, 35-40...
- `other` - for all other cases (usually not used in Russian)

**Example:**

```xml
<!-- res/values/strings.xml -->
<plurals name="days_count">
    <item quantity="one">%d день</item>
    <item quantity="few">%d дня</item>
    <item quantity="many">%d дней</item>
    <item quantity="other">%d дней</item>
</plurals>

<!-- res/values-en/strings.xml -->
<plurals name="days_count">
    <item quantity="one">%d day</item>
    <item quantity="other">%d days</item>
</plurals>
```

**Usage in code:**

```kotlin
val days = 5
val text = resources.getQuantityString(R.plurals.days_count, days, days)
// Result: "5 дней" (ru) or "5 days" (en)
```

### Formatting

**Basic tools:**

- `String.format()` - for formatting strings with placeholders
- `MessageFormat` - for complex formatting (if needed)
- `DateFormat` - for dates considering user locale

**String formatting example:**

```xml
<!-- res/values/strings.xml -->
<string name="items_count_format">Найдено %d элементов</string>

<!-- res/values-en/strings.xml -->
<string name="items_count_format">Found %d items</string>
```

**Usage in code:**

```kotlin
val count = 42
val text = getString(R.string.items_count_format, count)
// Result: "Найдено 42 элементов"
```

**Date formatting example:**

```kotlin
val date = LocalDate.now()
val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
val formattedDate = date.format(formatter)
// Result depends on device locale
```

### Logging

**Important rule:** All logs in code are written in Russian regardless of app localization

```kotlin
// Correct
Log.e(TAG, "Ошибка загрузки данных: ${error.message}")
Log.i(TAG, "Загружено $count элементов")

// Incorrect
Log.e(TAG, "Error loading data: ${error.message}")
```

### Resource Organization

**String grouping:**

- Use prefixes for grouping strings (e.g., `auth_`, `parks_`, `events_`)
- Related strings should have similar names

**Grouping example:**

```xml
<!-- Authorization -->
<string name="auth_login">Войти</string>
<string name="auth_password">Пароль</string>
<string name="auth_forgot_password">Забыли пароль?</string>

<!-- Parks -->
<string name="parks_title">Площадки</string>
<string name="parks_empty">Нет площадок</string>
<string name="parks_filter">Фильтр</string>
```

### Working with Parameters

**Rules:**

1. Use positional parameters (`%1$s`, `%2$d`) for strings with multiple substitutions
2. Keep parameter order the same for all languages

**Example with multiple parameters:**

```xml
<!-- res/values/strings.xml -->
<string name="user_profile">%1$s, %2$d лет</string>

<!-- res/values-en/strings.xml -->
<string name="user_profile">%1$s, %2$d years old</string>
```

**Usage:**

```kotlin
val name = "Иван"
val age = 25
val text = getString(R.string.user_profile, name, age)
// Result: "Иван, 25 лет"
```

### Error Handling

**Localized error messages:**

```xml
<!-- Network errors -->
<string name="error_network">Ошибка сети. Проверьте подключение к интернету.</string>
<string name="error_timeout">Время ожидания истекло. Попробуйте еще раз.</string>

<!-- Authorization errors -->
<string name="error_invalid_credentials">Неверный логин или пароль</string>
<string name="error_unauthorized">Необходима авторизация</string>

<!-- Data errors -->
<string name="error_not_found">Данные не найдены</string>
<string name="error_invalid_data">Неверные данные</string>
```

**Usage in code:**

```kotlin
try {
    loadData()
} catch (e: IOException) {
    Log.e(TAG, "Ошибка сети: ${e.message}")
    showError(getString(R.string.error_network))
}
```

### Checklist for Adding Localization

1. Added string to `res/values/strings.xml` (Russian)
2. Added string to `res/values-en/strings.xml` (English)
3. Checked that `name` is unique and follows naming convention
4. Used correct format (simple string, plurals, formatting)
5. Checked usage in code or XML
6. Made sure logs are in Russian (if needed)
7. Tested on different device languages (if possible)
