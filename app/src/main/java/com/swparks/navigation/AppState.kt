package com.swparks.navigation

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

/**
 * AppState для управления навигацией приложения
 */
@Composable
fun rememberAppState(
    navController: NavHostController = rememberNavController(),
): AppState {
    return remember(navController) {
        AppState(navController)
    }
}

/**
 * Состояние приложения для навигации и авторизации
 */
class AppState(
    val navController: NavHostController,
) {
    private val previousDestination =
        mutableStateOf<androidx.navigation.NavDestination?>(null)

    // ==================== Состояние авторизации ====================

    /**
     * Текущий авторизованный пользователь
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
     * Используется вместо прямого присваивания для контроля изменений
     */
    fun updateCurrentUser(user: User?) {
        currentUser = user
    }

    // ============================================================

    /**
     * Текущий пункт назначения навигации
     */
    val currentDestination: androidx.navigation.NavDestination?
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
     * Текущее верхнеуровневое назначение (вкладка)
     * Учитывает подэкраны — если мы на UserParks, вернет PROFILE
     */
    val currentTopLevelDestination: TopLevelDestination?
        @Composable get() {
            val route = currentDestination?.route ?: return null
            return findTopLevelDestinationForRoute(route)
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
     * Находит TopLevelDestination для маршрута (учитывает parentTab)
     */
    private fun findTopLevelDestinationForRoute(route: String): TopLevelDestination? {
        val baseRoute = route.substringBefore("/")

        // Сначала ищем точное совпадение с вкладкой
        topLevelDestinations.find {
            it.route.substringBefore("/") == baseRoute
        }?.let { return it }

        // Если не нашли, ищем через parentTab (для подэкранов)
        val parentTabRoute = Screen.findParentTab(route)?.route?.substringBefore("/")
        return topLevelDestinations.find {
            it.route.substringBefore("/") == parentTabRoute
        }
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
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        val currentTab = currentRoute?.let { findTopLevelDestinationForRoute(it) }
        val isReselect = currentTab?.route == topLevelDestination.route

        if (isReselect) {
            // Повторное нажатие на текущую вкладку — сбрасываем стек до корня
            navController.navigate(topLevelDestination.route) {
                // Удаляем весь стек выше корня вкладки (включительно),
                // затем создаем заново
                popUpTo(topLevelDestination.route) {
                    inclusive = true  // Удаляем и сам корень вкладки
                }
                launchSingleTop = true
                // restoreState = false — не восстанавливаем, а создаем заново
            }
        } else {
            // Переход на другую вкладку — стандартная логика
            navController.navigate(topLevelDestination.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    /**
     * Навигация на вкладку профиля
     */
    fun navigateToProfile() {
        navigateToTopLevelDestination(PROFILE)
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
