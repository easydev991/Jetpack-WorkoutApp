# Mocking — MockK

MockK — основной mocking-фреймворк. **Mockito не используется.**

## Базовые операторы

| Тип метода | Stub | Verify |
|---|---|---|
| suspend | `coEvery` | `coVerify` |
| не-suspend (в т.ч. геттеры) | `every` | `verify` |

```kotlin
val authRepository: AuthRepository = mockk()

// suspend
coEvery { authRepository.login(credentials) } returns Result.success(...)

// не-suspend (одноразовый коллаборатор, не Flow)
every { api.fetchItems() } returns listOf(item1, item2)
```

Для Flow-методов репозитория MockK не подходит (см. раздел «Fake vs
Mock» ниже) — здесь показан пример для stateless API, **не** для
UI state через `flowOf(...)` (это питфолл: эмиссия до подписки
теряется).

## Режимы mockk

- `mockk()` — строгий, всё мокается явно.
- `mockk(relaxed = true)` — возвращает дефолты (для не-critical
  зависимостей, `Logger`, `TokenEncoder`).
- `mockkObject(MyEnum)` — мок object/singleton.

## Mocking Android Log

```kotlin
@Before
fun setup() {
    mockkStatic(Log::class)
    every { Log.i(any(), any()) } returns 0
    every { Log.d(any(), any()) } returns 0
    every { Log.e(any(), any()) } returns 0
}

@After
fun tearDown() {
    unmockkAll()
}
```

## Захват аргументов

```kotlin
val slot = slot<LoginCredentials>()

coEvery { authRepository.login(capture(slot)) } returns Result.success(...)

// assert
assertEquals(testCredentials, slot.captured)
```

## throws

```kotlin
coEvery { api.getData() } throws IOException("network down")
```

## Fake vs Mock для репозиториев

→ `references/fakes-and-stateflow.md` (Bottom line).
