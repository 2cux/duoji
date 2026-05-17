package com.duoji.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.duoji.app.ui.bill.BillEditScreen
import com.duoji.app.ui.bill.BillListScreen
import com.duoji.app.ui.confirm.ConfirmScreen
import com.duoji.app.ui.home.HomeScreen
import com.duoji.app.ui.manual.ManualRecordScreen
import com.duoji.app.ui.record.RecordScreen
import com.duoji.app.ui.settings.SettingsScreen
import com.duoji.app.ui.statistics.StatisticsScreen

object Routes {
    const val HOME = "home"
    const val RECORD = "record"
    const val CONFIRM = "confirm"
    const val BILL_LIST = "billList"
    const val BILL_EDIT = "billEdit/{transactionId}"
    const val STATISTICS = "statistics"
    const val SETTINGS = "settings"
    const val MANUAL_RECORD = "manualRecord"

    fun billEdit(transactionId: String) = "billEdit/$transactionId"
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
                },
                onNavigateToBillList = {
                    navController.navigate(Routes.BILL_LIST) {
                        launchSingleTop = true
                    }
                },
                onNavigateToStatistics = {
                    navController.navigate(Routes.STATISTICS) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onNavigateToManualRecord = {
                    navController.navigate(Routes.MANUAL_RECORD)
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
                        launchSingleTop = true
                    }
                },
                onNavigateToManualRecord = {
                    navController.navigate(Routes.MANUAL_RECORD)
                }
            )
        }

        composable(Routes.CONFIRM) {
            ConfirmScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToBillList = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                    navController.navigate(Routes.BILL_LIST) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.BILL_LIST) {
            BillListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEdit = { transactionId ->
                    navController.navigate(Routes.billEdit(transactionId))
                },
                onNavigateToRecord = {
                    navController.navigate(Routes.RECORD) {
                        launchSingleTop = true
                    }
                },
                onNavigateToStatistics = {
                    navController.navigate(Routes.STATISTICS) {
                        launchSingleTop = true
                    }
                },
                onNavigateToManualRecord = {
                    navController.navigate(Routes.MANUAL_RECORD)
                }
            )
        }

        composable(Routes.BILL_EDIT) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId")
                ?: return@composable
            BillEditScreen(
                transactionId = transactionId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.STATISTICS) {
            StatisticsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRecord = {
                    navController.navigate(Routes.RECORD) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.MANUAL_RECORD) {
            ManualRecordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
