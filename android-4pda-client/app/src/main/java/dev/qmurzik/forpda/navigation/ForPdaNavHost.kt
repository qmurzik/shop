package dev.qmurzik.forpda.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.qmurzik.forpda.ui.bookmarks.BookmarksScreen
import dev.qmurzik.forpda.ui.forums.ForumsScreen
import dev.qmurzik.forpda.ui.login.LoginScreen
import dev.qmurzik.forpda.ui.profile.ProfileScreen
import dev.qmurzik.forpda.ui.search.SearchScreen
import dev.qmurzik.forpda.ui.thread.ThreadScreen
import dev.qmurzik.forpda.ui.topics.TopicsScreen

@Composable
fun ForPdaNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = bottomBarDestinations.any { dest ->
        currentDestination?.hierarchy?.any { it.route == dest.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomBarDestinations.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon(), contentDescription = null) },
                            label = { Text(destination.label()) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Forums.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destination.Forums.route) {
                ForumsScreen(onForumClick = { forumId ->
                    navController.navigate(Destination.Topics.createRoute(forumId))
                })
            }
            composable(Destination.Topics.route) {
                TopicsScreen(
                    onTopicClick = { topicId -> navController.navigate(Destination.Thread.createRoute(topicId)) },
                    onBackClick = { navController.popBackStack() },
                )
            }
            composable(Destination.Thread.route) {
                ThreadScreen(onBackClick = { navController.popBackStack() })
            }
            composable(Destination.Search.route) {
                SearchScreen(onTopicClick = { topicId -> navController.navigate(Destination.Thread.createRoute(topicId)) })
            }
            composable(Destination.Bookmarks.route) {
                BookmarksScreen()
            }
            composable(Destination.Profile.route) {
                ProfileScreen(onLoginClick = { navController.navigate(Destination.Login.route) })
            }
            composable(Destination.Login.route) {
                LoginScreen(onLoggedIn = { navController.popBackStack() })
            }
        }
    }
}

private fun Destination.icon() = when (this) {
    Destination.Forums -> Icons.Filled.Forum
    Destination.Search -> Icons.Filled.Search
    Destination.Bookmarks -> Icons.Filled.Bookmark
    Destination.Profile -> Icons.Filled.Person
    else -> Icons.Filled.Forum
}

private fun Destination.label() = when (this) {
    Destination.Forums -> "Форумы"
    Destination.Search -> "Поиск"
    Destination.Bookmarks -> "Закладки"
    Destination.Profile -> "Профиль"
    else -> ""
}
