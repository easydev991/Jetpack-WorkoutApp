package com.swparks.ui.screens.common

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.swparks.JetpackWorkoutApplication
import com.swparks.ui.model.TextEntryMode
import com.swparks.ui.state.TextEntryEvent
import com.swparks.ui.state.TextEntryUiState
import com.swparks.ui.viewmodel.ITextEntryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
private data class SheetContentParams(
    val sheetState: SheetState,
    val show: Boolean,
    val uiState: TextEntryUiState,
    val viewModel: ITextEntryViewModel,
    val onDismissed: () -> Unit,
    val dismissSheet: (() -> Unit) -> Unit,
    val snackbarHostState: SnackbarHostState
)

/**
 * Хост для [TextEntryScreen] в виде модального bottom sheet.
 *
 * [TextEntryScreen] открывается как ModalBottomSheet поверх текущего UI.
 * Закрытие листа разрешено только:
 * - по нажатию на крестик в левом верхнем углу (только если !uiState.isLoading: не идёт отправка)
 * - по системной кнопке «назад» или жесту возврата (только если !uiState.isLoading: не идёт отправка)
 * - автоматически после успешной отправки (событие Success)
 *
 * Закрытие по тапу вне области и свайпу листа вниз — запрещено.
 * Обратный свайп листа не срабатывает: перетаскивание отключено параметром
 * [ModalBottomSheet.sheetGesturesEnabled], взаимодействие с контентом не ограничивается.
 *
 * Ошибки показываются в snackbar'е внутри листа: ошибки валидации — из `uiState.error`
 * на самом экране, ошибки отправки — из события `TextEntryEvent.Error` в хосте.
 *
 * @param show Флаг для показа/скрытия листа
 * @param mode Режим работы экрана (тип операции, заголовок, валидация)
 * @param onDismissed Callback при закрытии листа (крестик или возврат)
 * @param onSendSuccess Callback при успешной отправке (для обновления данных в родительском компоненте)
 * @param viewModel Необязательная ViewModel для UI-тестов; при `null` создаётся через AppContainer
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEntrySheetHost(
    show: Boolean,
    mode: TextEntryMode,
    onDismissed: () -> Unit,
    onSendSuccess: () -> Unit = {},
    viewModel: ITextEntryViewModel? = null
) {
    val allowHide = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val appContainer =
        (LocalContext.current.applicationContext as JetpackWorkoutApplication).container

    val resolvedViewModel: ITextEntryViewModel =
        viewModel ?: remember(mode) {
            appContainer.textEntryViewModelFactory(mode)
        }
    val uiState by resolvedViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { newValue ->
                if (newValue == SheetValue.Hidden) allowHide.value else true
            }
        )

    fun dismiss(onComplete: () -> Unit) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        scope.launch {
            allowHide.value = true
            sheetState.hide()
            allowHide.value = false
            onComplete()
        }
    }

    LaunchedEffect(show) {
        if (show) {
            resolvedViewModel.resetState()
        }
    }

    LaunchedEffect(Unit) {
        resolvedViewModel.events.collect { event ->
            when (event) {
                is TextEntryEvent.Success -> dismiss(onSendSuccess)

                is TextEntryEvent.Error -> {
                    scope.launch { snackbarHostState.showSnackbar(event.message) }
                }
            }
        }
    }

    SheetContent(
        SheetContentParams(
            sheetState = sheetState,
            show = show,
            uiState = uiState,
            viewModel = resolvedViewModel,
            onDismissed = onDismissed,
            dismissSheet = ::dismiss,
            snackbarHostState = snackbarHostState
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetContent(params: SheetContentParams) {
    with(params) {
        if (show) {
            ModalBottomSheet(
                onDismissRequest = {
                    if (uiState.isLoading) return@ModalBottomSheet
                    dismissSheet(onDismissed)
                },
                sheetState = sheetState,
                dragHandle = {},
                properties =
                    ModalBottomSheetProperties(
                        shouldDismissOnBackPress = true,
                        shouldDismissOnClickOutside = false
                    ),
                sheetGesturesEnabled = false
            ) {
                TextEntryScreen(
                    viewModel = viewModel,
                    onDismiss = {
                        if (uiState.isLoading) return@TextEntryScreen
                        dismissSheet(onDismissed)
                    },
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}
