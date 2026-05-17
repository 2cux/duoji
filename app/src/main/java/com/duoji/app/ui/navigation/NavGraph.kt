package com.duoji.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.duoji.app.ui.confirm.ConfirmScreen
import com.duoji.app.ui.home.HomeScreen
import com.duoji.app.ui.record.RecordScreen

object Routes {
    const val HOME = "home"
    const val RECORD = "record"
    const val CONFIRM = "confirm"
}

@Composable
fun DuoJiNavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToRecord = {
                    navController.navigate(Routes.RECORD)
                }
            )
        }

        composable(Routes.RECORD) {
            RecordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToConfirm = {
                    navController.navigate(Routes.CONFIRM) {
                        // Remove record from back stack so back from confirm goes to home
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.CONFIRM) {
            ConfirmScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateHome = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }
    }
}
