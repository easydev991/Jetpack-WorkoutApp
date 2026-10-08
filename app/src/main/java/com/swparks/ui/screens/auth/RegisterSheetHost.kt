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
import com.swparks.data.DefaultAppContainer
import com.swparks.ui.state.RegisterUiState
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
 * Хост для экранов регистрации в виде полноэкранного модального листа
 * с внутренней навигацией (регистрация → выбор страны → выбор города).
 *
 * Закрытие листа разрешено только:
 * - по нажатию на крестик (только если !uiState.isBusy: не идёт регистрация)
 * - по системной кнопке или жесту «назад» (только если !uiState.isBusy)
 * - автоматически после успешной регистрации
 *
 * Закрытие по тапу вне области и свайпу вниз запрещено.
 * Обратный свайп листа не срабатывает: перетаскивание отключено параметром
 * [ModalBottomSheet.sheetGesturesEnabled], взаимодействие с контентом не ограничивается.
 *
 * @param show Флаг для показа/скрытия листа
 * @param appContainer DI-контейнер приложения (опционально; нужен для создания ViewModel
 * и глобальных ошибок, в UI-тестах не передаётся)
 * @param onDismissed Callback при закрытии листа
 * @param onRegisterSuccess Callback при успешной регистрации с userId
 * @param viewModel ViewModel регистрации (опционально; при null создаётся из [appContainer]).
 * Параметр для UI-тестов с фейковой реализацией
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterSheetHost(
    show: Boolean,
    appContainer: DefaultAppContainer? = null,
    onDismissed: () -> Unit,
    onRegisterSuccess: (userId: Long) -> Unit,
    viewModel: IRegisterViewModel? = null
) {
    var allowHide by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // ViewModel передаётся извне в UI-тестах; иначе создаётся из DI-контейнера
    val registerViewModel: IRegisterViewModel =
        viewModel ?: remember(appContainer) {
            appContainer?.registerViewModelFactory()
                ?: error("RegisterSheetHost: не переданы viewModel или appContainer")
        }
    val uiState by registerViewModel.uiState.collectAsState()
    val innerNavController: NavHostController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { newValue ->
                when (newValue) {
                    SheetValue.Expanded -> true
                    SheetValue.PartiallyExpanded -> false
                    SheetValue.Hidden -> allowHide
                }
            }
        )

    LaunchedEffect(appContainer) {
        appContainer?.userNotifier?.errorFlow?.collect { error ->
            snackbarHostState.showSnackbar(
                message = error.toUiText(context),
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
        }
    }

    LaunchedEffect(show) {
        if (show) registerViewModel.resetForNewSession()
    }

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
            dragHandle = null,
            properties =
                ModalBottomSheetProperties(
                    shouldDismissOnBackPress = true,
                    shouldDismissOnClickOutside = false
                ),
            sheetGesturesEnabled = false
        ) {
            RegisterSheetContent(
                registerViewModel = registerViewModel,
                innerNavController = innerNavController,
                uiState = uiState,
                callbacks =
                    RegisterSheetCallbacks(
                        onRegisterSuccess = onRegisterSuccess,
                        onDismissed = onDismissed
                    ),
                config =
                    RegisterSheetConfig(
                        snackbarHostState = snackbarHostState,
                        dismissSheet = { dismissSheet(it) }
                    )
            )
        }
    }
}

private data class RegisterSheetCallbacks(
    val onRegisterSuccess: (userId: Long) -> Unit,
    val onDismissed: () -> Unit
)

private data class RegisterSheetConfig(
    val snackbarHostState: SnackbarHostState,
    val dismissSheet: (() -> Unit) -> Unit
)

@Composable
private fun RegisterSheetContent(
    registerViewModel: IRegisterViewModel,
    innerNavController: NavHostController,
    uiState: RegisterUiState,
    callbacks: RegisterSheetCallbacks,
    config: RegisterSheetConfig
) {
    Box(modifier = Modifier.fillMaxSize()) {
        RegisterNavHost(
            viewModel = registerViewModel,
            innerNavController = innerNavController,
            onRegisterSuccess = { userId ->
                config.dismissSheet { callbacks.onRegisterSuccess(userId) }
            },
            onClose = {
                if (uiState.isBusy) return@RegisterNavHost
                config.dismissSheet(callbacks.onDismissed)
            }
        )

        SnackbarHost(
            hostState = config.snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
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
                onNavigationAction = { action ->
                    when (action) {
                        is RegisterNavigationAction.RegisterSuccess -> onRegisterSuccess(action.userId)
                        RegisterNavigationAction.Close -> onClose()
                        RegisterNavigationAction.SelectCountry ->
                            innerNavController.navigate(
                                RegisterRoutes.SELECT_COUNTRY
                            )

                        RegisterNavigationAction.SelectCity ->
                            innerNavController.navigate(
                                RegisterRoutes.SELECT_CITY
                            )
                    }
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
