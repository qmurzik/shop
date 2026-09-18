package dev.qmurzik.forpda.domain.model

data class UserProfile(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val rank: String? = null,
    val postsCount: Int = 0,
    val reputation: Int = 0,
    val registeredLabel: String? = null,
    val isLoggedIn: Boolean = false,
)
