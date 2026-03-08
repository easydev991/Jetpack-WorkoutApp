package com.swparks.ui.screens.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.swparks.data.AppContainer
import com.swparks.ui.ds.disableAllGestures
import com.swparks.ui.viewmodel.IRegisterViewModel
import com.swparks.util.toUiText
import kotlinx.coroutines.launch

/**
 * Экраны для внутренней навигации в sheet регистрации.
 */
private object RegisterRoutes {
    const val REGISTER = "register"
    const val SELECT_COUNTRY = "select_country"
    const val SELECT_CITY = "select_city"
}

/**
 * Хост для RegisterUserScreen в виде полноэкранного модального листа.
 *
 * RegisterUserScreen открывается как ModalBottomSheet на весь экран поверх текущего UI.
 * Закрытие листа разрешено только:
 * - по нажатию на крестик в левом верхнем углу (только если !uiState.isBusy)
 * - автоматически после успешной регистрации
 *
 * Закрытие по тапу вне области, свайпу вниз, системной кнопке/жесту "назад" — запрещено.
 *
 * Внутри sheet используется отдельная навигация для переходов между экранами:
 * - RegisterUserScreen (главный экран регистрации)
 * - RegisterSelectCountryScreen (выбор страны)
 * - RegisterSelectCityScreen (выбор города)
 *
 * @param show Флаг для показа/скрытия листа
 * @param appContainer DI контейнер для создания ViewModel
 * @param onDismissed Callback при закрытии листа
 * @param onRegisterSuccess Callback при успешной регистрации с userId
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterSheetHost(
    show: Boolean,
    appContainer: AppContainer,
    onDismissed: () -> Unit,
    onRegisterSuccess: (userId: Long) -> Unit
) {
    var allowHide by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Создаем ViewModel через factory метод
    val registerViewModel: IRegisterViewModel = remember(appContainer) {
        appContainer.registerViewModelFactory()
    }
    val uiState by registerViewModel.uiState.collectAsState()

    // Создаем внутренний NavController для навигации внутри sheet
    val innerNavController: NavHostController = rememberNavController()

    // SnackbarHostState для отображения ошибок внутри ModalBottomSheet
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Подписываемся на ошибки из UserNotifier для отображения в Snackbar
    LaunchedEffect(Unit) {
        appContainer.userNotifier.errorFlow.collect { error ->
            val message = error.toUiText(context)
            snackbarHostState.showSnackbar(
                message = message,
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Сбрасываем состояние при каждом открытии sheet
    LaunchedEffect(show) {
        if (show) {
            registerViewModel.resetForNewSession()
        }
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            // Запрещаем любые изменения состояния sheet'а, кроме явного закрытия
            when (newValue) {
                SheetValue.Expanded -> true
                SheetValue.PartiallyExpanded -> false
                SheetValue.Hidden -> allowHide
            }
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
                // Игнорируем тап вне sheet / системный dismiss
            },
            sheetState = sheetState,
            dragHandle = null,
            properties = ModalBottomSheetProperties(
                shouldDismissOnBackPress = false,
                shouldDismissOnClickOutside = false
            ),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Внутренняя навигация внутри sheet
                RegisterNavHost(
                    viewModel = registerViewModel,
                    innerNavController = innerNavController,
                    onRegisterSuccess = { userId ->
                        dismissSheet { onRegisterSuccess(userId) }
                    },
                    onClose = {
                        if (uiState.isBusy) return@RegisterNavHost
                        dismissSheet(onDismissed)
                    },
                    modifier = Modifier.disableAllGestures()
                )

                // Snackbar внизу экрана (поверх содержимого sheet)
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

/**
 * Внутренний NavHost для навигации между экранами регистрации.
 */
@Composable
private fun RegisterNavHost(
    viewModel: IRegisterViewModel,
    innerNavController: NavHostController,
    onRegisterSuccess: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = innerNavController,
        startDestination = RegisterRoutes.REGISTER,
        modifier = modifier
    ) {
        composable(RegisterRoutes.REGISTER) {
            RegisterUserScreen(
                viewModel = viewModel,
                onRegisterSuccess = onRegisterSuccess,
                onClose = onClose,
                onSelectCountry = {
                    innerNavController.navigate(RegisterRoutes.SELECT_COUNTRY)
                },
                onSelectCity = {
                    innerNavController.navigate(RegisterRoutes.SELECT_CITY)
                }
            )
        }

        composable(RegisterRoutes.SELECT_COUNTRY) {
            RegisterSelectCountryScreen(
                viewModel = viewModel,
                onBackClick = {
                    innerNavController.popBackStack()
                }
            )
        }

        composable(RegisterRoutes.SELECT_CITY) {
            RegisterSelectCityScreen(
                viewModel = viewModel,
                onBackClick = {
                    innerNavController.popBackStack()
                }
            )
        }
    }
}
