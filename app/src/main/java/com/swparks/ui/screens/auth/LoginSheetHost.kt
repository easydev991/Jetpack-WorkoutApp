package com.swparks.ui.screens.auth

import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swparks.ui.viewmodel.LoginViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Хост для LoginScreen в виде полноэкранного модального листа.
 *
 * LoginScreen открывается как ModalBottomSheet на весь экран поверх текущего UI.
 * Закрытие листа разрешено только:
 * - по нажатию на крестик в левом верхнем углу (только если !uiState.isBusy: не идёт логин и не загружаются данные)
 * - автоматически после успешной авторизации
 *
 * Закрытие по тапу вне области, свайпу вниз, системной кнопке/жесту "назад" — запрещено.
 * Все жесты блокируются на уровне контента через Modifier.
 *
 * ВАЖНО: Загрузка данных пользователя выполняется в ProfileViewModel при открытии профиля.
 *
 * @param show Флаг для показа/скрытия листа
 * @param onDismissed Callback при закрытии листа
 * @param onLoginSuccess Callback при успешной авторизации
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginSheetHost(
    show: Boolean,
    onDismissed: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var allowHide by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Создаем ViewModel на уровне хоста
    val loginViewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory)
    val uiState by loginViewModel.uiState.collectAsState()

    // Сбрасываем состояние при каждом открытии sheet
    LaunchedEffect(show) {
        if (show) {
            loginViewModel.resetForNewSession()
        }
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            // Запрещаем скрытие всегда, кроме явного close/success
            if (newValue == SheetValue.Hidden) allowHide else true
        }
    )

    if (show) {
        ModalBottomSheet(
            onDismissRequest = {
                // Игнорируем тап вне sheet / системный dismiss
            },
            sheetState = sheetState,
            dragHandle = {}, // СКРЫВАЕМ визуальный drag handle (pin вверху)
            properties = ModalBottomSheetProperties(
                shouldDismissOnBackPress = false // ЗАПРЕЩАЕМ системную кнопку "назад"
            ),
        ) {
            LoginScreen(
                viewModel = loginViewModel,
                onDismiss = {
                    if (uiState.isBusy) return@LoginScreen
                    scope.launch {
                        allowHide = true
                        sheetState.hide()
                        allowHide = false
                        onDismissed()
                    }
                },
                onLoginSuccess = {
                    scope.launch {
                        allowHide = true
                        sheetState.hide()
                        allowHide = false
                        onLoginSuccess()
                    }
                },
                modifier = Modifier.disableAllGestures() // Блокируем все жесты для всего контента sheet
            )
        }
    }
}

/**
 * Модификатор для блокировки всех вертикальных жестов (свайп вниз/вверх) и горизонтальных жестов.
 *
 * Перехватывает все жесты и блокирует их для предотвращения случайных действий.
 * Используется для предотвращения случайного закрытия ModalBottomSheet жестами.
 *
 * @param threshold Минимальное расстояние свайпа для активации блокировки
 */
fun Modifier.disableAllGestures(
    threshold: Float = 0.5f
): Modifier = pointerInput(Unit) {
    detectDragGestures { change, dragAmount ->
        // Блокируем все жесты (как вертикальные, так и горизонтальные)
        // Порог чувствительности для игнорирования случайных касаний
        if (abs(dragAmount.x) > threshold || abs(dragAmount.y) > threshold) {
            change.consume()
        }
    }
}
