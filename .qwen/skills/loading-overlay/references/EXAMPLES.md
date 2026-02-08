# Примеры реализации блокировки контента при загрузке

Все примеры основаны на реальных экранах из проекта Jetpack-WorkoutApp.

## Пример 1: Полная реализация экрана с загрузкой (MyFriendsScreen)

Файлы:
- `app/src/main/java/com/swparks/viewmodel/FriendsListViewModel.kt`
- `app/src/main/java/com/swparks/ui/screens/profile/MyFriendsScreen.kt`

### ViewModel - состояние загрузки и обработка действий

```kotlin
class FriendsListViewModel(
    private val swRepository: SWRepository
) : ViewModel() {

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    fun onAcceptFriendRequest(userId: Long) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                swRepository.acceptFriendRequest(userId)
                // Обновление списка друзей...
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка принятия заявки: ${e.message}")
            } finally {
                _isProcessing.value = false
            }
        }
    }
}
```

### UI экрана и безымянный компонент

```kotlin
@Composable
fun MyFriendsScreen(
    modifier: Modifier = Modifier,
    viewModel: FriendsListViewModel,
    onBackClick: () -> Unit,
    parentPaddingValues: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    MyFriendsScreenContent(
        modifier = modifier,
        uiState = uiState,
        isProcessing = isProcessing,
        onBackClick = onBackClick,
        parentPaddingValues = parentPaddingValues,
        onAcceptFriendRequest = { viewModel.onAcceptFriendRequest(it) },
        onDeclineFriendRequest = { viewModel.onDeclineFriendRequest(it) },
        onFriendClick = { viewModel.onFriendClick(it) }
    )
}

@Composable
fun MyFriendsScreenContent(
    modifier: Modifier = Modifier,
    uiState: FriendsListUiState,
    isProcessing: Boolean,
    onBackClick: () -> Unit,
    parentPaddingValues: PaddingValues,
    onAcceptFriendRequest: (Long) -> Unit,
    onDeclineFriendRequest: (Long) -> Unit,
    onFriendClick: (Long) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.friends)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(parentPaddingValues)
                .padding(innerPadding)
        ) {
            when (uiState) {
                is FriendsListUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is FriendsListUiState.Error -> {
                    Text(uiState.message, modifier = Modifier.align(Alignment.Center))
                }
                is FriendsListUiState.Success -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        SuccessContent(
                            friendRequests = uiState.friendRequests,
                            friends = uiState.friends,
                            onAcceptFriendRequest = onAcceptFriendRequest,
                            onDeclineFriendRequest = onDeclineFriendRequest,
                            onFriendClick = onFriendClick,
                            enabled = !isProcessing
                        )
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .matchParentSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
```

### Контент списка с передачей enabled

```kotlin
@Composable
private fun SuccessContent(
    friendRequests: List<User>,
    friends: List<User>,
    onAcceptFriendRequest: (Long) -> Unit,
    onDeclineFriendRequest: (Long) -> Unit,
    onFriendClick: (Long) -> Unit,
    enabled: Boolean
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
    ) {
        // Заявки в друзья
        if (friendRequests.isNotEmpty()) {
            item {
                SectionView(
                    titleID = R.string.requests,
                    titleBottomPadding = dimensionResource(R.dimen.spacing_xsmall)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
                    ) {
                        friendRequests.forEach { user ->
                            FriendRequestRowView(
                                data = FriendRequestData(
                                    modifier = Modifier,
                                    imageStringURL = user.image,
                                    name = user.name,
                                    address = null,
                                    onClickAccept = { onAcceptFriendRequest(user.id) },
                                    onClickDecline = { onDeclineFriendRequest(user.id) },
                                    enabled = enabled  // Передача в компонент
                                )
                            )
                        }
                    }
                }
            }
            item { HorizontalDivider() }
        }

        // Список друзей
        if (friends.isNotEmpty()) {
            item {
                SectionView(
                    titleID = R.string.friends,
                    titleBottomPadding = dimensionResource(R.dimen.spacing_xsmall)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
                    ) {
                        friends.forEach { user ->
                            Box(
                                modifier = Modifier
                                    .clickable(enabled = enabled) {  // Кликабельность
                                        onFriendClick(user.id)
                                    }
                            ) {
                                UserRowView(
                                    modifier = Modifier,
                                    imageStringURL = user.image,
                                    name = user.name,
                                    address = null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
```

## Пример 2: Комбинирование нескольких состояний загрузки

Иногда нужно комбинировать несколько источников загрузки (начальная загрузка + действие пользователя):

```kotlin
class EventsViewModel(
    private val eventsRepository: EventsRepository
) : ViewModel() {

    private val _isLoadingEvents = MutableStateFlow(false)
    val isLoadingEvents: StateFlow<Boolean> = _isLoadingEvents.asStateFlow()

    private val _isCreatingEvent = MutableStateFlow(false)
    val isCreatingEvent: StateFlow<Boolean> = _isCreatingEvent.asStateFlow()

    // Комбинированное состояние для блокировки UI
    val isProcessing: StateFlow<Boolean> = combine(
        _isLoadingEvents,
        _isCreatingEvent
    ) { loadingEvents, creatingEvent ->
        loadingEvents || creatingEvent
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun loadEvents() {
        viewModelScope.launch {
            _isLoadingEvents.value = true
            try {
                eventsRepository.getEvents()
            } finally {
                _isLoadingEvents.value = false
            }
        }
    }

    fun createEvent(event: Event) {
        viewModelScope.launch {
            _isCreatingEvent.value = true
            try {
                eventsRepository.createEvent(event)
            } finally {
                _isCreatingEvent.value = false
            }
        }
    }
}
```

## Пример 3: Индикатор загрузки внутри кнопки

Для кнопок с операциями CRUD удобно показывать индикатор внутри кнопки:

```kotlin
@Composable
fun SaveButton(
    onSave: () -> Unit,
    isSaving: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onSave,
        enabled = !isSaving,
        modifier = modifier
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(stringResource(R.string.save))
        }
    }
}
```

## Ключевые моменты реализации

### 1. Box с fillMaxSize и matchParentSize()

```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    Content(enabled = !isProcessing)
    if (isProcessing) {
        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.Center)
                .matchParentSize()  // Только размер родителя, не всего экрана
        )
    }
}
```

### 2. Всегда используй try-finally

```kotlin
viewModelScope.launch {
    _isProcessing.value = true
    try {
        repository.performAction()
    } catch (e: Exception) {
        Log.e(TAG, "Ошибка: ${e.message}")
    } finally {
        _isProcessing.value = false  // Выполнится в любом случае
    }
}
```

### 3. Передача enabled во все интерактивные элементы

```kotlin
// Кликабельный Box
Box(modifier = Modifier.clickable(enabled = enabled) { onClick() })

// Кнопки
Button(onClick = onClick, enabled = enabled)

// Пользовательские компоненты
CustomComponent(onClick = onClick, enabled = enabled)
```
