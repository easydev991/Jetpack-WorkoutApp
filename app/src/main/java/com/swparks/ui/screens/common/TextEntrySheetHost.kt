package com.swparks.ui.screens.common

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.swparks.JetpackWorkoutApplication
import com.swparks.ui.ds.disableAllGestures
import com.swparks.ui.model.TextEntryMode
import com.swparks.ui.state.TextEntryEvent
import com.swparks.ui.state.TextEntryUiState
import com.swparks.ui.viewmodel.ITextEntryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
private data class SheetContentParams(
    val sheetState: SheetState,
    val show: Boolean,
    val uiState: TextEntryUiState,
    val viewModel: ITextEntryViewModel,
    val scope: CoroutineScope,
    val onDismissed: () -> Unit,
    val allowHide: MutableState<Boolean>
)

/**
 * Хост для [TextEntryScreen] в виде модального bottom sheet.
 *
 * [TextEntryScreen] открывается как ModalBottomSheet поверх текущего UI.
 * Закрытие листа разрешено только:
 * - по нажатию на крестик в левом верхнем углу (только если !uiState.isLoading: не идёт отправка)
 * - автоматически после успешной отправки (событие Success)
 *
 * Закрытие по тапу вне области, свайпу вниз, системной кнопке/жесту "назад" — запрещено.
 *
 * Ошибки обрабатываются через [userNotifier] в ViewModel (Snackbar отображается автоматически в любом экране).
 *
 * @param show Флаг для показа/скрытия листа
 * @param mode Режим работы экрана (тип операции, заголовок, валидация)
 * @param onDismissed Callback при закрытии листа (нажатие на кнопку X)
 * @param onSendSuccess Callback при успешной отправке (для обновления данных в родительском компоненте)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEntrySheetHost(
    show: Boolean,
    mode: TextEntryMode,
    onDismissed: () -> Unit,
    onSendSuccess: () -> Unit = {}
) {
    val allowHide = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val appContainer =
        (LocalContext.current.applicationContext as JetpackWorkoutApplication).container

    val viewModel: ITextEntryViewModel =
        remember(mode) {
            appContainer.textEntryViewModelFactory(mode)
        }
    val uiState by viewModel.uiState.collectAsState()

    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { newValue ->
                if (newValue == SheetValue.Hidden) allowHide.value else true
            }
        )

    LaunchedEffect(show) {
        if (show) {
            viewModel.resetState()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TextEntryEvent.Success -> {
                    scope.launch {
                        allowHide.value = true
                        sheetState.hide()
                        allowHide.value = false
                        onSendSuccess()
                    }
                }

                is TextEntryEvent.Error -> {}
            }
        }
    }

    SheetContent(
        SheetContentParams(
            sheetState = sheetState,
            show = show,
            uiState = uiState,
            viewModel = viewModel,
            scope = scope,
            onDismissed = onDismissed,
            allowHide = allowHide
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetContent(params: SheetContentParams) {
    with(params) {
        if (show) {
            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = sheetState,
                dragHandle = {},
                properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false)
            ) {
                TextEntryScreen(
                    modifier = Modifier.disableAllGestures(),
                    viewModel = viewModel,
                    onDismiss = {
                        if (uiState.isLoading) return@TextEntryScreen
                        scope.launch {
                            allowHide.value = true
                            sheetState.hide()
                            allowHide.value = false
                            onDismissed()
                        }
                    }
                )
            }
        }
    }
}
