package com.muddassir.deathcode.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.muddassir.deathcode.ui.admin.AdminScreen
import com.muddassir.deathcode.ui.browse.BrowseScreen
import com.muddassir.deathcode.ui.community.CommunityScreen
import com.muddassir.deathcode.ui.home.HomeScreen
import com.muddassir.deathcode.ui.keyboard.KeyboardScreen
import com.muddassir.deathcode.ui.notes.NotesScreen
import com.muddassir.deathcode.ui.search.SearchScreen
import com.muddassir.deathcode.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val NOTES = "notes"
    const val COMMUNITY = "community"
    const val KEYBOARD = "keyboard"
    const val SETTINGS = "settings"
    const val SEARCH = "search"
    const val ADMIN = "admin"
    const val BROWSE = "browse/{nodeId}"
    const val ADMIN_BROWSE = "adminBrowse/{nodeId}"

    fun browse(nodeId: String) = "browse/$nodeId"
    fun adminBrowse(nodeId: String) = "adminBrowse/$nodeId"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Filled.Home),
    Tab(Routes.NOTES, "My Notes", Icons.Filled.Edit),
    Tab(Routes.COMMUNITY, "Community", Icons.Filled.Person),
    Tab(Routes.KEYBOARD, "Keyboard", Icons.Filled.List),
    Tab(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
)

/**
 * Main navigation.
 *
 * The five top level destinations match the product structure (Home, My Notes, Community,
 * Keyboard, Settings); content browsing and search are pushed on top and support unlimited
 * nesting via a single reusable route.
 */
@Composable
fun DeathCodeNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenNode = { navController.navigate(Routes.browse(it)) },
                    onOpenSearch = { navController.navigate(Routes.SEARCH) },
                    onOpenNotes = { navController.navigate(Routes.NOTES) },
                )
            }

            composable(Routes.NOTES) {
                NotesScreen(onOpenNode = { navController.navigate(Routes.browse(it)) })
            }

            composable(Routes.COMMUNITY) {
                CommunityScreen(onOpenNode = { navController.navigate(Routes.browse(it)) })
            }

            composable(Routes.KEYBOARD) { KeyboardScreen() }

            composable(Routes.SETTINGS) {
                SettingsScreen(onOpenAdmin = { navController.navigate(Routes.ADMIN) })
            }

            composable(Routes.SEARCH) {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onOpenNode = { navController.navigate(Routes.browse(it)) },
                )
            }

            composable(Routes.ADMIN) {
                AdminScreen(onOpenNode = { navController.navigate(Routes.adminBrowse(it)) })
            }

            composable(
                route = Routes.BROWSE,
                arguments = listOf(navArgument("nodeId") { type = NavType.StringType }),
            ) { entry ->
                val nodeId = entry.arguments?.getString("nodeId").orEmpty()
                BrowseScreen(
                    nodeId = nodeId,
                    onBack = { navController.popBackStack() },
                    onOpenNode = { navController.navigate(Routes.browse(it)) },
                )
            }

            composable(
                route = Routes.ADMIN_BROWSE,
                arguments = listOf(navArgument("nodeId") { type = NavType.StringType }),
            ) { entry ->
                val nodeId = entry.arguments?.getString("nodeId").orEmpty()
                BrowseScreen(
                    nodeId = nodeId,
                    onBack = { navController.popBackStack() },
                    onOpenNode = { navController.navigate(Routes.adminBrowse(it)) },
                    adminMode = true,
                )
            }
        }
    }
}
