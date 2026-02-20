package com.swparks.navigation

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.swparks.R
import com.swparks.data.model.User
import com.swparks.navigation.TopLevelDestinations.EVENTS
import com.swparks.navigation.TopLevelDestinations.MESSAGES
import com.swparks.navigation.TopLevelDestinations.MORE
import com.swparks.navigation.TopLevelDestinations.PARKS
import com.swparks.navigation.TopLevelDestinations.PROFILE

private const val TAG = "Navigation"

/**
 * AppState для управления навигацией приложения
 */
@Composable
fun rememberAppState(
    navController: NavHostController = rememberNavController(),
): AppState {
    val appState = remember(navController) {
        AppState(navController)
    }

    // Слушаем изменения навигации для обновления активной вкладки
    DisposableEffect(navController) {
        Log.d(TAG, "DisposableEffect: добавляем OnDestinationChangedListener")
        val listener =
            NavController.OnDestinationChangedListener { _, destination, _ ->
                Log.d(TAG, "OnDestinationChangedListener: destination=${destination.route}")
                appState.onDestinationChanged(destination.route)
            }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            Log.d(TAG, "DisposableEffect: удаляем OnDestinationChangedListener")
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    return appState
}

/**
 * Состояние приложения для навигации и авторизации
 */
class AppState(
    val navController: NavHostController,
) {
    private val previousDestination =
        mutableStateOf<NavDestination?>(null)

    // ==================== Состояние авторизации ====================

    /**
     * Текущий авторизованный пользователь
     *
     * Используем mutableStateOf для обеспечения рекомпозиции в Compose
     * private set запрещает прямое присваивание, заставляя использовать updateCurrentUser()
     */
    var currentUser by mutableStateOf<User?>(null)
        private set

    /**
     * Флаг авторизации пользователя
     * Вычисляемое свойство, которое автоматически пересчитывается при изменении currentUser
     */
    val isAuthorized: Boolean
        get() = currentUser != null

    /**
     * Безопасное обновление текущего пользователя
     *
     * Используется вместо прямого присваивания для контроля изменений
     */
    fun updateCurrentUser(user: User?) {
        currentUser = user
    }

    // ============================================================

    /**
     * Текущий пункт назначения навигации
     */
    val currentDestination: NavDestination?
        @Composable get() {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentEntry = navBackStackEntry

            return currentEntry?.destination.also { destination ->
                if (destination != null) {
                    previousDestination.value = destination
                }
            } ?: previousDestination.value
        }

    /**
     * Текущее верхнеуровневое назначение (вкладка).
     * Состояние "липкое": обновляется только когда мы попадаем на корневой экран вкладки.
     * Если мы уходим вглубь вкладки, это поле хранит ссылку на "родительскую" вкладку.
     * Это позволяет корректно определять физический стек навигации.
     */
    var currentTopLevelDestination by mutableStateOf<TopLevelDestination?>(null)
        private set

    /**
     * Обновляет активную вкладку на основе текущего маршрута.
     * Проверяет как прямое совпадение с корневыми вкладками, так и parentTab для дочерних экранов.
     * Это позволяет корректно определять активную вкладку даже когда restoreState
     * восстанавливает стек с дочерними экранами (например, edit_profile внутри Profile).
     */
    fun onDestinationChanged(route: String?) {
        Log.d(TAG, "onDestinationChanged: route=$route")

        // Сначала проверяем прямое совпадение с корневой вкладкой
        val matchingTab = topLevelDestinations.find { it.route == route }
        if (matchingTab != null) {
            Log.d(
                TAG,
                "  -> найдена вкладка: ${matchingTab.route}, обновляем currentTopLevelDestination"
            )
            currentTopLevelDestination = matchingTab
            return
        }

        // Если прямого совпадения нет, проверяем parentTab для дочерних экранов
        // Это решает проблему: когда restoreState восстанавливает стек с дочерним экраном,
        // onDestinationChanged вызывается с маршрутом дочернего экрана, а не корневого
        val parentTab = Screen.findParentTab(route ?: "")
        if (parentTab != null) {
            val parentTopLevelDestination =
                topLevelDestinations.find { it.route == parentTab.route }
            if (parentTopLevelDestination != null) {
                Log.d(
                    TAG,
                    "  -> дочерний экран с parentTab=${parentTab.route}, обновляем currentTopLevelDestination"
                )
                currentTopLevelDestination = parentTopLevelDestination
                return
            }
        }

        Log.d(TAG, "  -> маршрут не соответствует никакой вкладке")
    }

    /**
     * Проверяет, является ли текущий маршрут корневым (верхнеуровневым) экраном
     * или дочерним (подэкраном внутри вкладки)
     * Возвращает true только для корневых экранов без parentTab
     */
    val isCurrentRouteTopLevel: Boolean
        @Composable get() {
            val route = currentDestination?.route ?: return false
            val baseRoute = route.substringBefore("/")
            val screen = Screen.allScreens.find {
                it.route.substringBefore("/") == baseRoute
            }
            return screen?.parentTab == null
        }

    /**
     * Список верхнеуровневых назначений (вкладок)
     */
    val topLevelDestinations: List<TopLevelDestination> = listOf(
        PARKS,
        EVENTS,
        MESSAGES,
        PROFILE,
        MORE,
    )

    /**
     * UI логика для навигации к верхнеуровневому назначению.
     * При повторном нажатии на текущую вкладку сбрасывает стек на корень.
     *
     * @param topLevelDestination: Назначение, к которому нужно перейти.
     */
    fun navigateToTopLevelDestination(topLevelDestination: TopLevelDestination) {
        // Проверяем, активна ли уже эта вкладка
        // Мы используем "липкое" состояние currentTopLevelDestination, которое помнит,
        // в какой вкладке мы находимся, даже если ушли на дочерний экран.
        val isReselect = currentTopLevelDestination?.route == topLevelDestination.route

        Log.d(
            TAG,
            "navigateToTopLevelDestination: ${topLevelDestination.route}, isReselect=$isReselect, currentTopLevelDestination=${currentTopLevelDestination?.route}"
        )

        if (isReselect) {
            // Повторное нажатие на текущую вкладку — сбрасываем стек до корня
            Log.d(
                TAG,
                "  -> повторное нажатие: сбрасываем стек до корня ${topLevelDestination.route}"
            )
            navController.navigate(topLevelDestination.route) {
                // Удаляем весь стек выше корня вкладки (включительно),
                // затем создаем заново
                popUpTo(topLevelDestination.route) {
                    inclusive = true  // Удаляем и сам корень вкладки
                }
                launchSingleTop = true
                // restoreState = false — не восстанавливаем, а создаем заново
            }
            Log.d(
                TAG,
                "  -> navigate() вызван с popUpTo(${topLevelDestination.route}, inclusive=true)"
            )
        } else {
            // Переход на другую вкладку — стандартная логика
            Log.d(TAG, "  -> переход на другую вкладку: ${topLevelDestination.route}")
            navController.navigate(topLevelDestination.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            Log.d(
                TAG,
                "  -> navigate() вызван с popUpTo(startDestination), saveState=true, restoreState=true"
            )
        }
    }

}

/**
 * Определения верхнеуровневых назначений (вкладок)
 */
object TopLevelDestinations {
    val PARKS = TopLevelDestination(
        route = Screen.Parks.route,
        selectedIcon = Icons.Filled.Map,
        unselectedIcon = Icons.Outlined.Map,
        iconTextId = R.string.parks,
        titleTextId = R.string.parks_title,
    )

    val EVENTS = TopLevelDestination(
        route = Screen.Events.route,
        selectedIcon = Icons.Filled.Event,
        unselectedIcon = Icons.Outlined.Event,
        iconTextId = R.string.events,
        titleTextId = R.string.events_title,
    )

    val MESSAGES = TopLevelDestination(
        route = Screen.Messages.route,
        selectedIcon = Icons.AutoMirrored.Filled.Chat,
        unselectedIcon = Icons.AutoMirrored.Outlined.Chat,
        iconTextId = R.string.messages,
        titleTextId = R.string.messages_title,
    )

    val PROFILE = TopLevelDestination(
        route = Screen.Profile.route,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        iconTextId = R.string.profile,
        titleTextId = R.string.profile_title,
    )

    val MORE = TopLevelDestination(
        route = Screen.More.route,
        selectedIcon = Icons.AutoMirrored.Filled.List,
        unselectedIcon = Icons.AutoMirrored.Outlined.List,
        iconTextId = R.string.more,
        titleTextId = R.string.more_title,
    )
}
