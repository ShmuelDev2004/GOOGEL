package app.sederyom.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.sederyom.R
import app.sederyom.ui.dayedit.DayEditScreen
import app.sederyom.ui.dayedit.DayEditViewModel
import app.sederyom.ui.settings.SettingsScreen
import app.sederyom.ui.settings.SettingsViewModel
import app.sederyom.ui.template.TemplateScreen
import app.sederyom.ui.template.TemplateViewModel
import app.sederyom.ui.today.TodayScreen
import app.sederyom.ui.today.TodayViewModel
import java.time.LocalDate

object Routes {
    const val TODAY = "today"
    const val TEMPLATE = "template"
    const val SETTINGS = "settings"
    const val ARG_EPOCH_DAY = "epochDay"
    const val DAY_EDIT = "dayedit/{$ARG_EPOCH_DAY}"

    fun dayEdit(epochDay: Long) = "dayedit/$epochDay"
}

private data class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.TODAY, R.string.nav_today, Icons.Default.Home),
    TopLevelDestination(Routes.TEMPLATE, R.string.nav_template, Icons.Default.DateRange),
    TopLevelDestination(Routes.SETTINGS, R.string.nav_settings, Icons.Default.Settings),
)

private fun NavDestination?.isIn(top: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.route == top.route } == true

@Composable
fun SederYomApp() {
    val container = rememberAppContainer()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = topLevelDestinations.any { destination.isIn(it) }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelDestinations.forEach { top ->
                        NavigationBarItem(
                            selected = destination.isIn(top),
                            onClick = {
                                navController.navigate(top.route) {
                                    popUpTo(Routes.TODAY) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(top.icon, contentDescription = null) },
                            label = { Text(stringResource(top.labelRes)) },
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.TODAY,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.TODAY) {
                val viewModel: TodayViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            TodayViewModel(
                                container.observeSchedule,
                                container.resolveDayPlan,
                                container.findNowAndNext,
                            )
                        }
                    },
                )
                TodayScreen(viewModel, onEditDay = { navController.navigate(Routes.dayEdit(it.toEpochDay())) })
            }
            composable(Routes.TEMPLATE) {
                val viewModel: TemplateViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            TemplateViewModel(
                                container.observeSchedule,
                                container.saveTemplateBlock,
                                container.deleteTemplateBlock,
                                container.suggestNewBlock,
                            )
                        }
                    },
                )
                TemplateScreen(viewModel)
            }
            composable(Routes.SETTINGS) {
                val viewModel: SettingsViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            SettingsViewModel(
                                container.observeSettings,
                                container.updateSettings,
                                container.exportBackup,
                                container.prepareImport,
                                container.applyImport,
                            )
                        }
                    },
                )
                SettingsScreen(viewModel)
            }
            composable(
                route = Routes.DAY_EDIT,
                arguments = listOf(navArgument(Routes.ARG_EPOCH_DAY) { type = NavType.LongType }),
            ) { entry ->
                val epochDay = entry.arguments?.getLong(Routes.ARG_EPOCH_DAY) ?: LocalDate.now().toEpochDay()
                val date = LocalDate.ofEpochDay(epochDay)
                val viewModel: DayEditViewModel = viewModel(
                    key = "dayedit-$epochDay",
                    factory = viewModelFactory {
                        initializer {
                            DayEditViewModel(
                                date,
                                container.observeSchedule,
                                container.resolveDayPlan,
                                container.validateBlock,
                                container.suggestNewBlock,
                                container.saveDayOverride,
                                container.resetDayOverride,
                            )
                        }
                    },
                )
                DayEditScreen(date, viewModel, onClose = { navController.popBackStack() })
            }
        }
    }
}
