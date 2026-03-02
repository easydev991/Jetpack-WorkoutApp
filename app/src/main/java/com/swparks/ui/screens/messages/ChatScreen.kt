package com.swparks.ui.screens.messages

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swparks.R
import com.swparks.data.model.MessageResponse
import com.swparks.ui.ds.AsyncImageConfig
import com.swparks.ui.ds.ChatInputBar
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.ds.MessageBubbleView
import com.swparks.ui.ds.MessageType
import com.swparks.ui.ds.SWAsyncImage
import com.swparks.ui.state.ChatEvent
import com.swparks.ui.state.ChatUiState
import com.swparks.ui.viewmodel.IChatViewModel
import com.swparks.util.DateFormatter
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Экран диалога (чата) с пользователем.
 *
 * @param modifier Модификатор
 * @param viewModel ViewModel для управления состоянием
 * @param otherUserId ID собеседника (для отправки сообщений)
 * @param userName Имя собеседника для отображения в TopAppBar
 * @param userImage URL аватара собеседника
 * @param currentUserId ID текущего пользователя (для определения типа сообщения)
 * @param onBackClick Callback при нажатии на кнопку назад
 * @param onAvatarClick Callback при нажатии на аватар (переход в профиль)
 * @param onMessageSent Callback при успешной отправке сообщения (для обновления списка диалогов)
 */
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: IChatViewModel,
    otherUserId: Int,
    userName: String,
    userImage: String?,
    currentUserId: Int?,
    onBackClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onMessageSent: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val messageText by viewModel.messageText

    // Подписываемся на события чата
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ChatEvent.MessageSent -> {
                    onMessageSent()
                }
            }
        }
    }

    ChatContent(
        modifier = modifier,
        uiState = uiState,
        isLoading = isLoading,
        messageText = messageText,
        otherUserId = otherUserId,
        userName = userName,
        userImage = userImage,
        currentUserId = currentUserId,
        onMessageTextChange = { viewModel.messageText.value = it },
        onSendClick = { userId ->
            viewModel.sendMessage(userId)
        },
        onRefresh = { viewModel.refreshMessages() },
        onBackClick = onBackClick,
        onAvatarClick = onAvatarClick,
        onMarkAsRead = { userId -> viewModel.markAsRead(userId) }
    )
}

/**
 * Контент экрана чата без ViewModel для тестирования.
 *
 * @param otherUserId ID собеседника (для отправки сообщений)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatContent(
    modifier: Modifier = Modifier,
    uiState: ChatUiState,
    isLoading: Boolean,
    messageText: String,
    otherUserId: Int,
    userName: String,
    userImage: String?,
    currentUserId: Int?,
    onMessageTextChange: (String) -> Unit,
    onSendClick: (userId: Int) -> Unit,
    onRefresh: () -> Unit,
    onBackClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onMarkAsRead: (userId: Int) -> Unit
) {
    val scrollState = rememberLazyListState()
    val topAppBarState = rememberTopAppBarState()

    Scaffold(
        modifier = modifier,
        topBar = {
            ChatTopAppBar(
                userName = userName,
                userImage = userImage,
                onBackClick = onBackClick,
                onAvatarClick = onAvatarClick,
                onRefresh = onRefresh,
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
            )
        },
        bottomBar = {
            ChatInputBar(
                text = messageText,
                onTextChange = onMessageTextChange,
                isLoading = isLoading,
                onSendClick = {
                    onSendClick(otherUserId)
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is ChatUiState.Loading -> {
                    LoadingOverlayView()
                }

                is ChatUiState.Success -> {
                    if (uiState.messages.isEmpty()) {
                        // Пустой диалог
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.chat_empty_messages),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        MessagesList(
                            messages = uiState.messages,
                            currentUserId = currentUserId,
                            scrollState = scrollState,
                            onMarkAsRead = onMarkAsRead
                        )
                    }

                    // LoadingOverlay при отправке сообщения
                    if (isLoading) {
                        LoadingOverlayView()
                    }
                }

                is ChatUiState.Error -> {
                    // Error state не отображаем - ошибки показываются через тосты
                    // на корневом экране через UserNotifier
                }
            }
        }
    }
}

/**
 * Список сообщений с автопрокруткой и markAsRead.
 */
