package com.swparks.ui.screens.common

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.swparks.JetpackWorkoutApplication
import com.swparks.ui.ds.disableAllGestures
import com.swparks.ui.model.TextEntryMode
import com.swparks.ui.state.TextEntryEvent
import com.swparks.ui.viewmodel.ITextEntryViewModel
import kotlinx.coroutines.launch

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
    var allowHide by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val appContainer =
        (LocalContext.current.applicationContext as JetpackWorkoutApplication).container

    val viewModel: ITextEntryViewModel = remember(mode) {
        appContainer.textEntryViewModelFactory(mode)
    }
    val uiState by viewModel.uiState.collectAsState()

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            if (newValue == SheetValue.Hidden) allowHide else true
        }
    )

    // Сбрасываем состояние при каждом открытии sheet
    LaunchedEffect(show) {
        if (show) {
            viewModel.resetState()
        }
    }

    // Обработка событий Success
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TextEntryEvent.Success -> {
                    // Логика закрытия sheet и внешний коллбэк
                    scope.launch {
                        allowHide = true
                        sheetState.hide()
                        allowHide = false
                        onSendSuccess() // Внешний коллбэк для родительского компонента
                    }
                }

                is TextEntryEvent.Error -> {
                    // Ошибки обрабатываются через userNotifier в ViewModel (Snackbar отображается автоматически)
                    // Ничего не делаем здесь
                }
            }
        }
    }

    if (show) {
        ModalBottomSheet(
            onDismissRequest = {
                // Игнорируем тап вне sheet / системный dismiss
            },
            sheetState = sheetState,
            dragHandle = {},
            properties = ModalBottomSheetProperties(
                shouldDismissOnBackPress = false
            )
        ) {
            TextEntryScreen(
                modifier = Modifier.disableAllGestures(),
                viewModel = viewModel,
                onDismiss = {
                    if (uiState.isLoading) return@TextEntryScreen
                    scope.launch {
                        allowHide = true
                        sheetState.hide()
                        allowHide = false
                        onDismissed()
                    }
                }
            )
        }
    }
}
