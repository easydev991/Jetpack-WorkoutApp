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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.swparks.R
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
 * Состояние приложения для навигации
 */
class AppState(
    val navController: NavHostController,
) {
    private val previousDestination =
        mutableStateOf<androidx.navigation.NavDestination?>(null)

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
     */
    val currentTopLevelDestination: TopLevelDestination?
        @Composable get() {
            return topLevelDestinations.firstOrNull { destination ->
                currentDestination?.route == destination.route
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
     * Верхнеуровневые назначения имеют только одну копию назначения в стеке,
     * сохраняют и восстанавливают состояние при навигации.
     *
     * @param topLevelDestination: Назначение, к которому нужно перейти.
     */
    fun navigateToTopLevelDestination(topLevelDestination: TopLevelDestination) {
        navController.navigate(topLevelDestination.route) {
            // Очищаем стек до стартовой точки назначения в графе,
            // чтобы избежать накопления большого стека при выборе элементов
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            // Избегаем множественных копий одного назначения при повторном выборе
            launchSingleTop = true
            // Восстанавливаем состояние при повторном выборе элемента
            restoreState = true
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