@Composable
private fun MessagesList(
    messages: List<MessageResponse>,
    currentUserId: Int?,
    scrollState: LazyListState,
    onMarkAsRead: (userId: Int) -> Unit
) {
    val context = LocalContext.current

    // Автопрокрутка к последнему сообщению при загрузке
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scrollState.animateScrollToItem(messages.size - 1)
        }
    }

    // markAsRead при появлении последнего сообщения на экране
    LaunchedEffect(messages, scrollState) {
        if (messages.isEmpty()) return@LaunchedEffect

        snapshotFlow {
            val lastVisibleIndex = scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisibleIndex >= messages.size - 1
        }
            .distinctUntilChanged()
            .collect { isLastMessageVisible ->
                if (isLastMessageVisible) {
                    messages.lastOrNull()?.userId?.let { userId ->
                        // Вызываем markAsRead только для входящих сообщений
                        if (userId != currentUserId) {
                            onMarkAsRead(userId)
                        }
                    }
                }
            }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = scrollState,
        contentPadding = PaddingValues(
            start = dimensionResource(R.dimen.spacing_regular),
            end = dimensionResource(R.dimen.spacing_regular),
            top = dimensionResource(R.dimen.spacing_small),
            bottom = dimensionResource(R.dimen.spacing_regular)
        ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
    ) {
        itemsIndexed(messages) { _, message ->
            val isSentByCurrentUser = message.userId == currentUserId
            val messageType = if (isSentByCurrentUser) MessageType.SENT else MessageType.INCOMING

            // Форматируем дату
            val formattedDate = DateFormatter.formatDate(
                context = context,
                dateString = message.created,
                showTimeInThisYear = true
            )

            MessageBubbleView(
                messageType = messageType,
                messageBody = message.parsedMessage ?: message.message ?: "",
                dateString = formattedDate
            )
        }
    }
}

/**
 * TopAppBar для экрана чата.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopAppBar(
    userName: String,
    userImage: String?,
    onBackClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onRefresh: () -> Unit,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior
) {
    CenterAlignedTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Аватар справа от имени (кликабельный)
                IconButton(
                    onClick = onAvatarClick,
                    modifier = Modifier.testTag("AvatarButton")
                ) {
                    SWAsyncImage(
                        config = AsyncImageConfig(
                            imageStringURL = userImage,
                            size = 32.dp,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            shape = CircleShape,
                            showBorder = false
                        )
                    )
                }
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_xxsmall)))
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        },
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.refresh)
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}

// Preview-функции

@Preview(
    showBackground = true,
    locale = "ru"
)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
fun ChatContentPreview() {
    val messages = listOf(
        MessageResponse(
            id = 1,
            userId = 2,
            message = "Привет! Как дела?",
            name = "Друг",
            created = "2024-01-15T10:30:00Z"
        ),
        MessageResponse(
            id = 2,
            userId = 1,
            message = "Привет! Всё отлично, спасибо!",
            name = "Я",
            created = "2024-01-15T10:31:00Z"
        ),
        MessageResponse(
            id = 3,
            userId = 2,
            message = "Отлично! Чем занимаешься?",
            name = "Друг",
            created = "2024-01-15T10:32:00Z"
        )
    )

    MaterialTheme {
        Surface {
            ChatContent(
                uiState = ChatUiState.Success(messages),
                isLoading = false,
                messageText = "",
                otherUserId = 2,
                userName = "Иван Петров",
                userImage = null,
                currentUserId = 1,
                onMessageTextChange = {},
                onSendClick = {},
                onRefresh = {},
                onBackClick = {},
                onAvatarClick = {},
                onMarkAsRead = {}
            )
        }
    }
}

@Preview(
    showBackground = true,
    locale = "ru"
)
@Composable
fun ChatContentLoadingPreview() {
    MaterialTheme {
        Surface {
            ChatContent(
                uiState = ChatUiState.Loading,
                isLoading = false,
                messageText = "",
                otherUserId = 2,
                userName = "Иван Петров",
                userImage = null,
                currentUserId = 1,
                onMessageTextChange = {},
                onSendClick = {},
                onRefresh = {},
                onBackClick = {},
                onAvatarClick = {},
                onMarkAsRead = {}
            )
        }
    }
}

@Preview(
    showBackground = true,
    locale = "ru"
)
@Composable
fun ChatContentEmptyPreview() {
    MaterialTheme {
        Surface {
            ChatContent(
                uiState = ChatUiState.Success(emptyList()),
                isLoading = false,
                messageText = "",
                otherUserId = 2,
                userName = "Иван Петров",
                userImage = null,
                currentUserId = 1,
                onMessageTextChange = {},
                onSendClick = {},
                onRefresh = {},
                onBackClick = {},
                onAvatarClick = {},
                onMarkAsRead = {}
            )
        }
    }
}
