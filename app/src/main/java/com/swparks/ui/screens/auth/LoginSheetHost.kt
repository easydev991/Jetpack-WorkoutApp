package com.swparks.ui.screens.auth

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swparks.ui.viewmodel.ILoginViewModel
import com.swparks.ui.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

/**
 * Хост для LoginScreen в виде полноэкранного модального листа.
 *
 * LoginScreen открывается как ModalBottomSheet на весь экран поверх текущего UI.
 * Закрытие листа разрешено только:
 * - по нажатию на крестик в левом верхнем углу (только если !uiState.isBusy: не идёт логин и не загружаются данные)
 * - по системной кнопке или жесту «назад» (только если !uiState.isBusy)
 * - автоматически после успешной авторизации
 *
 * Закрытие по тапу вне области и свайпу вниз запрещено.
 * Обратный свайп листа не срабатывает: перетаскивание отключено параметром
 * [ModalBottomSheet.sheetGesturesEnabled], взаимодействие с контентом не ограничивается.
 *
 * ВАЖНО: Загрузка данных пользователя выполняется в ProfileViewModel при открытии профиля.
 *
 * @param show Флаг для показа/скрытия листа
 * @param onDismissed Callback при закрытии листа
 * @param onLoginSuccess Callback при успешной авторизации с userId
 * @param onResetSuccess Callback при успешном сбросе пароля с email (опционально)
 * @param viewModel ViewModel авторизации (опционально; при null создаётся на уровне хоста).
 * Параметр для UI-тестов с фейковой реализацией
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginSheetHost(
    show: Boolean,
    onDismissed: () -> Unit,
    onLoginSuccess: (userId: Long) -> Unit,
    // Новый параметр (опционально)
    onResetSuccess: (String) -> Unit = {},
    viewModel: ILoginViewModel? = null
) {
    var allowHide by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // ViewModel передаётся извне в UI-тестах; иначе создаётся на уровне хоста
    val loginViewModel: ILoginViewModel =
        viewModel ?: viewModel<LoginViewModel>(factory = LoginViewModel.Factory)
    val uiState by loginViewModel.uiState.collectAsStateWithLifecycle()

    // Сбрасываем состояние при каждом открытии sheet
    LaunchedEffect(show) {
        if (show) {
            loginViewModel.resetForNewSession()
        }
    }

    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { newValue ->
                // Разрешаем скрытие только через явное закрытие (крестик, возврат, успех)
                if (newValue == SheetValue.Hidden) allowHide else true
            }
        )

    fun dismissSheet(onComplete: () -> Unit) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        scope.launch {
            allowHide = true
            sheetState.hide()
            allowHide = false
            onComplete()
        }
    }

    if (show) {
        ModalBottomSheet(
            onDismissRequest = {
                if (uiState.isBusy) return@ModalBottomSheet
                dismissSheet(onDismissed)
            },
            sheetState = sheetState,
            dragHandle = {}, // СКРЫВАЕМ визуальный drag handle (pin вверху)
            properties =
                ModalBottomSheetProperties(
                    shouldDismissOnBackPress = true,
                    shouldDismissOnClickOutside = false
                ),
            sheetGesturesEnabled = false
        ) {
            LoginScreen(
                viewModel = loginViewModel,
                onDismiss = {
                    if (uiState.isBusy) return@LoginScreen
                    dismissSheet(onDismissed)
                },
                onLoginSuccess = { userId ->
                    dismissSheet { onLoginSuccess(userId) }
                },
                onResetSuccess = onResetSuccess // Передаем обработчик (если нужен проброс наверх)
            )
        }
    }
}
