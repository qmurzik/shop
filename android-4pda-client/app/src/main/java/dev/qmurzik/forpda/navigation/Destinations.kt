package dev.qmurzik.forpda.navigation

sealed class Destination(val route: String) {
    data object Forums : Destination("forums")
    data object Search : Destination("search")
    data object Bookmarks : Destination("bookmarks")
    data object Profile : Destination("profile")
    data object Login : Destination("login")

    data object Topics : Destination("forums/{forumId}") {
        const val ARG_FORUM_ID = "forumId"
        fun createRoute(forumId: String) = "forums/$forumId"
    }

    data object Thread : Destination("topics/{topicId}") {
        const val ARG_TOPIC_ID = "topicId"
        fun createRoute(topicId: String) = "topics/$topicId"
    }
}

val bottomBarDestinations = listOf(Destination.Forums, Destination.Search, Destination.Bookmarks, Destination.Profile)
