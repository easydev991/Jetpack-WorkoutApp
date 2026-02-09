package com.swparks.ui.screens.auth

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swparks.ui.ds.disableAllGestures
import com.swparks.ui.viewmodel.ILoginViewModel
import com.swparks.ui.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

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
 * @param onLoginSuccess Callback при успешной авторизации с userId
 * @param onResetSuccess Callback при успешном сбросе пароля с email (опционально)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginSheetHost(
    show: Boolean,
    onDismissed: () -> Unit,
    onLoginSuccess: (userId: Long) -> Unit,
    onResetSuccess: (String) -> Unit = {} // Новый параметр (опционально)
) {
    var allowHide by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Создаем ViewModel на уровне хоста с явным указанием типа
    val loginViewModel: ILoginViewModel =
        viewModel<LoginViewModel>(factory = LoginViewModel.Factory)
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
                onLoginSuccess = { userId ->
                    scope.launch {
                        allowHide = true
                        sheetState.hide()
                        allowHide = false
                        onLoginSuccess(userId)
                    }
                },
                onResetSuccess = onResetSuccess, // Передаем обработчик (если нужен проброс наверх)
                modifier = Modifier.disableAllGestures() // Блокируем все жесты для всего контента sheet
            )
        }
    }
}