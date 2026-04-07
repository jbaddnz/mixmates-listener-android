package es.mixmat.listener.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import es.mixmat.listener.data.api.AuthEvent
import es.mixmat.listener.ui.auth.TokenEntryViewModel
import es.mixmat.listener.ui.navigation.MixMatesNavGraph
import es.mixmat.listener.ui.navigation.Routes

@Composable
fun MixMatesListenerApp(authEvent: AuthEvent) {
    val navController = rememberNavController()
    val tokenViewModel: TokenEntryViewModel = hiltViewModel()
    val startDestination = if (tokenViewModel.hasToken()) Routes.LISTEN else Routes.TOKEN_ENTRY

    LaunchedEffect(Unit) {
        authEvent.tokenExpired.collect {
            navController.navigate(Routes.TOKEN_ENTRY) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        MixMatesNavGraph(
            navController = navController,
            startDestination = startDestination,
        )
    }
}
