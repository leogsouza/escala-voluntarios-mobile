package br.com.leogsouza.escalav.ui

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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

    LaunchedEffect(Unit) { authViewModel.checkSession() }

    val startDestination = if (authState is AuthState.Success) Screen.Calendar.route else Screen.Login.route

    NavHost(navController = navController, startDestination = startDestination) {
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
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
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
