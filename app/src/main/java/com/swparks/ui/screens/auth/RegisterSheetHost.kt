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

    val registerViewModel: IRegisterViewModel = remember(appContainer) {
        appContainer.registerViewModelFactory()
    }
    val uiState by registerViewModel.uiState.collectAsState()

    val innerNavController: NavHostController = rememberNavController()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

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

    LaunchedEffect(show) {
        if (show) {
            registerViewModel.resetForNewSession()
        }
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
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
            onDismissRequest = {},
            sheetState = sheetState,
            dragHandle = null,
            properties = ModalBottomSheetProperties(
                shouldDismissOnBackPress = false,
                shouldDismissOnClickOutside = false
            ),
        ) {
            RegisterSheetContent(
                registerViewModel = registerViewModel,
                innerNavController = innerNavController,
                snackbarHostState = snackbarHostState,
                onRegisterSuccess = onRegisterSuccess,
                onDismissed = onDismissed,
                uiState = uiState,
                dismissSheet = { dismissSheet(it) }
            )
        }
    }
}

@Composable
private fun RegisterSheetContent(
    registerViewModel: IRegisterViewModel,
    innerNavController: NavHostController,
    snackbarHostState: SnackbarHostState,
    onRegisterSuccess: (userId: Long) -> Unit,
    onDismissed: () -> Unit,
    uiState: RegisterUiState,
    dismissSheet: (() -> Unit) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
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

        SnackbarHost(
            hostState = snackbarHostState,
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
                        RegisterNavigationAction.SelectCountry -> innerNavController.navigate(
                            RegisterRoutes.SELECT_COUNTRY
                        )

                        RegisterNavigationAction.SelectCity -> innerNavController.navigate(
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
