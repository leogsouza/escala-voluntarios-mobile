package br.com.leogsouza.escalav.ui

import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Event
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.Modifier
import br.com.leogsouza.escalav.ui.auth.AuthState
import br.com.leogsouza.escalav.ui.auth.AuthViewModel
import br.com.leogsouza.escalav.ui.auth.LoginScreen
import br.com.leogsouza.escalav.ui.restrictions.RestrictionFormScreen
import br.com.leogsouza.escalav.ui.restrictions.RestrictionsScreen
import br.com.leogsouza.escalav.ui.schedule.DayDetailScreen
import br.com.leogsouza.escalav.ui.schedule.EventDetailScreen
import br.com.leogsouza.escalav.ui.schedule.ScheduleCalendarScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Calendar : Screen("calendar")
    object DayDetail : Screen("day/{date}") {
        fun createRoute(date: String) = "day/$date"
    }
    object EventDetail : Screen("event/{eventId}") {
        fun createRoute(id: Int) = "event/$id"
    }
    object Restrictions : Screen("restrictions")
    object NewRestriction : Screen("restrictions/new")
    object EditRestriction : Screen("restrictions/{id}/edit") {
        fun createRoute(id: Int) = "restrictions/$id/edit"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.state.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    LaunchedEffect(Unit) { authViewModel.checkSession() }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            navController.navigate(Screen.Calendar.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
        if (authState is AuthState.LoggedOut) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val bottomNavItems = listOf(Screen.Calendar, Screen.Restrictions)
    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        val icon = when (item) {
                            Screen.Calendar -> Icons.Filled.Event
                            Screen.Restrictions -> Icons.AutoMirrored.Filled.EventNote
                            else -> Icons.Filled.Event
                        }
                        val label = when (item) {
                            Screen.Calendar -> "Calendário"
                            Screen.Restrictions -> "Restrições"
                            else -> item.route
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(Screen.Calendar.route)
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    ) { scaffoldPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route,
            modifier = Modifier.padding(scaffoldPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(onLoginSuccess = {
                    navController.navigate(Screen.Calendar.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.Calendar.route) {
                ScheduleCalendarScreen(
                    onDayClick = { date -> navController.navigate(Screen.DayDetail.createRoute(date)) },
                    onRestrictionsClick = { navController.navigate(Screen.Restrictions.route) },
                    onLogout = { authViewModel.logout() }
                )
            }
            composable(Screen.DayDetail.route) { back ->
                val date = back.arguments?.getString("date") ?: return@composable
                DayDetailScreen(
                    date = date,
                    onEventClick = { id -> navController.navigate(Screen.EventDetail.createRoute(id)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.EventDetail.route) { back ->
                val id = back.arguments?.getString("eventId")?.toIntOrNull() ?: return@composable
                EventDetailScreen(eventId = id, onBack = { navController.popBackStack() })
            }
            composable(Screen.Restrictions.route) {
                RestrictionsScreen(
                    onNewRestriction = { navController.navigate(Screen.NewRestriction.route) },
                    onEditRestriction = { id -> navController.navigate(Screen.EditRestriction.createRoute(id)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.NewRestriction.route) {
                RestrictionFormScreen(
                    restrictionId = null,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.EditRestriction.route) { back ->
                val id = back.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                RestrictionFormScreen(
                    restrictionId = id,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
