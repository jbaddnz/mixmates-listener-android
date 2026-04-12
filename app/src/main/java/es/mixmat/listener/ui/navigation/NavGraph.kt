package es.mixmat.listener.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import es.mixmat.listener.ui.auth.TokenEntryScreen
import es.mixmat.listener.ui.history.HistoryDetailScreen
import es.mixmat.listener.ui.history.HistoryScreen
import es.mixmat.listener.ui.legal.LegalScreen
import es.mixmat.listener.ui.listen.ListenScreen
import es.mixmat.listener.ui.settings.SettingsScreen

object Routes {
    const val TOKEN_ENTRY = "token_entry"
    const val LISTEN = "listen"
    const val HISTORY = "history"
    const val HISTORY_DETAIL = "history/{id}"
    const val SETTINGS = "settings"
    const val LEGAL = "legal"

    fun historyDetail(id: String) = "history/$id"
}

@Composable
fun MixMatesNavGraph(
    navController: NavHostController,
    startDestination: String,
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.TOKEN_ENTRY) {
            TokenEntryScreen(
                onTokenSaved = {
                    navController.navigate(Routes.LISTEN) {
                        popUpTo(Routes.TOKEN_ENTRY) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.LISTEN) {
            ListenScreen(
                onNavigateToHistory = { navController.navigate(Routes.HISTORY) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToLegal = { navController.navigate(Routes.LEGAL) },
                onTokenCleared = {
                    navController.navigate(Routes.TOKEN_ENTRY) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.LEGAL) {
            LegalScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                onItemClick = { id -> navController.navigate(Routes.historyDetail(id)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.HISTORY_DETAIL) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            HistoryDetailScreen(
                historyId = id,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
